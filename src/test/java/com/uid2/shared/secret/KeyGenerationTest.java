package com.uid2.shared.secret;

import com.uid2.shared.model.KeyGenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class KeyGenerationTest {
    private IKeyGenerator generator;

    @BeforeEach
    void setUp() {
        this.generator = new SecureKeyGenerator();
    }

    @Test
    void formattedKeyHasCorrectFormat() throws Exception {
        KeyGenerationResult kgr = this.generator.generateFormattedKeyStringAndKeyHash(32);
        assertAll("formattedKeyHasCorrectFormat",
                () -> assertEquals(45, kgr.getKey().length()),
                () -> assertEquals(".", kgr.getKey().substring(6, 7)),
                () -> assertEquals(getKeyHashLength(32), kgr.getKeyHash().length()));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void formattedShortKeyHasCorrectFormat(Integer length) throws Exception {
        KeyGenerationResult kgr = this.generator.generateFormattedKeyStringAndKeyHash(length);
        assertAll("formattedShortKeyHasCorrectFormat",
                () -> assertEquals(4, kgr.getKey().length()),
                () -> assertFalse(kgr.getKey().contains(".")),
                () -> assertEquals(getKeyHashLength(length), kgr.getKeyHash().length()));
    }

    @Test
    void formattedKeyFor4HasCorrectFormat() throws Exception {
        KeyGenerationResult kgr = this.generator.generateFormattedKeyStringAndKeyHash(4);
        assertAll("formattedKeyFor4HasCorrectFormat",
                () -> assertEquals(9, kgr.getKey().length()),
                () -> assertEquals(".", kgr.getKey().substring(6, 7)),
                () -> assertEquals(getKeyHashLength(4), kgr.getKeyHash().length()));
    }

    @Test
    void compare() {
        String inputKey = "UID2-C-L-999-ZGdWcr.FLCaY73+ELYYVhYQPcvMF+VVaPa38Zc0RTVFk=";
        String keyHash = "P7MIM/IqqkdIKFnm7T6dFSlL5DdZOAi11ll5/kVZk9SPc/CsLxziRRfklj7hEcOi99GOB/ynxZIgZP0Pwf7dYQ==$qJ+O3DQmu2elWU+WvvFJZtiPJVIcNd507gkgptSCo4A=";
        assertTrue(this.generator.compareFormattedKeyStringAndKeyHash(inputKey, keyHash));
    }

    @Test
    void compareBm() throws Exception {
        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            KeyGenerationResult kgr = this.generator.generateFormattedKeyStringAndKeyHash(32);
            String inputKey = "UID2-C-L-999-" + kgr.getKey();
            String keyHash = kgr.getKeyHash();
            assertTrue(this.generator.compareFormattedKeyStringAndKeyHash(inputKey, keyHash));
        }
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        System.out.println("Duration = " + duration);
    }

    private int getKeyHashLength(int keyLen) {
        // 88 = SHA-512 output length in base64 format
        // 1 = $ delimiter
        return 88 + 1 + getBase64Length(keyLen);
    }

    private int getBase64Length(int keyLen) {
        // Each base64 char is used to represent 6 bits (2^6 = 64)
        // 4 base64 chars are used to represent 24 bits, or 3 bytes
        // 4*(n/3) base64 chars are used to represent n bytes
        return (int) (4 * Math.ceil((double) keyLen / 3));
    }
}
