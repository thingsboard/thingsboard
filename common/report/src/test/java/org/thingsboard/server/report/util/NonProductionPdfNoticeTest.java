// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DataConstants;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class NonProductionPdfNoticeTest {

    @Test
    void testAddNonProductionNoticeStampsSubjectMetadataAndVisibleFooterOnEveryPageWhenNonProduction() throws Exception {
        byte[] pdfBytes = createMinimalPdf();

        byte[] stampedPdfBytes = NonProductionPdfNotice.addNonProductionNotice(pdfBytes, true);

        try (PdfReader reader = new PdfReader(stampedPdfBytes)) {
            assertThat(reader.getInfo().get("Subject")).isEqualTo(DataConstants.NON_PRODUCTION_NOTICE);
            assertThat(reader.getNumberOfPages()).isEqualTo(2);

            // The footer text is what actually makes the notice visible on a printed/screenshotted page,
            // unlike the metadata field alone, so assert the text itself rather than only its metadata.
            // It cannot be grepped out of the raw page content stream: OpenPDF resolves Helvetica through
            // getCalculatedBaseFont and embeds it as a subsetted TrueType font addressed by two-byte glyph
            // ids (the string "Development" is written as the bytes 00 27 00 48 00 59 ..., each glyph id
            // being the character code less 0x1D), so the notice only reads back as text once it is decoded
            // through the font's ToUnicode map - which is exactly what a PDF viewer, a copy-paste out of the
            // rendered page, and PdfTextExtractor all do. Asserting the full string rather than an ASCII
            // prefix keeps the em dash covered. Checked on both pages, since "every page" otherwise rests on
            // reading the loop.
            PdfTextExtractor textExtractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                assertThat(textExtractor.getTextFromPage(page))
                        .as("notice text on page %d", page)
                        .contains(DataConstants.NON_PRODUCTION_NOTICE);

                assertOpaqueBackingRectangleIsFilledBeforeTheNoticeText(reader, page);
            }
        }
    }

    @Test
    void testAddNonProductionNoticeLeavesPdfUntouchedWhenNotNonProduction() throws Exception {
        byte[] pdfBytes = createMinimalPdf();

        byte[] result = NonProductionPdfNotice.addNonProductionNotice(pdfBytes, false);

        // calls the actual production conditional (not a copy of it) and asserts it's a true no-op
        assertThat(result).isEqualTo(pdfBytes);
        try (PdfReader reader = new PdfReader(result)) {
            assertThat(reader.getInfo().get("Subject")).isNotEqualTo(DataConstants.NON_PRODUCTION_NOTICE);
        }
    }

    /**
     * Pins the opaque backing rectangle itself, not just the notice text: the metadata and extracted-text
     * assertions would both also pass against a plain-text footer, which has no rectangle and could be hidden
     * by a matching pageBackground - the one defeat the rectangle exists to prevent.
     * <p>
     * {@code NonProductionPdfNotice#addNonProductionNotice} calls {@code rectangle(...)} then {@code fill()}
     * with nothing in between, which OpenPDF emits as the literal operator sequence "&lt;x&gt; &lt;y&gt;
     * &lt;w&gt; &lt;h&gt; re\nf\n" - a standalone "re" operator immediately followed (only whitespace between)
     * by a standalone "f" operator. That pairing is what would disappear if the rectangle were dropped, or the
     * fill call removed while the rectangle stayed, so it has to appear - and it has to appear before the
     * notice text, which is drawn after the rectangle is filled. The notice is the last thing drawn on the
     * page, so the show-text operator that paints it is the final "Tj" in the stream; requiring the fill to
     * precede it is the ordering check that would fail if the rectangle were painted over the text instead of
     * behind it. The glyph ids themselves are not greppable, but the operators around them are.
     * <p>
     * This is deliberately coupled to the operator bytes OpenPDF emits today, which is the cost of covering
     * the property at all: an OpenPDF upgrade that emits a combined path-paint sequence, or switches to "TJ",
     * fails here loudly and is cheap to re-pin, whereas dropping the assertion would leave the rectangle - the
     * whole reason the footer is not plain text - covered by nothing.
     */
    private static void assertOpaqueBackingRectangleIsFilledBeforeTheNoticeText(PdfReader reader, int page) throws Exception {
        String pageContent = new String(reader.getPageContent(page), StandardCharsets.ISO_8859_1);

        Matcher fillOperatorMatcher = Pattern.compile("re\\s+f\\b").matcher(pageContent);
        assertThat(fillOperatorMatcher.find()).as("rectangle fill operator sequence 're f' on page %d", page).isTrue();
        assertThat(fillOperatorMatcher.start())
                .as("rectangle filled before the notice text is drawn on page %d", page)
                .isLessThan(pageContent.lastIndexOf("Tj"));
    }

    private static byte[] createMinimalPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("test report content, page 1"));
        document.newPage();
        document.add(new Paragraph("test report content, page 2"));
        document.close();
        return out.toByteArray();
    }

}
