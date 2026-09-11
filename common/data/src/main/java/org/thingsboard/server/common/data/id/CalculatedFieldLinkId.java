// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema
public class CalculatedFieldLinkId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public CalculatedFieldLinkId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static CalculatedFieldLinkId fromString(String calculatedFieldLinkId) {
        return new CalculatedFieldLinkId(UUID.fromString(calculatedFieldLinkId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "CALCULATED_FIELD_LINK", allowableValues = "CALCULATED_FIELD_LINK")
    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD_LINK;
    }

}
