package com.uid2.shared.store;

import com.uid2.shared.model.SaltEntry;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.Instant;
import java.util.List;

public interface ISaltProvider {

    ISaltSnapshot getSnapshot(Instant asOf);

    static interface ISaltSnapshot {
        SaltEntry getRotatingSalt(byte[] identity);

        String getFirstLevelSalt();

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
            LOGGER.warn("slow mod-based indexer is used, this is intended only for unit test");
            int hash = ((shaBytes[0] & 0xFF) << 12) | ((shaBytes[1] & 0xFF) << 4) | ((shaBytes[2] & 0xFF) & 0xF);
            return hash % totalEntries;
        }
    }
}
