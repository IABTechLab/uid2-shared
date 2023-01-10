package com.uid2.shared.secure.gcp;

public class VmConfigId {
    private final String idString;
    private final String failedReason;
    private final boolean isValid;

    // for troubleshooting purposes
    private final String projectId;
    public static VmConfigId success(String idString, String projectId) {
        return new VmConfigId(true, idString, null, projectId);
    }
    public static VmConfigId failure(String reason, String projectId) {
        return new VmConfigId(false, null, reason, projectId);
    }

    private VmConfigId(boolean success, String idString, String reason, String projectId) {
        this.isValid = success;
        this.idString = idString;
        this.failedReason = reason;
        this.projectId = projectId;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public String getValue() {
        if (!isValid) throw new RuntimeException("value is invalid - check isValid() before using VmConfigId");
        return idString;
    }

    public String getFailedReason() {
        if (isValid) throw new RuntimeException("no failed reason - check isValid() before using VmConfigId");
        return failedReason;
    }

    /**
     * get project ID for troubleshooting
     * @return (nullable) projectId
     */
    public String getProjectId() {
        return projectId;
    }

    @Override
    public String toString() {
        return String.format("[success=%b, idString=%s, reason=%s, projectId=%s]",
                isValid, idString, failedReason, projectId);
    }
}
