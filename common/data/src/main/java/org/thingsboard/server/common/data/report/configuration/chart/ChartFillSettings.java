// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;

@Data
public class ChartFillSettings {

    private ChartFillType type;
    private Float opacity;
    private ChartFillSettingsGradient gradient;

    public ChartFillSettings() {}

    public ChartFillSettings(ChartFillSettings input) {
        if (input == null) {
            input = new ChartFillSettings();
        }
        this.type = input.getType() != null ? input.getType() : ChartFillType.none;
        this.opacity = input.getOpacity() != null ? input.getOpacity() : 0.4f;
        this.gradient = new ChartFillSettingsGradient(input.getGradient());
    }

}
