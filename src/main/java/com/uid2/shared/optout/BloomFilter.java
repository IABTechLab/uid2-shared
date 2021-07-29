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

import java.util.Arrays;

// a bloom filter is a huge dense bitfield, with array of long as backing storage.
// given a uniformly random distributed input (byte[]), as in our case, we can just
// extract certain bits from the input, which would be the lowest N bits required
// to address the entire bitfield.
//
// The bit index extracted to address the bit, is called bfIndex. Conceptually,
// 1 == bitfield[bfIndex] means the object (byte[]) is likely contained by bloom filter,
// 0 == bitfield[bfIndex] means the object is definitely not contained by bloom filter.
//
// Due to backing storage being long (64-bit), bfIndex can be split into 2 indices:
// A level 1 index, to address to the variable inside of long[] containing the bit
// A level 2 index, to address the bit within the long
//
// Note: this is a simplified bloomfilter implementation with just 1 hash function.
// Memory space usage can be reduced by ~6x by allowing k > 1 hash functions as a
// potential future improvement.
//
public class BloomFilter {
    // bits that can be stored in 2^31 bytes ~= 16 billion, should be more than enough
    public static final long MAX_CAPACITY = ((long) Integer.MAX_VALUE + 1) * 8;

    // It is slightly twisted: total 64 bits of 0 and 1 can be stored in long (0-63),
    // which requires 6 bits (2^6 = 64) from the bfIndex to address
    private static final int l2IndexBits = OptOutUtils.logTwo(Long.BYTES * 8);

    // a bit mask to extract l2Index from bfIndex
    private static final int l2IndexMask = (1 << BloomFilter.l2IndexBits) - 1;

    // bits needed for address level 1 index (the long array)
    private int l1IndexBits;

    // a bit mask to extract bfIndex from any byte array (using top bytes)
    // assuming max array size of 32 bits integer, plus bits required to address within long (6 bits)
    // total information stored in bloom filter can be stored in 38 bits index
    private long bfMask;

    // this is the size of bits
    private long size;

    // this is the capacity of bits
    private long capacity;

    // array of 64 bits long to store bloom filter bits
    private long[] bitfield = null;

    public BloomFilter(long proposedCapacity) {
        assert proposedCapacity > 0;
        int proposedArraySize = (int) ((proposedCapacity + l2IndexMask) >> l2IndexBits);
        int arraySize = Integer.highestOneBit(proposedArraySize);
        if (arraySize < proposedArraySize) {
            arraySize = Integer.highestOneBit(proposedArraySize << 1);
        }

        // initialize empty bitfield
        this.bitfield = new long[arraySize];
        this.size = 0;
        this.capacity = (long) arraySize * Long.BYTES * 8;

        // pre-calculated values for bf bit operations
        this.l1IndexBits = OptOutUtils.logTwo(arraySize);
        this.bfMask = (1L << (this.l1IndexBits + BloomFilter.l2IndexBits)) - 1;
    }

    // getter for capacity
    public long capacity() {
        return this.capacity;
    }

    // getter for size
    public long size() {
        return this.size;
    }

    // calculate load factor
    public float load() {
        return (float) this.size / this.capacity;
    }

    public long idealCapacity() {
        return BloomFilter.idealCapacity(this.size);
    }

    public static long idealCapacity(long size) {
        // ideal capacity should maintain ~1% miss rate, e.g. load factor ~= 0.01
        return Math.min(Long.highestOneBit(size) << 7, 0x800000000L);
    }

    // getter for bfMask
    public long bfMask() {
        return this.bfMask;
    }

    // zero out the backing store
    public void reset() {
        this.size = 0;
        Arrays.fill(this.bitfield, 0L);
    }

    public boolean add(byte[] bytes) {
        return this.add(bytes, 0);
    }

    public boolean add(byte[] bytes, int offset) {
        if (likelyContains(bytes, offset)) return false;

        ++this.size;
        long bfIndex = this.getBfIndex(bytes, offset);
        int l1Index = (int) (bfIndex >> BloomFilter.l2IndexBits);
        int l2Index = (int) (bfIndex & BloomFilter.l2IndexMask);
        this.bitfield[l1Index] |= this.getL2BitMask(l2Index);
        return true;
    }

    public boolean likelyContains(byte[] bytes) {
        return this.likelyContains(bytes, 0);
    }

    public boolean likelyContains(byte[] bytes, int offset) {
        long bfIndex = this.getBfIndex(bytes, offset);
        int l1Index = (int) (bfIndex >> BloomFilter.l2IndexBits);
        int l2Index = (int) (bfIndex & BloomFilter.l2IndexMask);
        return 0 != (this.bitfield[l1Index] & this.getL2BitMask(l2Index));
    }

    private long getBfIndex(byte[] bytes, int offset) {
        int totalBitsToExtract = this.l1IndexBits + BloomFilter.l2IndexBits;
        int highestBytesToExtract = (totalBitsToExtract + 7) / 8;
        assert highestBytesToExtract <= 5 && highestBytesToExtract >= 1;

        // using little-endian to extract bitIndex

        long val = 0;
        long bitShift = 0;
        for (int i = 0; i < highestBytesToExtract; ++i) {
            val += Byte.toUnsignedLong(bytes[offset + i]) << bitShift;
            bitShift += 8;
        }

        /*
        long bitIndex = bytes[--highestBytesToExtract];
        while (highestBytesToExtract-- > 0) {
            bitIndex = bitIndex * 256 + bytes[highestBytesToExtract];
        }
        */
        return val & this.bfMask;
    }

    private long getL2BitMask(int l2Index) {
        return 0x1L << l2Index;
    }
}
