// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.auth;

import io.vertx.core.json.JsonObject;
import java.util.Objects;

public class OperatorKey implements IAuthorizable {
    private final String key;
    private final String name;
    private final String contact;
    private final String protocol;
    // epochSeconds
    private final long created;

    public OperatorKey(String key, String name, String contact, String protocol, long created) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getContact() {return contact;}
    public String getProtocol() {return protocol;}
    public long getCreated() {return created;}

    public static OperatorKey valueOf(JsonObject json) {
        return new OperatorKey(
                json.getString("key"),
                json.getString("name"),
                json.getString("contact"),
                json.getString("protocol"),
                json.getLong("created")
        );
    }

    @Override
    public boolean hasRole(Role role) {
        return role == Role.OPERATOR;
    }

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) return true;

        // If the object is of a different type, return false
        if (!(o instanceof OperatorKey)) return false;

        OperatorKey b = (OperatorKey) o;

        // Compare the data members and return accordingly
        return this.key.equals(b.key)
                && this.name.equals(b.name)
                && this.contact.equals(b.contact)
                && this.protocol.equals(b.protocol)
                && this.created == b.created;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, contact, protocol, created);
    }
}
