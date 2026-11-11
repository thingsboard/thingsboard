// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util.itext;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.sheet.FontFaceRule;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.FontDescription;
import org.xhtmlrenderer.pdf.ITextFSFont;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.util.IOUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.lowagie.text.pdf.BaseFont.EMBEDDED;
import static com.lowagie.text.pdf.BaseFont.IDENTITY_H;
import static org.xhtmlrenderer.pdf.TrueTypeUtil.extractDescription;

@Slf4j
public class PdfReportFontResolver extends ITextFontResolver {

    private static final Map<String, PdfReportFontFamily> families = new HashMap<>();

    private final Map<String, PdfReportFontFamily> _fontFamilies = new HashMap<>();
    private final Map<String, FontDescription> _fontCache = new ConcurrentHashMap<>();

    public PdfReportFontResolver() {
    }

    @Override
    public FSFont resolveFont(@NotNull SharedContext renderingContext, FontSpecification spec) {
        return resolveFont(spec.families, spec.size, spec.fontWeight, spec.fontStyle);
    }

    @Override
    public void flushCache() {
    }

    @Override
    public void flushFontFaceFonts() {
    }

    @Override
    public void importFontFaces(List<FontFaceRule> fontFaces, UserAgentCallback userAgentCallback) {
    }

    private FSFont resolveFont(String [] families, float size, IdentValue weight, IdentValue style) {
        if (!(style == IdentValue.NORMAL || style == IdentValue.OBLIQUE
                || style == IdentValue.ITALIC)) {
            style = IdentValue.NORMAL;
        }
        if (families != null) {
            for (String family : families) {
                FSFont font = resolveFont(family, size, weight, style);
                if (font != null) {
                    log.debug("Resolved font {}:{}:{} -> {}", family, weight, style, font);
                    return font;
                }
            }
        }

        log.debug("Could not resolve font {}:{}:{} - fallback to Roboto", Arrays.toString(families), weight, style);
        return resolveFont("Roboto", size, weight, style);
    }

    private FSFont resolveFont(String fontFamily, float size, IdentValue weight, IdentValue style) {
        String normalizedFontFamily = stripQuotes(fontFamily);
        String cacheKey = String.format("%s-%s-%s", normalizedFontFamily, weight, style);
        FontDescription result = _fontCache.get(cacheKey);
        if (result != null) {
            log.debug("Resolved font {}:{}:{} -> {}", fontFamily, weight, style, result);
            return new ITextFSFont(result, size);
        }
        PdfReportFontFamily family = getPdfReportFonts().get(normalizedFontFamily);
        if (family != null) {
            result = family.match(ITextFontResolver.convertWeightToInt(weight), style);
            if (result != null) {
                _fontCache.put(cacheKey, result);
                return new ITextFSFont(result, size);
            }
        }
        return null;
    }

    private Map<String, PdfReportFontFamily> getPdfReportFonts() {
        if (_fontFamilies.isEmpty()) {
            synchronized (_fontFamilies) {
                if (_fontFamilies.isEmpty()) {
                    _fontFamilies.putAll(loadPdfReportFonts());
                }
            }
        }
        return _fontFamilies;
    }

    private Map<String, PdfReportFontFamily> loadPdfReportFonts() {
        if (families.isEmpty()) {
            synchronized (families) {
                this.addRoboto();
                this.addNotoSans();
                this.addMonospace();
                this.addSansSerif();
                this.addSerif();
                this.addNotoSansArabic();
            }
        }
        return families;
    }

