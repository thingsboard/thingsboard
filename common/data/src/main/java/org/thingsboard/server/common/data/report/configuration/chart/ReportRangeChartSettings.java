// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReportRangeChartSettings extends ReportTimeSeriesChartSettings {

    private List<ColorRange> rangeColors;
    private String outOfRangeColor;
    private Boolean showRangeThresholds;
    private TimeSeriesChartThreshold rangeThreshold;
    private Boolean fillArea;
    private Float fillAreaOpacity;
    private LineSeriesSettings lineSettings;
    private String rangeUnits;
    private Integer rangeDecimals;

    public ReportRangeChartSettings() {}

    public ReportRangeChartSettings(ReportRangeChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportRangeChartSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Range chart");
        this.rangeColors = input.rangeColors != null ? new ArrayList<>(input.rangeColors) : new ArrayList<>();
        this.outOfRangeColor = input.outOfRangeColor != null ? input.outOfRangeColor : "#ccc";
        this.showRangeThresholds = input.showRangeThresholds != null ? input.showRangeThresholds : Boolean.TRUE;
        this.rangeThreshold = new TimeSeriesChartThreshold(input.getRangeThreshold());
        TimeSeriesChartThreshold inputRangeThreshold = input.getRangeThreshold();
        if (inputRangeThreshold == null) {
            inputRangeThreshold = new TimeSeriesChartThreshold();
        }
        this.rangeThreshold.setLineColor(inputRangeThreshold.getLineColor() != null ? inputRangeThreshold.getLineColor() : "#37383b");
        this.rangeThreshold.setLineType(inputRangeThreshold.getLineType() != null ? inputRangeThreshold.getLineType() : ChartLineType.dashed);
        this.rangeThreshold.setStartSymbol(inputRangeThreshold.getStartSymbol() != null ? inputRangeThreshold.getStartSymbol() : ChartShape.circle);
        this.rangeThreshold.setStartSymbolSize(inputRangeThreshold.getStartSymbolSize() != null ? inputRangeThreshold.getStartSymbolSize() : 5);
        this.rangeThreshold.setEndSymbol(inputRangeThreshold.getEndSymbol() != null ? inputRangeThreshold.getEndSymbol() : ChartShape.arrow);
        this.rangeThreshold.setEndSymbolSize(inputRangeThreshold.getEndSymbolSize() != null ? inputRangeThreshold.getEndSymbolSize() : 7);
        this.rangeThreshold.setLabelPosition(inputRangeThreshold.getLabelPosition() != null ? inputRangeThreshold.getLabelPosition() : ThresholdLabelPosition.insideEndTop);
        this.rangeThreshold.setLabelColor(inputRangeThreshold.getLabelColor() != null ? inputRangeThreshold.getLabelColor() : "#37383b");
        this.rangeThreshold.setEnableLabelBackground(inputRangeThreshold.getEnableLabelBackground() != null ? inputRangeThreshold.getEnableLabelBackground() : Boolean.TRUE);
        this.fillArea = input.getFillArea() != null ? input.getFillArea() : Boolean.TRUE;
        this.fillAreaOpacity = input.getFillAreaOpacity() != null ? input.getFillAreaOpacity() : 0.7f;
        this.lineSettings = new LineSeriesSettings(input.getLineSettings());
        this.rangeUnits = input.getRangeUnits() != null ? input.getRangeUnits() : "";
        this.rangeDecimals = input.getRangeDecimals() != null ? input.getRangeDecimals() : 0;
        this.setLegendLabelColor(input.getLegendLabelColor() != null ? input.getLegendLabelColor() : "rgba(0, 0, 0, 0.76)");
    }

    @JsonIgnore
    public TimeSeriesChartKeySettings toTimeSeriesChartKeySettings() {
        TimeSeriesChartKeySettings keySettings = new TimeSeriesChartKeySettings();
        keySettings.setSeriesType(TimeSeriesChartSeriesType.line);
        ChartFillSettings fillSettings = new ChartFillSettings();
        fillSettings.setType(this.fillArea ? ChartFillType.opacity : ChartFillType.none);
        fillSettings.setOpacity(this.fillAreaOpacity);
        LineSeriesSettings lineSettings = new LineSeriesSettings(this.lineSettings);
        lineSettings.setFillAreaSettings(fillSettings);
        keySettings.setLineSettings(lineSettings);
        return keySettings;
    }

}
