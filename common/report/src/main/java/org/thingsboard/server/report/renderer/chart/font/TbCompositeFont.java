// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.font;

import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.report.renderer.chart.graphics.TbGraphics2D;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

public class TbCompositeFont extends Font {

    private final List<Font> fallbackFonts;

    public TbCompositeFont(Font font, List<Font> fallbackFonts) {
        super(font);
        this.fallbackFonts = fallbackFonts;
    }

    @NotNull
    @Override
    public Font deriveFont(float size) {
        return new TbCompositeFont(super.deriveFont(size), fallbackFonts);
    }

    public void drawStringWithFallback(TbGraphics2D g2, String str, float x, float y) {
        List<Run> runs = detectFallbackFontRuns(str);
        if (runs.size() == 1 && runs.get(0).font == this) {
            g2.getDelegate().drawString(str, x, y);
        } else {
            AttributedString attributedString = this.runsToAttributedString(str, runs);
            g2.getDelegate().drawString(attributedString.getIterator(), x, y);
        }
    }

    @Override
    public Rectangle2D getStringBounds(char[] chars,
                                       int beginIndex, int limit,
                                       FontRenderContext frc) {
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException("beginIndex: " + beginIndex);
        }
        if (limit > chars.length) {
            throw new IndexOutOfBoundsException("limit: " + limit);
        }
        if (beginIndex > limit) {
            throw new IndexOutOfBoundsException("range length: " +
                    (limit - beginIndex));
        }
        String str = new String(chars, beginIndex, limit - beginIndex);
        List<Run> runs = detectFallbackFontRuns(str);
        if (runs.size() == 1 && runs.get(0).font == this) {
            return super.getStringBounds(str, beginIndex, limit, frc);
        } else {
            AttributedString attributedString = this.runsToAttributedString(str, runs);
            TextLayout tl = new TextLayout(attributedString.getIterator(), frc);
            return new Rectangle2D.Float(0, -tl.getAscent(), tl.getAdvance(),
                    tl.getAscent() + tl.getDescent() +
                            tl.getLeading());
        }
    }

    public int stringWidth(FontMetrics delegate, String str) {
        int len = str.length();
        char[] data = new char[len];
        str.getChars(0, len, data, 0);
        return charsWidth(delegate, data, 0, len);
    }

    public int charsWidth(FontMetrics delegate, char[] data, int off, int len) {
        if (len == 0) {
            return 0;
        }
        String str = new String(data, off, len);
        List<Run> runs = detectFallbackFontRuns(str);
        if (runs.size() == 1 && runs.get(0).font == this) {
            return delegate.charsWidth(data, off, len);
        } else {
            AttributedString attributedString = this.runsToAttributedString(str, runs);
            TextLayout tl = new TextLayout(attributedString.getIterator(), delegate.getFontRenderContext());
            return (int) (0.5 + tl.getAdvance());
        }
    }

    private AttributedString runsToAttributedString(String str, List<Run> runs) {
        AttributedString attributedString = new AttributedString(str);
        float size = this.getSize();
        for (Run run : runs) {
            attributedString.addAttribute(TextAttribute.FONT, run.font.deriveFont(size), run.beginIndex, run.endIndex);
        }
        return attributedString;
    }

    private Font detectFontForCodePoint(int codePoint) {
        if (this.canDisplay(codePoint)) {
            return this;
        }
        for (Font font : fallbackFonts) {
            if (font.canDisplay(codePoint)) {
                return font;
            }
        }
        return this;
    }

    private List<Run> detectFallbackFontRuns(String str) {
        List<Run> out = new ArrayList<>();
        if (str.isEmpty()) return out;
        int i, len = str.length();
        int cp = str.codePointAt(0);
        Font cur = this.detectFontForCodePoint(cp);
        int start = 0;
        for (i = Character.charCount(cp); i < len; ) {
            cp = str.codePointAt(i);
            Font f = this.detectFontForCodePoint(cp);
            if (f != cur) {
                out.add(new Run(start, i, cur));
                cur = f;
                start = i;
            }
            i += Character.charCount(cp);
        }
        out.add(new Run(start, str.length(), cur));
        return out;
    }

    private record Run(int beginIndex, int endIndex, Font font) {}

}
