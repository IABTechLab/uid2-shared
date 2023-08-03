package com.uid2.shared;

import java.time.Instant;

public interface IClock {
    Long getEpochSecond();
    Long getEpochMillis();
    Instant now();
}
