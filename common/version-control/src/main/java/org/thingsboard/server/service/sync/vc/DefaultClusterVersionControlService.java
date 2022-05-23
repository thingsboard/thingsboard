/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.vc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbVersionControlQueueFactory;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbVersionControlComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@TbVersionControlComponent
@Service
@RequiredArgsConstructor
public class DefaultClusterVersionControlService extends TbApplicationEventListener<PartitionChangeEvent> implements ClusterVersionControlService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbVersionControlQueueFactory queueFactory;
    private final DataDecodingEncodingService encodingService;
    private final GitRepositoryService vcService;
    private final NotificationsTopicService notificationsTopicService;

    private volatile ExecutorService consumerExecutor;
    private volatile TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> consumer;
    private volatile TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> producer;
    private volatile boolean stopped = false;

    @Value("${queue.vc.poll-interval:25}")
    private long pollDuration;
    @Value("${queue.vc.pack-processing-timeout:60000}")
    private long packProcessingTimeout;

    @PostConstruct
    public void init() {
        consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("vc-consumer"));
        producer = queueFactory.createTbCoreNotificationsMsgProducer();
        consumer = queueFactory.createToVersionControlMsgConsumer();
    }

    @PreDestroy
    public void stop() {
        stopped = true;
        if (consumer != null) {
            consumer.unsubscribe();
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        //TODO: cleanup repositories that we no longer manage in this node.
        consumer.subscribe(event.getPartitions());
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 2)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        consumerExecutor.execute(() -> consumerLoop(consumer));
    }

    void consumerLoop(TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> consumer) {
        while (!stopped && !consumer.isStopped()) {
            try {
                List<TbProtoQueueMsg<ToVersionControlServiceMsg>> msgs = consumer.poll(pollDuration);
                if (msgs.isEmpty()) {
                    continue;
                }
                for (TbProtoQueueMsg<ToVersionControlServiceMsg> msgWrapper : msgs) {
                    ToVersionControlServiceMsg msg = msgWrapper.getValue();
                    if (msg.hasClearRepositoryRequest()) {
                        handleClearRepositoryCommand(new VersionControlRequestCtx(msg, null));
                    } else {
                        VersionControlRequestCtx ctx = new VersionControlRequestCtx(msg, getEntitiesVersionControlSettings(msg));
                        if (msg.hasTestRepositoryRequest()) {
                            handleTestRepositoryCommand(ctx);
                        } else if (msg.hasInitRepositoryRequest()) {
                            handleInitRepositoryCommand(ctx);
                        }
                    }
                }
//                ConcurrentMap<UUID, TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> pendingMap = msgs.stream().collect(
//                        Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
//                CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
//                TbPackProcessingContext<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> ctx = new TbPackProcessingContext<>(
//                        processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
//                pendingMap.forEach((id, msg) -> {
//                    log.trace("[{}] Creating downlink callback for message: {}", id, msg.getValue());
//                    TbCallback callback = new TbPackCallback<>(id, ctx);
//                    try {
//                        handleDownlink(id, msg, callback);
//                    } catch (Throwable e) {
//                        log.warn("[{}] Failed to process notification: {}", id, msg, e);
//                        callback.onFailure(e);
//                    }
//                });
//                if (!processingTimeoutLatch.await(processingTimeout, TimeUnit.MILLISECONDS)) {
//                    ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process downlink: {}", id, msg.getValue()));
//                    ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process downlink: {}", id, msg.getValue()));
//                }
                consumer.commit();
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("Failed to obtain version control requests from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new version control messages", e2);
                    }
                }
            }
        }
        log.info("TB Version Control request consumer stopped.");
    }

    private void handleClearRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.clearRepository(ctx.getTenantId());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }

    private void handleInitRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.initRepository(ctx.getTenantId(), ctx.getSettings());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }


    private void handleTestRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.testRepository(ctx.getTenantId(), ctx.getSettings());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }

    private void reply(VersionControlRequestCtx ctx, Optional<Exception> e) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, ctx.getNodeId());
        TransportProtos.VersionControlResponseMsg.Builder builder = TransportProtos.VersionControlResponseMsg.newBuilder()
                .setRequestIdMSB(ctx.getRequestId().getMostSignificantBits())
                .setRequestIdLSB(ctx.getRequestId().getLeastSignificantBits());
        if (e.isPresent()) {
            builder.setError(e.get().getMessage());
        } else {
            builder.setGenericResponse(TransportProtos.GenericRepositoryResponseMsg.newBuilder().build());
        }
        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setVcResponseMsg(builder).build();
        log.trace("PUSHING msg: {} to: {}", msg, tpi);
        producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);
    }

    private EntitiesVersionControlSettings getEntitiesVersionControlSettings(ToVersionControlServiceMsg msg) {
        Optional<EntitiesVersionControlSettings> settingsOpt = encodingService.decode(msg.getVcSettings().toByteArray());
        if (settingsOpt.isPresent()) {
            return settingsOpt.get();
        } else {
            log.warn("Failed to parse VC settings: {}", msg.getVcSettings());
            throw new RuntimeException("Failed to parse vc settings!");
        }
    }

}
