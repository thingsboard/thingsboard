// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.query;

public enum EntityFilterType {
    SINGLE_ENTITY("singleEntity"),
    ENTITY_GROUP("entityGroup"),
    ENTITY_LIST("entityList"),
    ENTITY_NAME("entityName"),
    ENTITY_TYPE("entityType"),
    ENTITY_GROUP_LIST("entityGroupList"),
    ENTITY_GROUP_NAME("entityGroupName"),
    ENTITIES_BY_GROUP_NAME("entitiesByGroupName"),
    STATE_ENTITY("stateEntity"),
    STATE_ENTITY_OWNER("stateEntityOwner"),
    ASSET_TYPE("assetType"),
    DEVICE_TYPE("deviceType"),
    ENTITY_VIEW_TYPE("entityViewType"),
    EDGE_TYPE("edgeType"),
    RELATIONS_QUERY("relationsQuery"),
    ASSET_SEARCH_QUERY("assetSearchQuery"),
    DEVICE_SEARCH_QUERY("deviceSearchQuery"),
    ENTITY_VIEW_SEARCH_QUERY("entityViewSearchQuery"),
    EDGE_SEARCH_QUERY("edgeSearchQuery"),
    API_USAGE_STATE("apiUsageState"),
    SCHEDULER_EVENT("schedulerEvent");

    private final String label;

    EntityFilterType(String label) {
        this.label = label;
    }
}
