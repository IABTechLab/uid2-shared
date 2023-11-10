package com.uid2.shared.secure.azurecc;

import com.google.common.base.Strings;
import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationException;

public class PolicyValidator implements IPolicyValidator{
    private static final String LOCATION_CHINA = "china";
    private static final String LOCATION_EU = "europe";
    @Override
    public String validate(MaaTokenPayload maaTokenPayload, String publicKey) throws AttestationException {
        verifyVM(maaTokenPayload);
        verifyLocation(maaTokenPayload);
        verifyPublicKey(maaTokenPayload, publicKey);
        return maaTokenPayload.getCcePolicyDigest();
    }

    private void verifyPublicKey(MaaTokenPayload maaTokenPayload, String publicKey) throws AttestationException {
        if(Strings.isNullOrEmpty(publicKey)){
            throw new AttestationClientException("public key to check is null or empty");
        }
        var runtimePublicKey = maaTokenPayload.getRuntimeData().getPublicKey();
        if(!publicKey.equals(runtimePublicKey)){
            throw new AttestationClientException(
                    String.format("Public key in payload is not match expected value. More info: runtime(%s), expected(%s)",
                            runtimePublicKey,
                            publicKey
                    ));
        }
    }

    private void verifyVM(MaaTokenPayload maaTokenPayload) throws AttestationException {
        if(!maaTokenPayload.isSevSnpVM()){
            throw new AttestationClientException("Not in SevSnp VM");
        }
        if(!maaTokenPayload.isUtilityVMCompliant()){
            throw new AttestationClientException("Not run in Azure Compliance Utility VM");
        }
        if(maaTokenPayload.isVmDebuggable()){
            throw new AttestationClientException("The underlying harware should not run in debug mode");
        }
    }

    private void verifyLocation(MaaTokenPayload maaTokenPayload) throws AttestationException {
        var location = maaTokenPayload.getRuntimeData().getLocation();
        if(Strings.isNullOrEmpty(location)){
            throw new AttestationClientException("Location is not specified.");
        }
        var lowerCaseLocation = location.toLowerCase();
        if(lowerCaseLocation.contains(LOCATION_CHINA) ||
           lowerCaseLocation.contains(LOCATION_EU)){
            throw new AttestationClientException("Location is not supported. Value: " + location);
        }
    }
}
