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

// OptOutPartition is a sorted array of items, each item is a range of bytes within the store
// that stores identity hash (byte[]), advertising id (byte[]), and the timestamp of the optout entry
public class OptOutPartition extends OptOutCollection {
    private byte[] store = null;

    public OptOutPartition(byte[] backingStore) {
        super(backingStore);
        this.store = backingStore;
    }

    public boolean contains(byte[] identityHash) {
        return -1 != this.binarySearch(0, this.size(), identityHash);
    }

    public long getOptOutTimestamp(byte[] identityHash) {
        int entryIndex = this.binarySearch(0, this.size(), identityHash);
        if (-1 == entryIndex) {
            // -1 to indicate optout entry not exists
            return -1;
        }

        return getTimestampByIndex(entryIndex);
    }

    // A recursive binary search function. It returns
    // location of bytes in given array arr[left..right] is present,
    // otherwise -1
    private int binarySearch(int left, int right, byte[] identityHash) {
        if (right > left) {
            int mid = left + (right - 1 - left) / 2;
            int cmp = this.compareEntryToIdentityHash(mid, identityHash);

            // If the element is present at the middle itself
            if (cmp == 0)
                return mid;

            // If element is smaller than mid, then it can only be present in left subarray
            if (cmp > 0)
                return binarySearch(left, mid, identityHash);

            // Else the element can only be present in right subarray
            return binarySearch(mid + 1, right, identityHash);
        }

        // We reach here when element is not
        // present in array
        return -1;
    }

    private int compareEntryToIdentityHash(int entryIndex, byte[] identityHash) {
        // identityHash must be of expected size that contains the SHA256 hash
        assert identityHash.length == OptOutConst.Sha256Bytes;

        // start byte index is calculated from itemIndex and optout entry size
        int byteIndex = entryIndex * OptOutConst.EntrySize;

        // compare if bytes match
        return OptOutUtils.compareByteRange(this.store, byteIndex, identityHash, 0, OptOutConst.Sha256Bytes);
    }

    private long getTimestampByIndex(int entryIndex) {
        // start byte index is calculated from itemIndex and optout entry size
        return OptOutEntry.parseTimestamp(this.store, entryIndex * OptOutConst.EntrySize);
    }
}
