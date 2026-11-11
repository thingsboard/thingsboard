// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.role.Role;


public interface UserPermissionsService {

    MergedUserPermissions getMergedPermissions(User user, boolean isPublic) throws ThingsboardException;

    void onRoleUpdated(Role role) throws ThingsboardException;

    void onGroupPermissionUpdated(GroupPermission groupPermission) throws ThingsboardException;

    void onGroupPermissionDeleted(GroupPermission groupPermission) throws ThingsboardException;

    void onUserUpdatedOrRemoved(User user) throws ThingsboardException;

}
