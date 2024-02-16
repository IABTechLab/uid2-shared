package com.uid2.shared.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.junit.Assert;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UrlEquivalenceValidatorTest {

    @Mock
    private Logger loggerMock;

    @BeforeEach
    public void setup(){
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "http://example.com;http://example.com/",
            "https://example.com;https://example.com/",
            "https://example.com;https://EXAMPLE.com/",
            "https://example.com;https://example.COM/",
            "https://example.com;HTTPS://example.com/",
            "http://example.com:8080;http://example.com:8080/",
            "http://example.com:8080   ;http://example.com:8080/"
    }, delimiter = ';')
    public void urls_equal(String first, String second) {
        Assert.assertTrue(UrlEquivalenceValidator.areUrlsEquivalent(first, second, loggerMock));
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
        Assert.assertFalse(UrlEquivalenceValidator.areUrlsEquivalent(first, second, loggerMock));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "http//example.com;http://example.com;URL could not be parsed to a valid URL. Given URL: http//example.com",
            "http://example1.com;http//example2.com;URL could not be parsed to a valid URL. Given URL: http//example2.com",
            "foo://example1.com;http://example2.com;URL could not be parsed to a valid URL. Given URL: foo://example1.com",
            "http://example1.com;bar://example2.com;URL could not be parsed to a valid URL. Given URL: bar://example2.com",
    }, delimiter = ';')
    public void urls_invalid(String first, String second, String expectedError) {
        Assert.assertFalse(UrlEquivalenceValidator.areUrlsEquivalent(first, second, loggerMock));
        verify(loggerMock, times(1)).error(eq(expectedError), (Throwable) any());
    }

}
