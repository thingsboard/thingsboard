/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.cache.AttributesCacheUpdatedMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.attributes.TbAttributesCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.queue.TbPackCallback;
import org.thingsboard.server.service.queue.TbPackProcessingContext;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractConsumerService<N extends com.google.protobuf.GeneratedMessageV3> implements ApplicationListener<PartitionChangeEvent> {

    protected volatile ExecutorService consumersExecutor;
    protected volatile ExecutorService notificationsConsumerExecutor;
    protected volatile boolean stopped = false;

    protected final ActorSystemContext actorContext;
    protected final DataDecodingEncodingService encodingService;
    protected final TbTenantProfileCache tenantProfileCache;
    protected final TbDeviceProfileCache deviceProfileCache;
    protected final TbApiUsageStateService apiUsageStateService;

    @Nullable
    private final TbAttributesCache attributesCache;

    protected final TbQueueConsumer<TbProtoQueueMsg<N>> nfConsumer;

    public AbstractConsumerService(ActorSystemContext actorContext, DataDecodingEncodingService encodingService,
                                   TbTenantProfileCache tenantProfileCache, TbDeviceProfileCache deviceProfileCache,
                                   TbApiUsageStateService apiUsageStateService, Optional<TbAttributesCache> attributesCacheOpt,
                                   TbQueueConsumer<TbProtoQueueMsg<N>> nfConsumer) {
        this.actorContext = actorContext;
        this.encodingService = encodingService;
        this.tenantProfileCache = tenantProfileCache;
        this.deviceProfileCache = deviceProfileCache;
        this.apiUsageStateService = apiUsageStateService;
        this.attributesCache = attributesCacheOpt.orElse(null);
        this.nfConsumer = nfConsumer;
    }

    public void init(String mainConsumerThreadName, String nfConsumerThreadName) {
        this.consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(mainConsumerThreadName));
        this.notificationsConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(nfConsumerThreadName));
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 2)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Subscribing to notifications: {}", nfConsumer.getTopic());
        this.nfConsumer.subscribe();
        launchNotificationsConsumer();
        launchMainConsumers();
    }

    protected abstract ServiceType getServiceType();

    protected abstract void launchMainConsumers();

    protected abstract void stopMainConsumers();

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
                    ConcurrentMap<UUID, TbProtoQueueMsg<N>> pendingMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<N>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    pendingMap.forEach((id, msg) -> {
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

    protected void handleComponentLifecycleMsg(UUID id, ByteString nfMsg) {
        Optional<TbActorMsg> actorMsgOpt = encodingService.decode(nfMsg.toByteArray());
        if (actorMsgOpt.isPresent()) {
            TbActorMsg actorMsg = actorMsgOpt.get();
            if (actorMsg instanceof ComponentLifecycleMsg) {
                ComponentLifecycleMsg componentLifecycleMsg = (ComponentLifecycleMsg) actorMsg;
                log.info("[{}][{}][{}] Received Lifecycle event: {}", componentLifecycleMsg.getTenantId(), componentLifecycleMsg.getEntityId().getEntityType(),
                        componentLifecycleMsg.getEntityId(), componentLifecycleMsg.getEvent());
                if (EntityType.TENANT_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
                    TenantProfileId tenantProfileId = new TenantProfileId(componentLifecycleMsg.getEntityId().getId());
                    tenantProfileCache.evict(tenantProfileId);
                    if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED)) {
                        apiUsageStateService.onTenantProfileUpdate(tenantProfileId);
                    }
                } else if (EntityType.TENANT.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
                    tenantProfileCache.evict(componentLifecycleMsg.getTenantId());
                    if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED)) {
                        apiUsageStateService.onTenantUpdate(componentLifecycleMsg.getTenantId());
                    }
                } else if (EntityType.DEVICE_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
                    deviceProfileCache.evict(componentLifecycleMsg.getTenantId(), new DeviceProfileId(componentLifecycleMsg.getEntityId().getId()));
                } else if (EntityType.DEVICE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
                    deviceProfileCache.evict(componentLifecycleMsg.getTenantId(), new DeviceId(componentLifecycleMsg.getEntityId().getId()));
                } else if (EntityType.API_USAGE_STATE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
                    apiUsageStateService.onApiUsageStateUpdate(componentLifecycleMsg.getTenantId());
                }
            }
            log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg);
            actorContext.tellWithHighPriority(actorMsg);
        }
    }

    protected void handleAttributesCacheUpdatedMsg(UUID id, ByteString nfMsg) {
        if (attributesCache == null) {
            return;
        }
        Optional<TbActorMsg> actorMsgOpt = encodingService.decode(nfMsg.toByteArray());
        if (actorMsgOpt.isPresent()) {
            TbActorMsg actorMsg = actorMsgOpt.get();
            if (actorMsg instanceof AttributesCacheUpdatedMsg) {
                AttributesCacheUpdatedMsg attributesCacheUpdatedMsg = (AttributesCacheUpdatedMsg) actorMsg;
                if (AttributesCacheUpdatedMsg.INVALIDATE_ALL_CACHE_MSG.equals(attributesCacheUpdatedMsg)) {
                    log.info("[{}] Clearing all attributes cache", id);
                    attributesCache.invalidateAll();
                } else {
                    log.trace("[{}] Clearing attributes cache for {}", id, attributesCacheUpdatedMsg);
                    attributesCache.evict(attributesCacheUpdatedMsg.getTenantId(), attributesCacheUpdatedMsg.getEntityId(),
                            attributesCacheUpdatedMsg.getScope(), attributesCacheUpdatedMsg.getAttributeKeys());
                }
            }
        }
    }

    protected abstract void handleNotification(UUID id, TbProtoQueueMsg<N> msg, TbCallback callback) throws Exception;

    @PreDestroy
    public void destroy() {
        stopped = true;
        stopMainConsumers();
        if (nfConsumer != null) {
            nfConsumer.unsubscribe();
        }
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (notificationsConsumerExecutor != null) {
            notificationsConsumerExecutor.shutdownNow();
        }
    }
}
