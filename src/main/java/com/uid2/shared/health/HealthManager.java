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
