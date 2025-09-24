package com.uid2.shared.vertx;

import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.health.HealthComponent;
import com.uid2.shared.health.HealthManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Metrics;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//
// consumes events:
//   - cloudsync.<name>.refresh
//
// consumes events:
//   - cloudsync.<name>.upload
//
// produces events:
//   - cloudsync.<name>.downloaded    (String path, from s3-refresh)
//   - cloudsync.<name>.refreshed     (from s3-refresh)
//
public class CloudSyncVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudSyncVerticle.class);
    private final HealthComponent healthComponent;

    private final Counter counterRefreshed;
    private final Counter counterRefreshSkipped;
    private final Counter counterRefreshFailures;
    private final Counter counterDownloaded;
    private final Counter counterUploaded;
    private final Counter counterDownloadFailures;
    private final Counter counterUploadFailures;
    private final Gauge gaugeConsecutiveRefreshFailures;
    private final Timer downloadSuccessTimer;
    private final Timer downloadFailureTimer;

    private final String name;
    private final ICloudStorage cloudStorage;
    private final ICloudStorage localStorage;
    private final ICloudSync cloudSync;
    private final int downloadThreads;
    private final int uploadThreads;
    private final AtomicInteger storeRefreshIsFailing = new AtomicInteger(0);

    private final String eventRefresh;
    private final String eventRefreshed;
    private final String eventUpload;
    private final String eventDownloaded;

    private final HashSet<String> pendingUpload = new HashSet<>();
    private final HashSet<String> pendingDownload = new HashSet<>();

    private WorkerExecutor downloadExecutor = null;
    private WorkerExecutor uploadExecutor = null;

    private boolean isRefreshing = false;

    public CloudSyncVerticle(String name, ICloudStorage cloudStorage, ICloudStorage localStorage,
                             ICloudSync cloudSync, JsonObject jsonConfig) {
        this(name, cloudStorage, localStorage, cloudSync,
            jsonConfig.getInteger(Const.Config.CloudDownloadThreadsProp),
            jsonConfig.getInteger(Const.Config.CloudUploadThreadsProp));
    }

    public CloudSyncVerticle(String name, ICloudStorage cloudStorage, ICloudStorage localStorage,
                             ICloudSync cloudSync, int downloadThreads, int uploadThreads) {
        this.healthComponent = HealthManager.instance.registerComponent("cloudsync-" + name);
        this.healthComponent.setHealthStatus(false, "not started");

        this.name = name;
        this.cloudStorage = cloudStorage;
        this.localStorage = localStorage;
        this.cloudSync = cloudSync;
        this.downloadThreads = downloadThreads;
        this.uploadThreads = uploadThreads;

        String eventPrefix = "cloudsync." + this.name + ".";
        this.eventUpload = eventPrefix + "upload";
        this.eventDownloaded = eventPrefix + "downloaded";
        this.eventRefresh = eventPrefix + "refresh";
        this.eventRefreshed = eventPrefix + "refreshed";

        Gauge.builder("uid2_cloud_downloading", () -> this.pendingDownload.size())
            .tag("store", name)
            .description("gauge for how many s3 files are pending download")
            .register(Metrics.globalRegistry);

        Gauge.builder("uid2_cloud_uploading", () -> this.pendingUpload.size())
            .tag("store", name)
            .description("gauge for how many s3 files are pending upload")
            .register(Metrics.globalRegistry);

        this.counterRefreshed = Counter
            .builder("uid2_cloud_refreshed_total")
            .tag("store", name)
            .description("counter for how many times cloud storage files are refreshed")
            .register(Metrics.globalRegistry);

        this.counterRefreshSkipped = Counter
            .builder("uid2_cloud_refresh_skipped_total")
            .tag("store", name)
            .description("counter for how many times cloud storage refresh events are skipped due to in-progress refreshing")
            .register(Metrics.globalRegistry);

        this.counterRefreshFailures = Counter
            .builder("uid2_cloud_refresh_failures_total")
            .tag("store", name)
            .description("counter for number of " + name + " store refresh failures")
            .register(Metrics.globalRegistry);

        this.counterDownloaded = Counter
            .builder("uid2_cloud_downloaded_total")
            .tag("store", name)
            .description("counter for how many cloud files are downloaded")
            .register(Metrics.globalRegistry);

        this.counterUploaded = Counter
            .builder("uid2_cloud_uploaded_total")
            .tag("store", name)
            .description("counter for how many cloud files are uploaded")
            .register(Metrics.globalRegistry);

        this.counterDownloadFailures = Counter
            .builder("uid2_cloud_download_failures_total")
            .tag("store", name)
            .description("counter for how many cloud files downloads have failed")
            .register(Metrics.globalRegistry);

        this.counterUploadFailures = Counter
            .builder("uid2_cloud_upload_failures_total")
            .tag("store", name)
            .description("counter for how many cloud files uploads have failed")
            .register(Metrics.globalRegistry);

        this.gaugeConsecutiveRefreshFailures = Gauge
            .builder("uid2_cloud_downloaded_consecutive_refresh_failures", () -> this.storeRefreshIsFailing.get())
            .tag("store", name)
            .description("gauge for number of consecutive " + name + " store refresh failures")
            .register(Metrics.globalRegistry);

        this.downloadSuccessTimer = Metrics.timer("uid2_cloud_download_duration", "store_name", name, "status", "success");
        
        this.downloadFailureTimer = Metrics.timer("uid2_cloud_download_duration", "store_name", name, "status", "failure");
    }

    @Override
    public void start(Promise<Void> promise) {
        LOGGER.info("starting CloudSyncVerticle." + name);
        this.healthComponent.setHealthStatus(false, "still starting");

        this.downloadExecutor = vertx.createSharedWorkerExecutor("cloudsync-" + name + "-download-pool",
            this.downloadThreads);
        this.uploadExecutor = vertx.createSharedWorkerExecutor("cloudsync-" + name + "-upload-pool",
            this.uploadThreads);

        // handle refresh event
        vertx.eventBus().consumer(
            eventRefresh,
            o -> this.handleRefresh(o));

        // upload to cloud
        vertx.eventBus().<String>consumer(
            this.eventUpload,
            msg -> this.handleUpload(msg));

        cloudRefresh()
            .onFailure(t -> LOGGER.error("cloudRefresh failed: " + t.getMessage(), new Exception(t)))
            .onComplete(ar -> promise.handle(ar));

        promise.future()
            .onSuccess(v -> {
                LOGGER.info("started CloudSyncVerticle." + name);
                this.healthComponent.setHealthStatus(true);
            })
            .onFailure(e -> {
                LOGGER.error("failed starting CloudSyncVerticle." + name, new Exception(e));
                this.healthComponent.setHealthStatus(false, e.getMessage());
            });
    }

    @Override
    public void stop() {
        LOGGER.info("shutting down CloudSyncVerticle" + name);
    }

    public String eventRefresh() {
        return eventRefresh;
    }

    public String eventRefreshed() {
        return eventRefreshed;
    }

    public String eventUpload() {
        return eventUpload;
    }

    public String eventDownloaded() {
        return eventDownloaded;
    }

    private void handleRefresh(Message m) {
        cloudRefresh()
            .onSuccess(t -> this.storeRefreshIsFailing.set(0))
            .onFailure(t -> {
                this.counterRefreshFailures.increment();
                this.storeRefreshIsFailing.set(1);
                LOGGER.error("handleRefresh error: " + t.getMessage(), new Exception(t));
            });
    }

    private Future<Void> cloudRefresh() {
        if (this.isRefreshing) {
            LOGGER.debug("existing s3 refresh in-progress, skipping this one");
            counterRefreshSkipped.increment();
            return Future.succeededFuture();
        }

        Promise<Void> refreshPromise = Promise.promise();
        this.isRefreshing = true;
        vertx.executeBlocking(() -> {
            this.cloudRefreshEnsureInSync(refreshPromise, 0);
            return null;
        });

        return refreshPromise.future()
            .onComplete(v -> {
                this.isRefreshing = false;
                emitRefreshedEvent();
            });
            
    }

    // this is a blocking function
    private void cloudRefreshEnsureInSync(Promise<Void> refreshPromise, int iteration) {
        try {
            final List<Future> fs = new ArrayList<>();
            final boolean inSync = this.cloudSync.refresh(
                Instant.now(),
                this.cloudStorage,
                this.localStorage,
                downloads -> {
                    for (String d : downloads) {
                        fs.add(this.cloudDownloadFile(d));
                    }
                },
                deletes -> {
                    for (String d : deletes) {
                        fs.add(this.localDelete(d));
                    }
                });

            CompositeFuture.all(fs)
                .onFailure(e -> refreshPromise.fail(new Exception(e)))
                .onSuccess(v -> {
                    if (inSync) {
                        refreshPromise.complete();
                    }
                    else if (iteration >= 19) {
                        refreshPromise.fail(new Exception("Cannot full sync in 20 refresh iterations"));
                    } else {
                        if (iteration > 1) {
                            LOGGER.warn("Not synced in " + (iteration + 1) + " iterations, " + "" +
                                "last iteration contains " + fs.size() + " download/delete jobs");
                        }
                        cloudRefreshEnsureInSync(refreshPromise, iteration + 1);
                    }
                });
        } catch (CloudStorageException e) {
            refreshPromise.fail(new Exception(e));
        } catch (Exception e) {
            refreshPromise.fail(new Exception("unexpected error in cloudRefresh(): " + e.getMessage(), e));
        }
    }

    private Future<Void> localDelete(String fileToDelete) {
        boolean success;
        try {
            localStorage.delete(fileToDelete);
            success = true;
        } catch (Exception ex) {
            success = false;
        }
        LOGGER.info("delete " + fileToDelete + ": " + success);
        return Future.succeededFuture();
    }

    private void emitRefreshedEvent() {
        int iterations = (int) this.counterRefreshed.count();
        this.counterRefreshed.increment();
        LOGGER.trace("cloudsync " + this.name + " refreshed " + iterations);
        vertx.eventBus().publish(this.eventRefreshed, iterations);
    }

    private void handleUpload(Message<String> msg) {
        String fileToUpload = msg.body();
        if (fileToUpload == null) {
            LOGGER.warn("Received null filename for s3 upload, likely snapshot/log produce failed");
            msg.reply(false);
            return;
        } else if (this.pendingUpload.contains(fileToUpload)) {
            LOGGER.warn("Skip due to upload pending: " + fileToUpload);
            msg.reply(false);
            return;
        } else {
            LOGGER.info("Uploading: " + fileToUpload);
            this.pendingUpload.add(fileToUpload);
        }

        this.uploadExecutor.executeBlocking(() -> {
            this.cloudUploadBlocking(msg.body());
            return null;
        }).onComplete(ar -> {
            this.pendingUpload.remove(fileToUpload);
            this.handleAsyncResult(ar);
            msg.reply(ar.succeeded());

                // increase counter
                if (ar.succeeded()) {
                    this.counterUploaded.increment();
                } else {
                    this.counterUploadFailures.increment();
                }

                LOGGER.info("Upload result: " + ar.succeeded() + ", " + fileToUpload);
            });
    }

    private void cloudUploadBlocking(String fileToUpload) throws Exception {
        String cloudPath = this.cloudSync.toCloudPath(fileToUpload);
        try (InputStream localInput = this.localStorage.download(fileToUpload)) {
            this.cloudStorage.upload(localInput, cloudPath);
        }
    }

    private Future<Void> cloudDownloadFile(String s3Path) {
        if (s3Path == null) {
            LOGGER.warn("Received null path for s3 download");
            return Future.succeededFuture();
        } else if (this.pendingDownload.contains(s3Path)) {
            LOGGER.warn("Skip due to download pending: " + cloudStorage.mask(s3Path));
            return Future.succeededFuture();
        } else {
            LOGGER.trace("Downloading: " + cloudStorage.mask(s3Path));
            this.pendingDownload.add(s3Path);
        }

        Promise<Void> promise = Promise.promise();
        this.downloadExecutor.executeBlocking(() -> {
            this.cloudDownloadBlocking(s3Path);
            return null;
        }).onComplete(ar -> {
                this.pendingDownload.remove(s3Path);
                this.handleAsyncResult(ar);
                promise.complete();

                // increase counter, send event
                if (ar.succeeded()) {
                    vertx.eventBus().publish(this.eventDownloaded, this.cloudSync.toLocalPath(s3Path));
                    this.counterDownloaded.increment();
                } else {
                    this.counterDownloadFailures.increment();
                }

                LOGGER.trace("Download result: " + ar.succeeded() + ", " + cloudStorage.mask(s3Path));
            });

        return promise.future();
    }

    private void cloudDownloadBlocking(String s3Path) throws Exception {
        final long cloudDownloadStart = System.nanoTime();
        try {
            String localPath = this.cloudSync.toLocalPath(s3Path);
            try (InputStream cloudInput = this.cloudStorage.download(s3Path)) {
                final long cloudDownloadEnd = System.nanoTime();
                final long cloudDownloadTimeMs = (cloudDownloadEnd - cloudDownloadStart) / 1_000_000;
                
                this.localStorage.upload(cloudInput, localPath);
                
                downloadSuccessTimer.record(java.time.Duration.ofMillis(cloudDownloadTimeMs));
                LOGGER.info("S3 download completed: {} in {} ms", cloudStorage.mask(s3Path), cloudDownloadTimeMs);
            }
        } catch (Exception ex) {
            final long cloudDownloadEnd = System.nanoTime();
            final long cloudDownloadTimeMs = (cloudDownloadEnd - cloudDownloadStart) / 1_000_000;
            
            downloadFailureTimer.record(java.time.Duration.ofMillis(cloudDownloadTimeMs));            
            // Be careful as the s3Path may contain the pre-signed S3 token, so do not log the whole path
            LOGGER.error("download error: " + ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private void handleAsyncResult(AsyncResult ar) {
        if (ar.failed()) {
            Throwable ex = ar.cause();
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
