package com.uid2.shared.store.parser;

public class ParsingResult<T> {
    private final T data;
    private final Integer count;

    public ParsingResult(T data, Integer count) {
        this.data = data;
        this.count = count;
    }

    public T getData() {
        return data;
    }

    public Integer getCount() {
        return count;
    }
}
