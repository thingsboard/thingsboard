// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class LineSeriesSettings {

    private Boolean showLine;
    private Boolean step;
    private LineSeriesStepType stepType;
    private Boolean smooth;
    private ChartLineType lineType;
    private Float lineWidth;
    private Boolean showPoints;
    private Boolean showPointLabel;
    private ChartLabelPosition pointLabelPosition;
    private Font pointLabelFont;
    private String pointLabelColor;
    private Boolean enablePointLabelBackground;
    private String pointLabelBackground;
    private ChartShape pointShape;
    private Float pointSize;
    private ChartFillSettings fillAreaSettings;

    public LineSeriesSettings() {}

    public LineSeriesSettings(LineSeriesSettings input) {
        if (input == null) {
            input = new LineSeriesSettings();
        }
        this.showLine = input.getShowLine() != null ? input.getShowLine() : Boolean.TRUE;
        this.step = input.getStep() != null ? input.getStep() : Boolean.FALSE;
        this.stepType = input.getStepType() != null ? input.getStepType() : LineSeriesStepType.start;
        this.smooth = input.getSmooth() != null ? input.getSmooth() : Boolean.FALSE;
        this.lineType = input.getLineType() != null ? input.getLineType() : ChartLineType.solid;
        this.lineWidth = input.getLineWidth() != null ? input.getLineWidth() : 2.0f;
        this.showPoints = input.getShowPoints() != null ? input.getShowPoints() : Boolean.FALSE;
        this.showPointLabel = input.getShowPointLabel() != null ? input.getShowPointLabel() : Boolean.FALSE;
        this.pointLabelPosition = input.getPointLabelPosition() != null ? input.getPointLabelPosition() : ChartLabelPosition.top;
        this.pointLabelFont = input.getPointLabelFont() != null ? input.getPointLabelFont() : Font.builder().family("Roboto")
                .size(11f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.pointLabelColor = input.getPointLabelColor() != null ? input.getPointLabelColor() : "rgba(0, 0, 0, 0.76)";
        this.enablePointLabelBackground = input.getEnablePointLabelBackground() != null ? input.getEnablePointLabelBackground() : Boolean.FALSE;
        this.pointLabelBackground = input.getPointLabelBackground() != null ? input.getPointLabelBackground() : "rgba(255,255,255,0.56)";
        this.pointShape = input.getPointShape() != null ? input.getPointShape() : ChartShape.emptyCircle;
        this.pointSize = input.getPointSize() != null ? input.getPointSize() : 4.0f;
        this.fillAreaSettings = new ChartFillSettings(input.getFillAreaSettings());
    }

}
