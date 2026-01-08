package com.uid2.shared.logback;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;
import java.util.regex.Pattern;

public class MaskingPatternLayout extends PatternLayout {
    private static final Pattern maskPattern = Pattern.compile("\\S+s3\\.amazonaws\\.com/\\S*X-Amz-Security-Token=\\S+");
    private static final String maskBroadCheckSubstring = "X-Amz-Security-Token=";
    private static final String maskedRedaction = "REDACTED - S3";

    @Override
    public String doLayout(ILoggingEvent event) {
        return mask(super.doLayout(event));
    }

    private String mask(String message) {
        if (message == null) {
            return null;
        }

        String maskedMessage = message;
        if (maskedMessage.contains(maskBroadCheckSubstring)) {
            maskedMessage = maskPattern.matcher(maskedMessage).replaceAll(maskedRedaction);
        }

        return maskedMessage;
    }
}
