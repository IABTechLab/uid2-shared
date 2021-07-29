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

import com.uid2.shared.Const;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {
    // stores interval for configured log rotation period, which will be use to assess healthy status of index
    private final int deltaRotationInterval;
    private final int partitionInterval;
    private final int maxPartitions;
    private final boolean syntheticLogsEnabled;
    private final int maxSyntheticLogs;

    public FileUtils(JsonObject config) {
        this.deltaRotationInterval = config.getInteger(Const.Config.OptOutDeltaRotateIntervalProp);
        this.partitionInterval = config.getInteger(Const.Config.OptOutPartitionIntervalProp);
        this.maxPartitions = config.getInteger(Const.Config.OptOutMaxPartitionsProp);

        boolean isSyntheticLogsEnabled;
        try {
            isSyntheticLogsEnabled = config.getBoolean(Const.Config.OptOutSyntheticLogsEnabledProp);
        } catch (Exception ex){
            isSyntheticLogsEnabled = false;
        }
        this.syntheticLogsEnabled = isSyntheticLogsEnabled;
        if (this.syntheticLogsEnabled) {
            this.maxSyntheticLogs = config.getInteger(Const.Config.OptOutSyntheticLogsCountProp);
        } else {
            this.maxSyntheticLogs = 0;
        }
    }

    // used for finding files to download
    public List<String> filterNonExpired(Collection<String> collection, Instant now) {
        return collection.stream()
            .filter(f -> !isDeltaOrPartitionExpired(now, f))
            .collect(Collectors.toList());
    }

    public int maxPartitions() {
        return this.maxPartitions;
    }

    public int maxPartitionsWithSynthetic() {
        return this.maxPartitions + this.maxSyntheticLogs;
    }

    public boolean isDeltaOrPartitionExpired(Instant now, String fileName) {
        Instant ts = OptOutUtils.getFileTimestamp(fileName);
        if (ts == null) return true;

        if (this.syntheticLogsEnabled && OptOutUtils.isSyntheticFile(fileName)) {
            // synthetic files are never expired
            return false;
        } else {
            return isDeltaOrPartitionExpired(now, ts);
        }
    }

    public boolean isDeltaOrPartitionExpired(Instant now, Instant tsOfFile) {
        return tsOfFile.isBefore(now.minusSeconds(this.optOutMaxLifespanInSeconds()));
    }

    public boolean isFileInRange(String file, Instant timeLeft, Instant timeRight) {
        Instant ts = OptOutUtils.getFileTimestamp(file);

        if (this.syntheticLogsEnabled && OptOutUtils.isSyntheticFile(file)) {
            // synthetic files by-pass time range check (never expired)
            return true;
        } else {
            return ts.isAfter(timeLeft) && ts.isBefore(timeRight);
        }
    }

    public List<String> filterFileInRange(Collection<String> collection, Instant min, Instant max) {
        return collection.stream()
            .filter(f -> isFileInRange(f, min, max))
            .collect(Collectors.toList());
    }

    public int optOutMaxLifespanInSeconds() {
        return this.partitionInterval * this.maxPartitions;
    }

    public int lookbackGracePeriod() {
        return this.deltaRotationInterval * 3;
    }

    public Instant truncateToPartitionCutoffTime(Instant ts) {
        ts = ts.truncatedTo(ChronoUnit.DAYS);
        while (ts.plusSeconds(this.partitionInterval).isBefore(ts)) {
            ts = ts.plusSeconds(this.partitionInterval);
        }
        return ts;
    }
}
