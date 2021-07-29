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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LocalStorageMock implements ICloudStorage {
    private final String defaultWorkingDir;
    private static final List<String> EMPTY_LIST = new ArrayList<>();

    public LocalStorageMock() {
        this(System.getProperty("user.dir"));
    }

    public LocalStorageMock(String defaultWorkingDir) {
        this.defaultWorkingDir = defaultWorkingDir;
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        Path src = Paths.get(localPath);
        Path dst = getAbsolutePath(cloudPath);

        try {
            Utils.ensureDirectoryExists(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CloudStorageException("local upload(copy) error: " + e.getMessage(), e);
        }
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        Path dst = getAbsolutePath(cloudPath);
        try {
            Utils.ensureDirectoryExists(dst.getParent());
            Files.copy(input, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CloudStorageException("local upload(copy) error: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        Path p = getAbsolutePath(cloudPath);

        try {
            return new FileInputStream(p.toString());
        } catch (FileNotFoundException e) {
            throw new CloudStorageException("local download(open) error: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        Path p = getAbsolutePath(cloudPath);

        try {
            Files.delete(p);
        } catch (Exception e) {
            throw new CloudStorageException("local download(open) error: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        for (String p : cloudPaths) delete(p);
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        // Unlike other storage implementation, local storage doesn't return relative paths under the root
        // It will always return absolute paths
        try {
            Path prefixPath = getAbsolutePath(prefix);
            if (!Files.exists(prefixPath)) return EMPTY_LIST;
            else return Files.find(prefixPath, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                .map(p -> p.toString())
                .collect(Collectors.toList());
        }
        catch (IOException e) {
            throw new CloudStorageException("local list error: " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        try {
            return getAbsolutePath(cloudPath).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {
    }

    @Override
    public String mask(String cloudPath) {
        return cloudPath;
    }

    // Local Storage is special, both relative path and absolute path works.
    // Internally we convert relative path to absolute path when relative path is provided.
    private Path getAbsolutePath(String cloudPath) {
        if (cloudPath.charAt(0) == '/')
            return Paths.get(cloudPath);
        else if (Utils.IsWindows && cloudPath.length() > 1 && cloudPath.charAt(1) == ':')
            return Paths.get(cloudPath);
        else
            return Paths.get(this.defaultWorkingDir, cloudPath);
    }
}
