package com.uid2.shared.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeysetTest {
    @Test
    public void testEquals() {
        Instant created = Instant.now();
        Keyset a = new Keyset(1, 5, "", Set.of(1,2,3), created, true, true);
        Keyset b = new Keyset(1, 5, "", Set.of(1,2,3), created, true, true);
        Keyset c = new Keyset(1, 5, "", null, created, true, true);

        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(c.equals(a));
        assertTrue(c.equals(c));
    }
}
