// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;

@Data
public class ChartFillSettingsGradient {

    private Float start;
    private Float end;

    public ChartFillSettingsGradient() {}

    public ChartFillSettingsGradient(ChartFillSettingsGradient input) {
        if (input == null) {
            input = new ChartFillSettingsGradient();
        }
        this.start = input.getStart() != null ? input.getStart() : 100f;
        this.end = input.getEnd() != null ? input.getEnd() : 0f;
    }

}
