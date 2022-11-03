package com.uid2.shared.optout;

public class OptOutConst {
    // SHA-256 requires 32 bytes, each bytes requires 2 characters in hex representation
    public static final int Sha256Bits = 256;
    public static final int Sha256Bytes = Sha256Bits / 8;
    public static final int Sha256Characters = Sha256Bytes * 2;

    // Total OptOut entry size in file: identity_hash (sha256) + advertising_id (sha256) + long (timestamp)
    public static final int EntrySize = OptOutConst.Sha256Bytes + OptOutConst.Sha256Bytes + Long.BYTES;
}
