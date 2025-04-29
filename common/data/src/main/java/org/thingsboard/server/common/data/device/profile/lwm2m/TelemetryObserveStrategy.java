package org.thingsboard.server.common.data.device.profile.lwm2m;

import lombok.Getter;

public enum TelemetryObserveStrategy {

    SINGLE("One resource equals one single observe request", 0),
    COMPOSITE_ALL("All resources in one composite observe request", 1),
    COMPOSITE_BY_OBJECT("Grouped composite observe requests by object", 2);

    @Getter
    private final String description;

    @Getter
    private final int id;

    TelemetryObserveStrategy(String description, int id) {
        this.description = description;
        this.id = id;
    }

    public static TelemetryObserveStrategy fromDescription(String description) {
        for (TelemetryObserveStrategy strategy : values()) {
            if (strategy.description.equalsIgnoreCase(description)) {
                return strategy;
            }
        }
        return null;
    }

    public static TelemetryObserveStrategy fromId(int id) {
        for (TelemetryObserveStrategy strategy : values()) {
            if (strategy.id == id) {
                return strategy;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name() + " (" + id + "): " + description;
    }
}
