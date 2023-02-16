package com.uid2.shared.optout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

// OptOut entry format is:
// 32 bytes -- identity hash
// 32 bytes -- raw advertising id
// 7 bytes  -- timestamp (seconds); little endian
// 1 byte   -- metadata
// Total: 72 bytes
//
// Metadata format:
// Lower 5 bits  -- identity type
// Middle 1 bit  -- tombstone
// Higher 2 bits -- record version
// Total: 1 byte
//
// Record versions:
// 0 -- legacy, advertising id is 32 bytes
// 1 -- advertising id (v3) is 33 bytes (first byte is identity type followed by raw advertising id)
public final class OptOutEntry {
    public final byte[] identityHash;
    public final byte[] advertisingId;
    public final long timestamp;
    public final boolean isTombstone;

    private static final long timestampMask = 0xFFFFFFFFFFFFFFL;
    private static final byte adsIdTypeMask = 0x1F;
    private static final int adsIdVersionShift = 6;
    private static final byte tombstoneMask = 0x1;
    private static final int tombstoneShift = 5;

    @Deprecated
    public OptOutEntry(byte[] identityHash, byte[] advertisingId, long ts) {
        assert identityHash.length == OptOutConst.Sha256Bytes;
        assert advertisingId.length == OptOutConst.Sha256Bytes || advertisingId.length == OptOutConst.Sha256Bytes + 1;
        // assert ts >= 0;
        this.identityHash = identityHash;
        this.advertisingId = advertisingId;
        this.timestamp = ts;
        this.isTombstone = false;
    }

    public OptOutEntry(byte[] identityHash, byte[] advertisingId, long ts, boolean isTombstone) {
        assert identityHash.length == OptOutConst.Sha256Bytes;
        assert advertisingId.length == OptOutConst.Sha256Bytes || advertisingId.length == OptOutConst.Sha256Bytes + 1;
        // assert ts >= 0;
        this.identityHash = identityHash;
        this.advertisingId = advertisingId;
        this.timestamp = ts;
        this.isTombstone = isTombstone;
    }

    public static OptOutEntry parse(byte[] buffer, int bufferIndex) {
        assert bufferIndex + OptOutConst.EntrySize <= buffer.length;

        final byte metadata = buffer[bufferIndex + OptOutConst.EntrySize - 1];
        final byte adsIdVersion = (byte) (metadata >> adsIdVersionShift);
        final byte identityType = (byte) (metadata & adsIdTypeMask);
        final boolean isTombstone = (((byte) (metadata >> tombstoneShift)) & tombstoneMask) == 0x1;

        final byte[] idHash = Arrays.copyOfRange(buffer, bufferIndex, bufferIndex + OptOutConst.Sha256Bytes);
        bufferIndex += OptOutConst.Sha256Bytes;

        final byte[] adsId = adsIdVersion == 0
                ? Arrays.copyOfRange(buffer, bufferIndex, bufferIndex + OptOutConst.Sha256Bytes)
                : parseAdsIdV3(buffer, bufferIndex, identityType);
        bufferIndex += OptOutConst.Sha256Bytes;

        final long ts = ByteBuffer.wrap(buffer, bufferIndex, Long.BYTES).order(ByteOrder.LITTLE_ENDIAN).getLong() & timestampMask;

        return new OptOutEntry(idHash, adsId, ts, isTombstone);
    }

    private static byte[] parseAdsIdV3(byte[] buffer, int bufferIndex, byte identityType) {
        final byte[] adsId = new byte[OptOutConst.Sha256Bytes + 1];
        adsId[0] = identityType;
        System.arraycopy(buffer, bufferIndex, adsId, 1, OptOutConst.Sha256Bytes);
        return adsId;
    }

    public static long parseTimestamp(byte[] buffer, int bufferIndexForEntry) {
        assert bufferIndexForEntry + OptOutConst.EntrySize <= buffer.length;
        return ByteBuffer.wrap(buffer, bufferIndexForEntry + (OptOutConst.Sha256Bytes << 1), Long.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN).getLong() & timestampMask;
    }

    public static boolean parseTombstone(byte[] buffer, int bufferIndexForEntry) {
        assert bufferIndexForEntry + OptOutConst.EntrySize <= buffer.length;
        byte metadata = buffer[bufferIndexForEntry + (OptOutConst.Sha256Bytes << 1) + 7];
        return ((byte)(metadata >> tombstoneShift) & tombstoneMask) == (byte)0x1;
    }

    public static void setTimestamp(byte[] buffer, int bufferIndexForEntry, long timestamp) {
        assert bufferIndexForEntry + OptOutConst.EntrySize <= buffer.length;
        final byte metadata = buffer[bufferIndexForEntry + OptOutConst.EntrySize - 1];
        System.arraycopy(OptOutUtils.toByteArray(timestamp), 0, buffer, bufferIndexForEntry + (OptOutConst.Sha256Bytes << 1), Long.BYTES);
        buffer[bufferIndexForEntry + OptOutConst.EntrySize - 1] = metadata;
    }

