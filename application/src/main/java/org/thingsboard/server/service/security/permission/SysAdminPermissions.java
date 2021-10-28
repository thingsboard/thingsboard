/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.security.permission;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.security.model.SecurityUser;

@Component(value="sysAdminPermissions")
public class SysAdminPermissions extends AbstractPermissions {

    public SysAdminPermissions() {
        super();
        put(Resource.ADMIN_SETTINGS, PermissionChecker.allowAllPermissionChecker);
        put(Resource.DASHBOARD, PermissionChecker.allowReadPermissionChecker);
        put(Resource.TENANT, PermissionChecker.allowAllPermissionChecker);
        put(Resource.RULE_CHAIN, PermissionChecker.allowReadPermissionChecker);
        put(Resource.USER, PermissionChecker.allowAllPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, systemEntityPermissionChecker);
        put(Resource.WIDGET_TYPE, systemEntityPermissionChecker);
        put(Resource.OAUTH2_CONFIGURATION_INFO, PermissionChecker.allowAllPermissionChecker);
        put(Resource.OAUTH2_CONFIGURATION_TEMPLATE, PermissionChecker.allowAllPermissionChecker);
        put(Resource.TENANT_PROFILE, PermissionChecker.allowAllPermissionChecker);
        put(Resource.TB_RESOURCE, PermissionChecker.allowAllPermissionChecker);
        put(Resource.CUSTOMER, PermissionChecker.allowReadPermissionChecker);
        put(Resource.ASSET, PermissionChecker.allowReadPermissionChecker);
        put(Resource.DEVICE, PermissionChecker.allowReadPermissionChecker);
        put(Resource.ENTITY_VIEW, PermissionChecker.allowReadPermissionChecker);
        put(Resource.DEVICE_PROFILE, PermissionChecker.allowReadPermissionChecker);
        put(Resource.OTA_PACKAGE, PermissionChecker.allowReadPermissionChecker);
        put(Resource.EDGE, PermissionChecker.allowReadPermissionChecker);
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

}
