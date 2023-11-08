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
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.Hashing;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.util.MediaTypeUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Slf4j
public class SystemImagesMigrator { // TEMPORARY

    private static final Path dataDir = Path.of(
            "/home/*/thingsboard-ce/application/src/main/data"
    );
    private static final Path imagesDir = dataDir.resolve("images");
    private static final Path widgetBundlesDir = dataDir.resolve("json").resolve("system").resolve("widget_bundles");
    private static final Path widgetTypesDir = dataDir.resolve("json").resolve("system").resolve("widget_types");
    private static final Path demoDashboardsDir = dataDir.resolve("json").resolve("demo").resolve("dashboards");

    private static final Map<String, String> imageNames = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        Files.createDirectories(imagesDir);
        Path imageNamesFile = imagesDir.resolve("names.json");
        if (imageNamesFile.toFile().exists()) {
            Map<String, String> existingImageNames = JacksonUtil.OBJECT_MAPPER.readValue(imageNamesFile.toFile(), new TypeReference<>() {});
            imageNames.putAll(existingImageNames);
        }

        Files.list(widgetTypesDir).forEach(file -> {
            ObjectNode widgetTypeJson = (ObjectNode) JacksonUtil.toJsonNode(file.toFile());
            updateWidget(widgetTypeJson);
            saveJson(file, widgetTypeJson);
        });

        Files.list(widgetBundlesDir).forEach(file -> {
            JsonNode widgetsBundleDescriptorJson = JacksonUtil.toJsonNode(file.toFile());
            ObjectNode widgetsBundleJson = (ObjectNode) widgetsBundleDescriptorJson.get("widgetsBundle");
            updateWidgetsBundle(widgetsBundleJson);
            saveJson(file, widgetsBundleDescriptorJson);
        });

        Files.list(demoDashboardsDir).forEach(file -> {
            ObjectNode dashboardJson = (ObjectNode) JacksonUtil.toJsonNode(file.toFile());
            updateDashboard(dashboardJson);
            saveJson(file, dashboardJson);
        });

