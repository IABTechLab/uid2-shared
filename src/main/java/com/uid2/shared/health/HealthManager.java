package com.uid2.shared.health;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class HealthManager {
    public static HealthManager instance = new HealthManager();
    private AtomicReference<List<IHealthComponent>> componentList = new AtomicReference(new ArrayList<IHealthComponent>());

    public synchronized HealthComponent registerComponent(String name) {
        // default healthy if initial status not specified
        return registerComponent(name, true);
    }

    public synchronized HealthComponent registerComponent(String name, boolean initialHealthStatus) {
        HealthComponent component = new HealthComponent(name, initialHealthStatus);
        List<IHealthComponent> newList = new ArrayList<IHealthComponent>(this.componentList.get());
        newList.add(component);
        this.componentList.set(newList);
        return component;
    }

    public synchronized void registerGenericComponent(IHealthComponent component) {
        List<IHealthComponent> newList = new ArrayList<IHealthComponent>(this.componentList.get());
        newList.add(component);
        this.componentList.set(newList);
    }

    public boolean isHealthy() {
        // simple composite logic: service is healthy if none child component is unhealthy
        List<IHealthComponent> list = this.componentList.get();
        return list.stream().filter(c -> !c.isHealthy()).count() == 0;
    }

    public String reason() {
        List<IHealthComponent> list = this.componentList.get();
        // aggregate underlying unhealthy reasons for components that are not healthy
        List<String> reasons = list.stream()
            .filter(c -> !c.isHealthy())
            .map(c -> String.format("%s: %s", c.name(), c.reason()))
            .collect(Collectors.toList());
        return String.join("\n", reasons);
    }

    public void clearComponents() {
        this.componentList.set(new ArrayList<IHealthComponent>());
    }
}
