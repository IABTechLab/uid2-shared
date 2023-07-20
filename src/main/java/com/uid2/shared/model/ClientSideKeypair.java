package com.uid2.shared.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public class ClientSideKeypair {

    private final String subscriptionId;
    private final byte[] publicKeyBytes;
    private final byte[] privateKeyBytes;

    private final int siteId;

    private final String contact;

    private final Instant created;

    private final boolean disabled;

    public ClientSideKeypair(String subscriptionId, byte[] publicKeyBytes, byte[] privateKeyBytes, int siteId, String contact, Instant created, boolean disabled){
        this.subscriptionId = subscriptionId;
        this.publicKeyBytes = publicKeyBytes;
        this.privateKeyBytes = privateKeyBytes;
        this.siteId = siteId;
        this.contact = contact;
        this.created = created;
        this.disabled = disabled;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public byte[] getPublicKeyBytes() {
        return publicKeyBytes;
    }

    public byte[] getPrivateKeyBytes() {
        return privateKeyBytes;
    }

    public int getSiteId() {
        return siteId;
    }

    public String getContact() {
        return contact;
    }

    public Instant getCreated() {
        return created;
    }

    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if(!(o instanceof ClientSideKeypair)) return false;

        ClientSideKeypair b = (ClientSideKeypair) o;
        return this.subscriptionId.equals(b.subscriptionId)
                && Arrays.equals(this.publicKeyBytes, b.publicKeyBytes)
                && Arrays.equals(this.privateKeyBytes, b.privateKeyBytes)
                && this.siteId == b.siteId
                && this.created.equals(b.created)
                && this.contact.equals(b.contact)
                && this.disabled == b.disabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId, Arrays.hashCode(publicKeyBytes), Arrays.hashCode(privateKeyBytes), siteId, created, contact, disabled);
    }

}
