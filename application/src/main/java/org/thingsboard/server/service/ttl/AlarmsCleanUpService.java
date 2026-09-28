// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.ttl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmRef;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmsCleanUpService {

    @Value("${sql.ttl.alarms.removal_batch_size}")
    private Integer removalBatchSize;

    private final TenantService tenantService;
    private final AlarmDao alarmDao;
    private final AlarmService alarmService;
    private final EntityActionService entityActionService;
    private final PartitionService partitionService;
    private final TbTenantProfileCache tenantProfileCache;
    private final Optional<HousekeeperClient> housekeeperClient;

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.alarms.checking_interval})}", fixedDelayString = "${sql.ttl.alarms.checking_interval}")
    public void cleanUp() {
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
        for (TenantId tenantId : tenants) {
            try {
                cleanUp(tenantId);
            } catch (Exception e) {
                getLogger().warn("Failed to clean up alarms by ttl for tenant {}", tenantId, e);
            }
        }
    }

    private void cleanUp(TenantId tenantId) {
        if (!partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
            return;
        }

        Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
        if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getAlarmsTtlDays() == 0) {
            return;
        }

        long ttl = TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getAlarmsTtlDays());
        long expirationTime = System.currentTimeMillis() - ttl;

        PageLink removalBatchRequest = new PageLink(removalBatchSize, 0);
        long totalRemoved = 0;
        Set<String> typesToRemove = new HashSet<>();
        while (true) {
            PageData<AlarmRef> toRemove = alarmDao.findExpiredAlarmRefsByTenantId(expirationTime, tenantId, removalBatchRequest);
            List<UUID> removedAlarmIds = new ArrayList<>();
            for (AlarmRef expired : toRemove.getData()) {
                // Delete by (originator_id, id) so each removal routes to the single owning shard instead of a
                // cross-shard lookup-by-id on the originator-sharded alarm table. The scan returns only the
                // (alarmId, originator) pair; delAlarm loads the full row once on that shard for the delete event.
                Alarm alarm = alarmService.delAlarm(tenantId, expired.originator(), expired.alarmId(), false).getAlarm();
                if (alarm != null) {
                    entityActionService.pushEntityActionToRuleEngine(alarm.getOriginator(), alarm, tenantId, null, ActionType.ALARM_DELETE, null);
                    totalRemoved++;
                    typesToRemove.add(alarm.getType());
                    removedAlarmIds.add(alarm.getId().getId());
                }
            }
            // One batched alarm-comment cleanup task per removal batch instead of one per alarm (delAlarm above deletes
            // with the bulk flag, so it does not enqueue a per-alarm comment task).
            if (!removedAlarmIds.isEmpty()) {
                housekeeperClient.ifPresent(client -> client.submitTask(HousekeeperTask.deleteAlarmComments(tenantId, tenantId, removedAlarmIds)));
            }
            if (!toRemove.hasNext()) {
                break;
            }
        }

        alarmService.delAlarmTypes(tenantId, typesToRemove);

        if (totalRemoved > 0) {
            getLogger().info("Removed {} outdated alarm(s) for tenant {} older than {}", totalRemoved, tenantId, new Date(expirationTime));
        }
    }

    // wrapper for tests to spy on static logger
    Logger getLogger() {
        return log;
    }

}
