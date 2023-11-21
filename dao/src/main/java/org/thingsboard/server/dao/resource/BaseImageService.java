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
package org.thingsboard.server.dao.resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ImageDescriptor;
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
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.ImageContainerDao;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;
import org.thingsboard.server.dao.util.ImageUtils;
import org.thingsboard.server.dao.util.ImageUtils.ProcessedImage;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BaseImageService extends BaseResourceService implements ImageService {

    private static final int MAX_ENTITIES_TO_FIND = 10;
    @Autowired
    private AssetProfileDao assetProfileDao;
    @Autowired
    private DeviceProfileDao deviceProfileDao;
    @Autowired
    private WidgetsBundleDao widgetsBundleDao;
    @Autowired
    private WidgetTypeDao widgetTypeDao;
    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    private final Map<EntityType, ImageContainerDao<?>> imageContainerDaoMap = new HashMap<>();

    public BaseImageService(TbResourceDao resourceDao, TbResourceInfoDao resourceInfoDao, ResourceDataValidator resourceValidator) {
        super(resourceDao, resourceInfoDao, resourceValidator);
    }

    @PostConstruct
    public void init() {
        imageContainerDaoMap.put(EntityType.WIDGET_TYPE, widgetTypeDao);
        imageContainerDaoMap.put(EntityType.WIDGETS_BUNDLE, widgetsBundleDao);
        imageContainerDaoMap.put(EntityType.DEVICE_PROFILE, deviceProfileDao);
        imageContainerDaoMap.put(EntityType.ASSET_PROFILE, assetProfileDao);
        imageContainerDaoMap.put(EntityType.DASHBOARD, dashboardInfoDao);
    }


    @Transactional
    @Override
    public TbResourceInfo saveImage(TbResource image) throws Exception {
        if (image.getId() == null) {
            image.setResourceKey(getUniqueKey(image.getTenantId(), image.getFileName()));
        }
        resourceValidator.validate(image, TbResourceInfo::getTenantId);

        ImageDescriptor descriptor = image.getDescriptor(ImageDescriptor.class);
        Pair<ImageDescriptor, byte[]> result = processImage(image.getData(), descriptor);
        descriptor = result.getLeft();
        image.setEtag(descriptor.getEtag());
        image.setDescriptorValue(descriptor);
        image.setPreview(result.getRight());

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

        Pattern similarImagesPattern = Pattern.compile(
                Pattern.quote(basename) + "_(\\d+)\\.?" + Pattern.quote(extension)
        );
        int maxImageIdx = resourceInfoDao.findKeysByTenantIdAndResourceTypeAndResourceKeyStartingWith(
                        tenantId, ResourceType.IMAGE, basename + "_").stream()
                .map(key -> RegexUtils.getMatch(key, similarImagesPattern, 1))
                .filter(Objects::nonNull).mapToInt(Integer::parseInt)
                .max().orElse(0);
        String uniqueKey = basename + "_" + (maxImageIdx + 1);
        if (!extension.isEmpty()) {
            uniqueKey += "." + extension;
        }
        return uniqueKey;
    }

    @Override
    public TbResourceInfo saveImageInfo(TbResourceInfo imageInfo) {
        return saveResource(new TbResource(imageInfo));
    }

    @Override
    public TbResourceInfo getImageInfoByTenantIdAndKey(TenantId tenantId, String key) {
        return findResourceInfoByTenantIdAndKey(tenantId, ResourceType.IMAGE, key);
    }

    @Override
    public PageData<TbResourceInfo> getImagesByTenantId(TenantId tenantId, PageLink pageLink) {
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .resourceTypes(Set.of(ResourceType.IMAGE))
                .build();
        return findTenantResourcesByTenantId(filter, pageLink);
    }

    @Override
    public PageData<TbResourceInfo> getAllImagesByTenantId(TenantId tenantId, PageLink pageLink) {
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .resourceTypes(Set.of(ResourceType.IMAGE))
                .build();
        return findAllTenantResourcesByTenantId(filter, pageLink);
    }

    @Override
    public byte[] getImageData(TenantId tenantId, TbResourceId imageId) {
        return resourceDao.getResourceData(tenantId, imageId);
    }

    @Override
    public byte[] getImagePreview(TenantId tenantId, TbResourceId imageId) {
        return resourceDao.getResourcePreview(tenantId, imageId);
    }

    @Override
    public TbImageDeleteResult deleteImage(TbResourceInfo imageInfo, boolean force) {
        var tenantId = imageInfo.getTenantId();
        var imageId = imageInfo.getId();
        log.trace("Executing deleteImage [{}] [{}]", tenantId, imageId);
        Validator.validateId(imageId, INCORRECT_RESOURCE_ID + imageId);
        TbImageDeleteResult.TbImageDeleteResultBuilder result = TbImageDeleteResult.builder();
        boolean success = true;
        if (!force) {
            var link = imageInfo.getLink();
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
                result.affectedEntities(affectedEntities);
            }
        }
        if (success) {
            deleteResource(tenantId, imageId, force);
        }
        return result.success(success).build();
    }

    @Override
    public List<TbResourceInfo> findSimilarImagesByTenantIdAndKeyStartingWith(TenantId tenantId, byte[] data, String imageKeyStartingWith) {
        String etag = calculateEtag(data);
        return resourceInfoDao.findByTenantIdAndEtagAndKeyStartingWith(tenantId, etag, imageKeyStartingWith);
    }

}
