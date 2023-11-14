package com.uid2.shared.attest;

import com.uid2.enclave.IOperatorKeyRetriever;

import java.lang.reflect.Constructor;

public class OperatorKeyRetrieverFactory {
    public static IOperatorKeyRetriever getAzureOperatorKeyRetriever(String vaultName, String secretName) throws Exception {
        Class<?> cls = Class.forName("com.uid2.attestation.azure.AzureVaultOperatorKeyRetriever");
        Constructor<?> c = cls.getConstructor(String.class, String.class);
        return (IOperatorKeyRetriever) c.newInstance(vaultName, secretName);
    }

    public static IOperatorKeyRetriever getGcpOperatorKeyRetriever(String secretVersionName) throws Exception {
        Class<?> cls = Class.forName("com.uid2.attestation.gcp.GcpOperatorKeyRetriever");
        Constructor<?> c = cls.getConstructor(String.class);
        return (IOperatorKeyRetriever) c.newInstance(secretVersionName);
    }
}
