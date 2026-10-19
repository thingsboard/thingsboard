// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomMenuId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class CustomMenu extends CustomMenuInfo {

    @Schema(description = "Custom menu configuration")
    @Valid
    private CustomMenuConfig config;

    public CustomMenu() {
        super();
    }

    public CustomMenu(CustomMenuId id) {
        super(id);
    }

    public CustomMenu(CustomMenuInfo customMenuInfo) {
        super(customMenuInfo);
    }

    public CustomMenu(CustomMenu customMenu) {
        super(customMenu);
        this.config = customMenu.getConfig();
    }

    public CustomMenu(CustomMenuInfo customMenuInfo, CustomMenuConfig config) {
        super(customMenuInfo);
        this.config = config;
    }

}
