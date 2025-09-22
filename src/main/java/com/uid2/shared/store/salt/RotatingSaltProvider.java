package com.uid2.shared.store.salt;

import com.uid2.shared.Utils;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.SaltEntry;
import com.uid2.shared.store.reader.IMetadataVersionedStore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/*
  1. metadata.json format

    {
      "version" : <long>,
      "generated" : <unix_epoch_seconds>,
      "first_level" : "<first_level_salt>",
       "salts" : [
        {
            "effective" : <unix_epoch_seconds>,
            "location" : "s3_path_to_file.txt",
            "id_prefix" : "a",
            "id_secret" : "<secret_key>",
            "size" : 1048576
         }
       ]
    }

  2. currentSalt file format
  <id>,<hash_id>,<currentSalt>,<refreshFrom>,<previousSalt>,<currentKey.id>,<currentKey.key>,<currentKey.salt>,<previousKey.id>,<previousKey.key>,<previousKey.salt>
  9000099,1614556800000,currentSalt,1756252800000,previousSalt,100,currentKeyKey,currentKeySalt,1,previousKeyKey,previousKeySalt
 */
public class RotatingSaltProvider implements ISaltProvider, IMetadataVersionedStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingSaltProvider.class);

    private final DownloadCloudStorage metadataStreamProvider;
    private final DownloadCloudStorage contentStreamProvider;
    @Getter
    private final String metadataPath;
    private final AtomicReference<List<SaltSnapshot>> snapshotsByEffectiveTime = new AtomicReference<>();

    public RotatingSaltProvider(DownloadCloudStorage fileStreamProvider, String metadataPath) {
        this.metadataStreamProvider = fileStreamProvider;
        if (fileStreamProvider instanceof UidCoreClient) {
            this.contentStreamProvider = ((UidCoreClient) fileStreamProvider).getContentStorage();
        } else {
            this.contentStreamProvider = fileStreamProvider;
        }
        this.metadataPath = metadataPath;
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        try (InputStream s = this.metadataStreamProvider.download(this.metadataPath)) {
            return Utils.toJsonObject(s);
        }
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        final JsonArray salts = metadata.getJsonArray("salts");
        final String firstLevelSalt = metadata.getString("first_level");
        final SaltFileParser saltFileParser = new SaltFileParser(
                new IdHashingScheme(metadata.getString("id_prefix"), metadata.getString("id_secret")));
        final Instant now = Instant.now();
        final List<SaltSnapshot> snapshots = new ArrayList<>();

        int saltCount = 0;
        for (int i = 0; i < salts.size(); ++i) {
            final SaltSnapshot snapshot = this.loadSnapshot(salts.getJsonObject(i), firstLevelSalt, saltFileParser, now);
            if (snapshot == null) continue;
            snapshots.add(snapshot);

            // don't sum up the salts from snapshots to avoid screwing up metrics
            saltCount = snapshot.entries.length;
        }

        // Store snapshots in order of them becoming effective
        this.snapshotsByEffectiveTime.set(snapshots.stream()
                .sorted(Comparator.comparing(SaltSnapshot::getEffective))
                .collect(Collectors.toList()));

        return saltCount;
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    public List<SaltSnapshot> getSnapshots() {
        return this.snapshotsByEffectiveTime.get();
    }

    @Override
    public ISaltSnapshot getSnapshot(Instant asOf) {
        final List<SaltSnapshot> snapshots = this.snapshotsByEffectiveTime.get();
        // Last snapshot past its effective timestamp
        ISaltSnapshot current = null;
        for (SaltSnapshot snapshot : snapshots) {
            if (!snapshot.isEffective(asOf)) break;
            current = snapshot;
        }
        return current != null ? current : snapshots.getLast();
    }

    private SaltSnapshot loadSnapshot(JsonObject spec, String firstLevelSalt, SaltFileParser saltFileParser, Instant now) throws Exception {
        final Instant defaultExpires = now.plus(365, ChronoUnit.DAYS);
        final Instant effective = Instant.ofEpochMilli(spec.getLong("effective"));
        final Instant expires = Instant.ofEpochMilli(spec.getLong("expires", defaultExpires.toEpochMilli()));

        final String path = spec.getString("location");
        Integer size = spec.getInteger("size");

        final long downloadStart = System.currentTimeMillis();
        SaltEntry[] entries = readInputStream(this.contentStreamProvider.download(path), saltFileParser, size);
        final long downloadEnd = System.currentTimeMillis();
        LOGGER.info("Salt file downloaded: {} in {} ms", contentStreamProvider.mask(path), downloadEnd - downloadStart);

        LOGGER.info("Loaded {} salts", size);
        return new SaltSnapshot(effective, expires, entries, firstLevelSalt);
    }

    protected SaltEntry[] readInputStream(InputStream inputStream, SaltFileParser saltFileParser, Integer size) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] saltFileLines = reader.lines().toArray(String[]::new);
            return saltFileParser.parseFileLines(saltFileLines, size);
        }
    }

    public static class SaltSnapshot implements ISaltSnapshot {
        private static final ISaltEntryIndexer MILLION_ENTRY_INDEXER = new OneMillionSaltEntryIndexer();
        private static final ISaltEntryIndexer MOD_BASED_INDEXER = new ModBasedSaltEntryIndexer();

        @Getter
        private final Instant effective;
        private final Instant expires;
        private final SaltEntry[] entries;
        private final String firstLevelSalt;
        private final ISaltEntryIndexer saltEntryIndexer;

        public SaltSnapshot(Instant effective, Instant expires, SaltEntry[] entries, String firstLevelSalt) {
            this.effective = effective;
            this.expires = expires;
            this.entries = entries;
            this.firstLevelSalt = firstLevelSalt;
            if (entries.length == 1_048_576) {
                LOGGER.info("Total salt bucket entries 1 million, {}, special production currentSalt entry indexer", entries.length);
                this.saltEntryIndexer = MILLION_ENTRY_INDEXER;
            } else {
                LOGGER.warn("Total salt bucket entries {}, using slower mod-based indexer", entries.length);
                this.saltEntryIndexer = MOD_BASED_INDEXER;
            }
        }

        @Override
        public Instant getExpires() {
            return this.expires;
        }

        public boolean isEffective(Instant asOf) {
            return !this.effective.isAfter(asOf) && this.expires.isAfter(asOf);
        }

        @Override
        public String getFirstLevelSalt() {
            return firstLevelSalt;
        }

        @Override
        public SaltEntry[] getAllRotatingSalts() {
            return this.entries;
        }

        @Override
        public SaltEntry getRotatingSalt(byte[] identity) {
            final int idx = saltEntryIndexer.getIndex(identity, entries.length);
            return this.entries[idx];
        }

        @Override
        public List<SaltEntry> getModifiedSince(Instant timestamp) {
            final long timestampMillis = timestamp.toEpochMilli();
            return Arrays.stream(this.entries).filter(e -> e.lastUpdated() >= timestampMillis).collect(Collectors.toList());
        }
    }

}
