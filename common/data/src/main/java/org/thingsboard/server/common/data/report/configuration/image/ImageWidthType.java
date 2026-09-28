// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.image;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum ImageWidthType {

    FIT_WIDTH("fitWidth"),
    ORIGINAL("original"),
    CUSTOM("custom");

    @Getter
    private final String value;

    ImageWidthType(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ImageWidthType fromLabel(String value) {
        for (ImageWidthType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ImageWidthType: " + value);
    }
}
