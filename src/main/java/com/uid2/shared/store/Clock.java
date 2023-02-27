package com.uid2.shared.store;

import java.time.Instant;

public interface Clock {
    Instant now();
}
