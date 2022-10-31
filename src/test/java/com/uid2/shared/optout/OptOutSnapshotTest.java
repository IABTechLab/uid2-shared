package com.uid2.shared.optout;

import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.*;

public class OptOutSnapshotTest {
    @Test(expected = AssertionError.class)
    public void createFromNull_expectFail() {
        OptOutPartition snapshot = new OptOutPartition(null);
    }

    @Test(expected = AssertionError.class)
    public void createFromEmpty_expectFail() {
        OptOutPartition snapshot = new OptOutPartition(new byte[0]);
    }

    @Test
    public void createFromOne_tests() {
        OptOutHeap heap = new OptOutHeap(1);
        OptOutEntry entry = OptOutEntry.newTestEntry(1, 1);
        heap.add(entry);
        OptOutPartition snapshot = heap.toPartition(false);
        assertEquals(1, snapshot.size());
        assertTrue(snapshot.contains(entry.identityHash));
        assertEquals(1, snapshot.getOptOutTimestamp(entry.identityHash));

        for (int i = 0; i < 100; ++i) {
            OptOutEntry testEntry = OptOutEntry.newRandom();
            if (testEntry.idHashAsLong() == 1) continue;
            assertFalse(snapshot.contains(testEntry.identityHash));
            assertEquals(-1, snapshot.getOptOutTimestamp(testEntry.identityHash));
        }
    }

    @Test
    public void createFrom2_tests() {
        createFromN(2);
    }

    @Test
    public void createFrom3_tests() {
        createFromN(3);
    }

    @Test
    public void createFrom5_tests() {
        createFromN(5);
    }

    @Test
    public void createFrom10_tests() {
        createFromN(10);
    }

    @Test
    public void createFrom1000_tests() {
        createFromN(1000);
    }

    @Test
    public void createFromRandom_tests() {
        OptOutHeap heap = new OptOutHeap(1);
        HashSet<OptOutEntry> entries = new HashSet<>();
        for (int i = 0; i < 1000; ++i) {
            OptOutEntry entry = OptOutEntry.newRandom();
            entries.add(entry);
            heap.add(entry);
        }

        OptOutPartition snapshot = heap.toPartition(false);
        assertEquals(1000, snapshot.size());

        for (OptOutEntry entry : entries) {
            assertTrue(snapshot.contains(entry.identityHash));
            assertEquals(entry.timestamp, snapshot.getOptOutTimestamp(entry.identityHash));
        }

        for (int j = 0; j < 100; ++j) {
            OptOutEntry testEntry = OptOutEntry.newRandom();
            assertFalse(snapshot.contains(testEntry.identityHash));
            assertEquals(-1, snapshot.getOptOutTimestamp(testEntry.identityHash));
        }
    }

    @Test
    public void mergeDups_testOneNoDups() {
        OptOutHeap heap = new OptOutHeap(1);
        heap.add(OptOutEntry.newTestEntry(1, 2));

        OptOutPartition snapshot = heap.toPartition(true);
        assertEquals(1, snapshot.size());
        byte[] hash = OptOutEntry.idHashFromLong(1);
        assertEquals(2, snapshot.getOptOutTimestamp(hash));
    }

    @Test
    public void mergeDups_testTwoNoDups() {
        OptOutHeap heap = new OptOutHeap(1);
        heap.add(OptOutEntry.newTestEntry(1, 2));
        heap.add(OptOutEntry.newTestEntry(2, 1));

        OptOutPartition snapshot = heap.toPartition(true);
        assertEquals(2, snapshot.size());
        byte[] hash1 = OptOutEntry.idHashFromLong(1);
        assertEquals(2, snapshot.getOptOutTimestamp(hash1));
        byte[] hash2 = OptOutEntry.idHashFromLong(2);
        assertEquals(1, snapshot.getOptOutTimestamp(hash2));
    }

    @Test
    public void mergeDups_testTwoDups() {
        OptOutHeap heap = new OptOutHeap(1);
        heap.add(OptOutEntry.newTestEntry(1, 2));
        heap.add(OptOutEntry.newTestEntry(1, 1));

        OptOutPartition snapshot = heap.toPartition(true);
        assertEquals(1, snapshot.size());
        byte[] hash1 = OptOutEntry.idHashFromLong(1);
        assertEquals(2, snapshot.getOptOutTimestamp(hash1));
    }

    @Test
    public void mergeDups_testThreeNoDups() {
        OptOutHeap heap = new OptOutHeap(1);
        heap.add(OptOutEntry.newTestEntry(1, 2));
        heap.add(OptOutEntry.newTestEntry(2, 4));
        heap.add(OptOutEntry.newTestEntry(3, 6));

        OptOutPartition snapshot = heap.toPartition(true);
        assertEquals(3, snapshot.size());
        byte[] hash1 = OptOutEntry.idHashFromLong(1);
        assertEquals(2, snapshot.getOptOutTimestamp(hash1));
        byte[] hash2 = OptOutEntry.idHashFromLong(2);
        assertEquals(4, snapshot.getOptOutTimestamp(hash2));
        byte[] hash3 = OptOutEntry.idHashFromLong(3);
        assertEquals(6, snapshot.getOptOutTimestamp(hash3));
    }

