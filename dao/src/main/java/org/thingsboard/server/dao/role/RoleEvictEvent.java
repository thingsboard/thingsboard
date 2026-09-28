// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.role.Role;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class RoleEvictEvent {

    private final RoleId roleId;
    private Role savedRole;

}
