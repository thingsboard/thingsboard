// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema
public class ApiUsageStateId extends UUIDBased implements EntityId {

    @JsonCreator
    public ApiUsageStateId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static ApiUsageStateId fromString(String userId) {
        return new ApiUsageStateId(UUID.fromString(userId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "API_USAGE_STATE", allowableValues = "API_USAGE_STATE")
    @Override
    public EntityType getEntityType() {
        return EntityType.API_USAGE_STATE;
    }

}
