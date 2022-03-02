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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
public class DefaultDeviceStateService extends TbApplicationEventListener<PartitionChangeEvent> implements DeviceStateService {

    public static final String ACTIVITY_STATE = "active";
    public static final String LAST_CONNECT_TIME = "lastConnectTime";
    public static final String LAST_DISCONNECT_TIME = "lastDisconnectTime";
    public static final String LAST_ACTIVITY_TIME = "lastActivityTime";
    public static final String INACTIVITY_ALARM_TIME = "inactivityAlarmTime";
    public static final String INACTIVITY_TIMEOUT = "inactivityTimeout";

    public static final List<String> PERSISTENT_ATTRIBUTES = Arrays.asList(ACTIVITY_STATE, LAST_CONNECT_TIME,
            LAST_DISCONNECT_TIME, LAST_ACTIVITY_TIME, INACTIVITY_ALARM_TIME, INACTIVITY_TIMEOUT);

    private final TenantService tenantService;
    private final DeviceService deviceService;
    private final AttributesService attributesService;
    private final TimeseriesService tsService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;

    private TelemetrySubscriptionService tsSubService;

    @Value("${state.defaultInactivityTimeoutInSec}")
    @Getter
    private long defaultInactivityTimeoutInSec;

    @Value("${state.defaultStateCheckIntervalInSec}")
    @Getter
    private int defaultStateCheckIntervalInSec;

    @Value("${state.persistToTelemetry:false}")
    @Getter
    private boolean persistToTelemetry;

    @Value("${state.initFetchPackSize:1000}")
    @Getter
    private int initFetchPackSize;

    private ListeningScheduledExecutorService scheduledExecutor;
    private ExecutorService deviceStateExecutor;
    private final ConcurrentMap<TopicPartitionInfo, Set<DeviceId>> partitionedDevices = new ConcurrentHashMap<>();
    final ConcurrentMap<DeviceId, DeviceStateData> deviceStates = new ConcurrentHashMap<>();

    final Queue<Set<TopicPartitionInfo>> subscribeQueue = new ConcurrentLinkedQueue<>();

    public DefaultDeviceStateService(TenantService tenantService, DeviceService deviceService,
                                     AttributesService attributesService, TimeseriesService tsService,
                                     TbClusterService clusterService, PartitionService partitionService) {
        this.tenantService = tenantService;
        this.deviceService = deviceService;
        this.attributesService = attributesService;
        this.tsService = tsService;
        this.clusterService = clusterService;
        this.partitionService = partitionService;
    }

    @Autowired
    public void setTsSubService(TelemetrySubscriptionService tsSubService) {
        this.tsSubService = tsSubService;
    }

