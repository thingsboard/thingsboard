// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serial;
import java.util.UUID;

public class JobId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = -2225072123132918395L;

    @JsonCreator
    public JobId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "JOB", allowableValues = "JOB")
    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

}
