package com.uid2.shared.jmx;

public interface AdminApiMBean {
    boolean getPublishApiMetrics();

    void setPublishApiMetrics(boolean publishApiMetrics);

    boolean getCaptureRequests();

    void setCaptureRequests(boolean isCapture);

    boolean getCaptureFailureOnly();

    void setCaptureFailureOnly(boolean isFailureOnly);

    int getMaxCapturedRequests();

    void setMaxCapturedRequests(int maxRequests);

    String getApiContactRegex();

    void setApiContactRegex(String pattern);

    String dumpCaptureRequests();

    String[] getCapturedRequests();
}
