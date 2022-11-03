package com.uid2.shared.health;

public class HealthComponent implements IHealthComponent {
    private String name;
    private boolean isHealthy;
    private String reason;

    public HealthComponent(String name, boolean initialHealthStatus) {
        this.name = name;
        this.isHealthy = initialHealthStatus;
        this.reason = null;
    }

    public void setHealthStatus(boolean newHealthStatus) {
        this.setHealthStatus(newHealthStatus, null);
    }

    public void setHealthStatus(boolean newHealthStatus, String newReason) {
        this.isHealthy = newHealthStatus;
        this.reason = newReason;
    }

    public String name() {
        return this.name;
    }

    public boolean isHealthy() {
        return this.isHealthy;
    }

    public String reason() {
        return this.reason;
    }
}
