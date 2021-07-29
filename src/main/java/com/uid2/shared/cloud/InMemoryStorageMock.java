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

import com.uid2.shared.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryStorageMock implements ICloudStorage {

    public Map<String, byte[]> localFileSystemMock = new HashMap<>();
    public Map<String, byte[]> cloudFileSystemMock = new HashMap<>();

    public void save(byte[] content, String fullPath) {
        localFileSystemMock.put(fullPath, content);
    }

    public byte[] load(String fullPath) {
        return localFileSystemMock.get(fullPath);
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        cloudFileSystemMock.put(cloudPath, localFileSystemMock.get(localPath));
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        try {
            cloudFileSystemMock.put(cloudPath, Utils.readToEndAsString(input).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CloudStorageException("cannot read all contents as string from inputstream", e);
        }
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        byte[] content = cloudFileSystemMock.get(cloudPath).clone();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        return inputStream;
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        cloudFileSystemMock.remove(cloudPath);
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        for (String p : cloudPaths) {
            delete(p);
        }
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        return cloudFileSystemMock.keySet()
                .stream()
                .filter(x -> x.startsWith(prefix))
                .collect(Collectors.toList());
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        try {
            return new URL("mock://" + cloudPath);
        } catch (Exception e) {
            throw new CloudStorageException("cannot generate url", e);
        }
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {

    }

    @Override
    public String mask(String cloudPath) {
        return cloudPath;
    }
}
