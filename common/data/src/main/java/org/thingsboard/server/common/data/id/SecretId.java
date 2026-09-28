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
public class SecretId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = -1970910872850578069L;

    @JsonCreator
    public SecretId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static SecretId fromString(String secretId) {
        return new SecretId(UUID.fromString(secretId));
    }

    @Override
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "SECRET", allowableValues = "SECRET")
    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

}
