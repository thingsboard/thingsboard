// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThatCode;

public class TbNumberAxisTest {

    @Test
    void testChildAxisRendersWhenParentAxisIsHidden() {
        XYPlot plot = new XYPlot();
        plot.setDomainAxis(new NumberAxis());

        TbNumberAxis parentAxis = new TbNumberAxis(null, null);
        plot.setRangeAxis(0, parentAxis);
        TbNumberAxis childAxis = new TbNumberAxis(null, parentAxis);
        plot.setRangeAxis(1, childAxis);

        XYSeries series = new XYSeries("temperature");
        series.add(1, 20.5);
        series.add(2, 37.0);
        plot.setDataset(0, new XYSeriesCollection(series));
        plot.setRenderer(0, new XYLineAndShapeRenderer());
        plot.mapDatasetToRangeAxis(0, 1);

        // parent axis has no data mapped, so it is hidden like in TbTimeSeriesChart.postConfigureYAxes
        parentAxis.setVisible(false);

        JFreeChart chart = new JFreeChart(null, null, plot, false);
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            assertThatCode(() -> chart.draw(g2, new Rectangle2D.Double(0, 0, 400, 300))).doesNotThrowAnyException();
        } finally {
            g2.dispose();
        }
    }

}
