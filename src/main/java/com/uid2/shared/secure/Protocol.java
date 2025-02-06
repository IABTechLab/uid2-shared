package com.uid2.shared.secure;

public enum Protocol {
    GCP_OIDC("gcp-oidc"),
    GCP_VMID("gcp-vmid"),
    AWS_NITRO("aws-nitro"),
    AZURE_CC_ACI("azure-cc"),
    AZURE_CC_AKS("azure-cc-aks");

    private final String protocolValue;

    Protocol(String protocolValue) {
        this.protocolValue = protocolValue;
    }

    public String toString() {
        return this.protocolValue;
    }
}
