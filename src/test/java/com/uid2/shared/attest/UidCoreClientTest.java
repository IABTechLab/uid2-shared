package com.uid2.shared.attest;

import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.util.URLConnectionHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Proxy;
import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class UidCoreClientTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidCoreClientTest.class);
    private Proxy proxy = CloudUtils.defaultProxy;
    private AttestationResponseHandler mockAttestationResponseHandler = mock(AttestationResponseHandler.class);

    private URLConnectionHttpClient mockHttpClient = mock(URLConnectionHttpClient.class);

    private UidCoreClient uidCoreClient;

    public UidCoreClientTest() throws Exception {
    }

    @BeforeEach
    void setUp() {
        when(mockAttestationResponseHandler.getAppVersionHeader()).thenReturn("testAppVersionHeader");
        uidCoreClient = new UidCoreClient(
                "userToken", proxy,
                mockAttestationResponseHandler, mockHttpClient, false);
    }

    @Test
    public void Download_Succeed_RequestSentWithExpectedParameters() throws IOException, CloudStorageException, AttestationResponseHandlerException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);

        when(mockAttestationResponseHandler.getAttestationToken()).thenReturn("testAttestationToken");
        when(mockAttestationResponseHandler.getCoreJWT()).thenReturn("testCoreJWT");
        when(mockAttestationResponseHandler.getOptOutJWT()).thenReturn("testOptOutJWT");
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
        verify(mockAttestationResponseHandler, times(1)).attest();
        verify(mockHttpClient, times(1)).get("https://download", expectedHeaders);
    }

    @Test
    public void Download_AttestInternalFail_ExceptionThrown() throws IOException, AttestationResponseHandlerException {
        AttestationResponseHandlerException exception = new AttestationResponseHandlerException(AttestationResponseCode.AttestationFailure, "test failure");
        doThrow(exception).when(mockAttestationResponseHandler).attest();

        CloudStorageException result = assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://download");
        });
        String expectedExceptionMessage = "download error: AttestationResponseCode: AttestationFailure, test failure";
        assertEquals(expectedExceptionMessage, result.getMessage());
    }

    @Test
    public void Download_Attest401_getOptOut_NotCalled() throws IOException, AttestationResponseHandlerException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.statusCode()).thenReturn(401);

        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        when(mockHttpClient.get(eq("https://download"), any(HashMap.class))).thenReturn(mockHttpResponse);

        assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://download");
        });

        verify(mockAttestationResponseHandler, times(1)).attest();
        verify(mockAttestationResponseHandler, never()).getOptOutUrl();
    }

    @Test
    void getJwtReturnsCoreToken() {
        when(mockAttestationResponseHandler.getOptOutJWT()).thenReturn("optOutJWT");
        when(mockAttestationResponseHandler.getCoreJWT()).thenReturn("coreJWT");
        Assertions.assertEquals("coreJWT", this.uidCoreClient.getJWT());
    }
}