        saveJson(imageNamesFile, JacksonUtil.valueToTree(imageNames));
    }

    public static void updateWidgetsBundle(ObjectNode widgetsBundleJson) {
        String widgetsBundleName = widgetsBundleJson.get("title").asText();
        String widgetsBundleAlias = widgetsBundleJson.get("alias").asText();

        String image = getText(widgetsBundleJson, "image");
        String imageLink = saveImage(widgetsBundleName + " - image", widgetsBundleAlias, image, "widget_bundles");
        widgetsBundleJson.put("image", imageLink);
    }

    public static void updateWidget(ObjectNode widgetJson) {
        String widgetName = widgetJson.get("name").asText();
        String widgetFqn = widgetJson.get("fqn").asText();

        String previewImage = widgetJson.get("image").asText();
        String previewImageLink = saveImage(widgetName + " - image", widgetFqn, previewImage, "widgets");
        widgetJson.put("image", previewImageLink);

        ObjectNode descriptor = (ObjectNode) widgetJson.get("descriptor");
        JsonNode defaultConfig = JacksonUtil.toJsonNode(descriptor.get("defaultConfig").asText());
        updateWidgetConfig(defaultConfig, widgetName, widgetFqn, "widgets");
        descriptor.put("defaultConfig", defaultConfig.toString());
    }

    public static void updateDashboard(ObjectNode dashboardJson) {
        String title = dashboardJson.get("title").asText();
        String imageNamePrefix = title + " dashboard";
        String imageKeyPrefix = title.replace(" ", "_").toLowerCase();

        String image = getText(dashboardJson, "image");
        String imageLink = saveImage(imageNamePrefix + " - image", imageKeyPrefix + ".image", image, "dashboards");
        dashboardJson.put("image", imageLink);

        dashboardJson.get("configuration").get("widgets").elements().forEachRemaining(widgetConfig -> {
            String fqn;
            if (widgetConfig.has("typeFullFqn")) {
                fqn = StringUtils.substringAfter(widgetConfig.get("typeFullFqn").asText(), "."); // removing prefix ('system' or 'tenant')
            } else {
                fqn = widgetConfig.get("bundleAlias").asText() + "." + widgetConfig.get("typeAlias").asText();
            }
            String widgetName = widgetConfig.get("config").get("title").asText();
            updateWidgetConfig(widgetConfig.get("config"),
                    imageNamePrefix + " - " + widgetName + " widget",
                    imageKeyPrefix + "." + fqn + "###" + widgetConfig.get("id").asText(),
                    "dashboards");
        });
    }

    private static void updateWidgetConfig(JsonNode widgetConfigJson,
                                           String imageNamePrefix, String imageKeyPrefix,
                                           String directory) {
        ObjectNode widgetSettings = (ObjectNode) widgetConfigJson.get("settings");
        ArrayNode markerImages = (ArrayNode) widgetSettings.get("markerImages");
        if (markerImages != null && !markerImages.isEmpty()) {
            for (int i = 0; i < markerImages.size(); i++) {
                String imageName = imageNamePrefix + " - marker image " + (i + 1);
                String imageKey = imageKeyPrefix + "#marker_image_" + (i + 1);
                String imageLink = saveImage(imageName, imageKey, markerImages.get(i).asText(), directory);
                markerImages.set(i, imageLink);
            }
        }

        String mapImage = getText(widgetSettings, "mapImageUrl");
        if (mapImage != null) {
            String imageName = imageNamePrefix + " - map image";
            String imageKeySuffix = "#map_image";
            String imageKey = imageKeyPrefix + imageKeySuffix;
            String imageLink = saveImage(imageName, imageKey, mapImage, directory);
            widgetSettings.put("mapImageUrl", imageLink);
        }

        String backgroundImage = getText(widgetSettings, "backgroundImageUrl");
        if (backgroundImage != null) {
            String imageName = imageNamePrefix + " - background image";
            String imageKeySuffix = "#background_image";
            String imageKey = imageKeyPrefix + imageKeySuffix;
            String imageLink = saveImage(imageName, imageKey, backgroundImage, directory);
            widgetSettings.put("backgroundImageUrl", imageLink);
        }

        JsonNode backgroundConfigNode = widgetSettings.get("background");
        if (backgroundConfigNode != null && backgroundConfigNode.isObject()) {
            ObjectNode backgroundConfig = (ObjectNode) backgroundConfigNode;
            if ("image".equals(getText(backgroundConfig, "type"))) {
                String imageBase64 = getText(backgroundConfig, "imageBase64");
                if (imageBase64 != null) {
                    String imageName = imageNamePrefix + " - background image";
                    String imageKeySuffix = "#background_image";
                    String imageKey = imageKeyPrefix + imageKeySuffix;
                    String imageLink = saveImage(imageName, imageKey, imageBase64, directory);
                    backgroundConfig.set("imageBase64", null);
                    backgroundConfig.put("imageUrl", imageLink);
                    backgroundConfig.put("type", "imageUrl");
                }
            }
        }
    }

    @SneakyThrows
    private static String saveImage(String imageName, String imageKey, String data, String subDirectory) {
        if (data == null) {
            return null;
        }
        String base64Data = StringUtils.substringAfter(data, "base64,");
        if (base64Data.isEmpty()) {
            return data;
        }
        String imageMediaType = StringUtils.substringBetween(data, "data:", ";base64");
        String extension = MediaTypeUtils.mediaTypeToFileExtension(imageMediaType);
        imageKey += "." + extension;

        byte[] image = Base64.getDecoder().decode(base64Data);
        Files.createDirectories(imagesDir.resolve(subDirectory));
        Path file = imagesDir.resolve(subDirectory).resolve(imageKey);
        Files.deleteIfExists(file);
        Files.write(file, image);
        imageNames.put(imageKey, imageName);
        log.info("New image {} ('{}')", imageKey, imageName);
        return "/api/images/system/" + imageKey;
    }

    private static String getText(JsonNode jsonNode, String field) {
        return Optional.ofNullable(jsonNode.get(field))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText).orElse(null);
    }

    @SneakyThrows
    private static void saveJson(Path file, JsonNode json) {
        Files.write(file, JacksonUtil.toPrettyString(json).getBytes(StandardCharsets.UTF_8));
    }

}
