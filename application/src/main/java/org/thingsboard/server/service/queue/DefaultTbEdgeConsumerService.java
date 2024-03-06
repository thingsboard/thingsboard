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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.EdgeNotificationMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;
import org.thingsboard.server.service.edge.stats.EdgeConsumerStats;
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
public class DefaultTbEdgeConsumerService implements TbEdgeConsumerService {

    public static final String EDGE_IS_ROOT_BODY_KEY = "isRoot";

    private final ActorSystemContext actorContext;
    private final TbQueueConsumer<TbProtoQueueMsg<ToEdgeMsg>> mainConsumer;
    private final EdgeProcessor edgeProcessor;
    private final AssetEdgeProcessor assetProcessor;
    private final DeviceEdgeProcessor deviceProcessor;
    private final EntityViewEdgeProcessor entityViewProcessor;
    private final DashboardEdgeProcessor dashboardProcessor;
    private final RuleChainEdgeProcessor ruleChainProcessor;
    private final UserEdgeProcessor userProcessor;
    private final CustomerEdgeProcessor customerProcessor;
    private final DeviceProfileEdgeProcessor deviceProfileProcessor;
    private final AssetProfileEdgeProcessor assetProfileProcessor;
    private final OtaPackageEdgeProcessor otaPackageProcessor;
    private final WidgetBundleEdgeProcessor widgetBundleProcessor;
    private final WidgetTypeEdgeProcessor widgetTypeProcessor;
    private final QueueEdgeProcessor queueProcessor;
    private final TenantEdgeProcessor tenantProcessor;
    private final TenantProfileEdgeProcessor tenantProfileProcessor;
    private final AlarmEdgeProcessor alarmProcessor;
    private final RelationEdgeProcessor relationProcessor;
    private final ResourceEdgeProcessor resourceProcessor;
    private final EdgeConsumerStats stats;

    protected volatile ListeningExecutorService consumersExecutor;
    private volatile boolean stopped = false;

    @Value("${actors.system.edge_dispatcher_pool_size:4}")
    private int edgeDispatcherSize;
    @Value("${queue.edge.pool_interval:25}")
    private int pollDuration;
    @Value("${queue.edge.pack_processing_timeout:10000}")
    private int packProcessingTimeout;
    @Value("${queue.edge.pack_processing_retries:10}")
    private int packProcessingRetries;
    @Value("${queue.edge.stats.enabled:false}")
    private boolean statsEnabled;

    public DefaultTbEdgeConsumerService(TbCoreQueueFactory tbCoreQueueFactory,
                                        ActorSystemContext actorContext, StatsFactory statsFactory,
                                        EdgeProcessor edgeProcessor, QueueEdgeProcessor queueProcessor,
                                        AssetEdgeProcessor assetProcessor, DeviceEdgeProcessor deviceProcessor,
                                        EntityViewEdgeProcessor entityViewProcessor, DashboardEdgeProcessor dashboardProcessor,
                                        RuleChainEdgeProcessor ruleChainProcessor, UserEdgeProcessor userProcessor,
                                        CustomerEdgeProcessor customerProcessor, OtaPackageEdgeProcessor otaPackageProcessor,
                                        DeviceProfileEdgeProcessor deviceProfileProcessor, AssetProfileEdgeProcessor assetProfileProcessor,
                                        WidgetBundleEdgeProcessor widgetBundleProcessor, WidgetTypeEdgeProcessor widgetTypeProcessor,
                                        TenantEdgeProcessor tenantProcessor, TenantProfileEdgeProcessor tenantProfileProcessor,
                                        AlarmEdgeProcessor alarmProcessor, RelationEdgeProcessor relationProcessor, ResourceEdgeProcessor resourceProcessor) {
        this.mainConsumer = tbCoreQueueFactory.createEdgeMsgConsumer();
        this.actorContext = actorContext;
        this.edgeProcessor = edgeProcessor;
        this.assetProcessor = assetProcessor;
        this.deviceProcessor = deviceProcessor;
        this.entityViewProcessor = entityViewProcessor;
        this.dashboardProcessor = dashboardProcessor;
        this.ruleChainProcessor = ruleChainProcessor;
        this.userProcessor = userProcessor;
        this.customerProcessor = customerProcessor;
        this.deviceProfileProcessor = deviceProfileProcessor;
        this.assetProfileProcessor = assetProfileProcessor;
        this.otaPackageProcessor = otaPackageProcessor;
        this.widgetBundleProcessor = widgetBundleProcessor;
        this.widgetTypeProcessor = widgetTypeProcessor;
        this.queueProcessor = queueProcessor;
        this.tenantProcessor = tenantProcessor;
        this.tenantProfileProcessor = tenantProfileProcessor;
        this.alarmProcessor = alarmProcessor;
        this.relationProcessor = relationProcessor;
        this.resourceProcessor = resourceProcessor;
        this.stats = new EdgeConsumerStats(statsFactory);
    }

