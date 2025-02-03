package com.uid2.shared.secure.azurecc;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MaaTokenPayload {
    public static final String SEV_SNP_VM_TYPE = "sevsnpvm";

    private String azure_compliant_uvm;
    private String attestationType;
    private String complianceStatus;
    private boolean vmDebuggable;
    private String ccePolicyDigest;

    private RuntimeData runtimeData;

    public boolean isSevSnpVM(){
        return SEV_SNP_VM_TYPE.equalsIgnoreCase(attestationType);
    }

    public boolean isUtilityVMCompliant(){
        return azure_compliant_uvm.equalsIgnoreCase(complianceStatus);
    }
}
