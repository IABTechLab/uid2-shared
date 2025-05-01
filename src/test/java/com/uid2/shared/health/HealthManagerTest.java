package com.uid2.shared.health;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HealthManagerTest {

    @BeforeEach
    public void setUp() {
        File file = new File(File.separator + "app" + File.separator + "pod_terminating");
        file.delete();
        HealthManager.instance.setPodTerminatingCheckInterval(0);
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
        HealthManager.instance.clearComponents();
        IHealthComponent component = HealthManager.instance.registerComponent("test-component");
        assertEquals(true, component.isHealthy());
    }

    @Test
    public void registerComponent_isUnhealthy() {
        HealthManager.instance.clearComponents();
        IHealthComponent component = HealthManager.instance.registerComponent("test-component", false);
        assertEquals(false, component.isHealthy());
    }

    @Test
    public void initialStatus_isHealthy() {
        HealthManager.instance.clearComponents();
        assertEquals(true, HealthManager.instance.isHealthy());
    }

    @Test
    public void singleComponent_checkHealthy() {
        HealthManager.instance.clearComponents();
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
    public void singleComponent_checkPodTerminating() throws IOException {
        HealthManager.instance.setPodTerminatingCheckInterval(3000);
        HealthManager.instance.clearComponents();
        HealthComponent component = HealthManager.instance.registerComponent("test-component");
        assertEquals(true, HealthManager.instance.isHealthy());
        assertEquals(true, component.isHealthy());

        try {
            File file = new File(File.separator + "app" + File.separator + "pod_terminating");
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Wait for the file check interval to pass
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 3000) {
            // Busy wait until the interval has passed
        }
        
        assertEquals(true, component.isHealthy());
        assertEquals(null, component.reason());
        assertEquals(false, HealthManager.instance.isHealthy());
        assertEquals("Pod is terminating", HealthManager.instance.reason());
    }

    @Test
    public void singleComponent_checkPodTerminatingDifferentInterval() throws IOException {
        File file = new File(File.separator + "app" + File.separator + "pod_terminating");
        file.delete();
        HealthManager.instance.clearComponents();
        HealthManager.instance.setPodTerminatingCheckInterval(1000);
        HealthComponent component = HealthManager.instance.registerComponent("test-component");
        assertEquals(true, HealthManager.instance.isHealthy());
        assertEquals(true, component.isHealthy());

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Wait for the file check interval to pass
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 1000) {
            // Busy wait until the interval has passed
        }
        
        assertEquals(true, component.isHealthy());
        assertEquals(null, component.reason());
        assertEquals(false, HealthManager.instance.isHealthy());
        assertEquals("Pod is terminating", HealthManager.instance.reason());
    }

    @Test
    public void multiComponents_checkHealthy() {
        HealthManager.instance.clearComponents();
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
}