    @Test
    public void mergeDups_testThreeDups() {
        OptOutHeap heap = new OptOutHeap(1);
        heap.add(OptOutEntry.newTestEntry(1, 2));
        heap.add(OptOutEntry.newTestEntry(1, 3));
        heap.add(OptOutEntry.newTestEntry(1, 1));

        OptOutPartition snapshot = heap.toPartition(true);
        assertEquals(1, snapshot.size());
        byte[] hash1 = OptOutEntry.idHashFromLong(1);
        assertEquals(3, snapshot.getOptOutTimestamp(hash1));
    }

    @Test
    public void mergeDups_testDupsInBeginning() {
        OptOutHeap heap = new OptOutHeap(1);
        heap.add(OptOutEntry.newTestEntry(1, 2));
        heap.add(OptOutEntry.newTestEntry(3, 6));
        heap.add(OptOutEntry.newTestEntry(2, 4));
        heap.add(OptOutEntry.newTestEntry(1, 5));
        heap.add(OptOutEntry.newTestEntry(1, 6));

        OptOutPartition snapshot = heap.toPartition(true);
        assertEquals(3, snapshot.size());
        byte[] hash2 = OptOutEntry.idHashFromLong(2);
        assertEquals(4, snapshot.getOptOutTimestamp(hash2));
        byte[] hash3 = OptOutEntry.idHashFromLong(3);
        assertEquals(6, snapshot.getOptOutTimestamp(hash3));

        // 1 should be merged
        byte[] hash1 = OptOutEntry.idHashFromLong(1);
        assertEquals(6, snapshot.getOptOutTimestamp(hash1));
    }

    @Test
    public void mergeDups_testDupsInMiddle() {
        OptOutHeap heap = new OptOutHeap(1);
        heap.add(OptOutEntry.newTestEntry(1, 2));
        heap.add(OptOutEntry.newTestEntry(3, 6));
        heap.add(OptOutEntry.newTestEntry(2, 4));
        heap.add(OptOutEntry.newTestEntry(2, 5));
        heap.add(OptOutEntry.newTestEntry(2, 6));

        OptOutPartition snapshot = heap.toPartition(true);
        assertEquals(3, snapshot.size());
        byte[] hash1 = OptOutEntry.idHashFromLong(1);
        assertEquals(2, snapshot.getOptOutTimestamp(hash1));
        byte[] hash3 = OptOutEntry.idHashFromLong(3);
        assertEquals(6, snapshot.getOptOutTimestamp(hash3));

        // 2 should be merged
        byte[] hash2 = OptOutEntry.idHashFromLong(2);
        assertEquals(6, snapshot.getOptOutTimestamp(hash2));
    }

    @Test
    public void mergeDups_testDupsInEnd() {
        OptOutHeap heap = new OptOutHeap(1);
        heap.add(OptOutEntry.newTestEntry(1, 2));
        heap.add(OptOutEntry.newTestEntry(3, 6));
        heap.add(OptOutEntry.newTestEntry(2, 4));
        heap.add(OptOutEntry.newTestEntry(3, 5));
        heap.add(OptOutEntry.newTestEntry(3, 26));

        OptOutPartition snapshot = heap.toPartition(true);
        assertEquals(3, snapshot.size());
        byte[] hash1 = OptOutEntry.idHashFromLong(1);
        assertEquals(2, snapshot.getOptOutTimestamp(hash1));
        byte[] hash2 = OptOutEntry.idHashFromLong(2);
        assertEquals(4, snapshot.getOptOutTimestamp(hash2));

        // 2 should be merged
        byte[] hash3 = OptOutEntry.idHashFromLong(3);
        assertEquals(26, snapshot.getOptOutTimestamp(hash3));
    }

    private void createFromN(int n) {
        OptOutHeap heap = new OptOutHeap(1);
        for (int i = 0; i < n; ++i) {
            OptOutEntry entry = OptOutEntry.newTestEntry(i, i * 3);
            heap.add(entry);
        }

        assertEquals(n, heap.size());
        OptOutPartition snapshot = heap.toPartition(false);
        assertEquals(n, snapshot.size());
        assertEquals(n, heap.size());

        for (int i = 0; i < snapshot.size(); ++i) {
            // System.out.format("hash %d\n", snapshot.get(i).idHashAsLong());
        }

        for (long i = 0; i < snapshot.size(); ++i) {
            byte[] hash = OptOutEntry.idHashFromLong(i);
            // System.out.format("test %d\n", i);
            assertTrue(snapshot.contains(hash));
            assertEquals(i * 3, snapshot.getOptOutTimestamp(hash));
        }

        for (int j = 0; j < 100; ++j) {
            OptOutEntry testEntry = OptOutEntry.newRandom();
            long val = testEntry.idHashAsLong();
            if (val < 0 || val >= n) continue;
            assertTrue(snapshot.contains(testEntry.identityHash));
            assertEquals(-1, snapshot.getOptOutTimestamp(testEntry.identityHash));
        }
    }
}
