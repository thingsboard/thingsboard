// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DoughnutLayout {
    DEFAULT("default"),
    WITH_TOTAL("with_total");

    private final String label;

    DoughnutLayout(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static DoughnutLayout fromLabel(String value) {
        for (DoughnutLayout layout : values()) {
            if (layout.label.equalsIgnoreCase(value)) {
                return layout;
            }
        }
        throw new IllegalArgumentException("Unknown DoughnutLayout: " + value);
    }
}
