// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;

@Schema
@Data
@AllArgsConstructor
public class HomeDashboardInfo {
    @Schema(description = "JSON object with the dashboard Id.")
    private DashboardId dashboardId;
    @Schema(description = HomeDashboard.HIDE_DASHBOARD_TOOLBAR_DESCRIPTION)
    private boolean hideDashboardToolbar;
}
