// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.util;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.thingsboard.server.dao.util.ImageUtils.ProcessedImage;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class ImageUtilsTest {

    private static final byte[] SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><text>A</text></svg>"
            .getBytes(StandardCharsets.UTF_8);

    @Test
    void svgThatCannotBeRenderedForLackOfFontsKeepsTheOriginalAsPreview() throws Exception {
        try (MockedStatic<ImageUtils> imageUtils = mockStatic(ImageUtils.class, CALLS_REAL_METHODS)) {
            imageUtils.when(() -> ImageUtils.processSvgImage(any(), anyString(), anyInt()))
                    .thenThrow(new NoClassDefFoundError("Could not initialize class com.github.weisj.jsvg.attributes.font.FontResolver$FontFamiliesCache"));

            ProcessedImage image = ImageUtils.processImage(SVG, "image/svg+xml", 250);

            assertThat(image.getData()).isEqualTo(SVG);
            assertThat(image.getPreview().getData()).isNull();
            assertThat(image.getMediaType()).isEqualTo("image/svg+xml");
        }
    }

}
