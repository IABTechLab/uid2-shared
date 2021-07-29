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
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PathConversionWrapper implements ICloudStorage {
    private final ICloudStorage backend;
    private final Function<String, String> forwardConverter;
    private final Function<String, String> backwardConverter;

    public PathConversionWrapper(ICloudStorage backend,
                                 Function<String, String> forwardConverter,
                                 Function<String, String> backwardConverter) {
        this.backend = backend;
        this.forwardConverter = forwardConverter;
        this.backwardConverter = backwardConverter;
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        // cloudPath is provided by local, apply backwardConverter
        backend.upload(localPath, backwardConverter.apply(cloudPath));
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        // cloudPath is provided by local, apply backwardConverter
        backend.upload(input, backwardConverter.apply(cloudPath));
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        // cloudPath is provided by local, apply backwardConverter
        return backend.download(backwardConverter.apply(cloudPath));
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        // cloudPath is provided by local, apply backwardConverter
        backend.delete(backwardConverter.apply(cloudPath));
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        // cloudPaths are provided by local, apply backwardConverter
        Collection<String> convertedPaths = cloudPaths.stream()
            .map(p -> backwardConverter.apply(p))
            .collect(Collectors.toList());
        backend.delete(convertedPaths);
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        // prefix is provided by local, apply backwardConverter
        // list is provided by cloud, apply forwardConverter
        return backend.list(backwardConverter.apply(prefix)).stream()
            .map(f -> forwardConverter.apply(f))
            .collect(Collectors.toList());
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        // cloudPath is provided by local, apply backwardConverter
        return backend.preSignUrl(backwardConverter.apply(cloudPath));
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {
        backend.setPreSignedUrlExpiry(expiry);
    }

    @Override
    public String mask(String cloudPath) {
        // no conversion needed before calling backend.mask()
        return backend.mask(cloudPath);
    }
}
