// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema(allOf = EntityId.class)
public class OAuth2ClientId extends UUIDBased implements EntityId {

    @JsonCreator
    public OAuth2ClientId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static OAuth2ClientId fromString(String oauth2ClientId) {
        return new OAuth2ClientId(UUID.fromString(oauth2ClientId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "OAUTH2_CLIENT", allowableValues = "OAUTH2_CLIENT")
    @Override
    public EntityType getEntityType() {
        return EntityType.OAUTH2_CLIENT;
    }
}
