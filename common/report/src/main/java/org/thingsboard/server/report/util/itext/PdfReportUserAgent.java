// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util.itext;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.report.util.ImageUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.pdf.PDFAsImage;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.util.ContentTypeDetectingInputStreamWrapper;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.XRLog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.thingsboard.server.report.util.ImageUtils.isEmptyImage;
import static org.thingsboard.server.report.util.ImageUtils.isInternalTbImage;
import static org.thingsboard.server.report.util.ImageUtils.isPublicTbImage;
import static org.thingsboard.server.report.util.ImageUtils.isTbImage;
import static org.xhtmlrenderer.util.ContentTypeDetectingInputStreamWrapper.detectContentType;
import static org.xhtmlrenderer.util.IOUtil.readBytes;
import static org.xhtmlrenderer.util.ImageUtil.isEmbeddedBase64Image;

public class PdfReportUserAgent extends ITextUserAgent {

    private final TbReportCtx _ctx;
    private final ReportDataService _dataService;
    private final ITextOutputDevice _outputDevice;
    private final int dotsPerPixel;
    private final int usablePageWidthPx;

    public PdfReportUserAgent(ReportDataService dataService, TbReportCtx ctx,
                              ITextOutputDevice outputDevice, int dotsPerPixel, int usablePageWidthPx) {
        super(outputDevice, dotsPerPixel);
        this._dataService = dataService;
        this._ctx = ctx;
        this._outputDevice = outputDevice;
        this.dotsPerPixel = dotsPerPixel;
        this.usablePageWidthPx = usablePageWidthPx;
    }

    @Override
    public String resolveURI(String uri) {
        return uri;
    }

    @Override
    public ImageResource getImageResource(String uriStr) {
        uriStr = StringUtils.removeStart(uriStr, DataConstants.TB_IMAGE_PREFIX);
        String unresolvedUri = uriStr;
        if (!isEmbeddedBase64Image(uriStr) && !isTbImage(uriStr) && !isEmptyImage(uriStr)) {
            uriStr = resolveURI(uriStr);
        }
        ImageResource resource = _imageCache.get(unresolvedUri);

        if (resource == null) {
            resource = loadImageResource(uriStr);
            _imageCache.put(unresolvedUri, resource);
        }
        if (resource != null) {
            FSImage image = resource.getImage();
            return new ImageResource(resource.getImageUri(), image);
        } else {
            return new ImageResource(uriStr, null);
        }
    }

    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        java.io.InputStream is = null;
        URL url = PdfReportUserAgent.class.getResource(uri);
        if (url == null) {
            if (uri.startsWith("/assets/")) {
                url = PdfReportUserAgent.class.getResource("/public" + uri);
            }
            if (url == null) {
                try {
                    URI parsedUri = new URI(uri);
                    String scheme = parsedUri.getScheme();
                    if (scheme != null && !scheme.equalsIgnoreCase("jar")) {
                        SsrfProtectionValidator.validateUri(parsedUri);
                    }
                } catch (URISyntaxException e) {
                    XRLog.exception("Invalid URI: " + uri, e);
                    throw new PdfReportImageException(uri, "Invalid URI syntax", null, e);
                } catch (RuntimeException e) {
                    XRLog.exception(e.getMessage());
                    throw new PdfReportImageException(uri, "URI is invalid", null, e);
                }
                return super.resolveAndOpenStream(uri);
            }
        }
        try {
            is = url.openStream();
        } catch (java.net.MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        } catch (java.io.FileNotFoundException e) {
            XRLog.exception("item at URI " + uri + " not found");
        } catch (java.io.IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return is;
    }

    private ImageResource loadImageResource(String uriStr) {
        String originalUri = uriStr;
        uriStr = isEmbeddedBase64Image(uriStr) ? "Base64 Data" : uriStr;
        try (InputStream is = resolveImageStream(originalUri)) {
            if (is != null) {
                try (ContentTypeDetectingInputStreamWrapper cis = detectContentType(is)) {
                    if (cis.isPdf()) {
                        URI uri = new URI(uriStr);
                        PdfReader reader = _outputDevice.getReader(uri);
                        Rectangle rect = reader.getPageSizeWithRotation(1);
                        float initialWidth = rect.getWidth() * _outputDevice.getDotsPerPoint();
                        float initialHeight = rect.getHeight() * _outputDevice.getDotsPerPoint();
                        PDFAsImage image = new PDFAsImage(uri, initialWidth, initialHeight);
                        return new ImageResource(uriStr, image);
                    } else if (cis.isSvg()) {
                        PdfSvgImage image = readSvg(uriStr, cis);
                        return new ImageResource(uriStr, image);
                    } else {
                        byte[] image = readBytes(cis);
                        ITextFSImage itextImage = new ITextFSImage(image, ImageUtils.getOriginalImageSize(image, this.dotsPerPixel), uriStr);
                        return new ImageResource(uriStr, itextImage);
                    }
                }
            }
        } catch (BadElementException | IOException | URISyntaxException e) {
            XRLog.exception("Can't read image file; unexpected problem for URI '" + uriStr + "'", e);
            return errorImageResource(uriStr, "Can't read image file", null, e);
        } catch (PdfReportImageException e) {
            return errorImageResource(e);
        }
        return errorLoadImageFromUriResource(uriStr, null);
    }

