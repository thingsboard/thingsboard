/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ImageUtils {

    private static final Map<String, String> mediaTypeMappings = Map.of(
            "jpeg", "jpg",
            "svg+xml", "svg",
            "x-icon", "ico"
    );

    public static String mediaTypeToFileExtension(String mimeType) {
        String subtype = MimeTypeUtils.parseMimeType(mimeType).getSubtype();
        return mediaTypeMappings.getOrDefault(subtype, subtype);
    }

    public static String fileExtensionToMediaType(String extension) {
        String subtype = mediaTypeMappings.entrySet().stream()
                .filter(mapping -> mapping.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst().orElse(extension);
        return new MimeType("image", subtype).toString();
    }

    public static ProcessedImage processImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        if (mediaTypeToFileExtension(mediaType).equals("svg")) {
            try {
                return processSvgImage(data, mediaType, thumbnailMaxDimension);
            } catch (Exception e) {
                if (log.isDebugEnabled()) { // printing stacktrace
                    log.warn("Couldn't process SVG image, leaving preview as original image", e);
                } else {
                    log.warn("Couldn't process SVG image, leaving preview as original image: {}", ExceptionUtils.getMessage(e));
                }
                return previewAsOriginalImage(data, mediaType);
            }
        }
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
        } catch (Exception ignored) {
        }
        if (bufferedImage == null) { // means that media type is not supported by ImageIO; extracting width and height from metadata and leaving preview as original image
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(data));
            ProcessedImage image = previewAsOriginalImage(data, mediaType);
            String dirName = "Unknown";
            for (Directory dir : metadata.getDirectories()) {
                Tag widthTag = dir.getTags().stream()
                        .filter(tag -> tag.getTagName().toLowerCase().contains("width"))
                        .findFirst().orElse(null);
                Tag heightTag = dir.getTags().stream()
                        .filter(tag -> tag.getTagName().toLowerCase().contains("height"))
                        .findFirst().orElse(null);
                if (widthTag == null || heightTag == null) {
                    continue;
                }
                int width = Integer.parseInt(dir.getObject(widthTag.getTagType()).toString());
                int height = Integer.parseInt(dir.getObject(heightTag.getTagType()).toString());
                image.setWidth(width);
                image.setHeight(height);
                image.getPreview().setWidth(width);
                image.getPreview().setHeight(height);
                dirName = dir.getName();
                break;
            }
            log.warn("Couldn't process {} ({}) with ImageIO, leaving preview as original image", mediaType, dirName);
            return image;
        }

        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setData(data);
        image.setSize(data.length);
        image.setWidth(bufferedImage.getWidth());
        image.setHeight(bufferedImage.getHeight());

        ProcessedImage preview = new ProcessedImage();
        int[] thumbnailDimensions = getThumbnailDimensions(image.getWidth(), image.getHeight(), thumbnailMaxDimension, true);
        preview.setWidth(thumbnailDimensions[0]);
        preview.setHeight(thumbnailDimensions[1]);

        if (preview.getWidth() == image.getWidth() && preview.getHeight() == image.getHeight()) {
            if (mediaType.equals("image/png")) {
                preview.setMediaType(mediaType);
                preview.setData(null);
                preview.setSize(data.length);
                image.setPreview(preview);
                return image;
            }
        }

        BufferedImage thumbnail = new BufferedImage(preview.getWidth(), preview.getHeight(), BufferedImage.TYPE_INT_ARGB);
        thumbnail.getGraphics().drawImage(bufferedImage, 0, 0, preview.getWidth(), preview.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "png", out);

        preview.setMediaType("image/png");
        preview.setData(out.toByteArray());
        preview.setSize(preview.getData().length);
        image.setPreview(preview);
        return image;
    }

    public static ProcessedImage processSvgImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
        Document document = factory.createDocument(null, new ByteArrayInputStream(data));
        Integer width = null;
        Integer height = null;
        String strWidth = document.getDocumentElement().getAttribute("width");
        String strHeight = document.getDocumentElement().getAttribute("height");
        if (StringUtils.isNotEmpty(strWidth) && StringUtils.isNotEmpty(strHeight)) {
            try {
                width = (int) Double.parseDouble(strWidth);
                height = (int) Double.parseDouble(strHeight);
            } catch (NumberFormatException ignored) {} // in case width and height are in %, mm, etc.
        }
        if (width == null || height == null) {
            String viewBox = document.getDocumentElement().getAttribute("viewBox");
            if (StringUtils.isNotEmpty(viewBox)) {
                String[] viewBoxValues = viewBox.split(" ");
                if (viewBoxValues.length > 3) {
                    width = (int) Double.parseDouble(viewBoxValues[2]);
                    height = (int) Double.parseDouble(viewBoxValues[3]);
                }
            }
        }
        if (width == null) {
            UserAgent agent = new UserAgentAdapter();
            DocumentLoader loader = new DocumentLoader(agent);
            BridgeContext context = new BridgeContext(agent, loader);
            context.setDynamic(true);
            GVTBuilder builder = new GVTBuilder();
            GraphicsNode root = builder.build(context, document);
            var bounds = root.getPrimitiveBounds();
            if (bounds != null) {
                width = (int) bounds.getWidth();
                height = (int) bounds.getHeight();
            }
        }
        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setWidth(width == null ? 0 : width);
        image.setHeight(height == null ? 0 : height);
        image.setData(data);
        image.setSize(data.length);

        PNGTranscoder transcoder = new PNGTranscoder();
        if (image.getSize() < 10240) { // if SVG is smaller than 10kB (average 250x250 PNG preview size)
            return withPreviewAsOriginalImage(image);
        } else if (image.getSize() > 102400 && image.getWidth() != 0) { // considering SVG image detailed after 100kB
            // increasing preview dimensions
            thumbnailMaxDimension = 512;
            int[] thumbnailDimensions = getThumbnailDimensions(image.getWidth(), image.getHeight(), thumbnailMaxDimension, false);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) thumbnailDimensions[0]);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) thumbnailDimensions[1]);
        } else {
            transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_WIDTH, (float) thumbnailMaxDimension);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_HEIGHT, (float) thumbnailMaxDimension);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transcoder.transcode(new TranscoderInput(new ByteArrayInputStream(data)), new TranscoderOutput(out));
        byte[] pngThumbnail = out.toByteArray();

        ProcessedImage preview = new ProcessedImage();
        preview.setWidth(thumbnailMaxDimension);
        preview.setHeight(thumbnailMaxDimension);
        preview.setMediaType("image/png");
        preview.setData(pngThumbnail);
        preview.setSize(pngThumbnail.length);
        image.setPreview(preview);
        return image;
    }

    private static ProcessedImage previewAsOriginalImage(byte[] data, String mediaType) {
        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setData(data);
        image.setSize(data.length);
        image.setWidth(0);
        image.setHeight(0);
        return withPreviewAsOriginalImage(image);
    }

    public static ProcessedImage withPreviewAsOriginalImage(ProcessedImage originalImage) {
        originalImage.setPreview(originalImage.withData(null));
        return originalImage;
    }

    public static ScadaSymbolMetadataInfo processScadaSymbolMetadata(String fileName, byte[] data) throws Exception {
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
        Document document = factory.createDocument(null, new ByteArrayInputStream(data));
        var metaElements = document.getElementsByTagName("tb:metadata");
        JsonNode metaData = null;
        if (metaElements.getLength() > 0) {
            metaData = JacksonUtil.toJsonNode(metaElements.item(0).getTextContent());
        }
        return new ScadaSymbolMetadataInfo(fileName, metaData);
    }

    private static int[] getThumbnailDimensions(int originalWidth, int originalHeight, int maxDimension, boolean originalIfSmaller) {
        if (originalWidth <= maxDimension && originalHeight <= maxDimension && originalIfSmaller) {
            return new int[]{originalWidth, originalHeight};
        }
        int thumbnailWidth;
        int thumbnailHeight;
        double aspectRatio = (double) originalWidth / originalHeight;
        if (originalWidth > originalHeight) {
            thumbnailWidth = maxDimension;
            thumbnailHeight = (int) (maxDimension / aspectRatio);
        } else {
            thumbnailWidth = (int) (maxDimension * aspectRatio);
            thumbnailHeight = maxDimension;
        }
        return new int[]{thumbnailWidth, thumbnailHeight};
    }

    public static String getEmbeddedBase64EncodedImg(String colorStr) {
        try {
            Color color = parseColor(colorStr); // Support for hex, rgb, hsla
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, color.getRGB());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64String = Base64.getEncoder().encodeToString(imageBytes);

            return "data:image/png;base64," + base64String;
        } catch (Exception e) {
            log.warn("Failed to generate embedded image for color: {}", colorStr, e);
            return null;
        }
    }

    private static Color parseColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            return Color.decode(colorStr);
        }

        if (colorStr.startsWith("rgb")) {
            return parseRgbColor(colorStr);
        }

        if (colorStr.startsWith("hsl")) {
            return parseHslaColor(colorStr);
        }

        throw new IllegalArgumentException("Unsupported color format: " + colorStr);
    }

    private static Color parseRgbColor(String rgb) {
        String[] rgbValues = rgb.replaceAll("[^0-9,]", "").split(",");
        int r = Integer.parseInt(rgbValues[0]);
        int g = Integer.parseInt(rgbValues[1]);
        int b = Integer.parseInt(rgbValues[2]);
        return new Color(r, g, b);
    }

    private static Color parseHslaColor(String hsla) {
        String[] hslaValues = hsla.replaceAll("[^0-9.,]", "").split(",");
        float h = Float.parseFloat(hslaValues[0]);
        float s = Float.parseFloat(hslaValues[1]) / 100;
        float l = Float.parseFloat(hslaValues[2]) / 100;
        float a = hslaValues.length > 3 ? Float.parseFloat(hslaValues[3]) : 1.0f;
        return hslaToColor(h, s, l, a);
    }

    private static Color hslaToColor(float h, float s, float l, float alpha) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = l - c / 2;

        float r = 0, g = 0, b = 0;
        if (h < 60) {
            r = c;
            g = x;
        } else if (h < 120) {
            r = x;
            g = c;
        } else if (h < 180) {
            g = c;
            b = x;
        } else if (h < 240) {
            g = x;
            b = c;
        } else if (h < 300) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }

        r += m;
        g += m;
        b += m;

        return new Color(clamp(r), clamp(g), clamp(b), clamp(alpha));
    }

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessedImage {
        private String mediaType;
        private int width;
        private int height;
        @With
        private byte[] data;
        private long size;
        private ProcessedImage preview;
    }

    @Data
    public static class ScadaSymbolMetadataInfo {
        private String title;
        private String description;
        private String[] searchTags;
        private int widgetSizeX;
        private int widgetSizeY;

        public ScadaSymbolMetadataInfo(String fileName, JsonNode metaData) {
            if (metaData != null && metaData.has("title")) {
                title = metaData.get("title").asText();
            } else {
                title = fileName;
            }
            if (metaData != null && metaData.has("description")) {
                description = metaData.get("description").asText();
            } else {
                description = "";
            }
            if (metaData != null && metaData.has("searchTags") && metaData.get("searchTags").isArray()) {
                var tagsNode = (ArrayNode) metaData.get("searchTags");
                searchTags = new String[tagsNode.size()];
                for (int i = 0; i < tagsNode.size(); i++) {
                    searchTags[i] = tagsNode.get(i).asText();
                }
            } else {
                searchTags = new String[0];
            }
            if (metaData != null && metaData.has("widgetSizeX")) {
                widgetSizeX = metaData.get("widgetSizeX").asInt();
            } else {
                widgetSizeX = 3;
            }
            if (metaData != null && metaData.has("widgetSizeY")) {
                widgetSizeY = metaData.get("widgetSizeY").asInt();
            } else {
                widgetSizeY = 3;
            }
        }
    }

}
