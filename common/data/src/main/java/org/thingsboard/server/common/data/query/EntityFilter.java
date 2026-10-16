// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
