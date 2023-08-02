package com.uid2.shared;

import java.time.Instant;

public class InstantClock implements IClock {
    @Override
    public Long getEpochSecond() {
        return Instant.now().getEpochSecond();
    }

    @Override
    public Long getEpochMillis() {
        return Instant.now().toEpochMilli();
    }

    @Override
    public Instant now() {
        return Instant.now();
    }
}
