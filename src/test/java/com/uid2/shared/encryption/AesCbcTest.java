package com.uid2.shared.encryption;

import com.uid2.shared.model.EncryptedPayload;
import com.uid2.shared.model.EncryptionKey;
import junit.framework.TestCase;
import org.junit.Assert;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

public class AesCbcTest extends TestCase {

    public void testEncryption() throws Exception {

        final EncryptionKey key = new EncryptionKey(1, Random.getRandomKeyBytes(), Instant.now(), Instant.now(), Instant.now(), -1);
        final String testString = "foo@bar.comasdadsjahjhafjhjkfhakjhfkjshdkjfhaskdjfh";

        final EncryptedPayload payload = AesCbc.encrypt(testString, key);
        final byte[] decrypted = AesCbc.decrypt(payload.getPayload(), key);

        final String decryptedString = new String(decrypted, "UTF-8");
        Assert.assertEquals(testString, decryptedString);
    }

}
