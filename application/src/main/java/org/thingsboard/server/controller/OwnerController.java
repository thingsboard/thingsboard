// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasName;
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
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class OwnerController extends AutoCommitController {

    public static final String OWNER_ID = "ownerId";
    public static final String ENTITY_TYPE = "entityType";
    public static final String ENTITY_ID = "entityId";

    @Autowired
    private OwnersCacheService ownersCacheService;

    @ApiOperation(value = "Change owner to tenant (changeOwnerToTenant)",
            notes = "Tenant changes Owner from Customer or sub-Customer to Tenant. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/owner/TENANT/{ownerId}/{entityType}/{entityId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void changeOwnerToTenant(@Parameter(description = TENANT_ID_PARAM_DESCRIPTION)
                                    @PathVariable(OWNER_ID) String ownerIdStr,
                                    @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION)
                                    @PathVariable(ENTITY_TYPE) String entityType,
                                    @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION)
                                    @PathVariable(ENTITY_ID) String entityIdStr,
                                    @Parameter(description = "An optional list of additional entity group ids")
                                    @RequestBody(required = false) String[] strEntityGroupIds) throws ThingsboardException {
        checkParameter(OWNER_ID, ownerIdStr);
        checkParameter(ENTITY_TYPE, entityType);
        checkParameter(ENTITY_ID, entityIdStr);
        TenantId targetOwnerId = TenantId.fromUUID(UUID.fromString(ownerIdStr));
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        if (!getCurrentUser().getTenantId().equals(targetOwnerId)) {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        checkEntityId(entityId, Operation.CHANGE_OWNER);
        List<EntityGroup> entityGroups = this.validateEntityGroupIds(strEntityGroupIds, entityId, targetOwnerId);
        changeOwner(getCurrentUser().getTenantId(), targetOwnerId, entityId);
        if (entityGroups != null) {
            this.addEntityToEntityGroups(getCurrentUser().getTenantId(), entityId, entityGroups);
        }
    }

    @ApiOperation(value = "Change owner to customer (changeOwnerToCustomer)",
            notes = "Tenant/Customer changes Owner to Customer or sub-Customer. " +
                    "Sub-Customer can`t perform this operation! " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/owner/CUSTOMER/{ownerId}/{entityType}/{entityId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void changeOwnerToCustomer(@Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
                                      @PathVariable(OWNER_ID) String ownerIdStr,
                                      @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION)
                                      @PathVariable(ENTITY_TYPE) String entityType,
                                      @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION)
                                      @PathVariable(ENTITY_ID) String entityIdStr,
                                      @Parameter(description = "An optional list of additional entity group ids")
                                      @RequestBody(required = false) String[] strEntityGroupIds) throws Exception {
        checkParameter(OWNER_ID, ownerIdStr);
        checkParameter(ENTITY_TYPE, entityType);
        checkParameter(ENTITY_ID, entityIdStr);
        SecurityUser currentUser = getCurrentUser();
        EntityId currentUserOwnerId = currentUser.getOwnerId();
        CustomerId targetOwnerId = new CustomerId(UUID.fromString(ownerIdStr));
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);

        if (!currentUserOwnerId.equals(targetOwnerId)) {
            Customer targetOwner = customerService.findCustomerById(currentUser.getTenantId(), targetOwnerId);
            Set<EntityId> targetOwnerOwners = ownersCacheService.getOwners(currentUser.getTenantId(), targetOwnerId, targetOwner);
            if (!targetOwnerOwners.contains(currentUserOwnerId)) {
                // Customer/Tenant Changes Owner from Customer to Sub-Customer - OK.
                // Customer/Tenant Changes Owner from Sub-Customer to Customer - OK.
                // Sub-Customer Changes Owner from Sub-Customer to Customer - NOT OK.
                throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
        List<EntityGroup> entityGroups = this.validateEntityGroupIds(strEntityGroupIds, entityId, targetOwnerId);
        changeOwner(currentUser.getTenantId(), targetOwnerId, entityId);
        if (entityGroups != null) {
            this.addEntityToEntityGroups(currentUser.getTenantId(), entityId, entityGroups);
        }
    }

    private EntityId changeOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId) throws ThingsboardException {
        try {
            EntityId previousOwnerId;
            switch (entityId.getEntityType()) {
                case DEVICE:
                    Device device = checkDeviceId(new DeviceId(entityId.getId()), Operation.CHANGE_OWNER);
                    previousOwnerId = device.getOwnerId();
                    ownersCacheService.changeDeviceOwner(tenantId, targetOwnerId, device);
                    logChangeOwnerAction(device.getId(), device, targetOwnerId);
                    break;
                case ASSET:
                    Asset asset = checkAssetId(new AssetId(entityId.getId()), Operation.CHANGE_OWNER);
                    previousOwnerId = asset.getOwnerId();
                    ownersCacheService.changeAssetOwner(tenantId, targetOwnerId, asset);
                    logChangeOwnerAction(asset.getId(), asset, targetOwnerId);
                    break;
                case ENTITY_VIEW:
                    EntityView entityView = checkEntityViewId(new EntityViewId(entityId.getId()), Operation.CHANGE_OWNER);
                    previousOwnerId = entityView.getOwnerId();
                    ownersCacheService.changeEntityViewOwner(tenantId, targetOwnerId, entityView);
                    logChangeOwnerAction(entityView.getId(), entityView, targetOwnerId);
                    break;
                case EDGE:
                    Edge edge = checkEdgeId(new EdgeId(entityId.getId()), Operation.CHANGE_OWNER);
                    previousOwnerId = edge.getOwnerId();
                    ownersCacheService.changeEdgeOwner(tenantId, targetOwnerId, edge);
                    logChangeOwnerAction(edge.getId(), edge, targetOwnerId);
                    break;
                case CUSTOMER:
                    Customer customer = checkCustomerId(new CustomerId(entityId.getId()), Operation.CHANGE_OWNER);
                    previousOwnerId = customer.getOwnerId();
                    ownersCacheService.changeCustomerOwner(tenantId, targetOwnerId, customer);
                    logChangeOwnerAction(customer.getId(), customer, targetOwnerId);
                    break;
                case USER:
                    User user = checkUserId(new UserId(entityId.getId()), Operation.CHANGE_OWNER);
                    previousOwnerId = user.getOwnerId();
                    ownersCacheService.changeUserOwner(tenantId, targetOwnerId, user);
                    logChangeOwnerAction(user.getId(), user, targetOwnerId);
                    break;
                case DASHBOARD:
                    Dashboard dashboard = checkDashboardId(new DashboardId(entityId.getId()), Operation.CHANGE_OWNER);
                    previousOwnerId = dashboard.getOwnerId();
                    ownersCacheService.changeDashboardOwner(tenantId, targetOwnerId, dashboard);
                    logChangeOwnerAction(dashboard.getId(), dashboard, targetOwnerId);
                    break;
                case AGENT:
                    Agent agent = checkAgentId(new AgentId(entityId.getId()), Operation.CHANGE_OWNER);
                    previousOwnerId = agent.getOwnerId();
                    ownersCacheService.changeAgentOwner(tenantId, targetOwnerId, agent);
                    logChangeOwnerAction(agent.getId(), agent, targetOwnerId);
                    break;
                default:
                    throw new ThingsboardException("EntityType does not support owner change: " + entityId.getEntityType(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            return previousOwnerId;
        } catch (ThingsboardException e) {
            logEntityActionService.logEntityAction(tenantId, entityId, ActionType.CHANGE_OWNER, getCurrentUser(), e);
            throw e;
        }
    }

    private List<EntityGroup> validateEntityGroupIds(String[] strEntityGroupIds, EntityId entityId, EntityId targetOwnerId) throws ThingsboardException {
        List<EntityGroup> entityGroups = null;
        if (strEntityGroupIds != null && strEntityGroupIds.length > 0) {
            checkArrayParameter("strEntityGroupIds", strEntityGroupIds);
            entityGroups = new ArrayList<>();
            for (String strEntityGroupId : strEntityGroupIds) {
                EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
                EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.ADD_TO_GROUP);
                if (!entityGroup.getOwnerId().equals(targetOwnerId)) {
                    throw new ThingsboardException("Unable to add entity to entity group with different owner!",
                            ThingsboardErrorCode.PERMISSION_DENIED);
                }
                if (entityGroup.getType() != entityId.getEntityType()) {
                    throw new ThingsboardException("Unable to add entity to entity group with different type!",
                            ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
                if (entityGroup.isGroupAll()) {
                    throw new ThingsboardException("Unable to add entity to entity group: " +
                            "Addition to entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
                }
                entityGroups.add(entityGroup);
            }
        }
        return entityGroups;
    }

    private void addEntityToEntityGroups(TenantId tenantId, EntityId entityId, List<EntityGroup> entityGroups) throws ThingsboardException {
        for (EntityGroup entityGroup : entityGroups) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroup.getId(), entityId);
            if (entityGroup.getType() == EntityType.USER) {
                userPermissionsService.onUserUpdatedOrRemoved(userService.findUserById(getTenantId(), new UserId(entityId.getId())));
            } else if (entityGroup.getType() == EntityType.DEVICE) {
                DeviceGroupOtaPackage fw =
                        deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(entityGroup.getId(), OtaPackageType.FIRMWARE);
                DeviceGroupOtaPackage sw =
                        deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(entityGroup.getId(), OtaPackageType.SOFTWARE);
                if (fw != null || sw != null) {
                    List<DeviceId> deviceIds = Collections.singletonList(new DeviceId(entityId.getId()));
                    otaPackageStateService.update(getTenantId(), deviceIds, fw != null, sw != null);
                }
            }
            logEntityActionService.logEntityAction(getTenantId(), entityId, null,
                    ActionType.ADDED_TO_ENTITY_GROUP, getCurrentUser(), entityId.toString(), entityGroup.getUuidId().toString(), entityGroup.getName());
        }
    }

    private <E extends HasName, I extends EntityId> void logChangeOwnerAction(I entityId, E entity, EntityId targetOwnerId) throws ThingsboardException {
        logEntityActionService.logEntityAction(getTenantId(), entityId, entity, ActionType.CHANGE_OWNER, getCurrentUser(), targetOwnerId);
    }

}
