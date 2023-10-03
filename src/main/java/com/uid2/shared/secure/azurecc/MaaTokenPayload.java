package com.uid2.shared.secure.azurecc;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MaaTokenPayload {
    public static final String SEV_SNP_VM_TYPE = "sevsnpvm";
    public static final String AZURE_COMPLIANT_UVM = "azure-compliant-uvm";

    private String attestationType;
    private String complianceStatus;
    private boolean vmDebuggable;
    private String ccePolicyDigest;

    private RuntimeData runtimeData;

    public boolean isSevSnpVM(){
        return SEV_SNP_VM_TYPE.equalsIgnoreCase(attestationType);
    }

    public boolean isUtilityVMCompliant(){
        return AZURE_COMPLIANT_UVM.equalsIgnoreCase(complianceStatus);
    }
}
