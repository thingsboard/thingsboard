// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.resource;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.FluentFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultTbResourceDataCache implements TbResourceDataCache {

    private final ResourceService resourceService;
    private final JpaExecutorService executorService;

    @Value("${cache.tbResourceData.maxSize:100000}")
    private int cacheMaxSize;
    @Value("${cache.tbResourceData.timeToLiveInMinutes:44640}")
    private int cacheValueTtl;
    private AsyncLoadingCache<ResourceDataKey, TbResourceDataInfo> cache;

    @PostConstruct
    private void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterAccess(cacheValueTtl, TimeUnit.MINUTES)
                .executor(executorService)
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> resourceService.getResourceDataInfo(key.tenantId(), key.resourceId()), executor));
    }

    @PreDestroy
    private void destroy() {
        cache.synchronous().invalidateAll();
        cache = null;
    }

    @Override
    public FluentFuture<TbResourceDataInfo> getResourceDataInfoAsync(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Retrieving resource data info by id [{}], tenant id [{}] from cache", resourceId, tenantId);
        return DonAsynchron.toFluentFuture(cache.get(new ResourceDataKey(tenantId, resourceId)));
    }

    @Override
    public void evictResourceData(TenantId tenantId, TbResourceId resourceId) {
        cache.asMap().remove(new ResourceDataKey(tenantId, resourceId));
        log.trace("Evicted resource data info with id [{}], tenant id [{}]", resourceId, tenantId);
    }

    record ResourceDataKey (TenantId tenantId, TbResourceId resourceId) {}

}
