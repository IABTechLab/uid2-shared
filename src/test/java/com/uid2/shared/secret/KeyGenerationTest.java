package com.uid2.shared.secret;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class KeyGenerationTest {
    private static final IKeyGenerator GENERATOR = new SecureKeyGenerator();

    @Test
    void formattedKeyHasCorrectFormat() throws Exception {
        String key = GENERATOR.generateFormattedKeyString(32);
        assertEquals(45, key.length());
        assertEquals(".", key.substring(6, 7));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void formattedShortKeyHasCorrectFormat(Integer length) throws Exception {
        String key = GENERATOR.generateFormattedKeyString(length);
        assertEquals(4, key.length());
        assertFalse(key.contains("."));
    }

    @Test
    void formattedKeyFor4HasCorrectFormat() throws Exception {
        String key = GENERATOR.generateFormattedKeyString(4);
        assertEquals(9, key.length());
        assertEquals(".", key.substring(6, 7));
    }
}
