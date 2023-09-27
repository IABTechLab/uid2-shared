package com.uid2.shared.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;

public class MaskingPatternLayout extends PatternLayout {
    private static final Map<String, String> MASKING_PATTERNS = Map.of(
            "[^\\s]+s3\\.amazonaws\\.com\\/[^\\s]*X-Amz-Security-Token=[^\\s]+", "REDACTED - S3"
    );

    @Override
    public String doLayout(ILoggingEvent event) {
        return mask(super.doLayout(event));
    }

    private String mask(String message) {
        if (message == null) {
            return null;
        }

        String maskedMessage = message;
        for (Map.Entry<String, String> entry : MASKING_PATTERNS.entrySet()) {
            String regex = entry.getKey();
            String mask = entry.getValue();
            maskedMessage = maskedMessage.replaceAll(regex, mask);
        }
        return maskedMessage;
    }
}
