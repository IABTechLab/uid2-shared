package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.AttestationFailure;
import com.uid2.shared.secure.Protocol;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MaaTokenPayload {
    public static final String SEV_SNP_VM_TYPE = "sevsnpvm";
    // the `x-ms-compliance-status` value for ACI CC
    public static final String AZURE_COMPLIANT_UVM = "azure-compliant-uvm";
    // the `x-ms-compliance-status` value for AKS CC
    public static final String AZURE_COMPLIANT_UVM_AKS = "azure-signed-katacc-uvm";

    private Protocol azureProtocol;
    private String attestationType;
    private String complianceStatus;
    private boolean vmDebuggable;
    private String ccePolicyDigest;

    private RuntimeData runtimeData;

    public boolean isSevSnpVM(){
        return SEV_SNP_VM_TYPE.equalsIgnoreCase(attestationType);
    }

    public boolean isUtilityVMCompliant() throws AttestationClientException {
        if (azureProtocol == Protocol.AZURE_CC_ACI) {
            return AZURE_COMPLIANT_UVM.equalsIgnoreCase(complianceStatus);
        } else if (azureProtocol == Protocol.AZURE_CC_AKS) {
            return AZURE_COMPLIANT_UVM_AKS.equalsIgnoreCase(complianceStatus);
        } else {
            throw new AttestationClientException(String.format("Azure protocol: %s not supported", azureProtocol), AttestationFailure.INVALID_PROTOCOL);
        }
    }
}
