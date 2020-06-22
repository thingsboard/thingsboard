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
package org.thingsboard.server.service.subscription;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.ClusterTopologyChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUnsubscribeCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityHistoryCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.TimeSeriesCmd;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;
import org.thingsboard.server.service.telemetry.sub.SubscriptionUpdate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@TbCoreComponent
@Service
public class DefaultTbEntityDataSubscriptionService implements TbEntityDataSubscriptionService {

    private static final int DEFAULT_LIMIT = 100;
    private final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<Integer, TbEntityDataSubCtx>> subscriptionsBySessionId = new ConcurrentHashMap<>();

    @Autowired
    private TelemetryWebSocketService wsService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    @Lazy
    private SubscriptionManagerService subscriptionManagerService;

    @Autowired
    private TimeseriesService tsService;

    @Value("${database.ts.type}")
    private String databaseTsType;

    private ExecutorService wsCallBackExecutor;
    private boolean tsInSqlDB;

    @PostConstruct
    public void initExecutor() {
        wsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ws-entity-sub-callback"));
        tsInSqlDB = databaseTsType.equalsIgnoreCase("sql") || databaseTsType.equalsIgnoreCase("timescale");
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    @EventListener(PartitionChangeEvent.class)
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            currentPartitions.clear();
            currentPartitions.addAll(partitionChangeEvent.getPartitions());
        }
    }

    @Override
    @EventListener(ClusterTopologyChangeEvent.class)
    public void onApplicationEvent(ClusterTopologyChangeEvent event) {
        if (event.getServiceQueueKeys().stream().anyMatch(key -> ServiceType.TB_CORE.equals(key.getServiceType()))) {
            /*
             * If the cluster topology has changed, we need to push all current subscriptions to SubscriptionManagerService again.
             * Otherwise, the SubscriptionManagerService may "forget" those subscriptions in case of restart.
             * Although this is resource consuming operation, it is cheaper than sending ping/pong commands periodically
             * It is also cheaper then caching the subscriptions by entity id and then lookup of those caches every time we have new telemetry in SubscriptionManagerService.
             * Even if we cache locally the list of active subscriptions by entity id, it is still time consuming operation to get them from cache
             * Since number of subscriptions is usually much less then number of devices that are pushing data.
//             */
//            subscriptionsBySessionId.values().forEach(map -> map.values()
//                    .forEach(sub -> pushSubscriptionToManagerService(sub, false)));
        }
    }

    @Override
    public void handleCmd(TelemetryWebSocketSessionRef session, EntityDataCmd cmd) {
        TbEntityDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx != null) {
            log.debug("[{}][{}] Updating existing subscriptions using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            //TODO: cleanup old subscription;
        } else {
            log.debug("[{}][{}] Creating new subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        if (cmd.getQuery() != null) {
            if (ctx.getQuery() == null) {
                log.debug("[{}][{}] Initializing data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            } else {
                log.debug("[{}][{}] Updating data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            }
            ctx.setQuery(cmd.getQuery());
            TenantId tenantId = ctx.getTenantId();
            CustomerId customerId = ctx.getCustomerId();
            EntityDataQuery query = ctx.getQuery();
            //Step 1. Update existing query with the contents of LatestValueCmd
            if (cmd.getLatestCmd() != null) {
                cmd.getLatestCmd().getKeys().forEach(key -> {
                    if (!query.getLatestValues().contains(key)) {
                        query.getLatestValues().add(key);
                    }
                });
            }
            PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, customerId, ctx.getQuery());
            ctx.setData(data);
        }
        ListenableFuture<TbEntityDataSubCtx> historyFuture;
        if (cmd.getHistoryCmd() != null) {
            historyFuture = handleHistoryCmd(ctx, cmd.getHistoryCmd());
        } else {
            historyFuture = Futures.immediateFuture(ctx);
        }
        if (cmd.getLatestCmd() != null) {
            Futures.addCallback(historyFuture, new FutureCallback<TbEntityDataSubCtx>() {
                @Override
                public void onSuccess(@Nullable TbEntityDataSubCtx theCtx) {
                    handleLatestCmd(theCtx, cmd.getLatestCmd());
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("[{}][{}] Failed to process command", session.getSessionId(), cmd.getCmdId());
                }
            }, wsCallBackExecutor);
        } else if (cmd.getTsCmd() != null) {
            handleTimeseriesCmd(ctx, cmd.getTsCmd());
        }
    }

    private TbEntityDataSubCtx createSubCtx(TelemetryWebSocketSessionRef sessionRef, EntityDataCmd cmd) {
        Map<Integer, TbEntityDataSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        TbEntityDataSubCtx ctx = new TbEntityDataSubCtx(sessionRef, cmd.getCmdId());
        ctx.setQuery(cmd.getQuery());
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    private TbEntityDataSubCtx getSubCtx(String sessionId, int cmdId) {
        Map<Integer, TbEntityDataSubCtx> sessionSubs = subscriptionsBySessionId.get(sessionId);
        if (sessionSubs != null) {
            return sessionSubs.get(cmdId);
        } else {
            return null;
        }
    }

    private void handleTimeseriesCmd(TbEntityDataSubCtx ctx, TimeSeriesCmd tsCmd) {
    }

    private void handleLatestCmd(TbEntityDataSubCtx ctx, LatestValueCmd latestCmd) {
        //Fetch the latest values for telemetry keys (in case they are not copied from NoSQL to SQL DB in hybrid mode.
        if (!tsInSqlDB) {
            List<String> allTsKeys = latestCmd.getKeys().stream()
                    .filter(key -> key.getType().equals(EntityKeyType.TIME_SERIES))
                    .map(EntityKey::getKey).collect(Collectors.toList());

            Map<EntityData, ListenableFuture<Map<String, TsValue>>> missingTelemetryFurutes = new HashMap<>();
            for (EntityData entityData : ctx.getData().getData()) {
                Map<EntityKeyType, Map<String, TsValue>> latestEntityData = entityData.getLatest();
                Map<String, TsValue> tsEntityData = latestEntityData.get(EntityKeyType.TIME_SERIES);
                Set<String> missingTsKeys = new LinkedHashSet<>(allTsKeys);
                if (tsEntityData != null) {
                    missingTsKeys.removeAll(tsEntityData.keySet());
                } else {
                    tsEntityData = new HashMap<>();
                    latestEntityData.put(EntityKeyType.TIME_SERIES, tsEntityData);
                }

                ListenableFuture<List<TsKvEntry>> missingTsData = tsService.findLatest(ctx.getTenantId(), entityData.getEntityId(), missingTsKeys);
                missingTelemetryFurutes.put(entityData, Futures.transform(missingTsData, this::toTsValue, MoreExecutors.directExecutor()));
            }
            Futures.addCallback(Futures.allAsList(missingTelemetryFurutes.values()), new FutureCallback<List<Map<String, TsValue>>>() {
                @Override
                public void onSuccess(@Nullable List<Map<String, TsValue>> result) {
                    missingTelemetryFurutes.forEach((key, value) -> {
                        try {
                            key.getLatest().get(EntityKeyType.TIME_SERIES).putAll(value.get());
                        } catch (InterruptedException | ExecutionException e) {
                            log.warn("[{}][{}] Failed to lookup latest telemetry: {}:{}", ctx.getSessionId(), ctx.getCmdId(), key.getEntityId(), allTsKeys, e);
                        }
                    });
                    EntityDataUpdate update;
                    if (!ctx.isInitialDataSent()) {
                        update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null);
                    } else {
                        update = new EntityDataUpdate(ctx.getCmdId(), null, ctx.getData().getData());
                    }
                    wsService.sendWsMsg(ctx.getSessionId(), update);
                    //TODO: create context for this (session, cmdId) that contains query, latestCmd and update. Subscribe + periodic updates.
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("[{}][{}] Failed to process websocket command: {}:{}", ctx.getSessionId(), ctx.getCmdId(), ctx.getQuery(), latestCmd, t);
                    wsService.sendWsMsg(ctx.getSessionId(),
                            new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to process websocket command!"));
                }
            }, wsCallBackExecutor);
        } else {
            if (!ctx.isInitialDataSent()) {
                EntityDataUpdate update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null);
                wsService.sendWsMsg(ctx.getSessionId(), update);
            }
            //TODO: create context for this (session, cmdId) that contains query, latestCmd and update. Subscribe + periodic updates.
        }
    }

    private Map<String, TsValue> toTsValue(List<TsKvEntry> data) {
        return data.stream().collect(Collectors.toMap(TsKvEntry::getKey, value -> new TsValue(value.getTs(), value.getValueAsString())));
    }

    private ListenableFuture<TbEntityDataSubCtx> handleHistoryCmd(TbEntityDataSubCtx ctx, EntityHistoryCmd historyCmd) {
        List<ReadTsKvQuery> tsKvQueryList = historyCmd.getKeys().stream().map(key -> new BaseReadTsKvQuery(
                key, historyCmd.getStartTs(), historyCmd.getEndTs(), historyCmd.getInterval(), getLimit(historyCmd.getLimit()), historyCmd.getAgg()
        )).collect(Collectors.toList());
        Map<EntityData, ListenableFuture<List<TsKvEntry>>> fetchResultMap = new HashMap<>();
        ctx.getData().getData().forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAll(ctx.getTenantId(), entityData.getEntityId(), tsKvQueryList)));
        return Futures.transform(Futures.allAsList(fetchResultMap.values()), f -> {
            fetchResultMap.forEach((entityData, future) -> {
                Map<String, List<TsValue>> keyData = new LinkedHashMap<>();
                historyCmd.getKeys().forEach(key -> keyData.put(key, new ArrayList<>()));
                try {
                    List<TsKvEntry> entityTsData = future.get();
                    if (entityTsData != null) {
                        entityTsData.forEach(entry -> keyData.get(entry.getKey()).add(new TsValue(entry.getTs(), entry.getValueAsString())));
                    }
                    keyData.forEach((k, v) -> entityData.getTimeseries().put(k, v.toArray(new TsValue[v.size()])));
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("[{}][{}][{}] Failed to fetch historical data", ctx.getSessionId(), ctx.getCmdId(), entityData.getEntityId(), e);
                    wsService.sendWsMsg(ctx.getSessionId(),
                            new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to fetch historical data!"));
                }
            });
            EntityDataUpdate update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null);
            wsService.sendWsMsg(ctx.getSessionId(), update);
            ctx.setInitialDataSent(true);
            return ctx;
        }, wsCallBackExecutor);
    }

    @Override
    public void cancelSubscription(String sessionId, EntityDataUnsubscribeCmd cmd) {
        TbEntityDataSubCtx ctx = getSubCtx(sessionId, cmd.getCmdId());
    }

//    //TODO 3.1: replace null callbacks with callbacks from websocket service.
//    @Override
//    public void addSubscription(TbSubscription subscription) {
//        EntityId entityId = subscription.getEntityId();
//        // Telemetry subscription on Entity Views are handled differently, because we need to allow only certain keys and time ranges;
//        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW) && TbSubscriptionType.TIMESERIES.equals(subscription.getType())) {
//            subscription = resolveEntityViewSubscription((TbTimeseriesSubscription) subscription);
//        }
//        pushSubscriptionToManagerService(subscription, true);
//        registerSubscription(subscription);
//    }

//    private void pushSubscriptionToManagerService(TbSubscription subscription, boolean pushToLocalService) {
//        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, subscription.getTenantId(), subscription.getEntityId());
//        if (currentPartitions.contains(tpi)) {
//            // Subscription is managed on the same server;
//            if (pushToLocalService) {
//                subscriptionManagerService.addSubscription(subscription, TbCallback.EMPTY);
//            }
//        } else {
//            // Push to the queue;
//            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toNewSubscriptionProto(subscription);
//            clusterService.pushMsgToCore(tpi, subscription.getEntityId().getId(), toCoreMsg, null);
//        }
//    }

    @Override
    public void onSubscriptionUpdate(String sessionId, SubscriptionUpdate update, TbCallback callback) {
//        TbSubscription subscription = subscriptionsBySessionId
//                .getOrDefault(sessionId, Collections.emptyMap()).get(update.getSubscriptionId());
//        if (subscription != null) {
//            switch (subscription.getType()) {
//                case TIMESERIES:
//                    TbTimeseriesSubscription tsSub = (TbTimeseriesSubscription) subscription;
//                    update.getLatestValues().forEach((key, value) -> tsSub.getKeyStates().put(key, value));
//                    break;
//                case ATTRIBUTES:
//                    TbAttributeSubscription attrSub = (TbAttributeSubscription) subscription;
//                    update.getLatestValues().forEach((key, value) -> attrSub.getKeyStates().put(key, value));
//                    break;
//            }
//            wsService.sendWsMsg(sessionId, update);
//        }
//        callback.onSuccess();
    }

//    @Override
//    public void cancelSubscription(String sessionId, int subscriptionId) {
//        log.debug("[{}][{}] Going to remove subscription.", sessionId, subscriptionId);
//        Map<Integer, TbSubscription> sessionSubscriptions = subscriptionsBySessionId.get(sessionId);
//        if (sessionSubscriptions != null) {
//            TbSubscription subscription = sessionSubscriptions.remove(subscriptionId);
//            if (subscription != null) {
//                if (sessionSubscriptions.isEmpty()) {
//                    subscriptionsBySessionId.remove(sessionId);
//                }
//                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, subscription.getTenantId(), subscription.getEntityId());
//                if (currentPartitions.contains(tpi)) {
//                    // Subscription is managed on the same server;
//                    subscriptionManagerService.cancelSubscription(sessionId, subscriptionId, TbCallback.EMPTY);
//                } else {
//                    // Push to the queue;
//                    TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toCloseSubscriptionProto(subscription);
//                    clusterService.pushMsgToCore(tpi, subscription.getEntityId().getId(), toCoreMsg, null);
//                }
//            } else {
//                log.debug("[{}][{}] Subscription not found!", sessionId, subscriptionId);
//            }
//        } else {
//            log.debug("[{}] No session subscriptions found!", sessionId);
//        }
//    }

    @Override
    public void cancelAllSessionSubscriptions(String sessionId) {
//        Map<Integer, TbSubscription> subscriptions = subscriptionsBySessionId.get(sessionId);
//        if (subscriptions != null) {
//            Set<Integer> toRemove = new HashSet<>(subscriptions.keySet());
//            toRemove.forEach(id -> cancelSubscription(sessionId, id));
//        }
    }

    private TbSubscription resolveEntityViewSubscription(TbTimeseriesSubscription subscription) {
        EntityView entityView = entityViewService.findEntityViewById(TenantId.SYS_TENANT_ID, new EntityViewId(subscription.getEntityId().getId()));

        Map<String, Long> keyStates;
        if (subscription.isAllKeys()) {
            keyStates = entityView.getKeys().getTimeseries().stream().collect(Collectors.toMap(k -> k, k -> 0L));
        } else {
            keyStates = subscription.getKeyStates().entrySet()
                    .stream().filter(entry -> entityView.getKeys().getTimeseries().contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return TbTimeseriesSubscription.builder()
                .serviceId(subscription.getServiceId())
                .sessionId(subscription.getSessionId())
                .subscriptionId(subscription.getSubscriptionId())
                .tenantId(subscription.getTenantId())
                .entityId(entityView.getEntityId())
                .startTime(entityView.getStartTimeMs())
                .endTime(entityView.getEndTimeMs())
                .allKeys(false)
                .keyStates(keyStates).build();
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

}
