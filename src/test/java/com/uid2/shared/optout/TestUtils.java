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
