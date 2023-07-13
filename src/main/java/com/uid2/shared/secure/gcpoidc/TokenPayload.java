package com.uid2.shared.secure.gcpoidc;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Builder
@Value
public class TokenPayload {
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
}