    @PostConstruct
    public void init() {
        deviceStateExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("device-state"));
        // Should be always single threaded due to absence of locks.
        scheduledExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("device-state-scheduled")));
        scheduledExecutor.scheduleAtFixedRate(this::updateInactivityStateIfExpired, new Random().nextInt(defaultStateCheckIntervalInSec), defaultStateCheckIntervalInSec, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (deviceStateExecutor != null) {
            deviceStateExecutor.shutdownNow();
        }
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId) {
        log.trace("on Device Connect [{}]", deviceId.getId());
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long ts = System.currentTimeMillis();
        stateData.getState().setLastConnectTime(ts);
        save(deviceId, LAST_CONNECT_TIME, ts);
        pushRuleEngineMessage(stateData, CONNECT_EVENT);
        checkAndUpdateState(deviceId, stateData);
        cleanDeviceStateIfBelongsExternalPartition(tenantId, deviceId);
    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long lastReportedActivity) {
        log.trace("on Device Activity [{}], lastReportedActivity [{}]", deviceId.getId(), lastReportedActivity);
        final DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (lastReportedActivity > 0 && lastReportedActivity > stateData.getState().getLastActivityTime()) {
            updateActivityState(deviceId, stateData, lastReportedActivity);
        }
        cleanDeviceStateIfBelongsExternalPartition(tenantId, deviceId);
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
            cleanUpDeviceStateMap(deviceId);
        }
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId) {
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long ts = System.currentTimeMillis();
        stateData.getState().setLastDisconnectTime(ts);
        save(deviceId, LAST_DISCONNECT_TIME, ts);
        pushRuleEngineMessage(stateData, DISCONNECT_EVENT);
        cleanDeviceStateIfBelongsExternalPartition(tenantId, deviceId);
    }

    @Override
    public void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout) {
        if (inactivityTimeout <= 0L) {
            return;
        }
        log.trace("on Device Activity Timeout Update device id {} inactivityTimeout {}", deviceId, inactivityTimeout);
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        stateData.getState().setInactivityTimeout(inactivityTimeout);
        checkAndUpdateState(deviceId, stateData);
        cleanDeviceStateIfBelongsExternalPartition(tenantId, deviceId);
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
                        Futures.addCallback(fetchDeviceState(device), new FutureCallback<DeviceStateData>() {
                            @Override
                            public void onSuccess(@Nullable DeviceStateData state) {
                                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, device.getId());
                                if (partitionedDevices.containsKey(tpi)) {
                                    addDeviceUsingState(tpi, state);
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

    /**
     * DiscoveryService will call this event from the single thread (one-by-one).
     * Events order is guaranteed by DiscoveryService.
     * The only concurrency is expected from the [main] thread on Application started.
     * Async implementation. Locks is not allowed by design.
     * Any locks or delays in this module will affect DiscoveryService and entire system
     */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            log.debug("onTbApplicationEvent ServiceType is TB_CORE, processing queue {}", partitionChangeEvent);
            subscribeQueue.add(partitionChangeEvent.getPartitions());
            scheduledExecutor.submit(this::pollInitStateFromDB);
        }
    }

    void pollInitStateFromDB() {
        final Set<TopicPartitionInfo> partitions = getLatestPartitionsFromQueue();
        if (partitions == null) {
            log.info("Device state service. Nothing to do. partitions is null");
            return;
        }
        initStateFromDB(partitions);
    }

    // TODO: move to utils
    Set<TopicPartitionInfo> getLatestPartitionsFromQueue() {
        log.debug("getLatestPartitionsFromQueue, queue size {}", subscribeQueue.size());
        Set<TopicPartitionInfo> partitions = null;
        while (!subscribeQueue.isEmpty()) {
            partitions = subscribeQueue.poll();
            log.debug("polled from the queue partitions {}", partitions);
        }
        log.debug("getLatestPartitionsFromQueue, partitions {}", partitions);
        return partitions;
    }

    private void initStateFromDB(Set<TopicPartitionInfo> partitions) {
        try {
            log.info("CURRENT PARTITIONS: {}", partitionedDevices.keySet());
            log.info("NEW PARTITIONS: {}", partitions);

            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(partitionedDevices.keySet());

            log.info("ADDED PARTITIONS: {}", addedPartitions);

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(partitionedDevices.keySet());
            removedPartitions.removeAll(partitions);

            log.info("REMOVED PARTITIONS: {}", removedPartitions);

            // We no longer manage current partition of devices;
            removedPartitions.forEach(partition -> {
                Set<DeviceId> devices = partitionedDevices.remove(partition);
                devices.forEach(this::cleanUpDeviceStateMap);
            });

            addedPartitions.forEach(tpi -> partitionedDevices.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()));

            initPartitions(addedPartitions);

            scheduledExecutor.submit(() -> {
                log.info("Managing following partitions:");
                partitionedDevices.forEach((tpi, devices) -> {
                    log.info("[{}]: {} devices", tpi.getFullTopicName(), devices.size());
                });
            });
        } catch (Throwable t) {
            log.warn("Failed to init device states from DB", t);
        }
    }

    //TODO 3.0: replace this dummy search with new functionality to search by partitions using SQL capabilities.
    //Adding only entities that are in new partitions
    boolean initPartitions(Set<TopicPartitionInfo> addedPartitions) {
        if (addedPartitions.isEmpty()) {
            return false;
        }

        List<Tenant> tenants = tenantService.findTenants(new PageLink(Integer.MAX_VALUE)).getData();
        for (Tenant tenant : tenants) {
            log.debug("Finding devices for tenant [{}]", tenant.getName());
            final PageLink pageLink = new PageLink(initFetchPackSize);
            scheduledExecutor.submit(() -> processPageAndSubmitNextPage(addedPartitions, tenant, pageLink, scheduledExecutor));

        }
        return true;
    }

    private void processPageAndSubmitNextPage(final Set<TopicPartitionInfo> addedPartitions, final Tenant tenant, final PageLink pageLink, final ExecutorService executor) {
        log.trace("[{}] Process page {} from {}", tenant, pageLink.getPage(), pageLink.getPageSize());
        List<ListenableFuture<Void>> fetchFutures = new ArrayList<>();
        PageData<Device> page = deviceService.findDevicesByTenantId(tenant.getId(), pageLink);
        for (Device device : page.getData()) {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenant.getId(), device.getId());
            if (addedPartitions.contains(tpi)) {
                log.debug("[{}][{}] Device belong to current partition. tpi [{}]. Fetching state from DB", device.getName(), device.getId(), tpi);
                ListenableFuture<Void> future = Futures.transform(fetchDeviceState(device), new Function<DeviceStateData, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable DeviceStateData state) {
                        if (state != null) {
                            addDeviceUsingState(tpi, state);
                            checkAndUpdateState(device.getId(), state);
                        } else {
                            log.warn("{}][{}] Fetched null state from DB", device.getName(), device.getId());
                        }
                        return null;
                    }
                }, deviceStateExecutor);
                fetchFutures.add(future);
            } else {
                log.debug("[{}][{}] Device doesn't belong to current partition. tpi [{}]", device.getName(), device.getId(), tpi);
            }
        }

        Futures.addCallback(Futures.successfulAsList(fetchFutures), new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(List<Void> result) {
                log.trace("[{}] Success init device state from DB for batch size {}", tenant.getId(), result.size());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[" + tenant.getId() + "] Failed to init device state service from DB", t);
                log.warn("[{}] Failed to init device state service from DB", tenant.getId(), t);
            }
        }, deviceStateExecutor);

        final PageLink nextPageLink = page.hasNext() ? pageLink.nextPageLink() : null;
        if (nextPageLink != null) {
            log.trace("[{}] Submit next page {} from {}", tenant, nextPageLink.getPage(), nextPageLink.getPageSize());
            executor.submit(() -> processPageAndSubmitNextPage(addedPartitions, tenant, nextPageLink, executor));
        }
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

    private void addDeviceUsingState(TopicPartitionInfo tpi, DeviceStateData state) {
        Set<DeviceId> deviceIds = partitionedDevices.get(tpi);
        if (deviceIds != null) {
            deviceIds.add(state.getDeviceId());
            deviceStates.put(state.getDeviceId(), state);
        } else {
            log.debug("[{}] Device belongs to external partition {}", state.getDeviceId(), tpi.getFullTopicName());
            throw new RuntimeException("Device belongs to external partition " + tpi.getFullTopicName() + "!");
        }
    }

    void updateInactivityStateIfExpired() {
        final long ts = System.currentTimeMillis();
        partitionedDevices.forEach((tpi, deviceIds) -> {
            log.debug("Calculating state updates. tpi {} for {} devices", tpi.getFullTopicName(), deviceIds.size());
            for (DeviceId deviceId : deviceIds) {
                updateInactivityStateIfExpired(ts, deviceId);
            }
        });
    }

    void updateInactivityStateIfExpired(long ts, DeviceId deviceId) {
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        updateInactivityStateIfExpired(ts, deviceId, stateData);
    }

    void updateInactivityStateIfExpired(long ts, DeviceId deviceId, DeviceStateData stateData) {
        log.trace("Processing state {} for device {}", stateData, deviceId);
        if (stateData != null) {
            DeviceState state = stateData.getState();
            if (!isActive(ts, state) && (state.getLastInactivityAlarmTime() == 0L || state.getLastInactivityAlarmTime() < state.getLastActivityTime()) && stateData.getDeviceCreationTime() + state.getInactivityTimeout() < ts) {
                state.setActive(false);
                state.setLastInactivityAlarmTime(ts);
                save(deviceId, INACTIVITY_ALARM_TIME, ts);
                save(deviceId, ACTIVITY_STATE, false);
                pushRuleEngineMessage(stateData, INACTIVITY_EVENT);
            }
        } else {
            log.debug("[{}] Device that belongs to other server is detected and removed.", deviceId);
            cleanUpDeviceStateMap(deviceId);
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
        return fetchDeviceStateData(deviceId);
    }

    DeviceStateData fetchDeviceStateData(final DeviceId deviceId) {
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

    private void cleanDeviceStateIfBelongsExternalPartition(TenantId tenantId, final DeviceId deviceId) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        if (!partitionedDevices.containsKey(tpi)) {
            cleanUpDeviceStateMap(deviceId);
            log.debug("[{}][{}] device belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                    , tenantId, deviceId, tpi.getFullTopicName());
        }
    }

    private void onDeviceDeleted(TenantId tenantId, DeviceId deviceId) {
        cleanUpDeviceStateMap(deviceId);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        Set<DeviceId> deviceIdSet = partitionedDevices.get(tpi);
        deviceIdSet.remove(deviceId);
    }

    private void cleanUpDeviceStateMap(DeviceId deviceId) {
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
            if (!persistToTelemetry || deviceStateData.getState().getInactivityTimeout() != TimeUnit.SECONDS.toMillis(defaultInactivityTimeoutInSec)) {
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
                    long inactivityTimeout = getEntryValue(data, INACTIVITY_TIMEOUT, TimeUnit.SECONDS.toMillis(defaultInactivityTimeoutInSec));
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
