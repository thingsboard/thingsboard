// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class TimeSeriesChartThreshold extends ValueSourceConfig {

    @JsonProperty("yAxisId")
    private String yAxisId;
    private String units;
    private Integer decimals;
    private String lineColor;
    private ChartLineType lineType;
    private Float lineWidth;
    private ChartShape startSymbol;
    private Float startSymbolSize;
    private ChartShape endSymbol;
    private Float endSymbolSize;
    private Boolean showLabel;
    private ThresholdLabelPosition labelPosition;
    private Font labelFont;
    private String labelColor;
    private Boolean enableLabelBackground;
    private String labelBackground;

    public TimeSeriesChartThreshold() {}

    public TimeSeriesChartThreshold(TimeSeriesChartThreshold input) {
        if (input == null) {
            input = new TimeSeriesChartThreshold();
        }
        this.setType(input.getType() != null ? input.getType() : ValueSourceType.constant);
        this.yAxisId = input.getYAxisId() != null ? input.getYAxisId() : "default";
        this.units = input.getUnits();
        this.decimals = input.getDecimals();
        this.lineColor = input.getLineColor() != null ? input.getLineColor() : "rgba(0, 0, 0, 0.76)";
        this.lineType = input.getLineType() != null ? input.getLineType() : ChartLineType.solid;
        this.lineWidth = input.getLineWidth() != null ? input.getLineWidth() : 1f;
        this.startSymbol = input.getStartSymbol() != null ? input.getStartSymbol() : ChartShape.none;
        this.startSymbolSize = input.getStartSymbolSize() != null ? input.getStartSymbolSize() : 5f;
        this.endSymbol = input.getEndSymbol() != null ? input.getEndSymbol() : ChartShape.arrow;
        this.endSymbolSize = input.getEndSymbolSize() != null ? input.getEndSymbolSize() : 5f;
        this.showLabel = input.getShowLabel() != null ? input.getShowLabel() : Boolean.TRUE;
        this.labelPosition = input.getLabelPosition() != null ? input.getLabelPosition() : ThresholdLabelPosition.end;
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "rgba(0, 0, 0, 0.76)";
        this.enableLabelBackground = input.getEnableLabelBackground() != null ? input.getEnableLabelBackground() : Boolean.FALSE;
        this.labelBackground = input.getLabelBackground() != null ? input.getLabelBackground() : "rgba(255,255,255,0.56)";
    }

}
