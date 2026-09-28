// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import org.thingsboard.server.common.data.DataConstants;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Applies the {@link DataConstants#NON_PRODUCTION_NOTICE} to an already-rendered raster report - the JPEG and
 * PNG forms of a dashboard report. The PDF counterpart is
 * {@link NonProductionPdfNotice#addNonProductionNotice(byte[], boolean)}; anything generating a PDF must keep
 * calling that rather than growing a second implementation here.
 * <p>
 * The notice is tiled diagonally over the whole bitmap rather than stamped into a footer, because a footer strip
 * on an image is cropped away by dragging a selection box in any viewer. It is stroked in translucent light and
 * filled in translucent dark so it stays legible over both light and dark dashboards without hiding them.
 */
public class NonProductionImageNotice {

    /** ImageIO format names, exposed so callers map a content type to a format in one place. */
    public static final String PNG_IMAGE_FORMAT = "png";
    public static final String JPEG_IMAGE_FORMAT = "jpg";

    // Sized relative to the bitmap so the notice reads the same at any capture resolution, with a floor that
    // keeps a small capture legible. These and the two spacing multiples below match the web UI's thumbnail
    // capture, so both rasterised artifacts carry the same marking.
    private static final int MINIMUM_FONT_SIZE = 11;
    private static final int FONT_SIZE_TO_IMAGE_WIDTH_DIVISOR = 32;
    private static final float MINIMUM_STROKE_WIDTH = 2f;
    private static final float STROKE_WIDTH_TO_FONT_SIZE_DIVISOR = 4f;
    // Gap between tiles and between tile rows, in multiples of the font size: wide enough to read as a
    // watermark rather than a wall of text, and positive, so neighbouring tiles never overlap.
    private static final int HORIZONTAL_GAP_IN_FONT_SIZES = 3;
    private static final int VERTICAL_STEP_IN_FONT_SIZES = 5;

    private static final Color NOTICE_STROKE_COLOR = new Color(255, 255, 255, 191);
    private static final Color NOTICE_FILL_COLOR = new Color(0, 0, 0, 140);

    // Stated explicitly rather than taking the ImageIO default (0.75), which visibly softens the thin lines and
    // small labels of a dashboard capture on the re-encode that drawing the notice forces.
    private static final float JPEG_RE_ENCODE_QUALITY = 0.95f;

    // Upper bound on the raster this class will decode, checked from the image header before any pixels are
    // allocated: a mostly-flat dashboard compresses so well that a modest response can decode to a raster larger
    // than the heap. 100 megapixels is an order of magnitude above any plausible capture (4K is about 8).
    private static final long MAXIMUM_DECODED_PIXEL_COUNT = 100_000_000L;

    /**
     * Returns {@code imageBytes} with the notice tiled across them when {@code nonProduction} is true, and the
     * original bytes untouched otherwise.
     * <p>
     * Every failure path throws rather than returning the input, which would hand back a clean, shareable image
     * of a development deployment that the caller believes was marked.
     *
     * @param imageFormatName {@link #PNG_IMAGE_FORMAT} or {@link #JPEG_IMAGE_FORMAT}
     */
    public static byte[] addNonProductionNotice(byte[] imageBytes, String imageFormatName, boolean nonProduction) throws IOException {
        if (!nonProduction) {
            return imageBytes;
        }
        if (!PNG_IMAGE_FORMAT.equals(imageFormatName) && !JPEG_IMAGE_FORMAT.equals(imageFormatName)) {
            throw new IOException("Failed to apply the non-production notice: unsupported image format " + imageFormatName);
        }
        checkDecodedPixelCount(imageBytes);
        BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (decodedImage == null) {
            // ImageIO signals "no reader could decode this" by returning null rather than by throwing.
            throw new IOException("Failed to apply the non-production notice: the report image could not be decoded");
        }
        BufferedImage stampedImage = toDrawableImage(decodedImage, imageFormatName);
        Graphics2D graphics = stampedImage.createGraphics();
        try {
            drawTiledNotice(graphics, stampedImage.getWidth(), stampedImage.getHeight());
        } finally {
            graphics.dispose();
        }
        return encodeImage(stampedImage, imageFormatName);
    }

    /**
     * Rejects an image whose header declares more than {@link #MAXIMUM_DECODED_PIXEL_COUNT} pixels, so an
     * over-sized capture fails the download instead of exhausting the heap. An input no reader recognises is
     * left for the decode below to report.
     */
    private static void checkDecodedPixelCount(byte[] imageBytes) throws IOException {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (imageInput == null) {
                return;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                return;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput);
                long pixelCount = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixelCount > MAXIMUM_DECODED_PIXEL_COUNT) {
                    throw new IOException("Failed to apply the non-production notice: the report image is too large to mark, "
                            + pixelCount + " pixels exceeds the limit of " + MAXIMUM_DECODED_PIXEL_COUNT);
                }
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Returns an image that can be both drawn on with translucent colours and written back out in the target
     * format, copying the decoded one only when it cannot.
     * <p>
     * A palette-based or bilevel image has no room for the blended colours a translucent overlay produces, so
     * the notice could be quantised down to invisible. The JDK's JPEG writer cannot write an alpha channel, so
     * an image that has one is flattened onto white - a transparent region of a capture is background, and
     * dashboard chrome is far more often light than dark.
     */
    private static BufferedImage toDrawableImage(BufferedImage decodedImage, String imageFormatName) {
        boolean targetIsJpeg = JPEG_IMAGE_FORMAT.equals(imageFormatName);
        boolean hasAlpha = decodedImage.getColorModel().hasAlpha();
        int imageType = decodedImage.getType();
        boolean needsCopy = imageType == BufferedImage.TYPE_BYTE_INDEXED
                || imageType == BufferedImage.TYPE_BYTE_BINARY
                || imageType == BufferedImage.TYPE_CUSTOM
                || (targetIsJpeg && hasAlpha);
        if (!needsCopy) {
            return decodedImage;
        }
        BufferedImage copy = new BufferedImage(decodedImage.getWidth(), decodedImage.getHeight(),
                targetIsJpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            if (targetIsJpeg) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, copy.getWidth(), copy.getHeight());
            }
            graphics.drawImage(decodedImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static void drawTiledNotice(Graphics2D graphics, int imageWidth, int imageHeight) {
        int fontSize = Math.max(MINIMUM_FONT_SIZE, Math.round((float) imageWidth / FONT_SIZE_TO_IMAGE_WIDTH_DIVISOR));
        Font noticeFont = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setStroke(new BasicStroke(Math.max(MINIMUM_STROKE_WIDTH, fontSize / STROKE_WIDTH_TO_FONT_SIZE_DIVISOR),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Laid out once and then only translated per tile. Outlines are also what allows the same shape to be
        // stroked in one colour and filled in another.
        GlyphVector noticeGlyphs = noticeFont.createGlyphVector(graphics.getFontRenderContext(), DataConstants.NON_PRODUCTION_NOTICE);
        Shape noticeOutline = noticeGlyphs.getOutline();

        double horizontalStep = noticeGlyphs.getLogicalBounds().getWidth() + (double) fontSize * HORIZONTAL_GAP_IN_FONT_SIZES;
        double verticalStep = (double) fontSize * VERTICAL_STEP_IN_FONT_SIZES;

        // Rotating about the origin by -45 degrees maps the device rectangle [0, width] x [0, height] back to
        // user-space x in [-height/sqrt(2), width/sqrt(2)] and y in [0, (width + height)/sqrt(2)], so the loops
        // sweep only that band. Each bound is padded by one step so a tile whose origin falls just outside it is
        // still drawn where it overlaps; the spill over the edges is clipped away.
        double diagonalScale = Math.sqrt(2);
        double minimumX = -imageHeight / diagonalScale - horizontalStep;
        double maximumX = imageWidth / diagonalScale + horizontalStep;
        double minimumY = -verticalStep;
        double maximumY = (imageWidth + imageHeight) / diagonalScale + verticalStep;

        graphics.rotate(-Math.PI / 4);
        for (double y = minimumY; y < maximumY; y += verticalStep) {
            for (double x = minimumX; x < maximumX; x += horizontalStep) {
                Shape placedNotice = AffineTransform.getTranslateInstance(x, y).createTransformedShape(noticeOutline);
                graphics.setColor(NOTICE_STROKE_COLOR);
                graphics.draw(placedNotice);
                graphics.setColor(NOTICE_FILL_COLOR);
                graphics.fill(placedNotice);
            }
        }
    }

    private static byte[] encodeImage(BufferedImage image, String imageFormatName) throws IOException {
        ByteArrayOutputStream encodedImage = new ByteArrayOutputStream();
        if (JPEG_IMAGE_FORMAT.equals(imageFormatName)) {
            // ImageIO.write gives no way to state a compression quality, so the writer is driven directly.
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(imageFormatName);
            if (!writers.hasNext()) {
                throw new IOException("Failed to apply the non-production notice: no writer available for image format " + imageFormatName);
            }
            ImageWriter writer = writers.next();
            ImageWriteParam writeParams = writer.getDefaultWriteParam();
            writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParams.setCompressionQuality(JPEG_RE_ENCODE_QUALITY);
            try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(encodedImage)) {
                writer.setOutput(imageOutput);
                writer.write(null, new IIOImage(image, null, null), writeParams);
            } finally {
                writer.dispose();
            }
        } else if (!ImageIO.write(image, imageFormatName, encodedImage)) {
            throw new IOException("Failed to apply the non-production notice: no writer available for image format " + imageFormatName);
        }
        return encodedImage.toByteArray();
    }

}
