package com.uid2.shared.optout;

import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudUtils;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedList;

import static org.junit.Assert.*;

public class OptOutUtilsTest {
    @Test(expected = AssertionError.class)
    public void logTwo_zeroInput() {
        OptOutUtils.logTwo(0);
    }

    @Test(expected = AssertionError.class)
    public void logTwo_negativeInput() {
        OptOutUtils.logTwo(-1);
    }

    @Test(expected = AssertionError.class)
    public void assertFailure_failsTests() {
        assert false;
    }

    @Test
    public void assertSuccess_passesTests() {
        assert true;
        assert 1 == 1;
    }

    @Test
    public void lowTwo_positiveTests() {
        assertEquals(0, OptOutUtils.logTwo(1));
        assertEquals(1, OptOutUtils.logTwo(2));
        assertEquals(1, OptOutUtils.logTwo(3));
        assertEquals(2, OptOutUtils.logTwo(4));
        assertEquals(2, OptOutUtils.logTwo(5));
        assertEquals(2, OptOutUtils.logTwo(6));
        assertEquals(2, OptOutUtils.logTwo(7));
        assertEquals(3, OptOutUtils.logTwo(8));
        assertEquals(5, OptOutUtils.logTwo(63));
        assertEquals(6, OptOutUtils.logTwo(64));
    }

    @Test
    public void intToByteArray_tests() {
        byte[] testArray = OptOutUtils.toByteArray(1);
        assertArrayEquals(OptOutUtils.toByteArray(0), new byte[]{0, 0, 0, 0});
        assertArrayEquals(OptOutUtils.toByteArray(1), new byte[]{1, 0, 0, 0});
        assertArrayEquals(OptOutUtils.toByteArray(0x0102), new byte[]{2, 1, 0, 0});
        assertArrayEquals(OptOutUtils.toByteArray(0x010203), new byte[]{3, 2, 1, 0});
        assertArrayEquals(OptOutUtils.toByteArray(0x100), new byte[]{0, 1, 0, 0});
        assertArrayEquals(OptOutUtils.toByteArray(0x7f7f7f7f), new byte[]{0x7f, 0x7f, 0x7f, 0x7f});
    }

    @Test
    public void longToByteArray_tests() {
        long val = OptOutUtils.toLongBE(OptOutUtils.toByteArray(127L), 0);
        assertEquals(0x7f00000000000000L, val);
    }

    @Test
    public void byteArrayToInt_tests() {
        assertEquals(0, OptOutUtils.toInt(new byte[]{0, 0, 0, 0}, 0));
        assertEquals(1, OptOutUtils.toInt(new byte[]{1, 0, 0, 0}, 0));
        assertEquals(127, OptOutUtils.toInt(new byte[]{127, 0, 0, 0}, 0));
        assertEquals(0x0102, OptOutUtils.toInt(new byte[]{0x2, 0x1, 0, 0}, 0));
        assertEquals(0x010203, OptOutUtils.toInt(new byte[]{0x3, 0x2, 0x1, 0}, 0));
        assertEquals(0x7f7f7f7f, OptOutUtils.toInt(new byte[]{0x7f, 0x7f, 0x7f, 0x7f}, 0));
    }

    @Test
    public void compareBytes_tests() {
        for (int i = 0; i < 1000; ++i) {
            long a = OptOutUtils.rand.nextLong();
            long b = OptOutUtils.rand.nextLong();
            int cmp1 = Long.compareUnsigned(a, b);
            // System.out.format("a vs b: %x vs %x (cmp %d)\n", a, b, cmp1);
            byte[] x = OptOutUtils.toByteArray(a);
            byte[] y = OptOutUtils.toByteArray(b);
            int cmp2 = OptOutUtils.compareBytes(x, y);
            if (cmp1 == 0) {
                assertTrue(cmp2 == 0);
            } else if (cmp1 > 0) {
                // little endian doesn't guarantee byte array comparison yields the same consistent result
                // assertTrue(cmp2 > 0);
            } else /* cmp1 < 0 */ {
                // little endian doesn't guarantee byte array comparison yields the same consistent result
                // assertTrue(cmp2 < 0);
            }
        }
    }

