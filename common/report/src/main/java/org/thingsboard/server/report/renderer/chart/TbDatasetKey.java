// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesStepType;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;

import java.util.Objects;

@Data
public class TbDatasetKey implements Comparable<TbDatasetKey> {

    private String yAxisId;
    private boolean comparison;
    private TimeSeriesChartSeriesType seriesType;
    private boolean stepLine;
    private LineSeriesStepType stepType;
    private boolean smoothLine;
    private boolean fillArea;

    private int datasetIndex;

    public TbDatasetKey(TimeSeriesChartKeySettings keySettings, boolean comparison) {
        this.comparison = comparison;
        this.yAxisId = keySettings.getYAxisId();
        this.seriesType = keySettings.getSeriesType();
        if (this.seriesType == TimeSeriesChartSeriesType.line) {
            LineSeriesSettings lineSettings = keySettings.getLineSettings();
            this.stepLine = lineSettings.getStep();
            if (this.stepLine) {
                this.stepType = lineSettings.getStepType();
            }
            this.smoothLine = lineSettings.getSmooth();
            this.fillArea = lineSettings.getFillAreaSettings().getType() != ChartFillType.none;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TbDatasetKey that)) return false;
        if (comparison != that.comparison) return false;
        if (!Objects.equals(yAxisId, that.yAxisId)) return false;
        if (seriesType == that.seriesType) {
            if (seriesType == TimeSeriesChartSeriesType.bar) {
                return true;
            } else {
                return stepLine == that.stepLine && stepType == that.stepType && smoothLine == that.smoothLine && fillArea == that.fillArea;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (seriesType == TimeSeriesChartSeriesType.bar) {
            return Objects.hash(comparison, yAxisId, seriesType);
        } else {
            return Objects.hash(comparison, yAxisId, stepLine, stepType, smoothLine, fillArea);
        }
    }

    @Override
    public int compareTo(@NotNull TbDatasetKey tbDatasetKey) {
        int result = 0;
        if (this.comparison && !tbDatasetKey.comparison) {
            result = 1;
        } else if (!this.comparison && tbDatasetKey.comparison) {
            result = -1;
        }
        if (result == 0) {
            if (this.seriesType == tbDatasetKey.seriesType) {
                if (this.seriesType != TimeSeriesChartSeriesType.bar) {
                    if (this.fillArea && !tbDatasetKey.fillArea) {
                        result = -1;
                    } else if (!this.fillArea && tbDatasetKey.fillArea) {
                        result = 1;
                    }
                }
            } else if (this.seriesType == TimeSeriesChartSeriesType.bar) {
                result = -1;
            } else {
                result = 1;
            }
        }
        if (result == 0 && !Objects.equals(yAxisId, tbDatasetKey.yAxisId)) {
            if (this.yAxisId.equals("default")) {
                result = -1;
            } else {
                result = 1;
            }
        }
        return result;
    }
}
