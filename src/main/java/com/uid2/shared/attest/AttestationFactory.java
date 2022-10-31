package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class AttestationFactory {
    public static IAttestationProvider getNitroAttestation() throws Exception {
        Class<?> cls = Class.forName("com.uid2.attestation.aws.NitroAttestationProvider");
        Constructor<?> c = cls.getConstructor();
        return (IAttestationProvider) c.newInstance();
    }

    public static IAttestationProvider getGcpVmidAttestation() throws Exception {
        Class<?> cls = Class.forName("com.uid2.attestation.gcp.VmidAttestationProvider");
        Constructor<?> c = cls.getConstructor();
        return (IAttestationProvider) c.newInstance();
    }

    public static IAttestationProvider getAzureAttestation() throws Exception {
        Class<?> cls = Class.forName("com.uid2.attestation.azure.AzureAttestationProvider");
        Constructor<?> c = cls.getConstructor();
        return (IAttestationProvider) c.newInstance();
    }
}
