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
package org.thingsboard.server.service.subscription;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.service.telemetry.sub.SubscriptionUpdate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@TbCoreComponent
@Service
public class DefaultTbEntityDataSubscriptionService implements TbEntityDataSubscriptionService {

    private static final int DEFAULT_LIMIT = 100;
    private final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<Integer, TbSubscription>> subscriptionsBySessionId = new ConcurrentHashMap<>();

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

    private ExecutorService wsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        wsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ws-entity-sub-callback"));
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
        if (cmd.getHistoryCmd() != null) {
            handleHistoryCmd(session, cmd.getCmdId(), cmd.getQuery(), cmd.getHistoryCmd());
        } else if (cmd.getLatestCmd() != null) {
            handleLatestCmd(session, cmd.getCmdId(), cmd.getQuery(), cmd.getLatestCmd());
        } else {
            handleTimeseriesCmd(session, cmd.getCmdId(), cmd.getQuery(), cmd.getTsCmd());
        }
    }

    private void handleTimeseriesCmd(TelemetryWebSocketSessionRef session, int cmdId, EntityDataQuery query, TimeSeriesCmd tsCmd) {
    }

    private void handleLatestCmd(TelemetryWebSocketSessionRef session, int cmdId, EntityDataQuery query, LatestValueCmd latestCmd) {

    }

    private void handleHistoryCmd(TelemetryWebSocketSessionRef session, int cmdId, EntityDataQuery query, EntityHistoryCmd historyCmd) {
        TenantId tenantId = session.getSecurityCtx().getTenantId();
        CustomerId customerId = session.getSecurityCtx().getCustomerId();
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, customerId, query);
        List<ReadTsKvQuery> tsKvQueryList = historyCmd.getKeys().stream().map(key -> new BaseReadTsKvQuery(
                key, historyCmd.getStartTs(), historyCmd.getEndTs(), historyCmd.getInterval(), getLimit(historyCmd.getLimit()), historyCmd.getAgg()
        )).collect(Collectors.toList());
        Map<EntityData, ListenableFuture<List<TsKvEntry>>> fetchResultMap = new HashMap<>();
        data.getData().forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAll(tenantId, entityData.getEntityId(), tsKvQueryList)));
        Futures.allAsList(fetchResultMap.values()).addListener(() -> {
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
                    log.warn("[{}][{}][{}] Failed to fetch historical data", session.getSessionId(), cmdId, entityData.getEntityId(), e);
                }
            });
            EntityDataUpdate update = new EntityDataUpdate(cmdId, data, null);
            wsService.sendWsMsg(session.getSessionId(), update);
        }, wsCallBackExecutor);
    }


    @Override
    public void cancelSubscription(String sessionId, EntityDataUnsubscribeCmd subscriptionId) {

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

    private void registerSubscription(TbSubscription subscription) {
        Map<Integer, TbSubscription> sessionSubscriptions = subscriptionsBySessionId.computeIfAbsent(subscription.getSessionId(), k -> new ConcurrentHashMap<>());
        sessionSubscriptions.put(subscription.getSubscriptionId(), subscription);
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

}
