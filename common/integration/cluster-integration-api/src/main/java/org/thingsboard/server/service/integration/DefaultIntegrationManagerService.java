// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.api.data.DefaultIntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.util.IntegrationUtil;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.notification.rule.trigger.IntegrationLifecycleEventTrigger;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.gen.integration.IntegrationValidationRequestProto;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationDownlinkMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationValidationResponseProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;
import org.thingsboard.server.service.cache.IntegrationExecutorAssetCache;
import org.thingsboard.server.service.cache.IntegrationExecutorAssetProfileCache;
import org.thingsboard.server.service.cache.IntegrationExecutorDeviceCache;
import org.thingsboard.server.service.cache.IntegrationExecutorDeviceProfileCache;
import org.thingsboard.server.service.cache.IntegrationExecutorTenantProfileCache;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.integration.state.IntegrationState;
import org.thingsboard.server.service.integration.state.ValidationTask;
import org.thingsboard.server.service.integration.state.ValidationTaskType;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent.DELETED;
import static org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent.FAILED;
import static org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent.STARTED;
import static org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent.STOPPED;
import static org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent.UPDATED;

@Slf4j
@TbCoreOrIntegrationExecutorComponent
@Service
@RequiredArgsConstructor
public class DefaultIntegrationManagerService implements IntegrationManagerService {

    private final ConcurrentMap<IntegrationId, IntegrationState> integrations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IntegrationState> integrationsByRoutingKeyMap = new ConcurrentHashMap<>();
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;
    private final TopicService topicService;
    private final IntegrationContextProvider integrationContextProvider;
    private final IntegrationConfigurationService configurationService;
    private final DataConverterService dataConverterService;
    private final EventStorageService eventStorageService;
    private final TbQueueProducerProvider producerProvider;
    private final NotificationRuleProcessor notificationRuleProcessor;
    private final Optional<CoapServerService> coapServerService;
    private final Optional<RemoteIntegrationRpcService> remoteRpcService;
    private final Set<IntegrationType> supportedIntegrationTypes = new HashSet<>();
    private final ConcurrentMap<UUID, ValidationTask> pendingValidationTasks = new ConcurrentHashMap<>();
    private final IntegrationStatisticsService integrationStatisticsService;
    private final TbQueueIntegrationExecutorSettings integrationExecutorSettings;
    private final Optional<IntegrationExecutorTenantProfileCache> tenantProfileCache;
    private final Optional<IntegrationExecutorDeviceProfileCache> deviceProfileCache;
    private final Optional<IntegrationExecutorAssetProfileCache> assetProfileCache;
    private final Optional<IntegrationExecutorDeviceCache> deviceCache;
    private final Optional<IntegrationExecutorAssetCache> assetCache;

    @Value("${integrations.reinit.enabled:false}")
    private boolean reInitEnabled;

    @Value("${integrations.reinit.frequency:3600000}")
    private long reInitFrequency;

    @Value("${integrations.statistics.enabled}")
    private boolean statisticsEnabled;

    @Value("${integrations.statistics.persist_frequency}")
    private long statisticsPersistFrequency;

    @Value("${integrations.allow_Local_network_hosts:true}")
    private boolean allowLocalNetworkHosts;

    @Getter
    @Value("${integrations.init.connection_check_api_request_timeout_sec:20}")
    private int integrationConnectionCheckApiRequestTimeoutSec;

    private ExecutorService commandExecutorService;
    private ScheduledExecutorService statisticsExecutorService;
    private ScheduledExecutorService reInitExecutorService;
    private ScheduledExecutorService lifecycleExecutorService;

    @PostConstruct
    public void init() {
        lifecycleExecutorService = ThingsBoardExecutors.newScheduledThreadPool(4, "ie-lifecycle");
        lifecycleExecutorService.scheduleWithFixedDelay(this::cleanupPendingValidationTasks, 1, 1, TimeUnit.MINUTES);
        commandExecutorService = ThingsBoardExecutors.newWorkStealingPool(4, "ie-commands");
        if (reInitEnabled) {
            reInitExecutorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("ie-reinit");
            reInitExecutorService.scheduleAtFixedRate(this::reInitIntegrations, reInitFrequency, reInitFrequency, TimeUnit.MILLISECONDS);
        }
        if (statisticsEnabled) {
            statisticsExecutorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("ie-stats");
            statisticsExecutorService.scheduleAtFixedRate(this::persistStatistics, statisticsPersistFrequency, statisticsPersistFrequency, TimeUnit.MILLISECONDS);
        }
        supportedIntegrationTypes.addAll(serviceInfoProvider.getSupportedIntegrationTypes());
    }

