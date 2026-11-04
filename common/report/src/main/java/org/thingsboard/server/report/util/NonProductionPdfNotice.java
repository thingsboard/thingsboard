// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import org.thingsboard.server.common.data.DataConstants;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Applies the {@link DataConstants#NON_PRODUCTION_NOTICE} to an already-rendered PDF report. The raster
 * counterpart is {@link NonProductionImageNotice}: a PDF page and a bitmap are defeated in different ways, so
 * they are marked in different ways.
 */
public class NonProductionPdfNotice {

    private static final float NOTICE_FONT_SIZE = 7f;
    // Distance from the page's physical bottom edge to the text baseline. No legible stamp fits between the
    // ~18pt non-printable margin of many printers and the 20pt default report content margin, so 14pt is a
    // compromise: the stamp sits inside the non-printable band, and its rectangle stops near the content margin.
    private static final float NOTICE_BASELINE_OFFSET = 14f;
    private static final float NOTICE_HORIZONTAL_PADDING = 4f;
    private static final float NOTICE_VERTICAL_PADDING = 1.5f;
    // Fixed pairing, whatever page background a tenant configures: their hardcoded contrast is the only thing
    // that determines whether the notice is legible.
    private static final Color NOTICE_TEXT_COLOR = new Color(60, 60, 60);
    private static final Color NOTICE_BACKGROUND_COLOR = new Color(224, 224, 224);

    /**
     * Applies the non-production notice to an already-rendered PDF when {@code nonProduction} is true, and
     * returns the original bytes untouched otherwise. Two markings are applied together:
     * <ul>
     *     <li>the "Subject" metadata field is set to the notice text, overwriting any existing Subject;</li>
     *     <li>the notice is also drawn as a small footer stamp on every page, because a report that is printed,
     *     screenshotted or pasted into a document shows no metadata, and metadata is trivially stripped.</li>
     * </ul>
     * The stamp is an opaque rectangle with contrasting text on top, painted last on the stamped over-content
     * ({@link PdfStamper#getOverContent}): a tenant-editable {@code PdfReportTemplateConfig#pageBackground}
     * could otherwise be set to match plain text and hide it. For the same reason it must not be moved into the
     * report template's own configurable header/footer mechanism.
     * <p>
     * This is the only place the PDF marking is decided - anything producing a PDF calls it.
     */
    public static byte[] addNonProductionNotice(byte[] pdfBytes, boolean nonProduction) throws IOException {
        if (!nonProduction) {
            return pdfBytes;
        }
        ByteArrayOutputStream stampedPdf = new ByteArrayOutputStream();
        try (PdfReader reader = new PdfReader(pdfBytes);
             PdfStamper stamper = new PdfStamper(reader, stampedPdf)) {
            stamper.setInfoDictionary(Map.of("Subject", DataConstants.NON_PRODUCTION_NOTICE));

            Font noticeFont = new Font(Font.HELVETICA, NOTICE_FONT_SIZE, Font.NORMAL, NOTICE_TEXT_COLOR);
            BaseFont baseFont = noticeFont.getCalculatedBaseFont(false);
            Phrase noticePhrase = new Phrase(DataConstants.NON_PRODUCTION_NOTICE, noticeFont);

            float textWidth = baseFont.getWidthPoint(DataConstants.NON_PRODUCTION_NOTICE, NOTICE_FONT_SIZE);
            float ascent = baseFont.getAscentPoint(DataConstants.NON_PRODUCTION_NOTICE, NOTICE_FONT_SIZE);
            float descent = baseFont.getDescentPoint(DataConstants.NON_PRODUCTION_NOTICE, NOTICE_FONT_SIZE);

            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                // getPageSizeWithRotation, not getPageSize: PdfStamperImp compensates over-content for a page's
                // /Rotate, so these coordinates must be in the same rotated frame. It reads the /MediaBox and
                // not any /CropBox, so a page with a tighter CropBox would put the stamp outside the visible
                // area - this renderer never sets one.
                Rectangle pageSize = reader.getPageSizeWithRotation(page);
                float centerX = (pageSize.getLeft() + pageSize.getRight()) / 2;
                float baselineY = pageSize.getBottom() + NOTICE_BASELINE_OFFSET;

                PdfContentByte overContent = stamper.getOverContent(page);

                overContent.saveState();
                overContent.setColorFill(NOTICE_BACKGROUND_COLOR);
                overContent.rectangle(centerX - textWidth / 2 - NOTICE_HORIZONTAL_PADDING,
                        baselineY + descent - NOTICE_VERTICAL_PADDING,
                        textWidth + NOTICE_HORIZONTAL_PADDING * 2,
                        ascent - descent + NOTICE_VERTICAL_PADDING * 2);
                overContent.fill();
                overContent.restoreState();

                ColumnText.showTextAligned(overContent, Element.ALIGN_CENTER, noticePhrase, centerX, baselineY, 0);
            }
        }
        return stampedPdf.toByteArray();
    }

}
