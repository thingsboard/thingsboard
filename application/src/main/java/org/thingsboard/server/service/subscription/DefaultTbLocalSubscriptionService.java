/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsCountSubscription;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscription;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@TbCoreComponent
@Service
public class DefaultTbLocalSubscriptionService implements TbLocalSubscriptionService {

    private final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, Map<Integer, TbSubscription<?>>> subscriptionsBySessionId = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, TbEntityLocalSubsInfo> subscriptionsByEntityId = new ConcurrentHashMap<>();

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    @Lazy
    private SubscriptionManagerService subscriptionManagerService;

    private String serviceId;
    private ExecutorService subscriptionUpdateExecutor;

    private final TbApplicationEventListener<PartitionChangeEvent> partitionChangeListener = new TbApplicationEventListener<>() {
        @Override
        protected void onTbApplicationEvent(PartitionChangeEvent event) {
            if (ServiceType.TB_CORE.equals(event.getServiceType())) {
                currentPartitions.clear();
                currentPartitions.addAll(event.getPartitions());
            }
        }
    };

    private final TbApplicationEventListener<ClusterTopologyChangeEvent> clusterTopologyChangeListener = new TbApplicationEventListener<>() {
        @Override
        protected void onTbApplicationEvent(ClusterTopologyChangeEvent event) {
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
    };

    @PostConstruct
    public void initExecutor() {
        subscriptionUpdateExecutor = ThingsBoardExecutors.newWorkStealingPool(20, getClass());
        serviceId = serviceInfoProvider.getServiceId();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (subscriptionUpdateExecutor != null) {
            subscriptionUpdateExecutor.shutdownNow();
        }
    }

    @Override
    @EventListener(PartitionChangeEvent.class)
    public void onApplicationEvent(PartitionChangeEvent event) {
        partitionChangeListener.onApplicationEvent(event);
    }

    @Override
    @EventListener(ClusterTopologyChangeEvent.class)
    public void onApplicationEvent(ClusterTopologyChangeEvent event) {
        clusterTopologyChangeListener.onApplicationEvent(event);
    }

    @Override
    public void addSubscription(TbSubscription<?> subscription) {
        TenantId tenantId = subscription.getTenantId();
        EntityId entityId = subscription.getEntityId();
        log.trace("[{}][{}] Register subscription: {}", tenantId, entityId, subscription);
        Map<Integer, TbSubscription<?>> sessionSubscriptions = subscriptionsBySessionId.computeIfAbsent(subscription.getSessionId(), k -> new ConcurrentHashMap<>());
        sessionSubscriptions.put(subscription.getSubscriptionId(), subscription);
        modifySubscription(tenantId, entityId, subscription, TbEntityLocalSubsInfo::add);
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
                modifySubscription(subscription.getTenantId(), subscription.getEntityId(), subscription, TbEntityLocalSubsInfo::remove);
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
                modifySubscription(subscription.getTenantId(), subscription.getEntityId(), subscription, TbEntityLocalSubsInfo::remove);
            }
        } else {
            log.debug("[{}] No session subscriptions found!", sessionId);
        }
    }

    @Override
    public void onTimeSeriesUpdate(EntityId entityId, List<TsKvEntry> data, TbCallback callback) {
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.TIMESERIES.equals(sub.getType()),
                s -> {
                    TbTimeseriesSubscription sub = (TbTimeseriesSubscription) s;
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
                        TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(sub.getSubscriptionId(), data);
                        update.getLatestValues().forEach((key, value) -> sub.getKeyStates().put(key, value));
                        subscriptionUpdateExecutor.submit(() -> sub.getUpdateProcessor().accept(sub, update));
                    }
                }, callback);
    }

    @Override
    public void onAttributesUpdate(EntityId entityId, List<TsKvEntry> data, TbCallback callback) {
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.ATTRIBUTES.equals(sub.getType()),
                s -> {
                    TbAttributeSubscription sub = (TbAttributeSubscription) s;
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
                        TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(sub.getSubscriptionId(), data);
                        update.getLatestValues().forEach((key, value) -> sub.getKeyStates().put(key, value));
                        subscriptionUpdateExecutor.submit(() -> sub.getUpdateProcessor().accept(sub, update));
                    }
                }, callback);
    }

    @Override
    public void onAlarmUpdate(EntityId entityId, AlarmInfo alarm, boolean deleted, TbCallback callback) {
        AlarmSubscriptionUpdate update = new AlarmSubscriptionUpdate(alarm, deleted);
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.ALARMS.equals(sub.getType()),
                update, callback);
    }

    @Override
    public void onNotificationUpdate(EntityId entityId, NotificationsSubscriptionUpdate update, TbCallback callback) {
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.NOTIFICATIONS.equals(sub.getType()) || TbSubscriptionType.NOTIFICATIONS_COUNT.equals(sub.getType()),
                update, callback);
    }

    @Override
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

    private <T> void processSubscriptionData(EntityId entityId,
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

    private void processSubscriptionData(EntityId entityId,
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

    private void modifySubscription(TenantId tenantId, EntityId entityId, TbSubscription<?> subscription, BiFunction<TbEntityLocalSubsInfo, TbSubscription<?>, TbEntitySubEvent> modification) {
        TbEntityLocalSubsInfo entitySubs = subscriptionsByEntityId.computeIfAbsent(entityId, id -> new TbEntityLocalSubsInfo(tenantId, entityId));
        entitySubs.getLock().lock();
        try {
            TbEntitySubEvent event = modification.apply(entitySubs, subscription);
            if (event != null) {
                pushSubEventToManagerService(tenantId, entityId, event);
            }
            //TODO: remove entitySubs if it is empty. Requires global lock?
        } finally {
            entitySubs.getLock().unlock();
        }
    }

    private void pushSubEventToManagerService(TenantId tenantId, EntityId entityId, TbEntitySubEvent event) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            // Subscription is managed on the same server;
            subscriptionManagerService.onSubEvent(serviceId, event, TbCallback.EMPTY);
        } else {
            // Push to the queue;
            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toSubEventProto(serviceId, event);
            clusterService.pushMsgToCore(tpi, entityId.getId(), toCoreMsg, null);
        }
    }

}
