// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import lombok.Data;
import org.jfree.chart.LegendItem;

@Data
public class TbLegendItem {

    private final LegendItem legendItem;
    private final TbLegendValues legendValues;

    public TbLegendItem(LegendItem legendItem, TbLegendValues legendValues) {
        this.legendItem = legendItem;
        this.legendValues = legendValues;
    }

}
