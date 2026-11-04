// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.awt.Paint;
import java.awt.geom.Rectangle2D;

@Data
@RequiredArgsConstructor
public class TbVisualMapArea {

    private final Paint paint;
    private final Paint fillPaint;
    private final Rectangle2D area;

}
