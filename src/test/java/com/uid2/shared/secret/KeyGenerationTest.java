package com.uid2.shared.secret;

import com.uid2.shared.model.KeyGenerationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class KeyGenerationTest {
    private static final IKeyGenerator GENERATOR = new SecureKeyGenerator();
    private static final String KEY_PREFIX = "UID2-C-L-999-";

    @Test
    void keyGenerationHasCorrectFormat() throws Exception {
        KeyGenerationResult kgr = GENERATOR.generateFormattedKeyStringAndKeyHash(KEY_PREFIX, 32);

        assertAll("keyGenerationHasCorrectFormat",
                () -> assertTrue(kgr.getKey().matches(KEY_PREFIX + ".{6}\\..{38}")),
                () -> assertTrue(kgr.getKeyHash().matches(KEY_PREFIX + ".{" + getKeyHashLength(32) + "}")));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void shortKeyGenerationHasCorrectFormat(Integer length) throws Exception {
        KeyGenerationResult kgr = GENERATOR.generateFormattedKeyStringAndKeyHash(KEY_PREFIX, length);

        assertAll("shortKeyGenerationHasCorrectFormat",
                () -> assertTrue(kgr.getKey().matches(KEY_PREFIX + ".{4}")),
                () -> assertTrue(kgr.getKeyHash().matches(KEY_PREFIX + ".{" + getKeyHashLength(length) + "}")));
    }

    @Test
    void keyGenerationFor4HasCorrectFormat() throws Exception {
        KeyGenerationResult kgr = GENERATOR.generateFormattedKeyStringAndKeyHash(KEY_PREFIX, 4);

        assertAll("keyGenerationFor4HasCorrectFormat",
                () -> assertTrue(kgr.getKey().matches(KEY_PREFIX + ".{6}\\..{2}")),
                () -> assertTrue(kgr.getKeyHash().matches(KEY_PREFIX + ".{" + getKeyHashLength(4) + "}")));
    }

    @Test
    void inputKeyAndKeyHashMatch() {
        String inputKey = "UID2-C-L-999-ZGdWcr.FLCaY73+ELYYVhYQPcvMF+VVaPa38Zc0RTVFk=";
        String keyHash = "UID2-C-L-999-P7MIM/IqqkdIKFnm7T6dFSlL5DdZOAi11ll5/kVZk9SPc/CsLxziRRfklj7hEcOi99GOB/ynxZIgZP0Pwf7dYQ==$qJ+O3DQmu2elWU+WvvFJZtiPJVIcNd507gkgptSCo4A=";
        assertTrue(GENERATOR.compareFormattedKeyStringAndKeyHash(inputKey, keyHash));
    }

    @Test
    void inputKeyPrefixAndKeyHashPrefixDoNotMatch() {
        String inputKey = "UID2-C-L-99-ZGdWcr.FLCaY73+ELYYVhYQPcvMF+VVaPa38Zc0RTVFk=";
        String keyHash = "UID2-C-L-999-P7MIM/IqqkdIKFnm7T6dFSlL5DdZOAi11ll5/kVZk9SPc/CsLxziRRfklj7hEcOi99GOB/ynxZIgZP0Pwf7dYQ==$qJ+O3DQmu2elWU+WvvFJZtiPJVIcNd507gkgptSCo4A=";
        assertFalse(GENERATOR.compareFormattedKeyStringAndKeyHash(inputKey, keyHash));
    }

    @Test
    void inputKeyAndKeyHashDoNotMatch() {
        String inputKey = "UID2-C-L-999-Zabcr.LCaY73ELYYVhYQPvMF+VaPa38Zc0RTVFk";
        String keyHash = "UID2-C-L-999-P7MIM/IqqkdIKFnm7T6dFSlL5DdZOAi11ll5/kVZk9SPc/CsLxziRRfklj7hEcOi99GOB/ynxZIgZP0Pwf7dYQ==$qJ+O3DQmu2elWU+WvvFJZtiPJVIcNd507gkgptSCo4A=";
        assertFalse(GENERATOR.compareFormattedKeyStringAndKeyHash(inputKey, keyHash));
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
