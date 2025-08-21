package com.uid2.shared.store;

import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.salt.ISaltProvider;
import com.uid2.shared.store.salt.RotatingSaltProvider;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotatingSaltProviderTest {
    @Mock
    private ICloudStorage cloudStorage;

    @Test
    void loadSaltSingleVersion() throws Exception {
        final String firstLevelSalt = "first_level_salt_value";
        final String idPrefix = "a";
        final String idSecret = "m3yMIcbg9vCaFLJsn4m4PfruZnvAZ72OxmFG5QsGMOw=";

        final Instant generatedTime = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant expireTime = Instant.now().plus(365, ChronoUnit.DAYS);

        final JsonObject metadataJson = new JsonObject();
        {
            metadataJson.put("version", 2);
            metadataJson.put("generated", generatedTime.getEpochSecond() * 1000L);
            metadataJson.put("first_level", firstLevelSalt);
            metadataJson.put("id_prefix", idPrefix);
            metadataJson.put("id_secret", idSecret);
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
        final String refreshFromTimeString = String.valueOf(generatedTime.plus(30, ChronoUnit.DAYS).getEpochSecond() * 1000L);
        final String salts =
                "1000000," + effectiveTimeString + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=," + refreshFromTimeString + ",,,,,,\n" +
                        "1000001," + effectiveTimeString + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=," + refreshFromTimeString + ",,,,,,\n" +
                        "1000002," + effectiveTimeString + ",+a5LPajo7uPfNcc9HH0Tn25b3RnSNZwe8YaAKcyeHaA=," + refreshFromTimeString + ",,,,,,\n" +
                        "1000003," + effectiveTimeString + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=," + refreshFromTimeString + ",,,,,,\n" +
                        "1000004," + effectiveTimeString + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=," + refreshFromTimeString + ",,,,,,\n" +
                        "1000005," + effectiveTimeString + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=," + refreshFromTimeString + ",,,,,,\n" +
                        "1000006," + effectiveTimeString + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=," + refreshFromTimeString + ",,,,,,\n" +
                        "1000007," + effectiveTimeString + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=," + refreshFromTimeString + ",,,,,,";

        when(cloudStorage.download("metadata"))
                .thenReturn(new ByteArrayInputStream(metadataJson.toString().getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("salts.txt"))
                .thenReturn(new ByteArrayInputStream(salts.toString().getBytes(StandardCharsets.US_ASCII)));

        RotatingSaltProvider saltsProvider = new RotatingSaltProvider(
                cloudStorage, "metadata");

        final JsonObject loadedMetadata = saltsProvider.getMetadata();
        saltsProvider.loadContent(loadedMetadata);
        assertEquals(2, saltsProvider.getVersion(loadedMetadata));

        final ISaltProvider.ISaltSnapshot snapshot = saltsProvider.getSnapshot(Instant.now());
        assertEquals(firstLevelSalt, snapshot.getFirstLevelSalt());
        assertTrue(snapshot.getModifiedSince(Instant.now().minus(1, ChronoUnit.HOURS)).isEmpty());
    }

    @Test
    void loadSaltMultipleVersions() throws Exception {
        final String firstLevelSalt = "first_level_salt_value";
        final String idPrefix = "a";
        final String idSecret = "m3yMIcbg9vCaFLJsn4m4PfruZnvAZ72OxmFG5QsGMOw=";

        final Instant generatedTimeV1 = Instant.now().minus(2, ChronoUnit.DAYS);
        final Instant expireTimeV1 = Instant.now().plus(365, ChronoUnit.DAYS);
        final Instant generatedTimeV2 = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant expireTimeV2 = Instant.now().plus(366, ChronoUnit.DAYS);

        final JsonObject metadataJson = new JsonObject();
        {
            metadataJson.put("version", 2);
            metadataJson.put("generated", generatedTimeV1.getEpochSecond() * 1000L);
            metadataJson.put("first_level", firstLevelSalt);
            metadataJson.put("id_prefix", idPrefix);
            metadataJson.put("id_secret", idSecret);
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
        final String refreshFromTimeStringV1 = String.valueOf(generatedTimeV1.plus(30, ChronoUnit.DAYS).getEpochSecond() * 1000L);
        final String effectiveTimeStringV2 = String.valueOf(generatedTimeV2.getEpochSecond() * 1000L);
        final String refreshFromTimeStringV2 = String.valueOf(generatedTimeV2.plus(60, ChronoUnit.DAYS).getEpochSecond() * 1000L);

        final String saltsV1 =
                "1000000," + effectiveTimeStringV1 + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000001," + effectiveTimeStringV1 + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000002," + effectiveTimeStringV1 + ",+a5LPajo7uPfNcc9HH0Tn25b3RnSNZwe8YaAKcyeHaA=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000003," + effectiveTimeStringV1 + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000004," + effectiveTimeStringV1 + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000005," + effectiveTimeStringV1 + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000006," + effectiveTimeStringV1 + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000007," + effectiveTimeStringV1 + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=," + refreshFromTimeStringV1 + ",,,,,,";

        // update key 1000002
        final String saltsV2 =
                "1000000," + effectiveTimeStringV1 + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000001," + effectiveTimeStringV1 + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000002," + effectiveTimeStringV2 + ",AP73KwZscb1ltQQH/B7fdbHUnMmbJNlRULxzklXUqaA=," + refreshFromTimeStringV2 + ",AP73KwZscb1ltQQH/B7fdbHUnMmbJNlRULxzklXUqaA=,,,,,\n" +
                        "1000003," + effectiveTimeStringV1 + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000004," + effectiveTimeStringV1 + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000005," + effectiveTimeStringV1 + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000006," + effectiveTimeStringV1 + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000007," + effectiveTimeStringV1 + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=," + refreshFromTimeStringV2 + ",,,,,,";

        when(cloudStorage.download("metadata"))
                .thenReturn(new ByteArrayInputStream(metadataJson.toString().getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("saltsV1.txt"))
                .thenReturn(new ByteArrayInputStream(saltsV1.getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("saltsV2.txt"))
                .thenReturn(new ByteArrayInputStream(saltsV2.getBytes(StandardCharsets.US_ASCII)));

        RotatingSaltProvider saltsProvider = new RotatingSaltProvider(
                cloudStorage, "metadata");

        final JsonObject loadedMetadata = saltsProvider.getMetadata();
        saltsProvider.loadContent(loadedMetadata);
        assertEquals(2, saltsProvider.getVersion(loadedMetadata));

        final ISaltProvider.ISaltSnapshot snapshot = saltsProvider.getSnapshot(Instant.now());
        assertEquals(firstLevelSalt, snapshot.getFirstLevelSalt());
        assertTrue(snapshot.getModifiedSince(Instant.now().minus(1, ChronoUnit.HOURS)).isEmpty());
        assertEquals(1, snapshot.getModifiedSince(Instant.now().minus(30, ChronoUnit.HOURS)).size());
        assertEquals(1000002, snapshot.getModifiedSince(Instant.now().minus(30, ChronoUnit.HOURS)).get(0).id());
    }

    @Test
    void loadSaltMultipleVersionsExpired() throws Exception {
        final String firstLevelSalt = "first_level_salt_value";
        final String idPrefix = "a";
        final String idSecret = "m3yMIcbg9vCaFLJsn4m4PfruZnvAZ72OxmFG5QsGMOw=";

        final Instant generatedTimeV1 = Instant.now().minus(3, ChronoUnit.DAYS);
        final Instant expireTimeV1 = Instant.now().minus(2, ChronoUnit.DAYS);
        final Instant generatedTimeV2 = Instant.now().minus(2, ChronoUnit.DAYS);
        final Instant expireTimeV2 = Instant.now().minus(1, ChronoUnit.DAYS);

        final JsonObject metadataJson = new JsonObject();
        {
            metadataJson.put("version", 2);
            metadataJson.put("generated", generatedTimeV1.getEpochSecond() * 1000L);
            metadataJson.put("first_level", firstLevelSalt);
            metadataJson.put("id_prefix", idPrefix);
            metadataJson.put("id_secret", idSecret);
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
        final String refreshFromTimeStringV1 = String.valueOf(generatedTimeV1.plus(30, ChronoUnit.DAYS).getEpochSecond() * 1000L);
        final String effectiveTimeStringV2 = String.valueOf(generatedTimeV2.getEpochSecond() * 1000L);
        final String refreshFromTimeStringV2 = String.valueOf(generatedTimeV2.plus(60, ChronoUnit.DAYS).getEpochSecond() * 1000L);

        final String saltsV1 =
                "1000000," + effectiveTimeStringV1 + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000001," + effectiveTimeStringV1 + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000002," + effectiveTimeStringV1 + ",+a5LPajo7uPfNcc9HH0Tn25b3RnSNZwe8YaAKcyeHaA=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000003," + effectiveTimeStringV1 + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000004," + effectiveTimeStringV1 + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000005," + effectiveTimeStringV1 + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000006," + effectiveTimeStringV1 + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=," + refreshFromTimeStringV1 + ",,,,,,\n" +
                        "1000007," + effectiveTimeStringV1 + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=," + refreshFromTimeStringV1 + ",,,,,,";

        // update key 1000002
        final String saltsV2 =
                "1000000," + effectiveTimeStringV1 + ",y5YitNf/KFtceipDz8nqsFVmBZsK3KY7s8bOVM4gMD4=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000001," + effectiveTimeStringV1 + ",z1uBoGyyzgna9i0o/r5eiD/wAhDX/2Q/6zX1p6hsF7I=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000002," + effectiveTimeStringV2 + ",AP73KwZscb1ltQQH/B7fdbHUnMmbJNlRULxzklXUqaA=," + refreshFromTimeStringV2 + ",AP73KwZscb1ltQQH/B7fdbHUnMmbJNlRULxzklXUqaA=,,,,,\n" +
                        "1000003," + effectiveTimeStringV1 + ",wAL6U+lu9gcMhSEySzWG9RQyoo446zAyGWKTW8VVoVw=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000004," + effectiveTimeStringV1 + ",eP9ZvW4igLQZ4QfzlyiXgKYFDZgmGOefaKDLEL0zuwE=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000005," + effectiveTimeStringV1 + ",UebesrNN0bQkm/QR7Jx7eav+UDXN5Gbq3zs1fLBMRy0=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000006," + effectiveTimeStringV1 + ",MtpALOziEJMtPlCQHk6RHALuWvRvRZpCDBmO0xPAia0=," + refreshFromTimeStringV2 + ",,,,,,\n" +
                        "1000007," + effectiveTimeStringV1 + ",7tjv+KXaSztTZHEHULacotHQ7IpGBcw6IymoRLObkT4=," + refreshFromTimeStringV2 + ",,,,,,";

        when(cloudStorage.download("metadata"))
                .thenReturn(new ByteArrayInputStream(metadataJson.toString().getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("saltsV1.txt"))
                .thenReturn(new ByteArrayInputStream(saltsV1.getBytes(StandardCharsets.US_ASCII)));
        when(cloudStorage.download("saltsV2.txt"))
                .thenReturn(new ByteArrayInputStream(saltsV2.getBytes(StandardCharsets.US_ASCII)));

        RotatingSaltProvider saltsProvider = new RotatingSaltProvider(
                cloudStorage, "metadata");

        final JsonObject loadedMetadata = saltsProvider.getMetadata();
        saltsProvider.loadContent(loadedMetadata);
        assertEquals(2, saltsProvider.getVersion(loadedMetadata));

        final ISaltProvider.ISaltSnapshot snapshot = saltsProvider.getSnapshot(Instant.now());
        assertEquals(firstLevelSalt, snapshot.getFirstLevelSalt());
        assertTrue(snapshot.getModifiedSince(Instant.now().minus(1, ChronoUnit.HOURS)).isEmpty());
        assertEquals(1, snapshot.getModifiedSince(Instant.now().minus(49, ChronoUnit.HOURS)).size());
        assertEquals(1000002, snapshot.getModifiedSince(Instant.now().minus(49, ChronoUnit.HOURS)).get(0).id());
    }
}
