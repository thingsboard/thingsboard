// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.group;

import org.thingsboard.server.common.data.EntityType;

import java.util.HashMap;
import java.util.Map;

public enum EntityField {

    CREATED_TIME("id", false),
    NAME("name"),
    AUTHORITY("authority"),
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    EMAIL("email"),
    TITLE("title"),
    COUNTRY("country"),
    STATE("state"),
    CITY("city"),
    ADDRESS("address"),
    ADDRESS2("address2"),
    ZIP("zip"),
    PHONE("phone"),
    TYPE("type"),
    DEVICE_PROFILE("type"),
    ASSET_PROFILE("type"),
    LABEL("label");

    private final boolean searchable;
    private final String columnName;

    EntityField(String columnName) {
        this(columnName, true);
    }

    EntityField(String columnName, boolean searchable) {
        this.columnName = columnName;
        this.searchable = searchable;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public boolean isSearchable() {
        return this.searchable;
    }

    protected static final Map<EntityType, EntityField[]> defaultFieldsByEntityType =
            new HashMap<>();
    static {
        defaultFieldsByEntityType.put(EntityType.USER, new EntityField[]{CREATED_TIME, FIRST_NAME, LAST_NAME, EMAIL});
        defaultFieldsByEntityType.put(EntityType.CUSTOMER, new EntityField[]{CREATED_TIME, TITLE, EMAIL, COUNTRY, CITY});
        defaultFieldsByEntityType.put(EntityType.ASSET, new EntityField[]{CREATED_TIME, NAME, ASSET_PROFILE});
        defaultFieldsByEntityType.put(EntityType.DEVICE, new EntityField[]{CREATED_TIME, NAME, DEVICE_PROFILE, LABEL});
        defaultFieldsByEntityType.put(EntityType.ENTITY_VIEW, new EntityField[]{CREATED_TIME, NAME, TYPE});
        defaultFieldsByEntityType.put(EntityType.EDGE, new EntityField[]{CREATED_TIME, NAME, TYPE, LABEL});
        defaultFieldsByEntityType.put(EntityType.DASHBOARD, new EntityField[]{CREATED_TIME, TITLE});
        defaultFieldsByEntityType.put(EntityType.AGENT, new EntityField[]{CREATED_TIME, NAME});
    }

}
