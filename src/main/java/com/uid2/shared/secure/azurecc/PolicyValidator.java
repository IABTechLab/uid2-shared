package com.uid2.shared.secure.azurecc;

import com.google.common.base.Strings;
import com.uid2.shared.secure.AttestationException;

public class PolicyValidator implements IPolicyValidator{
    private final String LOCATION_CHINA = "china";
    private final String LOCATION_EU = "europe";
    @Override
    public String validate(MaaTokenPayload maaTokenPayload, String publicKey) throws AttestationException {
        verifyVM(maaTokenPayload);
        verifyLocation(maaTokenPayload);
        verifyPublicKey(maaTokenPayload, publicKey);
        return maaTokenPayload.getCcePolicyDigest();
    }

    private void verifyPublicKey(MaaTokenPayload maaTokenPayload, String publicKey) throws AttestationException {
        if(Strings.isNullOrEmpty(publicKey)){
            throw new AttestationException("public key to check is null or empty");
        }
        var runtimePublicKey = maaTokenPayload.getRuntimeData().getPublicKey();
        if(!publicKey.equals(runtimePublicKey)){
            throw new AttestationException(
                    String.format("Public key in payload is not match expected value. More info: runtime(%s), expected(%s)",
                            runtimePublicKey,
                            publicKey
                    ));
        }
    }

    private void verifyVM(MaaTokenPayload maaTokenPayload) throws AttestationException {
        if(!maaTokenPayload.isSevSnpVM()){
            throw new AttestationException("Not in SevSnp VM");
        }
        if(!maaTokenPayload.isUtilityVMCompliant()){
            throw new AttestationException("Not run in Azure Compliance Utility VM");
        }
        if(maaTokenPayload.isVmDebuggable()){
            throw new AttestationException("The underlying harware should not run in debug mode");
        }
    }

    private void verifyLocation(MaaTokenPayload maaTokenPayload) throws AttestationException {
        var location = maaTokenPayload.getRuntimeData().getLocation();
        if(Strings.isNullOrEmpty(location)){
            throw new AttestationException("Location is not specified.");
        }
        var lowerCaseLocation = location.toLowerCase();
        if(lowerCaseLocation.contains(LOCATION_CHINA) ||
           lowerCaseLocation.contains(LOCATION_EU)){
            throw new AttestationException("Location is not supported. Value: " + location);
        }
    }
}
