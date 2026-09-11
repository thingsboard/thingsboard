// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serial;
import java.util.UUID;

@Schema(allOf = EntityId.class)
public class ApiKeyId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = -273913539653684641L;

    @JsonCreator
    public ApiKeyId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static ApiKeyId fromString(String secretId) {
        return new ApiKeyId(UUID.fromString(secretId));
    }

    @Override
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "API_KEY", allowableValues = "API_KEY")
    public EntityType getEntityType() {
        return EntityType.API_KEY;
    }

}
