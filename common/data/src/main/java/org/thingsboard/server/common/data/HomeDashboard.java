// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class HomeDashboard extends Dashboard {

    public static final String HIDE_DASHBOARD_TOOLBAR_DESCRIPTION = "Hide dashboard toolbar flag. Useful for rendering dashboards on mobile.";

    @Schema(description = HIDE_DASHBOARD_TOOLBAR_DESCRIPTION)
    private boolean hideDashboardToolbar;

    public HomeDashboard(Dashboard dashboard, boolean hideDashboardToolbar) {
        super(dashboard);
        this.hideDashboardToolbar = hideDashboardToolbar;
    }

}
