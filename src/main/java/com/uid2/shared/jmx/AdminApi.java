package com.uid2.shared.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdminApi implements AdminApiMBean {
    public final static AdminApi instance = new AdminApi();
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminApi.class);
    private AtomicReference<Boolean> _publishApiMetrics = new AtomicReference<>(true);
    private AtomicReference<Boolean> _captureRequests = new AtomicReference<>(false);
    private AtomicReference<Boolean> _captureFailureOnly = new AtomicReference<>(true);
    private AtomicReference<Integer> _maxCapturedRequests = new AtomicReference<>(100);
    private AtomicReference<Pattern> _apiContactPattern = new AtomicReference<>(Pattern.compile(".*", Pattern.CASE_INSENSITIVE));
    private List<Queue<String>> _listOfQueues = new ArrayList<>();

    @Override
    public boolean getPublishApiMetrics() {
        return _publishApiMetrics.get();
    }

    @Override
    public void setPublishApiMetrics(boolean publishApiMetrics) {
        _publishApiMetrics.set(publishApiMetrics);
    }

    @Override
    public boolean getCaptureRequests() {
        return _captureRequests.get();
    }

    @Override
    public void setCaptureRequests(boolean isCapture) {
        _captureRequests.set(isCapture);
    }

    @Override
    public boolean getCaptureFailureOnly() {
        return _captureFailureOnly.get();
    }

    @Override
    public void setCaptureFailureOnly(boolean isFailureOnly) {
        _captureFailureOnly.set(isFailureOnly);
    }

    @Override
    public int getMaxCapturedRequests() {
        return _maxCapturedRequests.get();
    }

    @Override
    public void setMaxCapturedRequests(int maxRequests) {
        _maxCapturedRequests.set(maxRequests);
    }

    @Override
    public String getApiContactRegex() {
        return _apiContactPattern.get().pattern();
    }

    @Override
    public void setApiContactRegex(String pattern) {
        _apiContactPattern.set(Pattern.compile(pattern));
    }

    @Override
    public String dumpCaptureRequests() {
        String[] arrayOfRequests = getCapturedRequests();

        try {
            Path outf = Files.createTempFile(Paths.get("/tmp"), "captured-reqs-", ".txt");
            FileWriter fw = new FileWriter(outf.toString());
            for (String req : arrayOfRequests) {
                fw.write(req);
            }

            fw.close();
            return outf.toString();
        } catch (IOException e) {
            LOGGER.error("dump error: ", e);
            return null;
        }
    }

    @Override
    public synchronized String[] getCapturedRequests() {
        List<String> list = _listOfQueues.stream().flatMap(Collection::stream).collect(Collectors.toList());
        return list.toArray(new String[list.size()]);
    }

    public Pattern getApiContactPattern() {
        return _apiContactPattern.get();
    }

    public synchronized Queue<String> allocateCapturedRequestQueue() {
        Queue<String> queue = new ConcurrentLinkedDeque<>();
        _listOfQueues.add(queue);
        return queue;
    }
}
