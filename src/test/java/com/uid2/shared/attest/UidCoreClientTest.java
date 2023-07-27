package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.cloud.ICloudStorage;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.apache.http.HttpHeaders;
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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.mockito.Mockito.*;

public class UidCoreClientTest{
    private Proxy proxy = CloudUtils.defaultProxy;
    private AttestationTokenRetriever attestationTokenRetriever = mock(AttestationTokenRetriever.class);

    private HttpClient mockHttpClient = mock(HttpClient.class);

    private UidCoreClient uidCoreClient = new UidCoreClient(
            "core_attest_url", "userToken", new ApplicationVersion("appName", "appVersion"), proxy,
            mock(IAttestationProvider.class), true, mockHttpClient);

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
    public void Download_Succeed_RequestSentWithExpectedParameters() throws IOException, CloudStorageException, InterruptedException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);

        when(attestationTokenRetriever.getAttestationToken()).thenReturn("testAttestationToken");
        uidCoreClient.setAttestationTokenRetriever(attestationTokenRetriever);
        uidCoreClient.setUserToken("testUserToken");

        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://download"))
                .GET()
                .setHeader(Const.Http.AppVersionHeader, "appName=appVersion")
                .setHeader("Authorization", "Bearer testUserToken")
                .setHeader("Attestation-Token", "testAttestationToken")
                .build();

        uidCoreClient.download("https://download");
        verify(mockHttpClient).send(
                argThat(request ->
                    request.uri().equals(httpRequest.uri()) && request.headers().equals(httpRequest.headers())),
                any());
    }

    @Test
    public void Download_EnforceHttpWhenPathNoHttps_ExceptionThrown() {
        when(attestationTokenRetriever.getAttestationToken()).thenReturn("testAttestationToken");
        uidCoreClient.setAttestationTokenRetriever(attestationTokenRetriever);
        uidCoreClient.setEnforceHttps(true);

        CloudStorageException result = Assert.assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("http://download");
        });
        String expectedExceptionMessage = "download http://download error: UidCoreClient requires HTTPS connection";
        Assert.assertEquals(expectedExceptionMessage, result.getMessage());
    }

    @Test
    public void Download_AttestInternalFail_ExceptionThrown() throws IOException, AttestationTokenRetrieverException {
        AttestationTokenRetrieverException exception = new AttestationTokenRetrieverException(401, "test failure");
        doThrow(exception).when(attestationTokenRetriever).attestInternal();
        uidCoreClient.setAttestationTokenRetriever(attestationTokenRetriever);

        CloudStorageException result = Assert.assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://download");
        });
        String expectedExceptionMessage = "download https://download error: http status: 401, test failure";
        Assert.assertEquals(expectedExceptionMessage, result.getMessage());
    }

    @Test
    public void Download_Attest401_AttestInternalCalledTwice() throws CloudStorageException, IOException, InterruptedException, AttestationTokenRetrieverException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.statusCode()).thenReturn(401);

        uidCoreClient.setAttestationTokenRetriever(attestationTokenRetriever);

        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        uidCoreClient.download("https://download");
        verify(attestationTokenRetriever, times(2)).attestInternal();
    }
}
