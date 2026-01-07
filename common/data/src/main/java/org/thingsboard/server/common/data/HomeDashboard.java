/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class HomeDashboard extends Dashboard {

    public static final String HIDE_DASHBOARD_TOOLBAR_DESCRIPTION = "Hide dashboard toolbar flag. Useful for rendering dashboards on mobile.";

    @Schema(description = HIDE_DASHBOARD_TOOLBAR_DESCRIPTION)
    private boolean hideDashboardToolbar;

    public HomeDashboard(Dashboard dashboard, boolean hideDashboardToolbar) {
        super(dashboard);
        this.hideDashboardToolbar = hideDashboardToolbar;
    }

}
