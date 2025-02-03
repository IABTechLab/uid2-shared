package com.uid2.shared.secure;

import com.uid2.shared.Utils;
import com.uid2.shared.secure.azurecc.IMaaTokenSignatureValidator;
import com.uid2.shared.secure.azurecc.IPolicyValidator;
import com.uid2.shared.secure.azurecc.MaaTokenSignatureValidator;
import com.uid2.shared.secure.azurecc.PolicyValidator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// CC stands for Confidential Container
@Slf4j
public class AzureCCAksCoreAttestationService extends AzureCCCoreAttestationServiceBase {
    private static final String AZURE_CC_PROTOCOL = "azure-cc-aks";

    public AzureCCAksCoreAttestationService(String maaServerBaseUrl, String attestationUrl) {
        super(new MaaTokenSignatureValidator(maaServerBaseUrl), new PolicyValidator(attestationUrl), AZURE_CC_PROTOCOL);
    }

    // used in UT
    protected AzureCCAksCoreAttestationService(IMaaTokenSignatureValidator tokenSignatureValidator, IPolicyValidator policyValidator) {
        super(tokenSignatureValidator, policyValidator, AZURE_CC_PROTOCOL);
    }
}
