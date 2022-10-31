package com.uid2.shared.health;

public interface IHealthComponent {
    String name();

    boolean isHealthy();

    String reason();
}
