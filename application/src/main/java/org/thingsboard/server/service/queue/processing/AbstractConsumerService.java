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
package org.thingsboard.server.service.queue.processing;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.resource.TbResourceDataCache;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldCache;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.TbPackCallback;
import org.thingsboard.server.service.queue.TbPackProcessingContext;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractConsumerService<N extends com.google.protobuf.GeneratedMessageV3> extends TbApplicationEventListener<PartitionChangeEvent> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ActorSystemContext actorContext;
    protected final TbTenantProfileCache tenantProfileCache;
    protected final TbDeviceProfileCache deviceProfileCache;
    protected final TbAssetProfileCache assetProfileCache;
    protected final TbResourceDataCache tbResourceDataCache;
    protected final CalculatedFieldCache calculatedFieldCache;
    protected final TbApiUsageStateService apiUsageStateService;
    protected final PartitionService partitionService;
    protected final ApplicationEventPublisher eventPublisher;
    protected final JwtSettingsService jwtSettingsService;

    protected QueueConsumerManager<TbProtoQueueMsg<N>> nfConsumer;

    protected ExecutorService consumersExecutor;
    protected ExecutorService mgmtExecutor;
    protected ScheduledExecutorService scheduler;

    public void init(String prefix) {
        this.consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(prefix + "-consumer"));
        this.mgmtExecutor = ThingsBoardExecutors.newWorkStealingPool(getMgmtThreadPoolSize(), prefix + "-mgmt");
        this.scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor(prefix + "-consumer-scheduler");

        this.nfConsumer = QueueConsumerManager.<TbProtoQueueMsg<N>>builder()
                .name(getServiceType().getLabel() + " Notifications")
                .msgPackProcessor(this::processNotifications)
                .pollInterval(getNotificationPollDuration())
                .consumerCreator(this::createNotificationsConsumer)
                .consumerExecutor(consumersExecutor)
                .threadPrefix("notifications")
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        startConsumers();
    }

    protected void startConsumers() {
        nfConsumer.subscribe();
        nfConsumer.launch();
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return event.getServiceType() == getServiceType();
    }

    protected abstract ServiceType getServiceType();

    protected void stopConsumers() {
        nfConsumer.stop();
    }

    protected abstract long getNotificationPollDuration();

    protected abstract long getNotificationPackProcessingTimeout();

    protected abstract int getMgmtThreadPoolSize();

    protected abstract TbQueueConsumer<TbProtoQueueMsg<N>> createNotificationsConsumer();

    protected void processNotifications(List<TbProtoQueueMsg<N>> msgs, TbQueueConsumer<TbProtoQueueMsg<N>> consumer) throws Exception {
        List<IdMsgPair<N>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).toList();
        ConcurrentMap<UUID, TbProtoQueueMsg<N>> pendingMap = orderedMsgList.stream().collect(
                Collectors.toConcurrentMap(IdMsgPair::uuid, IdMsgPair::msg));
        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
        TbPackProcessingContext<TbProtoQueueMsg<N>> ctx = new TbPackProcessingContext<>(
                processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
        orderedMsgList.forEach(element -> {
            UUID id = element.uuid();
            TbProtoQueueMsg<N> msg = element.msg();
            log.trace("[{}] Creating notification callback for message: {}", id, msg.getValue());
            TbCallback callback = new TbPackCallback<>(id, ctx);
            try {
                handleNotification(id, msg, callback);
            } catch (Throwable e) {
                log.warn("[{}] Failed to process notification: {}", id, msg, e);
                callback.onFailure(e);
            }
        });
        if (!processingTimeoutLatch.await(getNotificationPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
            ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process notification: {}", id, msg.getValue()));
            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process notification: {}", id, msg.getValue()));
        }
        consumer.commit();
    }

    protected final void handleComponentLifecycleMsg(UUID id, ComponentLifecycleMsg componentLifecycleMsg) {
        TenantId tenantId = componentLifecycleMsg.getTenantId();
        log.debug("[{}][{}][{}] Received Lifecycle event: {}", tenantId, componentLifecycleMsg.getEntityId().getEntityType(),
                componentLifecycleMsg.getEntityId(), componentLifecycleMsg.getEvent());
        if (EntityType.TENANT_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            TenantProfileId tenantProfileId = new TenantProfileId(componentLifecycleMsg.getEntityId().getId());
            tenantProfileCache.evict(tenantProfileId);
            if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED)) {
                apiUsageStateService.onTenantProfileUpdate(tenantProfileId);
                calculatedFieldCache.handleTenantProfileUpdate(tenantProfileId);
            }
        } else if (EntityType.TENANT.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                jwtSettingsService.reloadJwtSettings();
                return;
            } else {
                tenantProfileCache.evict(tenantId);
                partitionService.evictTenantInfo(tenantId);
                if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED)) {
                    apiUsageStateService.onTenantUpdate(tenantId);
                } else if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.DELETED)) {
                    apiUsageStateService.onTenantDelete(tenantId);
                    calculatedFieldCache.evictOwner(tenantId);
                    partitionService.removeTenant(tenantId);
                }
            }
        } else if (EntityType.DEVICE_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            deviceProfileCache.evict(tenantId, new DeviceProfileId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.DEVICE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            deviceProfileCache.evict(tenantId, new DeviceId(componentLifecycleMsg.getEntityId().getId()));
            if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.CREATED)) {
                calculatedFieldCache.addOwnerEntity(tenantId, componentLifecycleMsg.getEntityId());
            } else if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED) && componentLifecycleMsg.isOwnerChanged()) {
                calculatedFieldCache.updateOwnerEntity(tenantId, componentLifecycleMsg.getEntityId());
            } else if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.DELETED)) {
                calculatedFieldCache.evictEntity(componentLifecycleMsg.getEntityId());
            }
        } else if (EntityType.ASSET_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            assetProfileCache.evict(tenantId, new AssetProfileId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ASSET.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            assetProfileCache.evict(tenantId, new AssetId(componentLifecycleMsg.getEntityId().getId()));
            if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.CREATED)) {
                calculatedFieldCache.addOwnerEntity(tenantId, componentLifecycleMsg.getEntityId());
            } else if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED) && componentLifecycleMsg.isOwnerChanged()) {
                calculatedFieldCache.updateOwnerEntity(tenantId, componentLifecycleMsg.getEntityId());
            } else if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.DELETED)) {
                calculatedFieldCache.evictEntity(componentLifecycleMsg.getEntityId());
            }
        } else if (EntityType.ENTITY_VIEW.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            actorContext.getTbEntityViewService().onComponentLifecycleMsg(componentLifecycleMsg);
        } else if (EntityType.API_USAGE_STATE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            apiUsageStateService.onApiUsageStateUpdate(tenantId);
        } else if (EntityType.CUSTOMER.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.CREATED)) {
                calculatedFieldCache.addOwnerEntity(tenantId, componentLifecycleMsg.getEntityId());
            } else if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED) && componentLifecycleMsg.isOwnerChanged()) {
                calculatedFieldCache.updateOwnerEntity(tenantId, componentLifecycleMsg.getEntityId());
            } else if (componentLifecycleMsg.getEvent() == ComponentLifecycleEvent.DELETED) {
                apiUsageStateService.onCustomerDelete((CustomerId) componentLifecycleMsg.getEntityId());
                calculatedFieldCache.evictOwner(componentLifecycleMsg.getEntityId());
                calculatedFieldCache.evictEntity(componentLifecycleMsg.getEntityId());
            }
        } else if (EntityType.CALCULATED_FIELD.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            if (componentLifecycleMsg.getEvent() == ComponentLifecycleEvent.CREATED) {
                calculatedFieldCache.addCalculatedField(tenantId, (CalculatedFieldId) componentLifecycleMsg.getEntityId());
            } else if (componentLifecycleMsg.getEvent() == ComponentLifecycleEvent.UPDATED) {
                calculatedFieldCache.updateCalculatedField(tenantId, (CalculatedFieldId) componentLifecycleMsg.getEntityId());
            } else {
                calculatedFieldCache.evict((CalculatedFieldId) componentLifecycleMsg.getEntityId());
            }
        } else if (EntityType.TB_RESOURCE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            tbResourceDataCache.evictResourceData(tenantId, new TbResourceId(componentLifecycleMsg.getEntityId().getId()));
            return;
        }

        eventPublisher.publishEvent(componentLifecycleMsg);
        log.trace("[{}] Forwarding component lifecycle message to App Actor {}", id, componentLifecycleMsg);
        actorContext.tellWithHighPriority(componentLifecycleMsg);
    }

    protected abstract void handleNotification(UUID id, TbProtoQueueMsg<N> msg, TbCallback callback) throws Exception;

    @PreDestroy
    public void destroy() {
        stopConsumers();
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (mgmtExecutor != null) {
            mgmtExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

}
