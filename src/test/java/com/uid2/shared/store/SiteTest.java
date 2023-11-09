package com.uid2.shared.store;

import com.uid2.shared.model.Site;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SiteTest {
    @Test
    public void testEquals() {
        Site a = new Site(1, "1 name", "1 desc", true, new HashSet<>(), new HashSet<>(), true, 0L);
        Site b = new Site(1, "1 name", "1 desc", true, new HashSet<>(), new HashSet<>(), true, 0L);
        Site c = new Site(1, "1 name", "1 desc", true, new HashSet<>(), new HashSet<>(), null, 0L);

        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(c.equals(a));
        assertTrue(c.equals(c));
    }
}