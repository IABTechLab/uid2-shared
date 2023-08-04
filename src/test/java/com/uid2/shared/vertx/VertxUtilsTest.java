package com.uid2.shared.vertx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class VertxUtilsTest {
    @Test
    void parseClientAppVersionSuccess() {
        final String input = "uid2-operator=2.7.16-SNAPSHOT;uid2-attestation-api=1.1.0;uid2-shared=2.7.0-3e279acefa";
        var result = VertxUtils.parseClientAppVersion(input);
        assertEquals("uid2-operator", result.getKey());
        assertEquals("2.7.16-SNAPSHOT", result.getValue());
    }

    @Test
    void parseEmptyVersion() {
        var result = VertxUtils.parseClientAppVersion("");
        assertNull(result);
    }

    @Test
    void parseNoVersion() {
        var result = VertxUtils.parseClientAppVersion("uid2-operator=");
        assertEquals("uid2-operator", result.getKey());
        assertEquals("", result.getValue());
    }
}
