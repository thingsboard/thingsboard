/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@TbCoreComponent
@Service
public class DefaultTbLocalSubscriptionService implements TbLocalSubscriptionService {

    private final ConcurrentMap<String, Map<Integer, TbSubscription<?>>> subscriptionsBySessionId = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TbEntityLocalSubsInfo> subscriptionsByEntityId = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TbEntityUpdatesInfo> entityUpdates = new ConcurrentHashMap<>();

    private final AttributesService attrService;
    private final TimeseriesService tsService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;
    private final TbClusterService clusterService;
    private final SubscriptionManagerService subscriptionManagerService;

    private ExecutorService tsCallBackExecutor;

    public DefaultTbLocalSubscriptionService(AttributesService attrService, TimeseriesService tsService, TbServiceInfoProvider serviceInfoProvider,
                                             PartitionService partitionService, TbClusterService clusterService,
                                             @Lazy SubscriptionManagerService subscriptionManagerService) {
        this.attrService = attrService;
        this.tsService = tsService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
        this.clusterService = clusterService;
        this.subscriptionManagerService = subscriptionManagerService;
    }

    private String serviceId;
    private ExecutorService subscriptionUpdateExecutor;

    private final Lock subsLock = new ReentrantLock();

    @PostConstruct
    public void initExecutor() {
        subscriptionUpdateExecutor = ThingsBoardExecutors.newWorkStealingPool(20, getClass());
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ts-sub-callback"));
        serviceId = serviceInfoProvider.getServiceId();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (subscriptionUpdateExecutor != null) {
            subscriptionUpdateExecutor.shutdownNow();
        }
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    @EventListener(ClusterTopologyChangeEvent.class)
    public void onApplicationEvent(ClusterTopologyChangeEvent event) {
        if (event.getQueueKeys().stream().anyMatch(key -> ServiceType.TB_CORE.equals(key.getType()))) {
            /*
             * If the cluster topology has changed, we need to push all current subscriptions to SubscriptionManagerService again.
             * Otherwise, the SubscriptionManagerService may "forget" those subscriptions in case of restart.
             * Although this is resource consuming operation, it is cheaper than sending ping/pong commands periodically
             * It is also cheaper than caching the subscriptions by entity id and then lookup of those caches every time we have new telemetry in SubscriptionManagerService.
             * Even if we cache locally the list of active subscriptions by entity id, it is still time-consuming operation to get them from cache
             * Since number of subscriptions is usually much less than number of devices that are pushing data.
             */
            subscriptionsByEntityId.values().forEach(sub -> pushSubEventToManagerService(sub.getTenantId(), sub.getEntityId(), sub.toEvent(ComponentLifecycleEvent.UPDATED)));
        }
    }

    @Override
    public void onCoreStartupMsg(TransportProtos.CoreStartupMsg coreStartupMsg) {
        subscriptionUpdateExecutor.submit(() -> {
            Set<Integer> partitions = new HashSet<>(coreStartupMsg.getPartitionsList());
            AtomicInteger counter = new AtomicInteger();
            subscriptionsByEntityId.values().forEach(sub -> {
                var tpi = partitionService.resolve(ServiceType.TB_CORE, sub.getTenantId(), sub.getEntityId());
                if (!tpi.isMyPartition() && partitions.contains(tpi.getPartition().orElse(Integer.MAX_VALUE))) {
                    pushToQueue(sub.getEntityId(), sub.toEvent(ComponentLifecycleEvent.UPDATED), tpi);
                    counter.incrementAndGet();
                }
            });
            log.info("[{}] Pushed {} subscriptions to [{}]", serviceId, counter.get(), coreStartupMsg.getServiceId());
        });
    }

    @Override
    public void addSubscription(TbSubscription<?> subscription) {
        TenantId tenantId = subscription.getTenantId();
        EntityId entityId = subscription.getEntityId();
        log.debug("[{}][{}] Register subscription: {}", tenantId, entityId, subscription);
        Map<Integer, TbSubscription<?>> sessionSubscriptions = subscriptionsBySessionId.computeIfAbsent(subscription.getSessionId(), k -> new ConcurrentHashMap<>());
        sessionSubscriptions.put(subscription.getSubscriptionId(), subscription);
        modifySubscription(tenantId, entityId, subscription, true);
    }

    @Override
    public void onSubEventCallback(TransportProtos.TbEntitySubEventCallbackProto subEventCallback, TbCallback callback) {
        UUID entityId = new UUID(subEventCallback.getEntityIdMSB(), subEventCallback.getEntityIdLSB());
        onSubEventCallback(entityId, subEventCallback.getSeqNumber(), new TbEntityUpdatesInfo(subEventCallback.getAttributesUpdateTs(), subEventCallback.getTimeSeriesUpdateTs()), callback);
    }

    @Override
    public void onSubEventCallback(EntityId entityId, int seqNumber, TbEntityUpdatesInfo entityUpdatesInfo, TbCallback callback) {
        onSubEventCallback(entityId.getId(), seqNumber, entityUpdatesInfo, callback);
    }

    public void onSubEventCallback(UUID entityId, int seqNumber, TbEntityUpdatesInfo entityUpdatesInfo, TbCallback callback) {
        log.debug("[{}][{}] Processing sub event callback: {}.", entityId, seqNumber, entityUpdatesInfo);
        entityUpdates.put(entityId, entityUpdatesInfo);
        Set<TbSubscription<?>> pendingSubs = null;
        subsLock.lock();
        try {
            TbEntityLocalSubsInfo entitySubs = subscriptionsByEntityId.get(entityId);
            if (entitySubs != null) {
                pendingSubs = entitySubs.clearPendingSubscriptions(seqNumber);
            }
        } finally {
            subsLock.unlock();
        }
        if (pendingSubs != null) {
            pendingSubs.forEach(this::checkMissedUpdates);
        }
        callback.onSuccess();
    }

    @Override
    public void cancelSubscription(String sessionId, int subscriptionId) {
        log.debug("[{}][{}] Going to remove subscription.", sessionId, subscriptionId);
        Map<Integer, TbSubscription<?>> sessionSubscriptions = subscriptionsBySessionId.get(sessionId);
        if (sessionSubscriptions != null) {
            TbSubscription<?> subscription = sessionSubscriptions.remove(subscriptionId);
            if (subscription != null) {
                if (sessionSubscriptions.isEmpty()) {
                    subscriptionsBySessionId.remove(sessionId);
                }
                modifySubscription(subscription.getTenantId(), subscription.getEntityId(), subscription, false);
            } else {
                log.debug("[{}][{}] Subscription not found!", sessionId, subscriptionId);
            }
        } else {
            log.debug("[{}] No session subscriptions found!", sessionId);
        }
    }

    @Override
    public void cancelAllSessionSubscriptions(String sessionId) {
        log.debug("[{}] Going to remove session subscriptions.", sessionId);
        Map<Integer, TbSubscription<?>> sessionSubscriptions = subscriptionsBySessionId.remove(sessionId);
        if (sessionSubscriptions != null) {
            for (TbSubscription<?> subscription : sessionSubscriptions.values()) {
                modifySubscription(subscription.getTenantId(), subscription.getEntityId(), subscription, false);
            }
        } else {
            log.debug("[{}] No session subscriptions found!", sessionId);
        }
    }

    @Override
    public void onTimeSeriesUpdate(TransportProtos.TbSubUpdateProto proto, TbCallback callback) {
        //TODO: optimize to avoid re-wrapping from TsValueListProto -> List<KV> -> Map<String, List<Object>>. Low priority.
        onTimeSeriesUpdate(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()), TbSubscriptionUtils.fromProto(proto), callback);
    }

