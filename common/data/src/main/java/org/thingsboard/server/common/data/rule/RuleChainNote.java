// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * Represents a sticky note on the rule chain canvas.
 * Notes are purely visual metadata and are stored as part of the rule chain.
 */
@Schema
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleChainNote {

    @Schema(description = "Unique identifier of the note on the canvas")
    private String id;

    @Schema(description = "Horizontal position of the note on the canvas, in pixels")
    private int x;

    @Schema(description = "Vertical position of the note on the canvas, in pixels")
    private int y;

    @Schema(description = "Width of the note, in pixels")
    private int width;

    @Schema(description = "Height of the note, in pixels")
    private int height;

    @ToString.Exclude
    @Schema(description = "Markdown or HTML content of the note")
    private String content;

    @Schema(description = "Background color of the note in CSS hex format, e.g. '#FFF9C4'")
    private String backgroundColor;

    @Schema(description = "Border color of the note in CSS hex format, e.g. '#E6C800'")
    private String borderColor;

    @Schema(description = "Border width of the note in pixels")
    private Integer borderWidth;

    @Schema(description = "Whether to apply the default markdown stylesheet to the note content")
    private Boolean applyDefaultMarkdownStyle;

    @ToString.Exclude
    @Schema(description = "Custom CSS styles applied to the note content")
    private String markdownCss;

}