    @PreDestroy
    public void stop() {
        if (lifecycleExecutorService != null) {
            lifecycleExecutorService.shutdownNow();
        }
        if (commandExecutorService != null) {
            commandExecutorService.shutdownNow();
        }
        if (statisticsEnabled) {
            statisticsExecutorService.shutdownNow();
        }
        if (reInitEnabled) {
            reInitExecutorService.shutdownNow();
        }
    }

    @Override
    public void handleComponentLifecycleMsg(ComponentLifecycleMsg componentLifecycleMsg) {
        var entityType = componentLifecycleMsg.getEntityId().getEntityType();
        switch (entityType) {
            case INTEGRATION:
                processIntegrationUpdate(componentLifecycleMsg);
                break;
            case CONVERTER:
                processConverterUpdate(componentLifecycleMsg);
                break;
            case TENANT:
                processTenantUpdate(componentLifecycleMsg);
                break;
            case TENANT_PROFILE:
                tenantProfileCache.ifPresent(cache -> cache.evict((TenantProfileId) componentLifecycleMsg.getEntityId()));
                break;
            case DEVICE_PROFILE:
                deviceProfileCache.ifPresent(cache -> cache.evict(componentLifecycleMsg.getEntityId().getId()));
                break;
            case ASSET_PROFILE:
                assetProfileCache.ifPresent(cache -> cache.evict(componentLifecycleMsg.getEntityId().getId()));
                break;
            case DEVICE:
                deviceCache.ifPresent(cache -> cache.evict(componentLifecycleMsg.getEntityId().getId()));
                break;
            case ASSET:
                assetCache.ifPresent(cache -> cache.evict(componentLifecycleMsg.getEntityId().getId()));
                break;
            default:
                log.info("[{}][{}] Ignore update due to not supported entity type: {}",
                        componentLifecycleMsg.getTenantId(), componentLifecycleMsg.getEntityId(), componentLifecycleMsg.getEvent());
        }
    }

    private void processTenantUpdate(ComponentLifecycleMsg componentLifecycleMsg) {
        TenantId tenantId = TenantId.fromUUID(componentLifecycleMsg.getEntityId().getId());
        if (ComponentLifecycleEvent.DELETED.equals(componentLifecycleMsg.getEvent())) {
            integrations.values().stream().filter(state -> state.getTenantId().equals(tenantId)).forEach(state -> {
                scheduleIntegrationEvent(state.getTenantId(), state.getId(), DELETED);
            });
        }
        tenantProfileCache.ifPresent(cache -> cache.evict(tenantId));
    }

    @Override
    public void handleDownlink(IntegrationDownlinkMsgProto proto, TbCallback callback) {
        commandExecutorService.submit(() -> {
            try {
                TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
                IntegrationId integrationId = new IntegrationId(new UUID(proto.getIntegrationIdMSB(), proto.getIntegrationIdLSB()));
                IntegrationDownlinkMsg msg = new DefaultIntegrationDownlinkMsg(tenantId, integrationId, TbMsg.fromProto(null, proto.getDataProto(), TbMsgCallback.EMPTY), null);
                var state = integrations.get(integrationId);
                if (state == null) {
                    callback.onFailure(new RuntimeException("Integration is missing!"));
                    return;
                }
                ThingsboardPlatformIntegration<?> integration = state.getIntegration();
                ComponentLifecycleEvent currentState = state.getCurrentState();
                if (integration == null || currentState == null) {
                    callback.onFailure(new RuntimeException("Integration is not initialized yet!"));
                } else if (currentState == ComponentLifecycleEvent.FAILED) {
                    callback.onFailure(new RuntimeException("Integration failed to initialize!"));
                } else {
                    integration.onDownlinkMsg(msg);
                    callback.onSuccess();
                }
            } catch (Exception e) {
                callback.onFailure(e);
                throw handleException(e);
            }
        });
    }

