// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum BorderType {

    SOLID("solid"), DASHED("dashed"), DOTTED("dotted"), DOUBLE("double");

    @Getter
    private final String value;

    BorderType(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Converts the configured width into the CSS border-width.
     * <p>
     * The configured width is the thickness of a single line. For {@link #DOUBLE} the border is drawn as
     * two lines of that thickness with an equal gap between them, so the CSS width is three times larger.
     * This matches how Word stores the width of double borders (LibreOffice multiplies it by 3 when importing
     * Word documents). CSS uses the total width instead, so a "1px double" border would render as a single solid line.
     */
    public int toCssWidth(int lineWidth) {
        return this == DOUBLE ? lineWidth * 3 : lineWidth;
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