    private void addRoboto() {
        PdfReportFontFamily roboto = new PdfReportFontFamily("Roboto");
        loadFont(roboto, "/fonts/roboto/Roboto-Regular.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.NORMAL);
        loadFont(roboto, "/fonts/roboto/Roboto-Italic.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.ITALIC);
        loadFont(roboto, "/fonts/roboto/Roboto-Medium.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        loadFont(roboto, "/fonts/roboto/Roboto-MediumItalic.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        loadFont(roboto, "/fonts/roboto/Roboto-Bold.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.NORMAL);
        loadFont(roboto, "/fonts/roboto/Roboto-BoldItalic.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("Roboto", roboto);
    }

    private void addNotoSans() {
        PdfReportFontFamily notoSans = new PdfReportFontFamily("noto-sans");
        loadFont(notoSans, "/fonts/cjk/NotoSansSC-Regular.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.NORMAL);
        loadFont(notoSans, "/fonts/cjk/NotoSans-Italic.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.ITALIC);
        loadFont(notoSans, "/fonts/cjk/NotoSansSC-Medium.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        loadFont(notoSans, "/fonts/cjk/NotoSans-MediumItalic.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        loadFont(notoSans, "/fonts/cjk/NotoSansSC-Bold.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.NORMAL);
        loadFont(notoSans, "/fonts/cjk/NotoSans-BoldItalic.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("noto-sans", notoSans);
    }

    private void addMonospace() {
        PdfReportFontFamily monospace = new PdfReportFontFamily("monospace");
        loadFont(monospace, "/fonts/monospace/DejaVuSansMono.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.NORMAL);
        loadFont(monospace, "/fonts/monospace/DejaVuSansMono-Oblique.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.ITALIC);
        loadFont(monospace, "/fonts/monospace/DejaVuSansMono.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        loadFont(monospace, "/fonts/monospace/DejaVuSansMono-Oblique.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        loadFont(monospace, "/fonts/monospace/DejaVuSansMono-Bold.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.NORMAL);
        loadFont(monospace, "/fonts/monospace/DejaVuSansMono-BoldOblique.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("monospace", monospace);
    }

    private void addSansSerif() {
        PdfReportFontFamily sansSerif = new PdfReportFontFamily("sans-serif");
        loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Regular.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.NORMAL);
        loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Italic.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.ITALIC);
        loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Regular.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Italic.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Bold.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.NORMAL);
        loadFont(sansSerif, "/fonts/sansserif/LiberationSans-BoldItalic.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("sans-serif", sansSerif);
    }

    private void addSerif() {
        PdfReportFontFamily serif = new PdfReportFontFamily("serif");
        loadFont(serif, "/fonts/serif/LiberationSerif-Regular.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.NORMAL);
        loadFont(serif, "/fonts/serif/LiberationSerif-Italic.ttf", IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.ITALIC);
        loadFont(serif, "/fonts/serif/LiberationSerif-Regular.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        loadFont(serif, "/fonts/serif/LiberationSerif-Italic.ttf", IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        loadFont(serif, "/fonts/serif/LiberationSerif-Bold.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.NORMAL);
        loadFont(serif, "/fonts/serif/LiberationSerif-BoldItalic.ttf", IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("serif", serif);
    }

    private void addNotoSansArabic() {
        PdfReportFontFamily arabic = new PdfReportFontFamily("noto-sans-arabic");
        loadFont(arabic, "/fonts/notoSansArabic/NotoSansArabic-Regular.ttf",
                IDENTITY_H, EMBEDDED, IdentValue.NORMAL, IdentValue.NORMAL);
        loadFont(arabic, "/fonts/notoSansArabic/NotoSansArabic-Medium.ttf",
                IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        loadFont(arabic, "/fonts/notoSansArabic/NotoSansArabic-Bold.ttf",
                IDENTITY_H, EMBEDDED, IdentValue.BOLD, IdentValue.NORMAL);
        loadFont(arabic, "/fonts/notoSansArabic/NotoSansArabic-SemiBold.ttf",
                IDENTITY_H, EMBEDDED, IdentValue.FONT_WEIGHT_500 /* or 600 if you support it */, IdentValue.NORMAL);
        families.put("noto-sans-arabic", arabic);
    }

    private void loadFont(PdfReportFontFamily family,
                          String uri, String encoding, boolean embedded,
                          IdentValue fontWeightOverride, IdentValue fontStyleOverride) {
        try {
            byte[] ttfAfm = this.getBinaryResource(uri);
            BaseFont font = BaseFont.createFont(uri, encoding, embedded, false, ttfAfm, null);
            family.addFontDescription(extractDescription(uri, ttfAfm, font, true, fontWeightOverride, fontStyleOverride));
        } catch (DocumentException | IOException e) {
            log.warn("Could not load font " + uri, e);
        }
    }

    private String stripQuotes(String text) {
        String result = text;
        if (result.startsWith("\"")) {
            result = result.substring(1);
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private byte[] getBinaryResource(String uri) throws IOException {
        URL url = PdfReportFontResolver.class.getResource(uri);
        if (url == null) {
            throw new IOException("Could not find resource " + uri);
        } else {
            try (java.io.InputStream is = url.openStream()) {
                return is == null ? null : IOUtil.readBytes(is);
            } catch (Exception e) {
                throw new IOException("Could not read resource " + uri, e);
            }
        }
    }

    public List<FontDescription> getFallBackFonts() {
        List<String> fallbackFamilies = Arrays.asList("Roboto", "noto-sans", "noto-sans-arabic", "sans-serif", "serif", "monospace");
        List<FontDescription> fallbacks = new java.util.ArrayList<>(fallbackFamilies.size());
        for (String family : fallbackFamilies) {
            PdfReportFontFamily f = getPdfReportFonts().get(family);
            if (f != null) {
                for (FontDescription fd : f.getFontDescriptions()) {
                    if (!fallbacks.contains(fd)) {
                        fallbacks.add(fd);
                    }
                }
            }
        }
        return fallbacks;
    }
}


