// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.optout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import com.uid2.shared.cloud.CloudUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class OptOutUtils {
    // delta file name pattern: optout-delta-<replica_id>_yyyy-MM-ddTHH:mm:ssZ.dat
    public static final String prefixDeltaFile = "optout-delta-";
    // partition file name pattern: optout-partition-<replica_id>_yyyy-MM-ddTHH:MM:SSZ.dat
    public static final String prefixPartitionFile = "optout-partition-";
    private static final Logger LOGGER = LoggerFactory.getLogger(OptOutUtils.class);

    public static Random rand = new Random();
    public static ObjectMapper mapper = new ObjectMapper();
    public static String tmpDir = System.getProperty("java.io.tmpdir");

    public static Base64.Encoder base64Encoder = Base64.getEncoder();
    public static Base64.Decoder base64Decoder = Base64.getDecoder();

    public static byte[] nullHashBytes = new byte[OptOutConst.Sha256Bytes];
    public static String nullHash = OptOutUtils.byteArrayToHex(new byte[OptOutConst.Sha256Bytes]);

    public static String onesHash = new String(new char[OptOutConst.Sha256Characters]).replace("\0", "f");
    public static byte[] onesHashBytes = OptOutUtils.sha256HexToByteArray(OptOutUtils.onesHash);

    // comparator to help producing a sorted order of delta filenames
    public static final Comparator<String> DeltaFilenameComparator = new Comparator<String>() {
        @Override
        public int compare(String f1, String f2) {
            try {
                return (int) (OptOutUtils.getFileTimestamp(f1).getEpochSecond() - OptOutUtils.getFileTimestamp(f2).getEpochSecond());
            } catch (Exception ex) {
                LOGGER.error("unexpected exception for delta filename comparison: " + ex.getMessage());
                return f1.compareTo(f2);
            }
        }
    };

    // comparator to help producing a descending sorted order of delta filenames
    public static final Comparator<String> DeltaFilenameComparatorDescending = new Comparator<String>() {
        @Override
        public int compare(String f1, String f2) {
            try {
                return (int) (OptOutUtils.getFileTimestamp(f2).getEpochSecond() - OptOutUtils.getFileTimestamp(f1).getEpochSecond());
            } catch (Exception ex) {
                LOGGER.error("unexpected exception for delta filename comparison: " + ex.getMessage());
                return f2.compareTo(f1);
            }
        }
    };

    public static boolean isHexidecimal(char c) {
        // assuming ascii encoding
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static boolean isValidSha256Hex(String sha256Hex) {
        if (sha256Hex.length() != OptOutConst.Sha256Characters)
            return false;

        for (int i = 0; i < OptOutConst.Sha256Characters; ++i) {
            if (!isHexidecimal(sha256Hex.charAt(i))) return false;
        }
        return true;
    }

    public static byte[] sha256HexToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static String byteArrayToBase64String(byte[] bytes) {
        return base64Encoder.encodeToString(bytes);
    }

    public static byte[] base64StringTobyteArray(String b64) {
        try {
            return base64Decoder.decode(b64);
        } catch (IllegalArgumentException ex) {
            // base64 decoder throws if input is invalid, returning null for such case
            return null;
        }
    }

    public static int logTwo(int val) {
        assert val > 0;
        int powerOfTwo = 0;
        while (val != 1) {
            ++powerOfTwo;
            val >>= 1;
        }
        return powerOfTwo;
    }

    public static String[] jsonArrayToStringArray(String json) {
        try {
            return mapper.readValue(json, String[].class);
        } catch (Exception ex) {
            // this is internal message and not expected to be invalid, returning null
            return null;
        }
    }

    public static String toJson(String... strs) {
        return OptOutUtils.toJson(Arrays.asList(strs));
    }

    public static String toJson(Collection<String> strs) {
        try {
            return mapper.writeValueAsString(strs);
        } catch (Exception ex) {
            // this is internal message and not expected to be invalid, returning null
            return null;
        }
    }

    public static <T> List<T> toList(T... array) {
        return Arrays.stream(array).collect(Collectors.toList());
    }

    public static List<Long> toList(long[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toList());
    }

    public static long[] toLongArray(long... nums) {
        return nums;
    }

    public static <T> T[] toArray(T... array) {
        return array;
    }

    public static long[] toArray(List<Long> list) {
        return list.stream().mapToLong(l -> l).toArray();
    }

    public static Set<Long> toSet(long[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toSet());
    }

    public static <T> HashSet<T> toSet(T[] array) {
        return new HashSet<>(Arrays.asList(array));
    }

    public static byte[] toByteArray(int num) {
        return ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(num).array();
    }

    public static byte[] toByteArray(long num) {
        return ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(num).array();
    }

    public static byte[] toByteArrayBE(long num) {
        return ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN).putLong(num).array();
    }

    public static int toInt(byte[] bytes, int byteIndex) {
        assert byteIndex >= 0 && byteIndex + Integer.BYTES <= bytes.length;
        int val = 0;
        for (int i = 0; i < Integer.BYTES; i++) {
            val += ((int) bytes[byteIndex + i] & 0xffL) << (8 * i);
        }
        return val;
    }

    public static long toLong(byte[] bytes, int byteIndex) {
        assert byteIndex >= 0 && byteIndex + Long.BYTES <= bytes.length;
        long val = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            val += ((long) bytes[byteIndex + i] & 0xffL) << (8 * i);
        }
        return val;
    }

    public static long toLongBE(byte[] bytes, int byteIndex) {
        return ByteBuffer.wrap(bytes, 0, Long.BYTES).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    // positive if arr1 > arr2
    // negative if arr1 < arr2
    // 0 if arr1 == arr2
    public static int compareBytes(byte[] arr1, byte[] arr2) {
        assert arr1.length == arr2.length;
        return OptOutUtils.compareByteRange(arr1, 0, arr2, 0, arr1.length);
    }

    // positive if arr1 > arr2
    // negative if arr1 < arr2
    // 0 if arr1 == arr2
    public static int compareByteRange(byte[] arr1, int idx1, byte[] arr2, int idx2, int bytesToCompare) {
        assert idx1 >= 0 && idx1 + bytesToCompare <= arr1.length;
        assert idx2 >= 0 && idx2 + bytesToCompare <= arr2.length;
        for (int i = 0; i < bytesToCompare; ++i) {
            int cmp = OptOutUtils.compareByte(arr1[idx1 + i], arr2[idx2 + i]);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    public static int compareByte(byte a, byte b) {
        return Byte.toUnsignedInt(a) - Byte.toUnsignedInt(b);
    }

    public static boolean isDeltaFile(String fn) {
        return fn.contains(OptOutUtils.prefixDeltaFile);
    }

    public static boolean isPartitionFile(String fn) {
        return fn.contains(OptOutUtils.prefixPartitionFile);
    }

    public static long getFileEpochSeconds(String fullPath) {
        return OptOutUtils.getFileTimestamp(fullPath).getEpochSecond();
    }

    public static int getFileReplicaId(Path path) {
        // file name forpath optout-partition-999_2021-04-21T00.00.00Z_014e024f.dat
        String fn = path.getFileName().toString();
        int indexOfSep = fn.indexOf('_');
        if (indexOfSep == -1) return -1;

        // optout-partition-999
        String fileTypeAndReplicaId = fn.substring(0, indexOfSep);
        int lastIdxOfDash = fileTypeAndReplicaId.lastIndexOf("-");
        if (lastIdxOfDash == -1) return -1;

        // 999
        return Integer.valueOf(fileTypeAndReplicaId.substring(lastIdxOfDash + 1));
    }

    public static Instant getFileTimestamp(String fullPath) {
        if (fullPath.startsWith("https")) {
            fullPath = extractUrlPath(fullPath);
        }
        return OptOutUtils.getFileTimestamp(Paths.get(fullPath));
    }

    public static String extractUrlPath(String urlPath) {
        // remove query and extract path from URL
        try {
            URL url = new URL(urlPath);
            // return urlPath.replace("?" + url.getQuery(), "");
            return url.getPath();
        } catch (MalformedURLException e) {
            return urlPath;
        }
    }

    public static Instant getFileTimestamp(Path path) {
        String fileName = path.getFileName().toString();

        int indexOfSep = fileName.indexOf('_');
        if (indexOfSep == -1) return null;

        String timestamp = fileName.substring(indexOfSep + 1);
        int indexOfSep2 = timestamp.indexOf('_');
        if (indexOfSep2 == -1) return null;

        // yyyy-MM-ddTHH,mm,ssZ -> yyyy-MM-ddTHH:mm:ssZ
        timestamp = timestamp.substring(0, indexOfSep2);
        timestamp = timestamp.replace('.', ':');

        try {
            return Instant.parse(timestamp);
        } catch (Exception ex) {
            return null;
        }
    }

    public static long nowEpochSeconds() {
        return Instant.now().getEpochSecond();
    }

    // yyyy-MM-ddTHH:mm:ssZ -> yyyy-MM-ddTHH,mm,ssZ
    public static String timestampNowEscaped() {
        return timestampEscaped(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

    public static String timestampEscaped(Instant ts) {
        return ts.toString().replace(':', '.');
    }

    public static String newDeltaFileName(int replicaId) {
        String ts = timestampNowEscaped();
        return OptOutUtils.newDeltaFileName(ts, replicaId);
    }

    public static String newPartitionFileName(int replicaId) {
        String ts = timestampNowEscaped();
        return OptOutUtils.newPartitionFileName(ts, replicaId);
    }

    public static String newDeltaFileName(Instant now) {
        String ts = timestampEscaped(now);
        return OptOutUtils.newDeltaFileName(ts, 0);
    }

    public static String newPartitionFileName(Instant now) {
        String ts = timestampEscaped(now);
        return OptOutUtils.newPartitionFileName(ts, 0);
    }

    public static String newDeltaFileName(String ts, int replicaId) {
        return String.format("%s%03d_%s_%08x.dat", OptOutUtils.prefixDeltaFile, replicaId, ts, OptOutUtils.rand.nextInt());
    }

    public static String newPartitionFileName(String ts, int replicaId) {
        return String.format("%s%03d_%s_%08x.dat", OptOutUtils.prefixPartitionFile, replicaId, ts, OptOutUtils.rand.nextInt());
    }

    public static int getReplicaIdFromHostName(String hostname) {
        try {
            if (hostname.indexOf('.') > 0) {
                hostname = hostname.substring(0, hostname.indexOf('.'));
            }
            return Integer.valueOf(hostname.substring(hostname.lastIndexOf('-') + 1));
        } catch (Exception e) {
            LOGGER.error("unable to parse replica_id from hostname " + hostname);
            return 0;
        }
    }

    public static String getDateStr(Instant time) {
        String ts = time.toString(); // 2020-12-06T02:49:39.606119Z
        return ts.substring(0, 10);
    }

    public static int getReplicaId(JsonObject jsonConfig) {
        int replicaId = -1;
        try {
            replicaId = jsonConfig.getInteger(Const.Config.OptOutProducerReplicaIdProp);
        } catch (ClassCastException e) {
            replicaId = Integer.valueOf(jsonConfig.getString(Const.Config.OptOutProducerReplicaIdProp));
        } catch (NullPointerException e) {
            replicaId = -1;
        }

        int offset = 0;
        try {
            offset = jsonConfig.getInteger(Const.Config.OptOutProducerReplicaIdOffsetProp);
        } catch (ClassCastException e) {
            offset = Integer.valueOf(jsonConfig.getString(Const.Config.OptOutProducerReplicaIdOffsetProp));
        } catch (NullPointerException e) {
            offset = 0;
        }

        if (Utils.isProductionEnvionment() && replicaId == -1) {
            // read from hostname
            try {
                String hostname = InetAddress.getLocalHost().getHostName();
                replicaId = offset + OptOutUtils.getReplicaIdFromHostName(hostname);
            } catch (UnknownHostException e) {
                LOGGER.error("unable to retrieve hostname", e);
                replicaId = offset;
            }
        }

        return replicaId;
    }

    // this method assumes the list is sorted using comparator, and will insert the new value into the proper place
    public static void addSorted(LinkedList<String> list, String filename, Comparator<String> comparator) {
        ListIterator<String> iterator = list.listIterator();
        while (iterator.hasNext()) {
            // find the iterator to insert the filename
            if (comparator.compare(iterator.next(), filename) > 0) {
                // move back one iterator if it is not the first
                if (iterator.hasPrevious()) iterator.previous();
                break;
            }
        }
        iterator.add(filename);
    }

    public static Instant instantFloorByInterval(Instant now, int interval) {
        long minusSeconds = interval - getSecondsBeforeNextSlot(now, interval);
        return Instant.ofEpochSecond(now.getEpochSecond() - minusSeconds);
    }

    public static int getSecondsBeforeNextSlot(Instant now, int slotInterval) {
        if (slotInterval < 1) return 0;

        long secondsOfDay = now.getEpochSecond() - now.truncatedTo(ChronoUnit.DAYS).getEpochSecond();
        assert secondsOfDay >= 0;
        long secondsOfNextSlot = ((secondsOfDay + slotInterval - 1) / slotInterval) * slotInterval;
        assert secondsOfNextSlot >= secondsOfDay;
        return (int) (secondsOfNextSlot - secondsOfDay);
    }

    public static Future<Long> readTimestampFromFile(Vertx vertx, Path filePath, long defaultValue) {
        Promise<Long> promise = Promise.promise();
        vertx.<Long>executeBlocking(blockPromise -> {
            // create parent directory if not existing
            Utils.ensureDirectoryExists(filePath.getParent());

            if (Files.exists(filePath)) {
                try {
                    List<String> lines = Files.readAllLines(filePath);
                    if (lines.size() > 0) {
                        Long epochSeconds = Long.valueOf(lines.get(0));
                        blockPromise.complete(epochSeconds);
                        return;
                    }
                } catch (IOException e) {
                    blockPromise.fail(new Throwable(e));
                    return;
                }
            }
            try {
                Files.write(filePath, String.valueOf(defaultValue).getBytes(), StandardOpenOption.CREATE);
                blockPromise.complete(defaultValue);
            } catch (IOException e) {
                blockPromise.fail(new Throwable(e));
            }
        }, ar -> {
            promise.handle(ar);
        });
        return promise.future();
    }

    public static Future<Void> writeTimestampToFile(Vertx vertx, Path filePath, long timestamp) {
        Promise<Void> promise = Promise.promise();
        vertx.<Void>executeBlocking(blockPromise -> {
            // create parent directory if not existing
            Utils.ensureDirectoryExists(filePath.getParent());

            try {
                Files.write(filePath, String.valueOf(timestamp).getBytes());
                blockPromise.complete();
            } catch (IOException e) {
                blockPromise.fail(new Throwable(e));
            }
        }, ar -> {
            promise.handle(ar);
        });
        return promise.future();
    }

    public static Future<Void> appendLinesToFile(Vertx vertx, Path filePath, List<String> lines) {
        Promise<Void> promise = Promise.promise();
        vertx.<Void>executeBlocking(blockPromise -> {
            // create parent directory if not existing
            Utils.ensureDirectoryExists(filePath.getParent());
            // create empty file
            Utils.ensureFileExists(filePath);

            try {
                // append an empty line
                lines.add("");
                byte[] bytes = String.join("\n", lines).getBytes();
                Files.write(filePath, bytes, StandardOpenOption.APPEND);
                blockPromise.complete();
            } catch (IOException e) {
                blockPromise.fail(new Throwable(e));
            }
        }, ar -> {
            promise.handle(ar);
        });
        return promise.future();
    }

    public static Future<String[]> readLinesFromFile(Vertx vertx, Path filePath) {
        Promise<String[]> promise = Promise.promise();
        vertx.<String[]>executeBlocking(blockPromise -> {
            // create parent directory if not existing
            Utils.ensureDirectoryExists(filePath.getParent());

            try {
                if (Files.exists(filePath)) {
                    String[] lines = Files.readAllLines(filePath).stream().toArray(String[]::new);
                    blockPromise.complete(lines);
                } else {
                    blockPromise.complete(new String[0]);
                }
            } catch (IOException e) {
                blockPromise.fail(new Throwable(e));
            }
        }, ar -> {
            promise.handle(ar);
        });
        return promise.future();
    }

    // last partition file timestamp
    public static Instant lastPartitionTimestamp(Collection<String> collection) {
        Optional<Instant> tsOfLast = collection.stream()
            .filter(p -> OptOutUtils.isPartitionFile(p))
            .map(OptOutUtils::getFileTimestamp)
            .sorted(Comparator.reverseOrder()).findFirst();

        return tsOfLast.isPresent() ? tsOfLast.get() : Instant.EPOCH;
    }

    public static boolean isDeltaBeforePartition(Instant tsOfSnap, String deltaFile) {
        Instant ts = OptOutUtils.getFileTimestamp(deltaFile);
        if (ts == null) return true;
        else return ts.isBefore(tsOfSnap);
    }

    public static boolean isSyntheticFile(String fullPath) {
        if (fullPath.startsWith("https")) {
            fullPath = extractUrlPath(fullPath);
        }

        return OptOutUtils.getFileReplicaId(Paths.get(fullPath)) == 999;
    }

    public static String getDeltaConsumerDir(JsonObject config) {
        return String.format("%sconsumer/delta",
            CloudUtils.normalizDirPath(config.getString(Const.Config.OptOutDataDirProp)));
    }

    public static String getPartitionConsumerDir(JsonObject config) {
        return String.format("%sconsumer/partition",
            CloudUtils.normalizDirPath(config.getString(Const.Config.OptOutDataDirProp)));
    }
}
