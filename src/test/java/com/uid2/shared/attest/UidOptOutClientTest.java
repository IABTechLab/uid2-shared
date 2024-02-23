package com.uid2.shared.attest;

import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.util.URLConnectionHttpClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.mockito.Mockito.*;

public class UidOptOutClientTest {
    private Proxy proxy = CloudUtils.defaultProxy;
    private AttestationTokenRetriever mockAttestationTokenRetriever = mock(AttestationTokenRetriever.class);
    private URLConnectionHttpClient mockHttpClient = mock(URLConnectionHttpClient.class);
    private UidOptOutClient optOutClient;

    private MockedStatic<LoggerFactory> mockedLoggerFactory;
    private static Logger mockedLogger = mock(Logger.class);

    @BeforeClass
    public static void classSetup() {

    }

    @AfterEach
    public void close() {
        mockedLoggerFactory.close();
    }

    @BeforeEach
    void setUp() {
        mockedLoggerFactory = mockStatic(LoggerFactory.class);
        mockedLoggerFactory.when(() -> LoggerFactory.getLogger(any(Class.class))).thenReturn(mockedLogger);

        when(mockAttestationTokenRetriever.getOptOutJWT()).thenReturn("optOutJWT");
        when(mockAttestationTokenRetriever.getCoreJWT()).thenReturn("coreJWT");

        optOutClient = new UidOptOutClient(
                "userToken", proxy,
                mockAttestationTokenRetriever, mockHttpClient);
    }

    @Test
    void getJwtReturnsOptOutToken() {
        Assertions.assertEquals("optOutJWT", this.optOutClient.getJWT());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "/path1/path2?var=3;https://core.example.com/path1/path2?var=3",
            ";https://core.example.com",
            "/;https://core.example.com/",
    }, delimiter = ';')
    void usesOptOutUrlForDownloadWithPath(String path, String expectedFullPath) throws CloudStorageException, IOException {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "Hello, world!";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.get(any(), any())).thenReturn(mockHttpResponse);

        when(mockAttestationTokenRetriever.attested()).thenReturn(true);
        when(mockAttestationTokenRetriever.getAttestationToken()).thenReturn("testAttToken");
        when(mockAttestationTokenRetriever.getOptOutUrl()).thenReturn("https://core.example.com");

        InputStream is = this.optOutClient.download(path);
        verify(mockAttestationTokenRetriever, atLeastOnce()).getOptOutUrl();
        verify(mockHttpClient).get(eq(expectedFullPath), any());
    }

    @Test
    void returnsEmptyStreamWhenNoOptOutUrl() throws CloudStorageException, IOException {
        when(mockAttestationTokenRetriever.attested()).thenReturn(true);
        when(mockAttestationTokenRetriever.getOptOutUrl()).thenReturn(null);

        InputStream is = this.optOutClient.download("/path");
        Assert.assertEquals(0, is.available());
        verify(mockedLogger).warn("UidOptOutClient attempting to download from path: {}, but OptOutUrl not available", "/path");
    }

    @Test
    void returnsEmptyStreamWhenMalformedUrl() throws CloudStorageException, IOException {
        when(mockAttestationTokenRetriever.attested()).thenReturn(true);
        when(mockAttestationTokenRetriever.getOptOutUrl()).thenReturn("nowhere");

        InputStream is = this.optOutClient.download("/path");
        Assert.assertEquals(0, is.available());
        verify(mockedLogger).error(eq("Unable to parse OptOut URL"), any(MalformedURLException.class));
    }
}
