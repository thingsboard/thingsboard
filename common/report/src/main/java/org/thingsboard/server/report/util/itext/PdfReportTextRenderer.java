// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util.itext;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
import com.lowagie.text.pdf.BaseFont;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.extend.FontContext;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.pdf.FontDescription;
import org.xhtmlrenderer.pdf.ITextFSFont;
import org.xhtmlrenderer.pdf.ITextFSFontMetrics;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextTextRenderer;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.render.JustificationInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfReportTextRenderer extends ITextTextRenderer {

    private static final float TEXT_MEASURING_DELTA = 0.01f;

    private final Map<Integer, Map<IdentValue, List<FontDescription>>> fallbacksMap = new HashMap<>();

    public PdfReportTextRenderer(List<FontDescription> fallbackFonts) {
        if (fallbackFonts != null) {
            for (FontDescription fontDescription : fallbackFonts) {
                Map<IdentValue, List<FontDescription>> fdByStyle =
                        fallbacksMap.computeIfAbsent(fontDescription.getWeight(), k -> new HashMap<>());
                fdByStyle.computeIfAbsent(fontDescription.getStyle(), k -> new ArrayList<>()).add(fontDescription);
            }
        }
    }

    @Override
    public FSFontMetrics getFSFontMetrics(FontContext context, FSFont font, String string) {
        FontDescription description = ((ITextFSFont) font).getFontDescription();
        BaseFont bf = description.getFont();
        float size = font.getSize2D();
        float strikethroughThickness = description.getYStrikeoutSize() != 0 ?
                description.getYStrikeoutSize() / 1000f * size :
                size / 12.0f;

        return new ITextFSFontMetrics(
                bf.getFontDescriptor(BaseFont.AWT_ASCENT, size) + bf.getFontDescriptor(BaseFont.AWT_LEADING, size),
                -bf.getFontDescriptor(BaseFont.AWT_DESCENT, size),
                -description.getYStrikeoutPosition() / 1000f * size,
                strikethroughThickness,
                -description.getUnderlinePosition() / 1000f * size,
                description.getUnderlineThickness() / 1000f * size
        );
    }

    @Override
    public void drawString(OutputDevice outputDevice, String s, float x, float y) {
        drawString(outputDevice, s, x, y, null);
    }

    @Override
    public void drawString(OutputDevice outputDevice, String s, float x, float y, JustificationInfo info) {
        ITextOutputDevice iod = (ITextOutputDevice) outputDevice;

        if (!(iod.getSharedContext().getFont(iod.getFontSpecification()) instanceof ITextFSFont curFont)) {
            iod.drawString(s, x, y, info);
            return;
        }

        String vis = shapeAndReorderLTRParagraph(s);

        FontDescription primary = curFont.getFontDescription();
        float size = curFont.getSize2D();

        List<FontDescription> candidates = this.prepareFontCandidates(primary);

        List<Run> runs = shapeRunsByBaseFont(vis, candidates, size);

        float cursor = x;
        for (Run r : runs) {
            ITextFSFont runFsFont = new ITextFSFont(r.fd, size);
            iod.setFont(runFsFont);

            iod.drawString(r.text, cursor, y, info);
            cursor += r.widthPt;
        }

        iod.setFont(curFont);
    }

    @Override
    public int getWidth(FontContext context, FSFont font, String string) {
        if (font instanceof ITextFSFont curFont) {
            FontDescription primary = curFont.getFontDescription();
            float size = curFont.getSize2D();
            List<FontDescription> candidates = this.prepareFontCandidates(primary);
            String vis = shapeAndReorderLTRParagraph(string);
            List<Run> runs = shapeRunsByBaseFont(vis, candidates, size);
            float result = 0;
            for (Run r : runs) {
                result += r.widthPt;
            }
            if (result - Math.floor(result) < TEXT_MEASURING_DELTA) {
                return (int)result;
            } else {
                return (int)Math.ceil(result);
            }
        } else {
            return super.getWidth(context, font, string);
        }
    }

    private List<FontDescription> prepareFontCandidates(FontDescription primary) {
        List<FontDescription> candidates = new ArrayList<>();
        candidates.add(primary);
        candidates.addAll(fallbacksMap.get(primary.getWeight()).get(primary.getStyle()));
        return candidates;
    }

    private static String shapeAndReorderLTRParagraph(String logical) {
        try {
            int shapeFlags =
                    ArabicShaping.LETTERS_SHAPE |
                            ArabicShaping.TASHKEEL_RESIZE |
                            ArabicShaping.TEXT_DIRECTION_LOGICAL;

            String shaped = new ArabicShaping(shapeFlags).shape(logical);
            Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_LEFT_TO_RIGHT);
            int opts = Bidi.DO_MIRRORING | Bidi.INSERT_LRM_FOR_NUMERIC;
            return bidi.writeReordered(opts);
        } catch (Exception e) {
            return logical;
        }
    }

    private static List<Run> shapeRunsByBaseFont(String s, List<FontDescription> candidates, float size) {
        List<Run> out = new ArrayList<>();
        if (s.isEmpty()) return out;

        int i = 0, len = s.length();
        int cp = s.codePointAt(0);
        FontDescription cur = pick(candidates, cp);
        int start = 0;
        for (i = Character.charCount(cp); i < len; ) {
            cp = s.codePointAt(i);
            FontDescription fd = pick(candidates, cp);
            if (fd != cur) {
                String slice = s.substring(start, i);
                out.add(run(slice, cur, size));
                cur = fd;
                start = i;
            }
            i += Character.charCount(cp);
        }
        out.add(run(s.substring(start), cur, size));
        return out;
    }

    private static FontDescription pick(List<FontDescription> candidates, int codePoint) {
        for (FontDescription fd : candidates) {
            if (fd.getFont().charExists(codePoint)) return fd;
        }
        return candidates.get(0);
    }

    private static Run run(String text, FontDescription fd, float size) {
        float w = fd.getFont().getWidthPointKerned(text, size);
        return new Run(text, fd, w);
    }

    private record Run(String text, FontDescription fd, float widthPt) {}
}
