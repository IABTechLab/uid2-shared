package com.uid2.shared.store;

import com.uid2.shared.model.ClientType;
import com.uid2.shared.model.Site;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class SiteTest {
    @Test
    public void testEquals() {
        Site a = new Site(1, "1 name", "1 desc", true, new HashSet<>(), new HashSet<>(), new HashSet<>(), true, 0L);
        Site b = new Site(1, "1 name", "1 desc", true, new HashSet<>(), new HashSet<>(), new HashSet<>(), true, 0L);
        Site c = new Site(1, "1 name", "1 desc", true, new HashSet<>(), new HashSet<>(), new HashSet<>(), null, 0L);

        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(c.equals(a));
        assertTrue(c.equals(c));
    }

    @Test
    public void testToString() {
        Site a = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.DSP)), new HashSet<>(Collections.singletonList("test.com")), new HashSet<>(Arrays.asList("123456789", "com.123.game.app.android")), true, 0L);
        String expected = "Site(id=1, name=1 name, description=1 desc, enabled=true, domainNames=[test.com], appNames=[123456789, com.123.game.app.android], clientTypes=[DSP], visible=true, created=0)";
        assertEquals(expected, a.toString());
    }
}