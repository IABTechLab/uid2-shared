// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.health;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HealthManagerTest {

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
