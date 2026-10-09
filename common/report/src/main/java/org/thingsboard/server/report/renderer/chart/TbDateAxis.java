// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.Args;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbDateAxis extends DateAxis {

    private boolean gridlinesVisible;
    private transient Stroke gridlineStroke;
    private transient Paint gridlinePaint;

    public TbDateAxis(String label, TimeZone zone, Locale locale) {
        super(label, zone, locale);
        this.gridlinesVisible = true;
        this.gridlineStroke = new BasicStroke(1.0f);
        this.gridlinePaint = safeParseCssColor("rgba(0, 0, 0, 0.12)");
    }

    public boolean isGridlinesVisible() {
        return this.gridlinesVisible;
    }

    public void setGridlinesVisible(boolean visible) {
        if (this.gridlinesVisible != visible) {
            this.gridlinesVisible = visible;
            fireChangeEvent();
        }
    }

    public Stroke getGridlineStroke() {
        return this.gridlineStroke;
    }

    public void setGridlineStroke(Stroke stroke) {
        Args.nullNotPermitted(stroke, "stroke");
        this.gridlineStroke = stroke;
        fireChangeEvent();
    }

    public Paint getGridlinePaint() {
        return this.gridlinePaint;
    }

    public void setGridlinePaint(Paint paint) {
        Args.nullNotPermitted(paint, "paint");
        this.gridlinePaint = paint;
        fireChangeEvent();
    }

    @Override
    public AxisState draw(Graphics2D g2, double cursor, Rectangle2D plotArea,
                          Rectangle2D dataArea, RectangleEdge edge,
                          PlotRenderingInfo plotState) {
        AxisState state = super.draw(g2, cursor, plotArea, dataArea, edge, plotState);
        if (isVisible()) {
            drawGridlines(g2, dataArea, state.getTicks());
        }
        return state;
    }

    protected void drawGridlines(Graphics2D g2, Rectangle2D area,
                                 List<ValueTick> ticks) {
        if (isGridlinesVisible()) {
            for (ValueTick tick : ticks) {
                if (tick.getTickType() == TickType.MAJOR) {
                    XYPlot xyPlot = (XYPlot) getPlot();
                    xyPlot.getRenderer().drawDomainLine(g2, xyPlot, this,
                            area, tick.getValue(), getGridlinePaint(), getGridlineStroke());
                }
            }
        }
    }
}
