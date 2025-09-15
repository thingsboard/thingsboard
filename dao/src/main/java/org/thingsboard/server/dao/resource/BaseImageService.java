/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
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
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.ResourceExportData;
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
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;
import org.thingsboard.server.dao.util.ImageUtils;
import org.thingsboard.server.dao.util.ImageUtils.ProcessedImage;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.ArrayUtils.get;
import static org.thingsboard.server.common.data.StringUtils.isNotEmpty;

@Service
@Slf4j
public class BaseImageService extends BaseResourceService implements ImageService {

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
    private final Map<EntityType, ImageContainerDao<?>> imageContainerDaoMap = new HashMap<>();

    public BaseImageService(TbResourceDao resourceDao, TbResourceInfoDao resourceInfoDao, ResourceDataValidator resourceValidator,
                            AssetProfileDao assetProfileDao, DeviceProfileDao deviceProfileDao, WidgetsBundleDao widgetsBundleDao,
                            WidgetTypeDao widgetTypeDao, DashboardInfoDao dashboardInfoDao, RuleChainDao ruleChainDao) {
        super(resourceDao, resourceInfoDao, resourceValidator, widgetTypeDao, dashboardInfoDao, ruleChainDao);
        this.assetProfileDao = assetProfileDao;
        this.deviceProfileDao = deviceProfileDao;
        this.widgetsBundleDao = widgetsBundleDao;
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
            image.setResourceKey(getUniqueKey(image.getTenantId(), ResourceType.IMAGE, StringUtils.defaultIfEmpty(image.getResourceKey(), image.getFileName())));
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
        return getResourceData(tenantId, imageId);
    }

    @Override
    public byte[] getImagePreview(TenantId tenantId, TbResourceId imageId) {
        log.trace("Executing getImagePreview [{}] [{}]", tenantId, imageId);
        return resourceDao.getResourcePreview(tenantId, imageId);
    }

    @Override
    public ResourceExportData exportImage(TbResourceInfo imageInfo) {
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        byte[] data = getImageData(imageInfo.getTenantId(), imageInfo.getId());
        return ResourceExportData.builder()
                .link(imageInfo.getLink())
                .mediaType(descriptor.getMediaType())
                .fileName(imageInfo.getFileName())
                .title(imageInfo.getTitle())
                .type(ResourceType.IMAGE)
                .subType(imageInfo.getResourceSubType())
                .resourceKey(imageInfo.getResourceKey())
                .isPublic(imageInfo.isPublic())
                .publicResourceKey(imageInfo.getPublicResourceKey())
                .data(Base64.getEncoder().encodeToString(data))
                .build();
    }