    public static boolean isSpecialHash(byte[] hashBytes) {
        return Arrays.equals(hashBytes, OptOutUtils.nullHashBytes)
                || Arrays.equals(hashBytes, OptOutUtils.onesHashBytes);
    }

    // this method is for test
    public static byte[] idHashFromLong(long val) {
        return Arrays.copyOf(OptOutUtils.toByteArrayBE(val), OptOutConst.Sha256Bytes);
    }

    // this method is for test
    public static String idHashHexFromLong(long val) {
        return OptOutUtils.byteArrayToHex(OptOutEntry.idHashFromLong(val));
    }

    // this method is for test
    public static String idHashB64FromLong(long val) {
        return OptOutUtils.byteArrayToBase64String(OptOutEntry.idHashFromLong(val));
    }

    // this method is for test
    public static OptOutEntry newRandom() {
        byte[] bytes = new byte[OptOutConst.EntrySize];
        OptOutUtils.rand.nextBytes(bytes);
        bytes[OptOutConst.EntrySize - 1] = 0;
        return OptOutEntry.parse(bytes, 0);
    }

    // this method is for test
    public static OptOutEntry newTestEntry(long idHash, long timestamp) {
        // for test, using the same value for identity_hash and advertising_id
        byte[] id = idHashFromLong(idHash);
        return new OptOutEntry(id, id, timestamp, false);
    }

    // Overriding equals() to compare two OptOutEntry objects
    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) return true;

        OptOutEntry b = (OptOutEntry) o;

        // Compare the data members and return accordingly
        return Arrays.equals(this.identityHash, b.identityHash)
                && Arrays.equals(this.advertisingId, b.advertisingId)
                && this.timestamp == b.timestamp;
    }

    @Override
    public int hashCode() {
        return (int) (timestamp + Arrays.hashCode(identityHash) + Arrays.hashCode(advertisingId));
    }

    private static byte calcMetadata(byte[] advertisingId, boolean isTombstone) {
        return (byte) (
                (advertisingId.length == OptOutConst.Sha256Bytes ? 0 : ((1 << adsIdVersionShift) | advertisingId[0]))
                | (isTombstone ? (1 << tombstoneShift) : 0)
        );
    }

    public void copyToByteArray(byte[] bytes, int offset) {
        // copy identity hash
        System.arraycopy(this.identityHash, 0, bytes, offset, OptOutConst.Sha256Bytes);
        offset += OptOutConst.Sha256Bytes;

        final byte metadata = calcMetadata(this.advertisingId, isTombstone);

        // copy advertising id
        System.arraycopy(this.advertisingId, metadata == 0 ? 0 : 1, bytes, offset, OptOutConst.Sha256Bytes);
        offset += OptOutConst.Sha256Bytes;

        // copy timestamp
        System.arraycopy(OptOutUtils.toByteArray(this.timestamp), 0, bytes, offset, Long.BYTES);

        // set metadata
        bytes[offset + Long.BYTES - 1] = metadata;
    }

    @Deprecated
    public static void writeTo(ByteBuffer buffer, byte[] identityHash, byte[] advertisingId, long timestamp) {
        writeTo(buffer, identityHash, advertisingId, timestamp, false);
    }

    public static void writeTo(ByteBuffer buffer, byte[] identityHash, byte[] advertisingId, long timestamp, boolean isTombstone) {
        assert identityHash.length == OptOutConst.Sha256Bytes;
        assert advertisingId.length == OptOutConst.Sha256Bytes || advertisingId.length == OptOutConst.Sha256Bytes + 1;

        final byte metadata = calcMetadata(advertisingId, isTombstone);
        final byte[] timestampBytes = OptOutUtils.toByteArray(timestamp);
        timestampBytes[timestampBytes.length - 1] = metadata;

        buffer.put(identityHash);
        buffer.put(advertisingId, metadata == 0 ? 0 : 1, OptOutConst.Sha256Bytes);
        buffer.put(timestampBytes);
    }

    // this method is for test
    public long idHashAsLong() {
        return OptOutUtils.toLongBE(this.identityHash, 0);
    }

    // this method is for test
    public long advertisingIdAsLong() {
        return OptOutUtils.toLongBE(this.advertisingId, 0);
    }

    // this method is for test
    public String idHashToB64() {
        return OptOutUtils.byteArrayToBase64String(this.identityHash);
    }

    // this method is for test
    public String advertisingIdToB64() {
        return OptOutUtils.byteArrayToBase64String(this.advertisingId);
    }

    public boolean isSpecialHash() {
        return isSpecialHash(this.identityHash);
    }
}
