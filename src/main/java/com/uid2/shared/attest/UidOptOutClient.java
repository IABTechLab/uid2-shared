package com.uid2.shared.attest;

import com.uid2.shared.cloud.CloudUtils;

import java.net.Proxy;
import java.net.http.HttpClient;

public class UidOptOutClient extends UidCoreClient {
    public static UidOptOutClient createNoAttest(String userToken, boolean enforceHttps, AttestationTokenRetriever attestationTokenRetriever) {
        return new UidOptOutClient(userToken, CloudUtils.defaultProxy, enforceHttps, attestationTokenRetriever, null);
    }

    public UidOptOutClient(String userToken,
                         Proxy proxy,
                         boolean enforceHttps,
                         AttestationTokenRetriever attestationTokenRetriever) {
        super(userToken, proxy, enforceHttps, attestationTokenRetriever, null);
    }
    public UidOptOutClient(String userToken,
                         Proxy proxy,
                         boolean enforceHttps,
                         AttestationTokenRetriever attestationTokenRetriever,
                         HttpClient httpClient) {
        super(userToken, proxy, enforceHttps, attestationTokenRetriever, httpClient);
    }

    @Override
    protected String getJWT() {
        return this.getAttestationTokenRetriever().getOptOutJWT();
    }
}
