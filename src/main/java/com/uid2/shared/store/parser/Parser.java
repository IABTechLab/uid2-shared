package com.uid2.shared.store.parser;

import java.io.IOException;
import java.io.InputStream;

public interface Parser<T> {
    ParsingResult<T> deserialize(InputStream inputStream) throws IOException;
}
