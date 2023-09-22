package com.uid2.shared.secure.azurecc;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MaaTokenPayload {

    private String attestationType;
    private String complianceStatus;
    private boolean vmDebuggable;
    private String ccePolicy;

    private RuntimeData runtimeData;
}
