// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util.itext;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.SVGRenderingHints;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.DefaultParserProvider;
import com.github.weisj.jsvg.parser.DomProcessor;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.ParserProvider;
import com.github.weisj.jsvg.parser.SVGLoader;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class PdfSvgDocument {

    public static PdfSvgDocument fromSvgString(String svgString) {
        return fromSvgBytes(svgString.getBytes(StandardCharsets.UTF_8));
    }

    public static PdfSvgDocument fromSvgBytes(byte[] svgBytes) {
        SVGLoader loader = new SVGLoader();
        AtomicReference<ViewBox> viewBoxRef = new AtomicReference<>();
        ParserProvider parserProvider = new DefaultParserProvider() {
            public DomProcessor createPreProcessor() {
                return root -> {
                    viewBoxRef.set(root.attributeNode().getViewBox());
                };
            }
        };

        SVGDocument document = loader.load(new ByteArrayInputStream(svgBytes), null, LoaderContext.builder()
                .parserProvider(parserProvider)
                .build());
        if (document != null) {
            return new PdfSvgDocument(document, viewBoxRef.get());
        }
        return null;
    }

    private final SVGDocument _document;
    private final ViewBox _viewBox;

    public PdfSvgDocument(SVGDocument document, ViewBox viewBox) {
        this._document = document;
        this._viewBox = viewBox;
    }

    public FloatSize size() {
        return this._document.size();
    }

    public byte[] render(float targetWidth, float targetHeight, int usablePageWidthPx) throws Exception {
        ViewBox targetViewBox;
        if (_viewBox != null) {
            targetViewBox = new ViewBox(_viewBox.x, _viewBox.y, targetWidth, targetHeight);
        } else {
            targetViewBox = new ViewBox(0,0, size().width , size().height);
        }
        if (targetViewBox.width != usablePageWidthPx) {
            float scale = usablePageWidthPx / targetViewBox.width;
            targetViewBox.width = usablePageWidthPx;
            targetViewBox.height = targetViewBox.height * scale;
            targetViewBox.x = targetViewBox.x * scale;
            targetViewBox.y = targetViewBox.y * scale;
        }
        BufferedImage image = new BufferedImage((int)targetViewBox.width, (int)targetViewBox.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING, SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_ON);
        graphics.setRenderingHint(SVGRenderingHints.KEY_SOFT_CLIPPING, SVGRenderingHints.VALUE_SOFT_CLIPPING_ON);
        _document.render((Component)null,graphics, targetViewBox);
        graphics.dispose();
        return toCompressedPngData(image);
    }

    private static byte[] toCompressedPngData(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        ImageWriter writer = ImageIO.getImageWriters(type, "png").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.0f);
        }
        var output = ImageIO.createImageOutputStream(out);
        writer.setOutput(output);
        try {
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
            output.flush();
        }
        return out.toByteArray();
    }

}