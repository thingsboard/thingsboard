// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import java.awt.Paint;

public interface TbVisualMapRenderer {

    int getAreaPass();

    int getItemPass();

    void setCurrentVisualMapPaint(Paint paint);

    void setCurrentVisualMapFillPaint(Paint fillPaint);

    void setVisualMap(TbVisualMap visualMap);

}
