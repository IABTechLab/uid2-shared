package com.uid2.shared.optout;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OptOutEntryTest {
    @Test
    public void parseLegacyEntry() {
        final byte[] idHash = OptOutUtils.hexToByteArray("101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f");
        final byte[] adsId = OptOutUtils.hexToByteArray("303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        final byte[] timestamp = OptOutUtils.hexToByteArray("5051525354555600");

        final byte[] record = new byte[OptOutConst.EntrySize];
        System.arraycopy(idHash, 0, record, 0, OptOutConst.Sha256Bytes);
        System.arraycopy(adsId, 0, record, OptOutConst.Sha256Bytes, OptOutConst.Sha256Bytes);
        System.arraycopy(timestamp, 0, record, OptOutConst.Sha256Bytes * 2, Long.BYTES);

        final OptOutEntry entry = OptOutEntry.parse(record, 0);
        Assert.assertArrayEquals(idHash, entry.identityHash);
        Assert.assertArrayEquals(adsId, entry.advertisingId);
        Assert.assertEquals(0x56555453525150l, entry.timestamp);
    }

    @Test
    public void parseEntry() {
        final byte[] idHash = OptOutUtils.hexToByteArray("101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f");
        final byte[] adsId = OptOutUtils.hexToByteArray("303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        final byte[] timestamp = OptOutUtils.hexToByteArray("50515253545556");
        final byte metadata = 0x54;

        final byte[] record = new byte[OptOutConst.EntrySize];
        System.arraycopy(idHash, 0, record, 0, OptOutConst.Sha256Bytes);
        System.arraycopy(adsId, 0, record, OptOutConst.Sha256Bytes, OptOutConst.Sha256Bytes);
        System.arraycopy(timestamp, 0, record, OptOutConst.Sha256Bytes * 2, Long.BYTES - 1);
        record[OptOutConst.EntrySize - 1] = metadata;

        final byte[] expectedAdsId = new byte[33];
        expectedAdsId[0] = 0x14;
        System.arraycopy(adsId, 0, expectedAdsId, 1, OptOutConst.Sha256Bytes);

        final OptOutEntry entry = OptOutEntry.parse(record, 0);
        Assert.assertArrayEquals(idHash, entry.identityHash);
        Assert.assertArrayEquals(expectedAdsId, entry.advertisingId);
        Assert.assertEquals(0x56555453525150l, entry.timestamp);
    }

    @Test
    public void parseEntryAtOffset() {
        final int offset = 12;
        final byte[] idHash = OptOutUtils.hexToByteArray("101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f");
        final byte[] adsId = OptOutUtils.hexToByteArray("303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        final byte[] timestamp = OptOutUtils.hexToByteArray("50515253545556");
        final byte metadata = 0x54;

        final byte[] records = new byte[offset + OptOutConst.EntrySize];
        System.arraycopy(idHash, 0, records, offset, OptOutConst.Sha256Bytes);
        System.arraycopy(adsId, 0, records, offset + OptOutConst.Sha256Bytes, OptOutConst.Sha256Bytes);
        System.arraycopy(timestamp, 0, records, offset + OptOutConst.Sha256Bytes * 2, Long.BYTES - 1);
        records[offset + OptOutConst.EntrySize - 1] = metadata;

        final byte[] expectedAdsId = new byte[33];
        expectedAdsId[0] = 0x14;
        System.arraycopy(adsId, 0, expectedAdsId, 1, OptOutConst.Sha256Bytes);

        final OptOutEntry entry = OptOutEntry.parse(records, offset);
        Assert.assertArrayEquals(idHash, entry.identityHash);
        Assert.assertArrayEquals(expectedAdsId, entry.advertisingId);
        Assert.assertEquals(0x56555453525150l, entry.timestamp);
    }

    @Test
    public void parseTimestampAtOffset() {
        final int offset = 12;
        final byte[] idHash = OptOutUtils.hexToByteArray("101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f");
        final byte[] adsId = OptOutUtils.hexToByteArray("303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        final byte[] timestamp = OptOutUtils.hexToByteArray("50515253545556");
        final byte metadata = 0x54;

        final byte[] records = new byte[offset + OptOutConst.EntrySize];
        System.arraycopy(idHash, 0, records, offset, OptOutConst.Sha256Bytes);
        System.arraycopy(adsId, 0, records, offset + OptOutConst.Sha256Bytes, OptOutConst.Sha256Bytes);
        System.arraycopy(timestamp, 0, records, offset + OptOutConst.Sha256Bytes * 2, Long.BYTES - 1);
        records[offset + OptOutConst.EntrySize - 1] = metadata;

        Assert.assertEquals(0x56555453525150l, OptOutEntry.parseTimestamp(records, offset));
    }

    @Test
    public void setTimestampAtOffset() {
        final int offset = 12;
        final byte[] idHash = OptOutUtils.hexToByteArray("101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f");
        final byte[] adsId = OptOutUtils.hexToByteArray("303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        final byte[] timestamp = OptOutUtils.hexToByteArray("50515253545556");
        final byte metadata = 0x54;

        final byte[] records = new byte[offset + OptOutConst.EntrySize];
        System.arraycopy(idHash, 0, records, offset, OptOutConst.Sha256Bytes);
        System.arraycopy(adsId, 0, records, offset + OptOutConst.Sha256Bytes, OptOutConst.Sha256Bytes);
        System.arraycopy(timestamp, 0, records, offset + OptOutConst.Sha256Bytes * 2, Long.BYTES - 1);
        records[offset + OptOutConst.EntrySize - 1] = metadata;

        final long newTimestamp = 0x60616263646566l;
        OptOutEntry.setTimestamp(records, offset, newTimestamp);

        final byte[] expectedAdsId = new byte[33];
        expectedAdsId[0] = 0x14;
        System.arraycopy(adsId, 0, expectedAdsId, 1, OptOutConst.Sha256Bytes);

        final OptOutEntry entry = OptOutEntry.parse(records, offset);
        Assert.assertArrayEquals(idHash, entry.identityHash);
        Assert.assertArrayEquals(expectedAdsId, entry.advertisingId);
        Assert.assertEquals(newTimestamp, entry.timestamp);
    }

    @Test
    public void copyToByteArrayLegacy()
    {
        final byte[] idHash = OptOutUtils.hexToByteArray("101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f");
        final byte[] adsId = OptOutUtils.hexToByteArray("303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        final long timestamp = 0x56555453525150l;

        final int offset = 12;
        final byte[] records = new byte[offset + OptOutConst.EntrySize];
        final OptOutEntry input = new OptOutEntry(idHash, adsId, timestamp);
        input.copyToByteArray(records, offset);

        final OptOutEntry entry = OptOutEntry.parse(records, 12);
        Assert.assertArrayEquals(idHash, entry.identityHash);
        Assert.assertArrayEquals(adsId, entry.advertisingId);
        Assert.assertEquals(0x56555453525150l, entry.timestamp);
    }

    @Test
    public void copyToByteArray()
    {
        final byte[] idHash = OptOutUtils.hexToByteArray("101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f");
        final byte[] adsId = OptOutUtils.hexToByteArray("04303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        final long timestamp = 0x56555453525150l;

        final int offset = 12;
        final byte[] records = new byte[offset + OptOutConst.EntrySize];
        final OptOutEntry input = new OptOutEntry(idHash, adsId, timestamp);
        input.copyToByteArray(records, offset);

        final OptOutEntry entry = OptOutEntry.parse(records, 12);
        Assert.assertArrayEquals(idHash, entry.identityHash);
        Assert.assertArrayEquals(adsId, entry.advertisingId);
        Assert.assertEquals(0x56555453525150l, entry.timestamp);
    }

    @Test
    public void writeToByteBuffer()
    {
        final byte[] idHash = OptOutUtils.hexToByteArray("101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f");
        final byte[] adsId = OptOutUtils.hexToByteArray("04303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
        final long timestamp = 0x56555453525150l;

        ByteBuffer buffer = ByteBuffer.allocate(OptOutConst.EntrySize);
        OptOutEntry.writeTo(buffer, idHash, adsId, timestamp);

        final OptOutEntry entry = OptOutEntry.parse(buffer.array(), 0);
        Assert.assertArrayEquals(idHash, entry.identityHash);
        Assert.assertArrayEquals(adsId, entry.advertisingId);
        Assert.assertEquals(0x56555453525150l, entry.timestamp);
    }
}
