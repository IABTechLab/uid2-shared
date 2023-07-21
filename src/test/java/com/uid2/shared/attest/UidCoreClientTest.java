package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.cloud.ICloudStorage;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.apache.http.protocol.HttpService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.mockito.Mockito.*;

public class UidCoreClientTest{
    private Proxy proxy = CloudUtils.defaultProxy;
    private AttestationTokenRetriever attestationTokenRetriever = mock(AttestationTokenRetriever.class);
    private UidCoreClient uidCoreClient = new UidCoreClient(
            "core_attest_url", "userToken", new ApplicationVersion("appName", "appVersion"), proxy,
            mock(IAttestationProvider.class), true);

    public UidCoreClientTest() throws Exception {
    }

    public String convertInputStreamToString(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    @Test
    public void testDownload() throws IOException, CloudStorageException, InterruptedException {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);

        uidCoreClient.setAttestationTokenRetriever(attestationTokenRetriever);

        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://core-test.uidapi.com/attest"))
                .GET()
                .setHeader(Const.Http.AppVersionHeader, "appName=appVersion")
                .build();
        uidCoreClient.setHttpClient(mockHttpClient);

        InputStream result = uidCoreClient.download("https://core-test.uidapi.com/attest");
    }

}
