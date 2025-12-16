package com.uid2.shared.optout;

import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.vertx.ICloudSync;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OptOutCloudSync implements ICloudSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptOutCloudSync.class);

    private final boolean fullSync;
    private final boolean uploadOnly;
    private final String cloudFolder;
    private final String deltaConsumerDir;
    private final String partitionConsumerDir;
    private final String cloudPartitionFolder;
    private final String cloudDeltaRootFolder;
    private final String cloudSyntheticFolder;

    private final int deltaBacktrackInDays;
    private final int replicaId;
    private final int maxReplicas;
    private final FileUtils fileUtils;

    private final boolean syntheticLogsEnabled;
    private final int syntheticLogsCount;

    private Vertx vertx = null;
    private String eventMergeDelta = null;
    private Consumer<Collection<String>> handlerIndexUpdate = null;
    private AtomicReference<List<Consumer<Collection<String>>>> handlersNewCloudPaths = new AtomicReference<>(new ArrayList<>());

    public OptOutCloudSync(JsonObject jsonConfig, boolean fullSync) {
        this(jsonConfig, fullSync, false);
    }

    public OptOutCloudSync(JsonObject jsonConfig, boolean fullSync, boolean uploadOnly) {
        this.fullSync = fullSync;
        this.uploadOnly = uploadOnly;
        this.cloudFolder = CloudUtils.normalizDirPath(jsonConfig.getString(Const.Config.OptOutS3FolderProp));
        this.deltaConsumerDir = OptOutUtils.getDeltaConsumerDir(jsonConfig);
        this.partitionConsumerDir = OptOutUtils.getPartitionConsumerDir(jsonConfig);
        assert cloudFolder != null && deltaConsumerDir != null && partitionConsumerDir != null;

        this.cloudPartitionFolder = this.cloudFolder + "partition/";
        this.cloudDeltaRootFolder = this.cloudFolder + "delta/";

        this.fileUtils = new FileUtils(jsonConfig);

        this.deltaBacktrackInDays = jsonConfig.getInteger(Const.Config.OptOutDeltaBacktrackInDaysProp);
        assert this.deltaBacktrackInDays > 0;

        this.replicaId = OptOutUtils.getReplicaId(jsonConfig);
        this.maxReplicas = jsonConfig.getInteger(Const.Config.OptOutProducerMaxReplicasProp, 0);
        assert this.maxReplicas > 0;

        boolean isSyntheticLogsEnabled;
        try {
            isSyntheticLogsEnabled = jsonConfig.getBoolean(Const.Config.OptOutSyntheticLogsEnabledProp);
        } catch (Exception ex){
            isSyntheticLogsEnabled = false;
        }
        this.syntheticLogsEnabled = isSyntheticLogsEnabled;

        if (isSyntheticLogsEnabled) {
            this.syntheticLogsCount = jsonConfig.getInteger(Const.Config.OptOutSyntheticLogsCountProp);
            this.cloudSyntheticFolder = this.cloudFolder + "synthetic/";
        } else {
            this.syntheticLogsCount = 0;
            this.cloudSyntheticFolder = null;
        }
        this.mkdirsBlocking();
    }

    /**
     * Creates an upload-only OptOutCloudSync instance.
     * This skips all download/refresh operations.
     */
    public static OptOutCloudSync createUploadOnly(JsonObject jsonConfig, boolean fullSync) {
        return new OptOutCloudSync(jsonConfig, fullSync, true);
    }

    @Override
    public boolean isUploadOnly() {
        return this.uploadOnly;
    }

    @Override
    public String toCloudPath(String path) {
        if (OptOutUtils.isDeltaFile(path)) {
            return newCloudPathForDelta(path);
        } else if (OptOutUtils.isPartitionFile(path)) {
            return newCloudPathForPartition(path);
        } else {
            return null;
        }
    }

    @Override
    public String toLocalPath(String path) {
        if (path.startsWith("http")) {
            try {
                URL url = new URL(path);
                // use the URL path to convert to local file
                path = url.getPath();
            } catch (MalformedURLException e) {
                LOGGER.error("Unable to parse preSignedUrl (" + path + "): " + e.getMessage(), e);
            }
        }

        if (OptOutUtils.isDeltaFile(path)) {
            return newLocalPathForDelta(path);
        } else if (OptOutUtils.isPartitionFile(path)) {
            return newLocalForPartition(path);
        } else if (syntheticLogsEnabled && OptOutUtils.isSyntheticFile(path)) {
            // synthetic file is a partition file (pre-sorted), we can handle it the same as partition file
            return newLocalForPartition(path);
        } else {
            return null;
        }
    }

    @Override
    public boolean refresh(Instant now, ICloudStorage fsCloud, ICloudStorage fsLocal, Consumer<Set<String>> handleDownloads, Consumer<Set<String>> handleDeletes) throws CloudStorageException {
        // In upload-only mode, skip all download/sync operations
        if (uploadOnly) {
            return true;
        }

        // list local cached paths
        List<String> cachedPathList = new ArrayList<>();
        localListFiles(fsLocal, this.deltaConsumerDir, OptOutUtils.prefixDeltaFile, cachedPathList);
        localListFiles(fsLocal, this.partitionConsumerDir, OptOutUtils.prefixPartitionFile, cachedPathList);

        // list cloud paths
        List<String> cloudPathList = this.cloudListRelevantFiles(fsCloud);

        // saving a copy of refreshed files for both remote and local
        Set<String> cloudPaths = new HashSet<>(cloudPathList);
        Set<String> cachedPaths = new HashSet<>(cachedPathList);

        // saving a map that can map localPath back to cloudPath
        // this is needed because with preSignedURL cloud -> local conversion is lossy
        Map<String, String> localToCloud = cloudPaths.stream()
            .collect(Collectors.toMap(this::toLocalPath, Function.identity()));

        // convert cloudPath to localPath, and remove cached local files
        Set<String> missing = cloudPaths.stream()
            .map(this::toLocalPath)
            .collect(Collectors.toSet());
        missing.removeAll(cachedPaths);
        // use local to cloud map to retrieve list of cloud files to download
        missing = missing.stream()
            .map(p -> localToCloud.get(p))
            .collect(Collectors.toSet());

        // invoke callback to handle downloads
        handleDownloads.accept(missing);

        Set<String> deletes = cachedPaths.stream()
            .filter(f -> !localToCloud.keySet().contains(f))
            .collect(Collectors.toSet());

        // invoke callback to delete files
        handleDeletes.accept(deletes);

        // provide cloudPaths to registered handlers
        for (Consumer<Collection<String>> handler : this.handlersNewCloudPaths.get()) {
            handler.accept(cloudPaths);
        }

        // return true when there are no missing files && no deleting files
        boolean inSync = missing.size() == 0 && deletes.size() == 0;

        if (inSync) {
            if (this.enableDeltaMerging()) {
                Collection<String> deltasToMerge = this.getDeltasToMerge(now, cloudPaths, cachedPaths);
                if (deltasToMerge != null) {

                    if (deltasToMerge.size() == 0) {
                        LOGGER.warn("Skip partition produce due to no delta files found between now and last partition");
                    } else {
                        LOGGER.debug("sending " + this.eventMergeDelta);
                        vertx.eventBus().send(this.eventMergeDelta, Utils.toJson(deltasToMerge));
                    }
                }
            } else if (this.handlerIndexUpdate != null) {
                this.handlerIndexUpdate.accept(cachedPaths);
            }
        }

        return inSync;
    }

    public Object registerNewCloudPathsHandler(Consumer<Collection<String>> handler) {
        List<Consumer<Collection<String>>> newHandlerList =  new ArrayList<>(this.handlersNewCloudPaths.get());
        newHandlerList.add(handler);
        this.handlersNewCloudPaths.set(Collections.unmodifiableList(newHandlerList));
        return handler;
    }

    public void unregisterNewCloudPathsHandler(Object handler) {
        Consumer<Collection<String>> typedHandler = (Consumer<Collection<String>>) handler;
        List<Consumer<Collection<String>>> newHandlerList =  new ArrayList<>(this.handlersNewCloudPaths.get());
        newHandlerList.remove(typedHandler);
        this.handlersNewCloudPaths.set(Collections.unmodifiableList(newHandlerList));
    }

    public void registerNewCachedPathsHandler(Consumer<Collection<String>> handler) {
        if (handlerIndexUpdate != null) throw new UnsupportedOperationException("already set");
        this.handlerIndexUpdate = handler;
    }

    public boolean enableDeltaMerging() {
        return this.vertx != null && this.eventMergeDelta != null;
    }

    public void enableDeltaMerging(Vertx vertx, String event) {
        this.eventMergeDelta = event;
        this.vertx = vertx;
    }

    private Collection<String> getDeltasToMerge(Instant now, Set<String> cloudPaths, Set<String> cachedPaths) {
        LOGGER.trace("getDeltasToMerge: evaluating...");

        // get last partition timestamp
        Instant tsLast = OptOutUtils.lastPartitionTimestamp(cachedPaths);

        // get new partition timestamp
        Instant ts = fileUtils.truncateToPartitionCutoffTime(now);

        // if partition time is before last partition, no need to produce new partition
        if (ts.isBefore(tsLast)) {
            LOGGER.trace("getDeltasToMerge: found recent last partition at " + ts + ", skipping");
            return null;
        }

        // if new partition time has not passed yet, no ned to produce new partition
        if (ts.isAfter(now)) {
            LOGGER.info("getDeltasToMerge: next partition scheduled at " + ts + ", too early for next partition");
            return null;
        }

        // each replica will have its time range to take turns (grace period is set to 3 * delta interval)
        int replicaInTurn = (int) (now.getEpochSecond() - ts.getEpochSecond()) / fileUtils.lookbackGracePeriod();
        if (replicaInTurn >= this.maxReplicas) replicaInTurn %= this.maxReplicas;
        if (replicaInTurn != this.replicaId) {
            LOGGER.info("getDeltasToMerge: replica " + replicaInTurn + " needs to produce partition, this is replica " + this.replicaId);
            return null;
        }

        // find delta files that falls in the time window
        Instant tsOld = tsLast.equals(Instant.EPOCH) ? tsLast : tsLast.minusSeconds(fileUtils.lookbackGracePeriod());
        Instant tsNew = now;

        HashSet<String> cached = new HashSet<>(fileUtils.filterFileInRange(cachedPaths, tsOld, tsNew).stream()
            .filter(OptOutUtils::isDeltaFile).collect(Collectors.toList()));
        HashSet<String> remote = new HashSet<>(fileUtils.filterFileInRange(cloudPaths, tsOld, tsNew).stream()
            .map(this::toLocalPath)
            .filter(OptOutUtils::isDeltaFile).collect(Collectors.toList()));

        // skip if not all delta files within the time window has already been downloaded
        if (!cached.equals(remote)) return null;

        // return the list of delta files
        Collection<String> ret = Collections.unmodifiableSet(cached);
        LOGGER.info("getDeltasToMerge found " + ret.size() + " delta files to merge");
        return ret;
    }

    private String newCloudPathForDelta(String fileToUpload) {
        Path path = Paths.get(fileToUpload);
        String fileName = path.getFileName().toString();
        String ts = OptOutUtils.getFileTimestamp(path).toString(); // 2020-12-06T02:49:39.606119Z
        String dateStr = ts.substring(0, 10);
        return CloudUtils.normalizeFilePath(Paths.get(this.cloudFolder, "delta", dateStr, fileName));
    }

    private String newCloudPathForPartition(String fileToUpload) {
        Path path = Paths.get(fileToUpload);
        String fileName = path.getFileName().toString();
        return CloudUtils.normalizeFilePath(Paths.get(this.cloudFolder, "partition", fileName));
    }

    private String newLocalPathForDelta(String fileToDownload) {
        Path path = Paths.get(fileToDownload);
        String fileName = path.getFileName().toString();
        return Paths.get(this.deltaConsumerDir, fileName).toString();
    }

    private String newLocalForPartition(String fileToDownload) {
        Path path = Paths.get(fileToDownload);
        String fileName = path.getFileName().toString();
        return Paths.get(this.partitionConsumerDir, fileName).toString();
    }

    private String getCloudDeltaFolder(Instant day) {
        day.truncatedTo(ChronoUnit.DAYS);
        String dateStr = OptOutUtils.getDateStr(day) + "/";
        return this.cloudDeltaRootFolder + dateStr;
    }

    private void localListFiles(ICloudStorage fsLocal, String dirToScan, String filePrefix,
                                List<String> cachedPaths) throws CloudStorageException {
        List<String> found = fsLocal.list(dirToScan);
        for (String f : found) {
            Path p = Paths.get(f);
            if (!p.getFileName().toString().startsWith(filePrefix)) {
                LOGGER.warn("Not under " + filePrefix + ", unknown file " + f);
            } else if (OptOutUtils.getFileTimestamp(f) == null) {
                LOGGER.warn("Unrecognized timestamp, unknown file " + f);
            }

            cachedPaths.add(f);
        }
    }

    private List<String> cloudListRelevantFiles(ICloudStorage cloudStorage) throws CloudStorageException {
        if (fullSync) {
            List<String> fileList = cloudStorage.list(this.cloudPartitionFolder);
            fileList.addAll(cloudStorage.list(this.cloudDeltaRootFolder));
            if (syntheticLogsEnabled) fileList.addAll(listSyntheticLogs(cloudStorage));
            return fileList;
        }

        // list all partitions
        List<String> cloudFiles = cloudStorage.list(this.cloudPartitionFolder);
        Instant tsLast = OptOutUtils.lastPartitionTimestamp(cloudFiles);
        if (tsLast == Instant.EPOCH) {
            // if there are no partition yet, list all delta files under delta root
            cloudFiles.addAll(cloudStorage.list(this.cloudDeltaRootFolder));
        } else {
            Instant t = tsLast.minus(this.deltaBacktrackInDays, ChronoUnit.DAYS);
            Instant now = Instant.now();

            // list all deltas generated after before (the date last partition file is created - N days)
            while (t.isBefore(now)) {
                String deltaPrefixToList = this.getCloudDeltaFolder(t);
                cloudFiles.addAll(cloudStorage.list(deltaPrefixToList));
                t = t.plus(1, ChronoUnit.DAYS);
            }
        }

        if (this.syntheticLogsEnabled) {
            cloudFiles.addAll(this.listSyntheticLogs(cloudStorage));
        }
        return cloudFiles;
    }

    private List<String> listSyntheticLogs(ICloudStorage cloudStorage) throws CloudStorageException {
        return cloudStorage.list(this.cloudSyntheticFolder).stream()
            .sorted()
            .limit(this.syntheticLogsCount)
            .collect(Collectors.toList());
    }

    private void mkdirsBlocking() {
        Utils.ensureDirectoryExists(this.deltaConsumerDir);
        Utils.ensureDirectoryExists(this.partitionConsumerDir);
    }
}
