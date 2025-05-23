package com.uid2.shared.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class HealthManager {
    public static HealthManager instance = new HealthManager();
    private final AtomicReference<List<IHealthComponent>> components = new AtomicReference<>(new ArrayList<>());

    public synchronized HealthComponent registerComponent(String name) {
        // default healthy if initial status not specified
        return registerComponent(name, true);
    }

    public synchronized HealthComponent registerComponent(String name, boolean initialHealthStatus) {
        HealthComponent component = new HealthComponent(name, initialHealthStatus);

        return registerGenericComponent(component);
    }

    public synchronized <T extends IHealthComponent> T registerGenericComponent(T component) {
        var newList = new ArrayList<>(this.components.get());
        newList.add(component);
        this.components.set(newList);

        registerForHealthMetric(component);

        return component;
    }

    public boolean isHealthy() {
        return this.components.get().stream().allMatch(IHealthComponent::isHealthy);
    }

    private void registerForHealthMetric(IHealthComponent component) {
        Gauge
                .builder("uid2_component_health", () -> component.isHealthy() ? 1 : 0)
                .description("Health status of components within a service")
                .tag("component", component.name())
                .register(Metrics.globalRegistry);
    }

    public String reason() {
        // aggregate underlying unhealthy reasons for components that are not healthy
        List<String> reasons = this.components.get().stream()
                .filter(c -> !c.isHealthy())
                .map(c -> String.format("%s: %s", c.name(), c.reason()))
                .collect(Collectors.toList());
        return String.join("\n", reasons);
    }

    public void reset() {
        this.components.set(new ArrayList<>());
    }
}
