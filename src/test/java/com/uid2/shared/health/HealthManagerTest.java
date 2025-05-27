package com.uid2.shared.health;


import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HealthManagerTest {

    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        HealthManager.instance.reset();

        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
    }

    @AfterEach
    void tearDown() {
        Metrics.globalRegistry.clear();
        Metrics.globalRegistry.remove(registry);
        registry.close();
    }

    @Test
    public void createComponent_isHealthy() {
        IHealthComponent component = new HealthComponent("test-component", true);
        assertEquals(true, component.isHealthy());
    }

    @Test
    public void createComponent_isUnhealthy() {
        IHealthComponent component = new HealthComponent("test-component", false);
        assertEquals(false, component.isHealthy());
    }

    @Test
    public void registerComponent_isHealthy() {
        IHealthComponent component = HealthManager.instance.registerComponent("test-component");
        assertEquals(true, component.isHealthy());
    }

    @Test
    public void registerComponent_isUnhealthy() {
        IHealthComponent component = HealthManager.instance.registerComponent("test-component", false);
        assertEquals(false, component.isHealthy());
    }

    @Test
    public void initialStatus_isHealthy() {
        assertEquals(true, HealthManager.instance.isHealthy());
    }

    @Test
    public void singleComponent_checkHealthy() {
        HealthComponent component = HealthManager.instance.registerComponent("test-component");
        assertEquals(true, HealthManager.instance.isHealthy());
        assertEquals(true, component.isHealthy());

        component.setHealthStatus(false, "reason1");
        assertEquals(false, component.isHealthy());
        assertEquals("reason1", component.reason());
        assertEquals(false, HealthManager.instance.isHealthy());
        assertEquals("test-component: reason1", HealthManager.instance.reason());
    }

    @Test
    public void multiComponents_checkHealthy() {
        HealthComponent c1 = HealthManager.instance.registerComponent("test-component1");
        HealthComponent c2 = HealthManager.instance.registerComponent("test-component2");
        HealthComponent c3 = HealthManager.instance.registerComponent("test-component3");
        assertEquals(true, HealthManager.instance.isHealthy());

        c1.setHealthStatus(false, "reason1");
        assertEquals(false, c1.isHealthy());
        assertEquals("reason1", c1.reason());
        assertEquals(false, HealthManager.instance.isHealthy());
        assertEquals("test-component1: reason1", HealthManager.instance.reason());
        c1.setHealthStatus(true);

        c2.setHealthStatus(false, "reason2");
        assertEquals(false, c2.isHealthy());
        assertEquals("reason2", c2.reason());
        assertEquals(false, HealthManager.instance.isHealthy());
        assertEquals("test-component2: reason2", HealthManager.instance.reason());
        // not clearing c2

        c3.setHealthStatus(false, "reason3");
        assertEquals(false, c3.isHealthy());
        assertEquals("reason3", c3.reason());
        assertEquals(false, HealthManager.instance.isHealthy());
        assertEquals("test-component2: reason2\ntest-component3: reason3", HealthManager.instance.reason());

        // now clearing all
        c2.setHealthStatus(true);
        c3.setHealthStatus(true);
        assertEquals(true, HealthManager.instance.isHealthy());
    }

    @Test
    public void recordsMetrics_unhealthy() {
        HealthComponent component = HealthManager.instance.registerComponent("test-component");
        component.setHealthStatus(false, "reason");

        assertThat(testComponentHealth(registry)).isEqualTo(0);
    }

    @Test
    public void recordsMetrics_healthy() {
        HealthComponent component = HealthManager.instance.registerComponent("test-component");
        component.setHealthStatus(true, "reason");

        assertThat(testComponentHealth(registry)).isEqualTo(1);
    }

    @Test
    public void recordsMetrics_healthChanges() {
        HealthComponent component = HealthManager.instance.registerComponent("test-component");
        component.setHealthStatus(true, "reason");

        assertThat(testComponentHealth(registry)).isEqualTo(1);

        component.setHealthStatus(false, "different reason");
        assertThat(testComponentHealth(registry)).isEqualTo(0);
    }

    @Test
    public void recordsMetrics_forComponentsRegisteredBeforeRegistrySet() {
        HealthComponent component = HealthManager.instance.registerComponent("test-component");
        component.setHealthStatus(false, "reason");

        assertThat(testComponentHealth(registry)).isEqualTo(0);

    }

    private static double testComponentHealth(SimpleMeterRegistry registry) {
        return registry.get("uid2_component_health").tag("component", "test-component").gauge().value();
    }
}
