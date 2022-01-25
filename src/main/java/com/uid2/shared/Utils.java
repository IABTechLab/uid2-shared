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

package com.uid2.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.model.EncryptionKey;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String OS = System.getProperty("os.name").toLowerCase();
    public static final boolean IsWindows = OS.contains("win");

    public static boolean isProductionEnvironment() {
        // detect if it is running in KUBERNETES_SERVICE_HOST
        if (System.getenv("KUBERNETES_SERVICE_HOST") == null) {
            return false;
        } else {
            return true;
        }
    }

    public static int getPortOffset() {
        // read port_offset from env, the reason this can't be read from vertx-config
        // is Prometheus port needs to be specified before vertx creation
        String val = System.getenv("port_offset");
        int portOffset = 0;
        if (val != null) portOffset = Integer.valueOf(val);
        return portOffset;
    }

    public static boolean ensureDirectoryExists(String dir) {
        return Utils.ensureDirectoryExists(Paths.get(dir));
    }

    public static boolean ensureDirectoryExists(Path dir) {
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean ensureFileExists(String file) {
        return Utils.ensureFileExists(Paths.get(file));
    }

    public static boolean ensureFileExists(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public static String readToEnd(InputStream stream) throws IOException {
        final InputStreamReader reader = new InputStreamReader(stream);
        final char[] buff = new char[1024];
        final StringBuilder sb = new StringBuilder();
        for (int count; (count = reader.read(buff, 0, buff.length)) > 0;) {
            sb.append(buff, 0, count);
        }
        return sb.toString();
    }

    public static byte[] streamToByteArray(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public static InputStream localFileToStream(Path filePath) throws FileNotFoundException {
        return Utils.localFileToStream(filePath.toString());
    }

    public static InputStream localFileToStream(String fileName) throws FileNotFoundException {
        return new FileInputStream(new File(fileName));
    }

    public static <T> T toJson(InputStream stream) throws IOException {
        final InputStreamReader reader = new InputStreamReader(stream);
        final char[] buff = new char[1024];
        final StringBuilder sb = new StringBuilder();
        for (int count; (count = reader.read(buff, 0, buff.length)) > 0; ) {
            sb.append(buff, 0, count);
        }

        final T ret = (T) Json.decodeValue(sb.toString());
        return ret;
    }

    public static JsonObject toJsonObject(InputStream stream) throws IOException {
        return toJson(stream);
    }

    public static JsonArray toJsonArray(InputStream stream) throws IOException {
        return toJson(stream);
    }

    public static String toJson(Collection<String> strs) {
        try {
            return mapper.writeValueAsString(strs);
        } catch (Exception ex) {
            // this is internal message and not expected to be invalid, returning null
            return null;
        }
    }

    public static String toBase64String(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    public static byte[] decodeBase64String(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public static String maskPii(byte[] bytes) {
        if (bytes.length < 15) return "<...>";
        return String.format("%02x", bytes[0]) + "..." + String.format("%02x", bytes[bytes.length - 1]);
    }

    public static String maskPii(String pii) {
        if (pii.length() < 8) return "<...>";
        return pii.charAt(0) + "<...>" + pii.charAt(pii.length() - 1);
    }

    public static Instant nowUTCMillis() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public static String readToEndAsString(InputStream stream) throws IOException {
        final InputStreamReader reader = new InputStreamReader(stream);
        final char[] buff = new char[1024];
        final StringBuilder sb = new StringBuilder();
        for (int count; (count = reader.read(buff, 0, buff.length)) > 0;) {
            sb.append(buff, 0, count);
        }
        return sb.toString();
    }

    // Returns index of the first element for which comp(value, listItem) returns false
    // If no such element is found, returns list.size()
    // Modelled after C++ std::upper_bound()
    public static <T1, T2> int upperBound(List<T1> list, T2 value, BiPredicate<T2, T1> comp) {
        int it = 0;
        int first = 0;
        int count = list.size();
        int step = 0;
        while (count > 0) {
            step = count / 2;
            it = first + step;
            if (!comp.test(value, list.get(it))) {
                first = ++it;
                count -= step + 1;
            } else {
                count = step;
            }
        }
        return first;
    }
}
