// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

import java.util.List;
import java.util.UUID;

@Data
public class ReportRequest {

    @Schema(description = "Json object representing the report template id.")
    private ReportTemplateId reportTemplateId;
    @Schema(description = "Json object representing the report template config.")
    private ReportTemplateConfig reportTemplateConfig;

    @Schema(description = "Timezone used for report generation.", example = "Europe/Kiev")
    private String timezone;
    @Schema(description = "A string value representing the user id.", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private String userId;
    @Schema(description = "A boolean value indicating whether the generated report should be made public.", example = "false")
    private boolean makePublic;

    @Schema(description = "Json object representing the originator id.")
    private EntityId originator;

    private List<UUID> targets;
    private NotificationTemplateId notificationTemplateId;

}
