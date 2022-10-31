package com.uid2.shared.store;

import com.uid2.shared.Utils;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.SaltEntry;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.hashids.Hashids;

import java.io.BufferedReader;
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

  2. salt file format
        <id>,   <hash_id>,    <salt>
        9000099,1614556800000,salt
 */
public class RotatingSaltProvider implements ISaltProvider, IMetadataVersionedStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingSaltProvider.class);
    private static final int HashingSeed = 122054;
    public static RotatingSaltProvider INSTANCE;
    private final ICloudStorage metadataStreamProvider;
    private final ICloudStorage contentStreamProvider;
    private final String metadataPath;
    private final AtomicReference<List<SaltSnapshot>> snapshotsByEffectiveTime = new AtomicReference<>();

    public RotatingSaltProvider(ICloudStorage fileStreamProvider, String metadataPath) {
        this.metadataStreamProvider = fileStreamProvider;
        if (fileStreamProvider instanceof UidCoreClient) {
            this.contentStreamProvider = ((UidCoreClient) fileStreamProvider).getContentStorage();
        } else {
            this.contentStreamProvider = fileStreamProvider;
        }
        this.metadataPath = metadataPath;
    }

    public String getMetadataPath() { return this.metadataPath; }

    @Override
    public JsonObject getMetadata() throws Exception {
        InputStream s = this.metadataStreamProvider.download(this.metadataPath);
        return Utils.toJsonObject(s);
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        final JsonArray salts = metadata.getJsonArray("salts");
        final String firstLevelSalt = metadata.getString("first_level");
        final SaltEntryBuilder entryBuilder = new SaltEntryBuilder(
                new IdHashingScheme(metadata.getString("id_prefix"), metadata.getString("id_secret")));
        final Instant now = Instant.now();
        final List<SaltSnapshot> snapshots = new ArrayList<>();

        int saltCount = 0;
        for (int i = 0; i < salts.size(); ++i) {
            final SaltSnapshot snapshot = this.loadSnapshot(salts.getJsonObject(i), firstLevelSalt, entryBuilder, now);
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
        return current;
    }

    private SaltSnapshot loadSnapshot(JsonObject spec, String firstLevelSalt, SaltEntryBuilder entryBuilder, Instant now) throws Exception {
        final Instant defaultExpires = now.plus(365, ChronoUnit.DAYS);
        final Instant effective = Instant.ofEpochMilli(spec.getLong("effective"));
        final Instant expires = Instant.ofEpochMilli(spec.getLong("expires", defaultExpires.toEpochMilli()));

        // no point in loading expired snapshots
        if (now.isAfter(expires)) return null;

        final String path = spec.getString("location");
        final InputStream inputStream = this.contentStreamProvider.download(path);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        final SaltEntry[] entries = new SaltEntry[spec.getInteger("size")];

        int idx = 0;
        for (String l; (l = reader.readLine()) != null; ++idx) {
            // System.out.println("Processing Line " + l);
            final SaltEntry entry = entryBuilder.toEntry(l);
            entries[idx] = entry;
        }

        LOGGER.info("Loaded " + idx + " salts");
        return new SaltSnapshot(effective, expires, entries, firstLevelSalt);
    }

    public static class SaltSnapshot implements ISaltSnapshot {
        private final Instant effective;
        private final Instant expires;
        private final SaltEntry[] entries;
        private final String firstLevelSalt;
        private final ISaltEntryIndexer saltEntryIndexer;
        private static final ISaltEntryIndexer staticMillionEntryIndexer = new OneMillionSaltEntryIndexer();
        private static final ISaltEntryIndexer staticModBasedIndexer = new ModBasedSaltEntryIndexer();

        public SaltSnapshot(Instant effective, Instant expires, SaltEntry[] entries, String firstLevelSalt) {
            this.effective = effective;
            this.expires = expires;
            this.entries = entries;
            this.firstLevelSalt = firstLevelSalt;
            if (entries.length == 1_048_576) {
                LOGGER.info("Total salt entries 1 million, " + entries.length +", special production salt entry indexer");
                this.saltEntryIndexer = staticMillionEntryIndexer;
            } else {
                LOGGER.warn("Total salt entries " + entries.length +", using slower mod-based indexer");
                this.saltEntryIndexer = staticModBasedIndexer;
            }
        }

        public Instant getEffective() {
            return this.effective;
        }

        public Instant getExpires() {
            return this.expires;
        }

        public boolean isEffective(Instant asOf) {
            return !this.effective.isAfter(asOf) && this.expires.isAfter(asOf);
        }

        @Override
        public String getFirstLevelSalt() { return firstLevelSalt; }
        @Override
        public SaltEntry[] getAllRotatingSalts() { return this.entries; }

        @Override
        public SaltEntry getRotatingSalt(byte[] identity) {
            final int idx = saltEntryIndexer.getIndex(identity, entries.length);
            return this.entries[idx];
        }

        @Override
        public List<SaltEntry> getModifiedSince(Instant timestamp) {
            final long timestampMillis = timestamp.toEpochMilli();
            return Arrays.stream(this.entries).filter(e -> e.getLastUpdated() >= timestampMillis).collect(Collectors.toList());
        }
    }

    static final class IdHashingScheme {
        private final String prefix;
        private final Hashids hasher;

        public IdHashingScheme(final String prefix, final String secret) {
            this.prefix = prefix;
            this.hasher = new Hashids(secret, 9);
        }

        public String encode(long id) {
            return prefix + this.hasher.encode(id);
        }
    }

    static final class SaltEntryBuilder {
        private final IdHashingScheme idHashingScheme;

        public SaltEntryBuilder(IdHashingScheme idHashingScheme) {
            this.idHashingScheme = idHashingScheme;
        }

        public SaltEntry toEntry(String line) {
            try {
                final String[] fields = line.split(",");
                final long id = Integer.parseInt(fields[0]);
                final String hashedId = this.idHashingScheme.encode(id);
                final long lastUpdated = Long.parseLong(fields[1]);
                final String salt = fields[2];
                return new SaltEntry(id, hashedId, lastUpdated, salt);
            } catch (Exception e) {
                throw new RuntimeException("Trouble parsing Salt Entry " + line, e);
            }
        }
    }
}
