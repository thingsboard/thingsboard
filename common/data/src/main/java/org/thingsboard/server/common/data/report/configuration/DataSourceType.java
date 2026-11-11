// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DataSourceType {

    DEVICE("device"), ENTITY("entity"), ENTITY_COUNT("entityCount"), ALARM_COUNT("alarmCount");

    private final String label;

    DataSourceType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static DataSourceType fromLabel(String value) {
        for (DataSourceType type : values()) {
            if (type.label.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DataSourceType: " + value);
    }

}
