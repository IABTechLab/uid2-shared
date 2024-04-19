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
        Site a = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.DSP)), new HashSet<>(Collections.singletonList("test.com")), new HashSet<>(Arrays.asList("123456789", "com.123.Game.app.Android")), true, 0L);
        Site b = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.DSP)), new HashSet<>(Collections.singletonList("test.com")), new HashSet<>(Arrays.asList("123456789", "com.123.Game.app.Android")), true, 0L);
        Site b2 = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.DSP)), new HashSet<>(Collections.singletonList("test.com")), new HashSet<>(Arrays.asList("com.123.Game.app.Android", "123456789")), true, 0L);
        Site c = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.DSP)), new HashSet<>(Collections.singletonList("test.com")), new HashSet<>(Arrays.asList("123456789", "com.123.Game.app.Android")), null, 0L);
        Site d = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.PUBLISHER)), new HashSet<>(Collections.singletonList("test.com")), new HashSet<>(Arrays.asList("123456789", "com.123.Game.app.Android")), true, 0L);
        Site e = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.DSP)), new HashSet<>(Collections.singletonList("test1.com")), new HashSet<>(Arrays.asList("123456789", "com.123.Game.app.Android")), true, 0L);
        Site f = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.DSP)), new HashSet<>(Collections.singletonList("test.com")), new HashSet<>(Arrays.asList("123456789", "com.123.Game.app.android")), true, 0L);

        assertTrue(a.equals(b));
        assertTrue(a.equals(b2));
        assertFalse(a.equals(c));
        assertFalse(c.equals(a));
        assertTrue(c.equals(c));

        assertFalse(a.equals(d));
        assertFalse(a.equals(e));
        assertFalse(a.equals(f));
    }

    @Test
    public void testToString() {
        Site a = new Site(1, "1 name", "1 desc", true, new HashSet<>(Collections.singletonList(ClientType.DSP)), new HashSet<>(Collections.singletonList("test.com")), new HashSet<>(Arrays.asList("123456789", "com.123.Game.app.Android")), true, 0L);
        String expected = "Site(id=1, name=1 name, description=1 desc, enabled=true, domainNames=[test.com], appNames=[123456789, com.123.Game.app.Android], clientTypes=[DSP], visible=true, created=0)";
        assertEquals(expected, a.toString());
    }
}