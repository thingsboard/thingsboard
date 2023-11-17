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
package org.thingsboard.server.service.resource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
public class DefaultTbImageService extends AbstractTbEntityService implements TbImageService {

    private final ImageService imageService;
    private final Cache<ImageCacheKey, String> cache;

    public DefaultTbImageService(ImageService imageService,
                                 @Value("${cache.imageETags.timeToLiveInMinutes:120}") int cacheTtl,
                                 @Value("${cache.imageETags.maxSize:200000}") int cacheMaxSize) {
        this.imageService = imageService;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
    }

    @Override
    public String getETag(ImageCacheKey imageCacheKey) {
        return cache.getIfPresent(imageCacheKey);
    }

    @Override
    public void putETag(ImageCacheKey imageCacheKey, String etag) {
        cache.put(imageCacheKey, etag);
    }

    @Override
    public TbResourceInfo save(TbResource image, User user) throws Exception {
        ActionType actionType = image.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = image.getTenantId();
        try {
            TbResourceInfo savedImage = imageService.saveImage(image);
            notificationEntityService.logEntityAction(tenantId, savedImage.getId(), savedImage, actionType, user);
            return savedImage;
        } catch (Exception e) {
            image.setData(null);
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.TB_RESOURCE), image, actionType, user, e);
            throw e;
        }
    }

    @Override
    public TbResourceInfo save(TbResourceInfo imageInfo, User user) {
        TenantId tenantId = imageInfo.getTenantId();
        TbResourceId imageId = imageInfo.getId();
        try {
            imageInfo = imageService.saveImageInfo(imageInfo);
            notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.UPDATED, user);
            return imageInfo;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public void delete(TbResourceInfo imageInfo, User user) {
        TenantId tenantId = imageInfo.getTenantId();
        TbResourceId imageId = imageInfo.getId();
        try {
            imageService.deleteImage(tenantId, imageId);
            notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.DELETED, user, imageId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, imageId, ActionType.DELETED, user, e, imageId.toString());
            throw e;
        }
    }

}
