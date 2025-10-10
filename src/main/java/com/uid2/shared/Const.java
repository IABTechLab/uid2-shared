package com.uid2.shared;

import java.io.File;

public class Const {
    public static class Port {
        public static final int ServicePortForCore = 8088;
        public static final int ServicePortForOperator = 8080;
        public static final int ServicePortForOptOut = 8081;
        public static final int ServicePortForAdmin = 8089;

        public static final int PrometheusPortForCore = 9088;
        public static final int PrometheusPortForOperator = 9080;
        public static final int PrometheusPortForOptOut = 9081;
        public static final int PrometheusPortForAdmin = 9089;
    }

    public static class Config {
        // this is the system property name that vertx-config uses to read default config path
        public static final String VERTX_DEFAULT_CONFIG_PATH_PROP = "vertx-default-config-path";
        // this is the system property name that vertx-config uses to read override config path
        public static final String VERTX_CONFIG_PATH_PROP = "vertx-config-path";

        // this file stores default config values, can be overridden by env and sys variables
        public static final String DEFAULT_CONFIG_PATH = "conf" + File.separator + "default-config.json";
        // this files stores override config values that supersede everything else
        public static final String OVERRIDE_CONFIG_PATH = "conf" + File.separator + "config.json";
        // this file stores local debug config values, e.g. it runs standalone, not relying on other uid2 services.
        public static final String LOCAL_CONFIG_PATH = "conf" + File.separator + "local-config.json";
        // this file stores integration config values, e.g. it communicates with other uid2 services that runs locally.
        public static final String INTEG_CONFIG_PATH = "conf" + File.separator + "integ-config.json";

        public static final String UidInstanceIdPrefixProp = "uid_instance_id_prefix";

        // Azure
        public static final String MaaServerBaseUrlProp = "maa_server_base_url";

        // AWS S3
        public static final String AwsRegionProp = "aws_region";
        public static final String AccessKeyIdProp = "aws_access_key_id";
        public static final String SecretAccessKeyProp = "aws_secret_access_key";
        public static final String S3EndpointProp = "aws_s3_endpoint";
        public static final String CoreS3BucketProp = "core_s3_bucket";
        public static final String OptOutS3BucketProp = "optout_s3_bucket";
        public static final String S3VerboseLoggingProp = "aws_s3_verbose_logging";

        // Config stores
        public static final String CloudRefreshIntervalProp = "cloud_refresh_interval";
        public static final String CloudDownloadThreadsProp = "cloud_download_threads";
        public static final String CloudUploadThreadsProp = "cloud_upload_threads";
        public static final String ClientsMetadataPathProp = "clients_metadata_path";
        public static final String KeysMetadataPathProp = "keys_metadata_path";
        public static final String KeysAclMetadataPathProp = "keys_acl_metadata_path";
        public static final String KeysetsMetadataPathProp = "keysets_metadata_path";
        public static final String KeysetKeysMetadataPathProp = "keyset_keys_metadata_path";
        public static final String ClientSideKeypairsMetadataPathProp = "client_side_keypairs_metadata_path";
        public static final String ServiceMetadataPathProp = "services_metadata_path";
        public static final String ServiceLinkMetadataPathProp = "service_links_metadata_path";
        public static final String SitesMetadataPathProp = "sites_metadata_path";
        public static final String OperatorsMetadataPathProp = "operators_metadata_path";
        public static final String CloudEncryptionKeysMetadataPathProp = "cloud_encryption_keys_metadata_path";
        public static final String SaltsMetadataPathProp = "salts_metadata_path";
        public static final String OptOutMetadataPathProp = "optout_metadata_path";

        // Attestation
        public static final String CoreApiTokenProp = "core_api_token";
        public static final String CoreAttestUrlProp = "core_attest_url";
        public static final String CorePublicUrlProp = "core_public_url";
        public static final String OptOutUrlProp = "optout_url";
        public static final String AwsKmsJwtSigningKeyIdProp = "aws_kms_jwt_signing_key_id";
        public static final String AwsKmsJwtSigningPublicKeysProp = "aws_kms_jwt_signing_public_keys";
        public static final String EnforceJwtProp = "enforceJwt";

        // Optout
        public static final String OptOutS3FolderProp = "optout_s3_folder";
        public static final String OptOutDataDirProp = "optout_data_dir";
        public static final String OptOutReplicaUris = "optout_replica_uris";
        public static final String OptOutSyntheticLogsEnabledProp = "optout_synthetic_logs_enabled";
        public static final String OptOutSyntheticLogsCountProp = "optout_synthetic_logs_count";
        public static final String OptOutProducerReplicaIdProp = "optout_producer_replica_id";
        public static final String OptOutProducerReplicaIdOffsetProp = "optout_producer_replica_id_offset";
        public static final String OptOutProducerMaxReplicasProp = "optout_producer_max_replicas";
        public static final String OptOutDeltaRotateIntervalProp = "optout_delta_rotate_interval";
        public static final String OptOutDeltaBacktrackInDaysProp = "optout_delta_backtrack_in_days";
        public static final String OptOutPartitionIntervalProp = "optout_partition_interval";
        public static final String OptOutMaxPartitionsProp = "optout_max_partitions";

        // Others
        public static final String SaltsExpiredShutdownHours = "salts_expired_shutdown_hours";
        public static final String encryptionSupportVersion = "encryption_support_version";
    }

    public static class Http {
        public static String AppVersionHeader = "X-UID2-AppVersion";
        public static String ClientVersionHeader = "X-UID2-Client-Version";
    }

    public static class ResponseStatus {
        public static String Success = "success";
        public static String Unauthorized = "unauthorized";
        public static String ClientError = "client_error";
        public static String OptOut = "optout";
        public static String InvalidToken = "invalid_token";
    }

    public static class Name {
        public static String AsymetricEncryptionKeyClass = "RSA";
        public static String AsymetricEncryptionCipherClass = "RSA/ECB/PKCS1Padding";
    }

    public static class Data {

        public static final int MasterKeySiteId = -1; // This site id is reserved. It is assigned to master keys.
        public static final int RefreshKeySiteId = -2; // This site id is reserved. It is used to encrypt refresh token.
        @Deprecated
        public static final int DefaultClientSiteId = 1; // This site id is reserved. It is assigned to client access keys by default.
        public static final int AdvertisingTokenSiteId = 2; // This site id is reserved. It may not be used for client keys.


        public static final int MasterKeysetId = -1; // Keyset used by MasterKeySiteId
        public static final int RefreshKeysetId = -2; // Keyset used by RefreshKeySiteId
        public static final int FallbackPublisherKeysetId = 2; // Keyset used by AdvertisingTokenSiteId
    }

    public static class RoutingContextData {
        public static final String SiteId = "site-id";
    }

    public static class Attestation {
        public static final String AttestationTokenHeader = "Attestation-Token";
        public static final String AttestationJWTHeader = "Attestation-JWT";

    }
}
