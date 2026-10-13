// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

@Data
@NoArgsConstructor
public class GroupRoleDefinition {

    private String roleName;
    private EntityType groupType;
    private String groupName;

}
