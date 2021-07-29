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

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;

// append only heap
public class OptOutHeap extends OptOutCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptOutHeap.class);

    private byte[] tempEntry = null;
    private byte[] store = null;
    private int size = 0;
    private int capacity = 0;

    public OptOutHeap(int capacity) {
        // ensure size is smaller than 2GB
        assert capacity > 0 && capacity <= (Integer.MAX_VALUE / OptOutConst.EntrySize);
        this.store = new byte[capacity * OptOutConst.EntrySize];
        this.setStore(this.store);
        this.capacity = capacity;
        this.tempEntry = new byte[OptOutConst.EntrySize];
    }

    // clone an existing heap
    private OptOutHeap(OptOutHeap other) {
        this.store = Arrays.copyOf(other.store, other.store.length);
        this.setStore(this.store);
        this.size = other.size;
        this.capacity = other.capacity;
        this.tempEntry = new byte[OptOutConst.EntrySize];
    }

    public static int parent(int i) {
        return (i - 1) >> 1;
    }

    public static int leftChild(int i) {
        return 2 * i + 1;
    }

    public static int rightChild(int i) {
        return 2 * i + 2;
    }

    public int capacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public void reset() {
        this.size = 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    public OptOutHeap clone() {
        return new OptOutHeap(this);
    }

    public OptOutPartition toPartition(boolean mergeDups) {
        // make a copy of the current heap
        OptOutHeap copy = new OptOutHeap(this);

        // inline sort entries based on identity hash, on the copy
        int entries = copy.sortInline();

        if (entries > 1 && mergeDups) {
            // if there are duplicate entries (entries with the same identity hash)
            // mergeDups == true: merge those entries into a single entry that with highest timestamp
            // mergeDups == false: keep all duplicate entries while sorting
            int j = 0;
            for (int i = 1; i < entries; ++i) {
                if (0 == copy.compareEntriesInHeap(i, j)) {
                    // if identity hash matches, set timestamp to the max of two dupicates
                    long maxTs = Math.max(copy.getEntryTimestamp(i), copy.getEntryTimestamp(j));
                    copy.setEntryTimestamp(j, maxTs);
                } else {
                    ++j;
                    if (j < i) {
                        // if there are gaps, copy item[i] at location item[j+1]
                        copy.copyEntriesInHeap(i, j);
                    }
                }
            }

            // update new entries count (without dups)
            entries = j + 1;
        }

        // pass the sorted store to a new snapshot object
        byte[] data = Arrays.copyOfRange(copy.store, 0, entries * OptOutConst.EntrySize);
        OptOutPartition snapshot = new OptOutPartition(data);
        return snapshot;
    }

    public void add(OptOutCollection otherStore) {
        byte[] sourceData = otherStore.getStore();
        int newEntries = otherStore.size();
        this.checkCapacity(newEntries);

        // copy new entries using byte[] arraycopy
        int destOffset = size * OptOutConst.EntrySize;
        System.arraycopy(sourceData, 0, store, destOffset, newEntries * OptOutConst.EntrySize);

        // call heapify up
        for (int i = 0; i < newEntries; ++i) {
            this.heapifyUp(size++);
        }
    }

    public void add(OptOutEntry entry) {
        assert entry != null;
        this.checkCapacity(1);
        this.copyEntryIntoHeap(entry, size);
        this.heapifyUp(size++);
    }

    public void add(OptOutEntry[] entries) {
        assert entries != null;
        this.checkCapacity(entries.length);

        // copy entries
        for (int i = 0; i < entries.length; ++i) {
            this.copyEntryIntoHeap(entries[i], size + i);
        }

        // heapify them one by one
        for (int i = 0; i < entries.length; ++i) {
            this.heapifyUp(size++);
        }
    }

    private int sortInline() {
        // sort break heap property, size will be set to 0 after sort
        int origSize = this.size;
        while (--this.size > 0) {
            // swap item[0] (max value) to the last entry
            this.swapEntriesInHeap(0, this.size);
            this.heapifyDown(0);
        }

        return origSize;
    }

    private void checkCapacity(int newEntries) {
        int sizeNeeded = newEntries + size;
        if (sizeNeeded > capacity) {
            int newArraySize = Math.max(sizeNeeded, capacity * 2) * OptOutConst.EntrySize;
            if (newArraySize < 0) {
                LOGGER.fatal("checkCapacity error: old " + capacity + " plus " + newEntries + " new " + newArraySize);
                assert false;
            }

            // grow store if necessary
            this.store = Arrays.copyOf(this.store, newArraySize);
            this.setStore(this.store);
            this.capacity = capacity * 2;
        }
    }

    private void heapifyUp(int last) {
        // heapifyUp not needed for the first entry
        if (last == 0) return;

        // no need to do anything if heap property is already satisfied: item[last] <= item[parent(last)]
        if (this.compareEntriesInHeap(last, parent(last)) <= 0) return;

        // otherwise, make a copy of the new entry
        byte[] temp = this.copyEntryToTemp(last);

        // find the correct pos (i) for temp
        int i = last;
        do {
            // move parent down
            this.copyEntriesInHeap(parent(i), i);

            // compare against parent's parent
            i = parent(i);

            // break the loop when found the right place for temp
        } while (i > 0 && this.compareToEntryInHeap(temp, parent(i)) > 0);

        // copy temp to the right place
        this.copyEntryIntoHeap(temp, i);
    }

    private void heapifyDown(int i) {
        // heapifyDown not needed for entry with no childs
        if (leftChild(i) >= size) return;

        // assuming heapify from index 0, make a copy entry at index 0
        assert i == 0;
        byte[] temp = this.copyEntryToTemp(i);

        do {
            // find max child for i
            int child = maxChild(i);

            // break the loop if heap property is already satisfied: item[i] >= item[maxChild]
            int cmp = this.compareToEntryInHeap(temp, child);
            if (cmp >= 0) break;

            // move child up, and go one level down
            this.copyEntriesInHeap(child, i);
            i = child;

            // check if current i has child or not
        } while (leftChild(i) < size);

        // move entry into index i
        this.copyEntryIntoHeap(temp, i);
    }

    private int maxChild(int i) {
        int left = leftChild(i);
        int right = rightChild(i);
        // return left child's index, if there is no right child
        if (right >= size) return left;
        // otherwise return the larger one
        return this.compareEntriesInHeap(left, right) > 0 ? left : right;
    }

    private int compareToEntryInHeap(byte[] entryAsBytes, int heapPos) {
        int heapBufPos = heapPos * OptOutConst.EntrySize;
        // comparing only identity hash (length == Sha256Bytes)
        return OptOutUtils.compareByteRange(entryAsBytes, 0, store, heapBufPos, OptOutConst.Sha256Bytes);
    }

    private int compareEntriesInHeap(int i, int j) {
        int p1 = i * OptOutConst.EntrySize;
        int p2 = j * OptOutConst.EntrySize;
        // comparing only identity hash (length == Sha256Bytes)
        return OptOutUtils.compareByteRange(store, p1, store, p2, OptOutConst.Sha256Bytes);
    }

    private void swapEntriesInHeap(int srcPos, int dstPos) {
        // dst => temp
        byte[] temp = this.copyEntryToTemp(dstPos);
        // src => dst
        this.copyEntriesInHeap(srcPos, dstPos);
        // temp => src
        this.copyEntryIntoHeap(temp, srcPos);
    }

    private void copyEntriesInHeap(int srcPos, int dstPos) {
        int srcBufPos = srcPos * OptOutConst.EntrySize;
        int dstBufPos = dstPos * OptOutConst.EntrySize;
        // copy entire entry (length == EntrySize)
        System.arraycopy(store, srcBufPos, store, dstBufPos, OptOutConst.EntrySize);
    }

    private void copyEntryIntoHeap(OptOutEntry entry, int heapPos) {
        // copy identity_hash
        int heapBufPos = heapPos * OptOutConst.EntrySize;
        System.arraycopy(entry.identityHash, 0, store, heapBufPos, OptOutConst.Sha256Bytes);
        heapBufPos += OptOutConst.Sha256Bytes;

        // copy advertising_id
        System.arraycopy(entry.advertisingId, 0, store, heapBufPos, OptOutConst.Sha256Bytes);
        heapBufPos += OptOutConst.Sha256Bytes;

        // copy timestamp
        System.arraycopy(OptOutUtils.toByteArray(entry.timestamp), 0, store, heapBufPos, Long.BYTES);
    }

    private void copyEntryIntoHeap(byte[] entryAsBytes, int heapPos) {
        // copy identity hash + advertising id + timestamp
        int heapBufPos = heapPos * OptOutConst.EntrySize;
        System.arraycopy(entryAsBytes, 0, store, heapBufPos, OptOutConst.EntrySize);
    }

    private byte[] copyEntryToTemp(int i) {
        int heapBufPos = i * OptOutConst.EntrySize;
        System.arraycopy(store, heapBufPos, tempEntry, 0, OptOutConst.EntrySize);
        return tempEntry;
    }

    private long getEntryTimestamp(int pos) {
        int bufPos = pos * OptOutConst.EntrySize + (OptOutConst.Sha256Bytes << 1);
        return OptOutUtils.toLong(store, bufPos);
    }

    private void setEntryTimestamp(int pos, long timestamp) {
        int bufPos = pos * OptOutConst.EntrySize + (OptOutConst.Sha256Bytes << 1);
        byte[] tsBytes = OptOutUtils.toByteArray(timestamp);
        System.arraycopy(tsBytes, 0, store, bufPos, Long.BYTES);
    }
}
