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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.service.sync.tenant.util.Storage;
import org.thingsboard.server.service.sync.tenant.util.TenantImportResult;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantImportService {

    private final Storage storage;
    private final EntityDaoRegistry entityDaoRegistry;
    private final TransactionTemplate transactionTemplate;
    private final TenantDao tenantDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tenant-import"));
    private final Map<TenantId, TenantImportResult> results = new ConcurrentHashMap<>();

    private final List<String> importOrder = List.of(
            "Tenant", "Customer", "RuleChain", "Dashboard", "DeviceProfile", "Device"
    );

    public TenantId importTenant(InputStream dataStream) throws Exception {
        storage.unwrap(dataStream);

        AtomicReference<Tenant> tenant = new AtomicReference<>();
        storage.read("Tenant", dataWrapper -> {
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
                    importTenant(tenant.get());
                });
                result.setSuccess(true);
                result.setDone(true);
            } catch (Exception e) {
                log.error("Failed to import tenant", e);
                result.setError(ExceptionUtils.getStackTrace(e));
                result.setDone(true);
            }
        });
        results.put(tenantId, result);
        return tenantId;
    }

    public TenantImportResult getResult(TenantId tenantId) {
        return results.get(tenantId);
    }

    private void importTenant(Tenant tenant) {
        entityDaoRegistry.getTenantEntityDaos().keySet().stream()
                .sorted(Comparator.comparing(type -> {
                    int order = importOrder.indexOf(type);
                    if (order == -1) {
                        return Integer.MAX_VALUE;
                    }
                    return order;
                }))
                .forEach(type -> {
                    log.debug("[{}] Importing {} entities", tenant.getId(), type);
                    storage.read(type, dataWrapper -> {
                        Object entity = dataWrapper.getEntity();
                        entityDaoRegistry.getTenantEntityDao(type).save(TenantId.SYS_TENANT_ID, entity);
                        getResult(tenant.getId()).report(type);
                        log.trace("[{}][{}] Imported entity {}", tenant.getId(), type, entity);
                    });
                });
    }

}
