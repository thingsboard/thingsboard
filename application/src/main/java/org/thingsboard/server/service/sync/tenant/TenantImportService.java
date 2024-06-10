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
package org.thingsboard.server.service.sync.tenant;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.service.sync.tenant.util.DataWrapper;
import org.thingsboard.server.service.sync.tenant.util.Result;
import org.thingsboard.server.service.sync.tenant.util.ResultStore;
import org.thingsboard.server.service.sync.tenant.util.Storage;
import org.thingsboard.server.service.sync.tenant.util.TenantImportConfig;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.thingsboard.server.common.data.ObjectType.TENANT;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantImportService {

    private final Storage storage;
    private final EntityDaoRegistry entityDaoRegistry;
    private final TenantProfileService tenantProfileService;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;
    private ResultStore<ObjectType> resultStore;
    @Value("${cache.specs.tenantImportResults.timeToLiveInMinutes:1440}")
    private int resultsTtl;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tenant-import"));

    @PostConstruct
    private void init() {
        resultStore = ResultStore.<ObjectType>builder()
                .name("Tenant import")
                .ttlInMinutes(resultsTtl)
                .persistFrequency(100)
                .cacheName(CacheConstants.TENANT_IMPORT_RESULT_CACHE)
                .cacheManager(cacheManager)
                .build();
    }

    public UUID importTenant(InputStream dataStream, TenantImportConfig config) {
        storage.unwrapImportData(dataStream);

        AtomicReference<Tenant> tenant = new AtomicReference<>();
        storage.readAndProcess(TENANT, dataWrapper -> {
            tenant.set((Tenant) dataWrapper.getEntity());
        });
        if (tenant.get() == null) {
            throw new IllegalStateException("No tenant data found");
        }
        UUID tenantId = tenant.get().getUuidId();

        executor.submit(() -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    importTenant(tenant.get(), config);
                });
                resultStore.update(tenantId, result -> {
                    result.setSuccess(true);
                    result.setDone(true);
                });
            } catch (Throwable t) {
                log.error("Failed to import tenant", t);
                try {
                    resultStore.update(tenantId, result -> {
                        result.setError(ExceptionUtils.getStackTrace(t));
                        result.setDone(true);
                    });
                } catch (Exception e) {
                    log.error("Failed to handle import error", e);
                }
            }
            storage.cleanUpImportData();
        });

        return tenantId;
    }

    private void importTenant(Tenant tenant, TenantImportConfig config) {
        TenantId tenantId = tenant.getId();
        for (ObjectType type : ObjectType.values()) { // TODO: in parallel for related entities + ts kv
            log.debug("[{}] Importing {} entities", tenantId, type);
            storage.readAndProcess(type, dataWrapper -> {
                save(tenantId, type, dataWrapper);
            });
            resultStore.flush(tenantId.getId(), type);
        }

        clearCaches();
    }

    private void save(TenantId tenantId, ObjectType type, DataWrapper dataWrapper) {
        Object entity = dataWrapper.getEntity();
        if (entity instanceof Tenant tenant) {
            tenant.setTenantProfileId(tenantProfileService.findDefaultTenantProfile(TenantId.SYS_TENANT_ID).getId());
        }

        entityDaoRegistry.getDao(type).save(tenantId, entity);
        resultStore.report(tenantId.getId(), type);
        log.trace("[{}][{}] Imported entity {}", tenantId, type, entity);
    }

    private void clearCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    public Result<ObjectType> getResult(UUID tenantId) {
        return resultStore.getStoredResult(tenantId);
    }

}
