// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

import java.util.HashMap;
import java.util.Map;

@Data
public class TimeSeriesChartXAxisSettings extends TimeSeriesChartAxisSettings {

    private static final Map<FormatTimeUnit, String> defaultXAxisTicksFormat = new HashMap<>();
    static {
        defaultXAxisTicksFormat.put(FormatTimeUnit.millisecond, "HH:mm:ss SSS");
        defaultXAxisTicksFormat.put(FormatTimeUnit.second, "HH:mm:ss");
        defaultXAxisTicksFormat.put(FormatTimeUnit.minute, "HH:mm");
        defaultXAxisTicksFormat.put(FormatTimeUnit.hour, "HH:mm");
        defaultXAxisTicksFormat.put(FormatTimeUnit.day, "MMM dd");
        defaultXAxisTicksFormat.put(FormatTimeUnit.month, "MMM");
        defaultXAxisTicksFormat.put(FormatTimeUnit.year, "yyyy");
    }

    private Map<FormatTimeUnit, String> ticksFormat;

    public TimeSeriesChartXAxisSettings() {}

    public TimeSeriesChartXAxisSettings(TimeSeriesChartXAxisSettings input, boolean comparison) {
        super(input != null ? input : new TimeSeriesChartXAxisSettings());
        if (input == null) {
            input = new TimeSeriesChartXAxisSettings();
        }
        this.setPosition(input.getPosition() != null ? input.getPosition() : (comparison ? AxisPosition.top : AxisPosition.bottom));
        this.setTickLabelFont(input.getTickLabelFont() != null ? input.getTickLabelFont() : Font.builder().family("Roboto")
                .size(10f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build());
        if (input.getTicksFormat() == null) {
            this.ticksFormat = defaultXAxisTicksFormat;
        } else {
            this.ticksFormat = input.getTicksFormat();
            defaultXAxisTicksFormat.forEach((key, val) -> {
                if (!this.ticksFormat.containsKey(key)) {
                    this.ticksFormat.put(key, val);
                }
            });
        }
    }

}
