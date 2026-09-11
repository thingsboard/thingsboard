// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serial;
import java.util.UUID;

public final class AiModelId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 3021036138554389754L;

    @JsonCreator
    public AiModelId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @Override
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Entity type of the AI model",
            example = "AI_MODEL",
            allowableValues = "AI_MODEL"
    )
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

    public static AiModelId fromString(String uuid) {
        return new AiModelId(UUID.fromString(uuid));
    }

}
