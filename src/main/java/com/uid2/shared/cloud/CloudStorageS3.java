package com.uid2.shared.cloud;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CloudStorageS3 implements ICloudStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageS3.class);

    private final AmazonS3 s3;
    private final String bucket;
    private long preSignedUrlExpiryInSeconds = 3600;

    public CloudStorageS3(String accessKeyId, String secretAccessKey, String region, String bucket, String s3Endpoint) {
        // Reading https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        AWSCredentials creds = new BasicAWSCredentials(
            accessKeyId,
            secretAccessKey);
        if (s3Endpoint.isEmpty()) {
            this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .build();
        }
        else {
            this.s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region))
                .enablePathStyleAccess()
                .build();
        }
        this.bucket = bucket;
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        try {
            File file = new File(localPath);
            this.s3.putObject(bucket, cloudPath, file);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        try {
            this.s3.putObject(bucket, cloudPath, input, null);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        try {
            S3Object obj = this.s3.getObject(bucket, cloudPath);
            return obj.getObjectContent();
        } catch (Throwable t) {
            throw new CloudStorageException("s3 get error: " + t.getMessage(), t);
        }
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        try {
            this.s3.deleteObject(bucket, cloudPath);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 get error: " + t.getMessage(), t);
        }
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        if (cloudPaths.size() == 0) return;
        if (cloudPaths.size() <= 1000) {
            deleteInternal(cloudPaths);
            return;
        }

        List<String> pathList = new ArrayList<>();
        int i = 0;
        final int len = cloudPaths.size();
        for (String p : cloudPaths) {
            pathList.add(p);
            ++i;
            if (pathList.size() == 1000 || i == len) {
                deleteInternal(pathList);
                pathList.clear();
            }
        }

        if (pathList.size() != 0) throw new IllegalStateException();
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        try {
            ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(bucket)
                .withPrefix(prefix);
            ListObjectsV2Result result = null;
            List<S3ObjectSummary> objects = null;

            int reqCount = 0;
            List<String> s3Paths = new ArrayList<>();
            do {
                result = this.s3.listObjectsV2(req);
                objects = result.getObjectSummaries();

                LOGGER.trace("s3 listobjectv2 request for " + prefix + " " + reqCount++ + ", returned keycount " + result.getKeyCount());
                if (objects.size() > 0) {
                    LOGGER.trace("--> 1st key = " + objects.get(0).getKey());
                }

                for (S3ObjectSummary os : objects) {
                    s3Paths.add(os.getKey());
                }

                if (result.isTruncated()) {
                    req.setContinuationToken(result.getNextContinuationToken());
                    LOGGER.trace("--> truncated, continuationtoken: " + req.getContinuationToken());
                }
            } while (result.isTruncated());
            return s3Paths;
        } catch (Throwable t) {
            throw new CloudStorageException("s3 list error: " + t.getMessage(), t);
        }
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        try {
            // Set the presigned URL to expire after one hour.
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += preSignedUrlExpiryInSeconds * 1000;
            expiration.setTime(expTimeMillis);

            // Generate the presigned URL.
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(this.bucket, cloudPath)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            URL url = this.s3.generatePresignedUrl(generatePresignedUrlRequest);
            return url;
        } catch (Throwable t) {
            throw new CloudStorageException("s3 preSignUrl error: " + t.getMessage(), t);
        }
    }

    @Override
    public void setPreSignedUrlExpiry(long expiryInSeconds) {
        this.preSignedUrlExpiryInSeconds = expiryInSeconds;
    }

    @Override
    public String mask(String cloudPath) {
        return cloudPath;
    }

    private void deleteInternal(Collection<String> cloudPaths) throws CloudStorageException {
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (String p : cloudPaths) {
            keys.add(new DeleteObjectsRequest.KeyVersion(p));
        }
        DeleteObjectsRequest dor = new DeleteObjectsRequest(bucket)
            .withKeys(keys)
            .withQuiet(false);
        try {
            this.s3.deleteObjects(dor);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 get error: " + t.getMessage(), t);
        }
    }
}
