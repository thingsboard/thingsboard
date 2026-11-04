// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class ReportDoughnutChartSettings extends ReportLatestChartSettings {

    private DoughnutLayout layout;
    private Boolean clockwise;
    private Font totalValueFont;
    private String totalValueColor;

    public ReportDoughnutChartSettings() {}

    public ReportDoughnutChartSettings(ReportDoughnutChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportDoughnutChartSettings();
        }
        this.layout = input.getLayout() != null ? input.getLayout() : DoughnutLayout.DEFAULT;
        this.setShowTotal(this.layout == DoughnutLayout.WITH_TOTAL);
        this.clockwise = input.getClockwise() != null ? input.getClockwise() : Boolean.FALSE;
        this.totalValueFont = input.getTotalValueFont() != null ? input.getTotalValueFont() : Font.builder().family("Roboto")
                .size(24f)
                .weight(FontWeight.WEIGHT_500)
                .style(FontStyle.NORMAL)
                .build();
        this.totalValueColor = input.getTotalValueColor() != null ? input.getTotalValueColor() : "rgba(0, 0, 0, 0.87)";
    }

}
