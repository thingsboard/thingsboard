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
package org.thingsboard.server.service.stats;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.stats.EntityStatisticsValue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.stats.EntityStatisticsDao;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.apiusage.ApiStatsKey;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public abstract class BaseEntitiesStatisticsService<I extends EntityId> {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private PartitionService partitionService;
    @Autowired
    private TbApiUsageStateService apiUsageStateService;
    @Autowired
    private TimeseriesService timeseriesService;
    @Autowired
    protected EntityStatisticsDao entityStatisticsDao;

    @Scheduled(cron = "0 0 4 * * *", zone = "UTC") // every day at 4 AM
    protected void calculateStats() {
        if (!partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
            return;
        }
        log.info("[{}] Calculating stats", getClass().getSimpleName());
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 5000);
        for (TenantId tenantId : tenants) {
            log.debug("[{}] Calculating stats for tenant {}", getClass().getSimpleName(), tenantId);
            try {
                Pair<Long, Long> calculationPeriod = getCalculationPeriod();
                ApiUsageStateId usageStateId = Optional.ofNullable(apiUsageStateService.getApiUsageState(tenantId))
                        .map(IdBased::getId).orElseThrow(() -> new IllegalStateException("ApiUsageState not found"));

                PageDataIterable<I> entities = new PageDataIterable<>(pageLink -> findEntities(tenantId, pageLink), 2000);
                for (I entityId : entities) {
                    try {
                        EntityStatisticsValue value = calculateStats(tenantId, entityId, recordKey -> {
                            return getStatsSumForPeriod(tenantId, usageStateId, ApiStatsKey.of(recordKey, entityId.getId()),
                                    calculationPeriod.getFirst(), calculationPeriod.getSecond());
                        });
                        log.trace("[{}] Stats for {} {}: {}", getClass().getSimpleName(), entityId.getEntityType(), entityId, value);
                        entityStatisticsDao.updateStats(tenantId, entityId, previousValue -> {
                            if (previousValue != null) {
                                return previousValue.update(value);
                            } else {
                                return value;
                            }
                        }, calculationPeriod.getFirst());
                    } catch (Exception e) {
                        log.error("Failed to calculate stats for {} {}", entityId.getEntityType(), entityId, e);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to calculate entity stats for tenant {}", tenants, e);
            }
        }
        log.info("[{}] Finished stats calculation", getClass().getSimpleName());
    }

    protected abstract EntityStatisticsValue calculateStats(TenantId tenantId, I entityId, Function<ApiUsageRecordKey, Long> statsAssembler);

    protected abstract Pair<Long, Long> getCalculationPeriod();

    protected abstract PageData<I> findEntities(TenantId tenantId, PageLink pageLink);

    @SneakyThrows
    private long getStatsSumForPeriod(TenantId tenantId, ApiUsageStateId stateId, ApiStatsKey statsKey,
                                      long startTs, long endTs) {
        ReadTsKvQuery query = new BaseReadTsKvQuery(statsKey.getEntryKey(true), startTs, endTs, Aggregation.SUM);
        return timeseriesService.findAll(tenantId, stateId, List.of(query)).get()
                .stream().findFirst().flatMap(KvEntry::getLongValue).orElse(0L);
    }

}
