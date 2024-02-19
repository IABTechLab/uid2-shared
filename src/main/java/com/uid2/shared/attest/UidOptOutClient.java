package com.uid2.shared.attest;

import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.util.URLConnectionHttpClient;

import java.net.Proxy;
import java.net.http.HttpClient;

public class UidOptOutClient extends UidCoreClient {
    public static UidOptOutClient createNoAttest(String userToken, boolean enforceHttps, AttestationTokenRetriever attestationTokenRetriever) {
        return new UidOptOutClient(userToken, CloudUtils.defaultProxy, attestationTokenRetriever, null);
    }

    public UidOptOutClient(String userToken,
                         Proxy proxy,
                         AttestationTokenRetriever attestationTokenRetriever) {
        super(userToken, proxy, attestationTokenRetriever, null);
    }
    public UidOptOutClient(String userToken,
                           Proxy proxy,
                           AttestationTokenRetriever attestationTokenRetriever,
                           URLConnectionHttpClient httpClient) {
        super(userToken, proxy, attestationTokenRetriever, httpClient);
    }

    @Override
    protected String getJWT() {
        return this.getAttestationTokenRetriever().getOptOutJWT();
    }
}
