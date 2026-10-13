// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerEventDefinition extends CustomerEntityDefinition {

    private EntityId originatorId;
    private String type;
    private JsonNode schedule;
    private JsonNode configuration;

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
