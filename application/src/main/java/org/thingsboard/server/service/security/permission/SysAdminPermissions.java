// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.permission;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

@Component
public class SysAdminPermissions extends AbstractPermissions {

    public SysAdminPermissions() {
        super();
        put(Resource.PROFILE, genericPermissionChecker);
        put(Resource.ADMIN_SETTINGS, genericPermissionChecker);
        put(Resource.DASHBOARD, systemGenericReadPermissionChecker);
        put(Resource.ALARM, systemGenericReadPermissionChecker);
        put(Resource.TENANT, genericPermissionChecker);
        put(Resource.RULE_CHAIN, systemEntityPermissionChecker);
        put(Resource.USER, userPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, systemEntityPermissionChecker);
        put(Resource.WIDGET_TYPE, systemEntityPermissionChecker);
        put(Resource.WHITE_LABELING, genericPermissionChecker);
        put(Resource.OAUTH2_CLIENT, systemEntityPermissionChecker);
        put(Resource.MOBILE_APP, systemEntityPermissionChecker);
        put(Resource.MOBILE_APP_BUNDLE, systemEntityPermissionChecker);
        put(Resource.DOMAIN, systemEntityPermissionChecker);
        put(Resource.OAUTH2_CONFIGURATION_TEMPLATE, genericPermissionChecker);
        put(Resource.TENANT_PROFILE, genericPermissionChecker);
        put(Resource.TB_RESOURCE, systemEntityPermissionChecker);
        put(Resource.QUEUE, systemEntityPermissionChecker);
        put(Resource.NOTIFICATION, systemEntityPermissionChecker);
        put(Resource.MOBILE_APP_SETTINGS, genericPermissionChecker);
        put(Resource.CUSTOM_MENU, customMenuPermissionChecker);
        put(Resource.SECRET, systemEntityPermissionChecker);
        put(Resource.API_KEY, genericPermissionChecker);
        put(Resource.AUDIT_LOG, systemGenericReadPermissionChecker);
    }

    private static final PermissionChecker systemEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {

            if (entity.getTenantId() != null && !entity.getTenantId().isNullUid()) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }
    };

    private static final PermissionChecker systemGenericReadPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

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
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }
    };

    private static final PermissionChecker userPermissionChecker = new PermissionChecker<UserId, User>() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(Resource.USER, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, User userEntity) {
            return hasPermission(user, operation, userId, userEntity, null);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, User userEntity, EntityGroupId entityGroupId) {
            if (Authority.CUSTOMER_USER.equals(userEntity.getAuthority())) {
                return false;
            }
            return user.getUserPermissions().hasGenericPermission(Resource.USER, operation);
        }

    };

    private static final PermissionChecker customMenuPermissionChecker = new PermissionChecker() {
        @Override
        public boolean hasCustomMenuPermission(SecurityUser user, Operation operation, CustomMenuInfo customMenu) {
            return customMenu.getTenantId().isSysTenantId() && user.getUserPermissions().hasGenericPermission(Resource.CUSTOM_MENU, operation);
        }
    };

}