    @Test
    public void compareBytesBE_tests() {
        for (int i = 0; i < 1000; ++i) {
            long a = OptOutUtils.rand.nextLong();
            long b = OptOutUtils.rand.nextLong();
            int cmp1 = Long.compareUnsigned(a, b);
            // System.out.format("a vs b: %x vs %x (cmp %d)\n", a, b, cmp1);
            byte[] x = OptOutUtils.toByteArrayBE(a);
            byte[] y = OptOutUtils.toByteArrayBE(b);
            int cmp2 = OptOutUtils.compareBytes(x, y);
            if (cmp1 == 0) {
                assertTrue(cmp2 == 0);
            } else if (cmp1 > 0) {
                assertTrue(cmp2 > 0);
            } else /* cmp1 < 0 */ {
                assertTrue(cmp2 < 0);
            }
        }
    }

    @Test
    public void nullHash_tests() {
        assertTrue(OptOutUtils.isValidSha256Hex(OptOutUtils.nullHash));
        for (byte b : OptOutUtils.hexToByteArray(OptOutUtils.nullHash)) {
            assertEquals((byte) 0, b);
        }

        assertEquals(OptOutUtils.nullHash, OptOutUtils.byteArrayToHex(OptOutUtils.nullHashBytes));
    }

    @Test
    public void ffffHash_tests() {
        assertTrue(OptOutUtils.isValidSha256Hex(OptOutUtils.onesHash));
        for (byte b : OptOutUtils.hexToByteArray(OptOutUtils.onesHash)) {
            assertEquals((byte) 0xff, b);
        }

        assertEquals(OptOutUtils.onesHash, OptOutUtils.byteArrayToHex(OptOutUtils.onesHashBytes));
    }

    @Test
    public void replicaIdFromHostName_tests() {
        assertEquals(0, OptOutUtils.getReplicaIdFromHostName("uid2-optout-0.uid2-optout.uid.svc.cluster.local"));
        assertEquals(1, OptOutUtils.getReplicaIdFromHostName("uid2-optout-1.uid2-optout.uid.svc.cluster.local"));
        assertEquals(0, OptOutUtils.getReplicaIdFromHostName("uid2-optout-0"));
        assertEquals(1, OptOutUtils.getReplicaIdFromHostName("uid2-optout-1"));
    }

    @Test
    public void normalizeS3Folder_tests() {
        assertEquals("a/", CloudUtils.normalizDirPath("a"));
        assertEquals("a/", CloudUtils.normalizDirPath("a/"));
        assertEquals("b/a/", CloudUtils.normalizDirPath("b/a"));
        assertEquals("b/a/", CloudUtils.normalizDirPath("b/a/"));
    }

    @Test
    public void getDateStr_tests() {
        assertEquals("2020-12-06", OptOutUtils.getDateStr(Instant.parse("2020-12-06T02:49:39.606119Z")));
        assertEquals("2021-01-15", OptOutUtils.getDateStr(Instant.parse("2021-01-15T23:59:59.999999Z")));
        assertEquals("2021-01-15", OptOutUtils.getDateStr(Instant.parse("2021-01-15T00:00:00.000000Z")));
    }

    @Test
    public void secondsBeforeNextSlot_tests() {
        assertEquals(0, OptOutUtils.getSecondsBeforeNextSlot(Instant.parse("2021-02-09T00:00:00.000000Z"), 300));
        assertEquals(0, OptOutUtils.getSecondsBeforeNextSlot(Instant.parse("2021-02-09T23:55:00.000000Z"), 300));
        assertEquals(0, OptOutUtils.getSecondsBeforeNextSlot(Instant.parse("2021-02-09T00:05:00.000000Z"), 300));
        assertEquals(299, OptOutUtils.getSecondsBeforeNextSlot(Instant.parse("2021-02-09T00:05:01.000000Z"), 300));
        assertEquals(1, OptOutUtils.getSecondsBeforeNextSlot(Instant.parse("2021-02-09T00:09:59.000000Z"), 300));
    }

