package com.uid2.shared.audit;

import com.uid2.shared.Const;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class UidInstanceIdProvider {
    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = LoggerFactory.getLogger(UidInstanceIdProvider.class);

    private final String instanceId;

    public UidInstanceIdProvider(JsonObject config) {
        this(getPrefixFromConfig(config));
    }

    private static String getPrefixFromConfig(JsonObject config) {
        if (config == null || !config.containsKey(Const.Config.UidInstanceIdPrefixProp)) {
            throw new IllegalArgumentException("UidInstanceIdProvider requires 'uid_instance_id_prefix' in config");
        }

        return config.getString(Const.Config.UidInstanceIdPrefixProp);
    }

    public UidInstanceIdProvider(String instanceIdPrefix) {
        this(instanceIdPrefix, Long.toHexString(RANDOM.nextLong()));
    }

    public UidInstanceIdProvider(String instanceIdPrefix, String instanceIdSuffix) {
        if (instanceIdPrefix == null || instanceIdPrefix.isEmpty() || instanceIdSuffix == null || instanceIdSuffix.isEmpty()) {
            throw new IllegalArgumentException("UidInstanceIdProvider requires a non-empty instance ID");
        }

        this.instanceId = String.format("%s-%s", instanceIdPrefix, instanceIdSuffix);
        LOGGER.info("UID Instance ID initialized: {}", this.instanceId);
    }

    public String getInstanceId() {
        return instanceId;
    }
}
