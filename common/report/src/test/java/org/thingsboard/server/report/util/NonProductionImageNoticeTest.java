// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NonProductionImageNoticeTest {

    private static final int IMAGE_WIDTH = 800;
    private static final int IMAGE_HEIGHT = 600;

    // The coverage grid of testNonProductionNoticeMarksEveryCellOfTheImageWhateverItsAspectRatio. Both tile
    // steps are derived from the image WIDTH alone - the notice's own width plus three font sizes across, five
    // font sizes down - so the flatter the capture, the fewer tiles fit over it and the coarser the finest
    // grid it can be held to. Four divisions per axis is what the flattest of the three shapes below, 800x600,
    // allows to be asserted without measuring luck; the square 900x900 and the tall 300x1600 carry far more
    // tiles and would tolerate a finer one, but pitching the constant at them would fail the landscape case
    // over genuine gaps between tiles rather than over the notice failing to tile.
    //
    // An extremely wide, short capture is flatter still and used to be among the shapes; it was dropped rather
    // than accommodated by a coarser grid, for the reason given at the parameters.
    //
    // The floor is deliberately not fitted to a measured figure. Font.SANS_SERIF is a logical font whose
    // physical mapping differs between JDK builds and Linux distributions, and the step - so the tile phase,
    // not merely the count - moves with the metrics, so a floor with little headroom fails on an agent whose
    // fontconfig resolves it differently. Pinning a physical font is worse: no font file is guaranteed to be
    // present on a CI agent.
    private static final int COVERAGE_GRID_DIVISIONS = 4;
    private static final double MINIMUM_MARKED_PIXEL_RATIO_PER_CELL = 0.005;

    // The eight-byte PNG signature every PNG starts with, followed here by an IHDR chunk and nothing else.
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    // Mirrors NonProductionImageNotice.MAXIMUM_DECODED_PIXEL_COUNT, which is private. Restating it here is
    // deliberate: if the bound is ever changed, these tests should say so out loud rather than follow along.
    private static final long MAXIMUM_DECODED_PIXEL_COUNT = 100_000_000L;

    @Test
    void testNonProductionNoticeCoversEveryQuadrantOfThePngRatherThanASingleStrip() throws Exception {
        // The point of tiling rather than stamping a footer is that a mark spread over the whole bitmap
        // cannot be cropped off the way a strip along one edge can. That property is what this asserts:
        // marked pixels in all four quadrants. A footer implementation - which would still "add the notice"
        // by any looser reading - fails here, which is the reason the assertion is written this way.
        byte[] blankImage = givenBlankImage(NonProductionImageNotice.PNG_IMAGE_FORMAT);

        byte[] stampedImage = NonProductionImageNotice.addNonProductionNotice(blankImage, NonProductionImageNotice.PNG_IMAGE_FORMAT, true);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stampedImage));
        assertThat(decoded.getWidth()).isEqualTo(IMAGE_WIDTH);
        assertThat(decoded.getHeight()).isEqualTo(IMAGE_HEIGHT);
        for (int quadrantX = 0; quadrantX < 2; quadrantX++) {
            for (int quadrantY = 0; quadrantY < 2; quadrantY++) {
                assertThat(countMarkedPixels(decoded,
                        quadrantX * IMAGE_WIDTH / 2, quadrantY * IMAGE_HEIGHT / 2,
                        IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2))
                        .as("marked pixels in quadrant [%d,%d]", quadrantX, quadrantY)
                        .isGreaterThan(0);
            }
        }
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"300, 1600", "900, 900", "800, 600"})
    void testNonProductionNoticeMarksEveryCellOfTheImageWhateverItsAspectRatio(int imageWidth, int imageHeight) throws Exception {
        // The quadrant test above says the notice is not a single strip; this one says the tiling reaches the
        // whole bitmap whatever the shape of the capture. It is a forward guard rather than the record of a
        // bug that was found here. The tile loop does not sweep a safely oversized square - it sweeps exactly
        // the inverse image of the device rectangle under the -45 degree rotation, x over
        // [-height/sqrt(2), width/sqrt(2)] and y over [0, (width + height)/sqrt(2)], padded by one step at
        // each end. That narrow sweep is there for the work it saves; the oversized square it replaced was a
        // strict superset of it and could not under-cover, so this test is green against the wider sweep too
        // and is no evidence that narrowing it was needed or that it was done right.
        //
        // What the test does buy is that a future edit to those bounds cannot quietly lose coverage. They are
        // easy to get subtly wrong, and every way of getting them wrong loses one outer corner of the image
        // rather than an obvious central band: too small a maximum x loses the top-right corner, too large a
        // minimum x the bottom-left, too small a maximum y the bottom-right. A quadrant assertion survives all
        // of that, because the surviving tiles still reach into every quadrant. A sixteenth of the image does
        // not.
        //
        // The aspect ratios are the ones that stress the bounds in different directions: a tall, narrow
        // capture where the rotated band is far wider than it is tall, and a square one where the two corner
        // bounds bite simultaneously. An extremely wide, short capture is deliberately not among them: there
        // the horizontal step approaches the image width, so its emptiest cell is decided by the tile phase,
        // which moves with whatever physical font Font.SANS_SERIF resolves to on the agent - a floor on that
        // shape would be pinning the agent's fonts rather than the tiling.
        byte[] blankImage = givenBlankImage(NonProductionImageNotice.PNG_IMAGE_FORMAT, imageWidth, imageHeight);

        byte[] stampedImage = NonProductionImageNotice.addNonProductionNotice(blankImage, NonProductionImageNotice.PNG_IMAGE_FORMAT, true);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stampedImage));
        assertThat(decoded.getWidth()).isEqualTo(imageWidth);
        assertThat(decoded.getHeight()).isEqualTo(imageHeight);
        for (int column = 0; column < COVERAGE_GRID_DIVISIONS; column++) {
            for (int row = 0; row < COVERAGE_GRID_DIVISIONS; row++) {
                int originX = column * imageWidth / COVERAGE_GRID_DIVISIONS;
                int originY = row * imageHeight / COVERAGE_GRID_DIVISIONS;
                int cellWidth = (column + 1) * imageWidth / COVERAGE_GRID_DIVISIONS - originX;
                int cellHeight = (row + 1) * imageHeight / COVERAGE_GRID_DIVISIONS - originY;
                double markedRatio = (double) countMarkedPixels(decoded, originX, originY, cellWidth, cellHeight)
                        / ((long) cellWidth * cellHeight);
                assertThat(markedRatio)
                        .as("marked pixel ratio of cell [%d,%d] of the %dx%d image", column, row, imageWidth, imageHeight)
                        .isGreaterThanOrEqualTo(MINIMUM_MARKED_PIXEL_RATIO_PER_CELL);
            }
        }
    }

    @Test
    void testImageDeclaringMorePixelsThanTheLimitIsRejectedWithoutBeingDecoded() throws Exception {
        // The bound exists because the report service hands back a compressed capture, and a mostly flat
        // dashboard compresses so well that a small response can still decode to a raster larger than the
        // heap - on the caller's thread. So the bound has to be read from the header and be paid before the
        // decode, and this input is the proof: thirty-odd bytes, no image data at all, declaring one pixel
        // more than the limit. Nothing here can be decoded, so a check placed after the decode - or one
        // measuring the byte array rather than the declared dimensions - cannot produce this failure.
        long overTheLimit = MAXIMUM_DECODED_PIXEL_COUNT + 10_000L;
        byte[] oversizedHeader = givenPngHeaderDeclaring(10_000, (int) (overTheLimit / 10_000));
        assertThat(oversizedHeader.length).isLessThan(64);

        assertThatThrownBy(() -> NonProductionImageNotice.addNonProductionNotice(oversizedHeader, NonProductionImageNotice.PNG_IMAGE_FORMAT, true))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("too large to mark")
                .hasMessageContaining(Long.toString(overTheLimit))
                .hasMessageContaining(Long.toString(MAXIMUM_DECODED_PIXEL_COUNT));
    }

    @Test
    void testImageDeclaringExactlyTheLimitIsNotRejectedForItsSize() throws Exception {
        // The companion of the test above, one pixel the other side of the bound: the same header, the same
        // absent image data, a declared count of exactly the limit. The call still fails - there is nothing
        // to decode - but it must fail on the bytes rather than on the size, which is what pins the
        // comparison as strictly greater-than and stops the bound from quietly becoming an off-by-one that
        // rejects captures it was never meant to reject. Note that this passes vacuously against code with no
        // bound at all: the decoder chokes on the absent image data, and any such failure trivially satisfies
        // "does not say too large to mark". Pinning the comparison against off-by-one drift is therefore its
        // only purpose - it detects nothing on its own.
        byte[] atTheLimitHeader = givenPngHeaderDeclaring(10_000, (int) (MAXIMUM_DECODED_PIXEL_COUNT / 10_000));

        assertThatThrownBy(() -> NonProductionImageNotice.addNonProductionNotice(atTheLimitHeader, NonProductionImageNotice.PNG_IMAGE_FORMAT, true))
                .isInstanceOf(IOException.class)
                .hasMessageNotContaining("too large to mark");
    }

    @Test
    void testNonProductionNoticeIsAppliedToJpegAsWell() throws Exception {
        // JPEG is lossy, so an exact-pixel assertion would be measuring the codec rather than the notice.
        // What matters here is that the JPEG branch - which decodes, redraws and re-encodes through a
        // different writer path than PNG - produces a valid image of the original size that is not the input.
        byte[] blankImage = givenBlankImage(NonProductionImageNotice.JPEG_IMAGE_FORMAT);

        byte[] stampedImage = NonProductionImageNotice.addNonProductionNotice(blankImage, NonProductionImageNotice.JPEG_IMAGE_FORMAT, true);

        assertThat(stampedImage).isNotEqualTo(blankImage);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stampedImage));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(IMAGE_WIDTH);
        assertThat(decoded.getHeight()).isEqualTo(IMAGE_HEIGHT);
        assertThat(countMarkedPixels(decoded, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)).isGreaterThan(0);
    }

    @Test
    void testImageIsLeftByteForByteUntouchedWhenNotNonProduction() throws Exception {
        // A production instance must not pay a lossy re-encode - or any change at all - for this class
        // existing.
        byte[] blankImage = givenBlankImage(NonProductionImageNotice.JPEG_IMAGE_FORMAT);

        byte[] result = NonProductionImageNotice.addNonProductionNotice(blankImage, NonProductionImageNotice.JPEG_IMAGE_FORMAT, false);

        assertThat(result).isSameAs(blankImage);
    }

    @Test
    void testUndecodableImageFailsRatherThanReturningTheUnmarkedInput() {
        // The whole point of the failure policy: when the notice was required and could not be applied, the
        // caller must not receive bytes it will take for a marked artifact.
        byte[] notAnImage = "not an image".getBytes();

        assertThatThrownBy(() -> NonProductionImageNotice.addNonProductionNotice(notAnImage, NonProductionImageNotice.PNG_IMAGE_FORMAT, true))
                .isInstanceOf(IOException.class);
    }

    @Test
    void testUnsupportedFormatFailsRatherThanReturningTheUnmarkedInput() throws Exception {
        byte[] blankImage = givenBlankImage(NonProductionImageNotice.PNG_IMAGE_FORMAT);

        assertThatThrownBy(() -> NonProductionImageNotice.addNonProductionNotice(blankImage, "bmp", true))
                .isInstanceOf(IOException.class);
    }

    @Test
    void testNonProductionNoticeSurvivesAnIndexedPngRatherThanBeingQuantisedAway() throws Exception {
        // An indexed PNG is an ordinary thing for a screenshot pipeline to emit, and its palette has no room
        // for the blended colours a translucent overlay produces - drawing straight onto it would quantise the
        // notice down to whatever palette entry happens to be nearest, possibly back to the background. This
        // is the input that forces toDrawableImage to copy into a full-colour raster first.
        byte[] indexedImage = givenBlankIndexedImage();

        byte[] stampedImage = NonProductionImageNotice.addNonProductionNotice(indexedImage, NonProductionImageNotice.PNG_IMAGE_FORMAT, true);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stampedImage));
        assertThat(decoded.getWidth()).isEqualTo(IMAGE_WIDTH);
        assertThat(decoded.getHeight()).isEqualTo(IMAGE_HEIGHT);
        assertThat(countMarkedPixels(decoded, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)).isGreaterThan(0);
    }

    @Test
    void testImageWithAlphaIsFlattenedRatherThanRejectedByTheJpegWriter() throws Exception {
        // The JDK's JPEG writer refuses an image that still carries an alpha channel, so a capture that came
        // back with one has to be flattened onto white before it is re-encoded. Without that branch this call
        // fails instead of producing a marked JPEG.
        byte[] transparentImage = givenBlankTransparentImage();

        byte[] stampedImage = NonProductionImageNotice.addNonProductionNotice(transparentImage, NonProductionImageNotice.JPEG_IMAGE_FORMAT, true);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stampedImage));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(IMAGE_WIDTH);
        assertThat(decoded.getHeight()).isEqualTo(IMAGE_HEIGHT);
        assertThat(countMarkedPixels(decoded, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)).isGreaterThan(0);
    }

    /**
     * A uniformly white image, so that "marked" can be defined as simply "no longer white" - any pixel the
     * notice touched, whether by its dark fill or its light stroke over a white background, moves off pure
     * white, and nothing else in the image can.
     */
    private static byte[] givenBlankImage(String imageFormatName) throws IOException {
        return givenBlankImage(imageFormatName, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private static byte[] givenBlankImage(String imageFormatName, int imageWidth, int imageHeight) throws IOException {
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, imageWidth, imageHeight);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(image, imageFormatName, encoded);
        return encoded.toByteArray();
    }

    /**
     * A PNG signature followed by a single, checksum-correct IHDR chunk declaring the given dimensions, and
     * nothing after it - no image data, so the declared size costs nothing to produce. This is what lets the
     * pixel bound be tested at all: the production code reads the dimensions from the header through an
     * ImageIO reader and never decodes, so a few dozen bytes are enough to present it with a hundred-megapixel
     * image.
     */
    private static byte[] givenPngHeaderDeclaring(int declaredWidth, int declaredHeight) throws IOException {
        ByteArrayOutputStream headerChunk = new ByteArrayOutputStream();
        DataOutputStream headerChunkOutput = new DataOutputStream(headerChunk);
        headerChunkOutput.writeBytes("IHDR");
        headerChunkOutput.writeInt(declaredWidth);
        headerChunkOutput.writeInt(declaredHeight);
        headerChunkOutput.writeByte(8); // bit depth
        headerChunkOutput.writeByte(2); // colour type: truecolour
        headerChunkOutput.writeByte(0); // compression method
        headerChunkOutput.writeByte(0); // filter method
        headerChunkOutput.writeByte(0); // interlace method
        byte[] chunkTypeAndData = headerChunk.toByteArray();
        CRC32 checksum = new CRC32();
        checksum.update(chunkTypeAndData);

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        DataOutputStream pngOutput = new DataOutputStream(png);
        pngOutput.write(PNG_SIGNATURE);
        pngOutput.writeInt(chunkTypeAndData.length - "IHDR".length());
        pngOutput.write(chunkTypeAndData);
        pngOutput.writeInt((int) checksum.getValue());
        return png.toByteArray();
    }

    /**
     * The same uniformly white image as {@link #givenBlankImage(String)}, but palette-based, so that it decodes
     * back as {@link BufferedImage#TYPE_BYTE_INDEXED} and exercises the copy branch of toDrawableImage.
     */
    private static byte[] givenBlankIndexedImage() throws IOException {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(image, NonProductionImageNotice.PNG_IMAGE_FORMAT, encoded);
        return encoded.toByteArray();
    }

    /**
     * A fully transparent PNG, so the decoded image carries an alpha channel and the JPEG path has to flatten
     * it onto white before re-encoding.
     */
    private static byte[] givenBlankTransparentImage() throws IOException {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(image, NonProductionImageNotice.PNG_IMAGE_FORMAT, encoded);
        return encoded.toByteArray();
    }

    /**
     * Counts pixels that differ from white by more than a JPEG's worth of ringing, so the same measure is
     * usable for both formats.
     */
    private static int countMarkedPixels(BufferedImage image, int originX, int originY, int width, int height) {
        int markedPixels = 0;
        for (int x = originX; x < originX + width; x++) {
            for (int y = originY; y < originY + height; y++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red < 220 || green < 220 || blue < 220) {
                    markedPixels++;
                }
            }
        }
        return markedPixels;
    }

}
