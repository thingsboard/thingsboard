/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.apiusage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
@TbCoreComponent
@Service
public class DefaultTbApiUsageStateService implements TbApiUsageStateService {

    private final ApiUsageStateService apiUsageStateService;
    private final TimeseriesService tsService;
    private final ZoneId zoneId;
    private final Map<TenantId, TenantApiUsageState> tenantStates = new ConcurrentHashMap<>();

    public DefaultTbApiUsageStateService(ApiUsageStateService apiUsageStateService, TimeseriesService tsService) {
        this.apiUsageStateService = apiUsageStateService;
        this.tsService = tsService;
        this.zoneId = SchedulerUtils.getZoneId("UTC");
    }

    @Override
    public void process(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback) {
        ToUsageStatsServiceMsg statsMsg = msg.getValue();
        TenantId tenantId = new TenantId(new UUID(statsMsg.getTenantIdMSB(), statsMsg.getTenantIdLSB()));
        TenantApiUsageState tenantState = getOrFetchState(tenantId);
        long ts = tenantState.getCurrentMonthTs();
        List<TsKvEntry> updatedEntries = new ArrayList<>(ApiUsageRecordKey.values().length);
        for (UsageStatsKVProto kvProto : statsMsg.getValuesList()) {
            ApiUsageRecordKey recordKey = ApiUsageRecordKey.valueOf(kvProto.getKey());
            long newValue = tenantState.add(recordKey, kvProto.getValue());
            updatedEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(recordKey.name(), newValue)));
        }
        tsService.save(tenantId, tenantState.getEntityId(), updatedEntries, 0L);
    }

    private TenantApiUsageState getOrFetchState(TenantId tenantId) {
        TenantApiUsageState tenantState = tenantStates.get(tenantId);
        if (tenantState == null) {
            long currentMonthTs = LocalDate.now().withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
            ApiUsageState dbStateEntity = apiUsageStateService.findTenantApiUsageState(tenantId);
            tenantState = new TenantApiUsageState(currentMonthTs, dbStateEntity.getEntityId());
            try {
                List<TsKvEntry> dbValues = tsService.findAllLatest(tenantId, dbStateEntity.getEntityId()).get();
                for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                    TsKvEntry keyEntry = null;
                    for (TsKvEntry tsKvEntry : dbValues) {
                        if (tsKvEntry.getKey().equals(key.name())) {
                            keyEntry = tsKvEntry;
                            break;
                        }
                    }
                    if (keyEntry != null) {
                        tenantState.put(key, keyEntry.getLongValue().get());
                    } else {
                        tenantState.put(key, 0L);
                    }
                }
                tenantStates.put(tenantId, tenantState);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("[{}] Failed to fetch api usage state from db.", tenantId, e);
            }
        }
        return tenantState;
    }

}
