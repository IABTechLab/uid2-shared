package com.uid2.shared.model;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public class ClientSideKeypair {

    public static final int KEYPAIR_KEY_PREFIX_LENGTH = 9;

    private final String subscriptionId;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    private final int siteId;

    private final String contact;

    private final Instant created;

    private final boolean disabled;

    private final String publicKeyPrefix;
    private final String privateKeyPrefix;

    public ClientSideKeypair(String subscriptionId, String publicKeyString, String privateKeyString, int siteId, String contact, Instant created, boolean disabled){
        this.subscriptionId = subscriptionId;
        this.siteId = siteId;
        this.contact = contact;
        this.created = created;
        this.disabled = disabled;
        try {
            this.publicKeyPrefix = publicKeyString.substring(0, KEYPAIR_KEY_PREFIX_LENGTH);
            final KeyFactory kf = KeyFactory.getInstance("EC");
            final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString.substring(KEYPAIR_KEY_PREFIX_LENGTH)));
            this.publicKey = kf.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("bad public key");
        }
        try {
            this.privateKeyPrefix = privateKeyString.substring(0, KEYPAIR_KEY_PREFIX_LENGTH);
            final KeyFactory kf = KeyFactory.getInstance("EC");
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString.substring(KEYPAIR_KEY_PREFIX_LENGTH)));
            this.privateKey = kf.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("bad private key");
        }
    }

    public String getPublicKeyPrefix() {
        return publicKeyPrefix;
    }

    public String getPrivateKeyPrefix() {
        return privateKeyPrefix;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public PublicKey getPublicKey()  {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String encodePublicKeyToString() {
        return publicKeyPrefix + Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String encodePrivateKeyToString() {
        return privateKeyPrefix + Base64.getEncoder().encodeToString(privateKey.getEncoded());
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
                && Arrays.equals(this.publicKey.getEncoded(), b.publicKey.getEncoded())
                && Arrays.equals(this.privateKey.getEncoded(), b.privateKey.getEncoded())
                && this.siteId == b.siteId
                && this.created.equals(b.created)
                && this.contact.equals(b.contact)
                && this.disabled == b.disabled
                && this.publicKeyPrefix.equals(b.publicKeyPrefix)
                && this.privateKeyPrefix.equals(b.privateKeyPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId, Arrays.hashCode(publicKey.getEncoded()), Arrays.hashCode(privateKey.getEncoded()), siteId, created, contact, disabled, publicKeyPrefix, privateKeyPrefix);
    }

}
