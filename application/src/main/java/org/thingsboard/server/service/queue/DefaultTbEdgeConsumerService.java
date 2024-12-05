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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.edge.EdgeSessionMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.EdgeNotificationMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.queue.consumer.MainQueueConsumerManager;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.processing.IdMsgPair;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
public class DefaultTbEdgeConsumerService extends AbstractConsumerService<ToEdgeNotificationMsg> implements TbEdgeConsumerService {

    @Value("${queue.edge.pool-interval:25}")
    private int pollInterval;
    @Value("${queue.edge.pack-processing-timeout:10000}")
    private int packProcessingTimeout;
    @Value("${queue.edge.consumer-per-partition:false}")
    private boolean consumerPerPartition;
    @Value("${queue.edge.pack-processing-retries:3}")
    private int packProcessingRetries;
    @Value("${queue.edge.stats.enabled:false}")
    private boolean statsEnabled;

    private final TbCoreQueueFactory queueFactory;
    private final EdgeContextComponent edgeCtx;
    private final EdgeConsumerStats stats;

    private MainQueueConsumerManager<TbProtoQueueMsg<ToEdgeMsg>, EdgeQueueConfig> mainConsumer;

    public DefaultTbEdgeConsumerService(TbCoreQueueFactory tbCoreQueueFactory, ActorSystemContext actorContext,
                                        StatsFactory statsFactory, EdgeContextComponent edgeCtx) {
        super(actorContext, null, null, null, null, null,
                null, null);
        this.edgeCtx = edgeCtx;
        this.stats = new EdgeConsumerStats(statsFactory);
        this.queueFactory = tbCoreQueueFactory;
    }