    @Test
    public void instantFloorByInterval_tests() {
        assertEquals(
            Instant.parse("2021-02-09T00:00:00.000000Z"),
            OptOutUtils.instantFloorByInterval(Instant.parse("2021-02-09T00:00:00.322Z"), 0));

        assertEquals(
            Instant.parse("2021-02-09T00:05:00.000000Z"),
            OptOutUtils.instantFloorByInterval(Instant.parse("2021-02-09T00:05:59.456Z"), 300));

        assertEquals(
            Instant.parse("2021-02-09T09:15:00.000000Z"),
            OptOutUtils.instantFloorByInterval(Instant.parse("2021-02-09T09:25:09.999Z"), 900));

        assertEquals(
            Instant.parse("2021-02-09T00:00:00.000000Z"),
            OptOutUtils.instantFloorByInterval(Instant.parse("2021-02-09T09:25:09.999Z"), 86400 / 2));

        assertEquals(
            Instant.parse("2021-02-09T12:00:00.000000Z"),
            OptOutUtils.instantFloorByInterval(Instant.parse("2021-02-09T19:25:09.999Z"), 86400 / 2));
    }

    @Test
    public void addSorted_tests() {
        LinkedList<String> l1 = new LinkedList<>();
        String[] items = OptOutUtils.toArray(
            "optout/log/optout-log-000_2021-02-09T00.00.00Z_0000.dat",
            "optout/log/optout-log-000_2021-02-09T02.00.00Z_0000.dat",
            "optout/log/optout-log-000_2021-02-09T01.00.00Z_0000.dat",
            "optout/log/optout-log-000_2021-02-08T23.00.00Z_0000.dat"
        );

        OptOutUtils.addSorted(l1, items[0], OptOutUtils.DeltaFilenameComparator);
        assertEquals(1, l1.size());
        assertEquals(items[0], l1.get(0));

        OptOutUtils.addSorted(l1, items[1], OptOutUtils.DeltaFilenameComparator);
        assertEquals(2, l1.size());
        assertEquals(items[0], l1.get(0));
        assertEquals(items[1], l1.get(1));

        OptOutUtils.addSorted(l1, items[2], OptOutUtils.DeltaFilenameComparator);
        assertEquals(3, l1.size());
        assertEquals(items[0], l1.get(0));
        assertEquals(items[2], l1.get(1));
        assertEquals(items[1], l1.get(2));

        OptOutUtils.addSorted(l1, items[3], OptOutUtils.DeltaFilenameComparator);
        assertEquals(4, l1.size());
        assertEquals(items[3], l1.get(0));
        assertEquals(items[0], l1.get(1));
        assertEquals(items[2], l1.get(2));
        assertEquals(items[1], l1.get(3));
    }

    @Test
    public void getReplicaId_tests() {
        Path path1 = Paths.get("/optout/test/optout-partition-999_2021-04-21T00.00.00Z_014e024f.dat");
        assertEquals(999, OptOutUtils.getFileReplicaId(path1));

        Path path2 = Paths.get("optout/test/optout-partition-999_2021-04-21T00.00.00Z_014e024f.dat");
        assertEquals(999, OptOutUtils.getFileReplicaId(path2));

        Path path3 = Paths.get("optout-partition-999_2021-04-21T00.00.00Z_014e024f.dat");
        assertEquals(999, OptOutUtils.getFileReplicaId(path3));
    }

    @Test
    public void getConsumerDirs_tests() {
        JsonObject config1 = new JsonObject();
        config1.put(Const.Config.OptOutDataDirProp, "test");
        assertEquals("test/consumer/delta", OptOutUtils.getDeltaConsumerDir(config1));
        assertEquals("test/consumer/partition", OptOutUtils.getPartitionConsumerDir(config1));

        JsonObject config2 = new JsonObject();
        config1.put(Const.Config.OptOutDataDirProp, "/test");
        assertEquals("/test/consumer/delta", OptOutUtils.getDeltaConsumerDir(config1));
        assertEquals("/test/consumer/partition", OptOutUtils.getPartitionConsumerDir(config1));

        JsonObject config3 = new JsonObject();
        config1.put(Const.Config.OptOutDataDirProp, "test/");
        assertEquals("test/consumer/delta", OptOutUtils.getDeltaConsumerDir(config1));
        assertEquals("test/consumer/partition", OptOutUtils.getPartitionConsumerDir(config1));

        JsonObject config4 = new JsonObject();
        config1.put(Const.Config.OptOutDataDirProp, "/test/");
        assertEquals("/test/consumer/delta", OptOutUtils.getDeltaConsumerDir(config1));
        assertEquals("/test/consumer/partition", OptOutUtils.getPartitionConsumerDir(config1));
    }
}
