// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.font;

import org.jetbrains.annotations.NotNull;

import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;

public class TbCompositeFontMetrics extends FontMetrics {

    private final FontMetrics delegate;
    private final TbCompositeFont compositeFont;

    public TbCompositeFontMetrics(FontMetrics delegate, TbCompositeFont font) {
        super(font);
        this.delegate = delegate;
        this.compositeFont = font;
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return this.delegate.getFontRenderContext();
    }

    @Override
    public int charWidth(char ch) {
        return this.delegate.charWidth(ch);
    }

    @Override
    public int charWidth(int ch) {
        return this.delegate.charWidth(ch);
    }

    @Override
    public int stringWidth(@NotNull String str) {
        return this.compositeFont.stringWidth(delegate, str);
    }

    @Override
    public int charsWidth(char[] data, int off, int len) {
        return this.compositeFont.charsWidth(delegate, data, off, len);
    }

    @Override
    public int[] getWidths() {
        return this.delegate.getWidths();
    }

    @Override
    public int getMaxAdvance() {
        return this.delegate.getMaxAdvance();
    }

    @Override
    public int getAscent() {
        return this.delegate.getAscent();
    }

    @Override
    public int getDescent() {
        return this.delegate.getDescent();
    }

    @Override
    public int getLeading() {
        return this.delegate.getLeading();
    }

    @Override
    public int getHeight() {
        return this.delegate.getHeight();
    }
}
