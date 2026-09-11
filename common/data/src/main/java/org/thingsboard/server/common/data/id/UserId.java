// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema
public class UserId extends UUIDBased implements EntityId {

    @JsonCreator
    public UserId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static UserId fromString(String userId) {
        return new UserId(UUID.fromString(userId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "USER", allowableValues = "USER")
    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
