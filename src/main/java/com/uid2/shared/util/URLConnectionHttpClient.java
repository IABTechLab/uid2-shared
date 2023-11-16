package com.uid2.shared.util;

import com.uid2.shared.Utils;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class URLConnectionHttpClient {
    private static class URLConnectionHttpResponse<T> implements HttpResponse<T> {
        private final T body;
        private final int statusCode;

        public URLConnectionHttpResponse(int statusCode, T body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return null;
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return null;
        }
    }

    private final Proxy proxy;

    public URLConnectionHttpClient(Proxy proxy) {
        this.proxy = proxy;
    }

    public HttpResponse<String> get(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("GET");
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        int responseCode = connection.getResponseCode();
        String responseBody = responseCode == 200 ? Utils.readToEnd(connection.getInputStream()) : Utils.readToEnd(connection.getErrorStream());

        return new URLConnectionHttpResponse<>(responseCode, responseBody);
    }

    public HttpResponse<String> post(String url, String body, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("POST");
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        connection.setDoOutput(true);
        connection.getOutputStream().write(body.getBytes());

        int responseCode = connection.getResponseCode();
        String responseBody = responseCode == 200 ? Utils.readToEnd(connection.getInputStream()) : Utils.readToEnd(connection.getErrorStream());

        return new URLConnectionHttpResponse<>(responseCode, responseBody);
    }

    private HttpURLConnection openConnection(String url) throws IOException {
        if (proxy == null) {
            return (HttpURLConnection) new URL(url).openConnection();
        } else {
            return (HttpURLConnection) new URL(url).openConnection(proxy);
        }
    }
}
