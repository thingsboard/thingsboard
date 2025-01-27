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
package org.thingsboard.server.service.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeScopeProto;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityUpdateMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ComponentLifecycleEvent;
import org.thingsboard.server.gen.transport.TransportProtos.ComponentLifecycleMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtx;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldStateService;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.TsRollingArgumentEntry;
import org.thingsboard.server.service.cf.telemetry.CalculatedFieldAttributeUpdateRequest;
import org.thingsboard.server.service.cf.telemetry.CalculatedFieldTelemetryUpdateRequest;
import org.thingsboard.server.service.cf.telemetry.CalculatedFieldTimeSeriesUpdateRequest;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldExecutionService extends AbstractPartitionBasedService<CalculatedFieldId> implements CalculatedFieldExecutionService {

    public static final TbQueueCallback DUMMY_TB_QUEUE_CALLBACK = new TbQueueCallback() {
        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
        }

        @Override
        public void onFailure(Throwable t) {
        }
    };

    private final CalculatedFieldService calculatedFieldService;
    private final TbAssetProfileCache assetProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;
    private final CalculatedFieldCache calculatedFieldCache;
    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private final CalculatedFieldStateService stateService;
    private final TbClusterService clusterService;

    private ListeningExecutorService calculatedFieldExecutor;
    private ListeningExecutorService calculatedFieldCallbackExecutor;

    private final ConcurrentMap<CalculatedFieldEntityCtxId, CalculatedFieldEntityCtx> states = new ConcurrentHashMap<>();

    private static final int MAX_LAST_RECORDS_VALUE = 1024;

    private static final Set<EntityType> supportedReferencedEntities = EnumSet.of(
            EntityType.DEVICE, EntityType.ASSET, EntityType.CUSTOMER, EntityType.TENANT
    );

    @Value("${calculatedField.initFetchPackSize:50000}")
    @Getter
    private int initFetchPackSize;

    @PostConstruct
    public void init() {
        super.init();
        calculatedFieldExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field"));
        calculatedFieldCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field-callback"));
    }

    @PreDestroy
    public void stop() {
        super.stop();
        if (calculatedFieldExecutor != null) {
            calculatedFieldExecutor.shutdownNow();
        }
        if (calculatedFieldCallbackExecutor != null) {
            calculatedFieldCallbackExecutor.shutdownNow();
        }
    }

    @Override
    protected String getServiceName() {
        return "Calculated Field Execution";
    }

    @Override
    protected String getSchedulerExecutorName() {
        return "calculated-field-scheduled";
    }

    @Override
    public void pushRequestToQueue(TimeseriesSaveRequest request, TimeseriesSaveResult result, FutureCallback<Void> callback) {
        var tenantId = request.getTenantId();
        var entityId = request.getEntityId();
        //TODO: 1. check that request entity has calculated fields for entity or profile. If yes - push to corresponding partitions;
        //TODO: 2. check that request entity has calculated field links. If yes - push to corresponding partitions;
        //TODO: in 1 and 2 we should do the check as quick as possible. Should we also check the field/link keys?;
        checkEntityAndPushToQueue(tenantId, entityId, cf -> cf.matches(request.getEntries()), cf -> cf.linkMatches(entityId, request.getEntries()),
                () -> toCalculatedFieldTelemetryMsgProto(request, result), callback);
    }

    @Override
    public void pushRequestToQueue(AttributesSaveRequest request, List<Long> result, FutureCallback<Void> callback) {
        var tenantId = request.getTenantId();
        var entityId = request.getEntityId();
        checkEntityAndPushToQueue(tenantId, entityId, cf -> cf.matches(request.getEntries(), request.getScope()), cf -> cf.linkMatches(entityId, request.getEntries(), request.getScope()),
                () -> toCalculatedFieldTelemetryMsgProto(request, result), callback);
    }

    private void checkEntityAndPushToQueue(TenantId tenantId, EntityId entityId,
                                           Predicate<CalculatedFieldCtx> mainEntityFilter, Predicate<CalculatedFieldCtx> linkedEntityFilter,
                                           Supplier<ToCalculatedFieldMsg> msg, FutureCallback<Void> callback) {
        boolean send = checkEntityForCalculatedFields(tenantId, entityId, mainEntityFilter, linkedEntityFilter);
        if (send) {
            clusterService.pushMsgToCalculatedFields(tenantId, entityId, msg.get(), wrap(callback));
        } else {
            if (callback != null) {
                callback.onSuccess(null);
            }
        }
    }

    private boolean checkEntityForCalculatedFields(TenantId tenantId, EntityId entityId, Predicate<CalculatedFieldCtx> filter, Predicate<CalculatedFieldCtx> linkedEntityFilter) {
        boolean send = false;
        if (supportedReferencedEntities.contains(entityId.getEntityType())) {
            send = calculatedFieldCache.getCalculatedFieldCtxsByEntityId(entityId).stream().anyMatch(filter);
            if (!send) {
                send = calculatedFieldCache.getCalculatedFieldCtxsByEntityId(getProfileId(tenantId, entityId)).stream().anyMatch(filter);
            }
            if (!send) {
                send = calculatedFieldCache.getCalculatedFieldLinksByEntityId(entityId).stream()
                        .map(CalculatedFieldLink::getCalculatedFieldId)
                        .map(calculatedFieldCache::getCalculatedFieldCtx)
                        .anyMatch(linkedEntityFilter);
            }
        }
        return send;
    }

    @Override
    public ListenableFuture<CalculatedFieldState> fetchStateFromDb(CalculatedFieldCtx ctx, EntityId entityId) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        for (var entry : ctx.getArguments().entrySet()) {
            var argEntityId = entry.getValue().getRefEntityId() != null ? entry.getValue().getRefEntityId() : entityId;
            var argValueFuture = fetchKvEntry(ctx.getTenantId(), argEntityId, entry.getValue());
            argFutures.put(entry.getKey(), argValueFuture);
        }
        return Futures.whenAllComplete(argFutures.values()).call(() -> {
            var result = createStateByType(ctx.getCfType());
            result.updateState(argFutures.entrySet().stream()
                    .collect(Collectors.toMap(
                            Entry::getKey, // Keep the key as is
                            entry -> {
                                try {
                                    // Resolve the future to get the value
                                    return entry.getValue().get();
                                } catch (ExecutionException | InterruptedException e) {
                                    throw new RuntimeException("Error getting future result for key: " + entry.getKey(), e);
                                }
                            }
                    )));
            return result;
        }, calculatedFieldCallbackExecutor);
    }

    @Override
    public void pushStateToStorage(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback) {
        stateService.persistState(stateId, state, callback);
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        var result = new HashMap<TopicPartitionInfo, List<ListenableFuture<?>>>();
//        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
//        Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> tpiTargetEntityMap = new HashMap<>();
//
//        for (CalculatedField cf : cfs) {
//
//            Consumer<EntityId> resolvePartition = entityId -> {
//                TopicPartitionInfo tpi;
//                try {
//                    tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, cf.getTenantId(), entityId);
//                    if (addedPartitions.contains(tpi) && states.keySet().stream().noneMatch(ctxId -> ctxId.cfId().equals(cf.getId()))) {
//                        tpiTargetEntityMap.computeIfAbsent(tpi, k -> new ArrayList<>()).add(new CalculatedFieldEntityCtxId(cf.getId(), entityId));
//                    }
//                } catch (Exception e) {
//                    log.warn("Failed to resolve partition for CalculatedFieldEntityCtxId: entityId=[{}], tenantId=[{}]. Reason: {}",
//                            entityId, cf.getTenantId(), e.getMessage());
//                }
//            };
//
//            EntityId cfEntityId = cf.getEntityId();
//            if (isProfileEntity(cfEntityId)) {
//                calculatedFieldCache.getEntitiesByProfile(cf.getTenantId(), cfEntityId).forEach(resolvePartition);
//            } else {
//                resolvePartition.accept(cfEntityId);
//            }
//        }
//
//        for (var entry : tpiTargetEntityMap.entrySet()) {
//            for (List<CalculatedFieldEntityCtxId> partition : Lists.partition(entry.getValue(), 1000)) {
//                log.info("[{}] Submit task for CalculatedFields: {}", entry.getKey(), partition.size());
//                var future = calculatedFieldExecutor.submit(() -> {
//                    try {
//                        for (CalculatedFieldEntityCtxId ctxId : partition) {
//                            restoreState(ctxId.cfId(), ctxId.entityId());
//                        }
//                    } catch (Throwable t) {
//                        log.error("Unexpected exception while restoring CalculatedField states", t);
//                        throw t;
//                    }
//                });
//                result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(future);
//            }
//        }
        return result;
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(CalculatedFieldId entityId) {
        cleanupEntity(entityId);
    }

    private void cleanupEntity(CalculatedFieldId calculatedFieldId) {
        states.keySet().removeIf(ctxId -> ctxId.cfId().equals(calculatedFieldId));
    }

    @Override
    public void onCalculatedFieldLifecycleMsg(ComponentLifecycleMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            log.info("Received CalculatedFieldMsgProto for processing: tenantId=[{}], calculatedFieldId=[{}]", tenantId, calculatedFieldId);
            ComponentLifecycleEvent event = proto.getEvent();
            if (ComponentLifecycleEvent.DELETED.equals(event)) {
                log.warn("Executing onCalculatedFieldDelete, calculatedFieldId=[{}]", calculatedFieldId);
                calculatedFieldCache.evict(calculatedFieldId);
                onCalculatedFieldDelete(calculatedFieldId, callback);
                callback.onSuccess();
            }
            CalculatedField cf = calculatedFieldService.findById(tenantId, calculatedFieldId);
            if (ComponentLifecycleEvent.UPDATED.equals(event)) {
                log.info("Executing onCalculatedFieldUpdate, calculatedFieldId=[{}]", calculatedFieldId);
                boolean shouldReinit = onCalculatedFieldUpdate(cf, callback);
                if (!shouldReinit) {
                    return;
                }
            }
            if (cf != null) {
                calculatedFieldCache.addCalculatedField(tenantId, calculatedFieldId);
                EntityId entityId = cf.getEntityId();
                CalculatedFieldCtx calculatedFieldCtx = calculatedFieldCache.getCalculatedFieldCtx(calculatedFieldId);
                switch (entityId.getEntityType()) {
                    case ASSET, DEVICE -> {
                        log.info("Initializing state for entity: tenantId=[{}], entityId=[{}]", tenantId, entityId);
                        initializeStateForEntity(calculatedFieldCtx, entityId, callback);
                    }
                    case ASSET_PROFILE, DEVICE_PROFILE -> {
                        log.info("Initializing state for all entities in profile: tenantId=[{}], profileId=[{}]", tenantId, entityId);
                        Map<String, Argument> commonArguments = calculatedFieldCtx.getArguments().entrySet().stream()
                                .filter(entry -> entry.getValue().getRefEntityId() != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        fetchArguments(tenantId, entityId, commonArguments, commonArgs -> {
                            calculatedFieldCache.getEntitiesByProfile(tenantId, entityId).forEach(targetEntityId -> {
                                initializeStateForEntity(calculatedFieldCtx, targetEntityId, commonArgs, callback);
                            });
                        });
                    }
                    default ->
                            throw new IllegalArgumentException("Entity type '" + calculatedFieldId.getEntityType() + "' does not support calculated fields.");
                }
            } else {
                //Calculated field was probably deleted while message was in queue;
                log.warn("Calculated field not found, possibly deleted: {}", calculatedFieldId);
                callback.onSuccess();
            }
            callback.onSuccess();
            log.info("Successfully processed calculated field message for calculatedFieldId: [{}]", calculatedFieldId);
        } catch (Exception e) {
            log.trace("Failed to process calculated field msg: [{}]", proto, e);
            callback.onFailure(e);
        }
    }

    private boolean onCalculatedFieldUpdate(CalculatedField updatedCalculatedField, TbCallback callback) {
        CalculatedField oldCalculatedField = calculatedFieldCache.getCalculatedField(updatedCalculatedField.getId());
        boolean shouldReinit = true;
        if (hasSignificantChanges(oldCalculatedField, updatedCalculatedField)) {
            onCalculatedFieldDelete(updatedCalculatedField.getId(), callback);
        } else {
            calculatedFieldCache.updateCalculatedField(updatedCalculatedField.getTenantId(), updatedCalculatedField.getId());
            callback.onSuccess();
            shouldReinit = false;
        }
        return shouldReinit;
    }

    private void onCalculatedFieldDelete(CalculatedFieldId calculatedFieldId, TbCallback callback) {
        try {
            cleanupEntity(calculatedFieldId);
            states.keySet().removeIf(ctxId -> {
                if (ctxId.cfId().equals(calculatedFieldId)) {
                    stateService.removeState(ctxId);
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            log.trace("Failed to delete calculated field: [{}]", calculatedFieldId, e);
            callback.onFailure(e);
        }
    }

    private boolean hasSignificantChanges(CalculatedField oldCalculatedField, CalculatedField newCalculatedField) {
        if (oldCalculatedField == null) {
            return true;
        }
        boolean entityIdChanged = !oldCalculatedField.getEntityId().equals(newCalculatedField.getEntityId());
        boolean typeChanged = !oldCalculatedField.getType().equals(newCalculatedField.getType());
        boolean argumentsChanged = !oldCalculatedField.getConfiguration().getArguments().equals(newCalculatedField.getConfiguration().getArguments());

        return entityIdChanged || typeChanged || argumentsChanged;
    }

    @Override
    public void onTelemetryUpdate(CalculatedFieldTelemetryMsgProto proto, TbCallback callback) {
        try {
            CalculatedFieldTelemetryUpdateRequest request = fromProto(proto);
            EntityId entityId = request.getEntityId();

            if (supportedReferencedEntities.contains(entityId.getEntityType())) {
                TenantId tenantId = request.getTenantId();
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, entityId);

                if (tpi.isMyPartition()) {

                    processCalculatedFields(request, entityId);
                    processCalculatedFields(request, getProfileId(tenantId, entityId));

                    Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> tpiStatesToUpdate = new HashMap<>();
                    processCalculatedFieldLinks(request, tpiStatesToUpdate);
                    if (!tpiStatesToUpdate.isEmpty()) {
                        tpiStatesToUpdate.forEach((topicPartitionInfo, ctxIds) -> {
                            CalculatedFieldLinkedTelemetryMsgProto linkedTelemetryMsgProto = buildLinkedTelemetryMsgProto(proto, ctxIds);
                            clusterService.pushMsgToCalculatedFields(topicPartitionInfo, UUID.randomUUID(), ToCalculatedFieldMsg.newBuilder().setLinkedTelemetryMsg(linkedTelemetryMsgProto).build(), null);
                        });
                    }
                } else {
                    clusterService.pushMsgToCalculatedFields(tpi, UUID.randomUUID(), ToCalculatedFieldMsg.newBuilder().setTelemetryMsg(proto).build(), null);
                }
            }
        } catch (Exception e) {
            log.trace("Failed to update telemetry.", e);
        }
    }

    private void processCalculatedFields(CalculatedFieldTelemetryUpdateRequest request, EntityId cfTargetEntityId) {
        if (cfTargetEntityId != null) {
            calculatedFieldCache.getCalculatedFieldCtxsByEntityId(cfTargetEntityId).forEach(ctx -> {
                Map<String, KvEntry> updatedTelemetry = request.getMappedTelemetry(ctx, cfTargetEntityId);
                if (!updatedTelemetry.isEmpty()) {
                    EntityId targetEntityId = isProfileEntity(cfTargetEntityId) ? request.getEntityId() : cfTargetEntityId;
                    executeTelemetryUpdate(ctx, targetEntityId, request.getPreviousCalculatedFieldIds(), updatedTelemetry);
                }
            });
        }
    }

    private void processCalculatedFieldLinks(CalculatedFieldTelemetryUpdateRequest request, Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> tpiStates) {
        TenantId tenantId = request.getTenantId();
        EntityId entityId = request.getEntityId();

        calculatedFieldCache.getCalculatedFieldLinksByEntityId(entityId)
                .forEach(link -> {
                    CalculatedFieldId calculatedFieldId = link.getCalculatedFieldId();
                    CalculatedFieldCtx ctx = calculatedFieldCache.getCalculatedFieldCtx(calculatedFieldId);
                    EntityId targetEntityId = ctx.getEntityId();

                    if (isProfileEntity(targetEntityId)) {
                        calculatedFieldCache.getEntitiesByProfile(tenantId, targetEntityId).forEach(entityByProfile -> {
                            processCalculatedFieldLink(request, entityByProfile, ctx, tpiStates);
                        });
                    } else {
                        processCalculatedFieldLink(request, targetEntityId, ctx, tpiStates);
                    }
                });
    }

    private void processCalculatedFieldLink(CalculatedFieldTelemetryUpdateRequest request, EntityId targetEntity, CalculatedFieldCtx ctx, Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> tpiStates) {
        TopicPartitionInfo targetEntityTpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, request.getTenantId(), targetEntity);
        if (targetEntityTpi.isMyPartition()) {
            Map<String, KvEntry> updatedTelemetry = request.getMappedTelemetry(ctx, request.getEntityId());
            if (!updatedTelemetry.isEmpty()) {
                executeTelemetryUpdate(ctx, targetEntity, request.getPreviousCalculatedFieldIds(), updatedTelemetry);
            }
        } else {
            List<CalculatedFieldEntityCtxId> ctxIds = tpiStates.computeIfAbsent(targetEntityTpi, k -> new ArrayList<>());
            ctxIds.add(new CalculatedFieldEntityCtxId(ctx.getTenantId(), ctx.getCfId(), targetEntity));
        }
    }

    @Override
    public void onTelemetryUpdate(CalculatedFieldLinkedTelemetryMsgProto proto, TbCallback callback) {
        try {
            CalculatedFieldTelemetryUpdateRequest request = fromProto(proto.getMsg());

            if (proto.getLinksList().isEmpty()) {
                onTelemetryUpdate(proto, callback);
                return;
            }

            proto.getLinksList().forEach(ctxIdProto -> {
                CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(ctxIdProto.getCalculatedFieldIdMSB(), ctxIdProto.getCalculatedFieldIdLSB()));
                CalculatedFieldCtx ctx = calculatedFieldCache.getCalculatedFieldCtx(calculatedFieldId);

                Map<String, KvEntry> updatedTelemetry = request.getMappedTelemetry(ctx, request.getEntityId());
                if (!updatedTelemetry.isEmpty()) {
                    EntityId targetEntityId = EntityIdFactory.getByTypeAndUuid(ctxIdProto.getEntityType(), new UUID(ctxIdProto.getEntityIdMSB(), ctxIdProto.getEntityIdLSB()));
                    executeTelemetryUpdate(ctx, targetEntityId, request.getPreviousCalculatedFieldIds(), updatedTelemetry);
                }
            });
        } catch (Exception e) {
            log.trace("Failed to process telemetry update msg: [{}]", proto, e);
        }
    }

    private void executeTelemetryUpdate(CalculatedFieldCtx cfCtx, EntityId entityId, List<CalculatedFieldId> previousCalculatedFieldIds, Map<String, KvEntry> updatedTelemetry) {
        log.info("Received telemetry update msg: tenantId=[{}], entityId=[{}], calculatedFieldId=[{}]", cfCtx.getTenantId(), entityId, cfCtx.getCfId());
        Map<String, ArgumentEntry> argumentValues = updatedTelemetry.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> ArgumentEntry.createSingleValueArgument(entry.getValue())));

