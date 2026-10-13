// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ColumnConfiguration {

    private ColumnType type;
    private String key;
    private SortOrder sortOrder;

    public ColumnConfiguration() {}

    public ColumnConfiguration(ColumnType type, String key) {
        this(type, key, SortOrder.NONE);
    }

    public ColumnConfiguration(ColumnType type, String key, SortOrder sortOrder) {
        this.type = type;
        this.key = key;
        this.sortOrder = sortOrder;
    }

    public ColumnType getType() {
        return type;
    }

    public void setType(ColumnType type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
