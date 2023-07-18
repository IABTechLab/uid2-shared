package com.uid2.shared.secure.gcpoidc;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@Value
public class TokenPayload {
    public static final String DEBUG_DISABLED = "disabled-since-boot";
    public static final String CONFIDENTIAL_SPACE_SW_NAME = "CONFIDENTIAL_SPACE";
    public static final String CONFIDENTIAL_SPACE_STABLE = "STABLE";
    public static final String RESTART_POLICY_NEVER = "Never";

    //region Confidential Space image

    private String dbgStat;

    private String swName;

    private String swVersion;

    private List<String> csSupportedAttributes;

    //endregion

    //region Workload container

    private String workloadImageReference;

    private String workloadImageDigest;

    private List<String> cmdOverrides;

    private Map<String, String> envOverrides;

    private String restartPolicy;

    //endregion

    public boolean isDebugMode(){
        return !DEBUG_DISABLED.equalsIgnoreCase(dbgStat);
    }

    public boolean isStableVersion(){
        if(CollectionUtils.isEmpty(csSupportedAttributes)){
            return false;
        }
        for (var attribute: csSupportedAttributes) {
            if(CONFIDENTIAL_SPACE_STABLE.equalsIgnoreCase(attribute)){
                return true;
            }
        }
        return false;
    }

    public boolean isConfidentialSpaceSW(){
        return CONFIDENTIAL_SPACE_SW_NAME.equalsIgnoreCase(swName);
    }

    public boolean isRestartPolicyNever() {
        return RESTART_POLICY_NEVER.equalsIgnoreCase(restartPolicy);
    }
}
