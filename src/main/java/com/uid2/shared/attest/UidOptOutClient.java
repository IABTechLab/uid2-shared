package com.uid2.shared.attest;

import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.util.URLConnectionHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class UidOptOutClient extends UidCoreClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidOptOutClient.class);
    private AttestationTokenRetriever attestationTokenRetriever;

    public UidOptOutClient(String userToken,
                         Proxy proxy,
                         AttestationTokenRetriever attestationTokenRetriever) {
        super(userToken, proxy, attestationTokenRetriever, null);
        this.attestationTokenRetriever = attestationTokenRetriever;
    }
    public UidOptOutClient(String userToken,
                           Proxy proxy,
                           AttestationTokenRetriever attestationTokenRetriever,
                           URLConnectionHttpClient httpClient) {
        super(userToken, proxy, attestationTokenRetriever, httpClient);
        this.attestationTokenRetriever = attestationTokenRetriever;
    }

    @Override
    protected String getJWT() {
        return this.getAttestationTokenRetriever().getOptOutJWT();
    }

    @Override
    public InputStream download(String path) throws CloudStorageException {
        if (path == null) {
            path = "";
        }

        if (this.attestationTokenRetriever != null && this.attestationTokenRetriever.getOptOutUrl() != null) {
            try {
                URL baseUrl = new URL(this.attestationTokenRetriever.getOptOutUrl());
                URL fullUrl = new URL(baseUrl, path);

                return super.download(fullUrl.toExternalForm());
            } catch (MalformedURLException e) {
                LOGGER.error("Unable to parse OptOut URL", e);
            }
        }

        LOGGER.warn("UidOptOutClient attempting to download from path: {}, but OptOutUrl not available", path);
        return InputStream.nullInputStream();
    }
}
