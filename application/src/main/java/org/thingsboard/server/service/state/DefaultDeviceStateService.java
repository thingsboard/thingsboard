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
package org.thingsboard.server.service.state;

import com.datastax.driver.core.utils.UUIDs;
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
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
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
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
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
public class DefaultDeviceStateService implements DeviceStateService {

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

    private volatile boolean clusterUpdatePending = false;

    private ListeningScheduledExecutorService queueExecutor;
    private final ConcurrentMap<TopicPartitionInfo, Set<DeviceId>> partitionedDevices = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, DeviceStateData> deviceStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, Long> deviceLastReportedActivity = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, Long> deviceLastSavedActivity = new ConcurrentHashMap<>();

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
        // Should be always single threaded due to absence of locks.
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("device-state")));
        queueExecutor.scheduleAtFixedRate(this::updateState, new Random().nextInt(defaultStateCheckIntervalInSec), defaultStateCheckIntervalInSec, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
    }

    @Override
    public void onDeviceAdded(Device device) {
        sendDeviceEvent(device.getTenantId(), device.getId(), true, false, false);
    }

    @Override
    public void onDeviceUpdated(Device device) {
        sendDeviceEvent(device.getTenantId(), device.getId(), false, true, false);
    }

    @Override
    public void onDeviceDeleted(Device device) {
        sendDeviceEvent(device.getTenantId(), device.getId(), false, false, true);
    }

    @Override
    public void onDeviceConnect(DeviceId deviceId) {
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (stateData != null) {
            long ts = System.currentTimeMillis();
            stateData.getState().setLastConnectTime(ts);
            pushRuleEngineMessage(stateData, CONNECT_EVENT);
            save(deviceId, LAST_CONNECT_TIME, ts);
        }
    }

    @Override
    public void onDeviceActivity(DeviceId deviceId, long lastReportedActivity) {
        deviceLastReportedActivity.put(deviceId, lastReportedActivity);
        long lastSavedActivity = deviceLastSavedActivity.getOrDefault(deviceId, 0L);
        if (lastReportedActivity > 0 && lastReportedActivity > lastSavedActivity) {
            DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
            if (stateData != null) {
                save(deviceId, LAST_ACTIVITY_TIME, lastReportedActivity);
                deviceLastSavedActivity.put(deviceId, lastReportedActivity);
                DeviceState state = stateData.getState();
                state.setLastActivityTime(lastReportedActivity);
                if (!state.isActive()) {
                    state.setActive(true);
                    save(deviceId, ACTIVITY_STATE, state.isActive());
                    stateData.getMetaData().putValue("scope", SERVER_SCOPE);
                    pushRuleEngineMessage(stateData, ACTIVITY_EVENT);
                }
            }
        }
    }

    @Override
    public void onDeviceDisconnect(DeviceId deviceId) {
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (stateData != null) {
            long ts = System.currentTimeMillis();
            stateData.getState().setLastDisconnectTime(ts);
            pushRuleEngineMessage(stateData, DISCONNECT_EVENT);
            save(deviceId, LAST_DISCONNECT_TIME, ts);
        }
    }

    @Override
    public void onDeviceInactivityTimeoutUpdate(DeviceId deviceId, long inactivityTimeout) {
        if (inactivityTimeout == 0L) {
            return;
        }
        DeviceStateData stateData = deviceStates.get(deviceId);
        if (stateData != null) {
            long ts = System.currentTimeMillis();
            DeviceState state = stateData.getState();
            state.setInactivityTimeout(inactivityTimeout);
            boolean oldActive = state.isActive();
            state.setActive(ts < state.getLastActivityTime() + state.getInactivityTimeout());
            if (!oldActive && state.isActive() || oldActive && !state.isActive()) {
                save(deviceId, ACTIVITY_STATE, state.isActive());
            }
        }
    }

    @Override
    public void onQueueMsg(TransportProtos.DeviceStateServiceMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
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
                                    callback.onSuccess();
                                } else {
                                    log.warn("[{}][{}] Device belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                                            , tenantId, deviceId, tpi.getFullTopicName());
                                    callback.onFailure(new RuntimeException("Device belongs to external partition " + tpi.getFullTopicName() + "!"));
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.warn("Failed to register device to the state service", t);
                                callback.onFailure(t);
                            }
                        }, MoreExecutors.directExecutor());
                    } else if (proto.getUpdated()) {
                        DeviceStateData stateData = getOrFetchDeviceStateData(device.getId());
                        if (stateData != null) {
                            TbMsgMetaData md = new TbMsgMetaData();
                            md.putValue("deviceName", device.getName());
                            md.putValue("deviceType", device.getType());
                            stateData.setMetaData(md);
                        }
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

    volatile Set<TopicPartitionInfo> pendingPartitions;

    @Override
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            synchronized (this) {
                pendingPartitions = partitionChangeEvent.getPartitions();
                if (!clusterUpdatePending) {
                    clusterUpdatePending = true;
                    queueExecutor.submit(() -> {
                        clusterUpdatePending = false;
                        initStateFromDB();
                    });
                }
            }
        }
    }

    private void initStateFromDB() {
        try {
            log.info("CURRENT PARTITIONS: {}", partitionedDevices.keySet());
            log.info("NEW PARTITIONS: {}", pendingPartitions);

            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(pendingPartitions);
            addedPartitions.removeAll(partitionedDevices.keySet());

            log.info("ADDED PARTITIONS: {}", addedPartitions);

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(partitionedDevices.keySet());
            removedPartitions.removeAll(pendingPartitions);

            log.info("REMOVED PARTITIONS: {}", removedPartitions);

            // We no longer manage current partition of devices;
            removedPartitions.forEach(partition -> {
                Set<DeviceId> devices = partitionedDevices.remove(partition);
                devices.forEach(deviceId -> {
                    deviceStates.remove(deviceId);
                    deviceLastReportedActivity.remove(deviceId);
                    deviceLastSavedActivity.remove(deviceId);
                });
            });

            addedPartitions.forEach(tpi -> partitionedDevices.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()));

            //TODO 3.0: replace this dummy search with new functionality to search by partitions using SQL capabilities.
            // Adding only devices that are in new partitions
            List<Tenant> tenants = tenantService.findTenants(new TextPageLink(Integer.MAX_VALUE)).getData();
            for (Tenant tenant : tenants) {
                TextPageLink pageLink = new TextPageLink(initFetchPackSize);
                while (pageLink != null) {
                    List<ListenableFuture<Void>> fetchFutures = new ArrayList<>();
                    TextPageData<Device> page = deviceService.findDevicesByTenantId(tenant.getId(), pageLink);
                    pageLink = page.getNextPageLink();
                    for (Device device : page.getData()) {
                        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenant.getId(), device.getId());
                        if (addedPartitions.contains(tpi)) {
                            ListenableFuture<Void> future = Futures.transform(fetchDeviceState(device), new Function<DeviceStateData, Void>() {
                                @Nullable
                                @Override
                                public Void apply(@Nullable DeviceStateData state) {
                                    if (state != null) {
                                        addDeviceUsingState(tpi, state);
                                    }
                                    return null;
                                }
                            }, MoreExecutors.directExecutor());
                            fetchFutures.add(future);
                        }
                    }
                    try {
                        Futures.successfulAsList(fetchFutures).get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.warn("Failed to init device state service from DB", e);
                    }
                }
            }
            log.info("Managing following partitions:");
            partitionedDevices.forEach((tpi, devices) -> {
                log.info("[{}]: {} devices", tpi.getFullTopicName(), devices.size());
            });
        } catch (Throwable t) {
            log.warn("Failed to init device states from DB", t);
        }
    }

    private void addDeviceUsingState(TopicPartitionInfo tpi, DeviceStateData state) {
        partitionedDevices.computeIfAbsent(tpi, id -> ConcurrentHashMap.newKeySet()).add(state.getDeviceId());
        deviceStates.put(state.getDeviceId(), state);
    }

    private void updateState() {
        long ts = System.currentTimeMillis();
        Set<DeviceId> deviceIds = new HashSet<>(deviceStates.keySet());
        log.debug("Calculating state updates for {} devices", deviceStates.size());
        for (DeviceId deviceId : deviceIds) {
            DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
            if (stateData != null) {
                DeviceState state = stateData.getState();
                state.setActive(ts < state.getLastActivityTime() + state.getInactivityTimeout());
                if (!state.isActive() && (state.getLastInactivityAlarmTime() == 0L || state.getLastInactivityAlarmTime() < state.getLastActivityTime()) && UUIDs.unixTimestamp(deviceId.getId()) + state.getInactivityTimeout() < ts) {
                    state.setLastInactivityAlarmTime(ts);
                    pushRuleEngineMessage(stateData, INACTIVITY_EVENT);
                    save(deviceId, INACTIVITY_ALARM_TIME, ts);
                    save(deviceId, ACTIVITY_STATE, state.isActive());
                }
            } else {
                log.debug("[{}] Device that belongs to other server is detected and removed.", deviceId);
                deviceStates.remove(deviceId);
                deviceLastReportedActivity.remove(deviceId);
                deviceLastSavedActivity.remove(deviceId);
            }
        }
    }

    private DeviceStateData getOrFetchDeviceStateData(DeviceId deviceId) {
        DeviceStateData deviceStateData = deviceStates.get(deviceId);
        if (deviceStateData == null) {
            Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
            if (device != null) {
                try {
                    deviceStateData = fetchDeviceState(device).get();
                    deviceStates.putIfAbsent(deviceId, deviceStateData);
                } catch (InterruptedException | ExecutionException e) {
                    log.debug("[{}] Failed to fetch device state!", deviceId, e);
                }
            }
        }
        return deviceStateData;
    }

    private void sendDeviceEvent(TenantId tenantId, DeviceId deviceId, boolean added, boolean updated, boolean deleted) {
        TransportProtos.DeviceStateServiceMsgProto.Builder builder = TransportProtos.DeviceStateServiceMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setDeviceIdMSB(deviceId.getId().getMostSignificantBits());
        builder.setDeviceIdLSB(deviceId.getId().getLeastSignificantBits());
        builder.setAdded(added);
        builder.setUpdated(updated);
        builder.setDeleted(deleted);
        TransportProtos.DeviceStateServiceMsgProto msg = builder.build();
        clusterService.pushMsgToCore(tenantId, deviceId, TransportProtos.ToCoreMsg.newBuilder().setDeviceStateServiceMsg(msg).build(), null);
    }

    private void onDeviceDeleted(TenantId tenantId, DeviceId deviceId) {
        deviceStates.remove(deviceId);
        deviceLastReportedActivity.remove(deviceId);
        deviceLastSavedActivity.remove(deviceId);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        Set<DeviceId> deviceIdSet = partitionedDevices.get(tpi);
        deviceIdSet.remove(deviceId);
    }

    private ListenableFuture<DeviceStateData> fetchDeviceState(Device device) {
        if (persistToTelemetry) {
            ListenableFuture<List<TsKvEntry>> tsData = tsService.findLatest(TenantId.SYS_TENANT_ID, device.getId(), PERSISTENT_ATTRIBUTES);
            return Futures.transform(tsData, extractDeviceStateData(device), MoreExecutors.directExecutor());
        } else {
            ListenableFuture<List<AttributeKvEntry>> attrData = attributesService.find(TenantId.SYS_TENANT_ID, device.getId(), DataConstants.SERVER_SCOPE, PERSISTENT_ATTRIBUTES);
            return Futures.transform(attrData, extractDeviceStateData(device), MoreExecutors.directExecutor());
        }
    }

    private <T extends KvEntry> Function<List<T>, DeviceStateData> extractDeviceStateData(Device device) {
        return new Function<List<T>, DeviceStateData>() {
            @Nullable
            @Override
            public DeviceStateData apply(@Nullable List<T> data) {
                try {
                    long lastActivityTime = getEntryValue(data, LAST_ACTIVITY_TIME, 0L);
                    long inactivityAlarmTime = getEntryValue(data, INACTIVITY_ALARM_TIME, 0L);
                    long inactivityTimeout = getEntryValue(data, INACTIVITY_TIMEOUT, TimeUnit.SECONDS.toMillis(defaultInactivityTimeoutInSec));
                    boolean active = System.currentTimeMillis() < lastActivityTime + inactivityTimeout;
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
                    return DeviceStateData.builder()
                            .tenantId(device.getTenantId())
                            .deviceId(device.getId())
                            .metaData(md)
                            .state(deviceState).build();
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
            TbMsg tbMsg = TbMsg.newMsg(msgType, stateData.getDeviceId(), stateData.getMetaData().copy(), TbMsgDataType.JSON, data);
            clusterService.pushMsgToRuleEngine(stateData.getTenantId(), stateData.getDeviceId(), tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}] Failed to push inactivity alarm: {}", stateData.getDeviceId(), state, e);
        }
    }

    private void save(DeviceId deviceId, String key, long value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    TenantId.SYS_TENANT_ID, deviceId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry(key, value))),
                    new AttributeSaveCallback(deviceId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, deviceId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(deviceId, key, value));
        }
    }

    private void save(DeviceId deviceId, String key, boolean value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    TenantId.SYS_TENANT_ID, deviceId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))),
                    new AttributeSaveCallback(deviceId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, deviceId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(deviceId, key, value));
        }
    }

    private static class AttributeSaveCallback implements FutureCallback<Void> {
        private final DeviceId deviceId;
        private final String key;
        private final Object value;

        AttributeSaveCallback(DeviceId deviceId, String key, Object value) {
            this.deviceId = deviceId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@Nullable Void result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", deviceId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", deviceId, key, value, t);
        }
    }
}
