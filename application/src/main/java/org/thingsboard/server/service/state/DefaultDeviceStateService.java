/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.state;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.sql.query.EntityQueryRepository;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.DbTypeInfoComponent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.DataConstants.CONNECT_EVENT;
import static org.thingsboard.server.common.data.DataConstants.DISCONNECT_EVENT;
import static org.thingsboard.server.common.data.DataConstants.INACTIVITY_EVENT;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

/**
 * Created by ashvayka on 01.05.18.
 */
@Service
@TbCoreComponent
@Slf4j
public class DefaultDeviceStateService extends AbstractPartitionBasedService<DeviceId> implements DeviceStateService {

    public static final String ACTIVITY_STATE = "active";
    public static final String LAST_CONNECT_TIME = "lastConnectTime";
    public static final String LAST_DISCONNECT_TIME = "lastDisconnectTime";
    public static final String LAST_ACTIVITY_TIME = "lastActivityTime";
    public static final String INACTIVITY_ALARM_TIME = "inactivityAlarmTime";
    public static final String INACTIVITY_TIMEOUT = "inactivityTimeout";

    private static final List<EntityKey> PERSISTENT_TELEMETRY_KEYS = Arrays.asList(
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_ACTIVITY_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, INACTIVITY_ALARM_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, INACTIVITY_TIMEOUT),
            new EntityKey(EntityKeyType.TIME_SERIES, ACTIVITY_STATE),
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_CONNECT_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_DISCONNECT_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT));

    private static final List<EntityKey> PERSISTENT_ATTRIBUTE_KEYS = Arrays.asList(
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_ACTIVITY_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_ALARM_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, ACTIVITY_STATE),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_CONNECT_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_DISCONNECT_TIME));

    public static final List<String> PERSISTENT_ATTRIBUTES = Arrays.asList(ACTIVITY_STATE, LAST_CONNECT_TIME,
            LAST_DISCONNECT_TIME, LAST_ACTIVITY_TIME, INACTIVITY_ALARM_TIME, INACTIVITY_TIMEOUT);
    private static final List<EntityKey> PERSISTENT_ENTITY_FIELDS = Arrays.asList(
            new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "type"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));

    private final TenantService tenantService;
    private final DeviceService deviceService;
    private final AttributesService attributesService;
    private final TimeseriesService tsService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final EntityQueryRepository entityQueryRepository;
    private final DbTypeInfoComponent dbTypeInfoComponent;

    private TelemetrySubscriptionService tsSubService;

    @Value("${state.defaultInactivityTimeoutInSec}")
    @Getter
    @Setter
    private long defaultInactivityTimeoutInSec;

    @Value("#{${state.defaultInactivityTimeoutInSec} * 1000}")
    @Getter
    @Setter
    private long defaultInactivityTimeoutMs;

    @Value("${state.defaultStateCheckIntervalInSec}")
    @Getter
    private int defaultStateCheckIntervalInSec;

    @Value("${state.persistToTelemetry:false}")
    @Getter
    @Setter
    private boolean persistToTelemetry;

    @Value("${state.initFetchPackSize:50000}")
    @Getter
    private int initFetchPackSize;

    private ListeningExecutorService deviceStateExecutor;

    final ConcurrentMap<DeviceId, DeviceStateData> deviceStates = new ConcurrentHashMap<>();

    public DefaultDeviceStateService(TenantService tenantService, DeviceService deviceService,
                                     AttributesService attributesService, TimeseriesService tsService,
                                     TbClusterService clusterService, PartitionService partitionService,
                                     TbServiceInfoProvider serviceInfoProvider,
                                     EntityQueryRepository entityQueryRepository,
                                     DbTypeInfoComponent dbTypeInfoComponent) {
        this.tenantService = tenantService;
        this.deviceService = deviceService;
        this.attributesService = attributesService;
        this.tsService = tsService;
        this.clusterService = clusterService;
        this.partitionService = partitionService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.entityQueryRepository = entityQueryRepository;
        this.dbTypeInfoComponent = dbTypeInfoComponent;
    }

    @Autowired
    public void setTsSubService(TelemetrySubscriptionService tsSubService) {
        this.tsSubService = tsSubService;
    }

    @PostConstruct
    public void init() {
        super.init();
        deviceStateExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "device-state"));
        scheduledExecutor.scheduleAtFixedRate(this::updateInactivityStateIfExpired, new Random().nextInt(defaultStateCheckIntervalInSec), defaultStateCheckIntervalInSec, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        super.stop();
        if (deviceStateExecutor != null) {
            deviceStateExecutor.shutdownNow();
        }
    }

    @Override
    protected String getServiceName() {
        return "Device State";
    }

    @Override
    protected String getSchedulerExecutorName() {
        return "device-state-scheduled";
    }

    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId) {
        if (cleanDeviceStateIfBelongsExternalPartition(tenantId, deviceId)) {
            return;
        }
        log.trace("on Device Connect [{}]", deviceId.getId());
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long ts = System.currentTimeMillis();
        stateData.getState().setLastConnectTime(ts);
        save(deviceId, LAST_CONNECT_TIME, ts);
        pushRuleEngineMessage(stateData, CONNECT_EVENT);
        checkAndUpdateState(deviceId, stateData);

    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long lastReportedActivity) {
        if (cleanDeviceStateIfBelongsExternalPartition(tenantId, deviceId)) {
            return;
        }
        log.trace("on Device Activity [{}], lastReportedActivity [{}]", deviceId.getId(), lastReportedActivity);
        final DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (lastReportedActivity > 0 && lastReportedActivity > stateData.getState().getLastActivityTime()) {
            updateActivityState(deviceId, stateData, lastReportedActivity);
        }
    }

    void updateActivityState(DeviceId deviceId, DeviceStateData stateData, long lastReportedActivity) {
        log.trace("updateActivityState - fetched state {} for device {}, lastReportedActivity {}", stateData, deviceId, lastReportedActivity);
        if (stateData != null) {
            save(deviceId, LAST_ACTIVITY_TIME, lastReportedActivity);
            DeviceState state = stateData.getState();
            state.setLastActivityTime(lastReportedActivity);
            if (!state.isActive()) {
                state.setActive(true);
                save(deviceId, ACTIVITY_STATE, true);
                pushRuleEngineMessage(stateData, ACTIVITY_EVENT);
            }
        } else {
            log.debug("updateActivityState - fetched state IN NULL for device {}, lastReportedActivity {}", deviceId, lastReportedActivity);
            cleanupEntity(deviceId);
        }
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId) {
        if (cleanDeviceStateIfBelongsExternalPartition(tenantId, deviceId)) {
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long ts = System.currentTimeMillis();
        stateData.getState().setLastDisconnectTime(ts);
        save(deviceId, LAST_DISCONNECT_TIME, ts);
        pushRuleEngineMessage(stateData, DISCONNECT_EVENT);
    }

    @Override
    public void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout) {
        if (cleanDeviceStateIfBelongsExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (inactivityTimeout <= 0L) {
            inactivityTimeout = defaultInactivityTimeoutInSec;
        }
        log.trace("on Device Activity Timeout Update device id {} inactivityTimeout {}", deviceId, inactivityTimeout);
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        stateData.getState().setInactivityTimeout(inactivityTimeout);
        checkAndUpdateState(deviceId, stateData);
    }

    @Override
    public void onQueueMsg(TransportProtos.DeviceStateServiceMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB()));
            if (proto.getDeleted()) {
                onDeviceDeleted(tenantId, deviceId);
                callback.onSuccess();
            } else {
                Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
                if (device != null) {
                    if (proto.getAdded()) {
                        Futures.addCallback(fetchDeviceState(device), new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable DeviceStateData state) {
                                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, device.getId());
                                if (addDeviceUsingState(tpi, state)) {
                                    save(deviceId, ACTIVITY_STATE, false);
                                    callback.onSuccess();
                                } else {
                                    log.debug("[{}][{}] Device belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                                            , tenantId, deviceId, tpi.getFullTopicName());
                                    callback.onFailure(new RuntimeException("Device belongs to external partition " + tpi.getFullTopicName() + "!"));
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.warn("Failed to register device to the state service", t);
                                callback.onFailure(t);
                            }
                        }, deviceStateExecutor);
                    } else if (proto.getUpdated()) {
                        DeviceStateData stateData = getOrFetchDeviceStateData(device.getId());
                        TbMsgMetaData md = new TbMsgMetaData();
                        md.putValue("deviceName", device.getName());
                        md.putValue("deviceType", device.getType());
                        stateData.setMetaData(md);
                        callback.onSuccess();
                    }
                } else {
                    //Device was probably deleted while message was in queue;
                    callback.onSuccess();
                }
            }
        } catch (Exception e) {
            log.trace("Failed to process queue msg: [{}]", proto, e);
            callback.onFailure(e);
        }
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        var result = new HashMap<TopicPartitionInfo, List<ListenableFuture<?>>>();
        PageDataIterable<DeviceIdInfo> deviceIdInfos = new PageDataIterable<>(deviceService::findDeviceIdInfos, initFetchPackSize);
        Map<TopicPartitionInfo, List<DeviceIdInfo>> tpiDeviceMap = new HashMap<>();

        for (DeviceIdInfo idInfo : deviceIdInfos) {
            TopicPartitionInfo tpi;
            try {
                tpi = partitionService.resolve(ServiceType.TB_CORE, idInfo.getTenantId(), idInfo.getDeviceId());
            } catch (Exception e) {
                log.warn("Failed to resolve partition for device with id [{}], tenant id [{}], customer id [{}]. Reason: {}",
                        idInfo.getDeviceId(), idInfo.getTenantId(), idInfo.getCustomerId(), e.getMessage());
                continue;
            }
            if (addedPartitions.contains(tpi) && !deviceStates.containsKey(idInfo.getDeviceId())) {
                tpiDeviceMap.computeIfAbsent(tpi, tmp -> new ArrayList<>()).add(idInfo);
            }
        }

        for (var entry : tpiDeviceMap.entrySet()) {
            AtomicInteger counter = new AtomicInteger(0);
            // hard-coded limit of 1000 is due to the Entity Data Query limitations and should not be changed.
            for (List<DeviceIdInfo> partition : Lists.partition(entry.getValue(), 1000)) {
                log.info("[{}] Submit task for device states: {}", entry.getKey(), partition.size());
                DevicePackFutureHolder devicePackFutureHolder = new DevicePackFutureHolder();
                var devicePackFuture = deviceStateExecutor.submit(() -> {
                    try {
                        List<DeviceStateData> states;
                        if (persistToTelemetry && !dbTypeInfoComponent.isLatestTsDaoStoredToSql()) {
                            states = fetchDeviceStateDataUsingSeparateRequests(partition);
                        } else {
                            states = fetchDeviceStateDataUsingEntityDataQuery(partition);
                        }
                        if (devicePackFutureHolder.future == null || !devicePackFutureHolder.future.isCancelled()) {
                            for (var state : states) {
                                if (!addDeviceUsingState(entry.getKey(), state)) {
                                    return;
                                }
                                checkAndUpdateState(state.getDeviceId(), state);
                            }
                            log.info("[{}] Initialized {} out of {} device states", entry.getKey().getPartition().orElse(0), counter.addAndGet(states.size()), entry.getValue().size());
                        }
                    } catch (Throwable t) {
                        log.error("Unexpected exception while device pack fetching", t);
                        throw t;
                    }
                });
                devicePackFutureHolder.future = devicePackFuture;
                result.computeIfAbsent(entry.getKey(), tmp -> new ArrayList<>()).add(devicePackFuture);
            }
        }
        return result;
    }

    private static class DevicePackFutureHolder {
        private volatile ListenableFuture<?> future;
    }

    void checkAndUpdateState(@Nonnull DeviceId deviceId, @Nonnull DeviceStateData state) {
        if (state.getState().isActive()) {
            updateInactivityStateIfExpired(System.currentTimeMillis(), deviceId, state);
        } else {
            //trying to fix activity state
            if (isActive(System.currentTimeMillis(), state.getState())) {
                updateActivityState(deviceId, state, state.getState().getLastActivityTime());
            }
        }
    }

    private boolean addDeviceUsingState(TopicPartitionInfo tpi, DeviceStateData state) {
        Set<DeviceId> deviceIds = partitionedEntities.get(tpi);
        if (deviceIds != null) {
            deviceIds.add(state.getDeviceId());
            deviceStates.putIfAbsent(state.getDeviceId(), state);
            return true;
        } else {
            log.debug("[{}] Device belongs to external partition {}", state.getDeviceId(), tpi.getFullTopicName());
            return false;
        }
    }

    void updateInactivityStateIfExpired() {
        try {
            final long ts = System.currentTimeMillis();
            partitionedEntities.forEach((tpi, deviceIds) -> {
                log.debug("Calculating state updates. tpi {} for {} devices", tpi.getFullTopicName(), deviceIds.size());
                for (DeviceId deviceId : deviceIds) {
                    try {
                        updateInactivityStateIfExpired(ts, deviceId);
                    } catch (Exception e) {
                        log.warn("[{}] Failed to update inactivity state", deviceId, e);
                    }
                }
            });
        } catch (Throwable t) {
            log.warn("Failed to update inactivity states", t);
        }
    }

    void updateInactivityStateIfExpired(long ts, DeviceId deviceId) {
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        updateInactivityStateIfExpired(ts, deviceId, stateData);
    }

    void updateInactivityStateIfExpired(long ts, DeviceId deviceId, DeviceStateData stateData) {
        log.trace("Processing state {} for device {}", stateData, deviceId);
        if (stateData != null) {
            DeviceState state = stateData.getState();
            if (!isActive(ts, state)
                    && (state.getLastInactivityAlarmTime() == 0L || state.getLastInactivityAlarmTime() < state.getLastActivityTime())
                    && stateData.getDeviceCreationTime() + state.getInactivityTimeout() < ts) {
                if (partitionService.resolve(ServiceType.TB_CORE, stateData.getTenantId(), deviceId).isMyPartition()) {
                    state.setActive(false);
                    state.setLastInactivityAlarmTime(ts);
                    save(deviceId, ACTIVITY_STATE, false);
                    save(deviceId, INACTIVITY_ALARM_TIME, ts);
                    pushRuleEngineMessage(stateData, INACTIVITY_EVENT);
                } else {
                    cleanupEntity(deviceId);
                }
            }
        } else {
            log.debug("[{}] Device that belongs to other server is detected and removed.", deviceId);
            cleanupEntity(deviceId);
        }
    }

    boolean isActive(long ts, DeviceState state) {
        return ts < state.getLastActivityTime() + state.getInactivityTimeout();
    }

    @Nonnull
    DeviceStateData getOrFetchDeviceStateData(DeviceId deviceId) {
        DeviceStateData deviceStateData = deviceStates.get(deviceId);
        if (deviceStateData != null) {
            return deviceStateData;
        }
        return fetchDeviceStateDataUsingEntityDataQuery(deviceId);
    }

    DeviceStateData fetchDeviceStateDataUsingEntityDataQuery(final DeviceId deviceId) {
        final Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
        if (device == null) {
            log.warn("[{}] Failed to fetch device by Id!", deviceId);
            throw new RuntimeException("Failed to fetch device by Id " + deviceId);
        }
        try {
            DeviceStateData deviceStateData = fetchDeviceState(device).get();
            deviceStates.putIfAbsent(deviceId, deviceStateData);
            return deviceStateData;
        } catch (InterruptedException | ExecutionException e) {
            log.warn("[{}] Failed to fetch device state!", deviceId, e);
            throw new RuntimeException(e);
        }
    }

    private boolean cleanDeviceStateIfBelongsExternalPartition(TenantId tenantId, final DeviceId deviceId) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        boolean cleanup = !partitionedEntities.containsKey(tpi);
        if (cleanup) {
            cleanupEntity(deviceId);
            log.debug("[{}][{}] device belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                    , tenantId, deviceId, tpi.getFullTopicName());
        }
        return cleanup;
    }

    private void onDeviceDeleted(TenantId tenantId, DeviceId deviceId) {
        cleanupEntity(deviceId);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        Set<DeviceId> deviceIdSet = partitionedEntities.get(tpi);
        if (deviceIdSet != null) {
            deviceIdSet.remove(deviceId);
        }
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(DeviceId deviceId) {
        cleanupEntity(deviceId);
    }

    private void cleanupEntity(DeviceId deviceId) {
        deviceStates.remove(deviceId);
    }


    private ListenableFuture<DeviceStateData> fetchDeviceState(Device device) {
        ListenableFuture<DeviceStateData> future;
        if (persistToTelemetry) {
            ListenableFuture<List<TsKvEntry>> tsData = tsService.findLatest(TenantId.SYS_TENANT_ID, device.getId(), PERSISTENT_ATTRIBUTES);
            future = Futures.transform(tsData, extractDeviceStateData(device), deviceStateExecutor);
        } else {
            ListenableFuture<List<AttributeKvEntry>> attrData = attributesService.find(TenantId.SYS_TENANT_ID, device.getId(), DataConstants.SERVER_SCOPE, PERSISTENT_ATTRIBUTES);
            future = Futures.transform(attrData, extractDeviceStateData(device), deviceStateExecutor);
        }
        return transformInactivityTimeout(future);
    }

    private ListenableFuture<DeviceStateData> transformInactivityTimeout(ListenableFuture<DeviceStateData> future) {
        return Futures.transformAsync(future, deviceStateData -> {
            if (!persistToTelemetry || deviceStateData.getState().getInactivityTimeout() != defaultInactivityTimeoutMs) {
                return future; //fail fast
            }
            var attributesFuture = attributesService.find(TenantId.SYS_TENANT_ID, deviceStateData.getDeviceId(), SERVER_SCOPE, INACTIVITY_TIMEOUT);
            return Futures.transform(attributesFuture, attributes -> {
                attributes.flatMap(KvEntry::getLongValue).ifPresent((inactivityTimeout) -> {
                    if (inactivityTimeout > 0) {
                        deviceStateData.getState().setInactivityTimeout(inactivityTimeout);
                    }
                });
                return deviceStateData;
            }, deviceStateExecutor);
        }, deviceStateExecutor);
    }

    private <T extends KvEntry> Function<List<T>, DeviceStateData> extractDeviceStateData(Device device) {
        return new Function<>() {
            @Nonnull
            @Override
            public DeviceStateData apply(@Nullable List<T> data) {
                try {
                    long lastActivityTime = getEntryValue(data, LAST_ACTIVITY_TIME, 0L);
                    long inactivityAlarmTime = getEntryValue(data, INACTIVITY_ALARM_TIME, 0L);
                    long inactivityTimeout = getEntryValue(data, INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
                    //Actual active state by wall-clock will updated outside this method. This method is only for fetch persistent state
                    final boolean active = getEntryValue(data, ACTIVITY_STATE, false);
                    DeviceState deviceState = DeviceState.builder()
                            .active(active)
                            .lastConnectTime(getEntryValue(data, LAST_CONNECT_TIME, 0L))
                            .lastDisconnectTime(getEntryValue(data, LAST_DISCONNECT_TIME, 0L))
                            .lastActivityTime(lastActivityTime)
                            .lastInactivityAlarmTime(inactivityAlarmTime)
                            .inactivityTimeout(inactivityTimeout)
                            .build();
                    TbMsgMetaData md = new TbMsgMetaData();
                    md.putValue("deviceName", device.getName());
                    md.putValue("deviceType", device.getType());
                    DeviceStateData deviceStateData = DeviceStateData.builder()
                            .customerId(device.getCustomerId())
                            .tenantId(device.getTenantId())
                            .deviceId(device.getId())
                            .deviceCreationTime(device.getCreatedTime())
                            .metaData(md)
                            .state(deviceState).build();
                    log.debug("[{}] Fetched device state from the DB {}", device.getId(), deviceStateData);
                    return deviceStateData;
                } catch (Exception e) {
                    log.warn("[{}] Failed to fetch device state data", device.getId(), e);
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private List<DeviceStateData> fetchDeviceStateDataUsingSeparateRequests(List<DeviceIdInfo> deviceIds) {
        List<Device> devices = deviceService.findDevicesByIds(deviceIds.stream().map(DeviceIdInfo::getDeviceId).collect(Collectors.toList()));
        List<ListenableFuture<DeviceStateData>> deviceStateFutures = new ArrayList<>();
        for (Device device : devices) {
            deviceStateFutures.add(fetchDeviceState(device));
        }
        try {
            List<DeviceStateData> result = Futures.successfulAsList(deviceStateFutures).get(5, TimeUnit.MINUTES);
            boolean success = true;
            for (int i = 0; i < result.size(); i++) {
                success = false;
                if (result.get(i) == null) {
                    DeviceIdInfo deviceIdInfo = deviceIds.get(i);
                    log.warn("[{}][{}] Failed to initialized device state due to:", deviceIdInfo.getTenantId(), deviceIdInfo.getDeviceId());
                }
            }
            return success ? result : result.stream().filter(Objects::nonNull).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Failed to initialized device state futures for ids: {} due to:", deviceIds, e);
            throw new RuntimeException(e);
        }
    }

    private List<DeviceStateData> fetchDeviceStateDataUsingEntityDataQuery(List<DeviceIdInfo> deviceIds) {
        EntityListFilter ef = new EntityListFilter();
        ef.setEntityType(EntityType.DEVICE);
        ef.setEntityList(deviceIds.stream().map(DeviceIdInfo::getDeviceId).map(DeviceId::getId).map(UUID::toString).collect(Collectors.toList()));

        EntityDataQuery query = new EntityDataQuery(ef,
                new EntityDataPageLink(deviceIds.size(), 0, null, null),
                PERSISTENT_ENTITY_FIELDS,
                persistToTelemetry ? PERSISTENT_TELEMETRY_KEYS : PERSISTENT_ATTRIBUTE_KEYS, Collections.emptyList());
        PageData<EntityData> queryResult = entityQueryRepository.findEntityDataByQueryInternal(query);

        Map<EntityId, DeviceIdInfo> deviceIdInfos = deviceIds.stream().collect(Collectors.toMap(DeviceIdInfo::getDeviceId, java.util.function.Function.identity()));

        return queryResult.getData().stream().map(ed -> toDeviceStateData(ed, deviceIdInfos.get(ed.getEntityId()))).collect(Collectors.toList());

    }

    DeviceStateData toDeviceStateData(EntityData ed, DeviceIdInfo deviceIdInfo) {
        long lastActivityTime = getEntryValue(ed, getKeyType(), LAST_ACTIVITY_TIME, 0L);
        long inactivityAlarmTime = getEntryValue(ed, getKeyType(), INACTIVITY_ALARM_TIME, 0L);
        long inactivityTimeout = getEntryValue(ed, getKeyType(), INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
        if (persistToTelemetry && inactivityTimeout == defaultInactivityTimeoutMs) {
            log.trace("[{}] default value for inactivity timeout fetched {}, going to fetch inactivity timeout from attributes",
                    deviceIdInfo.getDeviceId(), inactivityTimeout);
            inactivityTimeout = getEntryValue(ed, EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
        }
        //Actual active state by wall-clock will updated outside this method. This method is only for fetch persistent state
        final boolean active = getEntryValue(ed, getKeyType(), ACTIVITY_STATE, false);
        DeviceState deviceState = DeviceState.builder()
                .active(active)
                .lastConnectTime(getEntryValue(ed, getKeyType(), LAST_CONNECT_TIME, 0L))
                .lastDisconnectTime(getEntryValue(ed, getKeyType(), LAST_DISCONNECT_TIME, 0L))
                .lastActivityTime(lastActivityTime)
                .lastInactivityAlarmTime(inactivityAlarmTime)
                .inactivityTimeout(inactivityTimeout)
                .build();
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("deviceName", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "name", ""));
        md.putValue("deviceType", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "type", ""));
        return DeviceStateData.builder()
                .customerId(deviceIdInfo.getCustomerId())
                .tenantId(deviceIdInfo.getTenantId())
                .deviceId(deviceIdInfo.getDeviceId())
                .deviceCreationTime(getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "createdTime", 0L))
                .metaData(md)
                .state(deviceState).build();
    }

    private EntityKeyType getKeyType() {
        return persistToTelemetry ? EntityKeyType.TIME_SERIES : EntityKeyType.SERVER_ATTRIBUTE;
    }

    private String getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, String defaultValue) {
        return getEntryValue(ed, keyType, keyName, s -> s, defaultValue);
    }

    private long getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, long defaultValue) {
        return getEntryValue(ed, keyType, keyName, Long::parseLong, defaultValue);
    }

    private boolean getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, boolean defaultValue) {
        return getEntryValue(ed, keyType, keyName, Boolean::parseBoolean, defaultValue);
    }

    private <T> T getEntryValue(EntityData ed, EntityKeyType entityKeyType, String attributeName, Function<String, T> converter, T defaultValue) {
        if (ed != null && ed.getLatest() != null) {
            var map = ed.getLatest().get(entityKeyType);
            if (map != null) {
                var value = map.get(attributeName);
                if (value != null && !StringUtils.isEmpty(value.getValue())) {
                    try {
                        return converter.apply(value.getValue());
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }


    private long getEntryValue(List<? extends KvEntry> kvEntries, String attributeName, long defaultValue) {
        if (kvEntries != null) {
            for (KvEntry entry : kvEntries) {
                if (entry != null && !StringUtils.isEmpty(entry.getKey()) && entry.getKey().equals(attributeName)) {
                    return entry.getLongValue().orElse(defaultValue);
                }
            }
        }
        return defaultValue;
    }

    private boolean getEntryValue(List<? extends KvEntry> kvEntries, String attributeName, boolean defaultValue) {
        if (kvEntries != null) {
            for (KvEntry entry : kvEntries) {
                if (entry != null && !StringUtils.isEmpty(entry.getKey()) && entry.getKey().equals(attributeName)) {
                    return entry.getBooleanValue().orElse(defaultValue);
                }
            }
        }
        return defaultValue;
    }

    private void pushRuleEngineMessage(DeviceStateData stateData, String msgType) {
        DeviceState state = stateData.getState();
        try {
            String data;
            if (msgType.equals(CONNECT_EVENT)) {
                ObjectNode stateNode = JacksonUtil.convertValue(state, ObjectNode.class);
                stateNode.remove(ACTIVITY_STATE);
                data = JacksonUtil.toString(stateNode);
            } else {
                data = JacksonUtil.toString(state);
            }
            TbMsgMetaData md = stateData.getMetaData().copy();
            if (!persistToTelemetry) {
                md.putValue(DataConstants.SCOPE, SERVER_SCOPE);
            }
            TbMsg tbMsg = TbMsg.newMsg(msgType, stateData.getDeviceId(), stateData.getCustomerId(), md, TbMsgDataType.JSON, data);
            clusterService.pushMsgToRuleEngine(stateData.getTenantId(), stateData.getDeviceId(), tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}] Failed to push inactivity alarm: {}", stateData.getDeviceId(), state, e);
        }
    }

    private void save(DeviceId deviceId, String key, long value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotifyInternal(
                    TenantId.SYS_TENANT_ID, deviceId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry(key, value))),
                    new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, deviceId, DataConstants.SERVER_SCOPE, key, value, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    private void save(DeviceId deviceId, String key, boolean value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotifyInternal(
                    TenantId.SYS_TENANT_ID, deviceId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))),
                    new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, deviceId, DataConstants.SERVER_SCOPE, key, value, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    private static class TelemetrySaveCallback<T> implements FutureCallback<T> {
        private final DeviceId deviceId;
        private final String key;
        private final Object value;

        TelemetrySaveCallback(DeviceId deviceId, String key, Object value) {
            this.deviceId = deviceId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@Nullable T result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", deviceId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", deviceId, key, value, t);
        }
    }
}
