// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class UserGroupDefinition {

    private String name;
    private String jsonId;
    private List<String> genericRoles = Collections.emptyList();
    private List<GroupRoleDefinition> groupRoles = Collections.emptyList();

    public void setGroupRoles(List<GroupRoleDefinition> groupRoles) {
        if (groupRoles != null) {
            this.groupRoles = groupRoles;
        }
    }

    public void setGenericRoles(List<String> genericRoles) {
        if (genericRoles != null) {
            this.genericRoles = genericRoles;
        }
    }
}
