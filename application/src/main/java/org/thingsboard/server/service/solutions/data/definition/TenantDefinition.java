// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.Collections;
import java.util.List;

@Data
public class TenantDefinition extends BaseEntityDefinition {

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

    private List<UserGroupDefinition> userGroups = Collections.emptyList();

}
