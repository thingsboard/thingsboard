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
package org.thingsboard.server.service.ttl.rpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.rpc.RpcDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ttl.AbstractCleanUpService;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@ConditionalOnExpression("${sql.ttl.rpc.enabled:true}")
public class RpcCleanUpService extends AbstractCleanUpService {

    @Value("${sql.ttl.rpc.removal_batch_size:10000}")
    private int removalBatchSize;

    private final RpcDao rpcDao;
    private final TenantService tenantService;
    private final TbTenantProfileCache tenantProfileCache;

    public RpcCleanUpService(TenantService tenantService, PartitionService partitionService, TbTenantProfileCache tenantProfileCache, RpcDao rpcDao) {
        super(partitionService);
        this.tenantService = tenantService;
        this.tenantProfileCache = tenantProfileCache;
        this.rpcDao = rpcDao;
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.rpc.checking_interval})}", fixedDelayString = "${sql.ttl.rpc.checking_interval}")
    public void cleanUp() {
        if (!isSystemTenantPartitionMine()) {
            return;
        }
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
        for (TenantId tenantId : tenants) {
            try {
                Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
                if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getRpcTtlDays() == 0) {
                    continue;
                }

                long ttl = TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getRpcTtlDays());
                long expirationTime = System.currentTimeMillis() - ttl;

                int totalRemoved = cleanUpByTenant(tenantId, expirationTime);

                if (totalRemoved > 0) {
                    log.info("Removed {} outdated rpc(s) for tenant {} older than {}", totalRemoved, tenantId, new Date(expirationTime));
                }
            } catch (Exception e) {
                log.warn("Failed to clean up rpc by ttl for tenant {}", tenantId, e);
            }
        }
    }

    private int cleanUpByTenant(TenantId tenantId, long expirationTime) {
        int totalRemoved = 0;
        int batchRemoved;

        do {
            batchRemoved = rpcDao.deleteOutdatedRpcByTenantIdBatch(tenantId, expirationTime, removalBatchSize);
            totalRemoved += batchRemoved;

            if (batchRemoved > 0) {
                log.trace("Removed {} rpc in batch for tenant {}", batchRemoved, tenantId);
            }
        } while (batchRemoved >= removalBatchSize);

        return totalRemoved;
    }

}
