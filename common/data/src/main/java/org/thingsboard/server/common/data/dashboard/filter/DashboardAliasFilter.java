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
package org.thingsboard.server.common.data.dashboard.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DashboardSingleEntityFilter.class, name = "singleEntity"),
        @JsonSubTypes.Type(value = DashboardEntityListFilter.class, name = "entityList"),
        @JsonSubTypes.Type(value = DashboardEntityNameFilter.class, name = "entityName"),
        @JsonSubTypes.Type(value = DashboardEntityTypeFilter.class, name = "entityType"),
        @JsonSubTypes.Type(value = DashboardStateEntityFilter.class, name = "stateEntity"),
        @JsonSubTypes.Type(value = DashboardAssetTypeFilter.class, name = "assetType"),
        @JsonSubTypes.Type(value = DashboardDeviceTypeFilter.class, name = "deviceType"),
        @JsonSubTypes.Type(value = DashboardEdgeTypeFilter.class, name = "edgeType"),
        @JsonSubTypes.Type(value = DashboardEntityViewTypeFilter.class, name = "entityViewType"),
        @JsonSubTypes.Type(value = DashboardApiUsageStateFilter.class, name = "apiUsageState"),
        @JsonSubTypes.Type(value = DashboardRelationsQueryFilter.class, name = "relationsQuery"),
        @JsonSubTypes.Type(value = DashboardAssetSearchQueryFilter.class, name = "assetSearchQuery"),
        @JsonSubTypes.Type(value = DashboardDeviceSearchQueryFilter.class, name = "deviceSearchQuery"),
        @JsonSubTypes.Type(value = DashboardEntityViewSearchQueryFilter.class, name = "entityViewSearchQuery"),
        @JsonSubTypes.Type(value = DashboardEdgeSearchQueryFilter.class, name = "edgeSearchQuery")
})
public interface DashboardAliasFilter {}