    @Override
    public void onTimeSeriesUpdate(EntityId entityId, List<TsKvEntry> data, TbCallback callback) {
        onTimeSeriesUpdate(entityId.getId(), data, callback);
    }

    private void onTimeSeriesUpdate(UUID entityId, List<TsKvEntry> data, TbCallback callback) {
        entityUpdates.get(entityId).timeSeriesUpdateTs = System.currentTimeMillis();
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.TIMESERIES.equals(sub.getType()),
                s -> {
                    TbTimeSeriesSubscription sub = (TbTimeSeriesSubscription) s;
                    List<TsKvEntry> updateData = null;
                    if (sub.isAllKeys()) {
                        updateData = data;
                    } else {
                        for (TsKvEntry kv : data) {
                            if (sub.getKeyStates().containsKey((kv.getKey()))) {
                                if (updateData == null) {
                                    updateData = new ArrayList<>();
                                }
                                updateData.add(kv);
                            }
                        }
                    }
                    if (updateData != null) {
                        TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(sub.getSubscriptionId(), updateData);
                        update.getLatestValues().forEach((key, value) -> sub.getKeyStates().put(key, value));
                        subscriptionUpdateExecutor.submit(() -> sub.getUpdateProcessor().accept(sub, update));
                    }
                }, callback);
    }

    @Override
    public void onAttributesUpdate(TransportProtos.TbSubUpdateProto proto, TbCallback callback) {
        onAttributesUpdate(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()), proto.getScope(), TbSubscriptionUtils.fromProto(proto), callback);
    }

    @Override
    public void onAttributesUpdate(EntityId entityId, String scope, List<TsKvEntry> data, TbCallback callback) {
        onAttributesUpdate(entityId.getId(), scope, data, callback);
    }

    private void onAttributesUpdate(UUID entityId, String scope, List<TsKvEntry> data, TbCallback callback) {
        entityUpdates.get(entityId).attributesUpdateTs = System.currentTimeMillis();
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.ATTRIBUTES.equals(sub.getType()),
                s -> {
                    TbAttributeSubscription sub = (TbAttributeSubscription) s;
                    if (sub.getScope() == null || TbAttributeSubscriptionScope.ANY_SCOPE.equals(sub.getScope()) || sub.getScope().name().equals(scope)) {
                        List<TsKvEntry> updateData = null;
                        if (sub.isAllKeys()) {
                            updateData = data;
                        } else {
                            for (TsKvEntry kv : data) {
                                if (sub.getKeyStates().containsKey((kv.getKey()))) {
                                    if (updateData == null) {
                                        updateData = new ArrayList<>();
                                    }
                                    updateData.add(kv);
                                }
                            }
                        }
                        if (updateData != null) {
                            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(sub.getSubscriptionId(), updateData);
                            update.getLatestValues().forEach((key, value) -> sub.getKeyStates().put(key, value));
                            subscriptionUpdateExecutor.submit(() -> sub.getUpdateProcessor().accept(sub, update));
                        }
                    }
                }, callback);
    }

    @Override
    public void onAlarmUpdate(TransportProtos.TbAlarmSubUpdateProto proto, TbCallback callback) {
        onAlarmUpdate(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()), TbSubscriptionUtils.fromProto(proto), callback);
    }

    @Override
    public void onAlarmUpdate(EntityId entityId, AlarmInfo alarm, boolean deleted, TbCallback callback) {
        onAlarmUpdate(entityId.getId(), new AlarmSubscriptionUpdate(alarm, deleted), callback);
    }

    private void onAlarmUpdate(UUID entityId, AlarmSubscriptionUpdate update, TbCallback callback) {
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.ALARMS.equals(sub.getType()),
                update, callback);
    }

    @Override
    public void onNotificationUpdate(TransportProtos.NotificationsSubUpdateProto proto, TbCallback callback) {
        onNotificationUpdate(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()), TbSubscriptionUtils.fromProto(proto), callback);
    }

    @Override
    public void onNotificationUpdate(EntityId entityId, NotificationsSubscriptionUpdate update, TbCallback callback) {
        onNotificationUpdate(entityId.getId(), update, callback);
    }

    private void onNotificationUpdate(UUID entityId, NotificationsSubscriptionUpdate update, TbCallback callback) {
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.NOTIFICATIONS.equals(sub.getType()) || TbSubscriptionType.NOTIFICATIONS_COUNT.equals(sub.getType()),
                update, callback);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNotificationRequestUpdate(TenantId tenantId, NotificationRequestUpdate update, TbCallback callback) {
        log.trace("[{}] Received notification request update: {}", tenantId, update);
        NotificationsSubscriptionUpdate theUpdate = new NotificationsSubscriptionUpdate(update);
        subscriptionsByEntityId.values().forEach(subInfo -> {
            if (subInfo.isNf() && tenantId.equals(subInfo.getTenantId()) && EntityType.USER.equals(subInfo.getEntityId().getEntityType())) {
                subInfo.getSubs().forEach(s -> {
                    TbSubscription<NotificationsSubscriptionUpdate> sub = (TbSubscription<NotificationsSubscriptionUpdate>) s;
                    subscriptionUpdateExecutor.submit(() -> sub.getUpdateProcessor().accept(sub, theUpdate));
                });
            }
        });
        callback.onSuccess();
    }

    @SuppressWarnings("unchecked")
    private <T> void processSubscriptionData(UUID entityId,
                                             Predicate<TbSubscription<?>> filter,
                                             T data,
                                             TbCallback callback) {
        log.trace("[{}] Received subscription data: {}", entityId, data);
        var subs = subscriptionsByEntityId.get(entityId);
        if (subs != null) {
            subs.getSubs().forEach(s -> {
                if (filter.test(s)) {
                    subscriptionUpdateExecutor.submit(() -> {
                        TbSubscription<T> sub = (TbSubscription<T>) s;
                        sub.getUpdateProcessor().accept(sub, data);
                    });
                }
            });
        }
        callback.onSuccess();
    }

    private void processSubscriptionData(UUID entityId,
                                         Predicate<TbSubscription<?>> filter,
                                         Consumer<TbSubscription<?>> processor,
                                         TbCallback callback) {
        var subs = subscriptionsByEntityId.get(entityId);
        if (subs != null) {
            subs.getSubs().forEach(s -> {
                if (filter.test(s)) {
                    processor.accept(s);
                }
            });
        }
        callback.onSuccess();
    }

    private void modifySubscription(TenantId tenantId, EntityId entityId, TbSubscription<?> subscription, boolean add) {
        TbSubscription<?> missedUpdatesCandidate = null;
        TbEntitySubEvent event;
        subsLock.lock();
        try {
            TbEntityLocalSubsInfo entitySubs = subscriptionsByEntityId.computeIfAbsent(entityId.getId(), id -> new TbEntityLocalSubsInfo(tenantId, entityId));
            event = add ? entitySubs.add(subscription) : entitySubs.remove(subscription);
            if (entitySubs.isEmpty()) {
                subscriptionsByEntityId.remove(entityId.getId());
                entityUpdates.remove(entityId.getId());
            } else if (add) {
                missedUpdatesCandidate = entitySubs.registerPendingSubscription(subscription, event);
            }
        } finally {
            subsLock.unlock();
        }
        if (event != null) {
            log.trace("[{}][{}][{}] Event: {}", tenantId, entityId, subscription.getSubscriptionId(), event);
            pushSubEventToManagerService(tenantId, entityId, event);
            if (missedUpdatesCandidate != null) {
                checkMissedUpdates(missedUpdatesCandidate);
            }
        } else {
            log.trace("[{}][{}][{}] No changes detected.", tenantId, entityId, subscription.getSubscriptionId());
        }
    }

    private void pushSubEventToManagerService(TenantId tenantId, EntityId entityId, TbEntitySubEvent event) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (tpi.isMyPartition()) {
            // Subscription is managed on the same server;
            subscriptionManagerService.onSubEvent(serviceId, event, TbCallback.EMPTY);
        } else {
            pushToQueue(entityId, event, tpi);
        }
    }

    private void pushToQueue(EntityId entityId, TbEntitySubEvent event, TopicPartitionInfo tpi) {
        clusterService.pushMsgToCore(tpi, entityId.getId(), TbSubscriptionUtils.toSubEventProto(serviceId, event), null);
    }

    private void checkMissedUpdates(TbSubscription<?> subscription) {
        log.trace("[{}][{}][{}] Check missed updates for subscription: {}",
                subscription.getTenantId(), subscription.getEntityId(), subscription.getSubscriptionId(), subscription);
        switch (subscription.getType()) {
            case TIMESERIES:
                handleNewTelemetrySubscription((TbTimeSeriesSubscription) subscription);
                break;
            case ATTRIBUTES:
                handleNewAttributeSubscription((TbAttributeSubscription) subscription);
                break;
        }
    }

    private void handleNewAttributeSubscription(TbAttributeSubscription subscription) {
        log.trace("[{}][{}][{}] Processing attribute subscription for entity [{}]",
                subscription.getTenantId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
        var entityUpdateInfo = entityUpdates.get(subscription.getEntityId().getId());
        if (entityUpdateInfo != null && entityUpdateInfo.attributesUpdateTs > 0 && subscription.getQueryTs() > entityUpdateInfo.attributesUpdateTs) {
            log.trace("[{}][{}][{}] No need to check for missed updates [{}]",
                    subscription.getTenantId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
            return;
        }
        final Map<String, Long> keyStates = subscription.getKeyStates();
        AttributeScope scope;
        if (subscription.getScope() != null && subscription.getScope().getAttributeScope() != null) {
            scope = subscription.getScope().getAttributeScope();
        } else {
            scope = AttributeScope.CLIENT_SCOPE;
        }
        DonAsynchron.withCallback(attrService.find(subscription.getTenantId(), subscription.getEntityId(), scope, keyStates.keySet()), values -> {
                    List<TsKvEntry> updates = new ArrayList<>();
                    values.forEach(latestEntry -> {
                        if (latestEntry.getLastUpdateTs() > keyStates.get(latestEntry.getKey())) {
                            updates.add(new BasicTsKvEntry(latestEntry.getLastUpdateTs(), latestEntry));
                        }
                    });
                    var missedUpdates = updates.stream().filter(u -> u.getValue() != null).collect(Collectors.toList());
                    if (!missedUpdates.isEmpty()) {
                        onAttributesUpdate(subscription.getEntityId(), scope.name(), missedUpdates, TbCallback.EMPTY);
                    }
                },
                e -> log.error("Failed to fetch missed updates.", e), tsCallBackExecutor);
    }

    private void handleNewTelemetrySubscription(TbTimeSeriesSubscription subscription) {
        log.trace("[{}][{}][{}] Processing telemetry subscription for entity [{}]",
                subscription.getTenantId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
        var entityUpdateInfo = entityUpdates.get(subscription.getEntityId().getId());
        if (entityUpdateInfo != null && entityUpdateInfo.timeSeriesUpdateTs > 0 && subscription.getQueryTs() > entityUpdateInfo.timeSeriesUpdateTs) {
            log.trace("[{}][{}][{}] No need to check for missed updates. time [{}][{}] diff: {}ms",
                    subscription.getTenantId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getQueryTs(), entityUpdateInfo.timeSeriesUpdateTs, subscription.getQueryTs() - entityUpdateInfo.timeSeriesUpdateTs);
            return;
        }

        long curTs = System.currentTimeMillis();

        if (subscription.isLatestValues()) {
            DonAsynchron.withCallback(tsService.findLatest(subscription.getTenantId(), subscription.getEntityId(), subscription.getKeyStates().keySet()),
                    missedUpdates -> {
                        if (missedUpdates != null && !missedUpdates.isEmpty()) {
                            missedUpdates = missedUpdates.stream().filter(u -> u.getValue() != null).collect(Collectors.toList());
                            if (!missedUpdates.isEmpty()) {
                                onTimeSeriesUpdate(subscription.getEntityId(), missedUpdates, TbCallback.EMPTY);
                            }
                        }
                    },
                    e -> log.error("Failed to fetch missed updates.", e),
                    tsCallBackExecutor);
        } else {
            List<ReadTsKvQuery> queries = new ArrayList<>();
            subscription.getKeyStates().forEach((key, value) -> {
                if (curTs > value) {
                    long startTs = subscription.getStartTime() > 0 ? Math.max(subscription.getStartTime(), value + 1L) : (value + 1L);
                    long endTs = subscription.getEndTime() > 0 ? Math.min(subscription.getEndTime(), curTs) : curTs;
                    queries.add(new BaseReadTsKvQuery(key, startTs, endTs, 0, 1000, Aggregation.NONE));
                }
            });
            if (!queries.isEmpty()) {
                DonAsynchron.withCallback(tsService.findAll(subscription.getTenantId(), subscription.getEntityId(), queries),
                        missedUpdates -> {
                            if (missedUpdates != null && !missedUpdates.isEmpty()) {
                                missedUpdates = missedUpdates.stream().filter(u -> u.getValue() != null).collect(Collectors.toList());
                                if (!missedUpdates.isEmpty()) {
                                    onTimeSeriesUpdate(subscription.getEntityId(), missedUpdates, TbCallback.EMPTY);
                                }
                            }
                        },
                        e -> log.error("Failed to fetch missed updates.", e),
                        tsCallBackExecutor);
            }
        }
    }

}
