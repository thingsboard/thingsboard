// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class BarSeriesSettings {

    private Boolean showBorder;
    private Float borderWidth;
    private Float borderRadius;
    private Double barWidth;
    private Boolean showLabel;
    private ChartLabelPosition labelPosition;
    private Font labelFont;
    private String labelColor;
    private Boolean enableLabelBackground;
    private String labelBackground;
    private ChartFillSettings backgroundSettings;

    public BarSeriesSettings() {}

    public BarSeriesSettings(BarSeriesSettings input) {
        if (input == null) {
            input = new BarSeriesSettings();
        }
        this.showBorder = input.getShowBorder() != null ? input.getShowBorder() : Boolean.FALSE;
        this.borderWidth = input.getBorderWidth() != null ? input.getBorderWidth() : 2.0f;
        this.borderRadius = input.getBorderRadius() != null ? input.getBorderRadius() : 0.0f;
        this.showLabel = input.getShowLabel() != null ? input.getShowLabel() : Boolean.FALSE;
        this.labelPosition = input.getLabelPosition() != null ? input.getLabelPosition() : ChartLabelPosition.top;
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto")
                .size(11f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "rgba(0, 0, 0, 0.76)";
        this.enableLabelBackground = input.getEnableLabelBackground() != null ? input.getEnableLabelBackground() : Boolean.FALSE;
        this.labelBackground = input.getLabelBackground() != null ? input.getLabelBackground() : "rgba(255,255,255,0.56)";
        this.backgroundSettings = new ChartFillSettings(input.getBackgroundSettings());
    }

}

