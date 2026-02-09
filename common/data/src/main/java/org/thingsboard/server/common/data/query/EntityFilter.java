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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import static org.thingsboard.server.common.data.query.AliasEntityId.resolveAliasEntityId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleEntityFilter.class, name = "singleEntity"),
        @JsonSubTypes.Type(value = EntityListFilter.class, name = "entityList"),
        @JsonSubTypes.Type(value = EntityNameFilter.class, name = "entityName"),
        @JsonSubTypes.Type(value = EntityTypeFilter.class, name = "entityType"),
        @JsonSubTypes.Type(value = AssetTypeFilter.class, name = "assetType"),
        @JsonSubTypes.Type(value = DeviceTypeFilter.class, name = "deviceType"),
        @JsonSubTypes.Type(value = EdgeTypeFilter.class, name = "edgeType"),
        @JsonSubTypes.Type(value = EntityViewTypeFilter.class, name = "entityViewType"),
        @JsonSubTypes.Type(value = ApiUsageStateFilter.class, name = "apiUsageState"),
        @JsonSubTypes.Type(value = RelationsQueryFilter.class, name = "relationsQuery"),
        @JsonSubTypes.Type(value = AssetSearchQueryFilter.class, name = "assetSearchQuery"),
        @JsonSubTypes.Type(value = DeviceSearchQueryFilter.class, name = "deviceSearchQuery"),
        @JsonSubTypes.Type(value = EntityViewSearchQueryFilter.class, name = "entityViewSearchQuery"),
        @JsonSubTypes.Type(value = EdgeSearchQueryFilter.class, name = "edgeSearchQuery")
})
public interface EntityFilter {

    @JsonIgnore
    EntityFilterType getType();

    static void resolveEntityFilter(EntityFilter filter, TenantId tenantId, UserId userId, EntityId userOwnerId) {
        if (filter instanceof SingleEntityFilter singleEntityFilter) {
            AliasEntityId resolved = resolveAliasEntityId(singleEntityFilter.getSingleEntity(), tenantId, userId, userOwnerId);
            singleEntityFilter.setSingleEntity(resolved);
        } else if (filter instanceof RelationsQueryFilter queryFilter) {
            AliasEntityId resolved = resolveAliasEntityId(queryFilter.getRootEntity(), tenantId, userId, userOwnerId);
            queryFilter.setRootEntity(resolved);
        } else if (filter instanceof EntitySearchQueryFilter queryFilter) {
            AliasEntityId resolved = resolveAliasEntityId(queryFilter.getRootEntity(), tenantId, userId, userOwnerId);
            queryFilter.setRootEntity(resolved);
        }
    }
}
