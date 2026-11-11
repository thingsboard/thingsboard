// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

@Data
public class TimeSeriesChartKeySettings implements DataKeySettings {

    @JsonProperty("yAxisId")
    private String yAxisId;
    private Boolean showInLegend;
    private TimeSeriesChartSeriesType seriesType;
    private LineSeriesSettings lineSettings;
    private BarSeriesSettings barSettings;
    private DataKeyComparisonSettings comparisonSettings;

    public TimeSeriesChartKeySettings() {}

    public TimeSeriesChartKeySettings(TimeSeriesChartKeySettings input) {
        if (input == null) {
            input = new TimeSeriesChartKeySettings();
        }
        this.yAxisId = input.getYAxisId() != null ? input.getYAxisId() : "default";
        this.showInLegend = input.getShowInLegend() != null ? input.getShowInLegend() : Boolean.TRUE;
        this.seriesType = input.getSeriesType() != null ? input.getSeriesType() : TimeSeriesChartSeriesType.line;
        this.lineSettings = new LineSeriesSettings(input.getLineSettings());
        if (input.getBarSettings() != null && input.getBarSettings() instanceof BarWithLabelsSeriesSettings barWithLabelsSeriesSettings) {
            this.barSettings = new BarWithLabelsSeriesSettings(barWithLabelsSeriesSettings);
        } else {
            this.barSettings = new BarSeriesSettings(input.getBarSettings());
        }
        this.comparisonSettings = new DataKeyComparisonSettings(input.getComparisonSettings());
    }

    @Override
    public DataKeySettingsType getType() {
        return DataKeySettingsType.TIME_SERIES_CHART;
    }
}
