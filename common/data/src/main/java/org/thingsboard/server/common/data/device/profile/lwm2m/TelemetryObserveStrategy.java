// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile.lwm2m;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Observation strategy for telemetry. " +
        "SINGLE (0): one resource equals one single observe request. " +
        "COMPOSITE_ALL (1): all resources in one composite observe request. " +
        "COMPOSITE_BY_OBJECT (2): grouped composite observe requests by object.")
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
        throw new IllegalArgumentException("Unknown TelemetryObserveStrategy id: " + description);
    }

    public static TelemetryObserveStrategy fromId(int id) {
        for (TelemetryObserveStrategy strategy : values()) {
            if (strategy.id == id) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown TelemetryObserveStrategy id: " + id);
    }

}
