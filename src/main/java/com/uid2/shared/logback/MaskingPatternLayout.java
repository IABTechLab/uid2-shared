package com.uid2.shared.logback;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

public class MaskingPatternLayout extends PatternLayout {
    private static final Pattern AWS_PRESIGNED_URL_REGEX_PATTERN = Pattern.compile("\\S+s3\\.amazonaws\\.com/\\S*X-Amz-Security-Token=\\S+");
    private static final String AWS_SECURITY_TOKEN_HEADER = "X-Amz-Security-Token=";
    private static final String AWS_PRESIGNED_URL_REDACTION_MASK = "REDACTED - S3";

    @Override
    public String doLayout(ILoggingEvent event) {
        return mask(super.doLayout(event));
    }

    private String mask(String message) {
        if (message == null) {
            return null;
        }

        String maskedMessage = message;
        // Perform a broad check to potentially skip regex
        if (maskedMessage.contains(AWS_SECURITY_TOKEN_HEADER)) {
            maskedMessage = AWS_PRESIGNED_URL_REGEX_PATTERN.matcher(maskedMessage).replaceAll(AWS_PRESIGNED_URL_REDACTION_MASK);
        }

        return maskedMessage;
    }
}
