// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.query;

import lombok.Getter;

public enum EntityKeyType {

    ATTRIBUTE("attribute"),
    CLIENT_ATTRIBUTE("clientAttribute"),
    SHARED_ATTRIBUTE("sharedAttribute"),
    SERVER_ATTRIBUTE("serverAttribute"),
    TIME_SERIES("timeseries"),
    ENTITY_FIELD("entityField"),
    ALARM_FIELD("alarm");

    @Getter
    private final String name;

    EntityKeyType(String name) {
        this.name = name;
    }

    public static EntityKeyType fromName(String name) {
        for (EntityKeyType entityKeyType : EntityKeyType.values()) {
            if (entityKeyType.getName().equals(name)) {
                return entityKeyType;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported entity key type : %s", name));
    }

    public boolean isAttribute() {
        return this == ATTRIBUTE || this == CLIENT_ATTRIBUTE || this == SHARED_ATTRIBUTE || this == SERVER_ATTRIBUTE;
    }
}
