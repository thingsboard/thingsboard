// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.graphics;

import org.thingsboard.server.report.renderer.chart.font.TbCompositeFont;
import org.thingsboard.server.report.renderer.chart.font.TbCompositeFontMetrics;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

public class TbGraphics2D extends Graphics2D {

    private final Graphics2D delegate;

    public TbGraphics2D(Graphics2D delegate) {
        this.delegate = delegate;
    }

    public Graphics2D getDelegate() {
        return delegate;
    }

    @Override
    public void drawString(String str, int x, int y) {
        this.drawString(str, (float)x, (float)y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        Font font = this.delegate.getFont();
        if (font instanceof TbCompositeFont compositeFont) {
            compositeFont.drawStringWithFallback(this, str, x, y);
        } else {
            this.delegate.drawString(str, x, y);
        }
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        this.delegate.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        this.delegate.drawString(iterator, x, y);
    }

    @Override
    public void draw(Shape s) {
        this.delegate.draw(s);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return this.delegate.drawImage(img, xform, obs);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        this.delegate.drawImage(img, op, x, y);
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        this.delegate.drawRenderedImage(img, xform);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        this.delegate.drawRenderableImage(img, xform);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return this.delegate.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return this.delegate.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return this.delegate.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return this.delegate.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return this.delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return this.delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    @Override
    public void dispose() {
        this.delegate.dispose();
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        this.delegate.drawGlyphVector(g, x, y);
    }

    @Override
    public void fill(Shape s) {
        this.delegate.fill(s);
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return this.delegate.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return this.delegate.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp) {
        this.delegate.setComposite(comp);
    }

    @Override
    public void setPaint(Paint paint) {
        this.delegate.setPaint(paint);
    }

    @Override
    public void setStroke(Stroke s) {
        this.delegate.setStroke(s);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        this.delegate.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return this.delegate.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        this.delegate.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        this.delegate.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return this.delegate.getRenderingHints();
    }

    @Override
    public Graphics create() {
        return new TbGraphics2D((Graphics2D) this.delegate.create());
    }

    @Override
    public void translate(int x, int y) {
        this.delegate.translate(x, y);
    }

    @Override
    public Color getColor() {
        return this.delegate.getColor();
    }

    @Override
    public void setColor(Color c) {
        this.delegate.setColor(c);
    }

    @Override
    public void setPaintMode() {
        this.delegate.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        this.delegate.setXORMode(c1);
    }

    @Override
    public Font getFont() {
        return this.delegate.getFont();
    }

    @Override
    public void setFont(Font font) {
        this.delegate.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        FontMetrics fm = this.delegate.getFontMetrics(f);
        if (f instanceof TbCompositeFont compositeFont) {
            return new TbCompositeFontMetrics(fm, compositeFont);
        }
        return fm;
    }

    @Override
    public Rectangle getClipBounds() {
        return this.delegate.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        this.delegate.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        this.delegate.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        return this.delegate.getClip();
    }

    @Override
    public void setClip(Shape clip) {
        this.delegate.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        this.delegate.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        this.delegate.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        this.delegate.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        this.delegate.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        this.delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        this.delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        this.delegate.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        this.delegate.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        this.delegate.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        this.delegate.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        this.delegate.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        this.delegate.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        this.delegate.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void translate(double tx, double ty) {
        this.delegate.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        this.delegate.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        this.delegate.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        this.delegate.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        this.delegate.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform Tx) {
        this.delegate.transform(Tx);
    }

    @Override
    public void setTransform(AffineTransform Tx) {
        this.delegate.setTransform(Tx);
    }

    @Override
    public AffineTransform getTransform() {
        return this.delegate.getTransform();
    }

    @Override
    public Paint getPaint() {
        return this.delegate.getPaint();
    }

    @Override
    public Composite getComposite() {
        return this.delegate.getComposite();
    }

    @Override
    public void setBackground(Color color) {
        this.delegate.setBackground(color);
    }

    @Override
    public Color getBackground() {
        return this.delegate.getBackground();
    }

    @Override
    public Stroke getStroke() {
        return this.delegate.getStroke();
    }

    @Override
    public void clip(Shape s) {
        this.delegate.clip(s);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return this.delegate.getFontRenderContext();
    }
}
