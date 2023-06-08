package com.uid2.shared.store;

import java.time.Instant;

public interface IKeysetProvider {
    IKeysetSnapshot getSnapshot(Instant asOf);
    IKeysetSnapshot getSnapshot();
}
