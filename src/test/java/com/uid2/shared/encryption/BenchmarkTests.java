package com.uid2.shared.encryption;

import com.uid2.shared.model.EncryptedPayload;
import com.uid2.shared.model.KeysetKey;
import junit.framework.TestCase;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

public class BenchmarkTests extends TestCase {
    public void testBenchmarkCbc() throws Exception {
        if (System.getenv("SLOW_DEV_URANDOM") != null) {
            System.err.println("ignore this test since environment variable SLOW_DEV_URANDOM is set");
            return;
        }
        System.out.println("Java VM property java.security.egd: " + System.getProperty("java.security.egd"));
        final int runs = 1000000;
        final KeysetKey key = new KeysetKey(1, Random.getRandomKeyBytes(), Instant.now(), Instant.now(), Instant.now(), 123);

        final EncryptedPayload[] payloads = new EncryptedPayload[runs];

        final String[] inputs = new String[runs];
        for (int i = 0; i < runs; ++i) {
            final String input = "foo@bar.com" + i;
            inputs[i] = input;
            payloads[i] = AesCbc.encrypt(input, key);
        }

        long startBase = System.nanoTime();
        for (int i = 0; i < runs; ++i) {
            doSomething(payloads[0]);
        }
        long endBase = System.nanoTime();

        final SecretKey decryptionKey = new SecretKeySpec(key.getKeyBytes(), "AES");
        long startDecrypt = System.nanoTime();
        for (int i = 0; i < runs; ++i) {
            AesCbc.decrypt(payloads[0].getPayload(), decryptionKey);
        }
        long endDecrypt = System.nanoTime();

        long baseTime = endBase - startBase;
        long decryptTime = endDecrypt - startDecrypt;

        long overhead = (decryptTime - baseTime);
        double overheadPerEntry = overhead / (runs * 1.0);

        System.out.println("Number of Entries Tested = " + runs);
        System.out.println("Decryption Overhead per Entry (ms) = " + overheadPerEntry / (1000000 * 1.0));
    }

    public void testBenchmarkGcm() throws Exception {
        if (System.getenv("SLOW_DEV_URANDOM") != null) {
            System.err.println("ignore this test since environment variable SLOW_DEV_URANDOM is set");
            return;
        }
        System.out.println("Java VM property java.security.egd: " + System.getProperty("java.security.egd"));
        final int runs = 1000000;
        final KeysetKey key = new KeysetKey(1, Random.getRandomKeyBytes(), Instant.now(), Instant.now(), Instant.now(), 123);

        final EncryptedPayload[] payloads = new EncryptedPayload[runs];

        final String[] inputs = new String[runs];
        for (int i = 0; i < runs; ++i) {
            final String input = "foo@bar.com" + i;
            inputs[i] = input;
            payloads[i] = AesGcm.encrypt(input, key);
        }

        long startBase = System.nanoTime();
        for (int i = 0; i < runs; ++i) {
            doSomething(payloads[0]);
        }
        long endBase = System.nanoTime();

        final byte[] decryptionKey = key.getKeyBytes();
        long startDecrypt = System.nanoTime();
        for (int i = 0; i < runs; ++i) {
            AesGcm.decrypt(payloads[0].getPayload(), 0, decryptionKey);
        }
        long endDecrypt = System.nanoTime();

        long baseTime = endBase - startBase;
        long decryptTime = endDecrypt - startDecrypt;

        long overhead = (decryptTime - baseTime);
        double overheadPerEntry = overhead / (runs * 1.0);

        System.out.println("Number of Entries Tested = " + runs);
        System.out.println("Decryption Overhead per Entry (ms) = " + overheadPerEntry / (1000000 * 1.0));
    }
    private int count = 0;
    public void doSomething(EncryptedPayload loag) {
        count++;
    }
}
