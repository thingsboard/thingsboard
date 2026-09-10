// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class DomainId extends UUIDBased implements EntityId {

    @JsonCreator
    public DomainId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static DomainId fromString(String oauth2DomainId) {
        return new DomainId(UUID.fromString(oauth2DomainId));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DOMAIN;
    }
}
