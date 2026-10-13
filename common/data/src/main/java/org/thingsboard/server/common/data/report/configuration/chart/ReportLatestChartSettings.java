// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;

@Schema(subTypes = {ReportBarChartSettings.class, ReportPieChartSettings.class, ReportDoughnutChartSettings.class})
@Data
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReportBarChartSettings.class, name = "latestBarChart"),
        @JsonSubTypes.Type(value = ReportPieChartSettings.class, name = "pieChart"),
        @JsonSubTypes.Type(value = ReportDoughnutChartSettings.class, name = "doughnutChart"),
        @JsonSubTypes.Type(value = ReportDoughnutChartSettings.class, name = "horizontalDoughnutChart")
})
public class ReportLatestChartSettings {

    private Boolean showTitle;
    private String title;
    private Font titleFont;
    private String titleColor;
    private TextAlignment titleAlignment;

    private String units;
    private Integer decimals;

    private Boolean autoScale;
    private Boolean sortSeries;
    private Boolean showTotal;
    private Boolean showLegend;

    private LegendPosition legendPosition;
    private Font legendLabelFont;
    private String legendLabelColor;
    private Font legendValueFont;
    private String legendValueColor;
    private Boolean legendShowTotal;

    public ReportLatestChartSettings() {}

    public ReportLatestChartSettings(ReportLatestChartSettings input) {
        if (input == null) {
            input = new ReportLatestChartSettings();
        }
        this.showTitle = input.getShowTitle() != null ? input.getShowTitle() : Boolean.TRUE;
        this.title = input.getTitle() != null ? input.getTitle() : "Chart";
        this.titleFont = input.getTitleFont() != null ? input.getTitleFont() : Font.builder().family("Roboto")
                .size(18f)
                .weight(FontWeight.WEIGHT_500)
                .style(FontStyle.NORMAL)
                .build();
        this.titleColor = input.getTitleColor() != null ? input.getTitleColor() : "rgba(0, 0, 0, 0.87)";
        this.titleAlignment = input.getTitleAlignment() != null ? input.getTitleAlignment() : TextAlignment.CENTER;

        this.units = input.getUnits();
        this.decimals = input.getDecimals() != null ? input.getDecimals() : 0;

        this.autoScale = input.getAutoScale() != null ? input.getAutoScale() : Boolean.FALSE;
        this.sortSeries = input.getSortSeries() != null ? input.getSortSeries() : Boolean.FALSE;
        this.showTotal = input.getShowTotal() != null ? input.getShowTotal() : Boolean.FALSE;
        this.showLegend = input.getShowLegend() != null ? input.getShowLegend() : Boolean.TRUE;

        this.legendPosition = input.getLegendPosition() != null ? input.getLegendPosition() : LegendPosition.bottom;
        this.legendLabelFont = input.getLegendLabelFont() != null ? input.getLegendLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.legendLabelColor = input.getLegendLabelColor() != null ? input.getLegendLabelColor() : "rgba(0, 0, 0, 0.38)";
        this.legendValueFont = input.getLegendValueFont() != null ? input.getLegendValueFont() : Font.builder().family("Roboto")
                .size(14f)
                .weight(FontWeight.WEIGHT_500)
                .style(FontStyle.NORMAL)
                .build();
        this.legendValueColor = input.getLegendValueColor() != null ? input.getLegendValueColor() : "rgba(0, 0, 0, 0.87)";
        this.legendShowTotal = input.getLegendShowTotal() != null ? input.getLegendShowTotal() : Boolean.TRUE;
    }
}
