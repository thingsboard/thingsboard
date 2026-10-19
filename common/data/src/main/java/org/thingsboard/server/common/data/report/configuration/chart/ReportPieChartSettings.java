// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class ReportPieChartSettings extends ReportLatestChartSettings {

    private Boolean showLabel;
    private PieChartLabelPosition labelPosition;
    private Font labelFont;
    private String labelColor;
    private Float borderWidth;
    private String borderColor;
    private Double radius;
    private Boolean clockwise;

    public ReportPieChartSettings() {}

    public ReportPieChartSettings(ReportPieChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportPieChartSettings();
        }
        this.showLabel = input.getShowLabel() != null ? input.getShowLabel() : Boolean.TRUE;
        this.labelPosition = input.getLabelPosition() != null ? input.getLabelPosition() : PieChartLabelPosition.outside;
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "#000";
        this.borderWidth = input.getBorderWidth() != null ? input.getBorderWidth() : 0f;
        this.borderColor = input.getBorderColor() != null ? input.getBorderColor() : "#000";
        this.radius = input.getRadius() != null ? input.getRadius() : 80f;
        this.clockwise = input.getClockwise() != null ? input.getClockwise() : Boolean.FALSE;
    }

}
