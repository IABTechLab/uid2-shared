package com.uid2.shared.store.salt;

import org.hashids.Hashids;

public class IdHashingScheme {
    private final String prefix;
    private final Hashids hasher;

    public IdHashingScheme(final String prefix, final String secret) {
        this.prefix = prefix;
        this.hasher = new Hashids(secret, 9);
    }

    public String encode(long id) {
        return prefix + this.hasher.encode(id);
    }
}
