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

package com.uid2.shared.optout;

import java.util.function.Consumer;

public class OptOutCollection {
    private byte[] store = null;

    public OptOutCollection() {
    }

    public OptOutCollection(byte[] backingStore) {
        this.setStore(backingStore);
    }

    public OptOutCollection(OptOutEntry[] entries) {
        this.store = new byte[entries.length * OptOutConst.EntrySize];
        int offset = 0;
        for (OptOutEntry e : entries) {
            e.copyToByteArray(this.store, offset);
            offset += OptOutConst.EntrySize;
        }
    }

    public byte[] getStore() {
        return this.store;
    }

    protected void setStore(byte[] backingStore) {
        // size of byte store must be positive and multiply of entry size
        assert backingStore != null && backingStore.length > 0;
        assert (backingStore.length % OptOutConst.EntrySize) == 0;
        this.store = backingStore;
    }

    public int size() {
        return this.store.length / OptOutConst.EntrySize;
    }

    public OptOutEntry get(int pos) {
        assert pos >= 0 && pos < this.size();
        int bufPos = pos * OptOutConst.EntrySize;
        return OptOutEntry.parse(store, bufPos);
    }

    public void forEach(Consumer<OptOutEntry> func) {
        for (int i = 0; i < this.size(); ++i) {
            func.accept(OptOutEntry.parse(this.store, i * OptOutConst.EntrySize));
        }
    }

    public void set(BloomFilter bf) {
        for (int i = 0; i < this.size(); ++i) {
            int bufPos = i * OptOutConst.EntrySize;
            bf.add(this.store, bufPos);
        }
    }
}
