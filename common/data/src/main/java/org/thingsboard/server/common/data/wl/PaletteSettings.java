// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.wl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@Schema
@Data
@EqualsAndHashCode
public class PaletteSettings {
    @Schema(description = "Primary palette JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    private Palette primaryPalette;
    @Schema(description = "Accent palette JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    private Palette accentPalette;

    public PaletteSettings merge(PaletteSettings otherPaletteSettings) {
        if (this.primaryPalette == null || StringUtils.isEmpty(this.primaryPalette.getType())) {
            this.primaryPalette = otherPaletteSettings.primaryPalette;
        }
        if (this.accentPalette == null || StringUtils.isEmpty(this.accentPalette.getType())) {
            this.accentPalette = otherPaletteSettings.accentPalette;
        }
        return this;
    }

}
