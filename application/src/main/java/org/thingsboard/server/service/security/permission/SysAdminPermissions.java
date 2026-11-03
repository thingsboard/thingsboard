// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.permission;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

@Component(value = "sysAdminPermissions")
public class SysAdminPermissions extends AbstractPermissions {

    public SysAdminPermissions() {
        super();
        put(Resource.ADMIN_SETTINGS, PermissionChecker.allowAllPermissionChecker);
        put(Resource.DASHBOARD, new PermissionChecker.GenericPermissionChecker(Operation.READ));
        put(Resource.TENANT, PermissionChecker.allowAllPermissionChecker);
        put(Resource.RULE_CHAIN, systemEntityPermissionChecker);
        put(Resource.USER, userPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, systemEntityPermissionChecker);
        put(Resource.WIDGET_TYPE, systemEntityPermissionChecker);
        put(Resource.OAUTH2_CLIENT, systemEntityPermissionChecker);
        put(Resource.MOBILE_APP, systemEntityPermissionChecker);
        put(Resource.MOBILE_APP_BUNDLE, systemEntityPermissionChecker);
        put(Resource.DOMAIN, PermissionChecker.allowAllPermissionChecker);
        put(Resource.OAUTH2_CONFIGURATION_TEMPLATE, PermissionChecker.allowAllPermissionChecker);
        put(Resource.TENANT_PROFILE, PermissionChecker.allowAllPermissionChecker);
        put(Resource.TB_RESOURCE, systemEntityPermissionChecker);
        put(Resource.QUEUE, systemEntityPermissionChecker);
        put(Resource.NOTIFICATION, systemEntityPermissionChecker);
        put(Resource.MOBILE_APP_SETTINGS, PermissionChecker.allowAllPermissionChecker);
    }

    private static final PermissionChecker systemEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {

            if (entity.getTenantId() != null && !entity.getTenantId().isNullUid()) {
                return false;
            }
            return true;
        }
    };

    private static final PermissionChecker userPermissionChecker = new PermissionChecker<UserId, User>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, User userEntity) {
            if (Authority.CUSTOMER_USER.equals(userEntity.getAuthority())) {
                return false;
            }
            return true;
        }

    };

}
