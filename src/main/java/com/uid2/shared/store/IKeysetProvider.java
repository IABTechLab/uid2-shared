package com.uid2.shared.store;

import com.uid2.shared.auth.Keyset;

import java.time.Instant;
import java.util.Map;

public interface IKeysetProvider {
    IKeysetSnapshot getSnapshot(Instant asOf);
    IKeysetSnapshot getSnapshot();
}