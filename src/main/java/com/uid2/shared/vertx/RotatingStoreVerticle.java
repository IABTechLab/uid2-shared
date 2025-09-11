package com.uid2.shared.vertx;

import com.uid2.shared.health.HealthComponent;
import com.uid2.shared.health.HealthManager;
import com.uid2.shared.store.reader.IMetadataVersionedStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RotatingStoreVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingStoreVerticle.class);
    private final String storeName;
    private final HealthComponent healthComponent;
    private final Counter counterStoreRefreshTimeMs;
    private final Counter counterStoreRefreshed;
    private final Gauge gaugeStoreVersion;
    private final Gauge gaugeStoreEntryCount;
    private final Gauge gaugeConsecutiveRefreshFailures;
    private final Counter counterStoreRefreshFailures;
    private final IMetadataVersionedStore versionedStore;
    private final AtomicLong latestVersion = new AtomicLong(-1L);
    private final AtomicLong latestEntryCount = new AtomicLong(-1L);
    private final AtomicInteger storeRefreshIsFailing = new AtomicInteger(0);

    private final long refreshIntervalMs;

    public RotatingStoreVerticle(String storeName, long refreshIntervalMs, IMetadataVersionedStore versionedStore) {
        this.healthComponent = HealthManager.instance.registerComponent(storeName + "-rotator");
        this.healthComponent.setHealthStatus(false, "not started");

        this.storeName = storeName;
        this.counterStoreRefreshed = Counter
            .builder("uid2_config_store_refreshed_total")
            .tag("store", storeName)
            .description("counter for how many times " + storeName + " store is refreshed")
            .register(Metrics.globalRegistry);
        this.counterStoreRefreshTimeMs = Counter
            .builder("uid2_config_store_refreshtime_ms_total")
            .tag("store", storeName)
            .description("counter for total time (ms) " + storeName + " store spend in refreshing")
            .register(Metrics.globalRegistry);
        this.counterStoreRefreshFailures = Counter
            .builder("uid2_config_store_refresh_failures_total")
            .tag("store", storeName)
            .description("counter for number of " + storeName + " store refresh failures")
            .register(Metrics.globalRegistry);
        this.gaugeStoreVersion = Gauge
            .builder("uid2_config_store_version", () -> this.latestVersion.get())
            .tag("store", storeName)
            .description("gauge for " + storeName + " store version")
            .register(Metrics.globalRegistry);
        this.gaugeStoreEntryCount = Gauge
            .builder("uid2_config_store_entry_count", () -> this.latestEntryCount.get())
            .tag("store", storeName)
            .description("gauge for " + storeName + " store total entry count")
            .register(Metrics.globalRegistry);
        this.gaugeConsecutiveRefreshFailures = Gauge
            .builder("uid2_config_store_consecutive_refresh_failures", () -> this.storeRefreshIsFailing.get())
            .tag("store", storeName)
            .description("gauge for number of consecutive " + storeName + " store refresh failures")
            .register(Metrics.globalRegistry);
        this.versionedStore = versionedStore;
        this.refreshIntervalMs = refreshIntervalMs;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        this.healthComponent.setHealthStatus(false, "still starting");
        this.startRefresh(startPromise);
    }

    private void startRefresh(Promise<Void> promise) {
        LOGGER.info("Starting " + this.storeName + " loading");
        LOGGER.info("Starting {} store initial S3 loading", this.storeName);
        final long startupRefreshStart = System.nanoTime();

        vertx.executeBlocking(p -> {
            try {
                this.refresh();
                p.complete();
            } catch (Exception e) {
                p.fail(e);
            }
        }, ar -> {
            final long startupRefreshEnd = System.nanoTime();
            final long startupRefreshTimeMs = (startupRefreshEnd - startupRefreshStart) / 1000000;

            if (ar.succeeded()) {
                this.healthComponent.setHealthStatus(true);
                promise.complete();
                LOGGER.info("Successful " + this.storeName + " loading. Starting Background Refresh");
                LOGGER.info("Successful {} store initial S3 loading in {} ms. Starting Background Refresh", 
                    this.storeName, startupRefreshTimeMs);

                // Record startup-specific S3 store loading metric
                Gauge.builder("uid2_operator_startup_store_refresh_duration_ms", () -> (double) startupRefreshTimeMs)
                    .description("Time taken for initial store S3 refresh during startup")
                    .tags("store_name", this.storeName)
                    .register(Metrics.globalRegistry);

                this.startBackgroundRefresh();
            } else {
                this.healthComponent.setHealthStatus(false, ar.cause().getMessage());
                LOGGER.error("Failed " + this.storeName + " loading. Trying again in " + refreshIntervalMs + "ms", ar.cause());
                LOGGER.error("Failed {} store initial S3 loading after {} ms. Trying again in {} ms", 
                    this.storeName, startupRefreshTimeMs, refreshIntervalMs, ar.cause());
                vertx.setTimer(refreshIntervalMs, id -> this.startRefresh(promise));
            }
        });
    }

    private void startBackgroundRefresh() {
        vertx.setPeriodic(this.refreshIntervalMs, (id) -> {
            final long start = System.nanoTime();
            vertx.executeBlocking(promise -> {
                    try {
                        this.refresh();
                        promise.complete();
                    } catch (Exception e) {
                        promise.fail(e);
                    }
                },
                asyncResult -> {
                    final long end = System.nanoTime();
                    final long elapsed = ((end - start) / 1000000);
                    this.counterStoreRefreshTimeMs.increment(elapsed);
                    if (asyncResult.failed()) {
                        this.counterStoreRefreshFailures.increment();
                        this.storeRefreshIsFailing.set(1);
                        LOGGER.error("Failed to load " + this.storeName + ", " + elapsed + " ms", asyncResult.cause());
                    } else {
                        this.counterStoreRefreshed.increment();
                        this.storeRefreshIsFailing.set(0);
                        LOGGER.trace("Successfully refreshed " + this.storeName + ", " + elapsed + " ms");
                    }
                }
            );
        });
    }

    public synchronized void refresh() throws Exception {
        final JsonObject metadata = this.versionedStore.getMetadata();
        final long version = this.versionedStore.getVersion(metadata);
        if (version > this.latestVersion.get()) {
            long entryCount = this.versionedStore.loadContent(metadata);
            this.latestVersion.set(version);
            this.latestEntryCount.set(entryCount);
            LOGGER.info("Successfully loaded " + this.storeName + " version " + version);
        }
    }
}