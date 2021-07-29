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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MemCachedStorage implements ICloudStorage {
    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        throw new UnsupportedOperationException("MemCachedStorage::upload(localPath,cloudPath) method is not supported");
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        try {
            cache.put(cloudPath, Utils.streamToByteArray(input));
        } catch (IOException e) {
            throw new CloudStorageException("MemCachedStorage::upload error: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        return new ByteArrayInputStream(cache.get(cloudPath));
    }

    // more effective way to return the cache byte[] array directly
    public byte[] getBytes(String cloudPath) {
        return cache.get(cloudPath);
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        cache.remove(cloudPath);
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        for (String p : cloudPaths) delete(p);
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        return Collections.list(cache.keys()).stream()
            .filter(f -> f.startsWith(prefix))
            .collect(Collectors.toList());
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        throw new UnsupportedOperationException("MemCachedStorage::preSignUrl method is not supported");
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {
    }

    @Override
    public String mask(String cloudPath) {
        return cloudPath;
    }
}
