// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class TimeSeriesChartGridSettings {

    private Boolean show;
    private String backgroundColor;
    private Float borderWidth;
    private String borderColor;

    public TimeSeriesChartGridSettings() {}

    public TimeSeriesChartGridSettings(TimeSeriesChartGridSettings input) {
        if (input == null) {
            input = new TimeSeriesChartGridSettings();
        }
        this.show = input.getShow() != null ? input.getShow() : Boolean.FALSE;
        this.backgroundColor = input.getBackgroundColor() != null ? input.getBackgroundColor() : null;
        this.borderWidth = input.getBorderWidth() != null ? input.getBorderWidth() : 1f;
        this.borderColor = input.getBorderColor() != null ? input.getBorderColor() : "#ccc";
    }
}
