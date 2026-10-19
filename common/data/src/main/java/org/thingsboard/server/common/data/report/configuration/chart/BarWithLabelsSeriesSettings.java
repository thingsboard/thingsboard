// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class BarWithLabelsSeriesSettings extends BarSeriesSettings {

    private Boolean showSeriesLabel;
    private Font seriesLabelFont;
    private String seriesLabelColor;

    public BarWithLabelsSeriesSettings() {}

    public BarWithLabelsSeriesSettings(BarWithLabelsSeriesSettings input) {
        super(input);
        if (input == null) {
            input = new BarWithLabelsSeriesSettings();
        }
        this.showSeriesLabel = input.getShowSeriesLabel() != null ? input.getShowSeriesLabel() : Boolean.TRUE;
        this.seriesLabelFont = input.getSeriesLabelFont() != null ? input.getSeriesLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.seriesLabelColor = input.getSeriesLabelColor() != null ? input.getSeriesLabelColor() : "rgba(0, 0, 0, 0.54)";
    }
}
