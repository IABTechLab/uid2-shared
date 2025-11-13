package com.uid2.shared.attest;

import com.uid2.shared.Const;
import com.uid2.shared.audit.Audit;
import com.uid2.shared.audit.UidInstanceIdProvider;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.util.URLConnectionHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Proxy;
import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class UidCoreClientTest {
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
                mockAttestationResponseHandler, mockHttpClient, false, new UidInstanceIdProvider("test-instance", "id"));
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
        when(mockHttpResponse.statusCode()).thenReturn(200);

        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(Const.Http.AppVersionHeader, "testAppVersionHeader");
        expectedHeaders.put("Authorization", "Bearer testUserToken");
        expectedHeaders.put("Attestation-Token", "testAttestationToken");
        expectedHeaders.put("Attestation-JWT", "testCoreJWT");
        expectedHeaders.put(Audit.UID_INSTANCE_ID_HEADER, "test-instance-id");
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
        String expectedExceptionMessage = "Cannot download required files from UID2 core service, exception: AttestationResponseHandlerException, please visit UID2 guides for troubleshooting";
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

    @Test
    public void Download_Http403Error_LogsStatusCodeAndEndpoint() throws IOException, AttestationResponseHandlerException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.statusCode()).thenReturn(403);
        when(mockHttpClient.get(eq("https://core-prod.uidapi.com/sites/refresh"), any(HashMap.class))).thenReturn(mockHttpResponse);

        CloudStorageException result = assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://core-prod.uidapi.com/sites/refresh");
        });

        assertAll(
            () -> assertTrue(result.getMessage().contains("HTTP response code 403"), 
                "Should contain HTTP status code 403"),
            () -> assertTrue(result.getMessage().contains("Cannot download required files from UID2 core service"), 
                "Should have customer-friendly message"),
            () -> assertTrue(result.getMessage().contains("core-prod.uidapi.com/sites/refresh"), 
                "Should contain safe endpoint"),
            () -> assertTrue(result.getMessage().contains("please visit UID2 guides for troubleshooting"), 
                "Should reference documentation")
        );
    }

    @Test
    public void Download_Http404Error_LogsStatusCode() throws IOException, AttestationResponseHandlerException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.statusCode()).thenReturn(404);
        when(mockHttpClient.get(eq("https://core-prod.uidapi.com/keys/refresh"), any(HashMap.class))).thenReturn(mockHttpResponse);

        CloudStorageException result = assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://core-prod.uidapi.com/keys/refresh");
        });

        assertAll(
            () -> assertTrue(result.getMessage().contains("HTTP response code 404"), 
                "Should contain HTTP status code 404"),
            () -> assertTrue(result.getMessage().contains("core-prod.uidapi.com/keys/refresh"), 
                "Should contain endpoint")
        );
    }

    @Test
    public void Download_Http500Error_LogsStatusCode() throws IOException, AttestationResponseHandlerException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.get(eq("https://core-prod.uidapi.com/salts/refresh"), any(HashMap.class))).thenReturn(mockHttpResponse);

        CloudStorageException result = assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://core-prod.uidapi.com/salts/refresh");
        });

        assertAll(
            () -> assertTrue(result.getMessage().contains("HTTP response code 500"), 
                "Should contain HTTP status code 500"),
            () -> assertTrue(result.getMessage().contains("UID2 core service"), 
                "Should identify source as UID2 core")
        );
    }

    @Test
    public void Download_Http503Error_LogsStatusCode() throws IOException, AttestationResponseHandlerException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.statusCode()).thenReturn(503);
        when(mockHttpClient.get(eq("https://core-integ.uidapi.com/clients/refresh"), any(HashMap.class))).thenReturn(mockHttpResponse);

        CloudStorageException result = assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://core-integ.uidapi.com/clients/refresh");
        });

        assertTrue(result.getMessage().contains("HTTP response code 503"), 
            "Should contain HTTP status code 503");
    }

    @Test
    public void Download_NetworkError_LogsExceptionType() throws IOException, AttestationResponseHandlerException {
        IOException networkException = new IOException("Connection timeout");
        when(mockHttpClient.get(anyString(), any(HashMap.class))).thenThrow(networkException);

        CloudStorageException result = assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://core-prod.uidapi.com/sites/refresh");
        });

        assertAll(
            () -> assertTrue(result.getMessage().contains("Cannot download required files from UID2 core service"), 
                "Should have customer-friendly message"),
            () -> assertTrue(result.getMessage().contains("exception: IOException"), 
                "Should log exception type"),
            () -> assertTrue(result.getMessage().contains("please visit UID2 guides"), 
                "Should reference documentation")
        );
    }

    @Test
    public void Download_EndpointWithQueryParams_LogsOnlyHostAndPath() throws IOException, AttestationResponseHandlerException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.statusCode()).thenReturn(403);
        // URL with query params (simulating potential sensitive data)
        when(mockHttpClient.get(eq("https://core-prod.uidapi.com/sites/refresh?token=secret123"), any(HashMap.class))).thenReturn(mockHttpResponse);

        CloudStorageException result = assertThrows(CloudStorageException.class, () -> {
            uidCoreClient.download("https://core-prod.uidapi.com/sites/refresh?token=secret123");
        });

        assertAll(
            () -> assertTrue(result.getMessage().contains("core-prod.uidapi.com/sites/refresh"), 
                "Should contain host and path"),
            () -> assertFalse(result.getMessage().contains("token=secret123"), 
                "Should NOT contain query parameters with tokens")
        );
    }
}
