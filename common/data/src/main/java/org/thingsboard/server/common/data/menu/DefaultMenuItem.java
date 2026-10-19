// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.Views;

import java.util.List;

import static org.thingsboard.server.common.data.menu.MenuItemType.DEFAULT;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class DefaultMenuItem implements MenuItem {

    @Schema(description = "Unique identifier for predefined menu items", example = "home", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonView(Views.Public.class)
    private String id;
    @Schema(description = "Name of the menu item", example = "My Custom Menu")
    @JsonView(Views.Public.class)
    private String name;
    @Schema(description = "URL of the menu item icon. Overrides 'materialIcon'", example = "My Custom Menu")
    @JsonView(Views.Public.class)
    private String icon;
    @Schema(description = "Mark if menu item is visible for user")
    @JsonView(Views.Private.class)
    private boolean visible;
    @Schema(description = "List of child menu items")
    @JsonView(Views.Public.class)
    private List<DefaultMenuItem> pages;

    @Override
    @JsonView(Views.Private.class)
    public MenuItemType getType() {
        return DEFAULT;
    }

}
