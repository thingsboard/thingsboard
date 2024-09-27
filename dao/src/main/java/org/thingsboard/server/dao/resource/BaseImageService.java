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
package org.thingsboard.server.dao.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.ImageContainerDao;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;
import org.thingsboard.server.dao.util.ImageUtils;
import org.thingsboard.server.dao.util.ImageUtils.ProcessedImage;
import org.thingsboard.server.dao.util.JsonNodeProcessingTask;
import org.thingsboard.server.dao.util.JsonPathProcessingTask;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BaseImageService extends BaseResourceService implements ImageService {

    private static final int MAX_ENTITIES_TO_FIND = 10;
    private static final String DEFAULT_CONFIG_TAG = "defaultConfig";

    public static Map<String, String> DASHBOARD_BASE64_MAPPING = new HashMap<>();
    public static Map<String, String> WIDGET_TYPE_BASE64_MAPPING = new HashMap<>();

    static {
        DASHBOARD_BASE64_MAPPING.put("settings.dashboardLogoUrl", "$prefix logo");
        DASHBOARD_BASE64_MAPPING.put("states.default.layouts.main.gridSettings.backgroundImageUrl", "$prefix background");
        DASHBOARD_BASE64_MAPPING.put("states.default.layouts.right.gridSettings.backgroundImageUrl", "$prefix right background");
        DASHBOARD_BASE64_MAPPING.put("states.$stateId.layouts.main.gridSettings.backgroundImageUrl", "$prefix $stateId background");
        DASHBOARD_BASE64_MAPPING.put("states.$stateId.layouts.right.gridSettings.backgroundImageUrl", "$prefix $stateId right background");
        DASHBOARD_BASE64_MAPPING.put("widgets.*.config[$title].settings.backgroundImageUrl", "$prefix widget \"$title\" background");
        DASHBOARD_BASE64_MAPPING.put("widgets.*.config[$title].settings.mapImageUrl", "$prefix widget \"$title\" map image");
        DASHBOARD_BASE64_MAPPING.put("widgets.*.config[$title].settings.markerImage", "$prefix widget \"$title\" marker image");
        DASHBOARD_BASE64_MAPPING.put("widgets.*.config[$title].settings.markerImages", "$prefix widget \"$title\" marker image $index");
        DASHBOARD_BASE64_MAPPING.put("widgets.*.config[$title].settings.background.imageUrl", "$prefix widget \"$title\" background");
        DASHBOARD_BASE64_MAPPING.put("widgets.*.config[$title].settings.background.imageBase64", "$prefix widget \"$title\" background");
        DASHBOARD_BASE64_MAPPING.put("widgets.*.config[$title].datasources.*.dataKeys.*.settings.customIcon", "$prefix widget \"$title\" custom icon");

        WIDGET_TYPE_BASE64_MAPPING.put("settings.backgroundImageUrl", "$prefix background");
        WIDGET_TYPE_BASE64_MAPPING.put("settings.mapImageUrl", "$prefix map image");
        WIDGET_TYPE_BASE64_MAPPING.put("settings.markerImage", "Map marker image");
        WIDGET_TYPE_BASE64_MAPPING.put("settings.markerImages", "Map marker image $index");
        WIDGET_TYPE_BASE64_MAPPING.put("settings.background.imageUrl", "$prefix background");
        WIDGET_TYPE_BASE64_MAPPING.put("settings.background.imageBase64", "$prefix background");
        WIDGET_TYPE_BASE64_MAPPING.put("settings.scadaSymbolUrl", "$prefix SCADA symbol");
        WIDGET_TYPE_BASE64_MAPPING.put("datasources.*.dataKeys.*.settings.customIcon", "$prefix custom icon");
    }

    private final AssetProfileDao assetProfileDao;
    private final DeviceProfileDao deviceProfileDao;
    private final WidgetsBundleDao widgetsBundleDao;
    private final WidgetTypeDao widgetTypeDao;
    private final DashboardInfoDao dashboardInfoDao;
    private final Map<EntityType, ImageContainerDao<?>> imageContainerDaoMap = new HashMap<>();

    public BaseImageService(TbResourceDao resourceDao, TbResourceInfoDao resourceInfoDao, ResourceDataValidator resourceValidator,
                            AssetProfileDao assetProfileDao, DeviceProfileDao deviceProfileDao, WidgetsBundleDao widgetsBundleDao,
                            WidgetTypeDao widgetTypeDao, DashboardInfoDao dashboardInfoDao) {
        super(resourceDao, resourceInfoDao, resourceValidator);
        this.assetProfileDao = assetProfileDao;
        this.deviceProfileDao = deviceProfileDao;
        this.widgetsBundleDao = widgetsBundleDao;
        this.widgetTypeDao = widgetTypeDao;
        this.dashboardInfoDao = dashboardInfoDao;
    }

    @PostConstruct
    public void init() {
        imageContainerDaoMap.put(EntityType.WIDGET_TYPE, widgetTypeDao);
        imageContainerDaoMap.put(EntityType.WIDGETS_BUNDLE, widgetsBundleDao);
        imageContainerDaoMap.put(EntityType.DEVICE_PROFILE, deviceProfileDao);
        imageContainerDaoMap.put(EntityType.ASSET_PROFILE, assetProfileDao);
        imageContainerDaoMap.put(EntityType.DASHBOARD, dashboardInfoDao);
    }


    @Override
    @SneakyThrows
    public TbResourceInfo saveImage(TbResource image) {
        if (image.getId() == null) {
            image.setResourceKey(getUniqueKey(image.getTenantId(), StringUtils.defaultIfEmpty(image.getResourceKey(), image.getFileName())));
        }
        if (image.getResourceSubType() == null) {
            image.setResourceSubType(ResourceSubType.IMAGE);
        }
        resourceValidator.validate(image, TbResourceInfo::getTenantId);

        ImageDescriptor descriptor = image.getDescriptor(ImageDescriptor.class);
        Pair<ImageDescriptor, byte[]> result = processImage(image.getData(), descriptor);
        descriptor = result.getLeft();
        image.setEtag(descriptor.getEtag());
        image.setDescriptorValue(descriptor);
        image.setPreview(result.getRight());

        if (StringUtils.isEmpty(image.getPublicResourceKey()) || (image.getId() == null &&
                resourceInfoDao.existsByPublicResourceKey(ResourceType.IMAGE, image.getPublicResourceKey()))) {
            image.setPublicResourceKey(generatePublicResourceKey());
        }
        log.debug("[{}] Creating image {} ('{}')", image.getTenantId(), image.getResourceKey(), image.getName());
        return new TbResourceInfo(doSaveResource(image));
    }

    private Pair<ImageDescriptor, byte[]> processImage(byte[] data, ImageDescriptor descriptor) throws Exception {
        ProcessedImage image = ImageUtils.processImage(data, descriptor.getMediaType(), 250);
        ProcessedImage preview = image.getPreview();

        descriptor.setWidth(image.getWidth());
        descriptor.setHeight(image.getHeight());
        descriptor.setSize(image.getSize());
        descriptor.setEtag(calculateEtag(data));

        ImageDescriptor previewDescriptor = new ImageDescriptor();
        previewDescriptor.setWidth(preview.getWidth());
        previewDescriptor.setHeight(preview.getHeight());
        previewDescriptor.setMediaType(preview.getMediaType());
        previewDescriptor.setSize(preview.getSize());
        previewDescriptor.setEtag(preview.getData() != null ? calculateEtag(preview.getData()) : descriptor.getEtag());
        descriptor.setPreviewDescriptor(previewDescriptor);

        return Pair.of(descriptor, preview.getData());
    }

    private String getUniqueKey(TenantId tenantId, String filename) {
        if (!resourceInfoDao.existsByTenantIdAndResourceTypeAndResourceKey(tenantId, ResourceType.IMAGE, filename)) {
            return filename;
        }

        String basename = StringUtils.substringBeforeLast(filename, ".");
        String extension = StringUtils.substringAfterLast(filename, ".");

        Set<String> existing = resourceInfoDao.findKeysByTenantIdAndResourceTypeAndResourceKeyPrefix(
                tenantId, ResourceType.IMAGE, basename
        );
        String resourceKey = filename;
        int idx = 1;
        while (existing.contains(resourceKey)) {
            resourceKey = basename + "_(" + idx + ")." + extension;
            idx++;
        }
        log.debug("[{}] Generated unique key {} for image {}", tenantId, resourceKey, filename);
        return resourceKey;
    }

    private String generatePublicResourceKey() {
        return RandomStringUtils.randomAlphanumeric(32);
    }

    @Override
    public TbResourceInfo saveImageInfo(TbResourceInfo imageInfo) {
        log.trace("Executing saveImageInfo [{}] [{}]", imageInfo.getTenantId(), imageInfo.getId());
        return saveResource(new TbResource(imageInfo));
    }

    @Override
    public TbResourceInfo getImageInfoByTenantIdAndKey(TenantId tenantId, String key) {
        log.trace("Executing getImageInfoByTenantIdAndKey [{}] [{}]", tenantId, key);
        return findResourceInfoByTenantIdAndKey(tenantId, ResourceType.IMAGE, key);
    }

    @Override
    public TbResourceInfo getPublicImageInfoByKey(String publicResourceKey) {
        return resourceInfoDao.findPublicResourceByKey(ResourceType.IMAGE, publicResourceKey);
    }

    @Override
    public PageData<TbResourceInfo> getImagesByTenantId(TenantId tenantId, ResourceSubType imageSubType, PageLink pageLink) {
        log.trace("Executing getImagesByTenantId [{}]", tenantId);
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .resourceTypes(Set.of(ResourceType.IMAGE))
                .resourceSubTypes(Set.of(imageSubType))
                .build();
        return findTenantResourcesByTenantId(filter, pageLink);
    }

    @Override
    public PageData<TbResourceInfo> getAllImagesByTenantId(TenantId tenantId, ResourceSubType imageSubType, PageLink pageLink) {
        log.trace("Executing getAllImagesByTenantId [{}]", tenantId);
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .resourceTypes(Set.of(ResourceType.IMAGE))
                .resourceSubTypes(Set.of(imageSubType))
                .build();
        return findAllTenantResourcesByTenantId(filter, pageLink);
    }

    @Override
    public byte[] getImageData(TenantId tenantId, TbResourceId imageId) {
        log.trace("Executing getImageData [{}] [{}]", tenantId, imageId);
        return resourceDao.getResourceData(tenantId, imageId);
    }

    @Override
    public byte[] getImagePreview(TenantId tenantId, TbResourceId imageId) {
        log.trace("Executing getImagePreview [{}] [{}]", tenantId, imageId);
        return resourceDao.getResourcePreview(tenantId, imageId);
    }

    @Override
    public TbImageDeleteResult deleteImage(TbResourceInfo imageInfo, boolean force) {
        var tenantId = imageInfo.getTenantId();
        var imageId = imageInfo.getId();
        log.trace("Executing deleteImage [{}] [{}]", tenantId, imageId);
        Validator.validateId(imageId, id -> INCORRECT_RESOURCE_ID + id);
        TbImageDeleteResult.TbImageDeleteResultBuilder result = TbImageDeleteResult.builder();
        boolean success = true;
        if (!force) {
            var link = DataConstants.TB_IMAGE_PREFIX + imageInfo.getLink();
            Map<String, List<? extends HasId<?>>> affectedEntities = new HashMap<>();
            imageContainerDaoMap.forEach((entityType, imageContainerDao) -> {
                var entities = tenantId.isSysTenantId() ? imageContainerDao.findByImageLink(link, MAX_ENTITIES_TO_FIND) :
                        imageContainerDao.findByTenantAndImageLink(tenantId, link, MAX_ENTITIES_TO_FIND);
                if (!entities.isEmpty()) {
                    affectedEntities.put(entityType.name(), entities);
                }
            });
            if (!affectedEntities.isEmpty()) {
                success = false;
                result.references(affectedEntities);
            }
        }
        if (success) {
            deleteResource(tenantId, imageId, force);
        }
        return result.success(success).build();
    }

    @Override
    public String calculateImageEtag(byte[] imageData) {
        return calculateEtag(imageData);
    }

    @Override
    public TbResourceInfo findSystemOrTenantImageByEtag(TenantId tenantId, String etag) {
        log.trace("Executing findSystemOrTenantImageByEtag [{}] [{}]", tenantId, etag);
        return resourceInfoDao.findSystemOrTenantImageByEtag(tenantId, ResourceType.IMAGE, etag);
    }

    @Transactional(noRollbackFor = Exception.class) // we don't want transaction to rollback in case of an image processing failure
    @Override
    public boolean replaceBase64WithImageUrl(HasImage entity, String type) {
        log.trace("Executing replaceBase64WithImageUrl [{}] [{}] [{}]", entity.getTenantId(), type, entity.getName());
        String imageName = "\"" + entity.getName() + "\" ";
        if (entity.getTenantId() == null || entity.getTenantId().isSysTenantId()) {
            imageName += "system ";
        }
        imageName = imageName + type + " image";

        UpdateResult result = base64ToImageUrl(entity.getTenantId(), imageName, entity.getImage());
        entity.setImage(result.getValue());
        return result.isUpdated();
    }

    @Transactional(noRollbackFor = Exception.class) // we don't want transaction to rollback in case of an image processing failure
    @Override
    public boolean replaceBase64WithImageUrl(WidgetTypeDetails entity) {
        log.trace("Executing replaceBase64WithImageUrl [{}] [WidgetTypeDetails] [{}]", entity.getTenantId(), entity.getId());
        String prefix = "\"" + entity.getName() + "\" ";
        if (entity.getTenantId() == null || entity.getTenantId().isSysTenantId()) {
            prefix += "system ";
        }
        prefix += "widget";
        UpdateResult result = base64ToImageUrl(entity.getTenantId(), prefix + " image", entity.getImage());
        entity.setImage(result.getValue());
        boolean updated = result.isUpdated();
        if (entity.getDescriptor().isObject()) {
            ObjectNode descriptor = (ObjectNode) entity.getDescriptor();
            JsonNode defaultConfig = Optional.ofNullable(descriptor.get(DEFAULT_CONFIG_TAG))
                    .filter(JsonNode::isTextual).map(JsonNode::asText)
                    .map(JacksonUtil::toJsonNode).orElse(null);
            if (defaultConfig != null) {
                updated |= base64ToImageUrlUsingMapping(entity.getTenantId(), WIDGET_TYPE_BASE64_MAPPING, Collections.singletonMap("prefix", prefix), defaultConfig);
                descriptor.put(DEFAULT_CONFIG_TAG, defaultConfig.toString());
            }
        }
        updated |= base64ToImageUrlRecursively(entity.getTenantId(), prefix, entity.getDescriptor());
        return updated;
    }

    @Transactional(noRollbackFor = Exception.class) // we don't want transaction to rollback in case of an image processing failure
    @Override
    public boolean replaceBase64WithImageUrl(Dashboard entity) {
        log.trace("Executing replaceBase64WithImageUrl [{}] [Dashboard] [{}]", entity.getTenantId(), entity.getId());
        String prefix = "\"" + entity.getTitle() + "\" dashboard";
        var result = base64ToImageUrl(entity.getTenantId(), prefix + " image", entity.getImage());
        boolean updated = result.isUpdated();
        entity.setImage(result.getValue());
        updated |= base64ToImageUrlUsingMapping(entity.getTenantId(), DASHBOARD_BASE64_MAPPING, Collections.singletonMap("prefix", prefix), entity.getConfiguration());
        updated |= base64ToImageUrlRecursively(entity.getTenantId(), prefix, entity.getConfiguration());
        return updated;
    }

    private boolean base64ToImageUrlUsingMapping(TenantId tenantId, Map<String, String> mapping, Map<String, String> templateParams, JsonNode configuration) {
        boolean updated = false;
        for (var entry : mapping.entrySet()) {
            String expression = entry.getValue();
            Queue<JsonPathProcessingTask> tasks = new LinkedList<>();
            tasks.add(new JsonPathProcessingTask(entry.getKey().split("\\."), templateParams, configuration));
            while (!tasks.isEmpty()) {
                JsonPathProcessingTask task = tasks.poll();
                String token = task.currentToken();
                JsonNode node = task.getNode();
                if (node == null) {
                    continue;
                }
                if (token.equals("*") || token.startsWith("$")) {
                    String variableName = token.startsWith("$") ? token.substring(1) : null;
                    if (node.isArray()) {
                        ArrayNode childArray = (ArrayNode) node;
                        for (JsonNode element : childArray) {
                            tasks.add(task.next(element));
                        }
                    } else if (node.isObject()) {
                        ObjectNode on = (ObjectNode) node;
                        for (Iterator<Map.Entry<String, JsonNode>> it = on.fields(); it.hasNext(); ) {
                            var kv = it.next();
                            if (variableName != null) {
                                tasks.add(task.next(kv.getValue(), variableName, kv.getKey()));
                            } else {
                                tasks.add(task.next(kv.getValue()));
                            }
                        }
                    }
                } else {
                    String variableName = null;
                    String variableValue = null;
                    if (token.contains("[$")) {
                        variableName = StringUtils.substringBetween(token, "[$", "]");
                        token = StringUtils.substringBefore(token, "[$");
                    }
                    if (node.has(token)) {
                        JsonNode value = node.get(token);
                        if (variableName != null && value.has(variableName) && value.get(variableName).isTextual()) {
                            variableValue = value.get(variableName).asText();
                        }
                        if (task.isLast()) {
                            String name = expression;
                            for (var replacement : task.getVariables().entrySet()) {
                                name = name.replace("$" + replacement.getKey(), Strings.nullToEmpty(replacement.getValue()));
                            }
                            if (node.isObject() && value.isTextual()) {
                                var result = base64ToImageUrl(tenantId, name, value.asText());
                                ((ObjectNode) node).put(token, result.getValue());
                                updated |= result.isUpdated();
                            } else if (value.isArray()) {
                                ArrayNode array = (ArrayNode) value;
                                for (int i = 0; i < array.size(); i++) {
                                    String arrayElementName = name.replace("$index", Integer.toString(i));
                                    UpdateResult result = base64ToImageUrl(tenantId, arrayElementName, array.get(i).asText());
                                    array.set(i, result.getValue());
                                    updated |= result.isUpdated();
                                }
                            }
                        } else {
                            if (StringUtils.isNotEmpty(variableName)) {
                                tasks.add(task.next(value, variableName, variableValue));
                            } else {
                                tasks.add(task.next(value));
                            }
                        }
                    }
                }
            }
        }
        return updated;
    }

    private UpdateResult base64ToImageUrl(TenantId tenantId, String name, String data) {
        return base64ToImageUrl(tenantId, name, data, false);
    }

    private static final Pattern TB_IMAGE_METADATA_PATTERN = Pattern.compile("^tb-image:([^:]*):([^:]*):?([^:]*)?;data:(.*);.*");

    private UpdateResult base64ToImageUrl(TenantId tenantId, String name, String data, boolean strict) {
        if (StringUtils.isBlank(data)) {
            return UpdateResult.of(false, data);
        }
        var matcher = TB_IMAGE_METADATA_PATTERN.matcher(data);
        boolean matches = matcher.matches();
        String mdResourceKey = null;
        String mdResourceName = null;
        String mdResourceSubType = null;
        String mdMediaType;
        if (matches) {
            mdResourceKey = new String(Base64.getDecoder().decode(matcher.group(1)), StandardCharsets.UTF_8);
            mdResourceName = new String(Base64.getDecoder().decode(matcher.group(2)), StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(matcher.group(3))) {
                mdResourceSubType = new String(Base64.getDecoder().decode(matcher.group(3)), StandardCharsets.UTF_8);
            };
            mdMediaType = matcher.group(4);
        } else if (data.startsWith(DataConstants.TB_IMAGE_PREFIX + "data:image/") || (!strict && data.startsWith("data:image/"))) {
            mdMediaType = StringUtils.substringBetween(data, "data:", ";base64");
        } else {
            return UpdateResult.of(false, data);
        }
        String base64Data = StringUtils.substringAfter(data, "base64,");
        String extension = ImageUtils.mediaTypeToFileExtension(mdMediaType);
        byte[] imageData = Base64.getDecoder().decode(base64Data);
        String etag = calculateEtag(imageData);
        var imageInfo = findSystemOrTenantImageByEtag(tenantId, etag);
        if (imageInfo == null) {
            TbResource image = new TbResource();
            image.setTenantId(tenantId);
            image.setResourceType(ResourceType.IMAGE);
            if (StringUtils.isBlank(mdResourceName)) {
                mdResourceName = name;
            }
            image.setTitle(mdResourceName);

            String fileName;
            if (StringUtils.isBlank(mdResourceKey)) {
                fileName = StringUtils.strip(mdResourceName.toLowerCase()
                        .replaceAll("['\"]", "")
                        .replaceAll("[^\\pL\\d]+", "_"), "_") // leaving only letters and numbers
                        + "." + extension;
            } else {
                fileName = mdResourceKey;
            }
            if (StringUtils.isBlank(mdResourceSubType)) {
                image.setResourceSubType(ResourceSubType.IMAGE);
            } else {
                image.setResourceSubType(ResourceSubType.valueOf(mdResourceSubType));
            }
            image.setFileName(fileName);
            image.setDescriptor(JacksonUtil.newObjectNode().put("mediaType", mdMediaType));
            image.setData(imageData);
            image.setPublic(true);
            try {
                imageInfo = saveImage(image);
            } catch (Exception e) {
                if (log.isDebugEnabled()) { // printing stacktrace
                    log.warn("[{}][{}] Failed to replace Base64 with image url for {}", tenantId, name, StringUtils.abbreviate(data, 50), e);
                } else {
                    log.warn("[{}][{}] Failed to replace Base64 with image url for {}: {}", tenantId, name, StringUtils.abbreviate(data, 50), ExceptionUtils.getMessage(e));
                }
                return UpdateResult.of(false, data);
            }
        } else {
            log.debug("[{}] Using existing image {} ({} - '{}') for '{}'", tenantId, imageInfo.getResourceKey(), imageInfo.getTenantId(), imageInfo.getName(), name);
        }
        return UpdateResult.of(true, DataConstants.TB_IMAGE_PREFIX + imageInfo.getLink());
    }

    private boolean base64ToImageUrlRecursively(TenantId tenantId, String title, JsonNode root) {
        boolean updated = false;
        Queue<JsonNodeProcessingTask> tasks = new LinkedList<>();
        tasks.add(new JsonNodeProcessingTask(title, root));
        while (!tasks.isEmpty()) {
            JsonNodeProcessingTask task = tasks.poll();
            JsonNode node = task.getNode();
            if (node == null) {
                continue;
            }
            String currentPath = StringUtils.isBlank(task.getPath()) ? "" : (task.getPath() + " ");
            if (node.isObject()) {
                ObjectNode on = (ObjectNode) node;
                for (Iterator<String> it = on.fieldNames(); it.hasNext(); ) {
                    String childName = it.next();
                    JsonNode childValue = on.get(childName);
                    if (childValue.isTextual()) {
                        UpdateResult result = base64ToImageUrl(tenantId, currentPath + childName, childValue.asText(), true);
                        on.put(childName, result.getValue());
                        updated |= result.isUpdated();
                    } else if (childValue.isObject() || childValue.isArray()) {
                        tasks.add(new JsonNodeProcessingTask(currentPath + childName, childValue));
                    }
                }
            } else if (node.isArray()) {
                ArrayNode childArray = (ArrayNode) node;
                for (int i = 0; i < childArray.size(); i++) {
                    JsonNode element = childArray.get(i);
                    if (element.isObject()) {
                        tasks.add(new JsonNodeProcessingTask(currentPath + " " + i, element));
                    } else if (element.isTextual()) {
                        UpdateResult result = base64ToImageUrl(tenantId, currentPath + "." + i, element.asText(), true);
                        childArray.set(i, result.getValue());
                        updated |= result.isUpdated();
                    }
                }
            }
        }
        return updated;
    }

    @Override
    public void inlineImage(HasImage entity) {
        log.trace("Executing inlineImage [{}] [{}] [{}]", entity.getTenantId(), entity.getClass().getSimpleName(), entity.getName());
        entity.setImage(inlineImage(entity.getTenantId(), "image", entity.getImage(), true));
    }

    @Override
    public void inlineImages(Dashboard dashboard) {
        log.trace("Executing inlineImage [{}] [Dashboard] [{}]", dashboard.getTenantId(), dashboard.getId());
        inlineImage(dashboard);
        inlineIntoJson(dashboard.getTenantId(), dashboard.getConfiguration());
    }

    @Override
    public void inlineImages(WidgetTypeDetails widgetTypeDetails) {
        log.trace("Executing inlineImage [{}] [WidgetTypeDetails] [{}]", widgetTypeDetails.getTenantId(), widgetTypeDetails.getId());
        inlineImage(widgetTypeDetails);
        ObjectNode descriptor = (ObjectNode) widgetTypeDetails.getDescriptor();
        inlineIntoJson(widgetTypeDetails.getTenantId(), descriptor);
        if (descriptor.has(DEFAULT_CONFIG_TAG) && descriptor.get(DEFAULT_CONFIG_TAG).isTextual()) {
            try {
                var defaultConfig = JacksonUtil.toJsonNode(descriptor.get(DEFAULT_CONFIG_TAG).asText());
                inlineIntoJson(widgetTypeDetails.getTenantId(), defaultConfig);
                descriptor.put(DEFAULT_CONFIG_TAG, JacksonUtil.toString(defaultConfig));
            } catch (Exception e) {
                log.debug("[{}][{}] Failed to process default config: ", widgetTypeDetails.getTenantId(), widgetTypeDetails.getId(), e);
            }
        }
    }

    @Override
    public void inlineImageForEdge(HasImage entity) {
        log.trace("Executing inlineImageForEdge [{}] [{}] [{}]", entity.getTenantId(), entity.getClass().getSimpleName(), entity.getName());
        entity.setImage(inlineImage(entity.getTenantId(), "image", entity.getImage(), false));
    }

    @Override
    public void inlineImagesForEdge(Dashboard dashboard) {
        log.trace("Executing inlineImagesForEdge [{}] [Dashboard] [{}]", dashboard.getTenantId(), dashboard.getId());
        inlineImageForEdge(dashboard);
        inlineIntoJson(dashboard.getTenantId(), dashboard.getConfiguration(), false);
    }

    @Override
    public void inlineImagesForEdge(WidgetTypeDetails widgetTypeDetails) {
        log.trace("Executing inlineImage [{}] [WidgetTypeDetails] [{}]", widgetTypeDetails.getTenantId(), widgetTypeDetails.getId());
        inlineImageForEdge(widgetTypeDetails);
        inlineIntoJson(widgetTypeDetails.getTenantId(), widgetTypeDetails.getDescriptor(), false);
    }

    private void inlineIntoJson(TenantId tenantId, JsonNode root) {
        inlineIntoJson(tenantId, root, true);
    }

    private void inlineIntoJson(TenantId tenantId, JsonNode root, boolean addTbImagePrefix) {
        Queue<JsonNodeProcessingTask> tasks = new LinkedList<>();
        tasks.add(new JsonNodeProcessingTask("", root));
        while (!tasks.isEmpty()) {
            JsonNodeProcessingTask task = tasks.poll();
            JsonNode node = task.getNode();
            if (node == null) {
                continue;
            }
            String currentPath = StringUtils.isBlank(task.getPath()) ? "" : (task.getPath() + ".");
            if (node.isObject()) {
                ObjectNode on = (ObjectNode) node;
                for (Iterator<String> it = on.fieldNames(); it.hasNext(); ) {
                    String childName = it.next();
                    JsonNode childValue = on.get(childName);
                    if (childValue.isTextual()) {
                        on.put(childName, inlineImage(tenantId, currentPath + childName, childValue.asText(), addTbImagePrefix));
                    } else if (childValue.isObject() || childValue.isArray()) {
                        tasks.add(new JsonNodeProcessingTask(currentPath + childName, childValue));
                    }
                }
            } else if (node.isArray()) {
                ArrayNode childArray = (ArrayNode) node;
                for (int i = 0; i < childArray.size(); i++) {
                    JsonNode element = childArray.get(i);
                    if (element.isObject()) {
                        tasks.add(new JsonNodeProcessingTask(currentPath + "." + i, element));
                    } else if (element.isTextual()) {
                        childArray.set(i, inlineImage(tenantId, currentPath + "." + i, element.asText(), addTbImagePrefix));
                    }
                }
            }
        }
    }

    private String inlineImage(TenantId tenantId, String path, String url, boolean addTbImagePrefix) {
        try {
            ImageCacheKey key = getKeyFromUrl(tenantId, url);
            if (key != null) {
                var imageInfo = getImageInfoByTenantIdAndKey(key.getTenantId(), key.getResourceKey());
                if (imageInfo != null && !(TenantId.SYS_TENANT_ID.equals(imageInfo.getTenantId()) && ResourceSubType.SCADA_SYMBOL.equals(imageInfo.getResourceSubType()))) {
                    byte[] data = key.isPreview() ? getImagePreview(tenantId, imageInfo.getId()) : getImageData(tenantId, imageInfo.getId());
                    ImageDescriptor descriptor = getImageDescriptor(imageInfo, key.isPreview());
                    String tbImagePrefix = "";
                    if (addTbImagePrefix) {
                        tbImagePrefix = "tb-image:" + Base64.getEncoder().encodeToString(imageInfo.getResourceKey().getBytes(StandardCharsets.UTF_8)) + ":"
                                + Base64.getEncoder().encodeToString(imageInfo.getName().getBytes(StandardCharsets.UTF_8)) + ":"
                                + Base64.getEncoder().encodeToString(imageInfo.getResourceSubType().name().getBytes(StandardCharsets.UTF_8)) + ";";
                    }
                    return tbImagePrefix + "data:" + descriptor.getMediaType() + ";base64," + Base64.getEncoder().encodeToString(data);
                }
            }
        } catch (Exception e) {
            log.warn("[{}][{}][{}] Failed to inline image.", tenantId, path, url, e);
        }
        return url;
    }

    private ImageDescriptor getImageDescriptor(TbResourceInfo imageInfo, boolean preview) throws JsonProcessingException {
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        return preview ? descriptor.getPreviewDescriptor() : descriptor;
    }

    private ImageCacheKey getKeyFromUrl(TenantId tenantId, String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        TenantId imageTenantId = null;
        if (url.startsWith(DataConstants.TB_IMAGE_PREFIX + "/api/images/tenant/")) {
            imageTenantId = tenantId;
        } else if (url.startsWith(DataConstants.TB_IMAGE_PREFIX + "/api/images/system/")) {
            imageTenantId = TenantId.SYS_TENANT_ID;
        }
        if (imageTenantId != null) {
            var parts = url.split("/");
            if (parts.length == 5) {
                return ImageCacheKey.forImage(imageTenantId, parts[4], false);
            } else if (parts.length == 6 && "preview".equals(parts[5])) {
                return ImageCacheKey.forImage(imageTenantId, parts[4], true);
            }
        }
        return null;
    }

    @Data(staticConstructor = "of")
    private static class UpdateResult {
        private final boolean updated;
        private final String value;
    }
}
