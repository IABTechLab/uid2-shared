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

package com.uid2.shared.model;

import com.uid2.shared.model.KeyIdentifier;

import java.time.Instant;

public final class EncryptionKey {
    private final int id;
    private final byte[] keyBytes;
    private final Instant created;
    private final Instant activates;
    private final Instant expires;
    private final int siteId;

    public EncryptionKey(int id, byte[] keyBytes, Instant created, Instant activates, Instant expires, int siteId) {
        this.id = id;
        this.keyBytes = keyBytes;
        this.created = created;
        this.expires = expires;
        this.activates = activates;
        this.siteId = siteId;
    }

    public int getId() {
        return id;
    }

    public byte[] getKeyBytes() {
        return keyBytes;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getActivates() {
        return activates;
    }

    public Instant getExpires() {
        return expires;
    }

    public int getSiteId() { return siteId; }

    public KeyIdentifier getKeyIdentifier() {
        return new KeyIdentifier(this.id);
    }

    public boolean isExpired(Instant asOf) { return !expires.isAfter(asOf); }
}
