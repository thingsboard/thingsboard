/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    @Override
    public String toString() {
        return name() + " (" + id + "): " + description;
    }
}
