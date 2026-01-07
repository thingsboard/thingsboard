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
package org.thingsboard.server.service.subscription;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.discovery.event.OtherServiceShutdownEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@TbCoreComponent
@Service
@RequiredArgsConstructor
public class DefaultSubscriptionManagerService extends TbApplicationEventListener<PartitionChangeEvent> implements SubscriptionManagerService {

    private final TopicService topicService;
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueProducerProvider producerProvider;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final SubscriptionSchedulerComponent scheduler;

    private final Lock subsLock = new ReentrantLock();
    private final ConcurrentMap<EntityId, TbEntityRemoteSubsInfo> entitySubscriptions = new ConcurrentHashMap<>();

    private final ConcurrentMap<EntityId, TbEntityUpdatesInfo> entityUpdates = new ConcurrentHashMap<>();

    private String serviceId;
    private TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNotificationsProducer;

    private long initTs;

    @PostConstruct
    public void initExecutor() {
        serviceId = serviceInfoProvider.getServiceId();
        initTs = System.currentTimeMillis();
        toCoreNotificationsProducer = producerProvider.getTbCoreNotificationsMsgProducer();
        scheduler.scheduleWithFixedDelay(this::cleanupEntityUpdates, 1, 1, TimeUnit.HOURS);
    }

    @Override
    public void onSubEvent(String serviceId, TbEntitySubEvent event, TbCallback callback) {
        var tenantId = event.getTenantId();
        var entityId = event.getEntityId();
        log.trace("[{}][{}][{}] Processing subscription event {}", tenantId, entityId, serviceId, event);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (tpi.isMyPartition()) {
            subsLock.lock();
            try {
                var entitySubs = entitySubscriptions.computeIfAbsent(entityId, id -> new TbEntityRemoteSubsInfo(tenantId, entityId));
                boolean empty = entitySubs.updateAndCheckIsEmpty(serviceId, event);
                if (empty) {
                    entitySubscriptions.remove(entityId);
                }
            } finally {
                subsLock.unlock();
            }
            callback.onSuccess();
            if (event.hasTsOrAttrSub()) {
                sendSubEventCallback(tenantId, serviceId, entityId, event.getSeqNumber());
            }
        } else {
            log.warn("[{}][{}][{}] Event belongs to external partition. Probably re-balancing is in progress. Topic: {}"
                    , tenantId, entityId, serviceId, tpi.getFullTopicName());
            callback.onFailure(new RuntimeException("Entity belongs to external partition " + tpi.getFullTopicName() + "!"));
        }
    }

    @Override
    @EventListener(OtherServiceShutdownEvent.class)
    public void onApplicationEvent(OtherServiceShutdownEvent event) {
        if (event.getServiceTypes() != null && event.getServiceTypes().contains(ServiceType.TB_CORE)) {
            subsLock.lock();
            try {
                int sizeBeforeCleanup = entitySubscriptions.size();
                entitySubscriptions.entrySet().removeIf(kv -> kv.getValue().removeAndCheckIsEmpty(event.getServiceId()));
                log.info("[{}][{}] Removed {} entity subscription records due to server shutdown.", serviceId, event.getServiceId(), entitySubscriptions.size() - sizeBeforeCleanup);
            } finally {
                subsLock.unlock();
            }
        }
    }

    private void sendSubEventCallback(TenantId tenantId, String targetId, EntityId entityId, int seqNumber) {
        var update = getEntityUpdatesInfo(entityId);
        if (serviceId.equals(targetId)) {
            localSubscriptionService.onSubEventCallback(tenantId, entityId, seqNumber, update, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetId, entityId, TbSubscriptionUtils.toProto(tenantId, entityId.getId(), seqNumber, update));
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            entitySubscriptions.values().removeIf(sub ->
                    !partitionService.isMyPartition(ServiceType.TB_CORE, sub.getTenantId(), sub.getEntityId()));
        }
    }

