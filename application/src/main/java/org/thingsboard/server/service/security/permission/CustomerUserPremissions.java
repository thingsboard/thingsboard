package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CustomerUserPremissions extends HashMap<Resource, PermissionChecker> {

    public CustomerUserPremissions() {
        super();
        put(Resource.ALARM, TenantAdminPermissions.tenantEntityPermissionChecker);
        put(Resource.ASSET, customerEntityPermissionChecker);
        put(Resource.DEVICE, customerEntityPermissionChecker);
        put(Resource.CUSTOMER, customerPermissionChecker);
        put(Resource.DASHBOARD, customerDashboardPermissionChecker);
        put(Resource.ENTITY_VIEW, customerEntityPermissionChecker);
        put(Resource.TENANT, TenantAdminPermissions.tenantPermissionChecker);
    }

    public static final PermissionChecker customerEntityPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {

            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            if (!(entity instanceof HasCustomerId)) {
                return false;
            }
            if (!user.getCustomerId().equals(((HasCustomerId)entity).getCustomerId())) {
                return false;
            }
            return true;
        }
    };

    private static final PermissionChecker customerPermissionChecker =
            new PermissionChecker.GenericPermissionChecker(Operation.READ) {

                @Override
                public boolean hasPermission(SecurityUser user, TenantId tenantId, Operation operation, EntityId entityId) {
                    if (!super.hasPermission(user, tenantId, operation, entityId)) {
                        return false;
                    }
                    if (!user.getCustomerId().equals(entityId)) {
                        return false;
                    }
                    return true;
                }

            };

    private static final PermissionChecker customerDashboardPermissionChecker =
            new PermissionChecker.GenericPermissionChecker<DashboardInfo, DashboardId>(Operation.READ) {

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation, DashboardId dashboardId, DashboardInfo dashboard) {

                    if (!super.hasPermission(user, operation, dashboardId, dashboard)) {
                        return false;
                    }
                    if (!user.getTenantId().equals(dashboard.getTenantId())) {
                        return false;
                    }
                    if (!dashboard.isAssignedToCustomer(user.getCustomerId())) {
                        return false;
                    }
                    return true;
                }

            };
}
