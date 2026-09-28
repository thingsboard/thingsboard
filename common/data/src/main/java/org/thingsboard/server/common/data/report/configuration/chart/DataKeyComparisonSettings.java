// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;

@Data
public class DataKeyComparisonSettings {

    private Boolean showValuesForComparison;
    private String comparisonValuesLabel;
    private String color;

    public DataKeyComparisonSettings() {}

    public DataKeyComparisonSettings(DataKeyComparisonSettings input) {
        if (input == null) {
            input = new DataKeyComparisonSettings();
        }
        this.showValuesForComparison = input.getShowValuesForComparison() != null ? input.getShowValuesForComparison() : Boolean.FALSE;
        this.comparisonValuesLabel = input.getComparisonValuesLabel() != null ? input.getComparisonValuesLabel() : "";
        this.color = input.getColor() != null ? input.getColor() : "";
    }

}
