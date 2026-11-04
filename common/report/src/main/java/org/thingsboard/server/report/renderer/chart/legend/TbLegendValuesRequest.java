// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import lombok.Data;

@Data
public class TbLegendValuesRequest {

    private boolean min;
    private boolean max;
    private boolean avg;
    private boolean total;
    private boolean latest;

}
