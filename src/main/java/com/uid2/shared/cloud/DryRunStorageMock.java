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

package com.uid2.shared.cloud;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DryRunStorageMock implements ICloudStorage {
    private static final List<String> EMPTY_LIST = new ArrayList<>();
    private static final InputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

    private boolean isVerbose;
    public DryRunStorageMock(boolean isVerbose) {
        this.isVerbose = isVerbose;
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        logMessage("upload", "(" + localPath + ", " + cloudPath + ")");
        return;
    }

    @Override
    public void upload(InputStream inputStream, String cloudPath) throws CloudStorageException {
        logMessage("upload", "(stream, " + cloudPath + ")");
        return;
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        logMessage("download", "(" + cloudPath + ")");
        return EMPTY_STREAM;
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        logMessage("delete", "(" + cloudPath + ")");
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        logMessage("delete", "(..." + cloudPaths.size() + " paths...)");
        for (String p : cloudPaths) delete(p);
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        logMessage("delete", "(" + prefix + ")");
        return EMPTY_LIST;
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        logMessage("preSignUrl", "(" + cloudPath + ")");
        try {
            return new URL("mock://" + cloudPath);
        } catch (Exception e) {
            throw new CloudStorageException("dryRun::preSignUrl error: " + e.getMessage(), e);
        }
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {
        logMessage("setPreSignedUrlExpiry", "(" + expiry + ")");
    }

    @Override
    public String mask(String cloudPath) {
        logMessage("mask", "(" + cloudPath + ")");
        return cloudPath;
    }

    private void logMessage(String method, String msg) {
        if (isVerbose) {
            System.out.println(method + " " + msg);
        }
    }
}
