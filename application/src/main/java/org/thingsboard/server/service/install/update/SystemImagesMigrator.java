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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Base64Utils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.util.ImageUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class SystemImagesMigrator {

    private static final Path dataDir = Path.of(
            "/home/*/thingsboard-ce/application/src/main/data"
    );
    private static final Path imagesDir = dataDir.resolve("images");
    private static final Path widgetBundlesDir = dataDir.resolve("json").resolve("system").resolve("widget_bundles");
    private static final Path widgetTypesDir = dataDir.resolve("json").resolve("system").resolve("widget_types");
    private static final Path demoDashboardsDir = dataDir.resolve("json").resolve("demo").resolve("dashboards");

    public static void main(String[] args) throws Exception {
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
    }

    public static void updateWidgetsBundle(ObjectNode widgetsBundleJson) {
        String imageLink = getText(widgetsBundleJson, "image");
        widgetsBundleJson.put("image", inlineImage(imageLink, "widget_bundles"));
    }

    public static void updateWidget(ObjectNode widgetJson) {
        String previewImageLink = widgetJson.get("image").asText();
        widgetJson.put("image", inlineImage(previewImageLink, "widgets"));

        ObjectNode descriptor = (ObjectNode) widgetJson.get("descriptor");
        JsonNode defaultConfig = JacksonUtil.toJsonNode(descriptor.get("defaultConfig").asText());
        updateWidgetConfig(defaultConfig, "widgets");
        descriptor.put("defaultConfig", defaultConfig.toString());
    }

    public static void updateDashboard(ObjectNode dashboardJson) {
        String image = getText(dashboardJson, "image");
        dashboardJson.put("image", inlineImage(image, "dashboards"));

        dashboardJson.get("configuration").get("widgets").elements().forEachRemaining(widgetConfig -> {
            updateWidgetConfig(widgetConfig.get("config"), "dashboards");
        });
    }

    private static void updateWidgetConfig(JsonNode widgetConfigJson, String directory) {
        ObjectNode widgetSettings = (ObjectNode) widgetConfigJson.get("settings");
        ArrayNode markerImages = (ArrayNode) widgetSettings.get("markerImages");
        if (markerImages != null && !markerImages.isEmpty()) {
            for (int i = 0; i < markerImages.size(); i++) {
                markerImages.set(i, inlineImage(markerImages.get(i).asText(), directory));
            }
        }

        String mapImage = getText(widgetSettings, "mapImageUrl");
        if (mapImage != null) {
            widgetSettings.put("mapImageUrl", inlineImage(mapImage, directory));
        }

        String backgroundImage = getText(widgetSettings, "backgroundImageUrl");
        if (backgroundImage != null) {
            widgetSettings.put("backgroundImageUrl", inlineImage(backgroundImage, directory));
        }

        JsonNode backgroundConfigNode = widgetSettings.get("background");
        if (backgroundConfigNode != null && backgroundConfigNode.isObject()) {
            ObjectNode backgroundConfig = (ObjectNode) backgroundConfigNode;
            if ("imageUrl".equals(getText(backgroundConfig, "type"))) {
                String imageLink = getText(backgroundConfig, "imageUrl");
                if (imageLink != null && imageLink.startsWith("/api/images")) {
                    backgroundConfig.put("imageBase64", inlineImage(imageLink, directory));
                    backgroundConfig.set("imageUrl", null);
                    backgroundConfig.put("type", "image");
                }
            }
        }
    }

    @SneakyThrows
    private static String inlineImage(String url, String subDir) {
        if (url != null && url.startsWith("/api/images")) {
            String imageKey = StringUtils.substringAfterLast(url, "/");
            Path file = imagesDir.resolve(subDir).resolve(imageKey);
            String mediaType = ImageUtils.fileExtensionToMediaType(StringUtils.substringAfterLast(imageKey, "."));
            return "data:" + mediaType + ";base64," + Base64Utils.encodeToString(Files.readAllBytes(file));
        } else {
            return url;
        }
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
