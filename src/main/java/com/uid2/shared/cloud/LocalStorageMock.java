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
                .map(Path::toString)
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
