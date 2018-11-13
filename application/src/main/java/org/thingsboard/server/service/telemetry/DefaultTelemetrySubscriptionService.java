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
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.sub.Subscription;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;
import org.thingsboard.server.service.telemetry.sub.SubscriptionState;
import org.thingsboard.server.service.telemetry.sub.SubscriptionUpdate;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
public class DefaultTelemetrySubscriptionService implements TelemetrySubscriptionService {

    @Autowired
    private TelemetryWebSocketService wsService;

    @Autowired
    private AttributesService attrService;

    @Autowired
    private TimeseriesService tsService;

    @Autowired
    private ClusterRoutingService routingService;

    @Autowired
    private ClusterRpcService rpcService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    @Lazy
    private DeviceStateService stateService;

    @Autowired
    @Lazy
    private ActorService actorService;

    private ExecutorService tsCallBackExecutor;
    private ExecutorService wsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor();
        wsCallBackExecutor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
    }

    private final Map<EntityId, Set<Subscription>> subscriptionsByEntityId = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Subscription>> subscriptionsByWsSessionId = new ConcurrentHashMap<>();

    @Override
    public void addLocalWsSubscription(String sessionId, EntityId entityId, SubscriptionState sub) {
        long startTime = 0L;
        long endTime = 0L;
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW) && TelemetryFeature.TIMESERIES.equals(sub.getType())) {
            EntityView entityView = entityViewService.findEntityViewById(new EntityViewId(entityId.getId()));
            entityId = entityView.getEntityId();
            startTime = entityView.getStartTimeMs();
            endTime = entityView.getEndTimeMs();
            sub = getUpdatedSubscriptionState(entityId, sub, entityView);
        }
        Optional<ServerAddress> server = routingService.resolveById(entityId);
        Subscription subscription;
        if (server.isPresent()) {
            ServerAddress address = server.get();
            log.trace("[{}] Forwarding subscription [{}] for [{}] entity [{}] to [{}]", sessionId, sub.getSubscriptionId(), entityId.getEntityType().name(), entityId, address);
            subscription = new Subscription(sub, true, address, startTime, endTime);
            tellNewSubscription(address, sessionId, subscription);
        } else {
            log.trace("[{}] Registering local subscription [{}] for [{}] entity [{}]", sessionId, sub.getSubscriptionId(), entityId.getEntityType().name(), entityId);
            subscription = new Subscription(sub, true, null, startTime, endTime);
        }
        registerSubscription(sessionId, entityId, subscription);
    }

    private SubscriptionState getUpdatedSubscriptionState(EntityId entityId, SubscriptionState sub, EntityView entityView) {
        Map<String, Long> keyStates;
        if(sub.isAllKeys()) {
            keyStates = entityView.getKeys().getTimeseries().stream().collect(Collectors.toMap(k -> k, k -> 0L));
        } else {
            keyStates = sub.getKeyStates().entrySet()
                    .stream().filter(entry -> entityView.getKeys().getTimeseries().contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return new SubscriptionState(sub.getWsSessionId(), sub.getSubscriptionId(), entityId, sub.getType(), false, keyStates, sub.getScope());
    }

    @Override
    public void cleanupLocalWsSessionSubscriptions(TelemetryWebSocketSessionRef sessionRef, String sessionId) {
        cleanupLocalWsSessionSubscriptions(sessionId);
    }

    @Override
    public void removeSubscription(String sessionId, int subscriptionId) {
        log.debug("[{}][{}] Going to remove subscription.", sessionId, subscriptionId);
        Map<Integer, Subscription> sessionSubscriptions = subscriptionsByWsSessionId.get(sessionId);
        if (sessionSubscriptions != null) {
            Subscription subscription = sessionSubscriptions.remove(subscriptionId);
            if (subscription != null) {
                processSubscriptionRemoval(sessionId, sessionSubscriptions, subscription);
            } else {
                log.debug("[{}][{}] Subscription not found!", sessionId, subscriptionId);
            }
        } else {
            log.debug("[{}] No session subscriptions found!", sessionId);
        }
    }

    @Override
    public void saveAndNotify(EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback) {
        saveAndNotify(entityId, ts, 0L, callback);
    }

    @Override
    public void saveAndNotify(EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback) {
        ListenableFuture<List<Void>> saveFuture = tsService.save(entityId, ts, ttl);
        addMainCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onTimeseriesUpdate(entityId, ts));
    }

    @Override
    public void saveAndNotify(EntityId entityId, String scope, List<AttributeKvEntry> attributes, FutureCallback<Void> callback) {
        ListenableFuture<List<Void>> saveFuture = attrService.save(entityId, scope, attributes);
        addMainCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onAttributesUpdate(entityId, scope, attributes));
    }

    @Override
    public void saveAttrAndNotify(EntityId entityId, String scope, String key, long value, FutureCallback<Void> callback) {
        saveAndNotify(entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(EntityId entityId, String scope, String key, String value, FutureCallback<Void> callback) {
        saveAndNotify(entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(EntityId entityId, String scope, String key, double value, FutureCallback<Void> callback) {
        saveAndNotify(entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new DoubleDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(EntityId entityId, String scope, String key, boolean value, FutureCallback<Void> callback) {
        saveAndNotify(entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void onSharedAttributesUpdate(TenantId tenantId, DeviceId deviceId, Set<AttributeKvEntry> attributes) {
        DeviceAttributesEventNotificationMsg notificationMsg = DeviceAttributesEventNotificationMsg.onUpdate(tenantId,
                deviceId, DataConstants.SHARED_SCOPE, new ArrayList<>(attributes));
        actorService.onMsg(new SendToClusterMsg(deviceId, notificationMsg));
    }

    @Override
    public void onNewRemoteSubscription(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.SubscriptionProto proto;
        try {
            proto = ClusterAPIProtos.SubscriptionProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        Map<String, Long> statesMap = proto.getKeyStatesList().stream().collect(
                Collectors.toMap(ClusterAPIProtos.SubscriptionKetStateProto::getKey, ClusterAPIProtos.SubscriptionKetStateProto::getTs));
        Subscription subscription = new Subscription(
                new SubscriptionState(proto.getSessionId(), proto.getSubscriptionId(),
                        EntityIdFactory.getByTypeAndId(proto.getEntityType(), proto.getEntityId()),
                        TelemetryFeature.valueOf(proto.getType()), proto.getAllKeys(), statesMap, proto.getScope()),
                false, new ServerAddress(serverAddress.getHost(), serverAddress.getPort()));

        addRemoteWsSubscription(serverAddress, proto.getSessionId(), subscription);
    }

    @Override
    public void onRemoteSubscriptionUpdate(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.SubscriptionUpdateProto proto;
        try {
            proto = ClusterAPIProtos.SubscriptionUpdateProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        SubscriptionUpdate update = convert(proto);
        String sessionId = proto.getSessionId();
        log.trace("[{}] Processing remote subscription onUpdate [{}]", sessionId, update);
        Optional<Subscription> subOpt = getSubscription(sessionId, update.getSubscriptionId());
        if (subOpt.isPresent()) {
            updateSubscriptionState(sessionId, subOpt.get(), update);
            wsService.sendWsMsg(sessionId, update);
        }
    }

    @Override
    public void onRemoteSubscriptionClose(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.SubscriptionCloseProto proto;
        try {
            proto = ClusterAPIProtos.SubscriptionCloseProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        removeSubscription(proto.getSessionId(), proto.getSubscriptionId());
    }

    @Override
    public void onRemoteSessionClose(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.SessionCloseProto proto;
        try {
            proto = ClusterAPIProtos.SessionCloseProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        cleanupRemoteWsSessionSubscriptions(proto.getSessionId());
    }

    @Override
    public void onRemoteAttributesUpdate(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.AttributeUpdateProto proto;
        try {
            proto = ClusterAPIProtos.AttributeUpdateProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        onAttributesUpdate(EntityIdFactory.getByTypeAndId(proto.getEntityType(), proto.getEntityId()), proto.getScope(),
                proto.getDataList().stream().map(this::toAttribute).collect(Collectors.toList()));
    }

    @Override
    public void onRemoteTsUpdate(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.TimeseriesUpdateProto proto;
        try {
            proto = ClusterAPIProtos.TimeseriesUpdateProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        onTimeseriesUpdate(EntityIdFactory.getByTypeAndId(proto.getEntityType(), proto.getEntityId()),
                proto.getDataList().stream().map(this::toTimeseries).collect(Collectors.toList()));
    }

    @Override
    public void onClusterUpdate() {
        log.trace("Processing cluster onUpdate msg!");
        Iterator<Map.Entry<EntityId, Set<Subscription>>> deviceIterator = subscriptionsByEntityId.entrySet().iterator();
        while (deviceIterator.hasNext()) {
            Map.Entry<EntityId, Set<Subscription>> e = deviceIterator.next();
            Set<Subscription> subscriptions = e.getValue();
            Optional<ServerAddress> newAddressOptional = routingService.resolveById(e.getKey());
            if (newAddressOptional.isPresent()) {
                newAddressOptional.ifPresent(serverAddress -> checkSubsciptionsNewAddress(serverAddress, subscriptions));
            } else {
                checkSubsciptionsPrevAddress(subscriptions);
            }
            if (subscriptions.size() == 0) {
                log.trace("[{}] No more subscriptions for this device on current server.", e.getKey());
                deviceIterator.remove();
            }
        }
    }

    private void checkSubsciptionsNewAddress(ServerAddress newAddress, Set<Subscription> subscriptions) {
        Iterator<Subscription> subscriptionIterator = subscriptions.iterator();
        while (subscriptionIterator.hasNext()) {
            Subscription s = subscriptionIterator.next();
            if (s.isLocal()) {
                if (!newAddress.equals(s.getServer())) {
                    log.trace("[{}] Local subscription is now handled on new server [{}]", s.getWsSessionId(), newAddress);
                    s.setServer(newAddress);
                    tellNewSubscription(newAddress, s.getWsSessionId(), s);
                }
            } else {
                log.trace("[{}] Remote subscription is now handled on new server address: [{}]", s.getWsSessionId(), newAddress);
                subscriptionIterator.remove();
                //TODO: onUpdate state of subscription by WsSessionId and other maps.
            }
        }
    }

    private void checkSubsciptionsPrevAddress(Set<Subscription> subscriptions) {
        for (Subscription s : subscriptions) {
            if (s.isLocal() && s.getServer() != null) {
                log.trace("[{}] Local subscription is no longer handled on remote server address [{}]", s.getWsSessionId(), s.getServer());
                s.setServer(null);
            } else {
                log.trace("[{}] Remote subscription is on up to date server address.", s.getWsSessionId());
            }
        }
    }

    private void addRemoteWsSubscription(ServerAddress address, String sessionId, Subscription subscription) {
        EntityId entityId = subscription.getEntityId();
        log.trace("[{}] Registering remote subscription [{}] for device [{}] to [{}]", sessionId, subscription.getSubscriptionId(), entityId, address);
        registerSubscription(sessionId, entityId, subscription);
        if (subscription.getType() == TelemetryFeature.ATTRIBUTES) {
            final Map<String, Long> keyStates = subscription.getKeyStates();
            DonAsynchron.withCallback(attrService.find(entityId, DataConstants.CLIENT_SCOPE, keyStates.keySet()), values -> {
                        List<TsKvEntry> missedUpdates = new ArrayList<>();
                        values.forEach(latestEntry -> {
                            if (latestEntry.getLastUpdateTs() > keyStates.get(latestEntry.getKey())) {
                                missedUpdates.add(new BasicTsKvEntry(latestEntry.getLastUpdateTs(), latestEntry));
                            }
                        });
                        if (!missedUpdates.isEmpty()) {
                            tellRemoteSubUpdate(address, sessionId, new SubscriptionUpdate(subscription.getSubscriptionId(), missedUpdates));
                        }
                    },
                    e -> log.error("Failed to fetch missed updates.", e), tsCallBackExecutor);
        } else if (subscription.getType() == TelemetryFeature.TIMESERIES) {
            long curTs = System.currentTimeMillis();
            List<ReadTsKvQuery> queries = new ArrayList<>();
            subscription.getKeyStates().entrySet().forEach(e -> {
                queries.add(new BaseReadTsKvQuery(e.getKey(), e.getValue() + 1L, curTs));
            });

            DonAsynchron.withCallback(tsService.findAll(entityId, queries),
                    missedUpdates -> {
                        if (missedUpdates != null && !missedUpdates.isEmpty()) {
                            tellRemoteSubUpdate(address, sessionId, new SubscriptionUpdate(subscription.getSubscriptionId(), missedUpdates));
                        }
                    },
                    e -> log.error("Failed to fetch missed updates.", e),
                    tsCallBackExecutor);
        }
    }

    private void onAttributesUpdate(EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        Optional<ServerAddress> serverAddress = routingService.resolveById(entityId);
        if (!serverAddress.isPresent()) {
            onLocalAttributesUpdate(entityId, scope, attributes);
            if (entityId.getEntityType() == EntityType.DEVICE && DataConstants.SERVER_SCOPE.equalsIgnoreCase(scope)) {
                for (AttributeKvEntry attribute : attributes) {
                    if (attribute.getKey().equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT)) {
                        stateService.onDeviceInactivityTimeoutUpdate(new DeviceId(entityId.getId()), attribute.getLongValue().orElse(0L));
                    }
                }
            }
        } else {
            tellRemoteAttributesUpdate(serverAddress.get(), entityId, scope, attributes);
        }
    }

    private void onTimeseriesUpdate(EntityId entityId, List<TsKvEntry> ts) {
        Optional<ServerAddress> serverAddress = routingService.resolveById(entityId);
        if (!serverAddress.isPresent()) {
            onLocalTimeseriesUpdate(entityId, ts);
        } else {
            tellRemoteTimeseriesUpdate(serverAddress.get(), entityId, ts);
        }
    }

    private void onLocalAttributesUpdate(EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        onLocalSubUpdate(entityId, s -> TelemetryFeature.ATTRIBUTES == s.getType() && (StringUtils.isEmpty(s.getScope()) || scope.equals(s.getScope())), s -> {
            List<TsKvEntry> subscriptionUpdate = null;
            for (AttributeKvEntry kv : attributes) {
                if (s.isAllKeys() || s.getKeyStates().containsKey(kv.getKey())) {
                    if (subscriptionUpdate == null) {
                        subscriptionUpdate = new ArrayList<>();
                    }
                    subscriptionUpdate.add(new BasicTsKvEntry(kv.getLastUpdateTs(), kv));
                }
            }
            return subscriptionUpdate;
        });
    }

    private void onLocalTimeseriesUpdate(EntityId entityId, List<TsKvEntry> ts) {
        onLocalSubUpdate(entityId, s -> TelemetryFeature.TIMESERIES == s.getType(), s -> {
            List<TsKvEntry> subscriptionUpdate = null;
            for (TsKvEntry kv : ts) {
                if (isInTimeRange(s, kv.getTs()) && (s.isAllKeys() || s.getKeyStates().containsKey((kv.getKey())))) {
                    if (subscriptionUpdate == null) {
                        subscriptionUpdate = new ArrayList<>();
                    }
                    subscriptionUpdate.add(kv);
                }
            }
            return subscriptionUpdate;
        });
    }

    private boolean isInTimeRange(Subscription subscription, long kvTime) {
        return (subscription.getStartTime() == 0 || subscription.getStartTime() <= kvTime)
                && (subscription.getEndTime() == 0 || subscription.getEndTime() >= kvTime);
    }

    private void onLocalSubUpdate(EntityId entityId, Predicate<Subscription> filter, Function<Subscription, List<TsKvEntry>> f) {
        Set<Subscription> deviceSubscriptions = subscriptionsByEntityId.get(entityId);
        if (deviceSubscriptions != null) {
            deviceSubscriptions.stream().filter(filter).forEach(s -> {
                String sessionId = s.getWsSessionId();
                List<TsKvEntry> subscriptionUpdate = f.apply(s);
                if (subscriptionUpdate != null && !subscriptionUpdate.isEmpty()) {
                    SubscriptionUpdate update = new SubscriptionUpdate(s.getSubscriptionId(), subscriptionUpdate);
                    if (s.isLocal()) {
                        updateSubscriptionState(sessionId, s, update);
                        wsService.sendWsMsg(sessionId, update);
                    } else {
                        tellRemoteSubUpdate(s.getServer(), sessionId, update);
                    }
                }
            });
        } else {
            log.debug("[{}] No device subscriptions to process!", entityId);
        }
    }

    private void updateSubscriptionState(String sessionId, Subscription subState, SubscriptionUpdate update) {
        log.trace("[{}] updating subscription state {} using onUpdate {}", sessionId, subState, update);
        update.getLatestValues().entrySet().forEach(e -> subState.setKeyState(e.getKey(), e.getValue()));
    }

    private void registerSubscription(String sessionId, EntityId entityId, Subscription subscription) {
        Set<Subscription> deviceSubscriptions = subscriptionsByEntityId.computeIfAbsent(entityId, k -> ConcurrentHashMap.newKeySet());
        deviceSubscriptions.add(subscription);
        Map<Integer, Subscription> sessionSubscriptions = subscriptionsByWsSessionId.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
        sessionSubscriptions.put(subscription.getSubscriptionId(), subscription);
    }

    private void cleanupLocalWsSessionSubscriptions(String sessionId) {
        cleanupWsSessionSubscriptions(sessionId, true);
    }

    private void cleanupRemoteWsSessionSubscriptions(String sessionId) {
        cleanupWsSessionSubscriptions(sessionId, false);
    }

    private void cleanupWsSessionSubscriptions(String sessionId, boolean localSession) {
        log.debug("[{}] Removing all subscriptions for particular session.", sessionId);
        Map<Integer, Subscription> sessionSubscriptions = subscriptionsByWsSessionId.get(sessionId);
        if (sessionSubscriptions != null) {
            int sessionSubscriptionSize = sessionSubscriptions.size();

            for (Subscription subscription : sessionSubscriptions.values()) {
                EntityId entityId = subscription.getEntityId();
                Set<Subscription> deviceSubscriptions = subscriptionsByEntityId.get(entityId);
                deviceSubscriptions.remove(subscription);
                if (deviceSubscriptions.isEmpty()) {
                    subscriptionsByEntityId.remove(entityId);
                }
            }
            subscriptionsByWsSessionId.remove(sessionId);
            log.debug("[{}] Removed {} subscriptions for particular session.", sessionId, sessionSubscriptionSize);

            if (localSession) {
                notifyWsSubscriptionClosed(sessionId, sessionSubscriptions);
            }
        } else {
            log.debug("[{}] No subscriptions found!", sessionId);
        }
    }

    private void notifyWsSubscriptionClosed(String sessionId, Map<Integer, Subscription> sessionSubscriptions) {
        Set<ServerAddress> affectedServers = new HashSet<>();
        for (Subscription subscription : sessionSubscriptions.values()) {
            if (subscription.getServer() != null) {
                affectedServers.add(subscription.getServer());
            }
        }
        for (ServerAddress address : affectedServers) {
            log.debug("[{}] Going to onSubscriptionUpdate [{}] server about session close event", sessionId, address);
            tellRemoteSessionClose(address, sessionId);
        }
    }

    private void processSubscriptionRemoval(String sessionId, Map<Integer, Subscription> sessionSubscriptions, Subscription subscription) {
        EntityId entityId = subscription.getEntityId();
        if (subscription.isLocal() && subscription.getServer() != null) {
            tellRemoteSubClose(subscription.getServer(), sessionId, subscription.getSubscriptionId());
        }
        if (sessionSubscriptions.isEmpty()) {
            log.debug("[{}] Removed last subscription for particular session.", sessionId);
            subscriptionsByWsSessionId.remove(sessionId);
        } else {
            log.debug("[{}] Removed session subscription.", sessionId);
        }
        Set<Subscription> deviceSubscriptions = subscriptionsByEntityId.get(entityId);
        if (deviceSubscriptions != null) {
            boolean result = deviceSubscriptions.remove(subscription);
            if (result) {
                if (deviceSubscriptions.size() == 0) {
                    log.debug("[{}] Removed last subscription for particular device.", sessionId);
                    subscriptionsByEntityId.remove(entityId);
                } else {
                    log.debug("[{}] Removed device subscription.", sessionId);
                }
            } else {
                log.debug("[{}] Subscription not found!", sessionId);
            }
        } else {
            log.debug("[{}] No device subscriptions found!", sessionId);
        }
    }

    private void addMainCallback(ListenableFuture<List<Void>> saveFuture, final FutureCallback<Void> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    private void addWsCallback(ListenableFuture<List<Void>> saveFuture, Consumer<Void> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                callback.accept(null);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, wsCallBackExecutor);
    }

    private void tellNewSubscription(ServerAddress address, String sessionId, Subscription sub) {
        ClusterAPIProtos.SubscriptionProto.Builder builder = ClusterAPIProtos.SubscriptionProto.newBuilder();
        builder.setSessionId(sessionId);
        builder.setSubscriptionId(sub.getSubscriptionId());
        builder.setEntityType(sub.getEntityId().getEntityType().name());
        builder.setEntityId(sub.getEntityId().getId().toString());
        builder.setType(sub.getType().name());
        builder.setAllKeys(sub.isAllKeys());
        builder.setScope(sub.getScope());
        sub.getKeyStates().entrySet().forEach(e -> builder.addKeyStates(
                ClusterAPIProtos.SubscriptionKetStateProto.newBuilder().setKey(e.getKey()).setTs(e.getValue()).build()));
        rpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_TELEMETRY_SUBSCRIPTION_CREATE_MESSAGE, builder.build().toByteArray());
    }

    private void tellRemoteSubUpdate(ServerAddress address, String sessionId, SubscriptionUpdate update) {
        ClusterAPIProtos.SubscriptionUpdateProto.Builder builder = ClusterAPIProtos.SubscriptionUpdateProto.newBuilder();
        builder.setSessionId(sessionId);
        builder.setSubscriptionId(update.getSubscriptionId());
        builder.setErrorCode(update.getErrorCode());
        if (update.getErrorMsg() != null) {
            builder.setErrorMsg(update.getErrorMsg());
        }
        update.getData().entrySet().forEach(
                e -> {
                    ClusterAPIProtos.SubscriptionUpdateValueListProto.Builder dataBuilder = ClusterAPIProtos.SubscriptionUpdateValueListProto.newBuilder();

                    dataBuilder.setKey(e.getKey());
                    e.getValue().forEach(v -> {
                        Object[] array = (Object[]) v;
                        dataBuilder.addTs((long) array[0]);
                        dataBuilder.addValue((String) array[1]);
                    });

                    builder.addData(dataBuilder.build());
                }
        );
        rpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_TELEMETRY_SUBSCRIPTION_UPDATE_MESSAGE, builder.build().toByteArray());
    }

    private void tellRemoteAttributesUpdate(ServerAddress address, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        ClusterAPIProtos.AttributeUpdateProto.Builder builder = ClusterAPIProtos.AttributeUpdateProto.newBuilder();
        builder.setEntityId(entityId.getId().toString());
        builder.setEntityType(entityId.getEntityType().name());
        builder.setScope(scope);
        attributes.forEach(v -> builder.addData(toKeyValueProto(v.getLastUpdateTs(), v).build()));
        rpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_TELEMETRY_ATTR_UPDATE_MESSAGE, builder.build().toByteArray());
    }

    private void tellRemoteTimeseriesUpdate(ServerAddress address, EntityId entityId, List<TsKvEntry> ts) {
        ClusterAPIProtos.TimeseriesUpdateProto.Builder builder = ClusterAPIProtos.TimeseriesUpdateProto.newBuilder();
        builder.setEntityId(entityId.getId().toString());
        builder.setEntityType(entityId.getEntityType().name());
        ts.forEach(v -> builder.addData(toKeyValueProto(v.getTs(), v).build()));
        rpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_TELEMETRY_TS_UPDATE_MESSAGE, builder.build().toByteArray());
    }

    private void tellRemoteSessionClose(ServerAddress address, String sessionId) {
        ClusterAPIProtos.SessionCloseProto proto = ClusterAPIProtos.SessionCloseProto.newBuilder().setSessionId(sessionId).build();
        rpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_TELEMETRY_SESSION_CLOSE_MESSAGE, proto.toByteArray());
    }

    private void tellRemoteSubClose(ServerAddress address, String sessionId, int subscriptionId) {
        ClusterAPIProtos.SubscriptionCloseProto proto = ClusterAPIProtos.SubscriptionCloseProto.newBuilder().setSessionId(sessionId).setSubscriptionId(subscriptionId).build();
        rpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_TELEMETRY_SUBSCRIPTION_CLOSE_MESSAGE, proto.toByteArray());
    }

    private ClusterAPIProtos.KeyValueProto.Builder toKeyValueProto(long ts, KvEntry attr) {
        ClusterAPIProtos.KeyValueProto.Builder dataBuilder = ClusterAPIProtos.KeyValueProto.newBuilder();
        dataBuilder.setKey(attr.getKey());
        dataBuilder.setTs(ts);
        dataBuilder.setValueType(attr.getDataType().ordinal());
        switch (attr.getDataType()) {
            case BOOLEAN:
                Optional<Boolean> booleanValue = attr.getBooleanValue();
                booleanValue.ifPresent(dataBuilder::setBoolValue);
                break;
            case LONG:
                Optional<Long> longValue = attr.getLongValue();
                longValue.ifPresent(dataBuilder::setLongValue);
                break;
            case DOUBLE:
                Optional<Double> doubleValue = attr.getDoubleValue();
                doubleValue.ifPresent(dataBuilder::setDoubleValue);
                break;
            case STRING:
                Optional<String> stringValue = attr.getStrValue();
                stringValue.ifPresent(dataBuilder::setStrValue);
                break;
        }
        return dataBuilder;
    }

    private AttributeKvEntry toAttribute(ClusterAPIProtos.KeyValueProto proto) {
        return new BaseAttributeKvEntry(getKvEntry(proto), proto.getTs());
    }

    private TsKvEntry toTimeseries(ClusterAPIProtos.KeyValueProto proto) {
        return new BasicTsKvEntry(proto.getTs(), getKvEntry(proto));
    }

    private KvEntry getKvEntry(ClusterAPIProtos.KeyValueProto proto) {
        KvEntry entry = null;
        DataType type = DataType.values()[proto.getValueType()];
        switch (type) {
            case BOOLEAN:
                entry = new BooleanDataEntry(proto.getKey(), proto.getBoolValue());
                break;
            case LONG:
                entry = new LongDataEntry(proto.getKey(), proto.getLongValue());
                break;
            case DOUBLE:
                entry = new DoubleDataEntry(proto.getKey(), proto.getDoubleValue());
                break;
            case STRING:
                entry = new StringDataEntry(proto.getKey(), proto.getStrValue());
                break;
        }
        return entry;
    }

    private SubscriptionUpdate convert(ClusterAPIProtos.SubscriptionUpdateProto proto) {
        if (proto.getErrorCode() > 0) {
            return new SubscriptionUpdate(proto.getSubscriptionId(), SubscriptionErrorCode.forCode(proto.getErrorCode()), proto.getErrorMsg());
        } else {
            Map<String, List<Object>> data = new TreeMap<>();
            proto.getDataList().forEach(v -> {
                List<Object> values = data.computeIfAbsent(v.getKey(), k -> new ArrayList<>());
                for (int i = 0; i < v.getTsCount(); i++) {
                    Object[] value = new Object[2];
                    value[0] = v.getTs(i);
                    value[1] = v.getValue(i);
                    values.add(value);
                }
            });
            return new SubscriptionUpdate(proto.getSubscriptionId(), data);
        }
    }

    private Optional<Subscription> getSubscription(String sessionId, int subscriptionId) {
        Subscription state = null;
        Map<Integer, Subscription> subMap = subscriptionsByWsSessionId.get(sessionId);
        if (subMap != null) {
            state = subMap.get(subscriptionId);
        }
        return Optional.ofNullable(state);
    }
}
