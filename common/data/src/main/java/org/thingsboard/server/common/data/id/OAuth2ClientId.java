// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class OAuth2ClientId extends UUIDBased implements EntityId {

    @JsonCreator
    public OAuth2ClientId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static OAuth2ClientId fromString(String oauth2ClientId) {
        return new OAuth2ClientId(UUID.fromString(oauth2ClientId));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OAUTH2_CLIENT;
    }
}
