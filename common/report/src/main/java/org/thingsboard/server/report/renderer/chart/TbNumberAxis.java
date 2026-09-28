// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import lombok.Getter;
import lombok.Setter;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.Tick;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueAxisPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.data.Range;
import org.jfree.data.RangeType;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbNumberAxis extends NumberAxis {

    private boolean gridlinesVisible;
    private transient Stroke gridlineStroke;
    private transient Paint gridlinePaint;

    @Setter
    private Double axisMin;

    @Setter
    private Double axisMax;

    @Setter
    private Integer splitNumber;

    private final TbNumberTickUnitSource tbNumberTickUnitSource = new TbNumberTickUnitSource();
    private final TbNumberAxis parentAxis;

    private List<TbStateTick> stateTicks;

    private TbNumberAxisTicks ticks;

    public TbNumberAxis(String label, TbNumberAxis parentAxis) {
        super(label);
        this.setUpperMargin(0);
        this.setLowerMargin(0);
        this.parentAxis = parentAxis;
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

    public void setStateTicks(List<TbStateTick> stateTicks) {
        this.stateTicks = stateTicks;
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
                    Plot plot = getPlot();
                    if (plot instanceof XYPlot xyPlot) {
                        xyPlot.getRenderer().drawRangeLine(g2, xyPlot, this,
                                area, tick.getValue(), getGridlinePaint(), getGridlineStroke());
                    } else if (plot instanceof CategoryPlot categoryPlot) {
                        categoryPlot.getRenderer().drawRangeLine(g2, categoryPlot, this,
                                area, tick.getValue(), getGridlinePaint(), getGridlineStroke());
                    }
                }
            }
        }
    }

    @Override
    protected List<Tick> refreshTicksVertical(Graphics2D g2,
                                              Rectangle2D dataArea, RectangleEdge edge) {

        List<Tick> result = new ArrayList<>();
        Font tickLabelFont = getTickLabelFont();
        g2.setFont(tickLabelFont);
        if ((splitNumber == null || splitNumber == 0) && isAutoTickUnitSelection()) {
            selectAutoTickUnit(g2, dataArea, edge);
        }
        if (this.ticks == null) {
            this.adjustTicksAndRange();
        }

        if (ticks.getTicksCount() <= ValueAxis.MAXIMUM_TICK_COUNT) {

            boolean drawFirstLabel = true;
            boolean drawLastLabel = true;

            if (ticks.checkFirstTickIntersection()) {
                double labelHeight = calculateTickLabelHeight(g2, "123");
                double firstLabelPos = valueToJava2D(ticks.firstTickValue(getRange()), dataArea, edge);
                double nextLabelPos = valueToJava2D(ticks.getTickValue(1, getRange()), dataArea, edge);
                if ((firstLabelPos - labelHeight / 2) < (nextLabelPos + labelHeight / 2)) {
                    drawFirstLabel = false;
                }
            }

            if (ticks.checkLastTickIntersection()) {
                double labelHeight = calculateTickLabelHeight(g2, "123");
                double lastLabelPos = valueToJava2D(ticks.lastTickValue(getRange()), dataArea, edge);
                double prevLabelPos = valueToJava2D(ticks.getTickValue(ticks.getTicksCount()-2, getRange()), dataArea, edge);
                if ((lastLabelPos + labelHeight / 2) > (prevLabelPos - labelHeight / 2)) {
                    drawLastLabel = false;
                }
            }

            for (int i = 0; i < ticks.getTicksCount(); i++) {
                double currentTickValue = ticks.getTickValue(i, getRange());
                if (currentTickValue == 0.0 && Double.compare(currentTickValue, 0.0) < 0) {
                    currentTickValue = 0.0;
                }
                String tickLabel = null;
                boolean drawLabel = (i == 0 && drawFirstLabel) || (i == ticks.getTicksCount() - 1 && drawLastLabel) || i > 0 && i < ticks.getTicksCount() - 1;
                if (drawLabel) {
                    if (ticks.isStateTicks()) {
                        tickLabel = ticks.getStateTickLabel(i);
                    } else {
                        NumberFormat formatter = getNumberFormatOverride();
                        if (formatter != null) {
                            tickLabel = formatter.format(currentTickValue);
                        } else {
                            tickLabel = getTickUnit().valueToString(currentTickValue);
                        }
                    }
                }

                TextAnchor anchor;
                TextAnchor rotationAnchor;
                double angle = 0.0;
                if (edge == RectangleEdge.LEFT) {
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                }
                else {
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                }
                Tick tick = new NumberTick(currentTickValue, tickLabel, anchor,
                        rotationAnchor, angle);
                result.add(tick);
            }
        }
        return result;
    }

    @Override
    protected void autoAdjustRange() {

        Plot plot = getPlot();
        if (plot == null) {
            return;  // no plot, no data
        }

        if (plot instanceof ValueAxisPlot) {
            ValueAxisPlot vap = (ValueAxisPlot) plot;

            Range r = vap.getDataRange(this);
            if (r == null) {
                r = getDefaultAutoRange();
            }

            double upper = r.getUpperBound();
            double lower = r.getLowerBound();
            if (this.getRangeType() == RangeType.POSITIVE) {
                lower = Math.max(0.0, lower);
                upper = Math.max(0.0, upper);
            }
            else if (this.getRangeType() == RangeType.NEGATIVE) {
                lower = Math.min(0.0, lower);
                upper = Math.min(0.0, upper);
            }

            if (getAutoRangeIncludesZero()) {
                lower = Math.min(lower, 0.0);
                upper = Math.max(upper, 0.0);
            }
            double range = upper - lower;

            // if fixed auto range, then derive lower bound...
            double fixedAutoRange = getFixedAutoRange();
            if (fixedAutoRange > 0.0) {
                lower = upper - fixedAutoRange;
            }
            else {
                // ensure the autorange is at least <minRange> in size...
                double minRange = getAutoRangeMinimumSize();
                if (range < minRange) {
                    double expand = (minRange - range) / 2;
                    upper = upper + expand;
                    lower = lower - expand;
                    if (lower == upper) { // see bug report 1549218
                        double adjust = Math.abs(lower) / 10.0;
                        lower = lower - adjust;
                        upper = upper + adjust;
                    }
                    if (this.getRangeType() == RangeType.POSITIVE) {
                        if (lower < 0.0) {
                            upper = upper - lower;
                            lower = 0.0;
                        }
                    }
                    else if (this.getRangeType() == RangeType.NEGATIVE) {
                        if (upper > 0.0) {
                            lower = lower - upper;
                            upper = 0.0;
                        }
                    }
                }

                if (getAutoRangeStickyZero()) {
                    if (upper <= 0.0) {
                        upper = Math.min(0.0, upper + getUpperMargin() * range);
                    }
                    else {
                        upper = upper + getUpperMargin() * range;
                    }
                    if (lower >= 0.0) {
                        lower = Math.max(0.0, lower - getLowerMargin() * range);
                    }
                    else {
                        lower = lower - getLowerMargin() * range;
                    }
                }
                else {
                    upper = upper + getUpperMargin() * range;
                    lower = lower - getLowerMargin() * range;
                }
            }

            if (this.axisMin != null) {
                lower = this.axisMin;
            }

            if (this.axisMax != null) {
                upper = this.axisMax;
            }

            if (upper <= lower) {
                upper = lower + 1.0;
            }

            setRange(new Range(lower, upper), false, false);
        }

    }

    @Override
    protected double estimateMaximumTickLabelHeight(Graphics2D g2) {
        double result = calculateTickLabelHeight(g2, "123");
        return result * 2;
    }

    private double calculateTickLabelHeight(Graphics2D g2, String label) {
        RectangleInsets tickLabelInsets = getTickLabelInsets();
        double result = tickLabelInsets.getTop() + tickLabelInsets.getBottom();

        Font tickLabelFont = getTickLabelFont();
        FontRenderContext frc = g2.getFontRenderContext();
        result += tickLabelFont.getLineMetrics(label, frc).getHeight();
        return result;
    }

    private void adjustTicksAndRange() {
        double lower = this.axisMin != null ? this.axisMin : getRange().getLowerBound();
        double upper = this.axisMax != null ? this.axisMax : getRange().getUpperBound();
        if (upper <= lower) {
            upper = lower + 1.0;
        }
        double length = upper - lower;
        NumberTickUnit tickUnit = getTickUnit();
        double size = tickUnit.getSize();
        if (this.parentAxis != null) {
            if (this.axisMin == null) {
                Range parentRange = this.parentAxis.getRange();
                if (parentRange.getLowerBound() < 0 && parentRange.getUpperBound() > 0) {
                    double zeroDistance = Math.abs(parentRange.getLowerBound()) / parentRange.getLength();
                    lower -= zeroDistance * length;
                    length = upper - lower;
                }
            }
            size = calculateChildSize(length);
            int parentSplitCount = this.parentAxis.getTicks().getTicksCount() - 1;
            length = parentSplitCount * size;
            if (this.axisMin == null) {
                if (this.axisMax == null && upper == 0.0) {
                    lower = upper - length;
                } else {
                    lower = nearestLower(lower, size);
                }
            }
            if (this.axisMax == null) {
                upper = lower + length;
                if (upper < getRange().getUpperBound()) {
                    double newLength = getRange().getUpperBound() - lower;
                    size = calculateChildSize(newLength);
                    length = parentSplitCount * size;
                    upper = lower + length;
                }
                setRange(new Range(lower, upper), false, false);
            }
        } else {
            if (splitNumber != null && splitNumber > 0) {
                size = length / splitNumber;
            }
            if (isAutoTickUnitSelection()) {
                tickUnit = (NumberTickUnit) this.tbNumberTickUnitSource.getCeilingTickUnit(size);
                setTickUnit(tickUnit, false, false);
            }
        }

        double currentAxisMin;
        double currentAxisMax;
        size = tickUnit.getSize();
        lower = getRange().getLowerBound();
        upper = getRange().getUpperBound();

        if (this.axisMin == null) {
            if (this.parentAxis == null) {
                lower = nearestLower(lower, size);
            }
            currentAxisMin = lower;
        } else {
            currentAxisMin = this.axisMin;
        }
        if (this.axisMax == null) {
            if (this.parentAxis == null) {
                upper = nearestUpper(lower, upper, size);
            }
            currentAxisMax = upper;
        } else {
            currentAxisMax = this.axisMax;
        }
        if (currentAxisMax <= currentAxisMin) {
            currentAxisMax = currentAxisMin + 1.0;
        }
        setRange(new Range(currentAxisMin, currentAxisMax), false, false);
        this.calculateTicks();
    }

    private double calculateChildSize(double length) {
        NumberTickUnit tickUnit = getTickUnit();
        double size = tickUnit.getSize();
        int parentSplitCount = this.parentAxis.getTicks().getTicksCount() - 1;
        boolean roundToNearest = true;
        if (splitNumber != null && splitNumber > 0) {
            size = length / splitNumber;
        } else if (isAutoTickUnitSelection()) {
            size = length / parentSplitCount;
            roundToNearest = false;
        }
        if (isAutoTickUnitSelection()) {
            tickUnit = (NumberTickUnit) this.tbNumberTickUnitSource.getCeilingTickUnit(size, roundToNearest);
            setTickUnit(tickUnit, false, false);
            size = tickUnit.getSize();
        }
        return size;
    }

    private double nearestLower(double lower, double size) {
        BigDecimal sizeDecimal = BigDecimal.valueOf(size);
        BigDecimal rest = BigDecimal.valueOf(Math.abs(lower)).remainder(sizeDecimal);
        if (rest.compareTo(BigDecimal.ZERO) > 0) {
            if (lower < 0) {
                rest = sizeDecimal.subtract(rest);
            }
            return BigDecimal.valueOf(lower).subtract(rest).doubleValue();
        }
        return lower;
    }

    private double nearestUpper(double lower, double upper, double size) {
        BigDecimal upperDecimal = BigDecimal.valueOf(upper);
        BigDecimal sizeDecimal = BigDecimal.valueOf(size);
        BigDecimal length = upperDecimal.subtract(BigDecimal.valueOf(lower));
        BigDecimal rest = length.remainder(sizeDecimal);
        if (rest.compareTo(BigDecimal.ZERO) > 0) {
            return upperDecimal.add(sizeDecimal.subtract(rest)).doubleValue();
        }
        return upper;
    }

    private TbNumberAxisTicks getTicks() {
        return this.ticks;
    }

    private void calculateTicks() {
        if (stateTicks != null) {
            this.ticks = new TbNumberAxisTicks(true);
            for (TbStateTick stateTick : stateTicks) {
                this.ticks.addStateTickValue(stateTick.getValue(), stateTick.getLabel(), getRange());
            }
        } else {
            if (this.parentAxis == null) {
                TickUnit tu = getTickUnit();
                double size = tu.getSize();
                this.ticks = new TbNumberAxisTicks();
                double currentTickValue = getRange().getLowerBound();
                double maxTickValue = getRange().getUpperBound();
                this.ticks.addTickValue(currentTickValue, getRange());
                double lowestVisibleTickValue = isAutoTickUnitSelection() ? calculateLowestVisibleTickValue() : currentTickValue;
                if (lowestVisibleTickValue > currentTickValue) {
                    this.ticks.setAdditionalFistTick(true);
                    currentTickValue = lowestVisibleTickValue;
                } else {
                    currentTickValue += size;
                }
                while (currentTickValue < maxTickValue) {
                    this.ticks.addTickValue(currentTickValue, getRange());
                    currentTickValue += size;
                }
                if (maxTickValue > getRange().getLowerBound()) {
                    if (maxTickValue != currentTickValue) {
                        this.ticks.setAdditionalLastTick(true);
                    }
                    this.ticks.addTickValue(maxTickValue, getRange());
                }
            } else {
                Double unitSize = !isAutoTickUnitSelection() ? getTickUnit().getSize() : null;
                this.ticks = this.parentAxis.getTicks().computeChildTicks(this.splitNumber, unitSize, getRange());
            }
        }
    }

    private static class TbNumberAxisTicks {

        private final List<Double> tickValues = new ArrayList<>();
        private final List<String> stateTickLabels = new ArrayList<>();

        @Getter
        private final boolean stateTicks;

        @Getter
        @Setter
        private boolean additionalFistTick = false;

        @Getter
        @Setter
        private boolean additionalLastTick = false;

        public TbNumberAxisTicks() {
            this(false);
        }

        public TbNumberAxisTicks(boolean stateTicks) {
            this.stateTicks = stateTicks;
        }

        public void addTickValue(double tickValue, Range range) {
           addTick((tickValue - range.getLowerBound()) / range.getLength());
        }

        public void addStateTickValue(double tickValue, String label, Range range) {
            addStateTick((tickValue - range.getLowerBound()) / range.getLength(), label);
        }

        public void addTick(double tick) {
            tickValues.add(tick);
        }

        public void addStateTick(double tick, String label) {
            tickValues.add(tick);
            stateTickLabels.add(label);
        }

        public int getTicksCount() {
            return tickValues.size();
        }

        public Double getTickValue(int index, Range range) {
            return range.getLowerBound()  + tickValues.get(index) * range.getLength();
        }

        public String getStateTickLabel(int index) {
            return stateTickLabels.get(index);
        }

        public Double firstTickValue(Range range) {
            return getTickValue(0, range);
        }

        public Double lastTickValue(Range range) {
            return getTickValue(tickValues.size() - 1, range);
        }

        public boolean checkFirstTickIntersection() {
            return this.additionalFistTick && this.tickValues.size() > 2;
        }

        public boolean checkLastTickIntersection() {
            return this.additionalLastTick && this.tickValues.size() > 2;
        }

        public TbNumberAxisTicks computeChildTicks(Integer splitCount, Double unitSize, Range range) {
            if (this.additionalFistTick || this.additionalLastTick || ((splitCount == null || splitCount == 0) && unitSize == null)) {
                return this;
            } else {
                Integer step = null;
                int parentSplitCount = this.tickValues.size() - 1;
                if (splitCount == null || splitCount == 0) {
                    double unitStep =  unitSize / range.getLength();
                    splitCount = (int) (1 / unitStep);
                }
                if (parentSplitCount != splitCount && parentSplitCount % splitCount == 0) {
                    step = parentSplitCount / splitCount;
                }
                if (step == null && unitSize != null) {
                    double unitStep =  unitSize / range.getLength();
                    double parentStep = 1.0 / (this.tickValues.size() - 1);
                    if (unitStep != parentStep && unitStep % parentStep == 0) {
                        step = (int)(unitStep / parentStep);
                    }
                }
                if (step != null) {
                    TbNumberAxisTicks ticks = new TbNumberAxisTicks();
                    for (int i = 0; i < this.tickValues.size(); i += step) {
                        ticks.addTick(this.tickValues.get(i));
                    }
                    if ((this.tickValues.size() - 1) % step != 0) {
                        ticks.addTick(this.tickValues.get(this.tickValues.size() - 1));
                    }
                    return ticks;
                } else {
                    return this;
                }
            }
        }
    }
}
