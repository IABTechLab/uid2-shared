package com.uid2.shared.secure.gcpoidc;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.uid2.shared.Utils;
import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicyValidator implements IPolicyValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyValidator.class);

    public static final String ENV_ENVIRONMENT = "DEPLOYMENT_ENVIRONMENT";
    public static final String ENV_OPERATOR_API_KEY_SECRET_NAME = "API_TOKEN_SECRET_NAME";
    public static final String ENV_CORE_ENDPOINT = "CORE_BASE_URL";
    public static final String ENV_OPT_OUT_ENDPOINT = "OPTOUT_BASE_URL";

    public static final String EU_REGION_PREFIX = "europe";

    private static final List<String> REQUIRED_ENV_OVERRIDES = ImmutableList.of(
            ENV_ENVIRONMENT,
            ENV_OPERATOR_API_KEY_SECRET_NAME
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
        checkRegion(payload);
        var isDebugMode = checkConfidentialSpace(payload);
        var digest = checkWorkload(payload);
        checkCmdOverrides(payload);
        var env = checkEnvOverrides(payload);
        return generateEnclaveId(isDebugMode, digest, env);
    }

    private static boolean checkConfidentialSpace(TokenPayload payload) throws AttestationException{
        if(!payload.isConfidentialSpaceSW()){
            throw new AttestationClientException("Unexpected SW_NAME: " + payload.getSwName());
        }
        var isDebugMode = payload.isDebugMode();
        if(!isDebugMode && !payload.isStableVersion()){
            throw new AttestationClientException("Confidential space image version is not stable.");
        }
        return isDebugMode;
    }

    private static String checkWorkload(TokenPayload payload) throws AttestationException{
        if(!payload.isRestartPolicyNever()){
            throw new AttestationClientException("Restart policy is not set to Never. Value: " + payload.getRestartPolicy());
        }
        return payload.getWorkloadImageDigest();
    }

    // We don't support to launch UID2 instance in EU.
    // Currently, there's no GCP serving options in China mainland, so we will skip the check for CN.
    // More details about zone in https://cloud.google.com/compute/docs/regions-zones.
    private static String checkRegion(TokenPayload payload) throws AttestationException{
        var region = payload.getGceZone();
        if(Strings.isNullOrEmpty(region) || region.startsWith(EU_REGION_PREFIX)){
            throw new AttestationClientException("Region is not supported. Value: " + region);
        }
        return region;
    }

    private static void checkCmdOverrides(TokenPayload payload) throws AttestationException{
        if(!CollectionUtils.isEmpty(payload.getCmdOverrides())){
            throw new AttestationClientException("Payload should not have cmd overrides");
        }
    }

    private Environment checkEnvOverrides(TokenPayload payload) throws AttestationException{
        var envOverrides = payload.getEnvOverrides();
        if(MapUtils.isEmpty(envOverrides)){
            throw new AttestationClientException("env overrides should not be empty");
        }
        HashMap<String, String> envOverridesCopy = new HashMap(envOverrides);

        // check all required env overrides
        for(var envKey: REQUIRED_ENV_OVERRIDES){
            if(Strings.isNullOrEmpty(envOverridesCopy.get(envKey))){
                throw new AttestationClientException("Required env override is missing. key: " + envKey);
            }
        }

        // env could be parsed
        var env = Environment.fromString(envOverridesCopy.get(ENV_ENVIRONMENT));
        if(env == null){
            throw new AttestationClientException("Environment can not be parsed. " + envOverridesCopy.get(ENV_ENVIRONMENT));
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
            throw new AttestationClientException("More env overrides than allowed. " + envOverridesCopy);
        }

        return env;
    }

    private String generateEnclaveId(boolean isDebugMode, String imageDigest, Environment env) throws AttestationException {
        var str = String.format("%s,%s,%s", getVersion(), isDebugMode, imageDigest);
        LOGGER.info("Meta used to generate GCP EnclaveId: " + str);
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
