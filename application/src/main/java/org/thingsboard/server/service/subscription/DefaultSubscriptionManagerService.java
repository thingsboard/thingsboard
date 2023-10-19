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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos.LocalSubscriptionServiceMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmSubscriptionUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbSubscriptionUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbSubscriptionUpdateTsValue;
import org.thingsboard.server.gen.transport.TransportProtos.TbSubscriptionUpdateValueListProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@TbCoreComponent
@Service
@RequiredArgsConstructor
public class DefaultSubscriptionManagerService extends TbApplicationEventListener<PartitionChangeEvent> implements SubscriptionManagerService {

    private final AttributesService attrService;
    private final TimeseriesService tsService;
    private final NotificationsTopicService notificationsTopicService;
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueProducerProvider producerProvider;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final DeviceStateService deviceStateService;
    private final TbClusterService clusterService;

    //TODO: decide on the type of locks we will use?
    private final ReadWriteLock subsLock = new ReentrantReadWriteLock();
    private final ConcurrentMap<EntityId, TbEntityRemoteSubsInfo> subscriptionsByEntityId = new ConcurrentHashMap<>();
    private final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();

    private ExecutorService tsCallBackExecutor;
    private String serviceId;
    private TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNotificationsProducer;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ts-sub-callback"));
        serviceId = serviceInfoProvider.getServiceId();
        toCoreNotificationsProducer = producerProvider.getTbCoreNotificationsMsgProducer();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public void onSubEvent(String serviceId, TbEntitySubEvent event, TbCallback callback) {
        var tenantId = event.getTenantId();
        var entityId = event.getEntityId();
        log.trace("[{}][{}][{}] Processing subscription event {}", tenantId, entityId, serviceId, event);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            subsLock.writeLock().lock();
            try {
                var entitySubs = subscriptionsByEntityId.computeIfAbsent(entityId, id -> new TbEntityRemoteSubsInfo(tenantId, entityId));
                boolean empty = entitySubs.updateAndCheckIsEmpty(serviceId, event);
                if (empty) {
                    subscriptionsByEntityId.remove(entityId);
                }
            } finally {
                subsLock.writeLock().unlock();
            }
            callback.onSuccess();
            //TODO: send notification back to local sub service that we have done the subscription.
//            switch (subscription.getType()) {
//                case TIMESERIES:
//                    handleNewTelemetrySubscription((TbTimeseriesSubscription) subscription);
//                    break;
//                case ATTRIBUTES:
//                    handleNewAttributeSubscription((TbAttributeSubscription) subscription);
//                    break;
//                case ALARMS:
//                    handleNewAlarmsSubscription((TbAlarmsSubscription) subscription);
//                    break;
//            }
        } else {
            log.warn("[{}][{}][{}] Event belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                    , tenantId, entityId, serviceId, tpi.getFullTopicName());
            callback.onFailure(new RuntimeException("Entity belongs to external partition " + tpi.getFullTopicName() + "!"));
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            currentPartitions.clear();
            currentPartitions.addAll(partitionChangeEvent.getPartitions());
            subscriptionsByEntityId.values().removeIf(sub ->
                    !currentPartitions.contains(partitionService.resolve(ServiceType.TB_CORE, sub.getTenantId(), sub.getEntityId())));
        }
    }

