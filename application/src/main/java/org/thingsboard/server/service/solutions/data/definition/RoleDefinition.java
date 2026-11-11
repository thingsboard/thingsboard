// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.role.RoleType;

@Data
public class RoleDefinition implements EntityDefinition {

    private String name;
    private RoleType type;
    private JsonNode operations;

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }
}
