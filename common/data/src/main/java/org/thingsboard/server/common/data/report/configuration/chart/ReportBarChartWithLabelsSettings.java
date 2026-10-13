// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class ReportBarChartWithLabelsSettings extends ReportTimeSeriesChartSettings {

    private Boolean showBarLabel;
    private Font barLabelFont;
    private String barLabelColor;
    private Boolean showBarValue;
    private Font barValueFont;
    private String barValueColor;
    private Boolean showBarBorder;
    private Float barBorderWidth;
    private Float barBorderRadius;
    private ChartFillSettings barBackgroundSettings;
    private String barUnits;
    private Integer barDecimals;

    public ReportBarChartWithLabelsSettings() {}

    public ReportBarChartWithLabelsSettings(ReportBarChartWithLabelsSettings input) {
        super(input);
        if (input == null) {
            input = new ReportBarChartWithLabelsSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Bar chart with labels");
        this.showBarLabel = input.getShowBarLabel() != null ? input.getShowBarLabel() : Boolean.TRUE;
        this.barLabelFont = input.getBarLabelFont() != null ? input.getBarLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.barLabelColor = input.getBarLabelColor() != null ? input.getBarLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showBarValue = input.getShowBarValue() != null ? input.getShowBarValue() : Boolean.TRUE;
        this.barValueFont = input.getBarValueFont() != null ? input.getBarValueFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.BOLD)
                .style(FontStyle.NORMAL)
                .build();
        this.barValueColor = input.getBarValueColor() != null ? input.getBarValueColor() : "rgba(0, 0, 0, 0.76)";
        this.showBarBorder = input.getShowBarBorder() != null ? input.getShowBarBorder() : Boolean.FALSE;
        this.barBorderWidth = input.getBarBorderWidth() != null ? input.getBarBorderWidth() : 2f;
        this.barBorderRadius = input.getBarBorderRadius() != null ? input.getBarBorderRadius() : 0f;
        this.barBackgroundSettings = new ChartFillSettings(input.getBarBackgroundSettings());
        this.barUnits = input.getBarUnits() != null ? input.getBarUnits() : "%";
        this.barDecimals = input.getBarDecimals() != null ? input.getBarDecimals() : 0;
        TimeSeriesChartBarWidthSettings barWidthSettings = input.getBarWidthSettings();
        if (barWidthSettings == null) {
            barWidthSettings = new TimeSeriesChartBarWidthSettings();
        }
        barWidthSettings.setBarGap(barWidthSettings.getBarGap() != null ? barWidthSettings.getBarGap() : 0f);
        barWidthSettings.setIntervalGap(barWidthSettings.getIntervalGap() != null ? barWidthSettings.getIntervalGap() : 0.5f);
        this.setBarWidthSettings(barWidthSettings);
    }

    @JsonIgnore
    public TimeSeriesChartKeySettings toTimeSeriesChartKeySettings() {
        TimeSeriesChartKeySettings keySettings = new TimeSeriesChartKeySettings();
        keySettings.setSeriesType(TimeSeriesChartSeriesType.bar);

        BarWithLabelsSeriesSettings barSettings = new BarWithLabelsSeriesSettings();

        barSettings.setShowBorder(getShowBarBorder());
        barSettings.setBorderWidth(getBarBorderWidth());
        barSettings.setBorderRadius(getBarBorderRadius());
        barSettings.setBackgroundSettings(getBarBackgroundSettings());

        barSettings.setShowLabel(getShowBarValue());
        barSettings.setLabelFont(getBarValueFont());
        barSettings.setLabelColor(getBarValueColor());

        barSettings.setShowSeriesLabel(getShowBarLabel());
        barSettings.setSeriesLabelFont(getBarLabelFont());
        barSettings.setSeriesLabelColor(getBarLabelColor());

        keySettings.setBarSettings(barSettings);

        return keySettings;
    }

}
