// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.ImageFormat;
import org.thingsboard.server.common.data.report.configuration.components.AbstractChartComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.chart.graphics.TbGraphics2D;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public abstract class ChartRenderer<C extends AbstractChartComponent> extends AbstractImageRenderer<C> {

    private int width;
    private int height;

    @Override
    protected String getImageUrl(C component, ComponentData reportDataSource) {

        if (ImageWidthType.CUSTOM == component.getWidthType()) {
            this.width = component.getCustomWidth() >= 1 ? component.getCustomWidth() : 100;
        } else {
            this.width  = this.layoutWidthPx;
        }
        this.height = component.getHeight() >= 1 ? component.getHeight() : 400;


        int pixelDensity = 4;
        int imageWidth = this.width * pixelDensity;
        int imageHeight = this.height * pixelDensity;

        BufferedImage highResImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = new TbGraphics2D(highResImage.createGraphics());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.scale(pixelDensity, pixelDensity);

        try {
            JFreeChart chart = createChart(g2, component, reportDataSource);
            chart.draw(g2, new Rectangle(0, 0, this.width, this.height));
        } finally {
            g2.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(highResImage, ImageFormat.PNG, baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] imageData = baos.toByteArray();

        return encodeImage(imageData);
    }

    protected int getWidth() {
        return width;
    }

    protected int getHeight() {
        return height;
    }

    protected abstract JFreeChart createChart(Graphics2D g2, C component, ComponentData reportDataSource);

    private String encodeImage(byte[] imageBytes) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64;
    }
}
