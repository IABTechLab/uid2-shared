package com.uid2.shared.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.pattern.FormattingConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class MaskingPatternLayoutTest {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(MaskingPatternLayoutTest.class);
    private static final MaskingPatternLayout MASKING_PATTERN_LAYOUT = new MaskingPatternLayout();
    private static final String URL_WITHOUT_PROTOCOL = "myservice.s3.amazonaws.com/some/path?param1=value1&X-Amz-Security-Token=mysecurityToken&param3=value3";
    private static final Map<String, String> MASKED_MESSAGES = Map.of(
            "Error: " + URL_WITHOUT_PROTOCOL + " and something else", "Error: REDACTED - S3 and something else",
            "https://" + URL_WITHOUT_PROTOCOL, "REDACTED - S3",
            "http://" + URL_WITHOUT_PROTOCOL, "REDACTED - S3",
            URL_WITHOUT_PROTOCOL, "REDACTED - S3",
            "Should not be redacted", "Should not be redacted"
    );

    @BeforeAll
    public static void setupAll() {
        MASKING_PATTERN_LAYOUT.setPattern("%msg %ex");
        MASKING_PATTERN_LAYOUT.setContext(LOGGER.getLoggerContext());
        MASKING_PATTERN_LAYOUT.start();
    }

    @ParameterizedTest
    @MethodSource("maskedMessagesWithS3")
    public void testMaskedMessagesWithS3(String message, String maskedMessage) {
        String log = MASKING_PATTERN_LAYOUT.doLayout(getLoggingEvent(message));

        assertEquals(maskedMessage, log.trim());
    }

    private static Set<Arguments> maskedMessagesWithS3() {
        return MASKED_MESSAGES.entrySet().stream()
                .map(entry -> Arguments.of(
                        entry.getKey(),
                        entry.getValue()
                ))
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @MethodSource("maskedExceptionsWithS3")
    public void testMaskedExceptionsWithS3(Exception ex, String maskedMessage, String stackTrace) {
        String log = MASKING_PATTERN_LAYOUT.doLayout(getLoggingEvent(ex.getMessage(), ex));

        assertAll(
                "testMaskedExceptionsWithS3",
                () -> assertEquals(log.split("\n")[0].trim(), maskedMessage),
                () -> assertTrue(log.split("\n")[1].trim().startsWith(stackTrace))
        );
    }

    private static Set<Arguments> maskedExceptionsWithS3() {
        return MASKED_MESSAGES.entrySet().stream()
                .map(entry -> Arguments.of(
                        new Exception(entry.getKey()),
                        entry.getValue() + " java.lang.Exception: " + entry.getValue(),
                        "at com.uid2.shared.util.MaskingPatternLayoutTest.lambda$maskedExceptionsWithS3"
                ))
                .collect(Collectors.toSet());
    }

    private static ILoggingEvent getLoggingEvent(String message, Exception ex) {
        return new LoggingEvent(FormattingConverter.class.getName(), LOGGER, Level.ERROR, message, ex, null);
    }

    private static ILoggingEvent getLoggingEvent(String message) {
        return getLoggingEvent(message, null);
    }
}
