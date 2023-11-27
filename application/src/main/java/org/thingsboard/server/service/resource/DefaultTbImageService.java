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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
@TbCoreComponent
public class DefaultTbImageService extends AbstractTbEntityService implements TbImageService {

    private final TbClusterService clusterService;
    private final ImageService imageService;
    private final Cache<ImageCacheKey, String> etagCache;

    public DefaultTbImageService(TbClusterService clusterService, ImageService imageService,
                                 @Value("${cache.image.etag.timeToLiveInMinutes:44640}") int cacheTtl,
                                 @Value("${cache.image.etag.maxSize:10000}") int cacheMaxSize) {
        this.clusterService = clusterService;
        this.imageService = imageService;
        this.etagCache = Caffeine.newBuilder()
                .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
    }

    @Override
    public String getETag(ImageCacheKey imageCacheKey) {
        return etagCache.getIfPresent(imageCacheKey);
    }

    @Override
    public void putETag(ImageCacheKey imageCacheKey, String etag) {
        etagCache.put(imageCacheKey, etag);
    }

    @Override
    public void evictETag(ImageCacheKey imageCacheKey) {
        etagCache.invalidate(imageCacheKey);
    }

    @Override
    public TbResourceInfo save(TbResource image, User user) throws Exception {
        ActionType actionType = image.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = image.getTenantId();
        try {
            var oldEtag = getEtag(image);
            if (image.getId() == null && StringUtils.isNotEmpty(image.getResourceKey())) {
                var existingImage = imageService.getImageInfoByTenantIdAndKey(tenantId, image.getResourceKey());
                if (existingImage != null) {
                    image.setId(existingImage.getId());
                }
            }
            TbResourceInfo savedImage = imageService.saveImage(image);
            notificationEntityService.logEntityAction(tenantId, savedImage.getId(), savedImage, actionType, user);
            if (oldEtag.isPresent()) {
                var newEtag = getEtag(savedImage);
                if (newEtag.isPresent() && !oldEtag.get().equals(newEtag.get())) {
                    evictETag(new ImageCacheKey(image.getTenantId(), image.getResourceKey(), false));
                    evictETag(new ImageCacheKey(image.getTenantId(), image.getResourceKey(), true));
                    clusterService.broadcastToCore(TransportProtos.ToCoreNotificationMsg.newBuilder()
                            .setResourceCacheInvalidateMsg(TransportProtos.ResourceCacheInvalidateMsg.newBuilder()
                                    .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                                    .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                                    .setResourceKey(image.getResourceKey())
                                    .build())
                            .build());
                }
            }
            return savedImage;
        } catch (Exception e) {
            image.setData(null);
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.TB_RESOURCE), image, actionType, user, e);
            throw e;
        }
    }

    private Optional<String> getEtag(TbResourceInfo image) throws JsonProcessingException {
        var descriptor = image.getDescriptor(ImageDescriptor.class);
        return Optional.ofNullable(descriptor != null ? descriptor.getEtag() : null);
    }

    private Optional<String> getPreviewEtag(TbResourceInfo image) throws JsonProcessingException {
        var descriptor = image.getDescriptor(ImageDescriptor.class);
        descriptor = descriptor != null ? descriptor.getPreviewDescriptor() : null;
        return Optional.ofNullable(descriptor != null ? descriptor.getEtag() : null);
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
    public TbImageDeleteResult delete(TbResourceInfo imageInfo, User user, boolean force) {
        TenantId tenantId = imageInfo.getTenantId();
        TbResourceId imageId = imageInfo.getId();
        try {
            TbImageDeleteResult result = imageService.deleteImage(imageInfo, force);
            if (result.isSuccess()) {
                notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.DELETED, user, imageId.toString());
            }
            return result;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, imageId, ActionType.DELETED, user, e, imageId.toString());
            throw e;
        }
    }

    @Override
    public void inlineImages(Dashboard entity) {
        var tenantId = entity.getTenantId();
        entity.setImage(inlineImage(tenantId, "image", entity.getImage()));
        inlineIntoJson(tenantId, entity.getConfiguration());
    }

    @Override
    public void inlineImages(WidgetTypeDetails entity) {
        var tenantId = entity.getTenantId();
        entity.setImage(inlineImage(tenantId, "image", entity.getImage()));
        inlineIntoJson(tenantId, entity.getDescriptor());
    }

    @Override
    public void inlineImages(WidgetsBundle entity) {
        entity.setImage(inlineImage(entity.getTenantId(), "image", entity.getImage()));
    }

    @Override
    public void inlineImages(AssetProfile entity) {
        entity.setImage(inlineImage(entity.getTenantId(), "image", entity.getImage()));
    }

    @Override
    public void inlineImages(DeviceProfile entity) {
        entity.setImage(inlineImage(entity.getTenantId(), "image", entity.getImage()));
    }

    private void inlineIntoJson(TenantId tenantId, JsonNode root) {
        Queue<JsonNodeProcessingTask> tasks = new LinkedList<>();
        tasks.add(new JsonNodeProcessingTask("", root));
        while (!tasks.isEmpty()) {
            JsonNodeProcessingTask task = tasks.poll();
            JsonNode node = task.node;
            String currentPath = StringUtils.isBlank(task.path) ? "" : (task.path + ".");
            if (node.isObject()) {
                ObjectNode on = (ObjectNode) node;
                for (Iterator<String> it = on.fieldNames(); it.hasNext(); ) {
                    String childName = it.next();
                    JsonNode childValue = on.get(childName);
                    if (childValue.isTextual()) {
                        on.put(childName, inlineImage(tenantId, currentPath + childName, childValue.asText()));
                    } else if (childValue.isObject() || childValue.isArray()) {
                        tasks.add(new JsonNodeProcessingTask(currentPath + childName, childValue));
                    }
                }
            } else if (node.isArray()) {
                ArrayNode childArray = (ArrayNode) node;
                int i = 0;
                for (JsonNode element : childArray) {
                    if (element.isObject()) {
                        tasks.add(new JsonNodeProcessingTask(currentPath + "." + i, element));
                    }
                    i++;
                }
            }
        }
    }

    private static class JsonNodeProcessingTask {
        private final String path;
        private final JsonNode node;

        public JsonNodeProcessingTask(String path, JsonNode node) {
            this.path = path;
            this.node = node;
        }
    }

    private String inlineImage(TenantId tenantId, String path, String url) {
        try {
            ImageCacheKey key = getKeyFromUrl(tenantId, url);
            if (key != null) {
                var imageInfo = imageService.getImageInfoByTenantIdAndKey(key.getTenantId(), key.getKey());
                if (imageInfo != null) {
                    byte[] data = key.isPreview() ? imageService.getImagePreview(tenantId, imageInfo.getId()) : imageService.getImageData(tenantId, imageInfo.getId());
                    ImageDescriptor descriptor = getImageDescriptor(imageInfo, key.isPreview());
                    return "data:" + descriptor.getMediaType() + ";base64," + Base64Utils.encodeToString(data);
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
        if (url.startsWith("/api/images/tenant/")) {
            imageTenantId = tenantId;
        } else if (url.startsWith("/api/images/system/")) {
            imageTenantId = TenantId.SYS_TENANT_ID;
        }
        if (imageTenantId != null) {
            var parts = url.split("/");
            if (parts.length == 5) {
                return new ImageCacheKey(imageTenantId, parts[4], false);
            } else if (parts.length == 6 && "preview".equals(parts[5])) {
                return new ImageCacheKey(imageTenantId, parts[4], true);
            }
        }
        return null;
    }
}
