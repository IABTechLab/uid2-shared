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
