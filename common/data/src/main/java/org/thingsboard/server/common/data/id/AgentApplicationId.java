// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serial;
import java.util.UUID;

@Schema(allOf = EntityId.class)
public class AgentApplicationId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AgentApplicationId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AgentApplicationId fromString(String agentApplicationId) {
        return new AgentApplicationId(UUID.fromString(agentApplicationId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "AGENT_APPLICATION", allowableValues = "AGENT_APPLICATION")
    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APPLICATION;
    }

    public static AgentApplicationId fromMsbAndLsb(long msb, long lsb) {
        return new AgentApplicationId(new UUID(msb, lsb));
    }
}
