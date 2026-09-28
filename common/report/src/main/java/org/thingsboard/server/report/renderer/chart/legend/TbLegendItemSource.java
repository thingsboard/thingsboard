// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import java.util.List;

public interface TbLegendItemSource {

    List<TbLegendItem> getTbLegendItems(TbLegendValuesRequest request);

}
