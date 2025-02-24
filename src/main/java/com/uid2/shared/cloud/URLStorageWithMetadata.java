package com.uid2.shared.cloud;

import java.io.InputStream;
import java.net.HttpURLConnection;
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
            HttpURLConnection httpConn;
            
            if (this.proxy != null) {
                httpConn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                httpConn = (HttpURLConnection) url.openConnection();
            }
            
            int responseCode = httpConn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return httpConn.getInputStream();
            } else {
                throw new CloudStorageException("Cannot download required files, HTTP response code " + responseCode 
                + ", please visit UID2 guides for more details");
            }
        }
        catch (CloudStorageException e) {
            // Directly rethrow without wrapping again
            throw e;
        } 
        catch (Throwable t) {
            // Do not log the original exception as it may contain sensitive information such as the pre-signed URL
            throw new CloudStorageException("Cannot download required files, exception: " + t.getClass().getSimpleName() +
                ", please visit UID2 guides for more details");
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
