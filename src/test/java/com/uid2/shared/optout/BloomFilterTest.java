package com.uid2.shared.optout;


import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class BloomFilterTest {
    Random rand = new Random();

    @Test
    public void createZeroSize_expectFail() {
        assertThrows(AssertionError.class, () -> {
            BloomFilter bf = new BloomFilter(0);
        });
    }

    @Test
    public void createNegativeSize_expectFail() {
        assertThrows(AssertionError.class, () -> {
            BloomFilter bf = new BloomFilter(-1);
        });
    }

    @Test
    public void createOne_expectNotContainForEmpty() {
        BloomFilter bf = new BloomFilter(1);
        for (int i = 0; i < 100; ++i) {
            byte[] bytes = OptOutUtils.toByteArray(this.rand.nextInt());
            assertFalse(bf.likelyContains(bytes));
        }
    }

    @Test
    public void createOneAddZero_verifyResults() {
        BloomFilter bf = new BloomFilter(1);
        bf.add(new byte[]{0x0});

        assertFalse(bf.likelyContains(new byte[]{0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x2}));
        assertFalse(bf.likelyContains(new byte[]{0x3}));
        assertFalse(bf.likelyContains(new byte[]{0x7f}));

        assertFalse(bf.likelyContains(new byte[]{0x1, 0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x2, 0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x3, 0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x7f, 0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x7f, 0x1, 0x1}));

        assertTrue(bf.likelyContains(new byte[]{0x0, 0x02}));
        assertTrue(bf.likelyContains(new byte[]{0x0, 0x03}));
        assertTrue(bf.likelyContains(new byte[]{0x0, 0x7f}));
        assertTrue(bf.likelyContains(new byte[]{0x0, 0x1, 0x1}));
        assertTrue(bf.likelyContains(new byte[]{0x0, 0x2, 0x2}));
    }

    @Test
    public void createOneAddOne_verifyResults() {
        BloomFilter bf = new BloomFilter(1);
        bf.add(new byte[]{0x1});

        assertFalse(bf.likelyContains(new byte[]{0x0}));
        assertFalse(bf.likelyContains(new byte[]{0x2}));
        assertFalse(bf.likelyContains(new byte[]{0x3}));
        assertFalse(bf.likelyContains(new byte[]{0x7f}));

        assertFalse(bf.likelyContains(new byte[]{0x0, 0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x2, 0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x3, 0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x7f, 0x1}));
        assertFalse(bf.likelyContains(new byte[]{0x7f, 0x1, 0x1}));

        assertTrue(bf.likelyContains(new byte[]{0x1, 0x02}));
        assertTrue(bf.likelyContains(new byte[]{0x1, 0x03}));
        assertTrue(bf.likelyContains(new byte[]{0x1, 0x7f}));
        assertTrue(bf.likelyContains(new byte[]{0x1, 0x1, 0x1}));
        assertTrue(bf.likelyContains(new byte[]{0x1, 0x2, 0x2}));
    }

    @Test
    public void addManyTimes_verifyReturnValue() {
        BloomFilter bf = new BloomFilter(100);

        assertTrue(bf.add(new byte[]{0x1}));
        assertFalse(bf.add(new byte[]{0x1}));
        assertFalse(bf.add(new byte[]{0x1}));
        assertEquals(1, bf.size());
    }

    @Test
    public void create16_verifyResults() {
        createBloomFilter_verifyResults(16);
    }

    @Test
    public void create256_verifyResults() {
        createBloomFilter_verifyResults(256);
    }

    @Test
    public void create2048_verifyResults() {
        createBloomFilter_verifyResults(2048);
    }

    private void createBloomFilter_verifyResults(int bfSize) {
        BloomFilter bf = new BloomFilter(bfSize);
        HashSet<Integer> actualSet = new HashSet<Integer>();

        // add lots of elements (bloomfilter 50% loaded)
        for (int i = 0; i < bfSize / 2; ++i) {
            int val = this.rand.nextInt(bfSize);
            actualSet.add(val);
            bf.add(OptOutUtils.toByteArray(val));
        }

        // as long as the entropy is below the bfsize, bloomfilter should be 100% correct
        for (int i = 0; i < bfSize; ++i) {
            int val = i;
            byte[] bytes = OptOutUtils.toByteArray(val);
            // System.out.format("hello %d\n", val);
            assertEquals(actualSet.contains(val), bf.likelyContains(bytes));
        }

        // if there is a mismatch (val > bfSize), it is because the lowest bits match
        for (int i = 0; i < bfSize; ++i) {
            int val = (int) (this.rand.nextLong() + bfSize);
            byte[] bytes = OptOutUtils.toByteArray(val);
            int valLowestBits = (int) (val & bf.bfMask());
            if (bf.likelyContains(bytes)) {
                assertTrue(actualSet.contains(valLowestBits));
            }
        }
    }
}
