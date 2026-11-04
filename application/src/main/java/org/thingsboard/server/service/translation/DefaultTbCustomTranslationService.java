// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.translation.TranslationCacheKey;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractEtagCacheService;

@Service
@Slf4j
@TbCoreComponent
public class DefaultTbCustomTranslationService extends AbstractEtagCacheService<TranslationCacheKey> implements TbCustomTranslationService {

    private final CustomTranslationService customTranslationService;
    private final TbClusterService clusterService;

    public DefaultTbCustomTranslationService(TbClusterService clusterService, CustomTranslationService customTranslationService,
                                             @Value("${cache.translation.etag.timeToLiveInMinutes:44640}") int cacheTtl,
                                             @Value("${cache.translation.etag.maxSize:1000000}") int cacheMaxSize) {
        super(cacheTtl, cacheMaxSize);
        this.clusterService = clusterService;
        this.customTranslationService = customTranslationService;
    }

    @Override
    public void saveCustomTranslation(CustomTranslation customTranslation) {
        customTranslationService.saveCustomTranslation(customTranslation);
        evictFromCache(customTranslation.getTenantId());
    }

    @Override
    public void patchCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode customTranslation) {
        customTranslationService.patchCustomTranslation(tenantId, customerId, localeCode, customTranslation);
        evictFromCache(tenantId);
    }

    @Override
    public void deleteCustomTranslationKey(TenantId tenantId, CustomerId customerId, String localeCode, String keyPath) {
        customTranslationService.deleteCustomTranslationKeyByPath(tenantId, customerId, localeCode, keyPath);
        evictFromCache(tenantId);
    }

    @Override
    public void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        customTranslationService.deleteCustomTranslation(tenantId, customerId, localeCode);
        evictFromCache(tenantId);
    }

    private void evictFromCache(TenantId tenantId) {
        evictETags(TranslationCacheKey.forTenant(tenantId));
        clusterService.broadcastToCore(TransportProtos.ToCoreNotificationMsg.newBuilder()
                .setTranslationCacheInvalidateMsg(TransportProtos.TranslationCacheInvalidateMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .build())
                .build());
    }

    @Override
    public void evictETags(TranslationCacheKey cacheKey) {
        TenantId tenantId = cacheKey.getTenantId();
        if (tenantId.isSysTenantId()) {
            etagCache.invalidateAll();
        } else {
            invalidateByFilter(key -> tenantId.equals(key.getTenantId()));
        }
    }

}
