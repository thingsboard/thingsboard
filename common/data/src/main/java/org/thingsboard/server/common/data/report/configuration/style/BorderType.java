// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum BorderType {

    SOLID("solid"), DASHED("dashed"), DOTTED("dotted");

    @Getter
    private final String value;

    BorderType(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BorderType fromLabel(String value) {
        for (BorderType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown BorderType: " + value);
    }
}
