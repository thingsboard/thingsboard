// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum FontStyle {

    NORMAL("normal"), ITALIC("italic");

    @Getter
    private final String value;

    FontStyle(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FontStyle fromLabel(String value) {
        for (FontStyle type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown FontStyle: " + value);
    }
}
