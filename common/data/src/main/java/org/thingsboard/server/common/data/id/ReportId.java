// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema(allOf = EntityId.class)
public class ReportId extends UUIDBased implements EntityId {

    @JsonCreator
    public ReportId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static ReportId fromString(String reportId) {
        return new ReportId(UUID.fromString(reportId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "REPORT", allowableValues = "REPORT")
    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT;
    }

}
