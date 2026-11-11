// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreIntegrationExecutorQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;
import org.thingsboard.server.queue.util.TbPackCallback;
import org.thingsboard.server.queue.util.TbPackProcessingContext;
import org.thingsboard.server.service.cache.IntegrationExecutorCacheStartupService;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@TbCoreOrIntegrationExecutorComponent
@Service
public class DefaultClusterIntegrationService extends TbApplicationEventListener<PartitionChangeEvent> implements ClusterIntegrationService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueIntegrationExecutorSettings integrationNotificationSettings;
    private final TbCoreIntegrationExecutorQueueFactory queueFactory;
    private final IntegrationManagerService integrationManagerService;

    @Autowired(required = false)
    private IntegrationExecutorCacheStartupService cacheStartupService;

    private final Map<IntegrationType, Queue<Set<TopicPartitionInfo>>> subscribeEventsMap = new ConcurrentHashMap<>();

    public DefaultClusterIntegrationService(TbServiceInfoProvider serviceInfoProvider,
                                            TbQueueIntegrationExecutorSettings integrationNotificationSettings,
                                            TbCoreIntegrationExecutorQueueFactory queueFactory,
                                            IntegrationManagerService integrationManagerService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.integrationNotificationSettings = integrationNotificationSettings;
        this.queueFactory = queueFactory;
        this.integrationManagerService = integrationManagerService;
    }

    private volatile ExecutorService consumersExecutor;
    private volatile ExecutorService notificationsConsumerExecutor;
    private volatile ExecutorService startupExecutor;
    private volatile ListeningScheduledExecutorService queueExecutor;
    private final ConcurrentMap<IntegrationType, TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>>> consumers = new ConcurrentHashMap<>();
    private volatile TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> nfConsumer;

    private volatile boolean stopped = false;
    private volatile List<IntegrationType> supportedIntegrationTypes;

    private volatile boolean started = false;
    private final Queue<PartitionChangeEvent> pendingEvents = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        supportedIntegrationTypes = serviceInfoProvider.getSupportedIntegrationTypes();
        queueExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newSingleThreadScheduledExecutor("scheduler-service"));
        notificationsConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ie-nf-consumer"));
        consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("ie-downlink-consumer"));
        startupExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ie-startup"));
        nfConsumer = queueFactory.createToIntegrationExecutorNotificationsMsgConsumer();
        for (IntegrationType integrationType : supportedIntegrationTypes) {
            consumers.computeIfAbsent(integrationType, queueName -> queueFactory.createToIntegrationExecutorDownlinkMsgConsumer(integrationType));
        }
    }

    @PreDestroy
    public void stop() {
        stopped = true;
        if (nfConsumer != null) {
            nfConsumer.unsubscribe();
        }
        consumers.values().forEach(TbQueueConsumer::unsubscribe);
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
        if (notificationsConsumerExecutor != null) {
            notificationsConsumerExecutor.shutdownNow();
        }
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (startupExecutor != null) {
            startupExecutor.shutdownNow();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        log.debug("Received partition change event: {}", event);
        if (!started) {
            // Add to queue FIRST, then check again
            pendingEvents.add(event);
            if (!started) {
                log.debug("Integration Executor not started yet, event queued for later: {}", event);
                return; // doStartup() will drain the queue
            }
            // started became true after we added - fall through to drain
        }
        // Either already started or just became started - drain and process the latest pending
        drainPendingEvents();
    }

    private void drainPendingEvents() {
        // Each PartitionChangeEvent carries a full partition snapshot for TB_INTEGRATION_EXECUTOR,
        // so earlier pending events are superseded — discard them and process only the latest.
        PartitionChangeEvent latest = null;
        PartitionChangeEvent event;
        while ((event = pendingEvents.poll()) != null) {
            latest = event;
        }
        if (latest == null) {
            return;
        }
        try {
            processPartitionChangeEvent(latest);
        } catch (Throwable t) {
            log.error("Failed to handle partition change event: {}", latest, t);
        }
    }

    private void processPartitionChangeEvent(PartitionChangeEvent event) {
        event.getNewPartitions().forEach(((queueKey, partitions) -> {
            IntegrationType type = IntegrationType.valueOf(queueKey.getQueueName());
            subscribeEventsMap.computeIfAbsent(type, t -> new ConcurrentLinkedQueue<>()).add(partitions);
            queueExecutor.submit(() -> refreshIntegrationsByType(type));
            var consumer = consumers.get(type);
            if (consumer != null) {
                consumer.subscribe(partitions);
            } else {
                log.warn("No consumer found for integration type: {}", type);
            }
        }));
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        startupExecutor.execute(this::doStartup);
    }

    private void doStartup() {
        try {
            if (cacheStartupService != null) {
                cacheStartupService.preloadCaches();
            }

            boolean supported = !supportedIntegrationTypes.isEmpty();

            if (supported || serviceInfoProvider.isService(ServiceType.TB_CORE)) {
                log.debug("Subscribing to notifications: {}", nfConsumer.getTopic());
                this.nfConsumer.subscribe();
                launchNotificationsConsumer();
            }

            if (supported) {
                launchMainConsumers();
            }

            log.info("Integration Executor startup completed. Ready to handle partition changes.");
        } catch (Exception e) {
            log.error("Failed to complete Integration Executor startup. Will continue in degraded mode.", e);
        } finally {
            // Mark as started and drain pending events even on failure, so that partition change events are not stuck in the queue forever.
            // After started=true, any new events will drain themselves in onTbApplicationEvent().
            // ConcurrentLinkedQueue.poll() is atomic, so each event is processed exactly once.
            started = true;
            drainPendingEvents();
        }
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return ServiceType.TB_INTEGRATION_EXECUTOR.equals(event.getServiceType());
    }

    protected void launchMainConsumers() {
        consumers.forEach((integrationType, consumer) -> launchConsumer(consumer, integrationType));
    }

    void launchConsumer(TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> consumer, IntegrationType integrationType) {
        consumersExecutor.execute(() -> consumerLoop(consumer, integrationType));
    }

    void consumerLoop(TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> consumer, IntegrationType integrationType) {
        ThingsBoardThreadFactory.updateCurrentThreadName(integrationType.name());

        // Wait for caches to be preloaded before processing messages (only in Integration Executor)
        if (cacheStartupService != null) {
            try {
                log.debug("[{}] Waiting for Integration Executor caches to be ready...", integrationType);
                cacheStartupService.awaitCacheReady();
                log.debug("[{}] Integration Executor caches are ready, starting message processing", integrationType);
            } catch (InterruptedException e) {
                log.error("[{}] Interrupted while waiting for caches to be ready", integrationType, e);
                Thread.currentThread().interrupt();
                return;
            }
        }

        long pollDuration = integrationNotificationSettings.getPollInterval();
        long processingTimeout = integrationNotificationSettings.getPackProcessingTimeout();
        while (!stopped && !consumer.isStopped()) {
            try {
                List<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> msgs = consumer.poll(pollDuration);
                if (msgs.isEmpty()) {
                    continue;
                }
                ConcurrentMap<UUID, TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> pendingMap = msgs.stream().collect(
                        Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                TbPackProcessingContext<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> ctx = new TbPackProcessingContext<>(
                        processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                pendingMap.forEach((id, msg) -> {
                    log.trace("[{}] Creating downlink callback for message: {}", id, msg.getValue());
                    TbCallback callback = new TbPackCallback<>(id, ctx);
                    try {
                        handleDownlink(msg, callback);
                    } catch (Throwable e) {
                        log.warn("[{}] Failed to process notification: {}", id, msg, e);
                        callback.onFailure(e);
                    }
                });
                if (!processingTimeoutLatch.await(processingTimeout, TimeUnit.MILLISECONDS)) {
                    ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process downlink: {}", id, msg.getValue()));
                    ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process downlink: {}", id, msg.getValue()));
                }
                consumer.commit();
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("Failed to obtain downlink messages from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new downlink messages", e2);
                    }
                }
            }
        }
        log.info("TB Integration Downlink Consumer stopped.");
    }

    protected void launchNotificationsConsumer() {
        notificationsConsumerExecutor.submit(() -> {
            long pollDuration = integrationNotificationSettings.getPollInterval();
            long processingTimeout = integrationNotificationSettings.getPackProcessingTimeout();
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> msgs = nfConsumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> pendingMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> ctx = new TbPackProcessingContext<>(
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
                    if (!processingTimeoutLatch.await(processingTimeout, TimeUnit.MILLISECONDS)) {
                        ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process notification: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process notification: {}", id, msg.getValue()));
                    }
                    nfConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain notifications from queue.", e);
                        try {
                            Thread.sleep(pollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new notifications", e2);
                        }
                    }
                }
            }
            log.info("TB Integration Notifications Consumer stopped.");
        });
    }

    private void handleDownlink(TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg> msg, TbCallback callback) {
        log.trace("Received downlink: {}", msg);
        var downlinkMsg = msg.getValue();
        if (downlinkMsg.hasDownlinkMsg()) {
            integrationManagerService.handleDownlink(downlinkMsg.getDownlinkMsg(), callback);
        } else if (downlinkMsg.hasValidationRequestMsg()) {
            integrationManagerService.handleValidationRequest(downlinkMsg.getValidationRequestMsg(), callback);
        } else {
            callback.onSuccess();
        }
    }

    private void handleNotification(UUID id, TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg> msg, TbCallback callback) {
        ToIntegrationExecutorNotificationMsg nf = msg.getValue();
        handleComponentLifecycleMsg(id, ProtoUtils.fromProto(nf.getComponentLifecycle()));
        callback.onSuccess();
    }

    protected void handleComponentLifecycleMsg(UUID id, TbActorMsg actorMsg) {
        if (actorMsg instanceof ComponentLifecycleMsg componentLifecycleMsg) {
            log.info("[{}][{}][{}] Received Lifecycle event: {}", componentLifecycleMsg.getTenantId(),
                    componentLifecycleMsg.getEntityId().getEntityType(),
                    componentLifecycleMsg.getEntityId(), componentLifecycleMsg.getEvent());
            integrationManagerService.handleComponentLifecycleMsg(componentLifecycleMsg);
        }
        log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg);
    }

    private void refreshIntegrationsByType(IntegrationType type) {
        try {
            Set<TopicPartitionInfo> partitions = getLatestPartitionsFromQueue(type);
            if (partitions != null) {
                integrationManagerService.refresh(type, partitions);
            }
        } catch (Throwable t) {
            log.warn("[{}] Failed to refresh integrations", type, t);
        }
    }

    private Set<TopicPartitionInfo> getLatestPartitionsFromQueue(IntegrationType type) {
        var queue = subscribeEventsMap.get(type);
        log.debug("[{}] getLatestPartitionsFromQueue, queue size {}", type, queue.size());
        Set<TopicPartitionInfo> partitions = null;
        while (!queue.isEmpty()) {
            partitions = queue.poll();
            log.debug("[{}] polled from the queue partitions {}", type, partitions);
        }
        log.debug("[{}] getLatestPartitionsFromQueue, partitions {}", type, partitions);
        return partitions;
    }

}
