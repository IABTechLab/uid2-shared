package com.uid2.shared.secure;

import com.uid2.shared.secure.nitro.AttestationDocument;
import com.uid2.shared.secure.nitro.AttestationRequest;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.uid2.shared.util.UrlEquivalenceValidator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NitroCoreAttestationService implements ICoreAttestationService {

    private final String attestationUrl;
    private final Set<NitroEnclaveIdentifier> allowedEnclaveIds;
    private final ICertificateProvider certificateProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(NitroCoreAttestationService.class);

    public NitroCoreAttestationService(ICertificateProvider certificateProvider, String attestationUrl) {
        this.attestationUrl = attestationUrl;
        this.allowedEnclaveIds = new HashSet<>();
        this.certificateProvider = certificateProvider;
    }

    @Override
    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        try {
            AttestationRequest aReq = AttestationRequest.createFrom(attestationRequest);
            AttestationDocument aDoc = aReq.getAttestationDocument();
            handler.handle(Future.succeededFuture(attestInternal(publicKey, aReq, aDoc)));
        } catch (AttestationClientException ace) {
            handler.handle(Future.succeededFuture(new AttestationResult(ace)));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(new AttestationException(e)));
        }
    }

    private AttestationResult attestInternal(byte[] publicKey, AttestationRequest aReq, AttestationDocument aDoc) throws Exception {
        if (!aReq.verifyData()) {
            return new AttestationResult(AttestationFailure.BAD_PAYLOAD);
        }

        if (publicKey != null && publicKey.length > 0 && !Arrays.equals(publicKey, aDoc.getPublicKey())) {
            return new AttestationResult(AttestationFailure.BAD_PAYLOAD);
        }

        if (!aReq.verifyCertChain(certificateProvider.getRootCertificate())) {
            return new AttestationResult(AttestationFailure.BAD_CERTIFICATE);
        }

        String givenAttestationUrl = aDoc.getUserDataString();
        if (givenAttestationUrl != null && !givenAttestationUrl.isEmpty()) {
            if (!UrlEquivalenceValidator.areUrlsEquivalent(this.attestationUrl, givenAttestationUrl)) {
                return new AttestationResult(AttestationFailure.UNKNOWN_ATTESTATION_URL);
            }
        }

        NitroEnclaveIdentifier id = NitroEnclaveIdentifier.fromRaw(aDoc.getPcr(0));
        if (!allowedEnclaveIds.contains(id)) {
            return new AttestationResult(AttestationFailure.FORBIDDEN_ENCLAVE);
        }

        LOGGER.info("Successfully attested aws-nitro against registered enclaves, enclave id: " + id.toString());
        return new AttestationResult(aDoc.getPublicKey(), id.toString());
    }

    @Override
    public void registerEnclave(String encodedIdentifier) throws AttestationException {
        try {
            // we don't use raw bytes, just checking if it's valid b64
            Base64.getDecoder().decode(encodedIdentifier);
            this.addIdentifier(NitroEnclaveIdentifier.fromBase64(encodedIdentifier));
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public void unregisterEnclave(String encodedIdentifier) throws AttestationException {
        try {
            // we don't use raw bytes, just checking if it's valid b64
            Base64.getDecoder().decode(encodedIdentifier);
            this.removeIdentifier(NitroEnclaveIdentifier.fromBase64(encodedIdentifier));
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public Collection<String> getEnclaveAllowlist() {
        return this.allowedEnclaveIds.stream().map(id -> id.toString()).collect(Collectors.toList());
    }

    public void addIdentifier(NitroEnclaveIdentifier id) {
        this.allowedEnclaveIds.add(id);
    }

    public void removeIdentifier(NitroEnclaveIdentifier id) {
        this.allowedEnclaveIds.remove(id);
    }
}
