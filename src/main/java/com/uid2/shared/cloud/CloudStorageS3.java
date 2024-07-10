package com.uid2.shared.cloud;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CloudStorageS3 implements TaggableCloudStorage {
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
        } else {
            this.s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(creds))
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region))
                    .enablePathStyleAccess()
                    .build();
        }
        this.bucket = bucket;
    }

    public CloudStorageS3(String region, String bucket, String s3Endpoint) {
        this.bucket = bucket;

        // In theory `new InstanceProfileCredentialsProvider()` or even omitting credentials provider should work,
        // but for some unknown reason it doesn't. The credential it provides look realistic, but are not valid.
        // After a lot of experimentation and help of Abu Abraham and Isaac Wilson the only working solution we've
        // found was to explicitly extract env vars populated by the service account from the role and to
        // manually set it on the credentials provider.
        WebIdentityTokenCredentialsProvider credentialsProvider = WebIdentityTokenCredentialsProvider.builder()
                .roleArn(System.getenv("AWS_ROLE_ARN"))
                .webIdentityTokenFile(System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE"))
                .build();

        if (s3Endpoint.isEmpty()) {
            this.s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(credentialsProvider)
                    .withRegion(region)
                    .build();
        } else {
            this.s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(credentialsProvider)
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region))
                    .enablePathStyleAccess()
                    .build();

        }
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        try {
            File file = new File(localPath);
            var putResult = this.s3.putObject(bucket, cloudPath, file);
            this.checkVersioningEnabled(putResult);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        try {
            var putResult = this.s3.putObject(bucket, cloudPath, input, null);
            this.checkVersioningEnabled(putResult);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public void upload(String localPath, String cloudPath, Map<String, String> tags) throws CloudStorageException {
        try {
            File file = new File(localPath);
            PutObjectRequest putRequest = new PutObjectRequest(bucket, cloudPath, file);
            List<Tag> newTags = new ArrayList<>();
            tags.forEach((k, v) -> newTags.add(new Tag(k, v)));
            putRequest.setTagging(new ObjectTagging(newTags));
            var putResult = this.s3.putObject(putRequest);
            this.checkVersioningEnabled(putResult);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public void upload(InputStream input, String cloudPath, Map<String, String> tags) throws CloudStorageException {
        try {
            PutObjectRequest putRequest = new PutObjectRequest(bucket, cloudPath, input, null);
            List<Tag> newTags = new ArrayList<>();
            tags.forEach((k, v) -> newTags.add(new Tag(k, v)));
            putRequest.setTagging(new ObjectTagging(newTags));
            var putResult = this.s3.putObject(putRequest);
            this.checkVersioningEnabled(putResult);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        try {
            S3Object obj = this.s3.getObject(bucket, cloudPath);
            return obj.getObjectContent();
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                throw new CloudStorageException("The specified key does not exist: " + e.getClass().getSimpleName() + ": " + bucket);
            } else {
                throw new CloudStorageException("s3 get error: " + e.getClass().getSimpleName() + ": " + bucket);
            }
        } catch (Throwable t) {
            // Do not log the message or the original exception as that may contain the pre-signed url
            throw new CloudStorageException("s3 get error: " + t.getClass().getSimpleName() + ": " + bucket);
        }
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        try {
            this.s3.deleteObject(bucket, cloudPath);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 delete error: " + t.getMessage(), t);
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

    @Override
    public void setTags(String cloudPath, Map<String, String> tags) throws CloudStorageException {
        try {
            if (this.s3.doesObjectExist(this.bucket, cloudPath)) {
                List<Tag> newTags = new ArrayList<>();
                tags.forEach((k, v) -> newTags.add(new Tag(k, v)));

                this.s3.setObjectTagging(new SetObjectTaggingRequest(
                        this.bucket,
                        cloudPath,
                        new ObjectTagging(newTags)));
            } else {
                LOGGER.warn("CloudPath: {} does not exist in bucket: {}. Tags not set", cloudPath, this.bucket);
            }
        } catch (Throwable t) {
            throw new CloudStorageException("s3 set tags error", t);
        }
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

    private void checkVersioningEnabled(PutObjectResult putObjectResult) {
        try {
            String versionId = putObjectResult.getVersionId();
            if (versionId == null || versionId.isEmpty()) {
                LOGGER.warn(
                        "Bucket: {} in Region: {} does not have versioning configured. There is a potential for data loss",
                        this.bucket,
                        this.s3.getRegionName());
            } else {
                LOGGER.info(
                        "Bucket: {} in Region: {} has versioning configured.",
                        this.bucket,
                        this.s3.getRegionName());
            }
        } catch (Throwable t) {
            // don't want this to fail when writing, but should be logged
            LOGGER.error(
                    String.format("Unable to determine if the S3 bucket: %s has versioning enabled", this.bucket),
                    t);
        }
    }
}
