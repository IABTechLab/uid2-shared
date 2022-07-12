package com.uid2.shared.encryption;

import com.uid2.shared.model.EncryptedPayload;
import com.uid2.shared.model.EncryptionKey;
import junit.framework.TestCase;
import org.junit.Assert;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

public class AesCbcTest extends TestCase {

    public void testEncryptionDecryption() throws Exception {

        final EncryptionKey key = new EncryptionKey(1, Random.getRandomKeyBytes(), Instant.now(), Instant.now(), Instant.now(), -1);
        final String testString = "foo@bar.comasdadsjahjhafjhjkfhakjhfkjshdkjfhaskdjfh";

        final EncryptedPayload payload = AesCbc.encrypt(testString, key);
        final byte[] decrypted = AesCbc.decrypt(payload.getPayload(), key);

        final String decryptedString = new String(decrypted, "UTF-8");
        Assert.assertEquals(testString, decryptedString);
    }

    public void testDecryption() {
        byte[] keyBytes = new byte[]{-82, -66, -67, -114, 87, 24, -108, 82, -77, 112, 9, 80, 118, 39, 66, -35, 59, -81, -72, -81, 30, -41, 113, 101, -76, 79, 119, -73, 59, -39, 0, 75};
        byte[] encypted = new byte[]{34, -42, -99, 68, 110, 49, 45, 57, 11, 64, 74, -43, 86, -73, 33, -125, -100, 2, -27, 38, 103, 97, -17, -115, -116, 10, 102, -41, -35, -53, 34, 60, -44, 59, 101, 24, -14, 9, -56, -71, 86, -31, -44, -75, -124, -77, 58, -20, 3, 26, -39, 95, 100, 24, -110, 100, 34, 25, -4, 41, -93, -3, -83, -44, 91, -1, 34, 25, 83, -58, 42, -116, 51, -112, 91, 71, 8, -25, 26, 41};
        final EncryptionKey key = new EncryptionKey(1, keyBytes, Instant.now(), Instant.now(), Instant.now(), -1);
        byte[] payload = AesCbc.decrypt(encypted, key);
        String expected = "foo@bar.comasdadsjahjhafjhjkfhakjhfkjshdkjfhaskdjfh";
        String results = new String(payload);
        assertEquals(results, expected);
    }

}
