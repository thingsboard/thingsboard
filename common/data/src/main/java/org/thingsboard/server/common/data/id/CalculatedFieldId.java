// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serial;
import java.util.UUID;

@Schema
public class CalculatedFieldId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonCreator
    public CalculatedFieldId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static CalculatedFieldId fromString(String calculatedFieldId) {
        return new CalculatedFieldId(UUID.fromString(calculatedFieldId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "CALCULATED_FIELD", allowableValues = "CALCULATED_FIELD")
    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD;
    }

}
