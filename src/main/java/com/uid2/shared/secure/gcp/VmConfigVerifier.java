package com.uid2.shared.secure.gcp;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.gax.paging.Page;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.audit.AuditLog;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.uid2.shared.Utils;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class VmConfigVerifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(VmConfigVerifier.class);
    private static final String ENCLAVE_PARAM_PREFIX = "UID2_ENCLAVE_";

    private final GoogleCredentials credentials;
    public static final boolean VALIDATE_AUDITLOGS = true;
    public static final boolean VALIDATE_VMCONFIG = true;

    private final Set<String> enclaveParams;
    private final Set<String> allowedMethodsFromInstanceAuditLogs =
        new HashSet<String>(Collections.singletonList("v1.compute.instances.insert"));

    private final Compute computeApi;
    private final Logging loggingApi;

    public VmConfigVerifier(GoogleCredentials credentials, Set<String> enclaveParams) throws Exception {
        this.credentials = credentials;
        if (this.credentials != null) {
            LOGGER.info("Using Using Google Service Account: " + credentials.toString());
            final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            final HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            computeApi = new Compute.Builder(httpTransport, jsonFactory, requestInitializer)
                    .setApplicationName("UID-Operator/2.0")
                    .build();

            loggingApi = LoggingOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        } else {
            computeApi = null;
            loggingApi = null;
        }

        this.enclaveParams = enclaveParams;
        if (this.enclaveParams != null) {
            for (String enclaveParam : this.enclaveParams) {
                LOGGER.info("Allowed Enclave Parameter: " + normalizeEnclaveParam(enclaveParam));
            }
        }
    }

    public VmConfigId getVmConfigId(InstanceDocument id) {
        try {
            LOGGER.debug("Issuing instance get request...");
            Instance instance = computeApi.instances()
                .get(id.getProjectId(), id.getZone(), id.getInstanceId())
                .execute();

            StringBuilder str = new StringBuilder();
            for (AttachedDisk disk : instance.getDisks()) {
                if (!disk.getAutoDelete()) return VmConfigId.failure("!disk.autodelete", id.getProjectId());
                if (!disk.getBoot()) return VmConfigId.failure("!disk.getboot", id.getProjectId());

                String diskSourceUrl = disk.getSource();
                String imageUrl = getDiskSourceImage(diskSourceUrl);
                str.append(getSha256Base64Encoded(imageUrl));
            }

            Metadata metadata = instance.getMetadata();
            for (Metadata.Items metadataItem : metadata.getItems()) {
                if (metadataItem.getKey().equals("user-data")) {
                    String cloudInitConfig = metadataItem.getValue();
                    String templatizedConfig = templatizeVmConfig(cloudInitConfig);
                    str.append(getSha256Base64Encoded(templatizedConfig));
                } else {
                    LOGGER.debug("gcp-vmid attestation got unrecognized metadata key: " + metadataItem.getKey());
                    return VmConfigId.failure("bad metadata item: " + metadataItem.getKey(), id.getProjectId());
                }
            }

            String badAuditLog = findUnauthorizedAuditLog(id);
            if (badAuditLog != null) {
                LOGGER.debug("attestation failed because of audit log: " + badAuditLog);
                return VmConfigId.failure("bad audit log: " + badAuditLog, id.getProjectId());
            }

            // str is a concatenation of disk hashes and cloud-init hashes
            // configId is the SHA-256 output of str.toString()
            return VmConfigId.success(getSha256Base64Encoded(str.toString()), id.getProjectId());
        } catch (Exception e) {
            LOGGER.error("getVmConfigId error " + e.getMessage(), e);
            return VmConfigId.failure(e.getMessage(), id.getProjectId());
        }
    }

    public String templatizeVmConfig(String cloudInitConfig) {
        // return original value if no enclave parameter is specified
        if (this.enclaveParams == null) return cloudInitConfig;

        // If enclave param is `api_token`, we will look for the following line in the cloudInitConfig:
        //   Environment="UID2_ENCLAVE_API_TOKEN=token_value"
        // and replace it with dummy value to templatize the cloud-init config
        //   Environment="UID2_ENCLAVE_API_TOKEN=dummy"
        //
        // This is done so that the core don't need to approve different cloud-init that differs only in
        // the allowed enclave parameter values.

        for (String enclaveParam : this.enclaveParams) {
            String subRegex = String.format("^([ \t]*Environment=.%s)=.+?\"$", normalizeEnclaveParam(enclaveParam));
            Pattern pattern = Pattern.compile(subRegex, Pattern.MULTILINE );
            cloudInitConfig = pattern.matcher(cloudInitConfig).replaceAll("$1=dummy\"");
        }

        return cloudInitConfig;
    }

    private String getAuditLogFilter(InstanceDocument id) {
        return String.format("resource.type=gce_instance" +
            "  AND (" +
            "    logName=projects/%s/logs/cloudaudit.googleapis.com%%2Factivity" +
            "    OR logName=projects/%s/logs/cloudaudit.googleapis.com%%2Fdata_access" +
            "  )" +
            "  AND protoPayload.\"@type\"=\"type.googleapis.com/google.cloud.audit.AuditLog\"" +
            "  AND resource.labels.instance_id=%s",
            id.getProjectId(),
            id.getProjectId(),
            id.getInstanceId());
    }

    /**
     * Find the first unauthorized audit log and its reason.
     * @param id the instance document
     * @return reason the log is unauthorized, *null* if all passed or skipped.
     * @throws InvalidProtocolBufferException
     */
    private String findUnauthorizedAuditLog(InstanceDocument id) throws InvalidProtocolBufferException {
        if (!VALIDATE_AUDITLOGS) {
            LOGGER.fatal("Skip AuditLogs validation (VALIDATE_AUDITLOGS off)...");
            return null;
        }

        LOGGER.debug("Searching AuditLogs...");
        String logFilter = getAuditLogFilter(id);
        Page<LogEntry> entries = loggingApi.listLogEntries(Logging.EntryListOption.filter(logFilter));

        do {
            for (LogEntry logEntry : entries.iterateAll()) {
                Any data = (Any)logEntry.getPayload().getData();
                AuditLog auditLog = AuditLog.parseFrom(data.getValue());
                if (!validateAuditLog(auditLog)) {
                    return auditLog.getMethodName();
                }
            }
            entries = entries.getNextPage();
        } while (entries != null);

        return null;
    }

    private boolean validateAuditLog(AuditLog auditLog) {
        LOGGER.debug("Validating AuditLog for operation: " + auditLog.getMethodName());
        if (allowedMethodsFromInstanceAuditLogs.contains(auditLog.getMethodName())) {
            return true;
        } else {
            LOGGER.warn("gcp-vmid attestation receives unauthorized method: " + auditLog.getMethodName());
            return false;
        }
    }

    private String getDiskSourceImage(String diskSourceUrl) throws IOException {
        String[] splits = diskSourceUrl.split("/");
        String projectId = splits[6];
        String zone = splits[8];
        String diskId = splits[10];

        LOGGER.debug("Issuing disk get request for " + diskId + "...");
        Disk disk = computeApi.disks().get(projectId, zone, diskId).execute();
        return disk.getSourceImage();
    }

    private String getSha256Base64Encoded(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // input should contain only US-ASCII chars
        md.update(input.getBytes(StandardCharsets.US_ASCII));
        return Utils.toBase64String(md.digest());
    }

    private static String normalizeEnclaveParam(String name) {
        return ENCLAVE_PARAM_PREFIX + name.toUpperCase();
    }
}
