// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;

@Schema
@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DataKey {

    public DataKey(String name, String type, String label) {
        this.name = name;
        this.type = type;
        this.label = label;
    }

    private String name;
    private String type;
    private String label;
    private String color;
    private Integer decimals;
    private String units;
    private Aggregation aggregationType;
    private TimeWindowConfiguration timewindow;
    private boolean usePostProcessing;
    private String postFuncBody;
    private DataKeySettings settings;

    @JsonIgnore
    public boolean isComparisonKey() {
        if (settings != null && settings.getType() == DataKeySettingsType.TIME_SERIES_CHART) {
            TimeSeriesChartKeySettings timeSeriesChartKeySettings = (TimeSeriesChartKeySettings)settings;
            DataKeyComparisonSettings comparisonSettings = timeSeriesChartKeySettings.getComparisonSettings();
            if (comparisonSettings != null) {
                return comparisonSettings.getShowValuesForComparison() != null ? comparisonSettings.getShowValuesForComparison() : false;
            }
        }
        return false;
    }

}
