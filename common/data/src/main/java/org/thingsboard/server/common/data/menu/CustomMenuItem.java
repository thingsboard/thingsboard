// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.Views;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.common.data.menu.MenuItemType.CUSTOM;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class CustomMenuItem implements MenuItem {

    @Schema(description = "Name of the menu item", example = "My Custom Menu", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonView(Views.Public.class)
    private String name;
    @Schema(description = "URL of the menu item icon. Overrides 'materialIcon'", example = "My Custom Menu")
    @JsonView(Views.Public.class)
    private String icon;
    @Schema(description = "Type of menu item (LINK or SECTION). LINK type means item has no child items, SECTION type should have at least one child", example = "LINK")
    @NotNull
    @JsonView(Views.Public.class)
    private CMItemType menuItemType;
    @Schema(description = "Type of menu item (URL or DASHBOARD)", example = "URL")
    @JsonView(Views.Public.class)
    private CMItemLinkType linkType;
    @Schema(description = "Id of the Dashboard to open, when user clicks the menu item", example = "https://mycompany.com")
    @JsonView(Views.Public.class)
    private String dashboardId;
    @Schema(description = "Hide the dashboard toolbar")
    @JsonView(Views.Public.class)
    private Boolean hideDashboardToolbar;
    @Schema(description = "URL to open in the iframe, when user clicks the menu item", example = "https://myexternalurl.com")
    @JsonView(Views.Public.class)
    private String url;
    @Schema(description = "Set the access token of the current user to a new dashboard")
    @JsonView(Views.Public.class)
    private boolean setAccessToken;
    @Schema(description = "Mark if menu item is visible for user")
    @JsonView(Views.Private.class)
    private boolean visible;
    @Schema(description = "List of child menu items")
    @JsonView(Views.Public.class)
    private List<CustomMenuItem> pages;

    @Override
    @JsonView(Views.Private.class)
    public MenuItemType getType() {
        return CUSTOM;
    }

}
