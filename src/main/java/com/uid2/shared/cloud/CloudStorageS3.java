package com.uid2.shared.cloud;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseInputStream;
import java.time.Duration;
import java.net.URI;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CloudStorageS3 implements TaggableCloudStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageS3.class);

    private final S3Client s3;
    private final String bucket;
    private final boolean verbose;
    private long preSignedUrlExpiryInSeconds = 3600;

    public CloudStorageS3(String accessKeyId, String secretAccessKey, String region, String bucket, String s3Endpoint, boolean verbose) {
        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        
        if (s3Endpoint.isEmpty()) {
            this.s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                    .build();
        } else {
            this.s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                    .endpointOverride(URI.create(s3Endpoint))
                    .forcePathStyle(true)
                    .build();
        }
        this.bucket = bucket;
        this.verbose = verbose;
    }

    public CloudStorageS3(String accessKeyId, String secretAccessKey, String region, String bucket, String s3Endpoint) {
        this(accessKeyId, secretAccessKey, region, bucket, s3Endpoint, false);
    }

    public CloudStorageS3(String region, String bucket, String s3Endpoint) {
        // In theory `new InstanceProfileCredentialsProvider()` or even omitting credentials provider should work,
        // but for some unknown reason it doesn't. The credential it provides look realistic, but are not valid.
        // After a lot of experimentation and help of Abu Abraham and Isaac Wilson the only working solution we've
        // found was to explicitly extract env vars populated by the service account from the role and to
        // manually set it on the credentials provider.
        WebIdentityTokenFileCredentialsProvider credentialsProvider = WebIdentityTokenFileCredentialsProvider.builder()
                .roleArn(System.getenv("AWS_ROLE_ARN"))
                .webIdentityTokenFile(Paths.get(System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE")))
                .build();

        if (s3Endpoint.isEmpty()) {
            this.s3 = S3Client.builder()
                    .credentialsProvider(credentialsProvider)
                    .region(Region.of(region))
                    .build();
        } else {
            this.s3 = S3Client.builder()
                    .credentialsProvider(credentialsProvider)
                    .region(Region.of(region))
                    .endpointOverride(URI.create(s3Endpoint))
                    .forcePathStyle(true)
                    .build();
        }
        this.bucket = bucket;
        this.verbose = false;
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(cloudPath)
                    .build();
            var putResult = this.s3.putObject(putRequest, RequestBody.fromFile(Paths.get(localPath)));
            this.checkVersioningEnabled(putResult);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(cloudPath)
                    .build();
            var putResult = this.s3.putObject(putRequest, RequestBody.fromInputStream(input, input.available()));
            this.checkVersioningEnabled(putResult);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public void upload(String localPath, String cloudPath, Map<String, String> tags) throws CloudStorageException {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(cloudPath)
                    .tagging(createTagging(tags))
                    .build();
            var putResult = this.s3.putObject(putRequest, RequestBody.fromFile(Paths.get(localPath)));
            this.checkVersioningEnabled(putResult);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public void upload(InputStream input, String cloudPath, Map<String, String> tags) throws CloudStorageException {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(cloudPath)
                    .tagging(createTagging(tags))
                    .build();
            var putResult = this.s3.putObject(putRequest, RequestBody.fromInputStream(input, input.available()));
            this.checkVersioningEnabled(putResult);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 put error: " + t.getMessage(), t);
        }
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(cloudPath)
                    .build();
            ResponseInputStream<GetObjectResponse> obj = this.s3.getObject(getRequest);
            return obj;
        } catch (NoSuchKeyException e) {
            throw new CloudStorageException("The specified key does not exist: " + e.getClass().getSimpleName() + ": " + bucket);
        } catch (S3Exception e) {
            throw new CloudStorageException("s3 get error: " + e.getClass().getSimpleName() + ": " + bucket + (verbose ? " - " + e.getMessage() : ""));
        } catch (SdkClientException e) {
            throw new CloudStorageException("s3 get error: " + e.getClass().getSimpleName() + ": " + bucket + (verbose ? " - " + e.getMessage() : ""));
        } catch (Throwable t) {
            // Do not log the message or the original exception as that may contain the pre-signed url
            throw new CloudStorageException("s3 get error: " + t.getClass().getSimpleName() + ": " + bucket);
        }
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(cloudPath)
                    .build();
            this.s3.deleteObject(deleteRequest);
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
            ListObjectsV2Request.Builder reqBuilder = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix);
            ListObjectsV2Request req = reqBuilder.build();
            ListObjectsV2Response result = null;

            int reqCount = 0;
            List<String> s3Paths = new ArrayList<>();
            do {
                result = this.s3.listObjectsV2(req);
                List<S3Object> objects = result.contents();

                LOGGER.trace("s3 listobjectv2 request for " + prefix + " " + reqCount++ + ", returned keycount " + result.keyCount());
                if (!objects.isEmpty()) {
                    LOGGER.trace("--> 1st key = " + objects.get(0).key());
                }

                for (S3Object obj : objects) {
                    s3Paths.add(obj.key());
                }

                if (result.isTruncated()) {
                    req = reqBuilder.continuationToken(result.nextContinuationToken()).build();
                    LOGGER.trace("--> truncated, continuationtoken: " + result.nextContinuationToken());
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
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(cloudPath)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(preSignedUrlExpiryInSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            try (S3Presigner presigner = S3Presigner.create()) {
                return presigner.presignGetObject(presignRequest).url();
            }
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
            // Check if object exists by trying to get its metadata
            try {
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(this.bucket)
                        .key(cloudPath)
                        .build();
                this.s3.headObject(headRequest);
                
                // Object exists, set tags
                PutObjectTaggingRequest tagRequest = PutObjectTaggingRequest.builder()
                        .bucket(this.bucket)
                        .key(cloudPath)
                        .tagging(createTagging(tags))
                        .build();
                        
                this.s3.putObjectTagging(tagRequest);
            } catch (NoSuchKeyException e) {
                LOGGER.warn("CloudPath: {} does not exist in bucket: {}. Tags not set", cloudPath, this.bucket);
            }
        } catch (Throwable t) {
            throw new CloudStorageException("s3 set tags error", t);
        }
    }

    private void deleteInternal(Collection<String> cloudPaths) throws CloudStorageException {
        List<ObjectIdentifier> keys = new ArrayList<>();
        for (String p : cloudPaths) {
            keys.add(ObjectIdentifier.builder().key(p).build());
        }
        
        Delete delete = Delete.builder()
                .objects(keys)
                .quiet(false)
                .build();
                
        DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(delete)
                .build();
                
        try {
            this.s3.deleteObjects(deleteRequest);
        } catch (Throwable t) {
            throw new CloudStorageException("s3 delete error: " + t.getMessage(), t);
        }
    }

    private Tagging createTagging(Map<String, String> tags) {
        List<Tag> tagList = new ArrayList<>();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            tagList.add(Tag.builder().key(entry.getKey()).value(entry.getValue()).build());
        }
        return Tagging.builder().tagSet(tagList).build();
    }

    private void checkVersioningEnabled(PutObjectResponse putObjectResponse) {
        try {
            String versionId = putObjectResponse.versionId();
            String region = "unknown"; // S3Client doesn't expose region directly
            if (versionId == null || versionId.isEmpty()) {
                LOGGER.warn(
                        "Bucket: {} in Region: {} does not have versioning configured. There is a potential for data loss",
                        this.bucket,
                        region);
            } else {
                LOGGER.info(
                        "Bucket: {} in Region: {} has versioning configured.",
                        this.bucket,
                        region);
            }
        } catch (Throwable t) {
            // don't want this to fail when writing, but should be logged
            LOGGER.error(
                    String.format("Unable to determine if the S3 bucket: %s has versioning enabled", this.bucket),
                    t);
        }
    }
}
