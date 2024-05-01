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
package org.thingsboard.server.service.queue.processing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.TbPackCallback;
import org.thingsboard.server.service.queue.TbPackProcessingContext;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractConsumerService<N extends com.google.protobuf.GeneratedMessageV3> extends TbApplicationEventListener<PartitionChangeEvent> {

    protected volatile ExecutorService notificationsConsumerExecutor;
    protected volatile boolean stopped = false;
    protected volatile boolean isReady = false;
    protected final ActorSystemContext actorContext;
    protected final TbTenantProfileCache tenantProfileCache;
    protected final TbDeviceProfileCache deviceProfileCache;
    protected final TbAssetProfileCache assetProfileCache;
    protected final TbApiUsageStateService apiUsageStateService;
    protected final PartitionService partitionService;
    protected final ApplicationEventPublisher eventPublisher;

    protected final TbQueueConsumer<TbProtoQueueMsg<N>> nfConsumer;
    protected final JwtSettingsService jwtSettingsService;

    public void init(String nfConsumerThreadName) {
        this.notificationsConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(nfConsumerThreadName));
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Subscribing to notifications: {}", nfConsumer.getTopic());
        this.nfConsumer.subscribe();
        this.isReady = true;
        launchNotificationsConsumer();
        launchMainConsumers();
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return event.getServiceType() == getServiceType();
    }

    protected abstract ServiceType getServiceType();

    protected abstract void launchMainConsumers();

    protected abstract void stopConsumers();

    protected abstract long getNotificationPollDuration();

    protected abstract long getNotificationPackProcessingTimeout();

    protected void launchNotificationsConsumer() {
        notificationsConsumerExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<N>> msgs = nfConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    List<IdMsgPair<N>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
                    ConcurrentMap<UUID, TbProtoQueueMsg<N>> pendingMap = orderedMsgList.stream().collect(
                            Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<N>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    orderedMsgList.forEach(element -> {
                        UUID id = element.getUuid();
                        TbProtoQueueMsg<N> msg = element.getMsg();
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
                    nfConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain notifications from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new notifications", e2);
                        }
                    }
                }
            }
            log.info("TB Notifications Consumer stopped.");
        });
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
                    partitionService.removeTenant(tenantId);
                }
            }
        } else if (EntityType.DEVICE_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            deviceProfileCache.evict(tenantId, new DeviceProfileId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.DEVICE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            deviceProfileCache.evict(tenantId, new DeviceId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ASSET_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            assetProfileCache.evict(tenantId, new AssetProfileId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ASSET.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            assetProfileCache.evict(tenantId, new AssetId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ENTITY_VIEW.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            actorContext.getTbEntityViewService().onComponentLifecycleMsg(componentLifecycleMsg);
        } else if (EntityType.API_USAGE_STATE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            apiUsageStateService.onApiUsageStateUpdate(tenantId);
        } else if (EntityType.CUSTOMER.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            if (componentLifecycleMsg.getEvent() == ComponentLifecycleEvent.DELETED) {
                apiUsageStateService.onCustomerDelete((CustomerId) componentLifecycleMsg.getEntityId());
            }
        }

        eventPublisher.publishEvent(componentLifecycleMsg);
        log.trace("[{}] Forwarding component lifecycle message to App Actor {}", id, componentLifecycleMsg);
        actorContext.tellWithHighPriority(componentLifecycleMsg);
    }

    protected abstract void handleNotification(UUID id, TbProtoQueueMsg<N> msg, TbCallback callback) throws Exception;

    @PreDestroy
    public void destroy() {
        stopped = true;
        stopConsumers();
        if (nfConsumer != null) {
            nfConsumer.unsubscribe();
        }
        if (notificationsConsumerExecutor != null) {
            notificationsConsumerExecutor.shutdownNow();
        }
    }
}
