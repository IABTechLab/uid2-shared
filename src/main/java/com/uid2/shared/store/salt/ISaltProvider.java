package com.uid2.shared.store.salt;

import com.uid2.shared.model.SaltEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public interface ISaltProvider {

    ISaltSnapshot getSnapshot(Instant asOf);

    static interface ISaltSnapshot {
        SaltEntry getRotatingSalt(byte[] identity);

        String getFirstLevelSalt();
        Instant getExpires();

        List<SaltEntry> getModifiedSince(Instant timestamp);

        SaltEntry[] getAllRotatingSalts();
    }

    static interface ISaltEntryIndexer {
        int getIndex(byte[] shaBytes, int totalEntries);
    }

    static class OneMillionSaltEntryIndexer implements ISaltEntryIndexer {
        @Override
        public int getIndex(byte[] shaBytes, int totalEntries) {
            int hash = ((shaBytes[0] & 0xFF) << 12) | ((shaBytes[1] & 0xFF) << 4) | ((shaBytes[2] & 0xFF) & 0xF);
            return hash;
        }
    }

    static class ModBasedSaltEntryIndexer implements ISaltEntryIndexer {
        private static final Logger LOGGER = LoggerFactory.getLogger(ModBasedSaltEntryIndexer.class);

        @Override
        public int getIndex(byte[] shaBytes, int totalEntries) {
            int hash = ((shaBytes[0] & 0xFF) << 12) | ((shaBytes[1] & 0xFF) << 4) | ((shaBytes[2] & 0xFF) & 0xF);
            return hash % totalEntries;
        }
    }
}
