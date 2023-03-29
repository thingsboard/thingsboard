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
package org.thingsboard.server.service.stats.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.stats.EntityStatisticsValue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.stats.BaseEntitiesStatisticsService;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.ApiUsageRecordKey.TRANSPORT_DP_COUNT;
import static org.thingsboard.server.common.data.ApiUsageRecordKey.TRANSPORT_MSG_COUNT;

@Service
@TbCoreComponent
@ConditionalOnProperty(prefix = "usage.stats.devices", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DevicesStatisticsService extends BaseEntitiesStatisticsService<DeviceId> {

    private final DeviceService deviceService;
    private final PartitionService partitionService;
    @Value("${usage.stats.devices.ttl_in_days:45}")
    private int ttlInDays;

    @Override
    protected EntityStatisticsValue calculateStats(TenantId tenantId, DeviceId entityId, Function<ApiUsageRecordKey, Long> statsAssembler) {
        long dailyMsgCount = statsAssembler.apply(TRANSPORT_MSG_COUNT);
        long dailyDataPointsCount = statsAssembler.apply(TRANSPORT_DP_COUNT);
        DeviceClass deviceClass = DeviceClass.defineClass(dailyMsgCount, dailyDataPointsCount);

        return DeviceStats.builder()
                .deviceClass(deviceClass)
                .dailyMsgCount(dailyMsgCount)
                .dailyDataPointsCount(dailyDataPointsCount)
                .build();
    }

    @Override
    protected Pair<Long, Long> getCalculationPeriod() {
        long startTs = SchedulerUtils.getStartOfPreviousDay();
        long endTs = SchedulerUtils.getEndOfPreviousDay();
        return Pair.of(startTs, endTs);
    }

    @Override
    protected PageData<DeviceId> findEntities(TenantId tenantId, PageLink pageLink) {
        return deviceService.findDevicesIdsByTenantId(tenantId, pageLink);
    }

    /*
     * tenantId is optional. If not specified - statistics will be calculated in scope of the whole platform
     * */
    public DevicesSummaryStatistics getSummaryStatistics(TenantId tenantId, long startTs, long endTs) {
        long totalCount = tenantId != null ? deviceService.countByTenantId(tenantId) : deviceService.count();
        Map<DeviceClass, Integer> perClassCount = Arrays.stream(DeviceClass.values())
                .collect(Collectors.toMap(k -> k, deviceClass -> {
                    return entityStatisticsDao.countByTenantIdAndTsBetweenAndLatestValueProperty(tenantId, startTs, endTs, "deviceClass", deviceClass.name());
                }));

        return DevicesSummaryStatistics.builder()
                .tenantId(tenantId)
                .totalDevicesCount((int) totalCount)
                .perClassDevicesCount(perClassCount)
                .build();
    }

    @Scheduled(initialDelay = 1, fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    public void cleanUpOldStats() {
        if (ttlInDays == 0 || !partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
            return;
        }

        long expTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(ttlInDays);
        entityStatisticsDao.deleteByTsBefore(expTime);
    }

}
