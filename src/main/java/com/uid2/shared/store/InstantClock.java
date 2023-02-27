package com.uid2.shared.store;

import java.time.Instant;

public class InstantClock implements Clock{
    @Override
    public Instant now() {
        return Instant.now();
    }
}
