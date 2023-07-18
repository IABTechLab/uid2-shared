package com.uid2.shared.secure.gcpoidc;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.uid2.shared.Utils;
import com.uid2.shared.secure.AttestationException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicyValidator implements IPolicyValidator{

    public static final String ENV_ENVIRONMENT = "env";
    public static final String ENV_IDENTITY_SCOPE = "scope";
    public static final String ENV_OPERATOR_API_KEY = "api_key";

    public static final String ENV_CORE_ENDPOINT = "core_endpoint";

    public static final String ENV_OPT_OUT_ENDPOINT = "opt_out_endpoint";

    private static final List<String> REQUIRED_ENV_OVERRIDES = ImmutableList.of(
            ENV_ENVIRONMENT,
            ENV_IDENTITY_SCOPE,
            ENV_OPERATOR_API_KEY
    );

    private static final Map<Environment, List<String>> OPTIONAL_ENV_OVERRIDES_MAP = ImmutableMap.of(
        Environment.Integration, ImmutableList.of(ENV_CORE_ENDPOINT, ENV_OPT_OUT_ENDPOINT)
    );

    @Override
    public String getVersion() {
        return "V1";
    }

    @Override
    public String validate(TokenPayload payload) throws AttestationException {
        var isDebugMode = checkConfidentialSpace(payload);
        var digest = checkWorkload(payload);
        checkCmdOverrides(payload);
        var env = checkEnvOverrides(payload);
        return generateEnclaveId(isDebugMode, digest, env);
    }

    private static boolean checkConfidentialSpace(TokenPayload payload) throws AttestationException{
        if(!payload.isConfidentialSpaceSW()){
            throw new AttestationException("Unexpected SW_NAME: " + payload.getSwName());
        }
        var isDebugMode = payload.isDebugMode();
        if(!isDebugMode && !payload.isStableVersion()){
            throw new AttestationException("Confidential space image version is not stable.");
        }
        return isDebugMode;
    }

    private static String checkWorkload(TokenPayload payload) throws AttestationException{
        if(!payload.isRestartPolicyNever()){
            throw new AttestationException("Restart policy is not set to Never. Value: " + payload.getRestartPolicy());
        }
        return payload.getWorkloadImageDigest();
    }

    private static void checkCmdOverrides(TokenPayload payload) throws AttestationException{
        if(!CollectionUtils.isEmpty(payload.getCmdOverrides())){
            throw new AttestationException("Payload should not have cmd overrides");
        }
    }

    private Environment checkEnvOverrides(TokenPayload payload) throws AttestationException{
        var envOverrides = payload.getEnvOverrides();
        if(MapUtils.isEmpty(envOverrides)){
            throw new AttestationException("env overrides should not be empty");
        }
        HashMap<String, String> envOverridesCopy = new HashMap(envOverrides);

        // check all required env overrides
        for(var envKey: REQUIRED_ENV_OVERRIDES){
            if(Strings.isNullOrEmpty(envOverridesCopy.get(envKey))){
                throw new AttestationException("Required env override is missing. ket: " + envKey);
            }
        }

        // env could be parsed
        var env = Environment.fromString(envOverridesCopy.get(ENV_ENVIRONMENT));
        if(env == null){
            throw new AttestationException("Environment can not be parsed. " + envOverridesCopy.get(ENV_ENVIRONMENT));
        }

        // identityScope could be parsed
        var identityScope = IdentityScope.fromString(envOverridesCopy.get(ENV_IDENTITY_SCOPE));
        if(identityScope == null){
            throw new AttestationException("IdentityScope can not be parsed. " + envOverridesCopy.get(ENV_IDENTITY_SCOPE));
        }

        // make sure there's no unexpected overrides
        for(var envKey: REQUIRED_ENV_OVERRIDES){
            envOverridesCopy.remove(envKey);
        }
        var optionalEnvOverrides = OPTIONAL_ENV_OVERRIDES_MAP.get(env);
        if(!CollectionUtils.isEmpty(optionalEnvOverrides)){
            for(var envKey: optionalEnvOverrides){
                envOverridesCopy.remove(envKey);
            }
        }

        if(!envOverridesCopy.isEmpty()){
            throw new AttestationException("More env overrides than allowed. " + envOverridesCopy);
        }

        return env;
    }

    private String generateEnclaveId(boolean isDebugMode, String imageDigest, Environment env) throws AttestationException {
        var str = String.format("%s,%s,%s", getVersion(), isDebugMode, imageDigest);
        try {
            return getSha256Base64Encoded(str);
        } catch (NoSuchAlgorithmException e) {
            throw new AttestationException(e);
        }
    }

    private static String getSha256Base64Encoded(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // input should contain only US-ASCII chars
        md.update(input.getBytes(StandardCharsets.US_ASCII));
        return Utils.toBase64String(md.digest());
    }
}
