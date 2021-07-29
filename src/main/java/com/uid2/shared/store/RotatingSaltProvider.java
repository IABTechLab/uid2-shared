// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.store;

import com.uid2.shared.Utils;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.ICloudStorage;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicReference<SaltSnapshot> latestSnapshot = new AtomicReference<SaltSnapshot>();

    public RotatingSaltProvider(ICloudStorage fileStreamProvider, String metadataPath) {
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
        int totalCount = 0;
        for (int i = 0; i < salts.size(); ++i) {
            final SaltSnapshot snapshot = this.loadSnapshot(salts.getJsonObject(0), firstLevelSalt);
            totalCount += snapshot.entries.length;
            this.latestSnapshot.set(snapshot);
        }
        return totalCount;
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    @Override
    public ISaltSnapshot getSnapshot(Instant asOf) {
        return this.latestSnapshot.get();
    }

    @Override
    public ISaltSnapshot getSnapshot() {
        return this.getSnapshot(Instant.now());
    }

    private SaltSnapshot loadSnapshot(JsonObject spec, String firstLevelSalt) throws Exception {
        final SaltEntryBuilder entryBuilder = new SaltEntryBuilder(new IdHashingScheme(spec.getString("id_prefix"),
            spec.getString("id_secret")));

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
        return new SaltSnapshot(spec.getLong("effective"), entries, firstLevelSalt);
    }

    static class SaltSnapshot implements ISaltSnapshot {
        private final long effective;
        private final ISaltProvider.SaltEntry[] entries;
        private final String firstLevelSalt;
        private final ISaltEntryIndexer salEntryIndexer;
        private static final ISaltEntryIndexer staticMillionEntryIndexer = new OneMillionSaltEntryIndexer();
        private static final ISaltEntryIndexer staticModBasedIndexer = new ModBasedSaltEntryIndexer();

        public SaltSnapshot(long effective, ISaltProvider.SaltEntry[] entries, String firstLevelSalt) {
            this.effective = effective;
            this.entries = entries;
            this.firstLevelSalt = firstLevelSalt;
            if (entries.length == 1_048_576) {
                LOGGER.info("Total salt entries 1 million, " + entries.length +", special production salt entry indexer");
                salEntryIndexer = staticMillionEntryIndexer;
            } else {
                LOGGER.warn("Total salt entries " + entries.length +", using slower mod-based indexer");
                salEntryIndexer = staticModBasedIndexer;
            }
        }

        @Override
        public SaltEntry getRotatingSalt(String identity) {

            /**
             final byte[] shaBytes = EncodingUtils.fromBase64(identity);
             final int hash = MurmurHash3.hash32x86(shaBytes, 0, shaBytes.length, HashingSeed);
             return this.entries[Math.abs(hash % this.entries.length)];
             */

            final byte[] shaBytes = Base64.getDecoder().decode(identity);
            int idx = salEntryIndexer.getIndex(shaBytes, entries.length);
            return this.entries[idx];
        }

        @Override
        public String getFirstLevelSalt() {
            return this.firstLevelSalt;
        }

        @Override
        public List<SaltEntry> getModifiedSince(Instant timestamp) {
            long epochMillis = timestamp.toEpochMilli();
            if (epochMillis <= this.effective) {
                return Arrays.asList(this.entries);
            }

            return Collections.emptyList();
        }

        @Override
        public List<SaltEntry> getAllRotatingSalts() {
            return Arrays.asList(this.entries);
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
