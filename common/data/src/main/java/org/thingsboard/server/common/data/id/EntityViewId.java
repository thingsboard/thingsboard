// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
public class EntityViewId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public EntityViewId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static EntityViewId fromString(String entityViewID) {
        return new EntityViewId(UUID.fromString(entityViewID));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "ENTITY_VIEW", allowableValues = "ENTITY_VIEW")
    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }
}
