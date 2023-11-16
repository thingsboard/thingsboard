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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;
import org.thingsboard.server.dao.util.ImageUtils;
import org.thingsboard.server.dao.util.ImageUtils.ProcessedImage;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BaseImageService extends BaseResourceService implements ImageService {

    public BaseImageService(TbResourceDao resourceDao, TbResourceInfoDao resourceInfoDao, ResourceDataValidator resourceValidator) {
        super(resourceDao, resourceInfoDao, resourceValidator);
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
    public void deleteImage(TenantId tenantId, TbResourceId imageId) {
        deleteResource(tenantId, imageId);
    }

    @Override
    public String getImageLink(TbResourceInfo imageInfo) {
        String link = "/api/images/";
        if (imageInfo.getTenantId().isSysTenantId()) {
            link += "system/";
        } else {
            link += "tenant/";
        }
        link += imageInfo.getResourceKey();
        return link;
    }

}
