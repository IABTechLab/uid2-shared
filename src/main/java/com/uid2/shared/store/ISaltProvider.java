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

import com.uid2.shared.model.SaltEntry;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.Instant;
import java.util.List;

public interface ISaltProvider {

    ISaltSnapshot getSnapshot(Instant asOf);

    static interface ISaltSnapshot {
        SaltEntry getRotatingSalt(String identity);

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
