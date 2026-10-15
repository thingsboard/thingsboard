// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode
public class Palette {

    @Schema(description = "Name of the pre-defined palette, or 'custom'", example = "custom", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @JsonProperty("extends")
    @Schema(description = "Pre-defined palette name that the custom palette extends", example = "purple")
    private String extendsPalette;

    @Schema(description = "Mapping of hue identifier number to the rgb(a) color code")
    private Map<String, String> colors;

}
