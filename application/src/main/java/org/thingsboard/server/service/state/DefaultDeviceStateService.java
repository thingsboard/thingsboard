/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityTrigger;
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
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.sql.query.EntityQueryRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.DbTypeInfoComponent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

/**
 * Created by ashvayka on 01.05.18.
 */
@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
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
            new EntityKey(EntityKeyType.TIME_SERIES, ACTIVITY_STATE),
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_CONNECT_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_DISCONNECT_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT)); // inactivity timeout is always a server attribute, even when activity data is stored as time series

    private static final List<EntityKey> PERSISTENT_ATTRIBUTE_KEYS = Arrays.asList(
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_ACTIVITY_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_ALARM_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, ACTIVITY_STATE),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_CONNECT_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_DISCONNECT_TIME));

    public static final Set<String> ACTIVITY_KEYS_WITHOUT_INACTIVITY_TIMEOUT = Set.of(
            ACTIVITY_STATE, LAST_CONNECT_TIME, LAST_DISCONNECT_TIME, LAST_ACTIVITY_TIME, INACTIVITY_ALARM_TIME
    );

    public static final Set<String> ACTIVITY_KEYS_WITH_INACTIVITY_TIMEOUT = Set.of(
            ACTIVITY_STATE, LAST_CONNECT_TIME, LAST_DISCONNECT_TIME, LAST_ACTIVITY_TIME, INACTIVITY_ALARM_TIME, INACTIVITY_TIMEOUT
    );

    private static final List<EntityKey> PERSISTENT_ENTITY_FIELDS = Arrays.asList(
            new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "type"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "label"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));

    private final DeviceService deviceService;
    private final AttributesService attributesService;
    private final TimeseriesService tsService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    private final EntityQueryRepository entityQueryRepository;
    private final DbTypeInfoComponent dbTypeInfoComponent;
    private final TbApiUsageReportClient apiUsageReportClient;
    private final NotificationRuleProcessor notificationRuleProcessor;
    @Autowired
    @Lazy
    private TelemetrySubscriptionService tsSubService;

    @Value("#{${state.defaultInactivityTimeoutInSec} * 1000}")
    private long defaultInactivityTimeoutMs;

    @Value("${state.defaultStateCheckIntervalInSec}")
    private int defaultStateCheckIntervalInSec;

    @Value("${usage.stats.devices.report_interval:60}")
    private int defaultActivityStatsIntervalInSec;

    @Value("${state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @Value("${state.initFetchPackSize:50000}")
    private int initFetchPackSize;

    @Value("${state.telemetryTtl:0}")
    private int telemetryTtl;

    private ListeningExecutorService deviceStateExecutor;
    private ListeningExecutorService deviceStateCallbackExecutor;

    final ConcurrentMap<DeviceId, DeviceStateData> deviceStates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        super.init();
        deviceStateExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "device-state"));
        deviceStateCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "device-state-callback"));
        scheduledExecutor.scheduleWithFixedDelay(this::checkStates, new Random().nextInt(defaultStateCheckIntervalInSec), defaultStateCheckIntervalInSec, TimeUnit.SECONDS);
        scheduledExecutor.scheduleWithFixedDelay(this::reportActivityStats, defaultActivityStatsIntervalInSec, defaultActivityStatsIntervalInSec, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        super.stop();
        if (deviceStateExecutor != null) {
            deviceStateExecutor.shutdownNow();
        }
        if (deviceStateCallbackExecutor != null) {
            deviceStateCallbackExecutor.shutdownNow();
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
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long lastConnectTime) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (lastConnectTime < 0) {
            log.trace("[{}][{}] On device connect: received negative last connect ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastConnectTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastConnectTime = stateData.getState().getLastConnectTime();
        if (lastConnectTime <= currentLastConnectTime) {
            log.trace("[{}][{}] On device connect: received outdated last connect ts [{}]. Skipping this event. Current last connect ts [{}].",
                    tenantId.getId(), deviceId.getId(), lastConnectTime, currentLastConnectTime);
            return;
        }
        log.trace("[{}][{}] On device connect: processing connect event with ts [{}].", tenantId.getId(), deviceId.getId(), lastConnectTime);
        stateData.getState().setLastConnectTime(lastConnectTime);
        save(tenantId, deviceId, LAST_CONNECT_TIME, lastConnectTime);
        pushRuleEngineMessage(stateData, TbMsgType.CONNECT_EVENT);
        checkAndUpdateState(deviceId, stateData);
    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long lastReportedActivity) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        log.trace("[{}] on Device Activity [{}], lastReportedActivity [{}]", tenantId.getId(), deviceId.getId(), lastReportedActivity);
        final DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (lastReportedActivity > 0 && lastReportedActivity > stateData.getState().getLastActivityTime()) {
            updateActivityState(deviceId, stateData, lastReportedActivity);
        }
    }

    void updateActivityState(DeviceId deviceId, DeviceStateData stateData, long lastReportedActivity) {
        log.trace("updateActivityState - fetched state {} for device {}, lastReportedActivity {}", stateData, deviceId, lastReportedActivity);
        if (stateData != null) {
            save(stateData.getTenantId(), deviceId, LAST_ACTIVITY_TIME, lastReportedActivity);
            DeviceState state = stateData.getState();
            state.setLastActivityTime(lastReportedActivity);
            if (!state.isActive()) {
                if (lastReportedActivity <= state.getLastInactivityAlarmTime()) {
                    state.setLastInactivityAlarmTime(0);
                    save(stateData.getTenantId(), deviceId, INACTIVITY_ALARM_TIME, 0);
                }
                onDeviceActivityStatusChange(true, stateData);
            }
        } else {
            log.debug("updateActivityState - fetched state IS NULL for device {}, lastReportedActivity {}", deviceId, lastReportedActivity);
            cleanupEntity(deviceId);
        }
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long lastDisconnectTime) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (lastDisconnectTime < 0) {
            log.trace("[{}][{}] On device disconnect: received negative last disconnect ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastDisconnectTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastDisconnectTime = stateData.getState().getLastDisconnectTime();
        if (lastDisconnectTime <= currentLastDisconnectTime) {
            log.trace("[{}][{}] On device disconnect: received outdated last disconnect ts [{}]. Skipping this event. Current last disconnect ts [{}].",
                    tenantId.getId(), deviceId.getId(), lastDisconnectTime, currentLastDisconnectTime);
            return;
        }
        log.trace("[{}][{}] On device disconnect: processing disconnect event with ts [{}].", tenantId.getId(), deviceId.getId(), lastDisconnectTime);
        stateData.getState().setLastDisconnectTime(lastDisconnectTime);
        save(tenantId, deviceId, LAST_DISCONNECT_TIME, lastDisconnectTime);
        pushRuleEngineMessage(stateData, TbMsgType.DISCONNECT_EVENT);
    }

    @Override
    public void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (inactivityTimeout <= 0L) {
            inactivityTimeout = defaultInactivityTimeoutMs;
        }
        log.trace("[{}] on Device Activity Timeout Update device id {} inactivityTimeout {}", tenantId.getId(), deviceId.getId(), inactivityTimeout);
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        stateData.getState().setInactivityTimeout(inactivityTimeout);
        checkAndUpdateState(deviceId, stateData);
    }

    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long lastInactivityTime) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (lastInactivityTime < 0) {
            log.trace("[{}][{}] On device inactivity: received negative last inactivity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastInactivityAlarmTime = stateData.getState().getLastInactivityAlarmTime();
        if (lastInactivityTime <= currentLastInactivityAlarmTime) {
            log.trace("[{}][{}] On device inactivity: received last inactivity ts [{}] is less than current last inactivity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime, currentLastInactivityAlarmTime);
            return;
        }
        long currentLastActivityTime = stateData.getState().getLastActivityTime();
        if (lastInactivityTime <= currentLastActivityTime) {
            log.trace("[{}][{}] On device inactivity: received last inactivity ts [{}] is less or equal to current last activity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime, currentLastActivityTime);
            return;
        }
        log.trace("[{}][{}] On device inactivity: processing inactivity event with ts [{}].", tenantId.getId(), deviceId.getId(), lastInactivityTime);
        reportInactivity(lastInactivityTime, stateData);
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
                            public void onSuccess(DeviceStateData state) {
                                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, device.getId());
                                Set<DeviceId> deviceIds = partitionedEntities.get(tpi);
                                boolean isMyPartition = deviceIds != null;
                                if (isMyPartition) {
                                    deviceIds.add(state.getDeviceId());
                                    initializeActivityState(deviceId, state);
                                    callback.onSuccess();
                                } else {
                                    log.debug("[{}][{}] Device belongs to external partition. Probably rebalancing is in progress. Topic: {}", tenantId, deviceId, tpi.getFullTopicName());
                                    callback.onFailure(new RuntimeException("Device belongs to external partition " + tpi.getFullTopicName() + "!"));
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                log.warn("Failed to register device to the state service", t);
                                callback.onFailure(t);
                            }
                        }, deviceStateCallbackExecutor);
                    } else if (proto.getUpdated()) {
                        DeviceStateData stateData = getOrFetchDeviceStateData(device.getId());
                        TbMsgMetaData md = new TbMsgMetaData();
                        md.putValue("deviceName", device.getName());
                        md.putValue("deviceLabel", device.getLabel());
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

    private void onDeviceDeleted(TenantId tenantId, DeviceId deviceId) {
        cleanupEntity(deviceId);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        Set<DeviceId> deviceIdSet = partitionedEntities.get(tpi);
        if (deviceIdSet != null) {
            deviceIdSet.remove(deviceId);
        }
    }

    private void initializeActivityState(DeviceId deviceId, DeviceStateData fetchedState) {
        DeviceStateData cachedState = deviceStates.putIfAbsent(fetchedState.getDeviceId(), fetchedState);
        boolean activityState = Objects.requireNonNullElse(cachedState, fetchedState).getState().isActive();
        save(fetchedState.getTenantId(), deviceId, ACTIVITY_STATE, activityState);
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
                                TopicPartitionInfo tpi = entry.getKey();
                                Set<DeviceId> deviceIds = partitionedEntities.get(tpi);
                                boolean isMyPartition = deviceIds != null;
                                if (isMyPartition) {
                                    deviceIds.add(state.getDeviceId());
                                    deviceStates.putIfAbsent(state.getDeviceId(), state);
                                    checkAndUpdateState(state.getDeviceId(), state);
                                } else {
                                    log.debug("[{}] Device belongs to external partition {}", state.getDeviceId(), tpi.getFullTopicName());
                                }
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
        var deviceState = state.getState();
        if (deviceState.isActive()) {
            updateInactivityStateIfExpired(getCurrentTimeMillis(), deviceId, state);
        } else {
            //trying to fix activity state
            if (isActive(getCurrentTimeMillis(), deviceState)) {
                updateActivityState(deviceId, state, deviceState.getLastActivityTime());
                if (deviceState.getLastInactivityAlarmTime() != 0L && deviceState.getLastInactivityAlarmTime() >= deviceState.getLastActivityTime()) {
                    deviceState.setLastInactivityAlarmTime(0L);
                    save(state.getTenantId(), deviceId, INACTIVITY_ALARM_TIME, 0L);
                }
            }
        }
    }

    void checkStates() {
        try {
            final long ts = getCurrentTimeMillis();
            partitionedEntities.forEach((tpi, deviceIds) -> {
                log.debug("Calculating state updates. tpi {} for {} devices", tpi.getFullTopicName(), deviceIds.size());
                Set<DeviceId> idsFromRemovedTenant = new HashSet<>();
                for (DeviceId deviceId : deviceIds) {
                    DeviceStateData stateData;
                    try {
                        stateData = getOrFetchDeviceStateData(deviceId);
                    } catch (Exception e) {
                        log.error("[{}] Failed to get or fetch device state data", deviceId, e);
                        continue;
                    }
                    try {
                        updateInactivityStateIfExpired(ts, deviceId, stateData);
                    } catch (Exception e) {
                        if (e instanceof TenantNotFoundException) {
                            idsFromRemovedTenant.add(deviceId);
                        } else {
                            log.warn("[{}] Failed to update inactivity state [{}]", deviceId, e.getMessage());
                        }
                    }
                }
                deviceIds.removeAll(idsFromRemovedTenant);
            });
        } catch (Throwable t) {
            log.warn("Failed to check devices states", t);
        }
    }

    private void reportActivityStats() {
        try {
            Map<TenantId, Pair<AtomicInteger, AtomicInteger>> stats = new HashMap<>();
            for (DeviceStateData stateData : deviceStates.values()) {
                Pair<AtomicInteger, AtomicInteger> tenantDevicesActivity = stats.computeIfAbsent(stateData.getTenantId(),
                        tenantId -> Pair.of(new AtomicInteger(), new AtomicInteger()));
                if (stateData.getState().isActive()) {
                    tenantDevicesActivity.getLeft().incrementAndGet();
                } else {
                    tenantDevicesActivity.getRight().incrementAndGet();
                }
            }

            stats.forEach((tenantId, tenantDevicesActivity) -> {
                int active = tenantDevicesActivity.getLeft().get();
                int inactive = tenantDevicesActivity.getRight().get();
                apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.ACTIVE_DEVICES, active);
                apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.INACTIVE_DEVICES, inactive);
                if (active > 0) {
                    log.debug("[{}] Active devices: {}, inactive devices: {}", tenantId, active, inactive);
                }
            });
        } catch (Throwable t) {
            log.warn("Failed to report activity states", t);
        }
    }

    void updateInactivityStateIfExpired(long ts, DeviceId deviceId, DeviceStateData stateData) {
        log.trace("Processing state {} for device {}", stateData, deviceId);
        if (stateData != null) {
            DeviceState state = stateData.getState();
            if (!isActive(ts, state)
                    && (state.getLastInactivityAlarmTime() == 0L || state.getLastInactivityAlarmTime() <= state.getLastActivityTime())
                    && stateData.getDeviceCreationTime() + state.getInactivityTimeout() <= ts) {
                if (partitionService.resolve(ServiceType.TB_CORE, stateData.getTenantId(), deviceId).isMyPartition()) {
                    reportInactivity(ts, stateData);
                } else {
                    cleanupEntity(deviceId);
                }
            }
        } else {
            log.debug("[{}] Device that belongs to other server is detected and removed.", deviceId);
            cleanupEntity(deviceId);
        }
    }

    private void reportInactivity(long ts, DeviceStateData stateData) {
        var tenantId = stateData.getTenantId();
        var deviceId = stateData.getDeviceId();

        Futures.addCallback(save(stateData.getTenantId(), deviceId, INACTIVITY_ALARM_TIME, ts), new FutureCallback<>() {
            @Override
            public void onSuccess(Void success) {
                stateData.getState().setLastInactivityAlarmTime(ts);
                onDeviceActivityStatusChange(false, stateData);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                log.error("[{}][{}] Failed to update device last inactivity alarm time to '{}'. Device state data: {}", tenantId, deviceId, ts, stateData, t);
            }
        }, deviceStateCallbackExecutor);
    }

    private static boolean isActive(long ts, DeviceState state) {
        return ts < state.getLastActivityTime() + state.getInactivityTimeout();
    }

    @Nonnull
    DeviceStateData getOrFetchDeviceStateData(DeviceId deviceId) {
        return deviceStates.computeIfAbsent(deviceId, this::fetchDeviceStateDataUsingSeparateRequests);
    }

    DeviceStateData fetchDeviceStateDataUsingSeparateRequests(final DeviceId deviceId) {
        final Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
        if (device == null) {
            log.warn("[{}] Failed to fetch device by Id!", deviceId);
            throw new RuntimeException("Failed to fetch device by id [" + deviceId + "]!");
        }
        try {
            return fetchDeviceState(device).get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("[{}] Failed to fetch device state!", deviceId, e);
            throw new RuntimeException("Failed to fetch device state for device [" + deviceId + "]");
        }
    }

    private void onDeviceActivityStatusChange(boolean active, DeviceStateData stateData) {
        var tenantId = stateData.getTenantId();
        var deviceId = stateData.getDeviceId();

        Futures.addCallback(save(tenantId, deviceId, ACTIVITY_STATE, active), new FutureCallback<>() {
            @Override
            public void onSuccess(Void success) {
                stateData.getState().setActive(active);
                pushRuleEngineMessage(stateData, active ? TbMsgType.ACTIVITY_EVENT : TbMsgType.INACTIVITY_EVENT);
                TbMsgMetaData metaData = stateData.getMetaData();
                notificationRuleProcessor.process(DeviceActivityTrigger.builder()
                        .tenantId(tenantId)
                        .customerId(stateData.getCustomerId())
                        .deviceId(deviceId)
                        .active(active)
                        .deviceName(metaData.getValue("deviceName"))
                        .deviceType(metaData.getValue("deviceType"))
                        .deviceLabel(metaData.getValue("deviceLabel"))
                        .build());
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                log.error("[{}][{}] Failed to change device activity status to '{}'. Device state data: {}", tenantId, deviceId, active, stateData, t);
            }
        }, deviceStateCallbackExecutor);
    }

    boolean cleanDeviceStateIfBelongsToExternalPartition(TenantId tenantId, final DeviceId deviceId) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        boolean cleanup = !partitionedEntities.containsKey(tpi);
        if (cleanup) {
            cleanupEntity(deviceId);
            log.debug("[{}][{}] device belongs to external partition. Probably rebalancing is in progress. Topic: {}", tenantId, deviceId, tpi.getFullTopicName());
        }
        return cleanup;
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
            ListenableFuture<List<TsKvEntry>> timeseriesActivityDataFuture = tsService.findLatest(TenantId.SYS_TENANT_ID, device.getId(), ACTIVITY_KEYS_WITHOUT_INACTIVITY_TIMEOUT);
            ListenableFuture<Optional<AttributeKvEntry>> inactivityTimeoutAttributeFuture = attributesService.find(
                    TenantId.SYS_TENANT_ID, device.getId(), AttributeScope.SERVER_SCOPE, INACTIVITY_TIMEOUT
            );

            ListenableFuture<List<? extends KvEntry>> fullActivityDataFuture = Futures.whenAllSucceed(timeseriesActivityDataFuture, inactivityTimeoutAttributeFuture).call(() -> {
                List<TsKvEntry> activityTimeseries = Futures.getDone(timeseriesActivityDataFuture);
                Optional<AttributeKvEntry> inactivityTimeoutAttribute = Futures.getDone(inactivityTimeoutAttributeFuture);

                if (inactivityTimeoutAttribute.isPresent()) {
                    List<KvEntry> result = new ArrayList<>(activityTimeseries.size() + 1);
                    result.addAll(activityTimeseries);
                    result.add(inactivityTimeoutAttribute.get());
                    return result;
                } else {
                    return activityTimeseries;
                }
            }, deviceStateCallbackExecutor);

            future = Futures.transform(fullActivityDataFuture, extractDeviceStateData(device), MoreExecutors.directExecutor());
        } else {
            ListenableFuture<List<AttributeKvEntry>> attributesActivityDataFuture = attributesService.find(
                    TenantId.SYS_TENANT_ID, device.getId(), AttributeScope.SERVER_SCOPE, ACTIVITY_KEYS_WITH_INACTIVITY_TIMEOUT
            );
            future = Futures.transform(attributesActivityDataFuture, extractDeviceStateData(device), MoreExecutors.directExecutor());
        }
        return future;
    }

    private Function<List<? extends KvEntry>, DeviceStateData> extractDeviceStateData(Device device) {
        return new Function<>() {
            @Nonnull
            @Override
            public DeviceStateData apply(@Nullable List<? extends KvEntry> data) {
                try {
                    long lastActivityTime = getEntryValue(data, LAST_ACTIVITY_TIME, 0L);
                    long inactivityAlarmTime = getEntryValue(data, INACTIVITY_ALARM_TIME, 0L);
                    long inactivityTimeout = getEntryValue(data, INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
                    // Actual active state by wall-clock will be updated outside this method. This method is only for fetching persistent state
                    final boolean active = getEntryValue(data, ACTIVITY_STATE, false);
                    DeviceState deviceState = DeviceState.builder()
                            .active(active)
                            .lastConnectTime(getEntryValue(data, LAST_CONNECT_TIME, 0L))
                            .lastDisconnectTime(getEntryValue(data, LAST_DISCONNECT_TIME, 0L))
                            .lastActivityTime(lastActivityTime)
                            .lastInactivityAlarmTime(inactivityAlarmTime)
                            .inactivityTimeout(inactivityTimeout > 0 ? inactivityTimeout : defaultInactivityTimeoutMs)
                            .build();
                    TbMsgMetaData md = new TbMsgMetaData();
                    md.putValue("deviceName", device.getName());
                    md.putValue("deviceLabel", device.getLabel());
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
                    throw new RuntimeException("Failed to fetch device state data for device [" + device.getId() + "]", e);
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
            String deviceIdsStr = deviceIds.stream()
                    .map(DeviceIdInfo::getDeviceId)
                    .map(UUIDBased::getId)
                    .map(UUID::toString)
                    .collect(Collectors.joining(", "));
            log.warn("Failed to initialized device state futures for ids [{}] due to:", deviceIdsStr, e);
            throw new RuntimeException("Failed to initialized device state futures for ids [" + deviceIdsStr + "]!", e);
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

    private DeviceStateData toDeviceStateData(EntityData ed, DeviceIdInfo deviceIdInfo) {
        long lastActivityTime = getEntryValue(ed, getKeyType(), LAST_ACTIVITY_TIME, 0L);
        long inactivityAlarmTime = getEntryValue(ed, getKeyType(), INACTIVITY_ALARM_TIME, 0L);
        long inactivityTimeout = getEntryValue(ed, EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
        // Actual active state by wall-clock will be updated outside this method. This method is only for fetching persistent state
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
        md.putValue("deviceLabel", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "label", ""));
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

    private void pushRuleEngineMessage(DeviceStateData stateData, TbMsgType msgType) {
        var tenantId = stateData.getTenantId();
        var deviceId = stateData.getDeviceId();

        DeviceState state = stateData.getState();
        try {
            String data;
            if (msgType.equals(TbMsgType.CONNECT_EVENT)) {
                ObjectNode stateNode = JacksonUtil.convertValue(state, ObjectNode.class);
                stateNode.remove(ACTIVITY_STATE);
                data = JacksonUtil.toString(stateNode);
            } else {
                data = JacksonUtil.toString(state);
            }
            TbMsgMetaData md = stateData.getMetaData().copy();
            if (!persistToTelemetry) {
                md.putValue(SCOPE, SERVER_SCOPE);
            }
            TbMsg tbMsg = TbMsg.newMsg()
                    .type(msgType)
                    .originator(deviceId)
                    .customerId(stateData.getCustomerId())
                    .copyMetaData(md)
                    .dataType(TbMsgDataType.JSON)
                    .data(data)
                    .build();
            clusterService.pushMsgToRuleEngine(stateData.getTenantId(), stateData.getDeviceId(), tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push '{}' message to the rule engine due to {}. Device state: {}", tenantId, deviceId, msgType, e.getMessage(), state);
        }
    }

    private ListenableFuture<Void> save(TenantId tenantId, DeviceId deviceId, String key, long value) {
        return save(tenantId, deviceId, new LongDataEntry(key, value), getCurrentTimeMillis());
    }

    private ListenableFuture<Void> save(TenantId tenantId, DeviceId deviceId, String key, boolean value) {
        return save(tenantId, deviceId, new BooleanDataEntry(key, value), getCurrentTimeMillis());
    }

    private ListenableFuture<Void> save(TenantId tenantId, DeviceId deviceId, KvEntry kvEntry, long ts) {
        ListenableFuture<?> future;
        if (persistToTelemetry) {
            future = tsSubService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(deviceId)
                    .entry(new BasicTsKvEntry(ts, kvEntry))
                    .ttl(telemetryTtl)
                    .callback(new TelemetrySaveCallback<>(deviceId, kvEntry))
                    .build());
        } else {
            future = tsSubService.saveAttributesInternal(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(deviceId)
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new BaseAttributeKvEntry(ts, kvEntry))
                    .callback(new TelemetrySaveCallback<>(deviceId, kvEntry))
                    .build());
        }
        return Futures.transform(future, __ -> null, MoreExecutors.directExecutor());
    }

    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    private record TelemetrySaveCallback<T>(DeviceId deviceId, KvEntry kvEntry) implements FutureCallback<T> {

        @Override
        public void onSuccess(@Nullable T result) {
            log.trace("[{}] Successfully updated entry {}", deviceId, kvEntry);
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            log.warn("[{}] Failed to update entry {}", deviceId, kvEntry, t);
        }

    }

}
