package com.uid2.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlEquivalenceValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlEquivalenceValidator.class);
    private UrlEquivalenceValidator() {

    }

    public static Boolean areUrlsEquivalent(String url1, String url2) {
        LOGGER.debug("Checking URLs equivalent: URL1: '{}', URL2: '{}'", url1, url2);
        URL first;
        try {
            first = new URL(url1);
        } catch (MalformedURLException e) {
            LOGGER.error("URL could not be parsed to a valid URL. Given URL: " + url1, e);
            return false;
        }
        URL second;
        try {
            second = new URL(url2);
        } catch (MalformedURLException e) {
            LOGGER.error("URL could not be parsed to a valid URL. Given URL: " + url2, e);
            return false;
        }

        return first.getProtocol().equals(second.getProtocol()) && first.getHost().equalsIgnoreCase(second.getHost()) && first.getPort() == second.getPort();
    }
}
