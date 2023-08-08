package com.uid2.shared.attest;

import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.mockito.Mockito.*;

public class UidCoreClientTest {
    private Proxy proxy = CloudUtils.defaultProxy;
    private AttestationTokenRetriever mockAttestationTokenRetriever = mock(AttestationTokenRetriever.class);

    private HttpClient mockHttpClient = mock(HttpClient.class);

    private UidCoreClient uidCoreClient;

    public UidCoreClientTest() throws Exception {
    }

    @BeforeEach
    void setUp() {
        when(mockAttestationTokenRetriever.getAppVersionHeader()).thenReturn("testAppVersionHeader");
        uidCoreClient = new UidCoreClient(
                "userToken", proxy,
                true, mockAttestationTokenRetriever, mockHttpClient);
    }

    @Test
    public void Download_Succeed_RequestSentWithExpectedParameters() throws IOException, CloudStorageException, InterruptedException {
        ArgumentCaptor<HttpRequest> capturedRequest = ArgumentCaptor.forClass(HttpRequest.class);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);

        when(mockAttestationTokenRetriever.getAttestationToken()).thenReturn("testAttestationToken");
        when(mockAttestationTokenRetriever.getCoreJWT()).thenReturn("testCoreJWT");
        when(mockAttestationTokenRetriever.getOptOutJWT()).thenReturn("testOptOutJWT");
        uidCoreClient.setUserToken("testUserToken");

        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        HttpRequest expectedHttpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://download"))
                .GET()
                .setHeader(Const.Http.AppVersionHeader, "testAppVersionHeader")
                .setHeader("Authorization", "Bearer testUserToken")
                .setHeader("Attestation-Token", "testAttestationToken")
                .setHeader("Attestation-JWT", "testCoreJWT")
                .build();

        uidCoreClient.download("https://download");

        verify(mockHttpClient).send(capturedRequest.capture(), any(HttpResponse.BodyHandler.class));
        Assertions.assertEquals(expectedHttpRequest.method(), capturedRequest.getValue().method());
        Assertions.assertEquals(expectedHttpRequest.uri(), capturedRequest.getValue().uri());
        Assertions.assertEquals(expectedHttpRequest.headers(), capturedRequest.getValue().headers());

    }

    @Test
    public void Download_EnforceHttpWhenPathNoHttps_ExceptionThrown() {
        when(mockAttestationTokenRetriever.getAttestationToken()).thenReturn("testAttestationToken");

        CloudStorageException result = Assert.assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("http://download");
        });
        String expectedExceptionMessage = "download http://download error: UidCoreClient requires HTTPS connection";
        Assert.assertEquals(expectedExceptionMessage, result.getMessage());
    }

    @Test
    public void Download_AttestInternalFail_ExceptionThrown() throws IOException, AttestationTokenRetrieverException {
        AttestationTokenRetrieverException exception = new AttestationTokenRetrieverException(401, "test failure");
        doThrow(exception).when(mockAttestationTokenRetriever).attest();

        CloudStorageException result = Assert.assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://download");
        });
        String expectedExceptionMessage = "download https://download error: http status: 401, test failure";
        Assert.assertEquals(expectedExceptionMessage, result.getMessage());
    }

    @Test
    public void Download_Attest401_AttestCalledTwice() throws CloudStorageException, IOException, InterruptedException, AttestationTokenRetrieverException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.statusCode()).thenReturn(401);

        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        uidCoreClient.download("https://download");
        verify(mockAttestationTokenRetriever, times(2)).attest();
    }
}
