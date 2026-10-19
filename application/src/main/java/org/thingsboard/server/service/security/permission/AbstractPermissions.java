// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashMap;
import java.util.Optional;

public abstract class AbstractPermissions extends HashMap<Resource, PermissionChecker> implements Permissions {

    public AbstractPermissions() {
        super();
    }

    @Override
    public Optional<PermissionChecker> getPermissionChecker(Resource resource) {
        PermissionChecker permissionChecker = this.get(resource);
        return Optional.ofNullable(permissionChecker);
    }

    public static final PermissionChecker genericPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

    };

}
