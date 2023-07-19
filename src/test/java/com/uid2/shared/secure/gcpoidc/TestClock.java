package com.uid2.shared.secure.gcpoidc;

import com.google.api.client.util.Clock;

public class TestClock implements Clock {
    long currentTimeMs;

    @Override
    public long currentTimeMillis() {
        return currentTimeMs;
    }

    public void setCurrentTimeMs(long currentTimeMs){
        this.currentTimeMs = currentTimeMs;
    }
}
