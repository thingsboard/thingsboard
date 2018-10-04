/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

/**
 * Created by ashvayka on 01.05.18.
 */
@Service
@Slf4j
//TODO: refactor to use page links as cursor and not fetch all
public class DefaultDeviceStateService implements DeviceStateService {

    private static final ObjectMapper json = new ObjectMapper();
    public static final String ACTIVITY_STATE = "active";
    public static final String LAST_CONNECT_TIME = "lastConnectTime";
    public static final String LAST_DISCONNECT_TIME = "lastDisconnectTime";
    public static final String LAST_ACTIVITY_TIME = "lastActivityTime";
    public static final String INACTIVITY_ALARM_TIME = "inactivityAlarmTime";
    public static final String INACTIVITY_TIMEOUT = "inactivityTimeout";

    public static final List<String> PERSISTENT_ATTRIBUTES = Arrays.asList(ACTIVITY_STATE, LAST_CONNECT_TIME, LAST_DISCONNECT_TIME, LAST_ACTIVITY_TIME, INACTIVITY_ALARM_TIME, INACTIVITY_TIMEOUT);

    @Autowired
    private TenantService tenantService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    @Lazy
    private ActorService actorService;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    private ClusterRoutingService routingService;

    @Autowired
    private ClusterRpcService clusterRpcService;

    @Value("${state.defaultInactivityTimeoutInSec}")
    @Getter
    private long defaultInactivityTimeoutInSec;

    @Value("${state.defaultStateCheckIntervalInSec}")
    @Getter
    private long defaultStateCheckIntervalInSec;

// TODO in v2.1
//    @Value("${state.defaultStatePersistenceIntervalInSec}")
//    @Getter
//    private long defaultStatePersistenceIntervalInSec;
//
//    @Value("${state.defaultStatePersistencePack}")
//    @Getter
//    private long defaultStatePersistencePack;

    private ListeningScheduledExecutorService queueExecutor;

