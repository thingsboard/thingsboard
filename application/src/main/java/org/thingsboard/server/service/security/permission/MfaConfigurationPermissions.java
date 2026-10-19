// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.permission;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.service.security.model.SecurityUser;

@Component
public class MfaConfigurationPermissions extends AbstractPermissions {

    public MfaConfigurationPermissions() {
        super();
        put(Resource.PROFILE, profilePermissionChecker);
    }

    private static final PermissionChecker<UserId, User> profilePermissionChecker = new PermissionChecker<>() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            if (user.isSystemAdmin()) {
                return true;
            } else {
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            }
        }

    };

}
