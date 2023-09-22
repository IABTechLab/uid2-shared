package com.uid2.shared.secure.azurecc;

import com.azure.security.attestation.AttestationClientBuilder;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.uid2.shared.secure.AttestationException;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// MAA certs are stored as x5c(X.509 certificate chain), not supported by Google auth lib.
// So we have to build a thin layer to fetch Azure public key.
public class AzurePublicKeyProvider {

    private final LoadingCache<String, Map<String, PublicKey>> publicKeyCache;

    public AzurePublicKeyProvider() {
        this.publicKeyCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1L, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @Override
                    public Map<String, PublicKey> load(String maaServerBaseUrl) throws AttestationException {
                        return loadPublicKeys(maaServerBaseUrl);
                    }
                });
    }

    public PublicKey GetPublicKey(String maaServerBaseUrl, String kid) throws AttestationException {
        PublicKey key;
        try {
            key = publicKeyCache.get(maaServerBaseUrl).get(kid);
        }
        catch (ExecutionException e){
            throw new AttestationException(
                    String.format("Error fetching PublicKey from certificate location: %s, error: %s.", maaServerBaseUrl, e.getMessage())
            );
        }

        if(key == null){
            throw new AttestationException("Could not find PublicKey for provided keyId: " + kid);
        }
        return key;
    }

    // We don't want to reinvent the wheel. Leverage Azure Attestation client library to fetch certs.
    private static Map<String, PublicKey> loadPublicKeys(String maaServerBaseUrl) throws AttestationException {
        var attestationBuilder = new AttestationClientBuilder();
        var client = attestationBuilder
                .endpoint(maaServerBaseUrl)
                .buildClient();

        var signers = client.listAttestationSigners().getAttestationSigners();

        ImmutableMap.Builder<String, PublicKey> keyCacheBuilder = new ImmutableMap.Builder();

        for (var signer : signers){
            var keyId = signer.getKeyId();
            var certs = signer.getCertificates();
            if(!certs.isEmpty()){
                var publicKey = certs.get(0).getPublicKey();
                keyCacheBuilder.put(keyId, publicKey);
            }
        }

        var map = keyCacheBuilder.build();
        if(map.isEmpty()){
            throw new AttestationException("Fail to load certs from: " + maaServerBaseUrl);
        }

        return map;
    }
}
