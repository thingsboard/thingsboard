// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserDefinition extends BaseEntityDefinition {

    private String firstname;
    private String lastname;
    private String password;
    private DashboardUserDetailsDefinition dashboard;

    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
