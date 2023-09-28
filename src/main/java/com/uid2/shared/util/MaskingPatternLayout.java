package com.uid2.shared.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;
import java.util.regex.Pattern;

public class MaskingPatternLayout extends PatternLayout {
    private static final Map<Pattern, String> MASKING_PATTERNS = Map.of(
            Pattern.compile("\\S+s3\\.amazonaws\\.com/\\S*X-Amz-Security-Token=\\S+"), "REDACTED - S3"
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
        for (Map.Entry<Pattern, String> entry : MASKING_PATTERNS.entrySet()) {
            Pattern pattern = entry.getKey();
            String mask = entry.getValue();
            maskedMessage = pattern.matcher(maskedMessage).replaceAll(mask);
        }
        return maskedMessage;
    }
}
