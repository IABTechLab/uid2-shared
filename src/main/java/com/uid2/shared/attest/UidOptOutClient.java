package com.uid2.shared.attest;

import com.uid2.shared.audit.UidInstanceIdProvider;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.util.URLConnectionHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UidOptOutClient extends UidCoreClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidOptOutClient.class);
    private AttestationResponseHandler attestationResponseHandler;

    public UidOptOutClient(String userToken,
                           Proxy proxy,
                           AttestationResponseHandler attestationResponseHandler,
                           UidInstanceIdProvider uidInstanceIdProvider) {
        super(userToken, proxy, attestationResponseHandler, null, false, uidInstanceIdProvider);
        this.attestationResponseHandler = attestationResponseHandler;
    }

    public UidOptOutClient(String userToken,
                           Proxy proxy,
                           AttestationResponseHandler attestationResponseHandler,
                           URLConnectionHttpClient httpClient,
                           UidInstanceIdProvider uidInstanceIdProvider) {
        super(userToken, proxy, attestationResponseHandler, httpClient, false, uidInstanceIdProvider);
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
                URL fullUrl = buildOptOutUrl(this.attestationResponseHandler.getOptOutUrl(), path);
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

    /**
     * Builds an opt-out URL from the scheme, host and port of {@code optOutBaseUrl} and only the path and query of
     * {@code configuredLocation}, so the host always comes from the base URL.
     */
    public static URL buildOptOutUrl(String optOutBaseUrl, String configuredLocation) throws MalformedURLException {
        URL base = new URL(optOutBaseUrl);
        String pathAndQuery = "";
        if (configuredLocation != null && !configuredLocation.isEmpty()) {
            URI location;
            try {
                location = new URI(configuredLocation);
            } catch (URISyntaxException e) {
                throw (MalformedURLException) new MalformedURLException("Invalid opt-out path").initCause(e);
            }
            if (location.isOpaque()) {
                throw new MalformedURLException("Invalid opt-out path");
            }
            String rawPath = location.getRawPath();
            if (rawPath != null && !rawPath.isEmpty()) {
                pathAndQuery = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
            }
            if (location.getRawQuery() != null) {
                pathAndQuery += "?" + location.getRawQuery();
            }
        }
        return new URL(base.getProtocol(), base.getHost(), base.getPort(), pathAndQuery);
    }
}
