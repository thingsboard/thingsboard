// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.permission;

import jakarta.annotation.Nullable;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.Set;
import java.util.function.Function;


public interface OwnersCacheService {

    Set<EntityId> fetchOwnersHierarchy(TenantId tenantId, EntityId entityId);

    Set<EntityId> getOwners(TenantId tenantId, EntityId entityId, HasOwnerId hasOwnerId);

    Set<EntityId> getOwners(TenantId tenantId, EntityGroupId entityGroupId);

    @Nullable
    EntityId getOwner(TenantId tenantId, EntityId entityId);

    void clearOwners(EntityId entityId);

    Set<EntityId> getChildOwners(TenantId tenantId, EntityId parentOwnerId);

    void changeDashboardOwner(TenantId tenantId, EntityId targetOwnerId, Dashboard dashboard) throws ThingsboardException;

    void changeUserOwner(TenantId tenantId, EntityId targetOwnerId, User user) throws ThingsboardException;

    void changeCustomerOwner(TenantId tenantId, EntityId targetOwnerId, Customer customer) throws ThingsboardException;

    void changeEntityViewOwner(TenantId tenantId, EntityId targetOwnerId, EntityView entityView) throws ThingsboardException;

    void changeEdgeOwner(TenantId tenantId, EntityId targetOwnerId, Edge edge) throws ThingsboardException;

    void changeAssetOwner(TenantId tenantId, EntityId targetOwnerId, Asset asset) throws ThingsboardException;

    void changeDeviceOwner(TenantId tenantId, EntityId targetOwnerId, Device device) throws ThingsboardException;

    void changeAgentOwner(TenantId tenantId, EntityId targetOwnerId, Agent agent) throws ThingsboardException;

    void changeEntityOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId) throws ThingsboardException;

    boolean isChildOwner(TenantId tenantId, CustomerId parentOwnerId, CustomerId childOwnerId);

    <E extends BaseData<? extends UUIDBased>> PageData<E>
    getGroupEntities(TenantId tenantId, SecurityUser securityUser, EntityType entityType, Operation operation, PageLink pageLink,
                     Function<List<EntityGroupId>, PageData<E>> getEntitiesFunction) throws Exception;

    void changeEntityOwner(TenantId tenantId, EntityId entityId, EntityId targetOwnerId, EntityId currentOwnerId) throws ThingsboardException;

    Set<EntityId> getOwnedEntities(TenantId tenantId, EntityId ownerId, int fetchPackSize);

}
