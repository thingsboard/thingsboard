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
public class AgentAppProfileId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AgentAppProfileId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AgentAppProfileId fromString(String id) {
        return new AgentAppProfileId(UUID.fromString(id));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "AGENT_APP_PROFILE", allowableValues = "AGENT_APP_PROFILE")
    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_PROFILE;
    }
}
