// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.image;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum ImageAlignment {

    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    @Getter
    private final String value;

    ImageAlignment(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ImageAlignment fromLabel(String value) {
        for (ImageAlignment type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ImageAlignment: " + value);
    }
}
