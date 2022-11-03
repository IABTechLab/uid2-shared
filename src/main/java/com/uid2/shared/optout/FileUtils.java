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
