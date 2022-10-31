package com.uid2.shared.model;

import java.util.Objects;

public class EnclaveIdentifier {
    private final String name;
    private final String protocol;
    private final String identifier;
    private final long created;

    public EnclaveIdentifier(String name, String protocol, String identifier, long created) {
        this.name = name;
        this.protocol = protocol;
        this.identifier = identifier;
        this.created = created;
    }

    public String getName() { return name; }
    public String getProtocol() { return protocol; }
    public String getIdentifier() { return identifier; }
    public long getCreated() { return created; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnclaveIdentifier that = (EnclaveIdentifier) o;
        return protocol.equals(that.protocol) && identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, identifier);
    }
}