    @Override
    public TbResource toImage(TenantId tenantId, ResourceExportData imageData, boolean checkExisting) {
        byte[] data = Base64.getDecoder().decode(imageData.getData());
        if (checkExisting) {
            String etag = calculateImageEtag(data);
            TbResourceInfo existingImage = findSystemOrTenantImageByEtag(tenantId, etag);
            if (existingImage != null) {
                log.debug("[{}] Using existing image {}", tenantId, existingImage.getLink());
                return new TbResource(existingImage);
            }
        }

        TbResource image = new TbResource();
        image.setTenantId(tenantId);
        image.setFileName(imageData.getFileName());
        if (isNotEmpty(imageData.getTitle())) {
            image.setTitle(imageData.getTitle());
        } else {
            image.setTitle(imageData.getFileName());
        }
        if (imageData.getSubType() != null) {
            image.setResourceSubType(imageData.getSubType());
        } else {
            image.setResourceSubType(ResourceSubType.IMAGE);
        }
        image.setResourceType(ResourceType.IMAGE);
        image.setResourceKey(imageData.getResourceKey());
        image.setPublic(imageData.isPublic());
        image.setPublicResourceKey(imageData.getPublicResourceKey());
        ImageDescriptor descriptor = new ImageDescriptor();
        descriptor.setMediaType(imageData.getMediaType());
        image.setDescriptorValue(descriptor);
        image.setData(data);
        log.debug("[{}] Creating image {}", tenantId, image.getFileName());
        return image;
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
            success = deleteResource(tenantId, imageId, true)
                    .isSuccess();
        }
        return result.success(success).build();
    }

    @Override
    public TbResourceInfo createOrUpdateSystemImage(String resourceKey, byte[] data) {
        TbResource image;
        TbResourceInfo existingImage = findResourceInfoByTenantIdAndKey(TenantId.SYS_TENANT_ID, ResourceType.IMAGE, resourceKey);
        if (existingImage != null) {
            image = new TbResource(existingImage);
        } else {
            image = new TbResource();
            image.setTenantId(TenantId.SYS_TENANT_ID);
            image.setFileName(resourceKey);
            image.setTitle(resourceKey);
            image.setResourceKey(resourceKey);
            image.setResourceType(ResourceType.IMAGE);
            image.setResourceSubType(ResourceSubType.IMAGE);
        }
        ImageDescriptor descriptor = new ImageDescriptor();
        descriptor.setMediaType(ImageUtils.fileExtensionToMediaType(StringUtils.substringAfterLast(resourceKey, ".")));
        image.setDescriptorValue(descriptor);
        image.setData(data);
        image.setPublic(true);
        log.debug("{} system image {}", (image.getId() == null ? "Creating" : "Updating"), resourceKey);
        return saveImage(image);
    }

    @Override
    public String calculateImageEtag(byte[] imageData) {
        return calculateEtag(imageData);
    }

    @Override
    public TbResourceInfo findSystemOrTenantImageByEtag(TenantId tenantId, String etag) {
        return findSystemOrTenantResourceByEtag(tenantId, ResourceType.IMAGE, etag);
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

        UpdateResult result = convertToImageUrl(entity.getTenantId(), imageName, entity.getImage(), Collections.emptyMap());
        entity.setImage(result.getValue());
        return result.isUpdated();
    }

    @Transactional(noRollbackFor = Exception.class) // we don't want transaction to rollback in case of an image processing failure
    @Override
    public boolean updateImagesUsage(WidgetTypeDetails widgetTypeDetails) {
        TenantId tenantId = widgetTypeDetails.getTenantId();
        log.trace("Executing updateImagesUsage [{}] [WidgetTypeDetails] [{}]", tenantId, widgetTypeDetails.getId());
        String prefix = "\"" + widgetTypeDetails.getName() + "\" ";
        if (tenantId == null || tenantId.isSysTenantId()) {
            prefix += "system ";
        }
        prefix += "widget";
        Map<String, String> imagesLinks = getResourcesLinks(widgetTypeDetails.getResources());

        UpdateResult result = convertToImageUrl(tenantId, prefix + " image", widgetTypeDetails.getImage(), imagesLinks);
        boolean updated = result.isUpdated();
        widgetTypeDetails.setImage(result.getValue());

        if (widgetTypeDetails.getDescriptor().isObject()) {
            JsonNode defaultConfig = widgetTypeDetails.getDefaultConfig();
            if (defaultConfig != null) {
                updated |= convertToImageUrlsByMapping(tenantId, WIDGET_TYPE_BASE64_MAPPING, Collections.singletonMap("prefix", prefix), defaultConfig, imagesLinks);
                updated |= convertToImageUrls(tenantId, prefix, defaultConfig, imagesLinks);
                widgetTypeDetails.setDefaultConfig(defaultConfig);
            }
        }
        updated |= convertToImageUrls(tenantId, prefix, widgetTypeDetails.getDescriptor(), imagesLinks);
        return updated;
    }

    @Transactional(noRollbackFor = Exception.class) // we don't want transaction to rollback in case of an image processing failure
    @Override
    public boolean updateImagesUsage(Dashboard dashboard) {
        TenantId tenantId = dashboard.getTenantId();
        log.trace("Executing updateImagesUsage [{}] [Dashboard] [{}]", tenantId, dashboard.getId());
        String prefix = "\"" + dashboard.getTitle() + "\" dashboard";
        Map<String, String> imagesLinks = getResourcesLinks(dashboard.getResources());

        var result = convertToImageUrl(tenantId, prefix + " image", dashboard.getImage(), imagesLinks);
        boolean updated = result.isUpdated();
        dashboard.setImage(result.getValue());

        updated |= convertToImageUrlsByMapping(tenantId, DASHBOARD_BASE64_MAPPING, Collections.singletonMap("prefix", prefix), dashboard.getConfiguration(), imagesLinks);
        updated |= convertToImageUrls(tenantId, prefix, dashboard.getConfiguration(), imagesLinks);
        return updated;
    }

    private boolean convertToImageUrlsByMapping(TenantId tenantId, Map<String, String> mapping, Map<String, String> templateParams, JsonNode configuration, Map<String, String> links) {
        AtomicBoolean updated = new AtomicBoolean(false);
        JacksonUtil.replaceAllByMapping(configuration, mapping, templateParams, (name, value) -> {
            UpdateResult result = convertToImageUrl(tenantId, name, value, links);
            if (result.isUpdated()) {
                updated.set(true);
            }
            return result.getValue();
        });
        return updated.get();
    }

    private UpdateResult convertToImageUrl(TenantId tenantId, String name, String data, Map<String, String> links) {
        return convertToImageUrl(tenantId, name, data, false, links);
    }

    public static final Pattern TB_IMAGE_METADATA_PATTERN = Pattern.compile("^tb-image:([^;]+);data:(.*);.*");

    private UpdateResult convertToImageUrl(TenantId tenantId, String name, String data, boolean strict, Map<String, String> imagesLinks) {
        if (StringUtils.isBlank(data)) {
            return UpdateResult.of(false, data);
        }

        String link = getImageLink(data);
        if (link != null) {
            String newLink = imagesLinks.get(link);
            if (newLink == null || newLink.equals(link)) {
                return UpdateResult.of(false, data);
            } else {
                return UpdateResult.of(true, DataConstants.TB_IMAGE_PREFIX + newLink);
            }
        }

        String resourceKey = null;
        String resourceName = null;
        String resourceSubType = null;
        String etag = null;
        String mediaType;
        var matcher = TB_IMAGE_METADATA_PATTERN.matcher(data);
        if (matcher.matches()) {
            String[] metadata = matcher.group(1).split(":");
            resourceKey = decode(get(metadata, 0));
            resourceName = decode(get(metadata, 1));
            resourceSubType = decode(get(metadata, 2));
            etag = get(metadata, 3);
            mediaType = matcher.group(2);
        } else if (data.startsWith(DataConstants.TB_IMAGE_PREFIX + "data:image/") || (!strict && data.startsWith("data:image/"))) {
            mediaType = StringUtils.substringBetween(data, "data:", ";base64");
        } else {
            return UpdateResult.of(false, data);
        }

        String base64Data = StringUtils.substringAfter(data, "base64,");
        byte[] imageData = StringUtils.isNotEmpty(base64Data) ? Base64.getDecoder().decode(base64Data) : null;
        if (StringUtils.isBlank(etag)) {
            etag = calculateEtag(imageData);
        }
        var imageInfo = findSystemOrTenantImageByEtag(tenantId, etag);
        if (imageInfo == null) {
            if (imageData == null) {
                return UpdateResult.of(false, data);
            }
            TbResource image = new TbResource();
            image.setTenantId(tenantId);
            image.setResourceType(ResourceType.IMAGE);
            if (StringUtils.isBlank(resourceName)) {
                resourceName = name;
            }
            image.setTitle(resourceName);

            String fileName;
            if (StringUtils.isBlank(resourceKey)) {
                String extension = ImageUtils.mediaTypeToFileExtension(mediaType);
                fileName = StringUtils.strip(resourceName.toLowerCase()
                        .replaceAll("['\"]", "")
                        .replaceAll("[^\\pL\\d]+", "_"), "_") // leaving only letters and numbers
                        + "." + extension;
            } else {
                fileName = resourceKey;
            }
            if (StringUtils.isBlank(resourceSubType)) {
                image.setResourceSubType(ResourceSubType.IMAGE);
            } else {
                image.setResourceSubType(ResourceSubType.valueOf(resourceSubType));
            }
            image.setFileName(fileName);
            image.setDescriptor(JacksonUtil.newObjectNode().put("mediaType", mediaType));
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

    private boolean convertToImageUrls(TenantId tenantId, String title, JsonNode root, Map<String, String> links) {
        AtomicBoolean updated = new AtomicBoolean(false);
        JacksonUtil.replaceAll(root, title, (path, value) -> {
            UpdateResult result = convertToImageUrl(tenantId, path, value, true, links);
            if (result.isUpdated()) {
                updated.set(true);
            }
            return result.getValue();
        });
        return updated.get();
    }

    @Override
    public <T extends HasImage> T inlineImage(T entity) {
        log.trace("Executing inlineImage [{}] [{}] [{}]", entity.getTenantId(), entity.getClass().getSimpleName(), entity.getName());
        if (StringUtils.isEmpty(entity.getImage())) {
            return entity;
        }
        if (cache instanceof CaffeineTbTransactionalCache) {
            entity = JacksonUtil.clone(entity); // cloning the entity to avoid updating the cached one
        }
        entity.setImage(inlineImage(entity.getTenantId(), "image", entity.getImage(), true));
        return entity;
    }

    @Override
    public Collection<TbResourceInfo> getUsedImages(Dashboard dashboard) {
        TenantId tenantId = dashboard.getTenantId();
        log.trace("Executing getUsedImages [{}] [Dashboard] [{}]", tenantId, dashboard.getId());
        Map<TbResourceId, TbResourceInfo> images = new HashMap<>();
        processImage(tenantId, "image", dashboard.getImage(), (key, imageInfo) -> {
            images.putIfAbsent(imageInfo.getId(), imageInfo);
            return null; // leaving the url as is
        });
        processImages(tenantId, dashboard.getConfiguration(), (key, imageInfo) -> {
            images.putIfAbsent(imageInfo.getId(), imageInfo);
            return null; // leaving the url as is
        });
        return images.values();
    }

    @Override
    public Collection<TbResourceInfo> getUsedImages(WidgetTypeDetails widgetTypeDetails) {
        TenantId tenantId = widgetTypeDetails.getTenantId();
        log.trace("Executing getUsedImages [{}] [WidgetTypeDetails] [{}]", tenantId, widgetTypeDetails.getId());
        Map<TbResourceId, TbResourceInfo> images = new HashMap<>();
        processImage(tenantId, "image", widgetTypeDetails.getImage(), (key, imageInfo) -> {
            images.putIfAbsent(imageInfo.getId(), imageInfo);
            return null; // leaving the url as is
        });
        processImages(tenantId, widgetTypeDetails.getDescriptor(), (key, imageInfo) -> {
            images.putIfAbsent(imageInfo.getId(), imageInfo);
            return null; // leaving the url as is
        });
        JsonNode defaultConfig = widgetTypeDetails.getDefaultConfig();
        if (defaultConfig != null) {
            processImages(tenantId, defaultConfig, (key, imageInfo) -> {
                images.putIfAbsent(imageInfo.getId(), imageInfo);
                return null; // leaving the url as is
            });
        }
        return images.values();
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
        inlineImages(dashboard.getTenantId(), dashboard.getConfiguration(), false);
    }

    @Override
    public void inlineImagesForEdge(WidgetTypeDetails widgetTypeDetails) {
        log.trace("Executing inlineImage [{}] [WidgetTypeDetails] [{}]", widgetTypeDetails.getTenantId(), widgetTypeDetails.getId());
        inlineImageForEdge(widgetTypeDetails);
        inlineImages(widgetTypeDetails.getTenantId(), widgetTypeDetails.getDescriptor(), false);
    }

    private void inlineImages(TenantId tenantId, JsonNode root, boolean addTbImagePrefix) {
        processImages(tenantId, root, (key, imageInfo) -> {
            return inlineImage(key, imageInfo, addTbImagePrefix);
        });
    }

    private String inlineImage(TenantId tenantId, String path, String url, boolean addTbImagePrefix) {
        return processImage(tenantId, path, url, (key, imageInfo) -> {
            return inlineImage(key, imageInfo, addTbImagePrefix);
        });
    }

    private String inlineImage(ImageCacheKey key, TbResourceInfo imageInfo, boolean addTbImagePrefix) {
        String value = "";
        if (addTbImagePrefix) {
            value = "tb-image:" + encode(imageInfo.getResourceKey()) + ":"
                    + encode(imageInfo.getName()) + ":"
                    + encode(imageInfo.getResourceSubType().name()) + ";";
        }

        ImageDescriptor descriptor = getImageDescriptor(imageInfo, key.isPreview());
        byte[] data = key.isPreview() ? getImagePreview(key.getTenantId(), imageInfo.getId()) : getImageData(key.getTenantId(), imageInfo.getId());
        return value + "data:" + descriptor.getMediaType() + ";base64," + encode(data);
    }

    private void processImages(TenantId tenantId, JsonNode node, BiFunction<ImageCacheKey, TbResourceInfo, String> processor) {
        JacksonUtil.replaceAll(node, "", (path, value) -> {
            return processImage(tenantId, path, value, processor);
        });
    }

    private String processImage(TenantId tenantId, String path, String imageUrl, BiFunction<ImageCacheKey, TbResourceInfo, String> processor) {
        try {
            ImageCacheKey key = getKeyFromUrl(tenantId, imageUrl);
            if (key != null) {
                var imageInfo = getImageInfoByTenantIdAndKey(key.getTenantId(), key.getResourceKey());
                if (imageInfo == null) {
                    return imageUrl;
                } else {
                    String result = processor.apply(key, imageInfo);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[{}][{}][{}] Failed to process image", tenantId, path, imageUrl, e);
        }
        return imageUrl;
    }

    private ImageDescriptor getImageDescriptor(TbResourceInfo imageInfo, boolean preview) {
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        return preview ? descriptor.getPreviewDescriptor() : descriptor;
    }

    private ImageCacheKey getKeyFromUrl(TenantId tenantId, String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        String link = getImageLink(url);
        if (link == null) {
            return null;
        }

        TenantId imageTenantId = null;
        if (link.startsWith("/api/images/tenant/")) {
            imageTenantId = tenantId;
        } else if (link.startsWith("/api/images/system/")) {
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

    private String getImageLink(String value) {
        if (value.startsWith(DataConstants.TB_IMAGE_PREFIX + "/api/images")) {
            return StringUtils.removeStart(value, DataConstants.TB_IMAGE_PREFIX);
        } else {
            return null;
        }
    }

    @Data(staticConstructor = "of")
    private static class UpdateResult {
        private final boolean updated;
        private final String value;
    }

}
