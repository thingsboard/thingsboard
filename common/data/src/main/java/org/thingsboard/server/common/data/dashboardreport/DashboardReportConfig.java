// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.dashboardreport;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DashboardReportConfig {

    @Schema(description = "Base URL of ThingsBoard UI that should be accessible by Web Report Server.", example = "https:thingsboard.cloud", requiredMode = Schema.RequiredMode.REQUIRED)
    String baseUrl;
    @Schema(description = "A string value representing the dashboard id.", example = "784f394c-42b6-435a-983c-b7beff2784f9", requiredMode = Schema.RequiredMode.REQUIRED)
    String dashboardId;
    @Schema(description = "Target dashboard state for dashboard report generation.")
    String state;
    @Schema(description = "Timezone in which target dashboard will be presented in dashboard report.", example = "Europe/Kiev", requiredMode = Schema.RequiredMode.REQUIRED)
    String timezone;
    @Schema(description = "If set, timewindow configured in the target dashboard will be used during dashboard report generation.", example = "true")
    boolean useDashboardTimewindow;
    @Schema(description = "Specific dashboard timewindow that will be used during dashboard report generation.")
    JsonNode timewindow;
    @Schema(description = "If set, timewindow configured in the target dashboard will be used during dashboard report generation.", example = "report-%d{yyyy-MM-dd_HH:mm:ss}", requiredMode = Schema.RequiredMode.REQUIRED)
    String namePattern;
    @Schema(description = "Dashboard report file type, can be PDF | PNG | JPEG.", example = "pdf")
    String type;
    @Schema(description = "If set, credentials of user created this dashboard report configuration will be used to open dashboard UI during dashboard report generation.", example = "true")
    boolean useCurrentUserCredentials;
    @Schema(description = "A string value representing the user id.", example = "784f394c-42b6-435a-983c-b7beff2784f9", requiredMode = Schema.RequiredMode.REQUIRED)
    String userId;

}
