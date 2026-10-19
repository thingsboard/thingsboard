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
public class AgentBulkActionId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AgentBulkActionId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AgentBulkActionId fromString(String id) {
        return new AgentBulkActionId(UUID.fromString(id));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "AGENT_BULK_ACTION", allowableValues = "AGENT_BULK_ACTION")
    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_BULK_ACTION;
    }

    public static AgentBulkActionId fromMsbAndLsb(long msb, long lsb) {
        return new AgentBulkActionId(new UUID(msb, lsb));
    }
}
