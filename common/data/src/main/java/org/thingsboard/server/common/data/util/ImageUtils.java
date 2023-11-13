/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageUtils {

    private static final Map<String, String> mediaTypeMappings = Map.of(
            "jpeg", "jpg",
            "svg+xml", "svg"
    );

    public static String mediaTypeToFileExtension(String mimeType) {
        String subtype = MimeTypeUtils.parseMimeType(mimeType).getSubtype();
        return mediaTypeMappings.getOrDefault(subtype, subtype);
    }

    public static String fileExtensionToMediaType(String type, String extension) {
        String subtype = mediaTypeMappings.entrySet().stream()
                .filter(mapping -> mapping.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst().orElse(extension);
        return new MimeType(type, subtype).toString();
    }

    public static ImageInfo processImage(byte[] imageData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        ImageThumbnail thumbnail = getImageThumbnail(image, 250);
        return new ImageInfo(image.getWidth(), image.getHeight(), thumbnail);
    }

    private static ImageThumbnail getImageThumbnail(BufferedImage originalImage, int maxDimension) throws IOException {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int thumbnailWidth;
        int thumbnailHeight;
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            thumbnailWidth = originalWidth;
            thumbnailHeight = originalHeight;
        } else {
            double aspectRatio = (double) originalWidth / originalHeight;
            if (originalWidth > originalHeight) {
                thumbnailWidth = maxDimension;
                thumbnailHeight = (int) (maxDimension / aspectRatio);
            } else {
                thumbnailWidth = (int) (maxDimension * aspectRatio);
                thumbnailHeight = maxDimension;
            }
        }
        BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
        thumbnail.getGraphics().drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "ignored", os);
        return new ImageThumbnail(thumbnail.getWidth(), thumbnail.getHeight(), os.toByteArray());
    }

    @Data
    public static class ImageInfo {
        private final int width;
        private final int height;
        private final ImageThumbnail thumbnail;
    }

    @Data
    public static class ImageThumbnail {
        private final int width;
        private final int height;
        private final byte[] data;
    }

}
