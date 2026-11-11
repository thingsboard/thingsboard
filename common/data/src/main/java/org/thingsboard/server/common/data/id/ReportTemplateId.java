// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema(allOf = EntityId.class)
public class ReportTemplateId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public ReportTemplateId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static ReportTemplateId fromString(String reportId) {
        return new ReportTemplateId(UUID.fromString(reportId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "REPORT_TEMPLATE", allowableValues = "REPORT_TEMPLATE")
    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }
}
