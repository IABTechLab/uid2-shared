package com.uid2.shared.cloud;

import com.uid2.shared.Const;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.file.Path;

public class CloudUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudUtils.class);
    public static Proxy defaultProxy = getDefaultProxy();

    public static TaggableCloudStorage createStorage(String cloudBucket, JsonObject jsonConfig) {
        var region = jsonConfig.getString(Const.Config.AwsRegionProp);
        return createStorage(cloudBucket, region, jsonConfig);
    }

    public static TaggableCloudStorage createStorage(String cloudBucket, String region, JsonObject jsonConfig) {
        var accessKeyId = jsonConfig.getString(Const.Config.AccessKeyIdProp);
        var secretAccessKey = jsonConfig.getString(Const.Config.SecretAccessKeyProp);
        var s3Endpoint = jsonConfig.getString(Const.Config.S3EndpointProp, "");
        var verboseLogging = jsonConfig.getBoolean(Const.Config.S3VerboseLoggingProp, false);

        if (accessKeyId == null || secretAccessKey == null) {
            // IAM authentication
            return new CloudStorageS3(region, cloudBucket, s3Endpoint);
        }
        // User access key authentication
        return new CloudStorageS3(accessKeyId, secretAccessKey, region, cloudBucket, s3Endpoint, verboseLogging);
    }

    // I think this is not used, Aleksandrs Ulme 26/07/2023
    @Deprecated
    public static TaggableCloudStorage createStorage(String cloudBucket) {
        return new CloudStorageS3(
                System.getProperty(Const.Config.AccessKeyIdProp),
                System.getProperty(Const.Config.SecretAccessKeyProp),
                System.getProperty(Const.Config.AwsRegionProp),
                cloudBucket,
                System.getProperty(Const.Config.S3EndpointProp, ""),
                Boolean.parseBoolean(System.getProperty(Const.Config.S3VerboseLoggingProp, "false"))
        );
    }


    public static String normalizeFilePath(Path path) {
        return normalizFilePath(path.toString());
    }

    public static String normalizFilePath(String pathStr) {
        return pathStr.replace('\\', '/');
    }

    public static String normalizeDirPath(Path path) {
        return normalizDirPath(path.toString());
    }

    public static String normalizDirPath(String pathStr) {
        pathStr = pathStr.replace('\\', '/');
        if (pathStr.endsWith("/")) return pathStr;
        else return pathStr + "/";
    }

    private static Proxy getDefaultProxy() {
        String httpProxy = System.getProperty("http_proxy");
        if (httpProxy != null && httpProxy.startsWith("socks5://")) {
            LOGGER.info("Using http_proxy: " + httpProxy);
            String[] splits = httpProxy.substring("socks5://".length()).split(":");
            SocketAddress addr = new InetSocketAddress(splits[0], Integer.valueOf(splits[1]));
            return new Proxy(Proxy.Type.SOCKS, addr);
        } else {
            LOGGER.info("Unsupported http_proxy, not creating default proxy: " + httpProxy);
            return null;
        }
    }
}
