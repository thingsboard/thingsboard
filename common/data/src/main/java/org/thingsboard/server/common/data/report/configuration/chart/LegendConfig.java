// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class LegendConfig {
    private LegendPosition position;
    private Boolean sortDataKeys;
    private Boolean showMin;
    private Boolean showMax;
    private Boolean showAvg;
    private Boolean showTotal;
    private Boolean showLatest;

    public LegendConfig() {}

    public LegendConfig(LegendConfig input) {
        if (input == null) {
            input = new LegendConfig();
        }
        this.position = input.getPosition() != null ? input.getPosition() : LegendPosition.top;
        this.sortDataKeys = input.getSortDataKeys() != null ? input.getSortDataKeys() : Boolean.FALSE;
        this.showMin = input.getShowMin() != null ? input.getShowMin() : Boolean.FALSE;
        this.showMax = input.getShowMax() != null ? input.getShowMax() : Boolean.FALSE;
        this.showAvg = input.getShowAvg() != null ? input.getShowAvg() : Boolean.FALSE;
        this.showTotal = input.getShowTotal() != null ? input.getShowTotal() : Boolean.FALSE;
        this.showLatest = input.getShowLatest() != null ? input.getShowLatest() : Boolean.FALSE;
    }

    @JsonIgnore
    public boolean isSimpleLegend() {
        return !this.showMin && !this.showMax && !this.showAvg && !this.showTotal && !this.showLatest;
    }
}
