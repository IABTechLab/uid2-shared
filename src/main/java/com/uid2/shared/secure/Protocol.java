package com.uid2.shared.secure;

public enum Protocol {
    GCP_OIDC,
    GCP_VMID,
    AWS_NITRO,
    AZURE_CC_ACI,
    AZURE_CC_AKS;

    public String toString() {
        switch(this) {
            case GCP_OIDC:
                return "gcp-oidc";
            case GCP_VMID:
                return "gcp-vmid";
            case AWS_NITRO:
                return "aws-nitro";
            case AZURE_CC_ACI:
                return "azure-cc";
            case AZURE_CC_AKS:
                return "azure-cc-aks";
            default:
                return "unknown-protocol";
        }

    }
}