    private ConcurrentMap<TenantId, Set<DeviceId>> tenantDevices = new ConcurrentHashMap<>();
    private ConcurrentMap<DeviceId, DeviceStateData> deviceStates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Should be always single threaded due to absence of locks.
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        queueExecutor.submit(this::initStateFromDB);
        queueExecutor.scheduleAtFixedRate(this::updateState, defaultStateCheckIntervalInSec, defaultStateCheckIntervalInSec, TimeUnit.SECONDS);
        //TODO: schedule persistence in v2.1;
    }

    @PreDestroy
    public void stop() {
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
    }

    @Override
    public void onDeviceAdded(Device device) {
        queueExecutor.submit(() -> onDeviceAddedSync(device));
    }

    @Override
    public void onDeviceUpdated(Device device) {
        queueExecutor.submit(() -> onDeviceUpdatedSync(device));
    }

    @Override
    public void onDeviceConnect(DeviceId deviceId) {
        queueExecutor.submit(() -> onDeviceConnectSync(deviceId));
    }

    @Override
    public void onDeviceActivity(DeviceId deviceId) {
        queueExecutor.submit(() -> onDeviceActivitySync(deviceId));
    }

    @Override
    public void onDeviceDisconnect(DeviceId deviceId) {
        queueExecutor.submit(() -> onDeviceDisconnectSync(deviceId));
    }

    @Override
    public void onDeviceDeleted(Device device) {
        queueExecutor.submit(() -> onDeviceDeleted(device.getTenantId(), device.getId()));
    }

    @Override
    public void onDeviceInactivityTimeoutUpdate(DeviceId deviceId, long inactivityTimeout) {
        queueExecutor.submit(() -> onInactivityTimeoutUpdate(deviceId, inactivityTimeout));
    }

    @Override
    public void onClusterUpdate() {
        queueExecutor.submit(this::onClusterUpdateSync);
    }

    @Override
    public void onRemoteMsg(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.DeviceStateServiceMsgProto proto;
        try {
            proto = ClusterAPIProtos.DeviceStateServiceMsgProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        TenantId tenantId = new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB()));
        if (proto.getDeleted()) {
            queueExecutor.submit(() -> onDeviceDeleted(tenantId, deviceId));
        } else {
            Device device = deviceService.findDeviceById(deviceId);
            if (device != null) {
                if (proto.getAdded()) {
                    onDeviceAdded(device);
                } else if (proto.getUpdated()) {
                    onDeviceUpdated(device);
                }
            }
        }
    }

    private void onClusterUpdateSync() {
        List<Tenant> tenants = tenantService.findTenants(new TextPageLink(Integer.MAX_VALUE)).getData();
        for (Tenant tenant : tenants) {
            List<ListenableFuture<DeviceStateData>> fetchFutures = new ArrayList<>();
            List<Device> devices = deviceService.findDevicesByTenantId(tenant.getId(), new TextPageLink(Integer.MAX_VALUE)).getData();
            for (Device device : devices) {
                if (!routingService.resolveById(device.getId()).isPresent()) {
                    if (!deviceStates.containsKey(device.getId())) {
                        fetchFutures.add(fetchDeviceState(device));
                    }
                } else {
                    Set<DeviceId> tenantDeviceSet = tenantDevices.get(tenant.getId());
                    if (tenantDeviceSet != null) {
                        tenantDeviceSet.remove(device.getId());
                    }
                    deviceStates.remove(device.getId());
                }
            }
            try {
                Futures.successfulAsList(fetchFutures).get().forEach(this::addDeviceUsingState);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to init device state service from DB", e);
            }
        }
    }

    private void initStateFromDB() {
        List<Tenant> tenants = tenantService.findTenants(new TextPageLink(Integer.MAX_VALUE)).getData();
        for (Tenant tenant : tenants) {
            List<ListenableFuture<DeviceStateData>> fetchFutures = new ArrayList<>();
            List<Device> devices = deviceService.findDevicesByTenantId(tenant.getId(), new TextPageLink(Integer.MAX_VALUE)).getData();
            for (Device device : devices) {
                if (!routingService.resolveById(device.getId()).isPresent()) {
                    fetchFutures.add(fetchDeviceState(device));
                }
            }
            try {
                Futures.successfulAsList(fetchFutures).get().forEach(this::addDeviceUsingState);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to init device state service from DB", e);
            }
        }
    }

    private void addDeviceUsingState(DeviceStateData state) {
        tenantDevices.computeIfAbsent(state.getTenantId(), id -> ConcurrentHashMap.newKeySet()).add(state.getDeviceId());
        deviceStates.put(state.getDeviceId(), state);
    }

    private void updateState() {
        long ts = System.currentTimeMillis();
        Set<DeviceId> deviceIds = new HashSet<>(deviceStates.keySet());
        for (DeviceId deviceId : deviceIds) {
            DeviceStateData stateData = deviceStates.get(deviceId);
            DeviceState state = stateData.getState();
            state.setActive(ts < state.getLastActivityTime() + state.getInactivityTimeout());
            if (!state.isActive() && (state.getLastInactivityAlarmTime() == 0L || state.getLastInactivityAlarmTime() < state.getLastActivityTime())) {
                state.setLastInactivityAlarmTime(ts);
                pushRuleEngineMessage(stateData, INACTIVITY_EVENT);
                saveAttribute(deviceId, INACTIVITY_ALARM_TIME, ts);
                saveAttribute(deviceId, ACTIVITY_STATE, state.isActive());
            }
        }
    }

    private void onDeviceConnectSync(DeviceId deviceId) {
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (stateData != null) {
            long ts = System.currentTimeMillis();
            stateData.getState().setLastConnectTime(ts);
            pushRuleEngineMessage(stateData, CONNECT_EVENT);
            saveAttribute(deviceId, LAST_CONNECT_TIME, ts);
        }
    }

    private void onDeviceDisconnectSync(DeviceId deviceId) {
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (stateData != null) {
            long ts = System.currentTimeMillis();
            stateData.getState().setLastDisconnectTime(ts);
            pushRuleEngineMessage(stateData, DISCONNECT_EVENT);
            saveAttribute(deviceId, LAST_DISCONNECT_TIME, ts);
        }
    }

    private void onDeviceActivitySync(DeviceId deviceId) {
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (stateData != null) {
            DeviceState state = stateData.getState();
            long ts = System.currentTimeMillis();
            state.setActive(true);
            stateData.getState().setLastActivityTime(ts);
            pushRuleEngineMessage(stateData, ACTIVITY_EVENT);
            saveAttribute(deviceId, LAST_ACTIVITY_TIME, ts);
            saveAttribute(deviceId, ACTIVITY_STATE, state.isActive());
        }
    }

    private DeviceStateData getOrFetchDeviceStateData(DeviceId deviceId) {
        DeviceStateData deviceStateData = deviceStates.get(deviceId);
        if (deviceStateData == null) {
            if (!routingService.resolveById(deviceId).isPresent()) {
                Device device = deviceService.findDeviceById(deviceId);
                if (device != null) {
                    try {
                        deviceStateData = fetchDeviceState(device).get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.debug("[{}] Failed to fetch device state!", deviceId, e);
                    }
                }
            }
        }
        return deviceStateData;
    }

    private void onInactivityTimeoutUpdate(DeviceId deviceId, long inactivityTimeout) {
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
                saveAttribute(deviceId, ACTIVITY_STATE, state.isActive());
            }
        }
    }

    private void onDeviceAddedSync(Device device) {
        Optional<ServerAddress> address = routingService.resolveById(device.getId());
        if (!address.isPresent()) {
            Futures.addCallback(fetchDeviceState(device), new FutureCallback<DeviceStateData>() {
                @Override
                public void onSuccess(@Nullable DeviceStateData state) {
                    addDeviceUsingState(state);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("Failed to register device to the state service", t);
                }
            });
        } else {
            sendDeviceEvent(device.getTenantId(), device.getId(), address.get(), true, false, false);
        }
    }

    private void sendDeviceEvent(TenantId tenantId, DeviceId deviceId, ServerAddress address, boolean added, boolean updated, boolean deleted) {
        log.trace("[{}][{}] Device is monitored on other server: {}", tenantId, deviceId, address);
        ClusterAPIProtos.DeviceStateServiceMsgProto.Builder builder = ClusterAPIProtos.DeviceStateServiceMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setDeviceIdMSB(deviceId.getId().getMostSignificantBits());
        builder.setDeviceIdLSB(deviceId.getId().getLeastSignificantBits());
        builder.setAdded(added);
        builder.setUpdated(updated);
        builder.setDeleted(deleted);
        clusterRpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_DEVICE_STATE_SERVICE_MESSAGE, builder.build().toByteArray());
    }

    private void onDeviceUpdatedSync(Device device) {
        Optional<ServerAddress> address = routingService.resolveById(device.getId());
        if (!address.isPresent()) {
            DeviceStateData stateData = getOrFetchDeviceStateData(device.getId());
            if (stateData != null) {
                TbMsgMetaData md = new TbMsgMetaData();
                md.putValue("deviceName", device.getName());
                md.putValue("deviceType", device.getType());
                stateData.setMetaData(md);
            }
        } else {
            sendDeviceEvent(device.getTenantId(), device.getId(), address.get(), false, true, false);
        }
    }

    private void onDeviceDeleted(TenantId tenantId, DeviceId deviceId) {
        Optional<ServerAddress> address = routingService.resolveById(deviceId);
        if (!address.isPresent()) {
            deviceStates.remove(deviceId);
            Set<DeviceId> deviceIds = tenantDevices.get(tenantId);
            if (deviceIds != null) {
                deviceIds.remove(deviceId);
                if (deviceIds.isEmpty()) {
                    tenantDevices.remove(tenantId);
                }
            }
        } else {
            sendDeviceEvent(tenantId, deviceId, address.get(), false, false, true);
        }
    }

    private ListenableFuture<DeviceStateData> fetchDeviceState(Device device) {
        ListenableFuture<List<AttributeKvEntry>> attributes = attributesService.find(device.getId(), DataConstants.SERVER_SCOPE, PERSISTENT_ATTRIBUTES);
        return Futures.transform(attributes, new Function<List<AttributeKvEntry>, DeviceStateData>() {
            @Nullable
            @Override
            public DeviceStateData apply(@Nullable List<AttributeKvEntry> attributes) {
                long lastActivityTime = getAttributeValue(attributes, LAST_ACTIVITY_TIME, 0L);
                long inactivityAlarmTime = getAttributeValue(attributes, INACTIVITY_ALARM_TIME, 0L);
                long inactivityTimeout = getAttributeValue(attributes, INACTIVITY_TIMEOUT, TimeUnit.SECONDS.toMillis(defaultInactivityTimeoutInSec));
                boolean active = System.currentTimeMillis() < lastActivityTime + inactivityTimeout;
                DeviceState deviceState = DeviceState.builder()
                        .active(active)
                        .lastConnectTime(getAttributeValue(attributes, LAST_CONNECT_TIME, 0L))
                        .lastDisconnectTime(getAttributeValue(attributes, LAST_DISCONNECT_TIME, 0L))
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
            }
        });
    }

    private long getAttributeValue(List<AttributeKvEntry> attributes, String attributeName, long defaultValue) {
        for (AttributeKvEntry attribute : attributes) {
            if (attribute.getKey().equals(attributeName)) {
                return attribute.getLongValue().orElse(defaultValue);
            }
        }
        return defaultValue;
    }

    private void pushRuleEngineMessage(DeviceStateData stateData, String msgType) {
        DeviceState state = stateData.getState();
        try {
            TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), msgType, stateData.getDeviceId(), stateData.getMetaData().copy(), TbMsgDataType.JSON
                    , json.writeValueAsString(state)
                    , null, null, 0L);
            actorService.onMsg(new ServiceToRuleEngineMsg(stateData.getTenantId(), tbMsg));
        } catch (Exception e) {
            log.warn("[{}] Failed to push inactivity alarm: {}", stateData.getDeviceId(), state, e);
        }
    }

    private void saveAttribute(DeviceId deviceId, String key, long value) {
        tsSubService.saveAttrAndNotify(deviceId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(deviceId, key, value));
    }

    private void saveAttribute(DeviceId deviceId, String key, boolean value) {
        tsSubService.saveAttrAndNotify(deviceId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(deviceId, key, value));
    }

    private class AttributeSaveCallback implements FutureCallback<Void> {
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
