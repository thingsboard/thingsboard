// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.image;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum ImageSourceType {

    IMAGE("image"),
    ENTITY_KEY("entityKey");

    @Getter
    private final String value;

    ImageSourceType(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ImageSourceType fromLabel(String value) {
        for (ImageSourceType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ImageSourceType: " + value);
    }
}
