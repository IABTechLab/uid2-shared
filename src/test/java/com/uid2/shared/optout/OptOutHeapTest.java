package com.uid2.shared.optout;


import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


public class OptOutHeapTest {
    @Test
    public void createZeroSize_expectFail() {
        assertThrows(AssertionError.class, () -> {
            OptOutHeap heap = new OptOutHeap(0);
        });
    }

    @Test
    public void createNegativeSize_expectFail() {
        assertThrows(AssertionError.class, () -> {
            OptOutHeap heap = new OptOutHeap(-1);
        });
    }

    @Test
    public void createOneSizeAddOne() {
        OptOutHeap heap = new OptOutHeap(1);
        OptOutEntry entry = OptOutEntry.newRandom();
        heap.add(entry);
        assertEquals(1, heap.size());
        heap.forEach(e -> {
            assertFalse(entry == e);
            assertEquals(entry, e);
            assertArrayEquals(entry.identityHash, e.identityHash);
            assertArrayEquals(entry.advertisingId, e.advertisingId);
            assertEquals(entry.timestamp, e.timestamp);
        });
    }

    @Test
    public void createSmallHeapRandomValues_checkNoLoss() {
        OptOutHeap heap = new OptOutHeap(100);
        HashSet<OptOutEntry> entrySet = new HashSet<>();
        HashSet<Long> timestampSet = new HashSet<>();
        for (int i = 0; i < 50; ++i) {
            OptOutEntry entry = OptOutEntry.newRandom();
            heap.add(entry);
            entrySet.add(entry);
            timestampSet.add(entry.timestamp);
            // System.out.format("added %d %d\n", entry.idHashAsLong(), entry.timestamp);
        }
        assertEquals(entrySet.size(), heap.size());
        heap.forEach(e -> {
            // System.out.format("found %d %d\n", e.idHashAsLong(), e.timestamp);

            // each entry in heap should be in the entrySet once and only once
            assertTrue(entrySet.contains(e));
            entrySet.remove(e);

            // additionally check timestamp
            assertTrue(timestampSet.contains(e.timestamp));
            timestampSet.remove(e.timestamp);
        });
    }

    @Test
    public void createSmallHeapRandomValues_checkHeapProperty() {
        OptOutHeap heap = new OptOutHeap(100);
        for (int i = 0; i < 50; ++i) {
            OptOutEntry entry = OptOutEntry.newRandom();
            heap.add(entry);
            // System.out.format("added %d %d\n", entry.idHashAsLong(), entry.timestamp);
        }

        checkHeapProperty(heap, 0);
    }

    @Test
    public void testSortInline() {
        OptOutHeap heap = new OptOutHeap(100);
        for (int i = 0; i < heap.capacity(); ++i) {
            long random = OptOutUtils.rand.nextLong();
            OptOutEntry entry = OptOutEntry.newTestEntry(random, random);
            heap.add(entry);
            // System.out.format("added 0x%016xL 0x%016xL\n", entry.idHashAsLong(), entry.timestamp);
        }

        // heap.add(OptOutEntry.newTestEntry(0x94ad36770a667e32L, 0x94ad36770a667e32L));
        checkHeapProperty(heap, 0);

        OptOutPartition s = heap.toPartition(false);
        long prevHash = s.get(0).idHashAsLong();
        for (int i = 1; i < heap.size(); ++i) {
            long curHash = s.get(i).idHashAsLong();
            // System.out.format("identity hash 0x%016xL 0x%016xL\n", prevHash, curHash);
            prevHash = curHash;
            assertTrue(Long.compareUnsigned(prevHash, curHash) <= 0);
        }
    }

    @Test
    public void testAutoGrow() {
        OptOutHeap heap = new OptOutHeap(1);
        for (int i = 0; i < 100; ++i) {
            OptOutEntry entry = OptOutEntry.newRandom();
            heap.add(entry);
            // System.out.format("added %d %d\n", entry.idHashAsLong(), entry.timestamp);
        }

        checkHeapProperty(heap, 0);
    }

    private void checkHeapProperty(OptOutHeap heap, int i) {
        OptOutEntry self = heap.get(i);

        int leftChild = OptOutHeap.leftChild(i);
        if (leftChild >= heap.size()) return;
        OptOutEntry left = heap.get(leftChild);
        // System.out.format("self vs left: %x %x\n", self.idHashAsLong(), left.idHashAsLong());
        assertTrue(Arrays.compareUnsigned(self.identityHash, left.identityHash) >0 );
        checkHeapProperty(heap, leftChild);

        int rightChild = OptOutHeap.rightChild(i);
        if (rightChild >= heap.size()) return;
        OptOutEntry right = heap.get(rightChild);
        // System.out.format("self vs right: %x %x\n", self.idHashAsLong(), right.idHashAsLong());
        assertTrue(Arrays.compareUnsigned(self.identityHash, left.identityHash) >0 );
        checkHeapProperty(heap, rightChild);
    }
}
