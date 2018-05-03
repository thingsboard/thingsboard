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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.extensions.core.plugin.telemetry.handlers.TelemetryFeature;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.Subscription;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionState;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionUpdate;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.state.DeviceStateService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
    @Lazy
    private DeviceStateService stateService;

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

    private final Map<EntityId, Set<Subscription>> subscriptionsByEntityId = new HashMap<>();

    private final Map<String, Map<Integer, Subscription>> subscriptionsByWsSessionId = new HashMap<>();

    @Override
    public void addLocalWsSubscription(String sessionId, EntityId entityId, SubscriptionState sub) {
        Optional<ServerAddress> server = routingService.resolveById(entityId);
        Subscription subscription;
        if (server.isPresent()) {
            ServerAddress address = server.get();
            log.trace("[{}] Forwarding subscription [{}] for device [{}] to [{}]", sessionId, sub.getSubscriptionId(), entityId, address);
            subscription = new Subscription(sub, true, address);
//            rpcHandler.onNewSubscription(ctx, address, sessionId, subscription);
        } else {
            log.trace("[{}] Registering local subscription [{}] for device [{}]", sessionId, sub.getSubscriptionId(), entityId);
            subscription = new Subscription(sub, true);
        }
        registerSubscription(sessionId, entityId, subscription);
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
//            rpcHandler.onAttributesUpdate(ctx, serverAddress.get(), entityId, entries);
        }
    }

    private void onTimeseriesUpdate(EntityId entityId, List<TsKvEntry> ts) {
        Optional<ServerAddress> serverAddress = routingService.resolveById(entityId);
        if (!serverAddress.isPresent()) {
            onLocalTimeseriesUpdate(entityId, ts);
        } else {
//            rpcHandler.onTimeseriesUpdate(ctx, serverAddress.get(), entityId, entries);
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
                if (s.isAllKeys() || s.getKeyStates().containsKey((kv.getKey()))) {
                    if (subscriptionUpdate == null) {
                        subscriptionUpdate = new ArrayList<>();
                    }
                    subscriptionUpdate.add(kv);
                }
            }
            return subscriptionUpdate;
        });
    }

    private void onLocalSubUpdate(EntityId entityId, Predicate<Subscription> filter, Function<Subscription, List<TsKvEntry>> f) {
        Set<Subscription> deviceSubscriptions = subscriptionsByEntityId.get(entityId);
        if (deviceSubscriptions != null) {
            deviceSubscriptions.stream().filter(filter).forEach(s -> {
                String sessionId = s.getWsSessionId();
                List<TsKvEntry> subscriptionUpdate = f.apply(s);
                if (subscriptionUpdate == null || !subscriptionUpdate.isEmpty()) {
                    SubscriptionUpdate update = new SubscriptionUpdate(s.getSubscriptionId(), subscriptionUpdate);
                    if (s.isLocal()) {
                        updateSubscriptionState(sessionId, s, update);
                        wsService.sendWsMsg(sessionId, update);
                    } else {
                        //TODO: ashvayka
//                        rpcHandler.onSubscriptionUpdate(ctx, s.getServer(), sessionId, update);
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
        Set<Subscription> deviceSubscriptions = subscriptionsByEntityId.computeIfAbsent(entityId, k -> new HashSet<>());
        deviceSubscriptions.add(subscription);
        Map<Integer, Subscription> sessionSubscriptions = subscriptionsByWsSessionId.computeIfAbsent(sessionId, k -> new HashMap<>());
        sessionSubscriptions.put(subscription.getSubscriptionId(), subscription);
    }

    public void cleanupLocalWsSessionSubscriptions(String sessionId) {
        cleanupWsSessionSubscriptions(sessionId, true);
    }

    public void cleanupRemoteWsSessionSubscriptions(String sessionId) {
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
//            rpcHandler.onSessionClose(ctx, address, sessionId);
        }
    }

    private void processSubscriptionRemoval(String sessionId, Map<Integer, Subscription> sessionSubscriptions, Subscription subscription) {
        EntityId entityId = subscription.getEntityId();
        if (subscription.isLocal() && subscription.getServer() != null) {
//            rpcHandler.onSubscriptionClose(ctx, subscription.getServer(), sessionId, subscription.getSubscriptionId());
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
}
