package com.uid2.shared.store;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class CloudPath {
    private final URI uriBase;
    private final Path path;
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    public CloudPath(String path) {
        if (path.contains("://")) {
            URI uri = URI.create(path);
            this.uriBase = uri;
            this.path = Paths.get(uri.getPath());
        } else {
            this.uriBase = null;
            this.path = Paths.get(path);
        }
    }

    public CloudPath(Path path) {
        this.path = path;
        this.uriBase = null;
    }

    public CloudPath(URI uri) {
        this.uriBase = uri;
        this.path = Paths.get(uri.getPath());
    }

    private CloudPath(URI base, Path path) {
        this.uriBase = base;
        this.path = path;
    }

    public CloudPath getParent() {
        return new CloudPath(uriBase, path.getParent());
    }

    public CloudPath resolve(String other) {
        return new CloudPath(uriBase, path.resolve(other));
    }

    public CloudPath resolve(CloudPath other) {
        return new CloudPath(uriBase, path.resolve(other.path));
    }

    public CloudPath getFileName() {
        return new CloudPath(null, path.getFileName());
    }

    @Override
    public String toString() {
        String path = this.path.toString();

        // Java's Path always converts everything to path standards of the OS it's running on.
        // The paths are actually S3 paths so should always be Linux style, so we need to convert them when on Windows
        if (isWindows) {
            path = path.replace("\\", "/");
        };

        if (uriBase != null) {
            return path.isEmpty() ?
                    uriBase.getScheme() + "://" + uriBase.getAuthority() :
                    uriBase.resolve("/").resolve(path).normalize().toString();
        }

        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudPath cloudPath = (CloudPath) o;

        // comparing file path to uri
        if ((uriBase == null) != (cloudPath.uriBase == null))
            return false;

        if (uriBase == null) {
            return path.equals(cloudPath.path);
        } else {
            // we only use `scheme://authority` part of the uriBase, hence 'Base'
            return uriBase.getScheme().equals(cloudPath.uriBase.getScheme()) &&
                    uriBase.getAuthority().equals(cloudPath.uriBase.getAuthority()) &&
                    path.equals(cloudPath.path);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(uriBase, path);
    }
}
