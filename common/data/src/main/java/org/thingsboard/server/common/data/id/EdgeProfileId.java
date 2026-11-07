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
public class EdgeProfileId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonCreator
    public EdgeProfileId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static EdgeProfileId fromString(String id) {
        return new EdgeProfileId(UUID.fromString(id));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "EDGE_PROFILE", allowableValues = "EDGE_PROFILE")
    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE_PROFILE;
    }
}
