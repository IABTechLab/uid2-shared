package com.uid2.shared.store;

public class FileName {
    private final String prefix;
    private final String suffix;

    public FileName(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public String toString() {
        return prefix + suffix;
    }
}
