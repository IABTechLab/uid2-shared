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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class URLStorageWithMetadata implements ICloudStorage {
    private final Proxy proxy;

    public URLStorageWithMetadata() {
        this(null);
    }

    public URLStorageWithMetadata(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        // this is a read-only cloud storage
        throw new UnsupportedOperationException("URLStorageWithMetadata::upload method is not supported");
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        // this is a read-only cloud storage
        throw new UnsupportedOperationException("URLStorageWithMetadata::upload method is not supported");
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        try {
            URL url = new URL(cloudPath);

            if (this.proxy != null) {
                return url.openConnection(proxy).getInputStream();
            } else {
                return url.openStream();
            }
        } catch (Throwable t) {
            throw new CloudStorageException("url download error: " + t.getMessage(), t);
        }
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        // this is a read-only cloud storage
        throw new UnsupportedOperationException("URLStorageWithMetadata::upload method is not supported");
    }

    @Override
    public void delete(Collection<String> cloudPath) throws CloudStorageException {
        // this is a read-only cloud storage
        throw new UnsupportedOperationException("URLStorageWithMetadata::upload method is not supported");
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        // TODO improve prefix matching
        return extractListFromMetadata().stream()
            .filter(url -> url.contains(prefix))
            .collect(Collectors.toList());
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        throw new UnsupportedOperationException("URLStorageWithMetadata::preSignUrl method is not supported");
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {
        throw new UnsupportedOperationException("PreSignedURLStorage::setPreSignedUrlExpiry method is not supported");
    }

    @Override
    public String mask(String cloudPath) {
        try {
            URL url = new URL(cloudPath);
            return cloudPath.replace("?" + url.getQuery(), "");
        } catch (MalformedURLException e) {
            return cloudPath;
        }
    }

    protected abstract List<String> extractListFromMetadata() throws CloudStorageException;
}
