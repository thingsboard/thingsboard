// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import lombok.Data;

@Data
public class TbLatestChartLegendItem {

    private String color;
    private String label;
    private String value;
    private boolean hasValue;
    private boolean total;

}
