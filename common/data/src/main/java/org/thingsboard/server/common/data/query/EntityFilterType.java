// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

public enum EntityFilterType {
    SINGLE_ENTITY("singleEntity"),
    ENTITY_LIST("entityList"),
    ENTITY_NAME("entityName"),
    ENTITY_TYPE("entityType"),
    ASSET_TYPE("assetType"),
    DEVICE_TYPE("deviceType"),
    ENTITY_VIEW_TYPE("entityViewType"),
    EDGE_TYPE("edgeType"),
    RELATIONS_QUERY("relationsQuery"),
    ASSET_SEARCH_QUERY("assetSearchQuery"),
    DEVICE_SEARCH_QUERY("deviceSearchQuery"),
    ENTITY_VIEW_SEARCH_QUERY("entityViewSearchQuery"),
    EDGE_SEARCH_QUERY("edgeSearchQuery"),
    API_USAGE_STATE("apiUsageState");

    private final String label;

    EntityFilterType(String label) {
        this.label = label;
    }
}