    @PostConstruct
    public void init() {
        super.init("tb-edge");

        this.mainConsumer = MainQueueConsumerManager.<TbProtoQueueMsg<ToEdgeMsg>, EdgeQueueConfig>builder()
                .queueKey(new QueueKey(ServiceType.TB_CORE).withQueueName(DataConstants.EDGE_QUEUE_NAME))
                .config(EdgeQueueConfig.of(consumerPerPartition, pollInterval))
                .msgPackProcessor(this::processMsgs)
                .consumerCreator((config, partitionId) -> queueFactory.createEdgeMsgConsumer())
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .build();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void startConsumers() {
        super.startConsumers();
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        var partitions = event.getEdgePartitions();
        log.debug("Subscribing to partitions: {}", partitions);
        mainConsumer.update(partitions);
    }

    private void processMsgs(List<TbProtoQueueMsg<ToEdgeMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToEdgeMsg>> consumer, EdgeQueueConfig edgeQueueConfig) throws InterruptedException {
        List<IdMsgPair<ToEdgeMsg>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).toList();
        ConcurrentMap<UUID, TbProtoQueueMsg<ToEdgeMsg>> pendingMap = orderedMsgList.stream().collect(
                Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
        TbPackProcessingContext<TbProtoQueueMsg<ToEdgeMsg>> ctx = new TbPackProcessingContext<>(
                processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
        PendingMsgHolder pendingMsgHolder = new PendingMsgHolder();
        Future<?> submitFuture = consumersExecutor.submit(() -> {
            orderedMsgList.forEach((element) -> {
                UUID id = element.getUuid();
                TbProtoQueueMsg<ToEdgeMsg> msg = element.getMsg();
                TbCallback callback = new TbPackCallback<>(id, ctx);
                try {
                    ToEdgeMsg toEdgeMsg = msg.getValue();
                    pendingMsgHolder.setToEdgeMsg(toEdgeMsg);
                    if (toEdgeMsg.hasEdgeNotificationMsg()) {
                        pushNotificationToEdge(toEdgeMsg.getEdgeNotificationMsg(), 0, packProcessingRetries, callback);
                    }
                    if (statsEnabled) {
                        stats.log(toEdgeMsg);
                    }
                } catch (Throwable e) {
                    log.warn("[{}] Failed to process message: {}", id, msg, e);
                    callback.onFailure(e);
                }
            });
        });
        if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
            if (!submitFuture.isDone()) {
                submitFuture.cancel(true);
                ToEdgeMsg lastSubmitMsg = pendingMsgHolder.getToEdgeMsg();
                log.info("Timeout to process message: {}", lastSubmitMsg);
            }
            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
        }
        consumer.commit();
    }

    private static class PendingMsgHolder {
        @Getter
        @Setter
        private volatile ToEdgeMsg toEdgeMsg;
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollInterval;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected int getMgmtThreadPoolSize() {
        return Math.max(Runtime.getRuntime().availableProcessors(), 4);
    }

    @Override
    protected TbQueueConsumer<TbProtoQueueMsg<ToEdgeNotificationMsg>> createNotificationsConsumer() {
        return queueFactory.createToEdgeNotificationsMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToEdgeNotificationMsg> msg, TbCallback callback) {
        ToEdgeNotificationMsg toEdgeNotificationMsg = msg.getValue();
        try {
            if (toEdgeNotificationMsg.hasEdgeHighPriority()) {
                EdgeSessionMsg edgeSessionMsg = ProtoUtils.fromProto(toEdgeNotificationMsg.getEdgeHighPriority());
                edgeCtx.getEdgeRpcService().onToEdgeSessionMsg(edgeSessionMsg.getTenantId(), edgeSessionMsg);
                callback.onSuccess();
            } else if (toEdgeNotificationMsg.hasEdgeEventUpdate()) {
                EdgeSessionMsg edgeSessionMsg = ProtoUtils.fromProto(toEdgeNotificationMsg.getEdgeEventUpdate());
                edgeCtx.getEdgeRpcService().onToEdgeSessionMsg(edgeSessionMsg.getTenantId(), edgeSessionMsg);
                callback.onSuccess();
            } else if (toEdgeNotificationMsg.hasToEdgeSyncRequest()) {
                EdgeSessionMsg edgeSessionMsg = ProtoUtils.fromProto(toEdgeNotificationMsg.getToEdgeSyncRequest());
                edgeCtx.getEdgeRpcService().onToEdgeSessionMsg(edgeSessionMsg.getTenantId(), edgeSessionMsg);
                callback.onSuccess();
            } else if (toEdgeNotificationMsg.hasFromEdgeSyncResponse()) {
                EdgeSessionMsg edgeSessionMsg = ProtoUtils.fromProto(toEdgeNotificationMsg.getFromEdgeSyncResponse());
                edgeCtx.getEdgeRpcService().onToEdgeSessionMsg(edgeSessionMsg.getTenantId(), edgeSessionMsg);
                callback.onSuccess();
            } else if (toEdgeNotificationMsg.hasComponentLifecycle()) {
                ComponentLifecycleMsg componentLifecycle = ProtoUtils.fromProto(toEdgeNotificationMsg.getComponentLifecycle());
                TenantId tenantId = componentLifecycle.getTenantId();
                EdgeId edgeId = new EdgeId(componentLifecycle.getEntityId().getId());
                if (ComponentLifecycleEvent.DELETED.equals(componentLifecycle.getEvent())) {
                    edgeCtx.getEdgeRpcService().deleteEdge(tenantId, edgeId);
                } else if (ComponentLifecycleEvent.UPDATED.equals(componentLifecycle.getEvent())) {
                    Edge edge = edgeCtx.getEdgeService().findEdgeById(tenantId, edgeId);
                    edgeCtx.getEdgeRpcService().updateEdge(tenantId, edge);
                }
                callback.onSuccess();
            }
        } catch (Exception e) {
            log.error("Error processing edge notification message", e);
            callback.onFailure(e);
        }

        if (statsEnabled) {
            stats.log(msg.getValue());
        }
    }

    private void pushNotificationToEdge(EdgeNotificationMsgProto edgeNotificationMsg, int retryCount, int retryLimit, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
        log.debug("[{}] Pushing notification to edge {}", tenantId, edgeNotificationMsg);
        try {
            EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
            ListenableFuture<Void> future;
            switch (type) {
                case EDGE -> future = edgeCtx.getEdgeProcessor().processEdgeNotification(tenantId, edgeNotificationMsg);
                case ASSET -> future = edgeCtx.getAssetProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case ASSET_PROFILE -> future = edgeCtx.getAssetProfileProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case DEVICE -> future = edgeCtx.getDeviceProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case DEVICE_PROFILE -> future = edgeCtx.getDeviceProfileProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case ENTITY_VIEW -> future = edgeCtx.getEntityViewProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case DASHBOARD -> future = edgeCtx.getDashboardProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case RULE_CHAIN -> future = edgeCtx.getRuleChainProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case USER -> future = edgeCtx.getUserProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case CUSTOMER -> future = edgeCtx.getCustomerProcessor().processCustomerNotification(tenantId, edgeNotificationMsg);
                case OTA_PACKAGE -> future = edgeCtx.getOtaPackageProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case WIDGETS_BUNDLE -> future = edgeCtx.getWidgetBundleProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case WIDGET_TYPE -> future = edgeCtx.getWidgetTypeProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case QUEUE -> future = edgeCtx.getQueueProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case ALARM -> future = edgeCtx.getAlarmProcessor().processAlarmNotification(tenantId, edgeNotificationMsg);
                case ALARM_COMMENT -> future = edgeCtx.getAlarmProcessor().processAlarmCommentNotification(tenantId, edgeNotificationMsg);
                case RELATION -> future = edgeCtx.getRelationProcessor().processRelationNotification(tenantId, edgeNotificationMsg);
                case TENANT -> future = edgeCtx.getTenantProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case TENANT_PROFILE -> future = edgeCtx.getTenantProfileProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case NOTIFICATION_RULE, NOTIFICATION_TARGET, NOTIFICATION_TEMPLATE ->
                        future = edgeCtx.getNotificationEdgeProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case TB_RESOURCE -> future = edgeCtx.getResourceProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                case DOMAIN, OAUTH2_CLIENT -> future = edgeCtx.getOAuth2EdgeProcessor().processEntityNotification(tenantId, edgeNotificationMsg);
                default -> {
                    future = Futures.immediateFuture(null);
                    log.warn("[{}] Edge event type [{}] is not designed to be pushed to edge", tenantId, type);
                }
            }
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void unused) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(@NotNull Throwable throwable) {
                    if (retryCount < retryLimit) {
                        log.warn("[{}] Retry {} for message due to failure: {}", tenantId, retryCount + 1, throwable.getMessage());
                        pushNotificationToEdge(edgeNotificationMsg, retryCount + 1, retryLimit, callback);
                    } else {
                        callBackFailure(tenantId, edgeNotificationMsg, callback, throwable);
                    }
                }
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            if (retryCount < retryLimit) {
                log.warn("[{}] Retry {} for message due to exception: {}", tenantId, retryCount + 1, e.getMessage());
                pushNotificationToEdge(edgeNotificationMsg, retryCount + 1, retryLimit, callback);
            } else {
                callBackFailure(tenantId, edgeNotificationMsg, callback, e);
            }
        }
    }

    private void callBackFailure(TenantId tenantId, EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback, Throwable throwable) {
        log.error("[{}] Can't push to edge updates, edgeNotificationMsg [{}]", tenantId, edgeNotificationMsg, throwable);
        callback.onFailure(throwable);
    }

    @Scheduled(fixedDelayString = "${queue.edge.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
            stats.reset();
        }
    }

    @Override
    protected void stopConsumers() {
        super.stopConsumers();
        mainConsumer.stop();
        mainConsumer.awaitStop();
    }

    @Data(staticConstructor = "of")
    public static class EdgeQueueConfig implements QueueConfig {
        private final boolean consumerPerPartition;
        private final int pollInterval;
    }

}
