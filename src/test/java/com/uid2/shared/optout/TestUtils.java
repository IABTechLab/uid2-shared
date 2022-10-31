package com.uid2.shared.optout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class TestUtils {
    public static OptOutEntry[] toEntries(long... ids) {
        long now = OptOutUtils.nowEpochSeconds();
        return Arrays.stream(ids).boxed().map(id -> OptOutEntry.newTestEntry(id, now))
            .toArray(OptOutEntry[]::new);
    }

    public static String newSuffix() {
        return String.format("_%s_%08x.dat", OptOutUtils.timestampNowEscaped(), OptOutUtils.rand.nextInt());
    }

    public static String newSnapshotFileName() {
        try {
            Path tmpFile = Files.createTempFile(OptOutUtils.prefixPartitionFile, newSuffix());
            return tmpFile.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String newLogFileName() {
        try {
            Path tmpFile = Files.createTempFile(OptOutUtils.prefixDeltaFile, newSuffix());
            return tmpFile.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String newLogFile(List<Long> list) {
        return TestUtils.newLogFile(OptOutUtils.toArray(list));
    }

    public static String newLogFile(long... ids) {
        return newLogFile(TestUtils.toEntries(ids));
    }

    public static String newLogFile(OptOutEntry[] entries) {
        try {
            Path tmpFile = Files.createTempFile(OptOutUtils.prefixDeltaFile, newSuffix());
            OptOutCollection store = new OptOutCollection(entries);
            Files.write(tmpFile, store.getStore(), StandardOpenOption.CREATE);
            return tmpFile.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
