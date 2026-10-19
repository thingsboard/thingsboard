// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum CalculatedFieldType {

    SIMPLE("Simple"),
    SCRIPT("Script"),
    GEOFENCING("Geofencing"),
    ALARM("Alarm"),
    PROPAGATION("Propagation"),
    RELATED_ENTITIES_AGGREGATION("Related entities aggregation"),
    ENTITY_AGGREGATION("Time series data aggregation");

    public static final Set<CalculatedFieldType> all = Collections.unmodifiableSet(EnumSet.allOf(CalculatedFieldType.class));

    @Getter
    private final String displayName;

    CalculatedFieldType(String displayName) {
        this.displayName = displayName;
    }

}
