// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.List;
import java.util.UUID;

@Data
public class ReportConfig {

    @NotNull
    @Schema(description = "Json object representing the report template id.")
    private ReportTemplateId reportTemplateId;
    @NotNull
    @Schema(description = "Json object representing the user id.", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private UserId userId;
    @Schema(description = "Timezone in which target dashboard will be presented in dashboard report.", example = "Europe/Kiev", requiredMode = Schema.RequiredMode.REQUIRED)
    private String timezone;

    @Schema(description = "List of ids representing the notification targets.")
    private List<UUID> targets;
    @Schema(description = "Json object representing the notification template id.", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private NotificationTemplateId notificationTemplateId;
    @Schema(description = "Indicates if report should be made public and available by public link.", example = "true")
    private boolean makePublic;

}
