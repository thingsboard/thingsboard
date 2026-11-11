// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Schema
@Data
public abstract class TimeSeriesChartAxisSettings {

    private Boolean show;
    private String label;
    private Font labelFont;
    private String labelColor;
    private AxisPosition position;
    private Boolean showTickLabels;
    private Font tickLabelFont;
    private String tickLabelColor;
    private Boolean showTicks;
    private String ticksColor;
    private Boolean showLine;
    private String lineColor;
    private Boolean showSplitLines;
    private String splitLinesColor;

    protected TimeSeriesChartAxisSettings() {}

    protected TimeSeriesChartAxisSettings(TimeSeriesChartAxisSettings input) {
        this.show = input.getShow() != null ? input.getShow() : Boolean.TRUE;
        this.label = input.getLabel() != null ? input.getLabel() : "";
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.BOLD)
                .style(FontStyle.NORMAL)
                .build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showTickLabels = input.getShowTickLabels() != null ? input.getShowTickLabels() : Boolean.TRUE;
        this.tickLabelColor = input.getTickLabelColor() != null ? input.getTickLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showTicks = input.getShowTicks() != null ? input.getShowTicks() : Boolean.TRUE;
        this.ticksColor = input.getTicksColor() != null ? input.getTicksColor() : "rgba(0, 0, 0, 0.54)";
        this.showLine = input.getShowLine() != null ? input.getShowLine() : Boolean.TRUE;
        this.lineColor = input.getLineColor() != null ? input.getLineColor() : "rgba(0, 0, 0, 0.54)";
        this.showSplitLines = input.getShowSplitLines() != null ? input.getShowSplitLines() : Boolean.TRUE;
        this.splitLinesColor = input.getShowSplitLines() != null ? input.getSplitLinesColor() : "rgba(0, 0, 0, 0.12)";
    }

}
