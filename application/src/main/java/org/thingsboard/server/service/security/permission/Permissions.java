// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.permission;

import java.util.Optional;

public interface Permissions {

    Optional<PermissionChecker> getPermissionChecker(Resource resource);

}