//        updateOrInitializeState(cfCtx, entityId, argumentValues, previousCalculatedFieldIds);
    }

    @Override
    public void onEntityUpdateMsg(CalculatedFieldEntityUpdateMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));

            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, entityId);
            if (tpi.isMyPartition()) {
                log.info("Received CalculatedFieldEntityUpdateMsgProto for processing: tenantId=[{}], entityId=[{}]", tenantId, entityId);
                if (proto.getDeleted()) {
                    log.info("Executing CalculatedFieldEntityUpdateMsgProto msg: entity deleted from profile, tenantId=[{}], entityId=[{}]", tenantId, entityId);

                    EntityId oldProfileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getOldProfileIdMSB(), proto.getOldProfileIdLSB()));
                    calculatedFieldCache.getCalculatedFieldsByEntityId(entityId).forEach(cf -> clearState(cf.getId(), entityId));
                    calculatedFieldCache.getCalculatedFieldsByEntityId(oldProfileId).forEach(cf -> clearState(cf.getId(), entityId));
                }
                if (proto.getAdded()) {
                    log.info("Executing CalculatedFieldEntityUpdateMsgProto msg: entity added to profile, tenantId=[{}], entityId=[{}]", tenantId, entityId);

                    EntityId newProfileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getNewProfileIdMSB(), proto.getNewProfileIdLSB()));
                    initializeStateForEntityByProfile(entityId, newProfileId, callback);
                }
                if (proto.getUpdated()) {
                    log.info("Executing CalculatedFieldEntityUpdateMsgProto msg: entity changed the profile, tenantId=[{}], entityId=[{}]", tenantId, entityId);

                    EntityId oldProfileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getOldProfileIdMSB(), proto.getOldProfileIdLSB()));
                    EntityId newProfileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getNewProfileIdMSB(), proto.getNewProfileIdLSB()));

                    calculatedFieldCache.getCalculatedFieldsByEntityId(oldProfileId).forEach(cf -> clearState(cf.getId(), entityId));
                    initializeStateForEntityByProfile(entityId, newProfileId, callback);
                }
            } else {
                clusterService.pushNotificationToCalculatedFields(tenantId, entityId, ToCalculatedFieldNotificationMsg.newBuilder().setEntityUpdateMsg(proto).build(), null);
            }
        } catch (Exception e) {
            log.trace("Failed to process entity update msg: [{}]", proto, e);
        }
    }

    private void clearState(CalculatedFieldId calculatedFieldId, EntityId entityId) {
        log.warn("Executing clearState, calculatedFieldId=[{}], entityId=[{}]", calculatedFieldId, entityId);
    }

    private void initializeStateForEntityByProfile(EntityId entityId, EntityId profileId, TbCallback callback) {
        calculatedFieldCache.getCalculatedFieldsByEntityId(profileId).stream()
                .map(cf -> calculatedFieldCache.getCalculatedFieldCtx(cf.getId()))
                .forEach(cfCtx -> initializeStateForEntity(cfCtx, entityId, callback));
    }

    private void initializeStateForEntity(CalculatedFieldCtx calculatedFieldCtx, EntityId entityId, TbCallback callback) {
        initializeStateForEntity(calculatedFieldCtx, entityId, new HashMap<>(), callback);
    }

    private void initializeStateForEntity(CalculatedFieldCtx calculatedFieldCtx, EntityId entityId, Map<String, ArgumentEntry> commonArguments, TbCallback callback) {
        Map<String, ArgumentEntry> argumentValues = new HashMap<>(commonArguments);
        List<ListenableFuture<ArgumentEntry>> futures = new ArrayList<>();

        calculatedFieldCtx.getArguments().forEach((key, argument) -> {
            if (!commonArguments.containsKey(key)) {
                futures.add(Futures.transform(fetchArgumentValue(calculatedFieldCtx.getTenantId(), entityId, argument),
                        result -> {
                            argumentValues.put(key, result);
                            return result;
                        }, calculatedFieldCallbackExecutor));
            }
        });

        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
            @Override
            public void onSuccess(List<ArgumentEntry> results) {
//                updateOrInitializeState(calculatedFieldCtx, entityId, argumentValues, new ArrayList<>());
                callback.onSuccess();
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to initialize state for entity: [{}]", entityId, t);
                callback.onFailure(t);
            }
        }, calculatedFieldCallbackExecutor);
    }

