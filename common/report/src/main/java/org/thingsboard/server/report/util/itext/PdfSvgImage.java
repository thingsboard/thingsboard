// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util.itext;

import com.google.errorprone.annotations.CheckReturnValue;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.Size;
import org.xhtmlrenderer.pdf.ITextFSImage;

public class PdfSvgImage extends ITextFSImage {

    private final PdfSvgDocument _svgDocument;
    private byte[] _image;
    private final float dotsPerPixel;
    private final int usablePageWidthPx;

    public PdfSvgImage(PdfSvgDocument svgDocument, float dotsPerPixel, int usablePageWidthPx) {
        this(svgDocument, dotsPerPixel, usablePageWidthPx, new Size((int)(svgDocument.size().width * dotsPerPixel), (int)(svgDocument.size().height * dotsPerPixel)));
    }

    public PdfSvgImage(PdfSvgDocument svgDocument, float dotsPerPixel, int usablePageWidthPx, Size size) {
        super(null, size, null);
        this._svgDocument = svgDocument;
        this.dotsPerPixel = dotsPerPixel;
        this.usablePageWidthPx = usablePageWidthPx;
    }

    @CheckReturnValue
    @Override
    public FSImage scale(int width, int height) {
        Size newSize = size.scale(width, height);
        if (size != newSize) {
           return new PdfSvgImage(_svgDocument, dotsPerPixel, usablePageWidthPx, newSize);
        }
        return this;
    }

    @Override
    public byte[] getImage() {
        if (_image == null) {
            try {
                _image = this._svgDocument.render((float) getWidth() / this.dotsPerPixel,
                        (float) getHeight() / this.dotsPerPixel, this.usablePageWidthPx);
            } catch (Exception e) {}
        }
        return _image;
    }
}
