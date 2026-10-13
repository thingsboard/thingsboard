// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.group;

import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.query.EntityKeyType;

public enum ColumnType {
    CLIENT_ATTRIBUTE(EntityKeyType.CLIENT_ATTRIBUTE, DataConstants.CLIENT_SCOPE),
    SHARED_ATTRIBUTE(EntityKeyType.SHARED_ATTRIBUTE, DataConstants.SHARED_SCOPE),
    SERVER_ATTRIBUTE(EntityKeyType.SERVER_ATTRIBUTE, DataConstants.SERVER_SCOPE),
    TIMESERIES(EntityKeyType.TIME_SERIES),
    ENTITY_FIELD(EntityKeyType.ENTITY_FIELD);

    private final String attributeScope;
    private final EntityKeyType entityKeyType;

    ColumnType(EntityKeyType entityKeyType, String attributeScope) {
        this.entityKeyType = entityKeyType;
        this.attributeScope = attributeScope;
    }

    ColumnType(EntityKeyType entityKeyType) {
        this(entityKeyType, null);
    }

    public String getAttributeScope() {
        return this.attributeScope;
    }

    public boolean isAttribute() {
        return this.attributeScope != null;
    }

    public EntityKeyType getEntityKeyType() {
        return entityKeyType;
    }
}
