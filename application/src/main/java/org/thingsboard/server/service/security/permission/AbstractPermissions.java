// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.permission;

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
}
