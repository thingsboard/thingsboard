/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos.EdgeNotificationMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
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

    private final TbCoreQueueFactory queueFactory;
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
    private final TenantEdgeProcessor tenantEdgeProcessor;
    private final TenantProfileEdgeProcessor tenantProfileEdgeProcessor;
    private final AlarmEdgeProcessor alarmProcessor;
    private final RelationEdgeProcessor relationProcessor;
    private final EdgeConsumerStats stats;

    private volatile ListeningExecutorService consumerExecutor;
    private volatile TbQueueConsumer<TbProtoQueueMsg<ToEdgeMsg>> consumer;
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

    public DefaultTbEdgeConsumerService(TbCoreQueueFactory queueFactory, StatsFactory statsFactory,
                                        EdgeProcessor edgeProcessor, QueueEdgeProcessor queueProcessor,
                                        AssetEdgeProcessor assetProcessor, DeviceEdgeProcessor deviceProcessor,
                                        EntityViewEdgeProcessor entityViewProcessor, DashboardEdgeProcessor dashboardProcessor,
                                        RuleChainEdgeProcessor ruleChainProcessor, UserEdgeProcessor userProcessor,
                                        CustomerEdgeProcessor customerProcessor, OtaPackageEdgeProcessor otaPackageProcessor,
                                        DeviceProfileEdgeProcessor deviceProfileProcessor, AssetProfileEdgeProcessor assetProfileProcessor,
                                        WidgetBundleEdgeProcessor widgetBundleProcessor, WidgetTypeEdgeProcessor widgetTypeProcessor,
                                        TenantEdgeProcessor tenantEdgeProcessor, TenantProfileEdgeProcessor tenantProfileEdgeProcessor,
                                        AlarmEdgeProcessor alarmProcessor, RelationEdgeProcessor relationProcessor) {
        this.queueFactory = queueFactory;
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
        this.tenantEdgeProcessor = tenantEdgeProcessor;
        this.tenantProfileEdgeProcessor = tenantProfileEdgeProcessor;
        this.alarmProcessor = alarmProcessor;
        this.relationProcessor = relationProcessor;
        this.stats = new EdgeConsumerStats(statsFactory);
    }

    @PostConstruct
    public void initExecutor() {
        consumerExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(edgeDispatcherSize, "edge-notification-consumer"));
        consumer = queueFactory.createEdgeMsgConsumer();
    }

    @PreDestroy
    public void shutdownExecutor() {
        stopped = true;
        if (consumer != null) {
            consumer.unsubscribe();
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }


    @Override
    public void onApplicationEvent(PartitionChangeEvent event) {
        if (event.getServiceType().equals(ServiceType.TB_EDGE)) {
            log.info("Subscribing to partitions: {}", event.getPartitions());
            consumer.subscribe(event.getPartitions());
        }
    }


    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 2)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        consumerExecutor.execute(this::launchEdgeConsumer);
    }

    protected void launchEdgeConsumer() {
        while (!stopped && !consumer.isStopped()) {
            try {
                List<TbProtoQueueMsg<ToEdgeMsg>> msgs = consumer.poll(pollDuration);
                if (msgs.isEmpty()) {
                    continue;
                }
                processMsgs(msgs);
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
        }
        log.info("TB Edge request consumer stopped.");
    }

    private void processMsgs(List<TbProtoQueueMsg<ToEdgeMsg>> msgs) throws InterruptedException {
        List<IdMsgPair<ToEdgeMsg>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
        ConcurrentMap<UUID, TbProtoQueueMsg<ToEdgeMsg>> pendingMap = orderedMsgList.stream().collect(
                Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
        int retryCount = 0;
        while (!stopped && !consumer.isStopped()) {
            CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
            TbPackProcessingContext<TbProtoQueueMsg<ToEdgeMsg>> ctx = new TbPackProcessingContext<>(
                    processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
            PendingMsgHolder pendingMsgHolder = new PendingMsgHolder();
            Future<?> submitFuture = consumerExecutor.submit(() ->
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
                ctx.getAckMap().forEach((id, msg) -> log.debug("[{}] Timeout to process message: {}", id, msg.getValue()));
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
        consumer.commit();
    }

    private void pushNotificationToEdge(EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
        log.debug("[{}] Pushing notification to edge {}", tenantId, edgeNotificationMsg);
        try {
            EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
            switch (type) {
                case EDGE:
                    edgeProcessor.processEdgeNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET:
                    assetProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE:
                    deviceProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ENTITY_VIEW:
                    entityViewProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case DASHBOARD:
                    dashboardProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case RULE_CHAIN:
                    ruleChainProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                    userProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case CUSTOMER:
                    customerProcessor.processCustomerNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE_PROFILE:
                    deviceProfileProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET_PROFILE:
                    assetProfileProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case OTA_PACKAGE:
                    otaPackageProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGETS_BUNDLE:
                    widgetBundleProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGET_TYPE:
                    widgetTypeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case QUEUE:
                    queueProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ALARM:
                    alarmProcessor.processAlarmNotification(tenantId, edgeNotificationMsg);
                    break;
                case RELATION:
                    relationProcessor.processRelationNotification(tenantId, edgeNotificationMsg);
                    break;
                case TENANT:
                    tenantEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case TENANT_PROFILE:
                    tenantProfileEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                default:
                    log.warn("[{}] Edge event type [{}] is not designed to be pushed to edge", tenantId, type);
            }
            callback.onSuccess();
        } catch (Exception e) {
            callBackFailure(tenantId, edgeNotificationMsg, callback, e);
        }
    }

    @Scheduled(fixedDelayString = "${queue.edge.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
            stats.reset();
        }
    }

    private void callBackFailure(TenantId tenantId, EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback, Throwable throwable) {
        log.error("[{}] Can't push to edge updates, edgeNotificationMsg [{}]", tenantId, edgeNotificationMsg, throwable);
        callback.onFailure(throwable);
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
