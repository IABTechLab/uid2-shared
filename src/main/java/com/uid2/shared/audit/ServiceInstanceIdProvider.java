package com.uid2.shared.audit;

import com.uid2.shared.Const;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class ServiceInstanceIdProvider {
    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInstanceIdProvider.class);

    private final String instanceId;

    public ServiceInstanceIdProvider(JsonObject config) {
        this(getPrefixFromConfig(config));
    }

    private static String getPrefixFromConfig(JsonObject config) {
        if (config == null || !config.containsKey(Const.Config.InstanceIdPrefixProp)) {
            throw new IllegalArgumentException("ServiceInstanceIdProvider requires 'instance_id_prefix' in config");
        }

        return config.getString(Const.Config.InstanceIdPrefixProp);
    }

    public ServiceInstanceIdProvider(String instanceIdPrefix) {
        this(instanceIdPrefix, Long.toHexString(RANDOM.nextLong()));
    }

    public ServiceInstanceIdProvider(String instanceIdPrefix, String instanceIdSuffix) {
        if (instanceIdPrefix == null || instanceIdPrefix.isEmpty()) {
            throw new IllegalArgumentException("ServiceInstanceIdProvider requires a non-empty instance ID");
        }

        this.instanceId = String.format("%s-%s", instanceIdPrefix, instanceIdSuffix);
        LOGGER.info("Service instance ID initialized: {}", this.instanceId);
    }

    public String getInstanceId() {
        return instanceId;
    }
}