    private InputStream resolveImageStream(String uriStr) throws PdfReportImageException {
        if (isEmbeddedBase64Image(uriStr)) {
            try {
                byte[] buffer = ImageUtil.getEmbeddedBase64Image(uriStr);
                return new ByteArrayInputStream(buffer);
            } catch (BadElementException e) {
                XRLog.exception("Can't read XHTML embedded image.", e);
                throw errorLoadImageFromBase64Exception(e);
            }
        } else if (isTbImage(uriStr)) {
            try {
                byte[] imageData = null;
                if (isInternalTbImage(uriStr)) {
                    imageData = this.loadInternalTbImage(uriStr);
                } else if (isPublicTbImage(uriStr)) {
                    imageData = this.loadPublicTbImage(uriStr);
                }
                if (imageData != null) {
                    return new ByteArrayInputStream(imageData);
                }
            } catch (Exception e) {
                XRLog.exception("Can't read TB image.", e);
                throw errorLoadTbImageException(uriStr, e);
            }
            throw errorLoadTbImageException(uriStr, null);
        } else if (isEmptyImage(uriStr)) {
            throw errorEmptyImageException(uriStr);
        } else {
            return resolveAndOpenStream(uriStr);
        }
    }

    private PdfSvgImage readSvg(String uri, InputStream in) throws IOException, PdfReportImageException {
        byte[] svgBytes = readBytes(in);
        PdfSvgDocument svgDocument = PdfSvgDocument.fromSvgBytes(svgBytes);
        if (svgDocument == null) {
            throw new PdfReportImageException(uri, "Could not load image from SVG.", null, null);
        }
        return new PdfSvgImage(svgDocument, this.dotsPerPixel, this.usablePageWidthPx);
    }

    private PdfReportImageException errorLoadImageFromBase64Exception(Exception exception) {
        return new PdfReportImageException(null, "Failed to load image from base64 data", "Base64 Data", exception);
    }

    private PdfReportImageException errorLoadTbImageException(final String uri, Exception exception) {
        return new PdfReportImageException(uri, "Can't read TB image.", null, exception);
    }

    private PdfReportImageException errorEmptyImageException(final String uri) {
        return new PdfReportImageException(uri, "Image uri is empty.", "URI is empty", null);
    }

    private ImageResource errorLoadImageFromUriResource(final String uri, Exception exception) {
        return errorImageResource(uri, "Failed to load image.", null, exception);
    }

    private ImageResource errorImageResource(PdfReportImageException exception) {
        return errorImageResource(exception.uri, exception.getMessage(), exception.uriString, exception.getCause());
    }

    private ImageResource errorImageResource(final String uri, String errorMessage, String uriString, Throwable exception) {
        HashMap<String, Object> errorImageVariables = new HashMap<>();
        errorImageVariables.put("errorMessage", errorMessage);
        if (uriString == null) {
            uriString = uri;
        }
        errorImageVariables.put("uri", uriString);
        if (exception != null) {
            errorImageVariables.put("exception", exception.getMessage());
            errorImageVariables.put("uriPos", 11);
        } else {
            errorImageVariables.put("uriPos", 16);
        }
        String errorImageSvg = ThymeleafUtil.renderFromSvgTemplate("svg/error-image", errorImageVariables);
        PdfSvgDocument svgDocument = PdfSvgDocument.fromSvgString(errorImageSvg);
        PdfSvgImage image = new PdfSvgImage(svgDocument, this.dotsPerPixel, this.usablePageWidthPx);
        return new ImageResource(uri, image);
    }

    private byte[] loadInternalTbImage(final String uri) throws Exception {
        String imageType = null;
        if (uri.startsWith("/api/images/tenant/")) {
            imageType = "tenant";
        } else if (uri.startsWith("/api/images/system/")) {
            imageType = "system";
        }
        if (imageType != null) {
            var parts = uri.split("/");
            if (parts.length >= 5) {
                String key = parts[4];
                key = URLDecoder.decode(key, StandardCharsets.UTF_8);
                return this._dataService.downloadImage(imageType, key, this._ctx);
            }
        }
        return null;
    }

    private byte[] loadPublicTbImage(final String uri) throws Exception {
        var parts = uri.split("/");
        if (parts.length >= 5) {
            String publicKey = parts[4];
            return this._dataService.downloadPublicImage(publicKey, this._ctx);
        }
        return null;
    }

    static class PdfReportImageException extends RuntimeException {

        private final String uri;
        private final String uriString;

        PdfReportImageException(final String uri, String errorMessage, String uriString, Exception exception) {
            super(errorMessage, exception);
            this.uri = uri;
            this.uriString = uriString;
        }

    }

}
