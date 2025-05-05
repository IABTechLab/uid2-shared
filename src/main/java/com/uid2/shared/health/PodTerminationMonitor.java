package com.uid2.shared.health;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PodTerminationMonitor implements IHealthComponent{
    private static final Logger LOGGER = LoggerFactory.getLogger(PodTerminationMonitor.class);
    private AtomicReference<Boolean> cachedPodTerminating = new AtomicReference<>(false);
    private long lastPodCheckTime = 0;
    private long fileCheckIntervalMs = 3000;


    public PodTerminationMonitor() {}

    public PodTerminationMonitor(long fileCheckIntervalMs) {
        this.fileCheckIntervalMs = fileCheckIntervalMs;
    }

    @Override
    public String name() {
        return "PodTerminationMonitor";
    }

    @Override
    public String reason() {
        return checkPodTerminating() ? "Pod is terminating" : "";
    }

    @Override
    public boolean isHealthy() {
        return !checkPodTerminating();
    }

    private boolean checkPodTerminating() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPodCheckTime >= fileCheckIntervalMs) {
            File file = new File(File.separator + "app" + File.separator + "pod_terminating");
            boolean newStatus = file.exists();
            if (newStatus) {
                LOGGER.info("pod will terminate soon");
            }
            cachedPodTerminating.set(newStatus);
            lastPodCheckTime = currentTime;
        }
        return cachedPodTerminating.get();
    }
}
