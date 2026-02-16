/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
