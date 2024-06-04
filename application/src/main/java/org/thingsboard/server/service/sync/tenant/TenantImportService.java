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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.service.sync.tenant.util.Storage;
import org.thingsboard.server.service.sync.tenant.util.TenantImportConfig;
import org.thingsboard.server.service.sync.tenant.util.TenantImportResult;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.thingsboard.server.common.data.ObjectType.TENANT;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantImportService {

    private final Storage storage;
    private final EntityDaoRegistry entityDaoRegistry;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tenant-import"));
    private Cache<UUID, TenantImportResult> results;

    @PostConstruct
    private void init() {
        results = Caffeine.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .build();
    }

    // todo: cancel
    public UUID importTenant(InputStream dataStream, TenantImportConfig config) {
        storage.unwrapImportData(dataStream);

        AtomicReference<Tenant> tenant = new AtomicReference<>();
        storage.readAndProcess(TENANT, dataWrapper -> {
            tenant.set((Tenant) dataWrapper.getEntity());
        });
        if (tenant.get() == null) {
            throw new IllegalStateException("No tenant data found");
        }
        TenantId tenantId = tenant.get().getId();

        TenantImportResult result = new TenantImportResult();
        executor.submit(() -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    importTenant(tenant.get(), config);
                });
                result.setSuccess(true);
                result.setDone(true);
            } catch (Exception e) {
                log.error("Failed to import tenant", e);
                result.setError(ExceptionUtils.getStackTrace(e));
                result.setDone(true);
            }
            storage.cleanUpImportData();
        });

        results.put(tenantId.getId(), result);
        return tenantId.getId();
    }

    private void importTenant(Tenant tenant, TenantImportConfig config) {
        TenantId tenantId = tenant.getId();
        TenantImportResult result = getResult(tenantId.getId());
        for (ObjectType type : ObjectType.values()) { // TODO: in parallel for related entities + ts kv
            log.debug("[{}] Importing {} entities", tenantId, type);
            storage.readAndProcess(type, dataWrapper -> {
                Object entity = dataWrapper.getEntity();
                entityDaoRegistry.getObjectDao(type).save(tenantId, entity);

                result.report(type);
                log.trace("[{}][{}] Imported entity {}", tenantId, type, entity);
            });
            log.debug("[{}] Imported {} {} entities", tenantId, result.getCount(type), type);
        }

        clearCaches();
    }

    private void clearCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    public TenantImportResult getResult(UUID tenantId) {
        TenantImportResult result = results.getIfPresent(tenantId);
        if (result == null) {
            throw new IllegalStateException("Import result for tenant id " + tenantId + " not found");
        }
        return result;
    }

}
