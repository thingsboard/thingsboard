/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
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
    private final PartitionService partitionService;
    private final ApiUsageStateService apiUsageStateService;
    private final TimeseriesService tsService;
    private final SchedulerComponent scheduler;
    private final TbTenantProfileCache tenantProfileCache;
    private final Map<TenantId, TenantApiUsageState> tenantStates = new ConcurrentHashMap<>();

    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;

    @Value("${usage.stats.check.cycle:60000}")
    private long nextCycleCheckInterval;

    private final Lock updateLock = new ReentrantLock();

    public DefaultTbApiUsageStateService(PartitionService partitionService, ApiUsageStateService apiUsageStateService, TimeseriesService tsService, SchedulerComponent scheduler, TbTenantProfileCache tenantProfileCache) {
        this.partitionService = partitionService;
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
        boolean stateUpdated = false;
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
                long newHourlyValue = tenantState.addToHourly(recordKey, kvProto.getValue());
                updatedEntries.add(new BasicTsKvEntry(hourTs, new LongDataEntry(HOURLY + recordKey.name(), newHourlyValue)));
                stateUpdated |= tenantState.checkStateUpdatedDueToThreshold(recordKey);
            }
        } finally {
            updateLock.unlock();
        }
        tsService.save(tenantId, tenantState.getEntityId(), updatedEntries, 0L);
        if (stateUpdated) {
            // Save new state into the database;
            apiUsageStateService.update(tenantState.getApiUsageState());
            //TODO: clear cache on cluster repartition.
            //TODO: update profiles on tenant and profile updates.
            //TODO: broadcast to everyone notifications about enabled/disabled features.
        }
    }

    @Override
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (partitionChangeEvent.getServiceType().equals(ServiceType.TB_CORE)) {
            tenantStates.entrySet().removeIf(entry -> !partitionService.resolve(ServiceType.TB_CORE, entry.getKey(), entry.getKey()).isMyPartition());
        }
    }

    @Override
    public TenantApiUsageState getApiUsageState(TenantId tenantId) {
        //We should always get it from the map of from the database;
        return null;
    }

    @Override
    public void onTenantProfileUpdate(TenantProfileId tenantProfileId) {
        TenantProfile tenantProfile = tenantProfileCache.get(tenantProfileId);
        updateLock.lock();
        try {
            tenantStates.values().forEach(state -> {
                if (tenantProfile.getId().equals(state.getTenantProfileId())) {
                    updateTenantState(state, tenantProfile);
                }
            });
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void onTenantUpdate(TenantId tenantId) {
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        updateLock.lock();
        try {
            TenantApiUsageState state = tenantStates.get(tenantId);
            if (state != null && !state.getTenantProfileId().equals(tenantProfile.getId())) {
                updateTenantState(state, tenantProfile);
            }
        } finally {
            updateLock.unlock();
        }
    }


    private void updateTenantState(TenantApiUsageState state, TenantProfile tenantProfile) {
        state.setTenantProfileData(tenantProfile.getProfileData());
        if (state.checkStateUpdatedDueToThresholds()) {
            apiUsageStateService.update(state.getApiUsageState());
            //TODO: send notification to cluster;
        }
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
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            tenantState = new TenantApiUsageState(tenantProfile, dbStateEntity);
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
