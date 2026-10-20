// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import lombok.Data;

@Data
public class TbLegendValues {

    private String min;
    private String max;
    private String avg;
    private String total;
    private String latest;

}