//    private void updateOrInitializeState(CalculatedFieldCtx calculatedFieldCtx, EntityId entityId, Map<String, ArgumentEntry> argumentValues, List<CalculatedFieldId> previousCalculatedFieldIds) {
//        CalculatedFieldId cfId = calculatedFieldCtx.getCfId();
//        Map<String, ArgumentEntry> argumentsMap = new HashMap<>(argumentValues);
//
//        CalculatedFieldEntityCtxId entityCtxId = new CalculatedFieldEntityCtxId(cfId, entityId);
//
//        states.compute(entityCtxId, (ctxId, ctx) -> {
//            CalculatedFieldEntityCtx calculatedFieldEntityCtx = ctx != null ? ctx : fetchCalculatedFieldEntityState(ctxId, calculatedFieldCtx.getCfType());
//
//            CompletableFuture<Void> updateFuture = new CompletableFuture<>();
//
//            Consumer<CalculatedFieldState> performUpdateState = (state) -> {
//                if (state.updateState(argumentsMap)) {
//                    calculatedFieldEntityCtx.setState(state);
//                    stateService.persistState(entityCtxId, calculatedFieldEntityCtx);
//                    Map<String, ArgumentEntry> arguments = state.getArguments();
//                    boolean allArgsPresent = arguments.keySet().containsAll(calculatedFieldCtx.getArguments().keySet()) &&
//                            !arguments.containsValue(SingleValueArgumentEntry.EMPTY) && !arguments.containsValue(TsRollingArgumentEntry.EMPTY);
//                    if (allArgsPresent) {
//                        performCalculation(calculatedFieldCtx, state, entityId, previousCalculatedFieldIds);
//                    }
//                    log.info("Successfully updated state: calculatedFieldId=[{}], entityId=[{}]", calculatedFieldCtx.getCfId(), entityId);
//                }
//                updateFuture.complete(null);
//            };
//
//            CalculatedFieldState state = calculatedFieldEntityCtx.getState();
//
//            boolean allKeysPresent = argumentsMap.keySet().containsAll(calculatedFieldCtx.getArguments().keySet());
//            boolean requiresTsRollingUpdate = calculatedFieldCtx.getArguments().values().stream()
//                    .anyMatch(argument -> ArgumentType.TS_ROLLING.equals(argument.getRefEntityKey().getType()) && state.getArguments().get(argument.getRefEntityKey().getKey()) == null);
//
//            if (!allKeysPresent || requiresTsRollingUpdate) {
//                Map<String, Argument> missingArguments = calculatedFieldCtx.getArguments().entrySet().stream()
//                        .filter(entry -> !argumentsMap.containsKey(entry.getKey()) || (ArgumentType.TS_ROLLING.equals(entry.getValue().getRefEntityKey().getType()) && state.getArguments().get(entry.getKey()) == null))
//                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//                fetchArguments(calculatedFieldCtx.getTenantId(), entityId, missingArguments, argumentsMap::putAll)
//                        .addListener(() -> performUpdateState.accept(state),
//                                calculatedFieldCallbackExecutor);
//            } else {
//                performUpdateState.accept(state);
//            }
//
//            try {
//                updateFuture.join();
//            } catch (Exception e) {
//                log.trace("Failed to update state for ctxId [{}].", ctxId, e);
//                throw new RuntimeException("Failed to update or initialize state.", e);
//            }
//
//            return calculatedFieldEntityCtx;
//        });
//    }

    @Override
    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, CalculatedFieldResult calculatedFieldResult, List<CalculatedFieldId> cfIds, TbCallback callback) {
        try {
            OutputType type = calculatedFieldResult.getType();
            TbMsgType msgType = OutputType.ATTRIBUTES.equals(type) ? TbMsgType.POST_ATTRIBUTES_REQUEST : TbMsgType.POST_TELEMETRY_REQUEST;
            TbMsgMetaData md = OutputType.ATTRIBUTES.equals(type) ? new TbMsgMetaData(Map.of(SCOPE, calculatedFieldResult.getScope().name())) : TbMsgMetaData.EMPTY;
            ObjectNode payload = createJsonPayload(calculatedFieldResult);
            TbMsg msg = TbMsg.newMsg().type(msgType).originator(entityId).previousCalculatedFieldIds(cfIds).metaData(md).data(JacksonUtil.writeValueAsString(payload)).build();
            clusterService.pushMsgToRuleEngine(tenantId, entityId, msg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    callback.onSuccess();
                    log.trace("[{}][{}] Pushed message to rule engine: {} ", tenantId, entityId, msg);
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onFailure(t);
                }
            });
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push message to rule engine. CalculatedFieldResult: {}", tenantId, entityId, calculatedFieldResult, e);
        }
    }

    private ListenableFuture<Void> fetchArguments(TenantId tenantId, EntityId entityId, Map<String, Argument> necessaryArguments, Consumer<Map<String, ArgumentEntry>> onComplete) {
        Map<String, ArgumentEntry> argumentValues = new HashMap<>();
        List<ListenableFuture<ArgumentEntry>> futures = new ArrayList<>();
        necessaryArguments.forEach((key, argument) -> {
            futures.add(Futures.transform(fetchArgumentValue(tenantId, entityId, argument),
                    result -> {
                        argumentValues.put(key, result);
                        return result;
                    }, calculatedFieldCallbackExecutor));
        });
        return Futures.transform(Futures.allAsList(futures), results -> {
            onComplete.accept(argumentValues);
            return null;
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchArgumentValue(TenantId tenantId, EntityId targetEntityId, Argument argument) {
        EntityId argumentEntityId = argument.getRefEntityId();
        EntityId entityId = (argumentEntityId == null || isProfileEntity(argumentEntityId))
                ? targetEntityId
                : argumentEntityId;
        return fetchKvEntry(tenantId, entityId, argument);
    }

    private ListenableFuture<ArgumentEntry> fetchKvEntry(TenantId tenantId, EntityId entityId, Argument argument) {
        return switch (argument.getRefEntityKey().getType()) {
            case TS_ROLLING -> fetchTsRolling(tenantId, entityId, argument);
            case ATTRIBUTE -> transformSingleValueArgument(
                    Futures.transform(
                            attributesService.find(tenantId, entityId, argument.getRefEntityKey().getScope(), argument.getRefEntityKey().getKey()),
                            result -> result.or(() -> Optional.of(new BaseAttributeKvEntry(createDefaultKvEntry(argument), System.currentTimeMillis(), 0L))),
                            calculatedFieldCallbackExecutor)
            );
            case TS_LATEST -> transformSingleValueArgument(
                    Futures.transform(
                            timeseriesService.findLatest(tenantId, entityId, argument.getRefEntityKey().getKey()),
                            result -> result.or(() -> Optional.of(new BasicTsKvEntry(System.currentTimeMillis(), createDefaultKvEntry(argument), 0L))),
                            calculatedFieldCallbackExecutor));
        };
    }

    private ListenableFuture<ArgumentEntry> transformSingleValueArgument(ListenableFuture<Optional<? extends KvEntry>> kvEntryFuture) {
        return Futures.transform(kvEntryFuture, kvEntry -> {
            if (kvEntry.isPresent() && kvEntry.get().getValue() != null) {
                return ArgumentEntry.createSingleValueArgument(kvEntry.get());
            } else {
                return SingleValueArgumentEntry.EMPTY;
            }
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchTsRolling(TenantId tenantId, EntityId entityId, Argument argument) {
        long currentTime = System.currentTimeMillis();
        long timeWindow = argument.getTimeWindow() == 0 ? System.currentTimeMillis() : argument.getTimeWindow();
        long startTs = currentTime - timeWindow;
        int limit = argument.getLimit() == 0 ? MAX_LAST_RECORDS_VALUE : argument.getLimit();

        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startTs, currentTime, 0, limit, Aggregation.NONE);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsRollingFuture, tsRolling -> tsRolling == null ? TsRollingArgumentEntry.EMPTY : ArgumentEntry.createTsRollingArgument(tsRolling), calculatedFieldCallbackExecutor);
    }

    private KvEntry createDefaultKvEntry(Argument argument) {
        String key = argument.getRefEntityKey().getKey();
        String defaultValue = argument.getDefaultValue();
        if (NumberUtils.isParsable(defaultValue)) {
            return new DoubleDataEntry(key, Double.parseDouble(defaultValue));
        }
        if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
            return new BooleanDataEntry(key, Boolean.parseBoolean(defaultValue));
        }
        return new StringDataEntry(key, defaultValue);
    }

    private ObjectNode createJsonPayload(CalculatedFieldResult calculatedFieldResult) {
        ObjectNode payload = JacksonUtil.newObjectNode();
        Map<String, Object> resultMap = calculatedFieldResult.getResultMap();
        resultMap.forEach((k, v) -> payload.set(k, JacksonUtil.convertValue(v, JsonNode.class)));
        return payload;
    }

    private CalculatedFieldState createStateByType(CalculatedFieldType calculatedFieldType) {
        return switch (calculatedFieldType) {
            case SIMPLE -> new SimpleCalculatedFieldState();
            case SCRIPT -> new ScriptCalculatedFieldState();
        };
    }

    private boolean isProfileEntity(EntityId entityId) {
        return EntityType.DEVICE_PROFILE.equals(entityId.getEntityType()) || EntityType.ASSET_PROFILE.equals(entityId.getEntityType());
    }

    private EntityId getProfileId(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case ASSET -> assetProfileCache.get(tenantId, (AssetId) entityId).getId();
            case DEVICE -> deviceProfileCache.get(tenantId, (DeviceId) entityId).getId();
            default -> null;
        };
    }

    private ToCalculatedFieldMsg toCalculatedFieldTelemetryMsgProto(TimeseriesSaveRequest request, TimeseriesSaveResult result) {
        ToCalculatedFieldMsg.Builder msg = ToCalculatedFieldMsg.newBuilder();

        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = buildTelemetryMsgProto(request.getTenantId(), request.getEntityId(), request.getPreviousCalculatedFieldIds());
        List<TsKvEntry> entries = request.getEntries();
        List<Long> versions = result.getVersions();
        for (int i = 0; i < entries.size(); i++) {
            long tsVersion = versions.get(i);
            TsKvProto tsProto = ProtoUtils.toTsKvProto(entries.get(i)).toBuilder().setVersion(tsVersion).build();
            telemetryMsg.addTsData(tsProto);
        }
        msg.setTelemetryMsg(telemetryMsg.build());

        return msg.build();
    }

    private ToCalculatedFieldMsg toCalculatedFieldTelemetryMsgProto(AttributesSaveRequest request, List<Long> versions) {
        ToCalculatedFieldMsg.Builder msg = ToCalculatedFieldMsg.newBuilder();

        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = buildTelemetryMsgProto(request.getTenantId(), request.getEntityId(), request.getPreviousCalculatedFieldIds());
        telemetryMsg.setScope(AttributeScopeProto.valueOf(request.getScope().name()));
        List<AttributeKvEntry> entries = request.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            long attrVersion = versions.get(i);
            AttributeValueProto attrProto = ProtoUtils.toProto(entries.get(i)).toBuilder().setVersion(attrVersion).build();
            telemetryMsg.addAttrData(attrProto);
        }
        msg.setTelemetryMsg(telemetryMsg.build());

        return msg.build();
    }

    private CalculatedFieldTelemetryMsgProto.Builder buildTelemetryMsgProto(TenantId tenantId, EntityId entityId, List<CalculatedFieldId> calculatedFieldIds) {
        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = CalculatedFieldTelemetryMsgProto.newBuilder();

        telemetryMsg.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        telemetryMsg.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());

        telemetryMsg.setEntityType(entityId.getEntityType().name());
        telemetryMsg.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        telemetryMsg.setEntityIdLSB(entityId.getId().getLeastSignificantBits());

        for (CalculatedFieldId cfId : calculatedFieldIds) {
            telemetryMsg.addPreviousCalculatedFields(toProto(cfId));
        }

        return telemetryMsg;
    }

    private CalculatedFieldLinkedTelemetryMsgProto buildLinkedTelemetryMsgProto(CalculatedFieldTelemetryMsgProto telemetryProto, List<CalculatedFieldEntityCtxId> links) {
        TransportProtos.CalculatedFieldLinkedTelemetryMsgProto.Builder builder = TransportProtos.CalculatedFieldLinkedTelemetryMsgProto.newBuilder();
        builder.setMsg(telemetryProto);
        for (CalculatedFieldEntityCtxId link : links) {
            builder.addLinks(toProto(link));
        }
        return builder.build();
    }

    private TransportProtos.CalculatedFieldEntityCtxIdProto toProto(CalculatedFieldEntityCtxId ctxId) {
        return TransportProtos.CalculatedFieldEntityCtxIdProto.newBuilder()
                .setCalculatedFieldIdMSB(ctxId.cfId().getId().getMostSignificantBits())
                .setCalculatedFieldIdLSB(ctxId.cfId().getId().getLeastSignificantBits())
                .setEntityType(ctxId.entityId().getEntityType().name())
                .setEntityIdMSB(ctxId.entityId().getId().getMostSignificantBits())
                .setEntityIdLSB(ctxId.entityId().getId().getLeastSignificantBits())
                .build();
    }

    private TransportProtos.CalculatedFieldIdProto toProto(CalculatedFieldId cfId) {
        return TransportProtos.CalculatedFieldIdProto.newBuilder()
                .setCalculatedFieldIdMSB(cfId.getId().getMostSignificantBits())
                .setCalculatedFieldIdLSB(cfId.getId().getLeastSignificantBits())
                .build();
    }

    private CalculatedFieldTelemetryUpdateRequest fromProto(CalculatedFieldTelemetryMsgProto proto) {
        TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));

        if (!proto.getTsDataList().isEmpty()) {
            List<TsKvEntry> updatedTelemetry = proto.getTsDataList().stream()
                    .map(ProtoUtils::fromProto)
                    .toList();
            return new CalculatedFieldTimeSeriesUpdateRequest(
                    tenantId, entityId, updatedTelemetry,
                    proto.getPreviousCalculatedFieldsList().stream()
                            .map(cfIdProto -> new CalculatedFieldId(
                                    new UUID(cfIdProto.getCalculatedFieldIdMSB(), cfIdProto.getCalculatedFieldIdLSB())))
                            .toList());
        } else {
            AttributeScope scope = AttributeScope.valueOf(proto.getScope().name());
            List<AttributeKvEntry> updatedTelemetry = proto.getAttrDataList().stream()
                    .map(ProtoUtils::fromProto)
                    .toList();
            return new CalculatedFieldAttributeUpdateRequest(
                    tenantId, entityId, scope, updatedTelemetry,
                    proto.getPreviousCalculatedFieldsList().stream()
                            .map(cfIdProto -> new CalculatedFieldId(
                                    new UUID(cfIdProto.getCalculatedFieldIdMSB(), cfIdProto.getCalculatedFieldIdLSB())))
                            .toList());
        }
    }

    private static TbQueueCallback wrap(FutureCallback<Void> callback) {
        if (callback != null) {
            return new FutureCallbackWrapper(callback);
        } else {
            return DUMMY_TB_QUEUE_CALLBACK;
        }
    }

    private static class FutureCallbackWrapper implements TbQueueCallback {
        private final FutureCallback<Void> callback;

        public FutureCallbackWrapper(FutureCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            callback.onSuccess(null);
        }

        @Override
        public void onFailure(Throwable t) {
            callback.onFailure(t);
        }
    }

}
