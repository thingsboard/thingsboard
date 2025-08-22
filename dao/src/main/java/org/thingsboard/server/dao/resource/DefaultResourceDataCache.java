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

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.resourceInfo.ResourceInfoEvictEvent;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultResourceDataCache implements ResourceDataCache {

    private final ResourceService resourceService;
    private final JpaExecutorService executorService;

    @Value("${cache.tbResourceData.maxSize:10000}")
    private int cacheMaxSize;
    @Value("${cache.tbResourceData.timeToLiveInMinutes:44640}")
    private int cacheValueTtl;
    private AsyncLoadingCache<ResourceDataKey, byte[]> cache;

    @PostConstruct
    private void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterAccess(cacheValueTtl, TimeUnit.MINUTES)
                .executor(executorService)
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return resourceService.getResourceData(key.tenantId, key.resourceId);
                    } catch (Exception e) {
                        log.error("Failed to retrieve resource data by id [{}], tenant id [{}]", key.resourceId, key.tenantId, e);
                        return null;
                    }
                }, executor));
    }

    public CompletableFuture<byte[]> getResourceData(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Retrieving resource data by id [{}], tenant id [{}] from cache", resourceId, tenantId);
        return cache.get(new ResourceDataKey(tenantId, resourceId));
    }

    @TransactionalEventListener(classes = ResourceInfoEvictEvent.class)
    public void handleEvictEvent(ResourceInfoEvictEvent event) {
        TenantId tenantId = event.getTenantId();
        TbResourceId resourceId = event.getResourceId();
        cache.synchronous().invalidate(new ResourceDataKey(tenantId, resourceId));
        log.trace("Evicted resource data with id [{}], tenant id [{}]", resourceId, tenantId);
    }

    record ResourceDataKey (TenantId tenantId, TbResourceId resourceId) {}

}