    @Override
    public void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, TbCallback callback) {
        onTimeSeriesUpdate(entityId, ts);
        callback.onSuccess();
    }

    @Override
    public void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, TbCallback callback) {
        onTimeSeriesUpdate(entityId,
                keys.stream().map(key -> new BasicTsKvEntry(0, new StringDataEntry(key, ""))).collect(Collectors.toList()));
        callback.onSuccess();
    }

    private void onTimeSeriesUpdate(EntityId entityId, List<TsKvEntry> update) {
        getEntityUpdatesInfo(entityId).timeSeriesUpdateTs = System.currentTimeMillis();
        TbEntityRemoteSubsInfo subInfo = entitySubscriptions.get(entityId);
        if (subInfo != null) {
            log.trace("[{}] Handling time-series update: {}", entityId, update);
            subInfo.getSubs().forEach((serviceId, sub) -> {
                if (sub.tsAllKeys) {
                    onTimeSeriesUpdate(serviceId, entityId, update);
                } else if (sub.tsKeys != null) {
                    List<TsKvEntry> tmp = getSubList(update, sub.tsKeys);
                    if (tmp != null) {
                        onTimeSeriesUpdate(serviceId, entityId, tmp);
                    }
                }
            });
        } else {
            log.trace("[{}] No time-series subscriptions for entity.", entityId);
        }
    }

    private void onTimeSeriesUpdate(String targetId, EntityId entityId, List<TsKvEntry> update) {
        if (serviceId.equals(targetId)) {
            localSubscriptionService.onTimeSeriesUpdate(entityId, update, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetId, entityId, TbSubscriptionUtils.toProto(entityId, update));
        }
    }

    @Override
    public void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, TbCallback callback) {
        getEntityUpdatesInfo(entityId).attributesUpdateTs = System.currentTimeMillis();
        processAttributesUpdate(entityId, scope, attributes);
        callback.onSuccess();
    }

    @Override
    public void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, TbCallback callback) {
        try {
            List<AttributeKvEntry> deletedEntries = keys.stream()
                    .<AttributeKvEntry>map(key -> new BaseAttributeKvEntry(0L, new StringDataEntry(key, "")))
                    .toList();
            processAttributesUpdate(entityId, scope, deletedEntries);
        } catch (Exception e) {
            callback.onFailure(e);
            return;
        }
        callback.onSuccess();
    }

    private void processAttributesUpdate(EntityId entityId, String scope, List<AttributeKvEntry> update) {
        TbEntityRemoteSubsInfo subInfo = entitySubscriptions.get(entityId);
        if (subInfo != null) {
            log.trace("[{}] Handling attributes update: {}", entityId, update);
            subInfo.getSubs().forEach((serviceId, sub) -> {
                if (sub.attrAllKeys) {
                    processAttributesUpdate(serviceId, entityId, scope, update);
                } else if (sub.attrKeys != null) {
                    List<AttributeKvEntry> tmp = getSubList(update, sub.attrKeys);
                    if (tmp != null) {
                        processAttributesUpdate(serviceId, entityId, scope, tmp);
                    }
                }
            });
        } else {
            log.trace("[{}] No attributes subscriptions for entity.", entityId);
        }
    }

    private void processAttributesUpdate(String targetId, EntityId entityId, String scope, List<AttributeKvEntry> update) {
        List<TsKvEntry> tsKvEntryList = update.stream().map(attr -> new BasicTsKvEntry(attr.getLastUpdateTs(), attr)).collect(Collectors.toList());
        if (serviceId.equals(targetId)) {
            localSubscriptionService.onAttributesUpdate(entityId, scope, tsKvEntryList, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetId, entityId, TbSubscriptionUtils.toProto(scope, entityId, tsKvEntryList));
        }
    }

    @Override
    public void onAlarmUpdate(TenantId tenantId, EntityId entityId, AlarmInfo alarm, TbCallback callback) {
        onAlarmSubUpdate(tenantId, entityId, alarm, false, callback);
    }

    @Override
    public void onAlarmDeleted(TenantId tenantId, EntityId entityId, AlarmInfo alarm, TbCallback callback) {
        onAlarmSubUpdate(tenantId, entityId, alarm, true, callback);
    }

    private void onAlarmSubUpdate(TenantId tenantId, EntityId entityId, AlarmInfo alarm, boolean deleted, TbCallback callback) {
        TbEntityRemoteSubsInfo subInfo = entitySubscriptions.get(entityId);
        if (subInfo != null) {
            log.trace("[{}][{}] Handling alarm update {}: {}", tenantId, entityId, alarm, deleted);
            for (Map.Entry<String, TbSubscriptionsInfo> entry : subInfo.getSubs().entrySet()) {
                if (entry.getValue().alarms) {
                    onAlarmSubUpdate(entry.getKey(), entityId, alarm, deleted);
                }
            }
        }
        callback.onSuccess();
    }

    private void onAlarmSubUpdate(String targetServiceId, EntityId entityId, AlarmInfo alarm, boolean deleted) {
        if (alarm == null) {
            log.warn("[{}] empty alarm update!", entityId);
            return;
        }
        if (serviceId.equals(targetServiceId)) {
            log.trace("[{}] Forwarding to local service: {} deleted: {}", entityId, alarm, deleted);
            localSubscriptionService.onAlarmUpdate(entityId, alarm, deleted, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetServiceId, entityId,
                    TbSubscriptionUtils.toAlarmSubUpdateToProto(entityId, alarm, deleted));
        }
    }

    private void sendCoreNotification(String targetServiceId, EntityId entityId, ToCoreNotificationMsg msg) {
        log.trace("[{}] Forwarding to remote service [{}]: {}", entityId, targetServiceId, msg);
        TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, targetServiceId);
        TbProtoQueueMsg<ToCoreNotificationMsg> queueMsg = new TbProtoQueueMsg<>(entityId.getId(), msg);
        toCoreNotificationsProducer.send(tpi, queueMsg, null);
    }

    @Override
    public void onNotificationUpdate(TenantId tenantId, UserId entityId, NotificationUpdate notificationUpdate, TbCallback callback) {
        TbEntityRemoteSubsInfo subInfo = entitySubscriptions.get(entityId);
        if (subInfo != null) {
            NotificationsSubscriptionUpdate subscriptionUpdate = new NotificationsSubscriptionUpdate(notificationUpdate);
            log.trace("[{}][{}] Handling notificationUpdate for user {}", tenantId, entityId, notificationUpdate);
            for (Map.Entry<String, TbSubscriptionsInfo> entry : subInfo.getSubs().entrySet()) {
                if (entry.getValue().notifications) {
                    onNotificationsSubUpdate(entry.getKey(), entityId, subscriptionUpdate);
                }
            }
        }
        callback.onSuccess();
    }

    private void onNotificationsSubUpdate(String targetServiceId, EntityId entityId, NotificationsSubscriptionUpdate subscriptionUpdate) {
        if (serviceId.equals(targetServiceId)) {
            log.trace("[{}] Forwarding to local service: {}", entityId, subscriptionUpdate);
            localSubscriptionService.onNotificationUpdate(entityId, subscriptionUpdate, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetServiceId, entityId,
                    TbSubscriptionUtils.notificationsSubUpdateToProto(entityId, subscriptionUpdate));
        }
    }

    private static <T extends KvEntry> List<T> getSubList(List<T> ts, Set<String> keys) {
        List<T> update = null;
        for (T entry : ts) {
            if (keys.contains(entry.getKey())) {
                if (update == null) {
                    update = new ArrayList<>(ts.size());
                }
                update.add(entry);
            }
        }
        return update;
    }

    private TbEntityUpdatesInfo getEntityUpdatesInfo(EntityId entityId) {
        return entityUpdates.computeIfAbsent(entityId, id -> new TbEntityUpdatesInfo(initTs));
    }

    private void cleanupEntityUpdates() {
        initTs = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        int sizeBeforeCleanup = entityUpdates.size();
        entityUpdates.entrySet().removeIf(kv -> {
            var v = kv.getValue();
            return initTs > v.attributesUpdateTs && initTs > v.timeSeriesUpdateTs;
        });
        log.info("Removed {} old entity update records.", entityUpdates.size() - sizeBeforeCleanup);
    }

}
