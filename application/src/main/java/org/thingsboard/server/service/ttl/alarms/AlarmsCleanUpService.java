/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.ttl.alarms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@PsqlDao
@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmsCleanUpService {
    @Value("${sql.ttl.alarms.removal_batch_size}")
    private Integer removalBatchSize;

    private final AlarmDao alarmDao;
    private final TenantDao tenantDao;
    private final PartitionService partitionService;
    private final TbTenantProfileCache tenantProfileCache;

    @Scheduled(initialDelayString = "${sql.ttl.alarms.checking_interval}", fixedDelayString = "${sql.ttl.alarms.checking_interval}")
    public void cleanUp() {
        PageLink tenantsBatchRequest = new PageLink(65536, 0);
        PageLink alarmsRemovalBatchRequest = new PageLink(removalBatchSize, 0);
        long currentTime = System.currentTimeMillis();

        PageData<TenantId> tenantsIds;
        do {
            tenantsIds = tenantDao.findTenantsIds(tenantsBatchRequest);
            tenantsIds.getData().stream()
                    .filter(tenantId -> partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition())
                    .forEach(tenantId -> {
                        Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
                        if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getAlarmsTtlDays() == 0) {
                            return;
                        }

                        PageData<UUID> toRemove;
                        long outdatageTime = currentTime - TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getAlarmsTtlDays());
                        log.info("Cleaning up outdated alarms for tenant {}", tenantId);
                        do {
                            toRemove = alarmDao.findAlarmsIdsByEndTsBeforeAndTenantId(outdatageTime, tenantId, alarmsRemovalBatchRequest);
                            alarmDao.removeAllByIds(toRemove.getData());
                        } while (toRemove.hasNext());
                    });

            tenantsBatchRequest = tenantsBatchRequest.nextPageLink();
        } while (tenantsIds.hasNext());
    }

}
