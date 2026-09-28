// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.permission.Resource;

import java.util.Optional;

public interface Permissions {

    Optional<PermissionChecker> getPermissionChecker(Resource resource);

}
