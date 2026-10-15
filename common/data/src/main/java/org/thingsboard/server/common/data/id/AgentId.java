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
public class AgentId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AgentId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AgentId fromString(String agentId) {
        return new AgentId(UUID.fromString(agentId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "AGENT", allowableValues = "AGENT")
    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT;
    }

    public static AgentId fromMsbAndLsb(long msb, long lsb) {
        return new AgentId(new UUID(msb, lsb));
    }
}
