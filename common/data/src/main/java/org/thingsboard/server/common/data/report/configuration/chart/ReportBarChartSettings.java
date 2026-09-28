// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class ReportBarChartSettings extends ReportLatestChartSettings {

    private Double axisMin;
    private Double axisMax;
    private Font axisTickLabelFont;
    private String axisTickLabelColor;
    private BarSeriesSettings barSettings;

    public ReportBarChartSettings() {}

    public ReportBarChartSettings(ReportBarChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportBarChartSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Bars");
        this.axisMin = input.getAxisMin();
        this.axisMax = input.getAxisMax();
        this.axisTickLabelFont = input.getAxisTickLabelFont() != null ? input.getAxisTickLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.axisTickLabelColor = input.getAxisTickLabelColor() != null ? input.getAxisTickLabelColor() : "rgba(0, 0, 0, 0.54)";
        BarSeriesSettings inputBarSettings = input.getBarSettings();
        this.barSettings = new BarSeriesSettings(inputBarSettings);
        this.barSettings.setBarWidth(inputBarSettings != null && inputBarSettings.getBarWidth() != null ? inputBarSettings.getBarWidth() : 80.0);
        this.barSettings.setShowLabel(inputBarSettings != null && inputBarSettings.getShowLabel() != null ? inputBarSettings.getShowLabel() : true);
    }

}
