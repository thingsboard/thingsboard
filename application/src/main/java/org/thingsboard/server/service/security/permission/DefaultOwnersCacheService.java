// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.permission;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
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
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
public class DefaultOwnersCacheService implements OwnersCacheService {

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    protected UserPermissionsService userPermissionsService;

    @Override
    public Set<EntityId> fetchOwnersHierarchy(TenantId tenantId, EntityId entityId) {
        return ownerService.fetchOwnersHierarchy(tenantId, entityId);
    }

    @Override
    public Set<EntityId> getOwners(TenantId tenantId, EntityId entityId, HasOwnerId hasOwnerId) {
        return ownerService.getOwners(tenantId, entityId, hasOwnerId);
    }

    @Override
    public Set<EntityId> getOwners(TenantId tenantId, EntityGroupId entityGroupId) {
        return ownerService.getOwners(tenantId, entityGroupId);
    }

    @Override
    @Nullable
    public EntityId getOwner(TenantId tenantId, EntityId entityId) {
        return ownerService.getOwner(tenantId, entityId);
    }

    @Override
    public void clearOwners(EntityId entityId) {
        ownerService.clearOwners(entityId);
    }

    @Override
    public Set<EntityId> getChildOwners(TenantId tenantId, EntityId parentOwnerId) {
        Set<EntityId> result = new HashSet<>();
        try {
            fetchChildOwners(tenantId, parentOwnerId, result);
        } catch (Exception e) {
            log.error("Failed to get child owners by parentOwnerId [{}]", parentOwnerId, e);
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void changeDashboardOwner(TenantId tenantId, EntityId targetOwnerId, Dashboard dashboard) throws ThingsboardException {
        changeEntityOwner(tenantId, targetOwnerId, dashboard.getId(),
                dashboard,
                dashboardService::saveDashboard);
    }

    @Override
    public void changeUserOwner(TenantId tenantId, EntityId targetOwnerId, User user) throws ThingsboardException {
        userPermissionsService.onUserUpdatedOrRemoved(user);
        changeEntityOwner(tenantId, targetOwnerId, user.getId(), user, targetUser -> {
            userService.changeOwner(targetUser, targetOwnerId);
        });
    }

    @Override
    public void changeCustomerOwner(TenantId tenantId, EntityId targetOwnerId, Customer customer) throws ThingsboardException {
        if (customer.isPublic()) {
            throw new ThingsboardException("Public customer owner can't be changed!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        var childOwnerIds = getChildOwners(tenantId, customer.getId());
        if (childOwnerIds.contains(targetOwnerId)) {
            // Making Sub-Customer as a Parent Customer - NOT OK.
            throw new ThingsboardException("Owner of the Customer can't be changed to its Sub-Customer!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        changeEntityOwner(tenantId, targetOwnerId, customer.getId(),
                customer,
                customerService::saveCustomer);
    }

    @Override
    public void changeEntityViewOwner(TenantId tenantId, EntityId targetOwnerId, EntityView entityView) throws ThingsboardException {
        changeEntityOwner(tenantId, targetOwnerId, entityView.getId(), entityView, entityViewService::saveEntityView);
    }

    @Override
    public void changeEdgeOwner(TenantId tenantId, EntityId targetOwnerId, Edge edge) throws ThingsboardException {
        changeEntityOwner(tenantId, targetOwnerId, edge.getId(), edge, e -> edgeService.saveEdge(e));
    }

    @Override
    public void changeAssetOwner(TenantId tenantId, EntityId targetOwnerId, Asset asset) throws ThingsboardException {
        changeEntityOwner(tenantId, targetOwnerId, asset.getId(), asset, assetService::saveAsset);
    }

    @Override
    public void changeDeviceOwner(TenantId tenantId, EntityId targetOwnerId, Device device) throws ThingsboardException {
        changeEntityOwner(tenantId, targetOwnerId, device.getId(), device, d -> deviceService.saveDevice(d));
    }

    @Override
    public void changeAgentOwner(TenantId tenantId, EntityId targetOwnerId, Agent agent) throws ThingsboardException {
        changeEntityOwner(tenantId, targetOwnerId, agent.getId(), agent, agentService::saveAgent);
    }

    @Transactional
    @Override
    public void changeEntityOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId) throws ThingsboardException {
        switch (entityId.getEntityType()) {
            case DEVICE:
                changeDeviceOwner(tenantId, targetOwnerId, getDeviceById(tenantId, entityId));
                break;
            case ASSET:
                changeAssetOwner(tenantId, targetOwnerId, getAssetById(tenantId, entityId));
                break;
            case CUSTOMER:
                changeCustomerOwner(tenantId, targetOwnerId, getCustomerById(tenantId, entityId));
                break;
            case USER:
                changeUserOwner(tenantId, targetOwnerId, getUserById(tenantId, entityId));
                break;
            case DASHBOARD:
                changeDashboardOwner(tenantId, targetOwnerId, getDashboardById(tenantId, entityId));
                break;
            case ENTITY_VIEW:
                changeEntityViewOwner(tenantId, targetOwnerId, getEntityViewById(tenantId, entityId));
                break;
            case EDGE:
                changeEdgeOwner(tenantId, targetOwnerId, getEdgeById(tenantId, entityId));
                break;
            case AGENT:
                changeAgentOwner(tenantId, targetOwnerId, getAgentById(tenantId, entityId));
                break;
            default:
                throw new RuntimeException("EntityType does not support owner change: " + entityId.getEntityType());
        }
    }

    @Override
    public boolean isChildOwner(TenantId tenantId, CustomerId parentOwnerId, CustomerId childOwnerId) {
        return getChildOwners(tenantId, parentOwnerId).stream().anyMatch(childOwnerId::equals);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends BaseData<? extends UUIDBased>> PageData<E>
    getGroupEntities(TenantId tenantId, SecurityUser securityUser, EntityType entityType, Operation operation, PageLink pageLink,
                     Function<List<EntityGroupId>, PageData<E>> getEntitiesFunction) throws Exception {
        Resource resource = Resource.resourceFromEntityType(entityType);
        if (Authority.TENANT_ADMIN.equals(securityUser.getAuthority()) &&
            securityUser.getUserPermissions().hasGenericPermission(resource, operation)) {
            switch (entityType) {
                case DEVICE:
                    return (PageData<E>) deviceService.findDevicesByTenantId(tenantId, pageLink);
                case ASSET:
                    return (PageData<E>) assetService.findAssetsByTenantId(tenantId, pageLink);
                case CUSTOMER:
                    return (PageData<E>) customerService.findCustomersByTenantId(tenantId, pageLink);
                case USER:
                    return (PageData<E>) userService.findUsersByTenantId(tenantId, pageLink);
                case DASHBOARD:
                    return (PageData<E>) dashboardService.findDashboardsByTenantId(tenantId, pageLink);
                case ENTITY_VIEW:
                    return (PageData<E>) entityViewService.findEntityViewByTenantId(tenantId, pageLink);
                case EDGE:
                    return (PageData<E>) edgeService.findEdgesByTenantId(tenantId, pageLink);
                case AGENT:
                    return (PageData<E>) agentService.findAgentsByTenantId(tenantId, pageLink);
                default:
                    throw new RuntimeException("EntityType does not supported: " + entityType);
            }
        } else {
            List<EntityGroupId> groupIds = this.getAllowedEntityGroupIds(tenantId, securityUser, entityType, operation);
            if (!groupIds.isEmpty()) {
                return getEntitiesFunction.apply(groupIds);
            } else {
                return PageData.emptyPageData();
            }
        }
    }

    private List<EntityGroupId> getAllowedEntityGroupIds(TenantId tenantId,
                                                         SecurityUser securityUser,
                                                         EntityType entityType,
                                                         Operation operation) {
        MergedGroupTypePermissionInfo groupTypePermissionInfo = null;
        if (operation == Operation.READ) {
            groupTypePermissionInfo = securityUser.getUserPermissions().getReadGroupPermissions().get(entityType);
        }
        Resource resource = Resource.resourceFromEntityType(entityType);
        if (securityUser.getUserPermissions().hasGenericPermission(resource, operation) ||
            (groupTypePermissionInfo != null && !groupTypePermissionInfo.getEntityGroupIds().isEmpty())) {

            Set<EntityGroupId> groupIds = new HashSet<>();
            if (securityUser.getUserPermissions().hasGenericPermission(resource, operation)) {
                Set<EntityId> ownerIds = getChildOwners(tenantId, securityUser.getOwnerId());
                for (EntityId ownerId : ownerIds) {
                    Optional<EntityGroup> entityGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, ownerId,
                            entityType, EntityGroup.GROUP_ALL_NAME);
                    entityGroup.ifPresent(group -> groupIds.add(group.getId()));
                }
            }
            if (groupTypePermissionInfo != null && !groupTypePermissionInfo.getEntityGroupIds().isEmpty()) {
                groupIds.addAll(groupTypePermissionInfo.getEntityGroupIds());
            }
            return new ArrayList<>(groupIds);
        } else {
            return Collections.emptyList();
        }
    }

    private Device getDeviceById(TenantId tenantId, EntityId entityId) {
        return deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
    }

    private Asset getAssetById(TenantId tenantId, EntityId entityId) {
        return assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
    }

    private Customer getCustomerById(TenantId tenantId, EntityId entityId) {
        return customerService.findCustomerById(tenantId, new CustomerId(entityId.getId()));
    }

    private User getUserById(TenantId tenantId, EntityId entityId) {
        return userService.findUserById(tenantId, new UserId(entityId.getId()));
    }

    private Dashboard getDashboardById(TenantId tenantId, EntityId entityId) {
        return dashboardService.findDashboardById(tenantId, new DashboardId(entityId.getId()));
    }

    private EntityView getEntityViewById(TenantId tenantId, EntityId entityId) {
        return entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId()));
    }

    private Edge getEdgeById(TenantId tenantId, EntityId entityId) {
        return edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId()));
    }

