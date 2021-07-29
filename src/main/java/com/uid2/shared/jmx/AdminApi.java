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

package com.uid2.shared.jmx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
