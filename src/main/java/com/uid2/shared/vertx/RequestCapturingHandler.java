package com.uid2.shared.vertx;

import com.uid2.shared.Const;
import com.uid2.shared.auth.IAuthorizable;
import com.uid2.shared.jmx.AdminApi;
import com.uid2.shared.middleware.AuthMiddleware;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;

public class RequestCapturingHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestCapturingHandler.class);
    private static final ZoneId ZONE_GMT = ZoneId.of("GMT");
    private Queue<String> _capturedRequests = null;
    private final Map<String, Counter> _apiMetricCounters = new HashMap<>();
    private final Map<String, Counter> _clientAppVersionCounters = new HashMap<>();

    private static String formatRFC1123DateTime(long time) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(time).atZone(ZONE_GMT));
    }

    @Override
    public void handle(RoutingContext context) {
        if (!AdminApi.instance.getCaptureRequests() && !AdminApi.instance.getPublishApiMetrics()) {
            context.next();
            return;
        }

        if (_capturedRequests == null) {
            _capturedRequests = AdminApi.instance.allocateCapturedRequestQueue();
        }

        long timestamp = System.currentTimeMillis();
        String remoteClient = getClientAddress(context.request().remoteAddress());
        HttpMethod method = context.request().method();
        String uri = context.request().uri();
        HttpVersion version = context.request().version();
        context.addBodyEndHandler(v -> captureNoThrow(context, timestamp, remoteClient, version, method, uri));
        context.next();
    }

    private String getClientAddress(SocketAddress inetSocketAddress) {
        if (inetSocketAddress == null) {
            return null;
        }
        return inetSocketAddress.host();
    }

    private void captureNoThrow(RoutingContext context, long timestamp, String remoteClient, HttpVersion version, HttpMethod method, String uri) {
        try {
            capture(context, timestamp, remoteClient, version, method, uri);
        } catch (Throwable t) {
            LOGGER.error("capture() throws", t);
        }
    }

    private void capture(RoutingContext context, long timestamp, String remoteClient, HttpVersion version, HttpMethod method, String uri) {
        HttpServerRequest request = context.request();

        String path = null;
        try {
            // If the current route is a known path, extract the full path from the request URI
            if (context.currentRoute().getPath() != null) {
                path = new URI(context.request().absoluteURI()).getPath();
            }
        } catch (NullPointerException | URISyntaxException ex) {
            // RoutingContextImplBase has a bug: context.currentRoute() throws with NullPointerException when called from bodyEndHandler for StaticHandlerImpl.sendFile()
        }

        if (path == null) {
            path = "unknown";
        }

        int status = request.response().getStatusCode();
        String apiContact;
        try {
            apiContact = (String) context.data().get(AuthMiddleware.API_CONTACT_PROP);
            apiContact = apiContact == null ? "unknown" : apiContact;
        } catch (Exception ex) {
            apiContact = "error: " + ex.getMessage();
        }

        String host = request.headers().contains("host") ? request.headers().get("host") : "NotSpecified";
        if (host.startsWith("10.")) {
            // mask ip address form of host to reduce the metrics tag pollution
            host = "10.x.x.x:xx";
        }

        String hostname = request.host();
        hostname = hostname.substring(0, hostname.indexOf(":"));;

        final Integer siteId = getSiteId(context);
        incrementMetricCounter(apiContact, siteId, host, status, method, path, hostname);

        if (request.headers().contains(Const.Http.AppVersionHeader)) {
            incrementAppVersionCounter(apiContact, request.headers().get(Const.Http.AppVersionHeader));
        }

        if (AdminApi.instance.getCaptureFailureOnly() && status < 400) {
            return;
        }

        Matcher m = AdminApi.instance.getApiContactPattern().matcher(apiContact);
        if (!m.find()) {
            return;
        }

        while (_capturedRequests.size() >= AdminApi.instance.getMaxCapturedRequests()) {
            _capturedRequests.remove();
        }

        long contentLength = request.response().bytesWritten();
        String versionFormatted = "-";
        switch (version) {
            case HTTP_1_0:
                versionFormatted = "HTTP/1.0";
                break;
            case HTTP_1_1:
                versionFormatted = "HTTP/1.1";
                break;
            case HTTP_2:
                versionFormatted = "HTTP/2.0";
                break;
        }

        final MultiMap headers = request.headers();

        // as per RFC1945 the header is referer but it is not mandatory some implementations use referrer
        String referrer = headers.contains("referrer") ? headers.get("referrer") : headers.get("referer");
        String userAgent = request.headers().get("user-agent");
        referrer = referrer == null ? "-" : referrer;
        userAgent = userAgent == null ? "-" : userAgent;

        String summary = String.format(
                "-->[%s] %s - - [%s] \"%s %s %s\" %d %d %s \"%s\" \"%s\"",
                apiContact,
                remoteClient,
                formatRFC1123DateTime(timestamp),
                method,
                uri,
                versionFormatted,
                status,
                contentLength,
                (System.currentTimeMillis() - timestamp),
                referrer,
                userAgent);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(summary);
        messageBuilder.append("\n");

        for (Map.Entry<String, String> h : headers) {
            messageBuilder.append(h.getKey());
            messageBuilder.append(": ");
            messageBuilder.append(h.getValue());
            messageBuilder.append("\n");
        }

        messageBuilder.append("<--\n");
        for (Map.Entry<String, String> h : request.response().headers()) {
            messageBuilder.append(h.getKey());
            messageBuilder.append(": ");
            messageBuilder.append(h.getValue());
            messageBuilder.append("\n");
        }

        _capturedRequests.add(messageBuilder.toString());
    }

    private static Integer getSiteId(RoutingContext context) {
        final Integer siteId = context.get(Const.RoutingContextData.SiteId);
        if (siteId != null) {
            return siteId;
        }

        final IAuthorizable profile = AuthMiddleware.getAuthClient(context);
        if (profile != null) {
            return profile.getSiteId();
        }

        return null;
    }

    private void incrementMetricCounter(String apiContact, Integer siteId, String host, int status, HttpMethod method, String path, String hostname) {
        assert apiContact != null;
        String key = apiContact + "|" + siteId + "|" + host + "|" + status + "|" + method.name() + "|" + path + "|" + hostname;
        if (!_apiMetricCounters.containsKey(key)) {
            Counter counter = Counter
                    .builder("uid2.http_requests")
                    .description("counter for how many http requests are processed per each api contact and status code")
                    .tags("api_contact", apiContact, "site_id", String.valueOf(siteId), "host", host, "status", String.valueOf(status), "method", method.name(), "path", path, "hostname", hostname)
                    .register(Metrics.globalRegistry);
            _apiMetricCounters.put(key, counter);
        }

        _apiMetricCounters.get(key).increment();
    }

    private void incrementAppVersionCounter(String apiContact, String appVersions) {
        assert apiContact != null;
        assert appVersions != null;

        AbstractMap.SimpleEntry<String, String> client = parseClientAppVersion(appVersions);
        if (client == null)
            return;

        final String key = apiContact + "|" + client.getKey() + "|" + client.getValue();
        if (!_clientAppVersionCounters.containsKey(key)) {
            Counter counter = Counter
                    .builder("uid2.client_versions")
                    .description("counter for how many http requests are processed per each api contact and status code")
                    .tags("api_contact", apiContact, "client_name", client.getKey(), "client_version", client.getValue())
                    .register(Metrics.globalRegistry);
            _clientAppVersionCounters.put(key, counter);
        }

        _clientAppVersionCounters.get(key).increment();
    }

    private static AbstractMap.SimpleEntry<String, String> parseClientAppVersion(String appVersions) {
        final int eqpos = appVersions.indexOf('=');
        if (eqpos == -1) {
            return null;
        }
        final String appName = appVersions.substring(0, eqpos);

        final int seppos = appVersions.indexOf(';', eqpos + 1);
        final String appVersion = seppos == -1
                ? appVersions.substring(eqpos + 1)
                : appVersions.substring(eqpos + 1, seppos);

        return new AbstractMap.SimpleEntry<>(appName, appVersion);
    }
}
