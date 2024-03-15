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
    private AttestationResponseHandler attestationResponseHandler;

    public UidOptOutClient(String userToken,
                           Proxy proxy,
                           AttestationResponseHandler attestationResponseHandler) {
        super(userToken, proxy, attestationResponseHandler, null);
        this.attestationResponseHandler = attestationResponseHandler;
    }

    public UidOptOutClient(String userToken,
                           Proxy proxy,
                           AttestationResponseHandler attestationResponseHandler,
                           URLConnectionHttpClient httpClient) {
        super(userToken, proxy, attestationResponseHandler, httpClient);
        this.attestationResponseHandler = attestationResponseHandler;
    }

    @Override
    protected String getJWT() {
        return this.getAttestationResponseHandler().getOptOutJWT();
    }

    @Override
    public InputStream download(String path) throws CloudStorageException {
        if (path == null) {
            path = "";
        }

        if (this.attestationResponseHandler.getOptOutUrl() != null) {
            try {
                URL baseUrl = new URL(this.attestationResponseHandler.getOptOutUrl());
                URL fullUrl = new URL(baseUrl, path);
                return super.download(fullUrl.toExternalForm());
            } catch (MalformedURLException e) {
                LOGGER.error("Unable to parse OptOut URL", e);
            } catch (Exception e) {
                // Specifically not logging the exception as it might contain sensitive URLs
                LOGGER.error("Unexpected error in UidOptOutClient download");
            }
        } else {
            LOGGER.warn("UidOptOutClient attempting to download but OptOutUrl not available");
        }

        return InputStream.nullInputStream();
    }
}
