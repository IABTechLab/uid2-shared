package com.uid2.shared.store;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class CloudPath {
    private final Path path;

    public CloudPath(String path) {
        this(Paths.get(path));
    }

    public CloudPath(Path path) {
        this.path = path;
    }

    public CloudPath getParent() {
        return new CloudPath(path.getParent());
    }

    public CloudPath resolve(String other) {
        return new CloudPath(path.resolve(other));
    }

    public CloudPath resolve(CloudPath other) {
        return new CloudPath(path.resolve(other.path));
    }

    public CloudPath getFileName() {
        return new CloudPath(path.getFileName());
    }

    @Override
    public String toString() {
        String path = this.path.toString();

        // Java's Path always converts everything to path standards of the OS it's running on.
        // The paths are actually S3 paths so should always be Linux style, so we need to convert them when on Windows
        return isWindows ? path.replace("\\", "/") : path ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudPath cloudPath = (CloudPath) o;
        return path.equals(cloudPath.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
}
