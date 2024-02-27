package com.uid2.shared.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.pattern.FormattingConverter;
import org.junit.jupiter.api.AfterAll;
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
public class MaskingLokiJsonEncoderTest {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(MaskingLokiJsonEncoderTest.class);
    private static final MaskingLokiJsonEncoder MASKING_LOKI_JSON_ENCODER = new MaskingLokiJsonEncoder();
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
        MaskingLokiJsonEncoder.MessageCfg message = new MaskingLokiJsonEncoder.MessageCfg();
        message.setPattern("%msg %ex");
        MASKING_LOKI_JSON_ENCODER.setMessage(message);
        MASKING_LOKI_JSON_ENCODER.setContext(LOGGER.getLoggerContext());
        MASKING_LOKI_JSON_ENCODER.start();
    }

    @AfterAll
    public static void teardownAll() {
        MASKING_LOKI_JSON_ENCODER.stop();
    }

    @ParameterizedTest
    @MethodSource("maskedMessagesWithS3")
    public void testMaskedMessagesWithS3(String message, String maskedMessage) {
        String log = MASKING_LOKI_JSON_ENCODER.eventToMessage(getLoggingEvent(message));

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
        String log = MASKING_LOKI_JSON_ENCODER.eventToMessage(getLoggingEvent(ex.getMessage(), ex));

        assertAll(
                "testMaskedExceptionsWithS3",
                () -> assertEquals(log.split("\n")[0].trim(), maskedMessage, String.format("'%s' must equal '%s'", log.split("\n")[0].trim(), maskedMessage)),
                () -> assertTrue(log.split("\n")[1].trim().startsWith(stackTrace), String.format("'%s' must start with '%s'", log.split("\n")[1].trim(), stackTrace))
        );
    }

    private static Set<Arguments> maskedExceptionsWithS3() {
        return MASKED_MESSAGES.entrySet().stream()
                .map(entry -> Arguments.of(
                        new Exception(entry.getKey()),
                        entry.getValue() + " java.lang.Exception: " + entry.getValue(),
                        "at " + MaskingLokiJsonEncoderTest.class.getCanonicalName() + ".lambda$maskedExceptionsWithS3"
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
