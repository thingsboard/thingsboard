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
package org.thingsboard.server.dao.util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    public static String fileExtensionToMediaType(String extension) {
        String subtype = mediaTypeMappings.entrySet().stream()
                .filter(mapping -> mapping.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst().orElse(extension);
        return new MimeType("image", subtype).toString();
    }

    public static ProcessedImage processImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        if (mediaTypeToFileExtension(mediaType).equals("svg")) {
            return processSvgImage(data, mediaType, thumbnailMaxDimension);
        }

        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setWidth(bufferedImage.getWidth());
        image.setHeight(bufferedImage.getHeight());
        image.setData(data);
        image.setSize(data.length);

        ProcessedImage preview = new ProcessedImage();
        int[] thumbnailDimensions = getThumbnailDimensions(image.getWidth(), image.getHeight(), thumbnailMaxDimension);
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

        BufferedImage thumbnail = new BufferedImage(preview.getWidth(), preview.getHeight(), BufferedImage.TYPE_INT_RGB);
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
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(
                XMLResourceDescriptor.getXMLParserClassName());
        Document document = factory.createDocument(
                null, new ByteArrayInputStream(data));
        Integer width = null;
        Integer height = null;
        String strWidth = document.getDocumentElement().getAttribute("width");
        String strHeight = document.getDocumentElement().getAttribute("height");
        if (StringUtils.isNotEmpty(strWidth) && StringUtils.isNotEmpty(strHeight)) {
            width = Integer.parseInt(strWidth);
            height = Integer.parseInt(strHeight);
        } else {
            String viewBox = document.getDocumentElement().getAttribute("viewBox");
            if (StringUtils.isNotEmpty(viewBox)) {
                String[] viewBoxValues = viewBox.split(" ");
                if (viewBoxValues.length > 3) {
                    width = Integer.parseInt(viewBoxValues[2]);
                    height = Integer.parseInt(viewBoxValues[3]);
                }
            }
        }
        if (width == null) {
            UserAgent agent = new UserAgentAdapter();
            DocumentLoader loader= new DocumentLoader(agent);
            BridgeContext context = new BridgeContext(agent, loader);
            context.setDynamic(true);
            GVTBuilder builder= new GVTBuilder();
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
        transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_WIDTH, (float) thumbnailMaxDimension);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_HEIGHT, (float) thumbnailMaxDimension);
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

    private static int[] getThumbnailDimensions(int originalWidth, int originalHeight, int maxDimension) {
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
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

    @Data
    public static class ProcessedImage {
        private String mediaType;
        private int width;
        private int height;
        private byte[] data;
        private long size;
        private ProcessedImage preview;
    }

}
