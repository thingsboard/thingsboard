// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.report.BaseReportTemplate;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CustomerUserPermissions extends AbstractPermissions {

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private OwnersCacheService ownersCacheService;

    @Autowired
    EntityServiceRegistry entityServiceRegistry;

    public CustomerUserPermissions() {
        super();
        put(Resource.PROFILE, genericPermissionChecker);
        put(Resource.ALARM, customerAlarmPermissionChecker);
        put(Resource.ASSET, customerGroupEntityPermissionChecker);
        put(Resource.DEVICE, customerGroupEntityPermissionChecker);
        put(Resource.CUSTOMER, customerGroupEntityPermissionChecker);
        put(Resource.DASHBOARD, customerGroupEntityPermissionChecker);
        put(Resource.ENTITY_VIEW, customerGroupEntityPermissionChecker);
        put(Resource.EDGE, customerGroupEntityPermissionChecker);
        put(Resource.ROLE, customerStandaloneEntityPermissionChecker);
        put(Resource.USER, customerGroupEntityPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, widgetsPermissionChecker);
        put(Resource.WIDGET_TYPE, widgetsPermissionChecker);
        put(Resource.SCHEDULER_EVENT, customerStandaloneEntityPermissionChecker);
        put(Resource.BLOB_ENTITY, customerStandaloneEntityPermissionChecker);
        put(Resource.CUSTOMER_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.DEVICE_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.ASSET_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.USER_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.ENTITY_VIEW_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.EDGE_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.DASHBOARD_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.WHITE_LABELING, customerWhiteLabelingPermissionChecker);
        put(Resource.GROUP_PERMISSION, customerGroupPermissionEntityChecker);
        put(Resource.AUDIT_LOG, genericPermissionChecker);
        put(Resource.DEVICE_PROFILE, profilePermissionChecker);
        put(Resource.ASSET_PROFILE, profilePermissionChecker);
        put(Resource.TB_RESOURCE, customerResourcePermissionChecker);
        put(Resource.OTA_PACKAGE, otaPackagePermissionChecker);
        put(Resource.MOBILE_APP_SETTINGS, qrCodeSettingsPermissionChecker);
        put(Resource.JOB, customerStandaloneEntityPermissionChecker);
        put(Resource.CUSTOM_MENU, customMenuPermissionChecker);
        put(Resource.OAUTH2_CLIENT, customerStandaloneEntityPermissionChecker);
        put(Resource.OAUTH2_CONFIGURATION_TEMPLATE, new PermissionChecker.GenericPermissionChecker(Operation.READ));
        put(Resource.DOMAIN, customerStandaloneEntityPermissionChecker);
        put(Resource.REPORT_TEMPLATE, reportTemplatePermissionChecker);
        put(Resource.REPORT, customerStandaloneEntityPermissionChecker);
        put(Resource.API_KEY, apiKeysPermissionChecker);
    }

    private final PermissionChecker<AlarmId, Alarm> customerAlarmPermissionChecker = new PermissionChecker<>() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, AlarmId alarmId, Alarm alarm) throws ThingsboardException {
            if (!user.getTenantId().equals(alarm.getTenantId())) {
                return false;
            }
            if (!(user.getUserPermissions().hasGenericPermission(Resource.ALARM, operation))) {
                return false;
            } else if (user.getCustomerId().equals(alarm.getCustomerId())) {
                return true;
            } else {
                EntityId originatorId = alarm.getOriginator();
                if (alarm.getOriginator() != null) {
                    Resource originatorResource = Resource.resourceFromEntityType(originatorId.getEntityType());
                    EntityDaoService entityDaoService = entityServiceRegistry.getServiceByEntityType(originatorId.getEntityType());
                    Optional<HasId<?>> entityOpt = entityDaoService.findEntity(user.getTenantId(), originatorId);
                    if (entityOpt.isPresent()) {
                        return CustomerUserPermissions.this.get(originatorResource).hasPermission(user, operation, originatorId, (TenantEntity) entityOpt.get());
                    }
                }
            }
            return false;
        }
    };

    private final PermissionChecker customerStandaloneEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }

            if (!(entity instanceof HasOwnerId)) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            if (entityId != null) {
                if (ownersCacheService.getOwners(user.getTenantId(), entityId, ((HasOwnerId) entity)).contains(user.getOwnerId())) {
                    // This entity does not have groups, so we are checking only generic level permissions
                    return user.getUserPermissions().hasGenericPermission(resource, operation);
                } else {
                    return false;
                }
            } else {
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            }
        }
    };

    private final PermissionChecker customerGroupPermissionEntityChecker = new PermissionChecker<GroupPermissionId, GroupPermission>() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, GroupPermissionId groupPermissionId, GroupPermission groupPermission) {
            if (!user.getTenantId().equals(groupPermission.getTenantId())) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(groupPermission.getEntityType());

            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }
    };

    private final PermissionChecker customerGroupEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) throws ThingsboardException {
            return hasPermission(user, operation, entityId, entity, null);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity, EntityGroupId entityGroupId) throws ThingsboardException {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            if (!(entity instanceof HasOwnerId)) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());

            if (entityGroupId != null) {
                if (!ownersCacheService.getOwners(user.getTenantId(), entityGroupId).contains(user.getOwnerId())) {
                    return false;
                }
            }

            if (entityId == null) {
                if (user.isCustomerUser() && user.getUserPrincipal().getType() != UserPrincipal.Type.PUBLIC_ID) {
                    EntityId ownerId = ((HasOwnerId) entity).getOwnerId();
                    var customerId = user.getCustomerId();

                    if (!customerId.equals(ownerId)) {
                        var owners = ownersCacheService.getOwners(entity.getTenantId(), ownerId, null);
                        if (!owners.contains(customerId)) {
                            return false;
                        }
                    }
                }
                if (user.getUserPermissions().hasGenericPermission(resource, operation)) {
                    return true;
                }
                if (!operation.isAllowedForGroupRole()) {
                    return false;
                }
                if (entityGroupId != null) {
                    if (user.getUserPermissions().hasGroupPermissions(entityGroupId, operation)) {
                        return true;
                    }
                }
            } else {
                if (operation == Operation.CLAIM_DEVICES) {
                    return user.getUserPermissions().hasGenericPermission(resource, operation);
                }
                if (entity.getEntityType() == EntityType.CUSTOMER && user.getCustomerId().equals(entityId) ||
                        ownersCacheService.getOwners(user.getTenantId(), entityId, ((HasOwnerId) entity)).contains(user.getOwnerId())) {
                    // This entity does have groups, so we are checking generic level permissions and then group specific permissions
                    if (user.getUserPermissions().hasGenericPermission(resource, operation)) {
                        return true;
                    }
                }
                if (!operation.isAllowedForGroupRole()) {
                    return false;
                }
                if (entityGroupId != null) {
                    if (user.getUserPermissions().hasGroupPermissions(entityGroupId, operation)) {
                        return true;
                    }
                }
                try {
                    List<EntityGroupId> entityGroupIds = entityGroupService.findEntityGroupsForEntityAsync(entity.getTenantId(), entityId).get();
                    for (EntityGroupId groupId : entityGroupIds) {
                        if (user.getUserPermissions().hasGroupPermissions(groupId, operation)) {
                            if (operation.isAllowedForGroupOwnerOnly()) {
                                if (ownersCacheService.getOwners(user.getTenantId(), groupId).contains(user.getOwnerId())) {
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
                }
            }
            return false;
        }
    };

    private static final PermissionChecker widgetsPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            if (!super.hasPermission(user, resource, operation)) {
                return false;
            }
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() != null && !entity.getTenantId().isNullUid() &&
                    !user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            // This entity does not have groups, so we are checking only generic level permissions
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }
    };

    private static final PermissionChecker customerResourcePermissionChecker = new PermissionChecker<TbResourceId, TbResourceInfo>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, TbResourceId resourceId, TbResourceInfo resource) {
            if (resource.getResourceType() == null || !resource.getResourceType().isCustomerAccess()) {
                return false;
            }
            if (operation == Operation.READ) {
                if (resource.getTenantId() == null || resource.getTenantId().isNullUid()) {
                    return true;
                }
                return user.getTenantId().equals(resource.getTenantId());
            } else {
                if (resource.getResourceType() == ResourceType.IMAGE) {
                    return user.getCustomerId().equals(resource.getCustomerId());
                } else {
                    return false;
                }
            }
        }

    };

    private final PermissionChecker customerEntityGroupPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) {

            Resource resource = Resource.groupResourceFromGroupType(entityGroup.getType());
            if (operation == Operation.CREATE) {
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            }
            boolean isOwner = ownersCacheService.getOwners(user.getTenantId(), entityGroup.getId(), entityGroup).contains(user.getOwnerId());
            if (isOwner) {
                // This entity is a group, so we are checking group generic permission first
                if (user.getUserPermissions().hasGenericPermission(resource, operation)) {
                    return true;
                }
            }
            if (!operation.isAllowedForGroupRole()) {
                return false;
            }
            if (operation.isGroupOperationAllowedForGroupOwnerOnly()) {
                return false;
            }
            //Just in case, we are also checking specific group permission
            return user.getUserPermissions().hasGroupPermissions(entityGroup.getId(), operation);
        }

        @Override
        public boolean hasEntityGroupInfoPermission(SecurityUser user, Operation operation, EntityGroupInfo entityGroup) {

            Resource resource = Resource.groupResourceFromGroupType(entityGroup.getType());
            if (operation == Operation.CREATE) {
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            }
            boolean isOwner = entityGroup.getOwnerIds().contains(user.getOwnerId());
            if (isOwner) {
                // This entity is a group, so we are checking group generic permission first
                if (user.getUserPermissions().hasGenericPermission(resource, operation)) {
                    return true;
                }
            }
            if (!operation.isAllowedForGroupRole()) {
                return false;
            }
            if (operation.isGroupOperationAllowedForGroupOwnerOnly()) {
                return false;
            }
            //Just in case, we are also checking specific group permission
            return user.getUserPermissions().hasGroupPermissions(entityGroup.getId(), operation);
        }

    };

    private final PermissionChecker customerWhiteLabelingPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            if (!whiteLabelingService.isWhiteLabelingAllowed(user.getTenantId(), user.getCustomerId())) {
                return false;
            } else {
                return user.getUserPermissions().hasGenericPermission(Resource.WHITE_LABELING, operation);
            }
        }

    };

    private static final PermissionChecker profilePermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            if (!super.hasPermission(user, resource, operation)) {
                return false;
            }
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() != null && !entity.getTenantId().isNullUid() &&
                    !user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            // This entity does not have groups, so we are checking only generic level permissions
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }
    };

    private final PermissionChecker<ReportTemplateId, BaseReportTemplate> reportTemplatePermissionChecker = new PermissionChecker<>() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, ReportTemplateId entityId, BaseReportTemplate reportTemplate) throws ThingsboardException {
            if (!user.getTenantId().equals(reportTemplate.getTenantId())) {
                return false;
            }
            if (customerStandaloneEntityPermissionChecker.hasPermission(user, operation, entityId, reportTemplate)) {
                return true;
            }
            // Report-template-specific: a customer may also READ templates owned above them (tenant / parent customer).
            if (operation == Operation.READ
                    && ownersCacheService.getOwners(user.getTenantId(), user.getId(), user).contains(reportTemplate.getOwnerId())) {
                return user.getUserPermissions().hasGenericPermission(Resource.resourceFromEntityType(reportTemplate.getEntityType()), operation);
            }
            return false;
        }
    };

    private static final PermissionChecker otaPackagePermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            if (!super.hasPermission(user, resource, operation)) {
                return false;
            }
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() != null && !entity.getTenantId().isNullUid() &&
                    !user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            // This entity does not have groups, so we are checking only generic level permissions
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }
    };

    private static final PermissionChecker qrCodeSettingsPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return operation == Operation.READ;
        }
    };

    private final PermissionChecker customMenuPermissionChecker = new PermissionChecker() {
        @Override
        public boolean hasCustomMenuPermission(SecurityUser user, Operation operation, CustomMenuInfo customMenu) {
            if (!whiteLabelingService.isWhiteLabelingAllowed(user.getTenantId(), user.getCustomerId()) ||
                    !user.getUserPermissions().hasGenericPermission(Resource.WHITE_LABELING, operation)) {
                return false;
            }
            if (operation == Operation.READ) {
                return user.getTenantId().equals(customMenu.getTenantId()) && customMenu.getCustomerId() != null &&
                        (user.getCustomerId().equals(customMenu.getCustomerId()) ||
                                ownersCacheService.getOwners(customMenu.getTenantId(), customMenu.getCustomerId(), null)
                                        .contains(user.getCustomerId()));
            } else {
                return user.getTenantId().equals(customMenu.getTenantId()) && user.getCustomerId().equals(customMenu.getCustomerId());
            }
        }
    };

    private static final PermissionChecker apiKeysPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            // This entity does not have groups, so we are checking only generic level permissions
            return user.getUserPermissions().hasGenericPermission(Resource.API_KEY, operation);
        }
    };

}
