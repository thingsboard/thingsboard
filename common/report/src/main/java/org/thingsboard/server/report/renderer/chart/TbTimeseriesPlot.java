// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.RendererUtils;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItemSource;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TbTimeseriesPlot extends XYPlot implements TbLegendItemSource {

    private TbVisualMap visualMap;

    public TbTimeseriesPlot() {
        super();
    }

    public void setVisualMap(TbVisualMap visualMap) {
        this.visualMap = visualMap;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
                     PlotState parentState, PlotRenderingInfo info) {

        this.updateInsets(g2, area);

        super.draw(g2, area, anchor, parentState, info);
    }

    @Override
    public boolean render(Graphics2D g2, Rectangle2D dataArea, int index,
                          PlotRenderingInfo info, CrosshairState crosshairState) {

        boolean foundData = false;
        XYDataset dataset = getDataset(index);
        if (!DatasetUtils.isEmptyOrNull(dataset)) {
            foundData = true;
            ValueAxis xAxis = getDomainAxisForDataset(index);
            ValueAxis yAxis = getRangeAxisForDataset(index);
            if (xAxis == null || yAxis == null) {
                return foundData;  // can't render anything without axes
            }
            XYItemRenderer renderer = getRenderer(index);
            if (renderer == null) {
                renderer = getRenderer();
                if (renderer == null) { // no default renderer available
                    return foundData;
                }
            }

            XYItemRendererState state = renderer.initialise(g2, dataArea, this,
                    dataset, info);

            if (this.visualMap != null && !this.visualMap.isEmpty() && renderer instanceof TbVisualMapRenderer visualMapRenderer) {
                int passCount = renderer.getPassCount();
                for (int pass = 0; pass < passCount; pass++) {
                    if (pass == visualMapRenderer.getAreaPass()) {
                        Shape savedClip = g2.getClip();
                        try {
                            List<TbVisualMapArea> visualAreas = this.visualMap.calculateAreas(yAxis, dataArea);
                            for (TbVisualMapArea visualArea : visualAreas) {
                                g2.setClip(visualArea.getArea());
                                visualMapRenderer.setVisualMap(null);
                                visualMapRenderer.setCurrentVisualMapPaint(visualArea.getPaint());
                                visualMapRenderer.setCurrentVisualMapFillPaint(visualArea.getFillPaint());
                                this.drawSeries(g2, dataArea, xAxis, yAxis, renderer, state, dataset, pass, pass, info, crosshairState);
                            }
                        } finally {
                            g2.setClip(savedClip);
                        }
                    } else {
                        visualMapRenderer.setCurrentVisualMapPaint(null);
                        visualMapRenderer.setCurrentVisualMapFillPaint(null);
                        if (pass == visualMapRenderer.getItemPass()) {
                            visualMapRenderer.setVisualMap(this.visualMap);
                        } else {
                            visualMapRenderer.setVisualMap(null);
                        }
                        this.drawSeries(g2, dataArea, xAxis, yAxis, renderer, state, dataset, pass, pass, info, crosshairState);
                    }
                }
            } else {
                int passCount = renderer.getPassCount();
                int startPass = 0;
                int endPass = passCount - 1;
                this.drawSeries(g2, dataArea, xAxis, yAxis, renderer, state, dataset, startPass, endPass, info, crosshairState);
            }

        }
        return foundData;
    }

    private void drawSeries(Graphics2D g2, Rectangle2D dataArea,
                            ValueAxis xAxis, ValueAxis yAxis,
                            XYItemRenderer renderer, XYItemRendererState state,
                            XYDataset dataset, int startPass, int endPass,
                            PlotRenderingInfo info, CrosshairState crosshairState) {
        SeriesRenderingOrder seriesOrder = getSeriesRenderingOrder();
        int passCount = renderer.getPassCount();
        if (seriesOrder == SeriesRenderingOrder.REVERSE) {
            //render series in reverse order
            for (int pass = startPass; pass <= endPass; pass++) {
                int seriesCount = dataset.getSeriesCount();
                for (int series = seriesCount - 1; series >= 0; series--) {
                    int firstItem = 0;
                    int lastItem = dataset.getItemCount(series) - 1;
                    if (lastItem == -1) {
                        continue;
                    }
                    if (state.getProcessVisibleItemsOnly()) {
                        int[] itemBounds = RendererUtils.findLiveItems(
                                dataset, series, xAxis.getLowerBound(),
                                xAxis.getUpperBound());
                        firstItem = Math.max(itemBounds[0] - 1, 0);
                        lastItem = Math.min(itemBounds[1] + 1, lastItem);
                    }
                    state.startSeriesPass(dataset, series, firstItem,
                            lastItem, pass, passCount);
                    for (int item = firstItem; item <= lastItem; item++) {
                        renderer.drawItem(g2, state, dataArea, info,
                                this, xAxis, yAxis, dataset, series, item,
                                crosshairState, pass);
                    }
                    state.endSeriesPass(dataset, series, firstItem,
                            lastItem, pass, passCount);
                }
            }
        }
        else {
            //render series in forward order
            for (int pass = startPass; pass <= endPass; pass++) {
                int seriesCount = dataset.getSeriesCount();
                for (int series = 0; series < seriesCount; series++) {
                    int firstItem = 0;
                    int lastItem = dataset.getItemCount(series) - 1;
                    if (state.getProcessVisibleItemsOnly()) {
                        int[] itemBounds = RendererUtils.findLiveItems(
                                dataset, series, xAxis.getLowerBound(),
                                xAxis.getUpperBound());
                        firstItem = Math.max(itemBounds[0] - 1, 0);
                        lastItem = Math.min(itemBounds[1] + 1, lastItem);
                    }
                    state.startSeriesPass(dataset, series, firstItem,
                            lastItem, pass, passCount);
                    for (int item = firstItem; item <= lastItem; item++) {
                        renderer.drawItem(g2, state, dataArea, info,
                                this, xAxis, yAxis, dataset, series, item,
                                crosshairState, pass);
                    }
                    state.endSeriesPass(dataset, series, firstItem,
                            lastItem, pass, passCount);
                }
            }
        }
    }

    private void updateInsets(Graphics2D g2, Rectangle2D area) {

        Rectangle2D measureArea = (Rectangle2D ) area.clone();
        RectangleInsets insets = getInsets();
        insets.trim(measureArea);
        AxisSpace space = calculateAxisSpace(g2, measureArea);
        Rectangle2D dataArea = space.shrink(measureArea, null);
        this.getAxisOffset().trim(dataArea);
        dataArea = integerise(dataArea);

        double dataAreaLeft = dataArea.getMinX();
        double dataAreaRight = area.getMaxX() - dataArea.getMaxX();

        boolean hasTopLabels = this.hasTopLabels();

        List<TbThresholdMarker> thresholdMarkers = this.computeVisibleThresholdMarkers();
        double[] offset = new double[]{0,0};
        for (TbThresholdMarker marker : thresholdMarkers) {
            double[] markerOffset = marker.measureOffset(g2);
            offset[0] = Math.max(offset[0], markerOffset[0]);
            offset[1] = Math.max(offset[1], markerOffset[1]);
            if (marker.getLabel() != null && !marker.getLabelPosition().name().endsWith("Bottom")) {
                hasTopLabels = true;
            }
        }

        offset[0] -= dataAreaLeft;
        offset[1] -= dataAreaRight;

        double minTopOffset = hasTopLabels ? 25 : 5;

        double topOffset = Math.max(insets.getTop(), minTopOffset);

        if (offset[0] > 0 || offset[1] > 0 || topOffset > insets.getTop()) {
            double leftOffset = offset[0] > 0 ? offset[0] : 0;
            double rightOffset = offset[1] > 0 ? offset[1] : 0;
            leftOffset += insets.getLeft();
            rightOffset += insets.getRight();
            setInsets(new RectangleInsets(topOffset, leftOffset, insets.getBottom(), rightOffset));
        }
    }

    private boolean hasTopLabels() {
        for (XYDataset dataset : this.getDatasets().values()) {
            if (dataset == null) {
                continue;
            }
            int datasetIndex = indexOf(dataset);
            XYItemRenderer renderer = this.getRenderer(datasetIndex);
            for (int series = 0; series < dataset.getSeriesCount(); series++) {
                if (renderer.isSeriesItemLabelsVisible(series)) {
                    ItemLabelPosition position = renderer.getSeriesPositiveItemLabelPosition(series);
                    if (position.getItemLabelAnchor().equals(ItemLabelAnchor.OUTSIDE12)) {
                        return true;
                    }
                    position = renderer.getSeriesNegativeItemLabelPosition(series);
                    if (position.getItemLabelAnchor().equals(ItemLabelAnchor.OUTSIDE12)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<TbThresholdMarker> computeVisibleThresholdMarkers() {
        List<TbThresholdMarker> thresholdMarkers = new ArrayList<>();
        for (XYDataset dataset : this.getDatasets().values()) {
            int datasetIndex = indexOf(dataset);
            Collection<Marker> markers = this.getRangeMarkers(datasetIndex, Layer.FOREGROUND);
            ValueAxis axis = getRangeAxisForDataset(datasetIndex);
            if (markers != null && axis != null) {
                Range range = axis.getRange();
                markers.forEach(marker -> {
                    if (marker instanceof TbThresholdMarker thresholdMarker) {
                        double value = thresholdMarker.getValue();
                        if (range.contains(value)) {
                            thresholdMarkers.add(thresholdMarker);
                        }
                    }
                });
            }
        }
        return thresholdMarkers;
    }

    private Rectangle integerise(Rectangle2D rect) {
        int x0 = (int) Math.ceil(rect.getMinX());
        int y0 = (int) Math.ceil(rect.getMinY());
        int x1 = (int) Math.floor(rect.getMaxX());
        int y1 = (int) Math.floor(rect.getMaxY());
        return new Rectangle(x0, y0, (x1 - x0), (y1 - y0));
    }

    @Override
    public List<TbLegendItem> getTbLegendItems(TbLegendValuesRequest request) {
        List<TbLegendItem> result = new ArrayList<>();
        for (XYDataset dataset : this.getDatasets().values()) {
            if (dataset == null) {
                continue;
            }
            int datasetIndex = indexOf(dataset);
            XYItemRenderer renderer = getRenderer(datasetIndex);
            if (renderer == null) {
                renderer = getRenderer(0);
            }
            if (renderer instanceof TbItemRenderer itemRenderer) {
                int seriesCount = dataset.getSeriesCount();
                for (int i = 0; i < seriesCount; i++) {
                    if (renderer.isSeriesVisible(i)
                            && renderer.isSeriesVisibleInLegend(i)) {
                        TbLegendItem item = itemRenderer.getTbLegendItem(request, datasetIndex, i);
                        if (item != null) {
                            result.add(item);
                        }
                    }
                }
            }
        }
        return result;
    }
}