    boolean isExpired(IntegrationValidationRequestProto validationRequestMsg, TbCallback callback) {
        if (validationRequestMsg.getDeadline() > 0 && validationRequestMsg.getDeadline() <= System.currentTimeMillis()) {
            UUID requestId = new UUID(validationRequestMsg.getIdMSB(), validationRequestMsg.getIdLSB());
            TenantId tenantId = TenantId.fromUUID(new UUID(validationRequestMsg.getTenantIdMSB(), validationRequestMsg.getTenantIdLSB()));
            log.debug("[{}][{}][{}] Integration validation expired: {}", tenantId, validationRequestMsg.getType(), requestId, validationRequestMsg.getDeadline());
            callback.onFailure(new TimeoutException("Integration validation expired"));
            return true;
        }
        return false;
    }

    @Override
    public void handleValidationRequest(IntegrationValidationRequestProto validationRequestMsg, TbCallback callback) {
        log.debug("submitting validation request: {}", validationRequestMsg);
        if (isExpired(validationRequestMsg, callback)) {
            return;
        }

        commandExecutorService.submit(() -> {

            log.debug("Starting validation request: {}", validationRequestMsg);
            if (isExpired(validationRequestMsg, callback)) {
                return;
            }

            UUID requestId = new UUID(validationRequestMsg.getIdMSB(), validationRequestMsg.getIdLSB());
            TenantId tenantId = TenantId.fromUUID(new UUID(validationRequestMsg.getTenantIdMSB(), validationRequestMsg.getTenantIdLSB()));
            var response = IntegrationValidationResponseProto.newBuilder();
            response.setIdMSB(requestId.getMostSignificantBits());
            response.setIdLSB(requestId.getLeastSignificantBits());
            try {
                ValidationTaskType validationTaskType = ValidationTaskType.valueOf(validationRequestMsg.getType());
                Integration configuration = ProtoUtils.fromProto(validationRequestMsg.getConfiguration());
                doValidateLocally(validationTaskType, configuration);
                log.trace("[{}][{}] Processed the validation request for integration: {}", tenantId, requestId, configuration);
            } catch (Exception e) {
                log.trace("[{}][{}][{}] Integration validation failed", tenantId, validationRequestMsg.getType(), requestId, e);
                response.setError(ByteString.copyFrom(JavaSerDesUtil.encode(e)));
            }
            TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, validationRequestMsg.getServiceId());
            TransportProtos.ToCoreNotificationMsg msg = TransportProtos.ToCoreNotificationMsg.newBuilder().setIntegrationValidationResponseMsg(response).build();
            producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.trace("[{}][{}] Published the validation response ", tenantId, requestId);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.debug("[{}][{}] Failed to publish the validation response ", tenantId, requestId, t);
                }
            });
            callback.onSuccess();

        });
    }

    private void doValidateLocally(ValidationTaskType validationTaskType, Integration configuration) throws Exception {
        IntegrationContext context = integrationContextProvider.buildIntegrationContext(configuration);
        ThingsboardPlatformIntegration<?> integration = createPlatformIntegration(configuration);
        switch (validationTaskType) {
            case VALIDATE:
                integration.validateConfiguration(configuration, allowLocalNetworkHosts);
                break;
            case CHECK_CONNECTION:
                integration.checkConnection(configuration, context);
        }
    }

    @Override
    public void handleValidationResponse(IntegrationValidationResponseProto validationResponseMsg, TbCallback callback) {
        UUID requestId = new UUID(validationResponseMsg.getIdMSB(), validationResponseMsg.getIdLSB());
        ValidationTask validationTask = pendingValidationTasks.remove(requestId);
        if (validationTask != null) {
            ByteString error = validationResponseMsg.getError();
            SettableFuture<Void> future = validationTask.getFuture();
            if (error.isEmpty()) {
                future.set(null);
            } else {
                future.setException(JavaSerDesUtil.decode(error.toByteArray()));
            }
        }
        callback.onSuccess();
    }

    private void cleanupPendingValidationTasks() {
        long expTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
        pendingValidationTasks.values().stream().filter(task -> task.getTs() < expTime).map(ValidationTask::getUuid).forEach(pendingValidationTasks::remove);
    }

    @Override
    public ListenableFuture<Void> validateIntegrationConfiguration(Integration configuration) {
        return validateConfiguration(configuration, ValidationTaskType.VALIDATE);
    }

    @Override
    public ListenableFuture<Void> checkIntegrationConnection(Integration configuration) {
        return validateConfiguration(configuration, ValidationTaskType.CHECK_CONNECTION);
    }

    private ListenableFuture<Void> validateConfiguration(Integration configuration, ValidationTaskType validationTaskType) {
        if (configuration.getTenantId() == null) {
            return Futures.immediateFailedFuture(new DataValidationException("Integration tenant should be specified!"));
        }
        if (StringUtils.isEmpty(configuration.getName())) {
            return Futures.immediateFailedFuture(new DataValidationException("Integration name should be specified!"));
        }
        if (configuration.getType() == null) {
            return Futures.immediateFailedFuture(new DataValidationException("Integration type should be specified!"));
        }
        if (StringUtils.isEmpty(configuration.getRoutingKey())) {
            return Futures.immediateFailedFuture(new DataValidationException("Integration routing key should be specified!"));
        }
        try {
            if (configuration.getType().isRemoteOnly() || configuration.isRemote()) {
                return Futures.immediateFuture(null);
            }
            if (configuration.getId() == null) {
                configuration = new Integration(configuration);
                configuration.setId(new IntegrationId(UUID.randomUUID()));
            }
            if (isMine(configuration)) {
                doValidateLocally(validationTaskType, configuration);
                return Futures.immediateFuture(null);
            } else {
                ValidationTask task = new ValidationTask(validationTaskType, configuration);
                pendingValidationTasks.put(task.getUuid(), task);

                var producer = producerProvider.getTbIntegrationExecutorDownlinkMsgProducer();
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_INTEGRATION_EXECUTOR, configuration.getType().name(), configuration.getTenantId(), configuration.getId())
                        .withTopic(topicService.buildTopicName(integrationExecutorSettings.getIntegrationDownlinkTopic(configuration.getType())));
                IntegrationValidationRequestProto requestProto = IntegrationValidationRequestProto.newBuilder()
                        .setIdMSB(task.getUuid().getMostSignificantBits())
                        .setIdLSB(task.getUuid().getLeastSignificantBits())
                        .setType(validationTaskType.name())
                        .setConfiguration(ProtoUtils.toProto(configuration))
                        .setServiceId(serviceInfoProvider.getServiceId())
                        .setTenantIdMSB(configuration.getTenantId().getId().getMostSignificantBits())
                        .setTenantIdLSB(configuration.getTenantId().getId().getLeastSignificantBits())
                        .setTimestamp(System.currentTimeMillis())
                        .setDeadline(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(this.getIntegrationConnectionCheckApiRequestTimeoutSec()))
                        .build();

                producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), ToIntegrationExecutorDownlinkMsg.newBuilder()
                        .setValidationRequestMsg(requestProto).build()), new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {

                    }

                    @Override
                    public void onFailure(Throwable t) {
                        pendingValidationTasks.remove(task.getUuid());
                        task.getFuture().setException(t);
                    }
                });
                return task.getFuture();
            }
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String key) {
        IntegrationState state = integrationsByRoutingKeyMap.get(key);
        if (state == null || state.getIntegration() == null) {
            Optional<Integration> configurationOpt = Optional.ofNullable(configurationService.getIntegration(TenantId.SYS_TENANT_ID, key));
            if (configurationOpt.isPresent()) {
                Integration integration = configurationOpt.get();
                if (!supportedIntegrationTypes.contains(integration.getType())) {
                    return Futures.immediateFailedFuture(new ThingsboardException("Current server does not support integrations with type: " + integration.getType() + "!", ThingsboardErrorCode.INVALID_ARGUMENTS));
                }
                if (integration.isRemote()) {
                    return Futures.immediateFailedFuture(new ThingsboardException("The integration is executed remotely!", ThingsboardErrorCode.INVALID_ARGUMENTS));
                }
                if (!integration.isEnabled()) {
                    return Futures.immediateFailedFuture(new ThingsboardException("The integration is disabled!", ThingsboardErrorCode.INVALID_ARGUMENTS));
                }
                if (integration.getType().isSingleton() && !isMine(integration)) {
                    return Futures.immediateFailedFuture(new ThingsboardException("Singleton integration already present on another node!", ThingsboardErrorCode.INVALID_ARGUMENTS));
                }
                return Futures.immediateFailedFuture(new ThingsboardException("Integration is not present in routing key map!", ThingsboardErrorCode.GENERAL));
            } else {
                return Futures.immediateFailedFuture(new ThingsboardException("Failed to find integration by routing key!", ThingsboardErrorCode.ITEM_NOT_FOUND));
            }
        } else {
            return Futures.immediateFuture(state.getIntegration());
        }
    }

    @Override
    public void refresh(IntegrationType integrationType, Set<TopicPartitionInfo> newPartitions) {
        if (!supportedIntegrationTypes.contains(integrationType)) {
            return;
        }
        if (newPartitions.isEmpty()) {
            return;
        }
        Set<Integer> ownedPartitions = newPartitions.stream()
                .map(tpi -> tpi.getPartition().orElse(-1))
                .filter(p -> p >= 0)
                .collect(Collectors.toSet());

        Set<IntegrationId> currentIntegrationIds = new HashSet<>(integrations.keySet());
        for (IntegrationId integrationId : currentIntegrationIds) {
            var state = integrations.get(integrationId);
            if (state == null) {
                continue;
            }
            ThingsboardPlatformIntegration<?> platformIntegration = state.getIntegration();
            Integration integration;
            if (platformIntegration != null) {
                integration = platformIntegration.getConfiguration();
            } else {
                integration = state.getConfiguration() != null
                        ? state.getConfiguration()
                        : configurationService.getIntegration(state.getTenantId(), state.getId());
            }
            if (integration != null && !isMineForPartitions(integration, ownedPartitions)) {
                scheduleIntegrationEvent(integration.getTenantId(), integration.getId(), ComponentLifecycleEvent.STOPPED);
            }
        }
        createIntegrations(configurationService.getActiveIntegrationList(integrationType, false), ownedPartitions);
    }

    private void createIntegrations(List<Integration> allIntegrations, Set<Integer> ownedPartitions) {
        try {
            for (Integration integration : allIntegrations) {
                try {
                    if (isMineForPartitions(integration, ownedPartitions)) {
                        IntegrationState existing = integrations.get(integration.getId());
                        if (existing != null && existing.getIntegration() != null) {
                            log.debug("[{}] Integration {} already running, skipping re-init", integration.getId(), integration.getName());
                            continue;
                        }
                        scheduleIntegrationEvent(integration.getTenantId(), integration.getId(), ComponentLifecycleEvent.CREATED);
                    }
                } catch (Exception e) {
                    log.info("[{}] Unable to initialize integration {}", integration.getId(), integration.getName(), e);
                }
            }
        } catch (Throwable th) {
            log.error("Could not init integrations", th);
        }
    }

    private void processConverterUpdate(ComponentLifecycleMsg msg) {
        ConverterId converterId = new ConverterId(msg.getEntityId().getId());
        if (msg.getEvent() == DELETED) {
            dataConverterService.deleteConverter(converterId);
        } else {
            Converter converter = configurationService.getConverter(msg.getTenantId(), converterId);
            if (msg.getEvent() == ComponentLifecycleEvent.CREATED) {
                dataConverterService.createConverter(converter);
            } else {
                dataConverterService.updateConverter(converter);
            }
        }
    }

    private void processIntegrationUpdate(ComponentLifecycleMsg msg) {
        var id = new IntegrationId(msg.getEntityId().getId());
        if (ComponentLifecycleEvent.DELETED.equals(msg.getEvent()) && integrations.containsKey(id)) {
            scheduleIntegrationEvent(msg.getTenantId(), (IntegrationId) msg.getEntityId(), DELETED);
        } else {
            Integration configuration = configurationService.getIntegration(msg.getTenantId(), id);
            if (configuration == null) {
                log.debug("[{}][{}] Ignore update event because integration is not found", msg.getTenantId(), id);
                return;
            }
            if (configuration.isRemote()) {
                scheduleIntegrationEvent(msg.getTenantId(), (IntegrationId) msg.getEntityId(), ComponentLifecycleEvent.STOPPED);
                remoteRpcService.ifPresent(service -> service.updateIntegration(configuration));
            } else {
                if (isMine(configuration)) {
                    scheduleIntegrationEvent(msg.getTenantId(), (IntegrationId) msg.getEntityId(), msg.getEvent());
                } else {
                    if (integrations.containsKey(id)) {
                        scheduleIntegrationEvent(msg.getTenantId(), (IntegrationId) msg.getEntityId(), ComponentLifecycleEvent.STOPPED);
                    } else {
                        log.debug("[{}][{}] Ignore update event for not mine integration", msg.getTenantId(), id);
                    }
                }
            }
        }
    }

    private void tryUpdate(IntegrationId id) {
        IntegrationState state = integrations.get(id);
        if (state == null) {
            log.debug("[{}] Skip processing of update due to missing state. Probably integration was already deleted.", id);
            return;
        }
        boolean locked = state.getUpdateLock().tryLock();
        if (locked) {
            boolean success = true;
            var old = state.getCurrentState();
            try {
                update(state);
            } catch (Throwable e) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Failed to update state", id, e);
                } else {
                    log.warn("[{}] Failed to update state due to: {}. Enabled debug level to get more details.", id, e.getMessage());
                }
                success = false;
            } finally {
                state.getUpdateLock().unlock();
            }
            onIntegrationStateUpdate(state, old, success);
        } else {
            lifecycleExecutorService.schedule(() -> tryUpdate(id), 10, TimeUnit.SECONDS);
        }
    }

    private void update(IntegrationState state) throws Exception {
        Queue<ComponentLifecycleEvent> stateQueue = state.getUpdateQueue();
        ComponentLifecycleEvent pendingEvent = null;
        while (!stateQueue.isEmpty()) {
            var update = stateQueue.poll();
            if (!DELETED.equals(pendingEvent)) {
                pendingEvent = update;
            }
        }
        if (pendingEvent != null) {
            switch (pendingEvent) {
                case CREATED:
                case STARTED:
                case UPDATED:
                case ACTIVATED:
                    Integration configuration = configurationService.getIntegration(state.getTenantId(), state.getId());
                    if (configuration != null) {
                        processUpdateEvent(state, configuration);
                    } else {
                        processStop(state, DELETED);
                    }
                    break;
                case STOPPED:
                case SUSPENDED:
                case DELETED:
                    persistCurrentStatistics(state);
                    processStop(state, pendingEvent);
                    break;
            }
            log.debug("[{}] Going to process new event: {}", state.getId(), pendingEvent);
        }
    }

    private void processUpdateEvent(IntegrationState state, Integration configuration) throws Exception {
        state.setConfiguration(configuration);
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}] Going to update the integration: {}", state.getTenantId(), configuration.getId(), configuration);
        } else {
            log.info("[{}][{}] Going to update the integration.", state.getTenantId(), configuration.getId());
        }
        if (state.getIntegration() == null) {
            if (configuration.isRemote()) {
                throw new ThingsboardException("The integration is executed remotely!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            if (!configuration.isEnabled()) {
                throw new ThingsboardException("The integration is disabled!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            IntegrationContext context = integrationContextProvider.buildIntegrationContext(configuration);
            ThingsboardPlatformIntegration<?> integration = createPlatformIntegration(configuration);
            integration.validateConfiguration(configuration, allowLocalNetworkHosts);
            state.setIntegration(integration);
            integrationsByRoutingKeyMap.putIfAbsent(configuration.getRoutingKey(), state);
            try {
                integration.init(new TbIntegrationInitParams(context, configuration, getUplinkDataConverter(configuration), getDownlinkDataConverter(configuration)));
                eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.STARTED, null);
                state.setCurrentState(ComponentLifecycleEvent.STARTED);
                processNotificationRule(configuration, STARTED, null);
            } catch (Exception e) {
                // init() may have already opened clients or bound ports, and destroy() is what releases them.
                // Clearing state.integration also keeps getIntegrationByRoutingKey from handing out a
                // half-initialized integration, and sends the next re-init through the fresh-init path.
                destroyFailedIntegration(state, configuration, integration);
                state.setCurrentState(ComponentLifecycleEvent.FAILED);
                eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.FAILED, e);
                processNotificationRule(configuration, STARTED, e);
                throw handleException(e);
            }
        } else {
            IntegrationContext context = integrationContextProvider.buildIntegrationContext(configuration);
            try {
                if (configuration.isEnabled()) {
                    state.getIntegration().update(new TbIntegrationInitParams(context, configuration, getUplinkDataConverter(configuration), getDownlinkDataConverter(configuration)));
                    eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.UPDATED, null);
                    state.setCurrentState(ComponentLifecycleEvent.STARTED);
                    processNotificationRule(configuration, UPDATED, null);
                } else {
                    persistCurrentStatistics(state);
                    processStop(state, ComponentLifecycleEvent.STOPPED);
                }
            } catch (Exception e) {
                state.setCurrentState(ComponentLifecycleEvent.FAILED);
                eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.UPDATED, e);
                processNotificationRule(configuration, UPDATED, e);
                throw handleException(e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}] Updated the integration successfully: {}", state.getTenantId(), configuration.getId(), configuration);
        } else {
            log.info("[{}][{}] Updated the integration successfully.", state.getTenantId(), configuration.getId());
        }
    }

    private void destroyFailedIntegration(IntegrationState state, Integration configuration, ThingsboardPlatformIntegration<?> integration) {
        try {
            integration.destroy();
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to destroy the partially initialized integration: ", state.getTenantId(), state.getId(), e);
        }
        state.setIntegration(null);
        integrationsByRoutingKeyMap.remove(configuration.getRoutingKey(), state);
    }

    private ThingsboardPlatformIntegration<?> createPlatformIntegration(Integration configuration) throws Exception {
        return IntegrationUtil.createPlatformIntegration(configuration.getType(), configuration.getConfiguration(), false, coapServerService.orElse(null));
    }

    private void processStop(IntegrationState state, ComponentLifecycleEvent event) {
        if (state == null) {
            return;
        }
        integrations.remove(state.getId());
        ThingsboardPlatformIntegration<?> integration = state.getIntegration();
        if (integration == null) {
            // The integration was already destroyed - a failed init clears the field. The stop itself still
            // has to be recorded, otherwise the lifecycle event and the notification rule are lost.
            Integration configuration = state.getConfiguration();
            state.setCurrentState(event);
            if (configuration != null) {
                eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), event, null);
                processNotificationRule(configuration, STOPPED, null);
            }
            return;
        }
        Integration configuration = integration.getConfiguration();
        integrationsByRoutingKeyMap.remove(configuration.getRoutingKey());
        state.setCurrentState(event);
        try {
            integration.destroy();
            eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), event, null);
            processNotificationRule(configuration, STOPPED, null);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[{}][{}] Failed to destroy the integration: {}", state.getTenantId(), state.getId(), configuration, e);
            } else {
                log.warn("[{}][{}] Failed to destroy the integration: ", state.getTenantId(), state.getId());
            }
            eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), event, e);
            processNotificationRule(configuration, STOPPED, e);
            throw handleException(e);
        }
    }

    private TBUplinkDataConverter getUplinkDataConverter(Integration integration) {
        return dataConverterService.getUplinkConverterById(integration.getTenantId(), integration.getDefaultConverterId())
                .orElseThrow(() -> new ThingsboardRuntimeException("Converter not found!", ThingsboardErrorCode.ITEM_NOT_FOUND));
    }

    private TBDownlinkDataConverter getDownlinkDataConverter(Integration integration) {
        return dataConverterService.getDownlinkConverterById(integration.getTenantId(), integration.getDownlinkConverterId())
                .orElse(null);
    }

    private boolean isMine(AbstractIntegration integration) {
        var type = integration.getType();
        if (supportedIntegrationTypes.contains(type)) {
            return !type.isSingleton() || partitionService.resolve(ServiceType.TB_INTEGRATION_EXECUTOR, type.name(),
                    integration.getTenantId(), integration.getId()).isMyPartition();
        } else {
            return false;
        }
    }

    private boolean isMineForPartitions(AbstractIntegration integration, Set<Integer> ownedPartitions) {
        var type = integration.getType();
        if (!supportedIntegrationTypes.contains(type)) {
            return false;
        }
        if (!type.isSingleton()) {
            return true;
        }
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_INTEGRATION_EXECUTOR, type.name(), integration.getTenantId(), integration.getId());
        int partition = tpi.getPartition().orElse(-1);
        return ownedPartitions.contains(partition);
    }

    private void scheduleIntegrationEvent(TenantId tenantId, IntegrationId id, ComponentLifecycleEvent event) {
        IntegrationState state = switch (event) {
            case CREATED, UPDATED -> integrations.computeIfAbsent(id, tmp -> new IntegrationState(tenantId, id));
            default -> integrations.get(id);
        };
        if (state != null) {
            state.getUpdateQueue().add(event);
            log.debug("[{}][{}] Scheduling new event: {}", tenantId, id, event);
            lifecycleExecutorService.submit(() -> tryUpdate(id));
        } else {
            log.info("[{}][{}] Ignoring new event: {}", tenantId, id, event);
        }
    }

    private void reInitIntegrations() {
        integrations.values().forEach(state -> {
            if (state.getConfiguration() != null && state.getCurrentState() != null && state.getCurrentState().equals(ComponentLifecycleEvent.FAILED)) {
                if (state.getUpdateLock().tryLock()) {
                    try {
                        processUpdateEvent(state, state.getConfiguration());
                    } catch (Exception e) {
                        log.trace("[{}][{}] Failed to re-initialize the integration:", state.getTenantId(), state.getId(), e);
                    } finally {
                        state.getUpdateLock().unlock();
                    }
                }
            }
        });
    }

    private void persistStatistics() {
        long ts = System.currentTimeMillis();
        integrations.forEach((id, integration) -> {
            doPersistStatistics(integration, ts, false);
        });
    }

    private void persistCurrentStatistics(IntegrationState integrationState) {
        if (statisticsEnabled) {
            doPersistStatistics(integrationState, System.currentTimeMillis(), true);
        }
    }

    private void doPersistStatistics(IntegrationState integrationState, long ts, boolean skipEmptyStatistics) {
        if (!STARTED.equals(integrationState.getCurrentState())) {
            log.debug("[{}{}] Can't persist statistics, integration has not started yet!}", integrationState.getTenantId(), integrationState.getId());
            return;
        }
        IntegrationStatistics statistics = integrationState.getIntegration().popStatistics();
        if (skipEmptyStatistics && statistics.isEmpty()) {
            return;
        }
        Integration integrationInfo = integrationState.getIntegration().getConfiguration();
        try {
            eventStorageService.persistStatistics(integrationInfo.getTenantId(), integrationInfo.getId(), ts, statistics, integrationState.getCurrentState());
        } catch (Exception e) {
            log.warn("[{}] Failed to persist statistics: {}", integrationInfo.getId(), statistics, e);
        }
    }

    private void processNotificationRule(Integration integration, ComponentLifecycleEvent event, Exception error) {
        if (!integration.getType().isSingleton()) {
            if (!partitionService.resolve(ServiceType.TB_INTEGRATION_EXECUTOR, integration.getType().name(), integration.getTenantId(), integration.getId()).isMyPartition()) {
                return;
            }
        }
        notificationRuleProcessor.process(IntegrationLifecycleEventTrigger.builder()
                .tenantId(integration.getTenantId())
                .integrationId(integration.getId())
                .integrationType(integration.getType())
                .integrationName(integration.getName())
                .event(event)
                .error(error)
                .build());
    }

    private RuntimeException handleException(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }

    private void onIntegrationStateUpdate(IntegrationState state, ComponentLifecycleEvent oldState, boolean success) {
        try {
            if (state.getConfiguration() != null) {
                var integrationType = state.getConfiguration().getType();
                if (integrationType != null && state.getCurrentState() != null) {
                    integrationStatisticsService.onIntegrationStateUpdate(integrationType, state.getCurrentState(), success);
                }
                if (oldState == null || !oldState.equals(state.getCurrentState())) {
                    int startedCount = (int) integrations.values()
                            .stream()
                            .filter(i -> i.getCurrentState() != null)
                            .filter(i -> i.getConfiguration() != null && i.getConfiguration().getType().equals(integrationType))
                            .filter(i -> STARTED.equals(i.getCurrentState()))
                            .count();

                    int failedCount = (int) integrations.values()
                            .stream()
                            .filter(i -> i.getCurrentState() != null)
                            .filter(i -> i.getConfiguration() != null && i.getConfiguration().getType().equals(integrationType))
                            .filter(i -> FAILED.equals(i.getCurrentState()))
                            .count();

                    integrationStatisticsService.onIntegrationsCountUpdate(integrationType, startedCount, failedCount);
                }
            }
        } catch (Exception e) {
            log.warn("[{}][{}][{}] Failed to process integration update event", state.getId(), oldState, success, e);
        }
    }

}
