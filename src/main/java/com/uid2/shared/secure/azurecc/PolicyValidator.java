package com.uid2.shared.secure.azurecc;

import com.google.common.base.Strings;
import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.AttestationFailure;
import com.uid2.shared.util.UrlEquivalenceValidator;

public class PolicyValidator implements IPolicyValidator{
    private static final String LOCATION_CHINA = "china";
    private static final String LOCATION_EU = "europe";
    private String attestationUrl;

    public PolicyValidator(String attestationUrl) {
        this.attestationUrl = attestationUrl;
    }
    @Override
    public String validate(MaaTokenPayload maaTokenPayload, String publicKey) throws AttestationException {
        verifyVM(maaTokenPayload);
        verifyLocation(maaTokenPayload);
        verifyPublicKey(maaTokenPayload, publicKey);
        verifyAttestationUrl(maaTokenPayload);
        return maaTokenPayload.getCcePolicyDigest();
    }

    private void verifyPublicKey(MaaTokenPayload maaTokenPayload, String publicKey) throws AttestationException {
        if(Strings.isNullOrEmpty(publicKey)){
            throw new AttestationClientException("public key to check is null or empty", AttestationFailure.BAD_FORMAT);
        }
        var runtimePublicKey = maaTokenPayload.getRuntimeData().getPublicKey();
        if(!publicKey.equals(runtimePublicKey)){
            throw new AttestationClientException(
                    String.format("Public key in payload does not match expected value. More info: runtime(%s), expected(%s)",
                            runtimePublicKey,
                            publicKey
                    ),
                    AttestationFailure.BAD_FORMAT);
        }
    }

    private void verifyAttestationUrl(MaaTokenPayload maaTokenPayload) throws AttestationException {
        String decodedRuntimeAttestationUrl = maaTokenPayload.getRuntimeData().getDecodedAttestationUrl();
        if (maaTokenPayload != null && !UrlEquivalenceValidator.areUrlsEquivalent(decodedRuntimeAttestationUrl, this.attestationUrl)) {
            throw new AttestationClientException("The given attestation URL is unknown. Given URL: " + decodedRuntimeAttestationUrl, AttestationFailure.UNKNOWN_ATTESTATION_URL);
        }
    }

    private void verifyVM(MaaTokenPayload maaTokenPayload) throws AttestationException {
        if(!maaTokenPayload.isSevSnpVM()){
            throw new AttestationClientException("Not in SevSnp VM", AttestationFailure.BAD_FORMAT);
        }
        if(!maaTokenPayload.isUtilityVMCompliant()){
            throw new AttestationClientException("Not run in Azure Compliance Utility VM", AttestationFailure.BAD_FORMAT);
        }
        if(maaTokenPayload.isVmDebuggable()){
            throw new AttestationClientException("The underlying hardware should not run in debug mode", AttestationFailure.BAD_FORMAT);
        }
    }

    private void verifyLocation(MaaTokenPayload maaTokenPayload) throws AttestationException {
        var location = maaTokenPayload.getRuntimeData().getLocation();
        if(Strings.isNullOrEmpty(location)){
            throw new AttestationClientException("Location is not specified.", AttestationFailure.BAD_PAYLOAD);
        }
        var lowerCaseLocation = location.toLowerCase();
        if(lowerCaseLocation.contains(LOCATION_CHINA) ||
           lowerCaseLocation.contains(LOCATION_EU)){
            throw new AttestationClientException("Location is not supported. Value: " + location, AttestationFailure.BAD_PAYLOAD);
        }
    }
}
