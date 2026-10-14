// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDefinition extends BaseEntityDefinition {

    private String firstname;
    private String lastname;
    private String password;
    private String group;
    private DashboardUserDetailsDefinition dashboard;

    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
