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
package org.thingsboard.server.service.queue;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueProvider;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.subscription.LocalSubscriptionService;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
@Slf4j
public class DefaultTbCoreConsumerService implements TbCoreConsumerService {

    @Value("${queue.core.poll_interval}")
    private long pollDuration;
    @Value("${queue.core.pack_processing_timeout}")
    private long packProcessingTimeout;
    @Value("${queue.core.stats.enabled:false}")
    private boolean statsEnabled;

    private final ActorSystemContext actorContext;
    private final DeviceStateService stateService;
    private final LocalSubscriptionService localSubscriptionService;
    private final SubscriptionManagerService subscriptionManagerService;
    private final TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> consumer;
    private final TbCoreConsumerStats stats = new TbCoreConsumerStats();
    private volatile ExecutorService mainConsumerExecutor;
    private volatile boolean stopped = false;

    public DefaultTbCoreConsumerService(TbCoreQueueProvider tbCoreQueueProvider, ActorSystemContext actorContext, DeviceStateService stateService, LocalSubscriptionService localSubscriptionService, SubscriptionManagerService subscriptionManagerService) {
        this.consumer = tbCoreQueueProvider.getToCoreMsgConsumer();
        this.actorContext = actorContext;
        this.stateService = stateService;
        this.localSubscriptionService = localSubscriptionService;
        this.subscriptionManagerService = subscriptionManagerService;
    }

    @PostConstruct
    public void init() {
        this.mainConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-core-consumer"));
    }

    @Override
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (partitionChangeEvent.getServiceKey().getServiceType() == ServiceType.TB_CORE) {
            log.info("Subscribing to partitions: {}", partitionChangeEvent.getPartitions());
            this.consumer.subscribe(partitionChangeEvent.getPartitions());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        mainConsumerExecutor.execute(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToCoreMsg>> msgs = consumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToCoreMsg>> pendingMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToCoreMsg>> successMap = new ConcurrentHashMap<>();
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToCoreMsg>> failedMap = new ConcurrentHashMap<>();
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    pendingMap.forEach((id, msg) -> {
                        TbMsgCallback callback = new MsgPackCallback<>(id, processingTimeoutLatch, pendingMap, successMap, failedMap);
                        try {
                            ToCoreMsg toCoreMsg = msg.getValue();
                            if (toCoreMsg.hasToDeviceActorMsg()) {
                                log.trace("[{}] Forwarding message to device actor {}", id, toCoreMsg.getToDeviceActorMsg());
                                forwardToDeviceActor(toCoreMsg.getToDeviceActorMsg(), callback);
                            } else if (toCoreMsg.hasDeviceStateServiceMsg()) {
                                log.trace("[{}] Forwarding message to state service {}", id, toCoreMsg.getDeviceStateServiceMsg());
                                forwardToStateService(toCoreMsg.getDeviceStateServiceMsg(), callback);
                            } else if (toCoreMsg.hasToSubscriptionMgrMsg()) {
                                log.trace("[{}] Forwarding message to subscription manager service {}", id, toCoreMsg.getToSubscriptionMgrMsg());
                                forwardToSubMgrService(toCoreMsg.getToSubscriptionMgrMsg(), callback);
                            } else if (toCoreMsg.hasToLocalSubscriptionServiceMsg()) {
                                log.trace("[{}] Forwarding message to local subscription service {}", id, toCoreMsg.getToLocalSubscriptionServiceMsg());
                                forwardToLocalSubMgrService(toCoreMsg.getToLocalSubscriptionServiceMsg(), callback);
                            }
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process message: {}", id, msg, e);
                            callback.onFailure(e);
                        }
                    });
                    if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
                        pendingMap.forEach((id, msg) -> log.warn("[{}] Timeout to process message: {}", id, msg.getValue()));
                        failedMap.forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
                    }
                    consumer.commit();
                } catch (Exception e) {
                    log.warn("Failed to obtain messages from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
            log.info("Tb Core Consumer stopped.");
        });
    }

    @Scheduled(fixedDelayString = "${queue.core.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
        }
    }

    @PreDestroy
    public void destroy() {
        stopped = true;
        if (consumer != null) {
            consumer.unsubscribe();
        }
        if (mainConsumerExecutor != null) {
            mainConsumerExecutor.shutdownNow();
        }
    }

    private void forwardToLocalSubMgrService(TransportProtos.LocalSubscriptionServiceMsgProto msg, TbMsgCallback callback) {
        if (msg.hasSubUpdate()) {
            localSubscriptionService.onSubscriptionUpdate(msg.getSubUpdate().getSessionId(), TbSubscriptionUtils.fromProto(msg.getSubUpdate()), callback);
        } else {
            throwNotHandled(msg, callback);
        }
    }

    private void forwardToSubMgrService(TransportProtos.SubscriptionMgrMsgProto msg, TbMsgCallback callback) {
        if (msg.hasAttributeSub()) {
            subscriptionManagerService.addSubscription(TbSubscriptionUtils.fromProto(msg.getAttributeSub()), callback);
        } else if (msg.hasTelemetrySub()) {
            subscriptionManagerService.addSubscription(TbSubscriptionUtils.fromProto(msg.getTelemetrySub()), callback);
        } else if (msg.hasSubClose()) {
            TransportProtos.TbSubscriptionCloseProto closeProto = msg.getSubClose();
            subscriptionManagerService.cancelSubscription(closeProto.getSessionId(), closeProto.getSubscriptionId(), callback);
        } else if (msg.hasTsUpdate()) {
            TransportProtos.TbTimeSeriesUpdateProto proto = msg.getTsUpdate();
            subscriptionManagerService.onTimeSeriesUpdate(
                    new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    TbSubscriptionUtils.toTsKvEntityList(proto.getDataList()), callback);
        } else if (msg.hasAttrUpdate()) {
            TransportProtos.TbAttributeUpdateProto proto = msg.getAttrUpdate();
            subscriptionManagerService.onAttributesUpdate(
                    new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), TbSubscriptionUtils.toAttributeKvList(proto.getDataList()), callback);
        } else {
            throwNotHandled(msg, callback);
        }
    }

    private void forwardToStateService(TransportProtos.DeviceStateServiceMsgProto deviceStateServiceMsg, TbMsgCallback callback) {
        if (statsEnabled) {
            stats.log(deviceStateServiceMsg);
        }
        stateService.onQueueMsg(deviceStateServiceMsg, callback);
    }

    private void forwardToDeviceActor(TransportToDeviceActorMsg toDeviceActorMsg, TbMsgCallback callback) {
        if (statsEnabled) {
            stats.log(toDeviceActorMsg);
        }
        actorContext.getAppActor().tell(new TransportToDeviceActorMsgWrapper(toDeviceActorMsg, callback), ActorRef.noSender());
    }

    private void throwNotHandled(Object msg, TbMsgCallback callback) {
        log.warn("Message not handled: {}", msg);
        callback.onFailure(new RuntimeException("Message not handled!"));
    }
}
