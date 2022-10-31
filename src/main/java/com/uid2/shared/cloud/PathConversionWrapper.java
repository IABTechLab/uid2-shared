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
