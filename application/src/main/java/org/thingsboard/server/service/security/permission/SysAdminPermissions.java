package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashMap;

public class SysAdminPermissions extends HashMap<Resource, PermissionChecker> {

    public SysAdminPermissions() {
        super();
        put(Resource.ADMIN_SETTINGS, PermissionChecker.allowAllPermissionChecker);
        put(Resource.DASHBOARD, new PermissionChecker.GenericPermissionChecker(Operation.READ));
        put(Resource.TENANT, PermissionChecker.allowAllPermissionChecker);
        put(Resource.RULE_CHAIN, systemEntityPermissionChecker);
    }

    private static final PermissionChecker systemEntityPermissionChecker = new PermissionChecker<HasTenantId, EntityId>() {

        @Override
        public boolean hasPermission(SecurityUser user, TenantId tenantId, Operation operation, EntityId entityId) {
            if (tenantId != null && !tenantId.isNullUid()) {
                return false;
            }
            return true;
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {

            if (entity.getTenantId() != null && !entity.getTenantId().isNullUid()) {
                return false;
            }
            return true;
        }
    };
}
