package com.uid2.shared.store;

import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.encryption.AesGcm;
import com.uid2.shared.model.CloudEncryptionKey;
import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import com.uid2.shared.store.scope.EncryptedScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EncryptedRotatingSaltProviderTest {
    @Mock
    private ICloudStorage cloudStorage;

    @Mock
    private RotatingCloudEncryptionKeyProvider keyProvider;
    private CloudEncryptionKey encryptionKey;

    @BeforeEach
    public void setup() {
        byte[] keyBytes = new byte[32];
        new Random().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        encryptionKey = new CloudEncryptionKey(1, 1, 0, 0, base64Key);

        Map<Integer, CloudEncryptionKey> mockKeyMap = new HashMap<>();
        mockKeyMap.put(encryptionKey.getId(), encryptionKey);
        when(keyProvider.getAll()).thenReturn(mockKeyMap);
        when(keyProvider.getKey(1)).thenReturn(mockKeyMap.get(1));
    }

    private InputStream getEncryptedStream(String content) {
        String secretKey = encryptionKey.getSecret();
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        byte[] encryptedPayload = AesGcm.encrypt(content.getBytes(StandardCharsets.UTF_8), secretKeyBytes);
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

        JsonObject encryptedJson = new JsonObject()
                .put("key_id", encryptionKey.getId())
                .put("encrypted_payload", encryptedPayloadBase64);

        String encryptedContent = encryptedJson.encodePrettily();
        return new ByteArrayInputStream(encryptedContent.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void metadataPath() {
        EncryptedRotatingSaltProvider saltsProvider = new EncryptedRotatingSaltProvider(
                cloudStorage, keyProvider, new EncryptedScope(new CloudPath("salts/metadata.json"), 1, true));

        assertEquals("salts/encrypted/1_public/metadata.json", saltsProvider.getMetadataPath());
    }

    @Test
    public void loadSaltSingleVersion() throws Exception {
        final String FIRST_LEVEL_SALT = "first_level_salt_value";
        final String ID_PREFIX = "a";
        final String ID_SECRET = "m3yMIcbg9vCaFLJsn4m4PfruZnvAZ72OxmFG5QsGMOw=";

        final Instant generatedTime = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant expireTime = Instant.now().plus(365, ChronoUnit.DAYS);

        final JsonObject metadataJson = new JsonObject();
        {
            metadataJson.put("version", 2);
            metadataJson.put("generated", generatedTime.getEpochSecond() * 1000L);
            metadataJson.put("first_level", FIRST_LEVEL_SALT);
            metadataJson.put("id_prefix", ID_PREFIX);
            metadataJson.put("id_secret", ID_SECRET);
            final JsonArray saltsRefList = new JsonArray();
            {
                final JsonObject saltsRef = new JsonObject();
                saltsRef.put("effective", generatedTime.getEpochSecond() * 1000L);
                saltsRef.put("expires", expireTime.getEpochSecond() * 1000L);
                saltsRef.put("location", "salts.txt");
                saltsRef.put("size", 8);
                saltsRefList.add(saltsRef);
            }
            metadataJson.put("salts", saltsRefList);
        }

        final String effectiveTimeString = String.valueOf(generatedTime.getEpochSecond() * 1000L);
        final String salts =
                "1000000," + effectiveTimeString + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=\n" +
                "1000001," + effectiveTimeString + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=\n" +
                "1000002," + effectiveTimeString + ",+a5LPajo7uPfNcc9HH0Tn25b3RnSNZwe8YaAKcyeHaA=\n" +
                "1000003," + effectiveTimeString + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=\n" +
                "1000004," + effectiveTimeString + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=\n" +
                "1000005," + effectiveTimeString + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=\n" +
                "1000006," + effectiveTimeString + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=\n" +
                "1000007," + effectiveTimeString + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=";

        when(cloudStorage.download("sites/encrypted/1_public/metadata.json"))
                .thenReturn(new ByteArrayInputStream(metadataJson.toString().getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("salts.txt"))
                .thenReturn(getEncryptedStream(salts));

        EncryptedRotatingSaltProvider saltsProvider = new EncryptedRotatingSaltProvider(
                cloudStorage, keyProvider, new EncryptedScope(new CloudPath("sites/metadata.json"), 1, true));

        final JsonObject loadedMetadata = saltsProvider.getMetadata();
        saltsProvider.loadContent(loadedMetadata);
        assertEquals(2, saltsProvider.getVersion(loadedMetadata));

        final ISaltProvider.ISaltSnapshot snapshot = saltsProvider.getSnapshot(Instant.now());
        assertEquals(FIRST_LEVEL_SALT, snapshot.getFirstLevelSalt());
        assertTrue(snapshot.getModifiedSince(Instant.now().minus(1, ChronoUnit.HOURS)).isEmpty());
    }

    @Test
    public void loadSaltSingleVersion1mil() throws Exception {
        final String FIRST_LEVEL_SALT = "first_level_salt_value";
        final String ID_PREFIX = "a";
        final String ID_SECRET = "m3yMIcbg9vCaFLJsn4m4PfruZnvAZ72OxmFG5QsGMOw=";

        final Instant generatedTime = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant expireTime = Instant.now().plus(365, ChronoUnit.DAYS);

        final JsonObject metadataJson = new JsonObject();
        {
            metadataJson.put("version", 2);
            metadataJson.put("generated", generatedTime.getEpochSecond() * 1000L);
            metadataJson.put("first_level", FIRST_LEVEL_SALT);
            metadataJson.put("id_prefix", ID_PREFIX);
            metadataJson.put("id_secret", ID_SECRET);
            final JsonArray saltsRefList = new JsonArray();
            {
                final JsonObject saltsRef = new JsonObject();
                saltsRef.put("effective", generatedTime.getEpochSecond() * 1000L);
                saltsRef.put("expires", expireTime.getEpochSecond() * 1000L);
                saltsRef.put("location", "salts.txt");
                saltsRef.put("size", 1000000);
                saltsRefList.add(saltsRef);
            }
            metadataJson.put("salts", saltsRefList);
        }

        final String effectiveTimeString = String.valueOf(generatedTime.getEpochSecond() * 1000L);
        StringBuilder salts = new StringBuilder();
        for (int i = 0; i < 1000000; i++) {
            salts.append(i).append(",").append(effectiveTimeString).append(",").append("salt-string").append("\n");
        }

        when(cloudStorage.download("sites/encrypted/1_public/metadata.json"))
                .thenReturn(new ByteArrayInputStream(metadataJson.toString().getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("salts.txt"))
                .thenReturn(getEncryptedStream(salts.toString()));

        EncryptedRotatingSaltProvider saltsProvider = new EncryptedRotatingSaltProvider(
                cloudStorage, keyProvider, new EncryptedScope(new CloudPath("sites/metadata.json"), 1, true));

        final JsonObject loadedMetadata = saltsProvider.getMetadata();
        saltsProvider.loadContent(loadedMetadata);
        assertEquals(2, saltsProvider.getVersion(loadedMetadata));

        final ISaltProvider.ISaltSnapshot snapshot = saltsProvider.getSnapshot(Instant.now());
        assertEquals(FIRST_LEVEL_SALT, snapshot.getFirstLevelSalt());
        assertTrue(snapshot.getModifiedSince(Instant.now().minus(1, ChronoUnit.HOURS)).isEmpty());
    }

    @Test
    public void loadSaltMultipleVersions() throws Exception {
        final String FIRST_LEVEL_SALT = "first_level_salt_value";
        final String ID_PREFIX = "a";
        final String ID_SECRET = "m3yMIcbg9vCaFLJsn4m4PfruZnvAZ72OxmFG5QsGMOw=";

        final Instant generatedTimeV1 = Instant.now().minus(2, ChronoUnit.DAYS);
        final Instant expireTimeV1 = Instant.now().plus(365, ChronoUnit.DAYS);
        final Instant generatedTimeV2 = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant expireTimeV2 = Instant.now().plus(366, ChronoUnit.DAYS);

        final JsonObject metadataJson = new JsonObject();
        {
            metadataJson.put("version", 2);
            metadataJson.put("generated", generatedTimeV1.getEpochSecond() * 1000L);
            metadataJson.put("first_level", FIRST_LEVEL_SALT);
            metadataJson.put("id_prefix", ID_PREFIX);
            metadataJson.put("id_secret", ID_SECRET);
            final JsonArray saltsRefList = new JsonArray();
            {
                final JsonObject saltsRef = new JsonObject();
                saltsRef.put("effective", generatedTimeV1.getEpochSecond() * 1000L);
                saltsRef.put("expires", expireTimeV1.getEpochSecond() * 1000L);
                saltsRef.put("location", "saltsV1.txt");
                saltsRef.put("size", 8);
                saltsRefList.add(saltsRef);
            }
            {
                final JsonObject saltsRef = new JsonObject();
                saltsRef.put("effective", generatedTimeV2.getEpochSecond() * 1000L);
                saltsRef.put("expires", expireTimeV2.getEpochSecond() * 1000L);
                saltsRef.put("location", "saltsV2.txt");
                saltsRef.put("size", 8);
                saltsRefList.add(saltsRef);
            }
            metadataJson.put("salts", saltsRefList);
        }

        final String effectiveTimeStringV1 = String.valueOf(generatedTimeV1.getEpochSecond() * 1000L);
        final String saltsV1 =
                "1000000," + effectiveTimeStringV1 + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=\n" +
                "1000001," + effectiveTimeStringV1 + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=\n" +
                "1000002," + effectiveTimeStringV1 + ",+a5LPajo7uPfNcc9HH0Tn25b3RnSNZwe8YaAKcyeHaA=\n" +
                "1000003," + effectiveTimeStringV1 + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=\n" +
                "1000004," + effectiveTimeStringV1 + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=\n" +
                "1000005," + effectiveTimeStringV1 + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=\n" +
                "1000006," + effectiveTimeStringV1 + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=\n" +
                "1000007," + effectiveTimeStringV1 + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=";

        // update key 1000002
        final String effectiveTimeStringV2 = String.valueOf(generatedTimeV2.getEpochSecond() * 1000L);
        final String saltsV2 =
                "1000000," + effectiveTimeStringV1 + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=\n" +
                "1000001," + effectiveTimeStringV1 + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=\n" +
                "1000002," + effectiveTimeStringV2 + ",AP73KwZscb1ltQQH/B7fdbHUnMmbJNlRULxzklXUqaA=\n" +
                "1000003," + effectiveTimeStringV1 + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=\n" +
                "1000004," + effectiveTimeStringV1 + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=\n" +
                "1000005," + effectiveTimeStringV1 + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=\n" +
                "1000006," + effectiveTimeStringV1 + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=\n" +
                "1000007," + effectiveTimeStringV1 + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=";

        when(cloudStorage.download("sites/encrypted/1_public/metadata.json"))
                .thenReturn(new ByteArrayInputStream(metadataJson.toString().getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("saltsV1.txt"))
                .thenReturn(getEncryptedStream(saltsV1));
        when(cloudStorage.download("saltsV2.txt"))
                .thenReturn(getEncryptedStream(saltsV2));

        EncryptedRotatingSaltProvider saltsProvider = new EncryptedRotatingSaltProvider(
                cloudStorage, keyProvider, new EncryptedScope(new CloudPath("sites/metadata.json"), 1, true));

        final JsonObject loadedMetadata = saltsProvider.getMetadata();
        saltsProvider.loadContent(loadedMetadata);
        assertEquals(2, saltsProvider.getVersion(loadedMetadata));

        final ISaltProvider.ISaltSnapshot snapshot = saltsProvider.getSnapshot(Instant.now());
        assertEquals(FIRST_LEVEL_SALT, snapshot.getFirstLevelSalt());
        assertTrue(snapshot.getModifiedSince(Instant.now().minus(1, ChronoUnit.HOURS)).isEmpty());
        assertEquals(1, snapshot.getModifiedSince(Instant.now().minus(30, ChronoUnit.HOURS)).size());
        assertEquals(1000002, snapshot.getModifiedSince(Instant.now().minus(30, ChronoUnit.HOURS)).get(0).getId());
    }

    @Test
    public void loadSaltMultipleVersionsExpired() throws Exception {
        final String FIRST_LEVEL_SALT = "first_level_salt_value";
        final String ID_PREFIX = "a";
        final String ID_SECRET = "m3yMIcbg9vCaFLJsn4m4PfruZnvAZ72OxmFG5QsGMOw=";

        final Instant generatedTimeV1 = Instant.now().minus(3, ChronoUnit.DAYS);
        final Instant expireTimeV1 = Instant.now().minus(2, ChronoUnit.DAYS);
        final Instant generatedTimeV2 = Instant.now().minus(2, ChronoUnit.DAYS);
        final Instant expireTimeV2 = Instant.now().minus(1, ChronoUnit.DAYS);

        final JsonObject metadataJson = new JsonObject();
        {
            metadataJson.put("version", 2);
            metadataJson.put("generated", generatedTimeV1.getEpochSecond() * 1000L);
            metadataJson.put("first_level", FIRST_LEVEL_SALT);
            metadataJson.put("id_prefix", ID_PREFIX);
            metadataJson.put("id_secret", ID_SECRET);
            final JsonArray saltsRefList = new JsonArray();
            {
                final JsonObject saltsRef = new JsonObject();
                saltsRef.put("effective", generatedTimeV1.getEpochSecond() * 1000L);
                saltsRef.put("expires", expireTimeV1.getEpochSecond() * 1000L);
                saltsRef.put("location", "saltsV1.txt");
                saltsRef.put("size", 8);
                saltsRefList.add(saltsRef);
            }
            {
                final JsonObject saltsRef = new JsonObject();
                saltsRef.put("effective", generatedTimeV2.getEpochSecond() * 1000L);
                saltsRef.put("expires", expireTimeV2.getEpochSecond() * 1000L);
                saltsRef.put("location", "saltsV2.txt");
                saltsRef.put("size", 8);
                saltsRefList.add(saltsRef);
            }
            metadataJson.put("salts", saltsRefList);
        }

        final String effectiveTimeStringV1 = String.valueOf(generatedTimeV1.getEpochSecond() * 1000L);
        final String saltsV1 =
                "1000000," + effectiveTimeStringV1 + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=\n" +
                        "1000001," + effectiveTimeStringV1 + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=\n" +
                        "1000002," + effectiveTimeStringV1 + ",+a5LPajo7uPfNcc9HH0Tn25b3RnSNZwe8YaAKcyeHaA=\n" +
                        "1000003," + effectiveTimeStringV1 + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=\n" +
                        "1000004," + effectiveTimeStringV1 + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=\n" +
                        "1000005," + effectiveTimeStringV1 + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=\n" +
                        "1000006," + effectiveTimeStringV1 + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=\n" +
                        "1000007," + effectiveTimeStringV1 + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=";

        // update key 1000002
        final String effectiveTimeStringV2 = String.valueOf(generatedTimeV2.getEpochSecond() * 1000L);
        final String saltsV2 =
                "1000000," + effectiveTimeStringV1 + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=\n" +
                        "1000001," + effectiveTimeStringV1 + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=\n" +
                        "1000002," + effectiveTimeStringV2 + ",AP73KwZscb1ltQQH/B7fdbHUnMmbJNlRULxzklXUqaA=\n" +
                        "1000003," + effectiveTimeStringV1 + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=\n" +
                        "1000004," + effectiveTimeStringV1 + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=\n" +
                        "1000005," + effectiveTimeStringV1 + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=\n" +
                        "1000006," + effectiveTimeStringV1 + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=\n" +
                        "1000007," + effectiveTimeStringV1 + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=";

        when(cloudStorage.download("sites/encrypted/1_public/metadata.json"))
                .thenReturn(new ByteArrayInputStream(metadataJson.toString().getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("saltsV1.txt"))
                .thenReturn(getEncryptedStream(saltsV1));
        when(cloudStorage.download("saltsV2.txt"))
                .thenReturn(getEncryptedStream(saltsV2));

        EncryptedRotatingSaltProvider saltsProvider = new EncryptedRotatingSaltProvider(
                cloudStorage, keyProvider, new EncryptedScope(new CloudPath("sites/metadata.json"), 1, true));

        final JsonObject loadedMetadata = saltsProvider.getMetadata();
        saltsProvider.loadContent(loadedMetadata);
        assertEquals(2, saltsProvider.getVersion(loadedMetadata));

        final ISaltProvider.ISaltSnapshot snapshot = saltsProvider.getSnapshot(Instant.now());
        assertEquals(FIRST_LEVEL_SALT, snapshot.getFirstLevelSalt());
        assertTrue(snapshot.getModifiedSince(Instant.now().minus(1, ChronoUnit.HOURS)).isEmpty());
        assertEquals(1, snapshot.getModifiedSince(Instant.now().minus(49, ChronoUnit.HOURS)).size());
        assertEquals(1000002, snapshot.getModifiedSince(Instant.now().minus(49, ChronoUnit.HOURS)).get(0).getId());
    }


}