    @PostConstruct
    public void initExecutor() {
        this.consumersExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(edgeDispatcherSize, "tb-edge-consumer"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        stopped = true;
        if (consumersExecutor != null) {
            consumersExecutor.shutdown();
        }
    }

    @Override
    public void onApplicationEvent(PartitionChangeEvent event) {
        if (event.getServiceType().equals(ServiceType.TB_CORE)) {
            log.info("Subscribing to partitions: {}", event.getPartitions());
            mainConsumer.subscribe(event.getPartitions());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 2)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        consumersExecutor.submit(this::launchEdgeConsumer);
    }

    protected void launchEdgeConsumer() {
        consumersExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToEdgeMsg>> msgs = mainConsumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    List<IdMsgPair<ToEdgeMsg>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToEdgeMsg>> pendingMap = orderedMsgList.stream().collect(
                            Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
                    int retryCount = 0;
                    while (!stopped && !consumersExecutor.isShutdown()) {
                        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                        TbPackProcessingContext<TbProtoQueueMsg<ToEdgeMsg>> ctx = new TbPackProcessingContext<>(
                                processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                        PendingMsgHolder pendingMsgHolder = new PendingMsgHolder();
                        Future<?> submitFuture = consumersExecutor.submit(() ->
                                orderedMsgList.forEach((element) -> {
                                    UUID id = element.getUuid();
                                    TbProtoQueueMsg<ToEdgeMsg> msg = element.getMsg();
                                    TbCallback callback = new TbPackCallback<>(id, ctx);
                                    try {
                                        ToEdgeMsg toEdgeMsg = msg.getValue();
                                        pendingMsgHolder.setToEdgeMsg(toEdgeMsg);
                                        if (toEdgeMsg.hasEdgeNotificationMsg()) {
                                            pushNotificationToEdge(toEdgeMsg.getEdgeNotificationMsg(), callback);
                                        }
                                        if (toEdgeMsg.hasEdgeEventUpdate()) {
                                            forwardToAppActor(id, ProtoUtils.fromProto(toEdgeMsg.getEdgeEventUpdate()));
                                        }
                                        if (toEdgeMsg.hasToEdgeSyncRequest()) {
                                            forwardToAppActor(id, ProtoUtils.fromProto(toEdgeMsg.getEdgeEventUpdate()));
                                        }
                                        if (toEdgeMsg.hasFromEdgeSyncResponse()) {
                                            forwardToAppActor(id, ProtoUtils.fromProto(toEdgeMsg.getEdgeEventUpdate()));
                                        }
                                        logToEdgeMsg(toEdgeMsg);
                                    } catch (Throwable e) {
                                        log.warn("[{}] Failed to process message: {}", id, msg, e);
                                        callback.onFailure(e);
                                    }
                                }));
                        if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
                            if (!submitFuture.isDone()) {
                                submitFuture.cancel(true);
                                ToEdgeMsg lastSubmitMsg = pendingMsgHolder.getToEdgeMsg();
                                log.info("Timeout to process message: {}", lastSubmitMsg);
                            }
                            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
                        } else {
                            break;
                        }
                        if (!ctx.getFailedMap().isEmpty() && retryCount != packProcessingRetries) {
                            pendingMap = ctx.getFailedMap();
                            retryCount++;
                        } else {
                            break;
                        }
                    }
                    mainConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain messages from queue.", e);
                        try {
                            Thread.sleep(pollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                        }
                    }
                }
                log.info("TB Edge request consumer stopped.");
            }
        });
    }

    private void pushNotificationToEdge(EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
        log.debug("[{}] Pushing notification to edge {}", tenantId, edgeNotificationMsg);
        try {
            EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
            ListenableFuture<Void> future;
            switch (type) {
                case EDGE:
                    future = edgeProcessor.processEdgeNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET:
                    future = assetProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET_PROFILE:
                    future = assetProfileProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE:
                    future = deviceProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE_PROFILE:
                    future = deviceProfileProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ENTITY_VIEW:
                    future = entityViewProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case DASHBOARD:
                    future = dashboardProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case RULE_CHAIN:
                    future = ruleChainProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                    future = userProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case CUSTOMER:
                    future = customerProcessor.processCustomerNotification(tenantId, edgeNotificationMsg);
                    break;
                case OTA_PACKAGE:
                    future = otaPackageProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGETS_BUNDLE:
                    future = widgetBundleProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGET_TYPE:
                    future = widgetTypeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case QUEUE:
                    future = queueProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ALARM:
                    future = alarmProcessor.processAlarmNotification(tenantId, edgeNotificationMsg);
                    break;
                case ALARM_COMMENT:
                    future = alarmProcessor.processAlarmCommentNotification(tenantId, edgeNotificationMsg);
                    break;
                case RELATION:
                    future = relationProcessor.processRelationNotification(tenantId, edgeNotificationMsg);
                    break;
                case TENANT:
                    future = tenantProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case TENANT_PROFILE:
                    future = tenantProfileProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case TB_RESOURCE:
                    future = resourceProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                default:
                    future = Futures.immediateFuture(null);
                    log.warn("[{}] Edge event type [{}] is not designed to be pushed to edge", tenantId, type);
            }
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void unused) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    callBackFailure(tenantId, edgeNotificationMsg, callback, throwable);
                }
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            callBackFailure(tenantId, edgeNotificationMsg, callback, e);
        }
    }

    private void callBackFailure(TenantId tenantId, EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback, Throwable throwable) {
        log.error("[{}] Can't push to edge updates, edgeNotificationMsg [{}]", tenantId, edgeNotificationMsg, throwable);
        callback.onFailure(throwable);
    }

    private void forwardToAppActor(UUID id, TbActorMsg actorMsg) {
        log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg);
        actorContext.tell(actorMsg);
    }

    @Scheduled(fixedDelayString = "${queue.edge.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
            stats.reset();
        }
    }

    private void logToEdgeMsg(ToEdgeMsg toEdgeMsg) {
        if (statsEnabled) {
            stats.log(toEdgeMsg);
        }
    }

    private static class PendingMsgHolder {
        @Getter
        @Setter
        private volatile ToEdgeMsg toEdgeMsg;
    }
}