    private Agent getAgentById(TenantId tenantId, EntityId entityId) {
        return agentService.findAgentById(tenantId, new AgentId(entityId.getId()));
    }

    private void fetchChildOwners(TenantId tenantId, EntityId entityId, Set<EntityId> result) throws Exception {
        result.add(entityId);
        Optional<EntityGroup> entityGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, entityId, EntityType.CUSTOMER, EntityGroup.GROUP_ALL_NAME);
        if (entityGroup.isPresent()) {
            List<EntityId> childOwnerIds = entityGroupService.findAllEntityIdsAsync(tenantId, entityGroup.get().getId(), new PageLink(Integer.MAX_VALUE)).get();
            for (EntityId ownerId : childOwnerIds) {
                fetchChildOwners(tenantId, ownerId, result);
            }
        }
    }

    @Override
    public void changeEntityOwner(TenantId tenantId, EntityId entityId, EntityId targetOwnerId, EntityId currentOwnerId) throws ThingsboardException {
        if (targetOwnerId.equals(currentOwnerId)) {
            throw new ThingsboardException("Entity already belongs to this owner!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        deleteFromGroupsAndAddToGroupAll(tenantId, entityId, targetOwnerId);

        clearOwners(entityId);
    }

    private <T extends HasOwnerId> void changeEntityOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId, T entity, Consumer<T> saveFunction)
            throws ThingsboardException {
        if (entity.getOwnerId().equals(targetOwnerId)) {
            throw new ThingsboardException("Entity already belongs to this owner!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        deleteFromGroupsAndAddToGroupAll(tenantId, entityId, targetOwnerId);

        EntityId previousOwnerId = entity.getOwnerId();
        entity.setOwnerId(targetOwnerId);
        saveFunction.accept(entity);
        clearOwners(entityId);

        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(entityId)
                .body(JacksonUtil.toString(previousOwnerId)).actionType(ActionType.CHANGE_OWNER).build());
    }

    private void deleteFromGroupsAndAddToGroupAll(TenantId tenantId, EntityId entityId, EntityId targetOwnerId) throws ThingsboardException {
        List<EntityGroupId> entityGroupList;
        try {
            entityGroupList = entityGroupService.findEntityGroupsForEntityAsync(tenantId, entityId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
        for (EntityGroupId entityGroupId : entityGroupList) {
            entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, entityId);
        }

        entityGroupService.addEntityToEntityGroupAll(tenantId, targetOwnerId, entityId);
    }

    @Override
    public Set<EntityId> getOwnedEntities(TenantId tenantId, EntityId ownerId, int fetchPackSize) {
        CustomerId customerId;
        switch (ownerId.getEntityType()) {
            case CUSTOMER -> customerId = (CustomerId) ownerId;
            case TENANT -> {
                if (!ownerId.equals(tenantId)) {
                    throw new IllegalArgumentException("Tenant-scoped getOwnedEntities called with mismatched tenantId [" + tenantId + "] and ownerId [" + ownerId + "]");
                }
                customerId = new CustomerId(CustomerId.NULL_UUID);
            }
            default -> {
                return Collections.emptySet();
            }
        }
        Set<EntityId> ownerEntities = new HashSet<>();
        PageDataIterable<DeviceId> deviceIds = new PageDataIterable<>(pageLink -> deviceService.findDeviceIdsByTenantIdAndCustomerId(tenantId, customerId, pageLink), fetchPackSize);
        deviceIds.forEach(ownerEntities::add);
        PageDataIterable<AssetId> assetIds = new PageDataIterable<>(pageLink -> assetService.findAssetIdsByTenantIdAndCustomerId(tenantId, customerId, pageLink), fetchPackSize);
        assetIds.forEach(ownerEntities::add);
        PageDataIterable<AgentId> agentIds = new PageDataIterable<>(pageLink -> agentService.findAgentIdsByTenantIdAndCustomerId(tenantId, customerId, pageLink), fetchPackSize);
        agentIds.forEach(ownerEntities::add);
        PageDataIterable<CustomerId> customerIds = new PageDataIterable<>(pageLink ->
                customerService.findCustomerIdsByTenantIdAndParentCustomerId(tenantId, customerId, pageLink), fetchPackSize);
        customerIds.forEach(ownerEntities::add);
        return ownerEntities;
    }

}
