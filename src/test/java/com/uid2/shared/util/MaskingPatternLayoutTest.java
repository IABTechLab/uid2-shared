package com.uid2.shared.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class MaskingPatternLayoutTest {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(MaskingPatternLayoutTest.class);
    private static final MaskingPatternLayout MASKING_PATTERN_LAYOUT = new MaskingPatternLayout();

    @BeforeAll
    public static void setupAll() {
        LoggerContext loggerContext = LOGGER.getLoggerContext();
        MASKING_PATTERN_LAYOUT.setPattern("%msg %ex");
        MASKING_PATTERN_LAYOUT.setContext(loggerContext);
        MASKING_PATTERN_LAYOUT.start();
    }

    @ParameterizedTest
    @MethodSource("maskedMessagesWithS3")
    public void testMaskedMessagesWithS3(String message, String maskedMessage) {
        String log = MASKING_PATTERN_LAYOUT.doLayout(getLoggingEvent(message));

        assertAll(
                "testMaskingMessageWithS3",
                () -> assertEquals(maskedMessage, log.trim())
        );
    }

    private static Set<Arguments> maskedMessagesWithS3() {
        String urlWithoutProtocol = "myservice.s3.amazonaws.com/some/path?param1=value1&X-Amz-Security-Token=mysecurityToken&param3=value3";
        Map<String, String> maskedMessages = Map.of(
                "Error: " + urlWithoutProtocol + " and something else", "Error: REDACTED - S3 and something else",
                "https://" + urlWithoutProtocol, "REDACTED - S3",
                "http://" + urlWithoutProtocol, "REDACTED - S3",
                urlWithoutProtocol, "REDACTED - S3"
        );

        return maskedMessages.entrySet().stream()
                .map(entry -> Arguments.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    private static ILoggingEvent getLoggingEvent(String msg, Exception ex) {
        return new LoggingEvent(FormattingConverter.class.getName(), LOGGER, Level.ERROR, msg, ex, null);
    }

    private static ILoggingEvent getLoggingEvent(String msg) {
        return getLoggingEvent(msg, null);
    }
}
