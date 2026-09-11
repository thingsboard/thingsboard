// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema(allOf = EntityId.class)
public class DomainId extends UUIDBased implements EntityId {

    @JsonCreator
    public DomainId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static DomainId fromString(String oauth2DomainId) {
        return new DomainId(UUID.fromString(oauth2DomainId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "DOMAIN", allowableValues = "DOMAIN")
    @Override
    public EntityType getEntityType() {
        return EntityType.DOMAIN;
    }
}
