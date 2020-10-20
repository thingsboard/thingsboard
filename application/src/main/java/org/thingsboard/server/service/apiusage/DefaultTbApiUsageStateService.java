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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.TenantProfile;
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
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.profile.TbTenantProfileCache;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@TbCoreComponent
@Service
public class DefaultTbApiUsageStateService implements TbApiUsageStateService {

    public static final String HOURLY = "HOURLY_";
    private final ApiUsageStateService apiUsageStateService;
    private final TimeseriesService tsService;
    private final SchedulerComponent scheduler;
    private final TbTenantProfileCache tenantProfileCache;
    private final Map<TenantId, TenantApiUsageState> tenantStates = new ConcurrentHashMap<>();

    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;

    @Value("${usage.stats.check.cycle:6000}")
    private long nextCycleCheckInterval;

    private final Lock updateLock = new ReentrantLock();

    public DefaultTbApiUsageStateService(ApiUsageStateService apiUsageStateService, TimeseriesService tsService, SchedulerComponent scheduler, TbTenantProfileCache tenantProfileCache) {
        this.apiUsageStateService = apiUsageStateService;
        this.tsService = tsService;
        this.scheduler = scheduler;
        this.tenantProfileCache = tenantProfileCache;
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            scheduler.scheduleAtFixedRate(this::checkStartOfNextCycle, nextCycleCheckInterval, nextCycleCheckInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void process(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback) {
        ToUsageStatsServiceMsg statsMsg = msg.getValue();
        TenantId tenantId = new TenantId(new UUID(statsMsg.getTenantIdMSB(), statsMsg.getTenantIdLSB()));
        TenantApiUsageState tenantState;
        List<TsKvEntry> updatedEntries;
        updateLock.lock();
        try {
            tenantState = getOrFetchState(tenantId);
            long ts = tenantState.getCurrentCycleTs();
            long hourTs = tenantState.getCurrentHourTs();
            long newHourTs = SchedulerUtils.getStartOfCurrentHour();
            if (newHourTs != hourTs) {
                tenantState.setHour(newHourTs);
            }
            updatedEntries = new ArrayList<>(ApiUsageRecordKey.values().length);
            for (UsageStatsKVProto kvProto : statsMsg.getValuesList()) {
                ApiUsageRecordKey recordKey = ApiUsageRecordKey.valueOf(kvProto.getKey());
                long newValue = tenantState.add(recordKey, kvProto.getValue());
                updatedEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(recordKey.name(), newValue)));
                newValue = tenantState.addToHourly(recordKey, kvProto.getValue());
                updatedEntries.add(new BasicTsKvEntry(hourTs, new LongDataEntry(HOURLY + recordKey.name(), newValue)));
            }
        } finally {
            updateLock.unlock();
        }
        tsService.save(tenantId, tenantState.getId(), updatedEntries, 0L);
    }

    @Override
    public TenantApiUsageState getApiUsageState(TenantId tenantId) {
        return null;
    }

    @Override
    public void onAddedToAllowList(TenantId tenantId) {

    }

    @Override
    public void onAddedToDenyList(TenantId tenantId) {

    }

    private void checkStartOfNextCycle() {
        updateLock.lock();
        try {
            long now = System.currentTimeMillis();
            tenantStates.values().forEach(state -> {
                if ((state.getNextCycleTs() > now) && (state.getNextCycleTs() - now < TimeUnit.HOURS.toMillis(1))) {
                    state.setCycles(state.getNextCycleTs(), SchedulerUtils.getStartOfNextNextMonth());
                }
            });
        } finally {
            updateLock.unlock();
        }
    }

    private TenantApiUsageState getOrFetchState(TenantId tenantId) {
        TenantApiUsageState tenantState = tenantStates.get(tenantId);
        if (tenantState == null) {
            ApiUsageState dbStateEntity = apiUsageStateService.findTenantApiUsageState(tenantId);
            if (dbStateEntity == null) {
                try {
                    dbStateEntity = apiUsageStateService.createDefaultApiUsageState(tenantId);
                } catch (Exception e) {
                    dbStateEntity = apiUsageStateService.findTenantApiUsageState(tenantId);
                }
            }
            tenantState = new TenantApiUsageState(dbStateEntity.getId());
            try {
                List<TsKvEntry> dbValues = tsService.findAllLatest(tenantId, dbStateEntity.getEntityId()).get();
                for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                    boolean cycleEntryFound = false;
                    boolean hourlyEntryFound = false;
                    for (TsKvEntry tsKvEntry : dbValues) {
                        if (tsKvEntry.getKey().equals(key.name())) {
                            cycleEntryFound = true;
                            tenantState.put(key, tsKvEntry.getLongValue().get());
                        } else if (tsKvEntry.getKey().equals(HOURLY + key.name())) {
                            hourlyEntryFound = true;
                            if (tsKvEntry.getTs() == tenantState.getCurrentHourTs()) {
                                tenantState.putHourly(key, tsKvEntry.getLongValue().get());
                            }
                        }
                        if (cycleEntryFound && hourlyEntryFound) {
                            break;
                        }
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
