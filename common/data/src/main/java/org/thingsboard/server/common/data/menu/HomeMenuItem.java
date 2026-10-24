// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.Views;

import static org.thingsboard.server.common.data.menu.MenuItemType.HOME;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class HomeMenuItem extends DefaultMenuItem {

    @Schema(description = "DEFAULT or DASHBOARD. DASHBOARD means default home page presentation changed to refer to dashboard")
    @JsonView(Views.Public.class)
    private HomeMenuItemType homeType;
    @Schema(description = "Id of the Dashboard to open, when user clicks the menu item")
    @JsonView(Views.Public.class)
    private String dashboardId;
    @Schema(description = "Hide the dashboard toolbar")
    @JsonView(Views.Public.class)
    private boolean hideDashboardToolbar;

    @Override
    @JsonView(Views.Private.class)
    public MenuItemType getType() {
        return HOME;
    }

    @Override
    @JsonView(Views.Private.class)
    public boolean isVisible() {
        return true;
    }
}
