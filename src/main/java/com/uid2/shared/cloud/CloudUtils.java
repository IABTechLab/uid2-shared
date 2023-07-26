package com.uid2.shared.cloud;

import com.google.api.services.compute.ComputeScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Collections;

public class CloudUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudUtils.class);
    public static Proxy defaultProxy = getDefaultProxy();

    public static TaggableCloudStorage createStorageWithIam(String cloudBucket, JsonObject jsonConfig) {
        return new CloudStorageS3(
                jsonConfig.getString(Const.Config.AwsRegionProp),
                cloudBucket,
                jsonConfig.getString(Const.Config.S3EndpointProp, "")
        );
    }

    // I think this is not used, Aleksandrs Ulme 26/07/2023
    @Deprecated
    public static TaggableCloudStorage createStorage(String cloudBucket) {
        return new CloudStorageS3(
                System.getProperty(Const.Config.AccessKeyIdProp),
                System.getProperty(Const.Config.SecretAccessKeyProp),
                System.getProperty(Const.Config.AwsRegionProp),
                cloudBucket,
                System.getProperty(Const.Config.S3EndpointProp, "")
        );
    }

    public static TaggableCloudStorage createStorage(String cloudBucket, JsonObject jsonConfig) {
        return new CloudStorageS3(
            jsonConfig.getString(Const.Config.AccessKeyIdProp),
            jsonConfig.getString(Const.Config.SecretAccessKeyProp),
            jsonConfig.getString(Const.Config.AwsRegionProp),
            cloudBucket,
            jsonConfig.getString(Const.Config.S3EndpointProp, "")
        );
    }

    public static GoogleCredentials getGoogleCredentialsFromConfig(JsonObject jsonConfig) {
        GoogleCredentials credentials = getGoogleCredentialsFromConfigInternal(jsonConfig);
        if (credentials != null && credentials.createScopedRequired()) {
            // only needs compute readonly scope
            LOGGER.info("Requesting scope: " + ComputeScopes.COMPUTE_READONLY);
            credentials.createScoped(Collections.singletonList(ComputeScopes.COMPUTE_READONLY));
        }
        return credentials;
    }

    private static GoogleCredentials getGoogleCredentialsFromConfigInternal(JsonObject jsonConfig) {
        if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null) {
            try {
                GoogleCredentials ret = GoogleCredentials.getApplicationDefault();
                LOGGER.info("Using GOOGLE_APPLICATION_CREDENTIALS from environment");
                return ret;

            } catch (Exception ex) {
                LOGGER.error("Unable to read google credentials " + ex.getMessage(), ex);
                return null;
            }
        }

        try {
            String encodedCreds = jsonConfig.getString(Const.Config.GoogleCredentialsProp);
            if (encodedCreds == null) return null;
            byte[] credentials = Utils.decodeBase64String(encodedCreds);
            if (credentials == null) return null;
            GoogleCredentials ret = GoogleCredentials.fromStream(new ByteArrayInputStream(credentials));
            LOGGER.info("Using google_credentials provided through vertx-config (env or config)");
            return ret;
        } catch (Exception ex) {
            LOGGER.error("Unable to read google credentials " + ex.getMessage(), ex);
            return null;
        }
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
