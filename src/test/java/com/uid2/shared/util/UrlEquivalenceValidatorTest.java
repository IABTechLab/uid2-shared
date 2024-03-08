package com.uid2.shared.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UrlEquivalenceValidatorTest {
    private MockedStatic<LoggerFactory> mockedLoggerFactory;
    private static Logger mockedLogger = mock(Logger.class);

    @BeforeEach
    public void setup() {
        mockedLoggerFactory = mockStatic(LoggerFactory.class);
        mockedLoggerFactory.when(() -> LoggerFactory.getLogger(UrlEquivalenceValidator.class)).thenReturn(mockedLogger);
    }

    @AfterEach
    public void close() {
        mockedLoggerFactory.close();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "http://example.com;http://example.com/",
            "http://example.com/path1;http://example.com/path2",
            "http://example.com/path1/path2;http://example.com/path2/path1",
            "https://example.com;https://example.com/",
            "https://example.com;https://EXAMPLE.com/",
            "https://example.com;https://example.COM/",
            "https://example.com;HTTPS://example.com/",
            "http://example.com:8080;http://example.com:8080/",
            "http://example.com:8080   ;http://example.com:8080/",
            "https://ade7-113-29-30-226.ngrok-free.app;https://ade7-113-29-30-226.ngrok-free.app/attest"
    }, delimiter = ';')
    public void urls_equal(String first, String second) {
        assertTrue(UrlEquivalenceValidator.areUrlsEquivalent(first, second));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "http://example.com;https://example.com/",
            "http://example.com;https://example.com/",
            "https://example.com:8081;http://example.com:8081/",
            "http://example1.com;http://example.com/",
            "http://example.com;http://example1.com/",
            "http://example.com:8081;http://example.com:8080/",
            "http://example.com:8081;http://example.com:8080/",
    }, delimiter = ';')
    public void urls_not_equal(String first, String second) {
        assertFalse(UrlEquivalenceValidator.areUrlsEquivalent(first, second));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "http//example.com;http://example.com;URL could not be parsed to a valid URL. Given URL: http//example.com",
            "http://example1.com;http//example2.com;URL could not be parsed to a valid URL. Given URL: http//example2.com",
            "foo://example1.com;http://example2.com;URL could not be parsed to a valid URL. Given URL: foo://example1.com",
            "http://example1.com;bar://example2.com;URL could not be parsed to a valid URL. Given URL: bar://example2.com",
            "http://example1.com:abc;http://example2.com;URL could not be parsed to a valid URL. Given URL: http://example1.com:abc",
    }, delimiter = ';')
    public void urls_invalid(String first, String second, String expectedError) {
        assertFalse(UrlEquivalenceValidator.areUrlsEquivalent(first, second));
        verify(mockedLogger).error(eq(expectedError), (Throwable) any());
    }
}