    @Override
    public void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, TbCallback callback) {
        processTimeSeriesUpdate(tenantId, entityId, ts);
        if (entityId.getEntityType() == EntityType.DEVICE) {
            updateDeviceInactivityTimeout(tenantId, entityId, ts);
        }
        callback.onSuccess();
    }

    @Override
    public void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, TbCallback callback) {
        processTimeSeriesUpdate(tenantId, entityId,
                keys.stream().map(key -> new BasicTsKvEntry(0, new StringDataEntry(key, ""))).collect(Collectors.toList()));
        if (entityId.getEntityType() == EntityType.DEVICE) {
            deleteDeviceInactivityTimeout(tenantId, entityId, keys);
        }
        callback.onSuccess();
    }

    public void processTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> update) {
        TbEntityRemoteSubsInfo subInfo = subscriptionsByEntityId.get(entityId);
        if (subInfo != null) {
            log.trace("[{}] Handling time-series update: {}", entityId, update);
            subInfo.getSubs().forEach((serviceId, sub) -> {
                if (sub.tsAllKeys) {
                    processTimeSeriesUpdate(serviceId, entityId, update);
                } else if (sub.tsKeys != null) {
                    List<TsKvEntry> tmp = getSubList(update, sub.tsKeys);
                    if (tmp != null) {
                        processTimeSeriesUpdate(serviceId, entityId, tmp);
                    }
                }
            });
        }
    }

    private void processTimeSeriesUpdate(String targetId, EntityId entityId, List<TsKvEntry> update) {
        if (serviceId.equals(targetId)) {
            localSubscriptionService.onTimeSeriesUpdate(entityId, update, TbCallback.EMPTY);
        } else {
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, targetId);
            //TODO: ignoreEmptyUpdates ???
            //toCoreNotificationsProducer.send(tpi, toProto(s, subscriptionUpdate, ignoreEmptyUpdates), null);
        }
    }

    @Override
    public void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, TbCallback callback) {
        onAttributesUpdate(tenantId, entityId, scope, attributes, true, callback);
    }

    @Override
    public void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, TbCallback callback) {
        processAttributesUpdate(tenantId, entityId, attributes);
        if (entityId.getEntityType() == EntityType.DEVICE) {
            if (TbAttributeSubscriptionScope.SERVER_SCOPE.name().equalsIgnoreCase(scope)) {
                updateDeviceInactivityTimeout(tenantId, entityId, attributes);
            } else if (TbAttributeSubscriptionScope.SHARED_SCOPE.name().equalsIgnoreCase(scope) && notifyDevice) {
                clusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onUpdate(tenantId,
                                new DeviceId(entityId.getId()), DataConstants.SHARED_SCOPE, new ArrayList<>(attributes))
                        , null);
            }
        }
        callback.onSuccess();
    }

    @Override
    public void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, TbCallback callback) {
        processAttributesUpdate(tenantId, entityId,
                keys.stream().map(key -> new BaseAttributeKvEntry(0, new StringDataEntry(key, ""))).collect(Collectors.toList()));
        if (entityId.getEntityType() == EntityType.DEVICE) {
            if (TbAttributeSubscriptionScope.SERVER_SCOPE.name().equalsIgnoreCase(scope)
                    || TbAttributeSubscriptionScope.ANY_SCOPE.name().equalsIgnoreCase(scope)) {
                deleteDeviceInactivityTimeout(tenantId, entityId, keys);
            } else if (TbAttributeSubscriptionScope.SHARED_SCOPE.name().equalsIgnoreCase(scope) && notifyDevice) {
                clusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(tenantId,
                        new DeviceId(entityId.getId()), scope, keys), null);
            }
        }
        callback.onSuccess();
    }

    public void processAttributesUpdate(TenantId tenantId, EntityId entityId, List<AttributeKvEntry> update) {
        TbEntityRemoteSubsInfo subInfo = subscriptionsByEntityId.get(entityId);
        if (subInfo != null) {
            log.trace("[{}] Handling time-series update: {}", entityId, update);
            subInfo.getSubs().forEach((serviceId, sub) -> {
                if (sub.attrAllKeys) {
                    processAttributesUpdate(serviceId, entityId, update);
                } else if (sub.attrKeys != null) {
                    List<AttributeKvEntry> tmp = getSubList(update, sub.attrKeys);
                    if (tmp != null) {
                        processAttributesUpdate(serviceId, entityId, tmp);
                    }
                }
            });
        }
    }

    private void processAttributesUpdate(String targetId, EntityId entityId, List<AttributeKvEntry> update) {
        if (serviceId.equals(targetId)) {
            localSubscriptionService.onAttributesUpdate(entityId, update, TbCallback.EMPTY);
        } else {
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, targetId);
            //TODO: ignoreEmptyUpdates ???
            //toCoreNotificationsProducer.send(tpi, toProto(s, subscriptionUpdate, ignoreEmptyUpdates), null);
        }
    }

    private void updateDeviceInactivityTimeout(TenantId tenantId, EntityId entityId, List<? extends KvEntry> kvEntries) {
        for (KvEntry kvEntry : kvEntries) {
            if (kvEntry.getKey().equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT)) {
                deviceStateService.onDeviceInactivityTimeoutUpdate(tenantId, new DeviceId(entityId.getId()), getLongValue(kvEntry));
            }
        }
    }

    private void deleteDeviceInactivityTimeout(TenantId tenantId, EntityId entityId, List<String> keys) {
        for (String key : keys) {
            if (key.equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT)) {
                deviceStateService.onDeviceInactivityTimeoutUpdate(tenantId, new DeviceId(entityId.getId()), 0);
            }
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
        TbEntityRemoteSubsInfo subInfo = subscriptionsByEntityId.get(entityId);
        if (subInfo != null) {
            log.trace("[{}][{}] Handling alarm update {}: {}", tenantId, entityId, alarm, deleted);
            for (Map.Entry<String, TbSubscriptionsInfo> entry : subInfo.getSubs().entrySet()) {
                if (entry.getValue().notifications) {
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
            log.trace("[{}] Forwarding to remote service [{}]: {} deleted: {}", entityId, targetServiceId, alarm, deleted);
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, targetServiceId);
            ToCoreNotificationMsg updateProto = TbSubscriptionUtils.notificationsSubUpdateToProto(subscription, subscriptionUpdate);
            TbProtoQueueMsg<ToCoreNotificationMsg> queueMsg = new TbProtoQueueMsg<>(entityId.getId(), updateProto);
            toCoreNotificationsProducer.send(tpi, queueMsg, null);
        }
    }

    @Override
    public void onNotificationUpdate(TenantId tenantId, UserId entityId, NotificationUpdate notificationUpdate, TbCallback callback) {
        TbEntityRemoteSubsInfo subInfo = subscriptionsByEntityId.get(entityId);
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
            log.trace("[{}] Forwarding to remote service [{}]: {}",
                    entityId, targetServiceId, subscriptionUpdate);
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, targetServiceId);
            ToCoreNotificationMsg updateProto = TbSubscriptionUtils.notificationsSubUpdateToProto(subscription, subscriptionUpdate);
            TbProtoQueueMsg<ToCoreNotificationMsg> queueMsg = new TbProtoQueueMsg<>(entityId.getId(), updateProto);
            toCoreNotificationsProducer.send(tpi, queueMsg, null);
        }
    }

    private void handleNewAttributeSubscription(TbAttributeSubscription subscription) {
        log.trace("[{}][{}][{}] Processing remote attribute subscription for entity [{}]",
                serviceId, subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());

        final Map<String, Long> keyStates = subscription.getKeyStates();
        DonAsynchron.withCallback(attrService.find(subscription.getTenantId(), subscription.getEntityId(), DataConstants.CLIENT_SCOPE, keyStates.keySet()), values -> {
                    List<TsKvEntry> missedUpdates = new ArrayList<>();
                    values.forEach(latestEntry -> {
                        if (latestEntry.getLastUpdateTs() > keyStates.get(latestEntry.getKey())) {
                            missedUpdates.add(new BasicTsKvEntry(latestEntry.getLastUpdateTs(), latestEntry));
                        }
                    });
                    if (!missedUpdates.isEmpty()) {
                        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, subscription.getServiceId());
                        toCoreNotificationsProducer.send(tpi, toProto(subscription, missedUpdates), null);
                    }
                },
                e -> log.error("Failed to fetch missed updates.", e), tsCallBackExecutor);
    }

    private void handleNewAlarmsSubscription(TbAlarmsSubscription subscription) {
        log.trace("[{}][{}][{}] Processing remote alarm subscription for entity [{}]",
                serviceId, subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
        //TODO: @dlandiak search all new alarms for this entity.
    }

    private void handleNewTelemetrySubscription(TbTimeseriesSubscription subscription) {
        log.trace("[{}][{}][{}] Processing remote telemetry subscription for entity [{}]",
                serviceId, subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());

        long curTs = System.currentTimeMillis();

        if (subscription.isLatestValues()) {
            DonAsynchron.withCallback(tsService.findLatest(subscription.getTenantId(), subscription.getEntityId(), subscription.getKeyStates().keySet()),
                    missedUpdates -> {
                        if (missedUpdates != null && !missedUpdates.isEmpty()) {
                            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, subscription.getServiceId());
                            toCoreNotificationsProducer.send(tpi, toProto(subscription, missedUpdates), null);
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
                                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, subscription.getServiceId());
                                toCoreNotificationsProducer.send(tpi, toProto(subscription, missedUpdates), null);
                            }
                        },
                        e -> log.error("Failed to fetch missed updates.", e),
                        tsCallBackExecutor);
            }
        }
    }

    private TbProtoQueueMsg<ToCoreNotificationMsg> toProto(TbSubscription subscription, List<TsKvEntry> updates) {
        return toProto(subscription, updates, true);
    }

    private TbProtoQueueMsg<ToCoreNotificationMsg> toProto(TbSubscription subscription, List<TsKvEntry> updates, boolean ignoreEmptyUpdates) {
        TbSubscriptionUpdateProto.Builder builder = TbSubscriptionUpdateProto.newBuilder();

        builder.setSessionId(subscription.getSessionId());
        builder.setSubscriptionId(subscription.getSubscriptionId());

        Map<String, List<Object>> data = new TreeMap<>();
        for (TsKvEntry tsEntry : updates) {
            List<Object> values = data.computeIfAbsent(tsEntry.getKey(), k -> new ArrayList<>());
            Object[] value = new Object[2];
            value[0] = tsEntry.getTs();
            value[1] = tsEntry.getValueAsString();
            values.add(value);
        }

        data.forEach((key, value) -> {
            TbSubscriptionUpdateValueListProto.Builder dataBuilder = TbSubscriptionUpdateValueListProto.newBuilder();
            dataBuilder.setKey(key);
            boolean hasData = false;
            for (Object v : value) {
                Object[] array = (Object[]) v;
                TbSubscriptionUpdateTsValue.Builder tsValueBuilder = TbSubscriptionUpdateTsValue.newBuilder();
                tsValueBuilder.setTs((long) array[0]);
                String strVal = (String) array[1];
                if (strVal != null) {
                    hasData = true;
                    tsValueBuilder.setValue(strVal);
                }
                dataBuilder.addTsValue(tsValueBuilder.build());
            }
            if (!ignoreEmptyUpdates || hasData) {
                builder.addData(dataBuilder.build());
            }
        });

        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setToLocalSubscriptionServiceMsg(
                        LocalSubscriptionServiceMsgProto.newBuilder().setSubUpdate(builder.build()).build())
                .build();
        return new TbProtoQueueMsg<>(subscription.getEntityId().getId(), toCoreMsg);
    }

    private TbProtoQueueMsg<ToCoreNotificationMsg> toProto(TbSubscription subscription, AlarmInfo alarm, boolean deleted) {
        TbAlarmSubscriptionUpdateProto.Builder builder = TbAlarmSubscriptionUpdateProto.newBuilder();

        builder.setSessionId(subscription.getSessionId());
        builder.setSubscriptionId(subscription.getSubscriptionId());
        builder.setAlarm(JacksonUtil.toString(alarm));
        builder.setDeleted(deleted);

        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setToLocalSubscriptionServiceMsg(
                        LocalSubscriptionServiceMsgProto.newBuilder()
                                .setAlarmSubUpdate(builder.build()).build())
                .build();
        return new TbProtoQueueMsg<>(subscription.getEntityId().getId(), toCoreMsg);
    }

    private static long getLongValue(KvEntry kve) {
        switch (kve.getDataType()) {
            case LONG:
                return kve.getLongValue().orElse(0L);
            case DOUBLE:
                return kve.getDoubleValue().orElse(0.0).longValue();
            case STRING:
                try {
                    return Long.parseLong(kve.getStrValue().orElse("0"));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            case JSON:
                try {
                    return Long.parseLong(kve.getJsonValue().orElse("0"));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            default:
                return 0L;
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

}
