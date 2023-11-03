package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.MediaTypeUtils;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.resource.ResourceService;

import java.util.List;
import java.util.Optional;

@Component
//@Profile("install")
@Slf4j
@RequiredArgsConstructor
public class ImagesUpdater {

    private final ResourceService resourceService;


    // TODO: solution templates


    public void updateWidgetImages(WidgetTypeDetails widgetTypeDetails) {
        String imageName = widgetTypeDetails.getName() + " - image";
        String imageKey = widgetTypeDetails.getFqn();
        String imageLink = saveImage(imageName, imageKey, widgetTypeDetails.getImage());
        widgetTypeDetails.setImage(imageLink);

        JsonNode defaultConfig = JacksonUtil.toJsonNode(widgetTypeDetails.getDescriptor().get("defaultConfig").asText());
        defaultConfig = updateWidgetConfig(TenantId.SYS_TENANT_ID, defaultConfig, imageName, imageKey, imageKey);
        ((ObjectNode) widgetTypeDetails.getDescriptor()).put("defaultConfig", defaultConfig.toString());
    }

    public void updateWidgetsBundleImages(WidgetsBundle widgetsBundle) {
        String imageLink = saveImage(widgetsBundle.getTitle(), widgetsBundle.getAlias(), widgetsBundle.getImage());
        widgetsBundle.setImage(imageLink);
    }

    public boolean updateDashboardImages(Dashboard dashboard) {
        String imageNamePrefix = dashboard.getTitle();
        String imageKeyPrefix = "dashboard_" + dashboard.getUuidId();
        boolean updated = false;

        String imageLink = saveImage(dashboard.getTenantId(), imageNamePrefix + " - image", imageKeyPrefix + ".image", dashboard.getImage(), null);
        dashboard.setImage(imageLink);

        for (ObjectNode widgetConfig : dashboard.getWidgetsConfig()) {
            String alias = widgetConfig.get("bundleAlias").asText() + "." + widgetConfig.get("typeAlias").asText();
            String widgetName = widgetConfig.get("config").get("title").asText();
            updateWidgetConfig(dashboard.getTenantId(), widgetConfig.get("config"),
                    imageNamePrefix + " - " + widgetName + " widget",
                    imageKeyPrefix + ".widget." + alias, alias);
        }

        return updated;
    }

    private JsonNode updateWidgetConfig(TenantId tenantId, JsonNode widgetConfig,
                                        String imageNamePrefix, String imageKeyPrefix,
                                        String fqn) {
        ObjectNode widgetSettings = (ObjectNode) widgetConfig.get("settings");
        ArrayNode markerImages = (ArrayNode) widgetSettings.get("markerImages");
        if (markerImages != null && !markerImages.isEmpty()) {
            for (int i = 0; i < markerImages.size(); i++) {
                String imageName = imageNamePrefix + " - marker image " + (i + 1);
                String imageKey = imageKeyPrefix + ".marker_image_" + (i + 1);
                String imageLink = saveImage(tenantId, imageName, imageKey, markerImages.get(i).asText(), fqn + ".marker_image_");
                markerImages.set(i, imageLink);
            }
        }

        String mapImage = getText(widgetSettings, "mapImageUrl");
        if (mapImage != null) {
            String imageName = imageNamePrefix + " - map image";
            String imageKeySuffix = ".map_image";
            String imageKey = imageKeyPrefix + imageKeySuffix;
            String imageLink = saveImage(tenantId, imageName, imageKey, mapImage, fqn + imageKeySuffix);
            widgetSettings.put("mapImageUrl", imageLink);
        }

        String backgroundImage = getText(widgetSettings, "backgroundImageUrl");
        if (backgroundImage != null) {
            String imageName = imageNamePrefix + " - background image";
            String imageKeySuffix = ".background_image";
            String imageKey = imageKeyPrefix + imageKeySuffix;
            String imageLink = saveImage(tenantId, imageName, imageKey, backgroundImage, fqn + imageKeySuffix);
            widgetSettings.put("backgroundImageUrl", imageLink);
        }

        JsonNode backgroundConfigNode = widgetSettings.get("background");
        if (backgroundConfigNode != null && backgroundConfigNode.isObject()) {
            ObjectNode backgroundConfig = (ObjectNode) backgroundConfigNode;
            if ("image".equals(getText(backgroundConfig, "type"))) {
                String imageBase64 = getText(backgroundConfig, "imageBase64");
                if (imageBase64 != null) {
                    String imageName = imageNamePrefix + " - background image";
                    String imageKeySuffix = ".background_image";
                    String imageKey = imageKeyPrefix + imageKeySuffix;
                    String imageLink = saveImage(tenantId, imageName, imageKey, imageBase64, fqn + imageKeySuffix);
                    backgroundConfig.set("imageBase64", null);
                    backgroundConfig.put("imageUrl", imageLink);
                    backgroundConfig.put("type", "imageUrl");
                }
            }
        }

        return widgetConfig;
    }

    private String getText(JsonNode jsonNode, String field) {
        return Optional.ofNullable(jsonNode.get(field))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText).orElse(null);
    }

    private String saveImage(String title, String key, String data) {
        return saveImage(TenantId.SYS_TENANT_ID, title, key, data, null);
    }

    private String saveImage(TenantId tenantId, String title, String key, String data,
                             String existingImageQuery) {
        if (data == null) {
            return null;
        }
        String base64Data = StringUtils.substringAfter(data, "base64,");
        if (base64Data.isEmpty()) {
            return data;
        }
        String imageMediaType = StringUtils.substringBetween(data, "data:", ";base64");
        String extension = MediaTypeUtils.getFileExtension(imageMediaType);
        key += "." + extension;

        TbResourceInfo resourceInfo = resourceService.findResourceInfoByTenantIdAndKey(tenantId, ResourceType.IMAGE, key);
        if (resourceInfo == null && !tenantId.isSysTenantId() && existingImageQuery != null) {
            List<TbResourceInfo> existing = resourceService.findByTenantIdAndDataAndKeyStartingWith(TenantId.SYS_TENANT_ID, base64Data, existingImageQuery);
            if (!existing.isEmpty()) {
                resourceInfo = existing.get(0);
                if (existing.size() > 1) {
                    log.warn("Found more than one system image resources for key {}", existingImageQuery);
                }
                log.info("Using system image {} for {}", resourceInfo.getLink(), key);
                return resourceInfo.getLink();
            }
        }
        TbResource resource;
        if (resourceInfo == null) {
            resource = new TbResource();
            resource.setTenantId(tenantId);
            resource.setResourceType(ResourceType.IMAGE);
            resource.setResourceKey(key);
        } else if (tenantId.isSysTenantId()) {
            resource = new TbResource(resourceInfo);
        } else {
            return resourceInfo.getLink();
        }
        resource.setTitle(title);
        resource.setFileName(key);
        resource.setMediaType(imageMediaType);
        resource.setBase64Data(base64Data);
        resource = resourceService.saveResource(resource);
        log.info("[{}] {} image '{}' {} ({})", tenantId, resourceInfo == null ? "Created" : "Updated",
                resource.getTitle(), resource.getResourceKey(), resource.getLink());
        return resource.getLink();

    }

}
