package com.uid2.shared;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ApplicationVersion {
    private final String appName;
    private final String appVersion;
    private final Map<String, String> componentVersions;

    public static ApplicationVersion load(String appName, String... componentNames) throws IOException {
        Map<String, String> componentVersions = new HashMap<>();
        for (String componentName : componentNames)
            componentVersions.put(componentName, loadVersion(componentName));
        return new ApplicationVersion(appName, loadVersion(appName), componentVersions);
    }

    public ApplicationVersion(String appName, String appVersion) {
        this(appName, appVersion, new HashMap<>());
    }

    public ApplicationVersion(String appName, String appVersion, Map<String, String> componentVersions) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.componentVersions = Collections.unmodifiableMap(componentVersions);
    }

    public String getAppName() { return appName; }
    public String getAppVersion() { return appVersion; }
    public Map<String, String> getComponentVersions() { return componentVersions; }

    private static String loadVersion(String componentName) throws IOException {
        InputStream is = ApplicationVersion.class.getClassLoader().getResourceAsStream(componentName + ".properties");
        Properties properties = new Properties();
        properties.load(is);
        return properties.getProperty("image.version");
    }
}