package com.uid2.shared.attest;

import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.util.URLConnectionHttpClient;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Proxy;
import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

public class UidCoreClientTest {
    private Proxy proxy = CloudUtils.defaultProxy;
    private AttestationTokenRetriever mockAttestationTokenRetriever = mock(AttestationTokenRetriever.class);

    private URLConnectionHttpClient mockHttpClient = mock(URLConnectionHttpClient.class);

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
    public void Download_Succeed_RequestSentWithExpectedParameters() throws IOException, CloudStorageException, AttestationTokenRetrieverException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);

        when(mockAttestationTokenRetriever.getAttestationToken()).thenReturn("testAttestationToken");
        when(mockAttestationTokenRetriever.getCoreJWT()).thenReturn("testCoreJWT");
        when(mockAttestationTokenRetriever.getOptOutJWT()).thenReturn("testOptOutJWT");
        uidCoreClient.setUserToken("testUserToken");

        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(Const.Http.AppVersionHeader, "testAppVersionHeader");
        expectedHeaders.put("Authorization", "Bearer testUserToken");
        expectedHeaders.put("Attestation-Token", "testAttestationToken");
        expectedHeaders.put("Attestation-JWT", "testCoreJWT");
        when(mockHttpClient.get("https://download", expectedHeaders)).thenReturn(mockHttpResponse);

        uidCoreClient.download("https://download");
        verify(mockAttestationTokenRetriever, times(1)).attest();
        verify(mockHttpClient, times(1)).get("https://download", expectedHeaders);
    }

    @Test
    public void DownloadWithAttest_UsesJWTSecondCall() throws IOException, CloudStorageException, AttestationTokenRetrieverException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);

        // this test checks that if the getCoreJWT returns null, then the UidCoreClient will call getCoreJWT after attestation to get the
        // expected value
        when(mockAttestationTokenRetriever.getAttestationToken()).thenReturn("testAttestationToken");
        when(mockAttestationTokenRetriever.getCoreJWT()).thenReturn(null,"testCoreJWT");
        when(mockAttestationTokenRetriever.getOptOutJWT()).thenReturn("testOptOutJWT");
        uidCoreClient.setUserToken("testUserToken");

        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(Const.Http.AppVersionHeader, "testAppVersionHeader");
        expectedHeaders.put("Authorization", "Bearer testUserToken");
        expectedHeaders.put("Attestation-Token", "testAttestationToken");
        expectedHeaders.put("Attestation-JWT", "testCoreJWT");

        when(mockHttpClient.get("https://download", expectedHeaders)).thenReturn(mockHttpResponse);

        uidCoreClient.download("https://download");
        assertAll(
                () -> verify(mockAttestationTokenRetriever, times(1)).attest(),
                () -> verify(mockAttestationTokenRetriever, times(2)).getCoreJWT(),
                () -> verify(mockHttpClient, times(1)).get("https://download", expectedHeaders)
        );
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

        when(mockHttpClient.get(eq("https://download"), any(HashMap.class))).thenReturn(mockHttpResponse);

        uidCoreClient.download("https://download");
        verify(mockAttestationTokenRetriever, times(2)).attest();
    }

    @Test
    void getJwtReturnsCoreToken() {
        when(mockAttestationTokenRetriever.getOptOutJWT()).thenReturn("optOutJWT");
        when(mockAttestationTokenRetriever.getCoreJWT()).thenReturn("coreJWT");
        Assertions.assertEquals("coreJWT", this.uidCoreClient.getJWT());
    }
}
