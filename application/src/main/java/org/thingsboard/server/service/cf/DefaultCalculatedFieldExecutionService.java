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
import com.google.common.collect.Lists;
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
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtx;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.TsRollingArgumentEntry;
import org.thingsboard.server.service.cf.telemetry.CalculatedFieldTelemetryUpdateRequest;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.util.ProtoUtils.fromObjectProto;
import static org.thingsboard.server.common.util.ProtoUtils.toObjectProto;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldExecutionService extends AbstractPartitionBasedService<CalculatedFieldId> implements CalculatedFieldExecutionService {

    private final CalculatedFieldService calculatedFieldService;
    private final TbAssetProfileCache assetProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;
    private final CalculatedFieldCache calculatedFieldCache;
    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private final RocksDBService rocksDBService;
    private final TbClusterService clusterService;
    private final TbelInvokeService tbelInvokeService;

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
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        var result = new HashMap<TopicPartitionInfo, List<ListenableFuture<?>>>();
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        Map<TopicPartitionInfo, List<CalculatedField>> tpiCalculatedFieldMap = new HashMap<>();

        for (CalculatedField cf : cfs) {
            TopicPartitionInfo tpi;
            try {
                tpi = partitionService.resolve(ServiceType.TB_CORE, cf.getTenantId(), cf.getId());
            } catch (Exception e) {
                log.warn("Failed to resolve partition for CalculatedField [{}], tenant id [{}]. Reason: {}",
                        cf.getId(), cf.getTenantId(), e.getMessage());
                continue;
            }
            if (addedPartitions.contains(tpi) && states.keySet().stream().noneMatch(ctxId -> ctxId.cfId().equals(cf.getId().getId()))) {
                tpiCalculatedFieldMap.computeIfAbsent(tpi, k -> new ArrayList<>()).add(cf);
            }
        }

        for (var entry : tpiCalculatedFieldMap.entrySet()) {
            for (List<CalculatedField> partition : Lists.partition(entry.getValue(), 1000)) {
                log.info("[{}] Submit task for CalculatedFields: {}", entry.getKey(), partition.size());
                var future = calculatedFieldExecutor.submit(() -> {
                    try {
                        for (CalculatedField cf : partition) {
                            EntityId cfEntityId = cf.getEntityId();
                            if (isProfileEntity(cfEntityId)) {
                                calculatedFieldCache.getEntitiesByProfile(cf.getTenantId(), cfEntityId)
                                        .forEach(entityId -> restoreState(cf, entityId));
                            } else {
                                restoreState(cf, cfEntityId);
                            }
                        }
                    } catch (Throwable t) {
                        log.error("Unexpected exception while restoring CalculatedField states", t);
                        throw t;
                    }
                });
                result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(future);
            }
        }
        return result;
    }

    private void restoreState(CalculatedField cf, EntityId entityId) {
        CalculatedFieldEntityCtxId ctxId = new CalculatedFieldEntityCtxId(cf.getId().getId(), entityId.getId());
        String storedState = rocksDBService.get(JacksonUtil.writeValueAsString(ctxId));

        if (storedState != null) {
            CalculatedFieldEntityCtx restoredCtx = JacksonUtil.fromString(storedState, CalculatedFieldEntityCtx.class);
            states.put(ctxId, restoredCtx);
            log.info("Restored state for CalculatedField [{}]", cf.getId());
        } else {
            log.warn("No state found for CalculatedField [{}], entity [{}].", cf.getId(), entityId);
        }
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(CalculatedFieldId entityId) {
        cleanupEntity(entityId);
    }

    private void cleanupEntity(CalculatedFieldId calculatedFieldId) {
        states.keySet().removeIf(ctxId -> ctxId.cfId().equals(calculatedFieldId.getId()));
    }

    @Override
    public void onCalculatedFieldMsg(TransportProtos.CalculatedFieldMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(proto.getCalculatedFieldIdMSB(), proto.getCalculatedFieldIdLSB()));
            log.info("Received CalculatedFieldMsgProto for processing: tenantId=[{}], calculatedFieldId=[{}]", tenantId, calculatedFieldId);
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, calculatedFieldId);
            if (!tpi.isMyPartition()) {
                clusterService.pushMsgToCore(tenantId, calculatedFieldId, TransportProtos.ToCoreMsg.newBuilder().setCalculatedFieldMsg(proto).build(), null);
                log.debug("[{}][{}] Calculated field belongs to external partition. Probably rebalancing is in progress. Topic: {}", tenantId, calculatedFieldId, tpi.getFullTopicName());
                callback.onFailure(new RuntimeException("Calculated field belongs to external partition " + tpi.getFullTopicName() + "!"));
            }
            if (proto.getDeleted()) {
                log.warn("Executing onCalculatedFieldDelete, calculatedFieldId=[{}]", calculatedFieldId);
                onCalculatedFieldDelete(tenantId, calculatedFieldId, callback);
                callback.onSuccess();
            }
            CalculatedField cf = calculatedFieldCache.getCalculatedField(tenantId, calculatedFieldId);
            if (proto.getUpdated()) {
                log.info("Executing onCalculatedFieldUpdate, calculatedFieldId=[{}]", calculatedFieldId);
                boolean shouldReinit = onCalculatedFieldUpdate(cf, callback);
                if (!shouldReinit) {
                    return;
                }
            }
            if (cf != null) {
                EntityId entityId = cf.getEntityId();
                CalculatedFieldCtx calculatedFieldCtx = calculatedFieldCache.getCalculatedFieldCtx(tenantId, calculatedFieldId, tbelInvokeService);
                switch (entityId.getEntityType()) {
                    case ASSET, DEVICE -> {
                        log.info("Initializing state for entity: tenantId=[{}], entityId=[{}]", tenantId, entityId);
                        initializeStateForEntity(calculatedFieldCtx, entityId, callback);
                    }
                    case ASSET_PROFILE, DEVICE_PROFILE -> {
                        log.info("Initializing state for all entities in profile: tenantId=[{}], profileId=[{}]", tenantId, entityId);
                        Map<String, Argument> commonArguments = calculatedFieldCtx.getArguments().entrySet().stream()
                                .filter(entry -> !isProfileEntity(entry.getValue().getEntityId()))
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
        CalculatedField oldCalculatedField = calculatedFieldCache.getCalculatedField(updatedCalculatedField.getTenantId(), updatedCalculatedField.getId());
        boolean shouldReinit = true;
        if (hasSignificantChanges(oldCalculatedField, updatedCalculatedField)) {
            onCalculatedFieldDelete(updatedCalculatedField.getTenantId(), updatedCalculatedField.getId(), callback);
        } else {
            callback.onSuccess();
            shouldReinit = false;
        }
        return shouldReinit;
    }

    private void onCalculatedFieldDelete(TenantId tenantId, CalculatedFieldId calculatedFieldId, TbCallback callback) {
        try {
            cleanupEntity(calculatedFieldId);
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, calculatedFieldId);
            Set<CalculatedFieldId> calculatedFieldIds = partitionedEntities.get(tpi);
            if (calculatedFieldIds != null) {
                calculatedFieldIds.remove(calculatedFieldId);
            }
            calculatedFieldCache.evict(calculatedFieldId);
            states.keySet().removeIf(ctxId -> ctxId.cfId().equals(calculatedFieldId.getId()));
            List<String> statesToRemove = states.keySet().stream()
                    .filter(ctxId -> ctxId.cfId().equals(calculatedFieldId.getId()))
                    .map(JacksonUtil::writeValueAsString)
                    .toList();
            rocksDBService.deleteAll(statesToRemove);
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
        CalculatedFieldConfiguration oldConfig = oldCalculatedField.getConfiguration();
        CalculatedFieldConfiguration newConfig = newCalculatedField.getConfiguration();
        boolean argumentsChanged = !oldConfig.getArguments().equals(newConfig.getArguments());
        boolean outputTypeChanged = !oldConfig.getOutput().getType().equals(newConfig.getOutput().getType());
        boolean expressionChanged = !oldConfig.getExpression().equals(newConfig.getExpression());

        return entityIdChanged || typeChanged || argumentsChanged || outputTypeChanged || expressionChanged;
    }

    @Override
    public void onTelemetryUpdate(CalculatedFieldTelemetryUpdateRequest calculatedFieldTelemetryUpdateRequest) {
        try {
            TenantId tenantId = calculatedFieldTelemetryUpdateRequest.getTenantId();
            EntityId entityId = calculatedFieldTelemetryUpdateRequest.getEntityId();
            AttributeScope scope = calculatedFieldTelemetryUpdateRequest.getScope();
            List<? extends KvEntry> telemetry = calculatedFieldTelemetryUpdateRequest.getKvEntries();
            List<CalculatedFieldId> calculatedFieldIds = calculatedFieldTelemetryUpdateRequest.getCalculatedFieldIds();

            if (supportedReferencedEntities.contains(entityId.getEntityType())) {
                EntityId profileId = getProfileId(tenantId, entityId);

                List<CalculatedFieldLink> cfLinks = Stream.concat(
                        calculatedFieldCache.getCalculatedFieldLinksByEntityId(tenantId, entityId).stream(),
                        profileId != null ? calculatedFieldCache.getCalculatedFieldLinksByEntityId(tenantId, profileId).stream() : Stream.empty()
                ).toList();

                cfLinks.forEach(link -> {
                    CalculatedFieldId calculatedFieldId = link.getCalculatedFieldId();
                    Map<String, String> telemetryKeys = getTelemetryKeysFromLink(link, scope);
                    Map<String, KvEntry> updatedTelemetry = telemetry.stream()
                            .filter(entry -> telemetryKeys.containsValue(entry.getKey()))
                            .collect(Collectors.toMap(
                                    entry -> getMappedKey(entry, telemetryKeys),
                                    entry -> entry,
                                    (v1, v2) -> v1
                            ));

                    if (!updatedTelemetry.isEmpty()) {
                        executeTelemetryUpdate(tenantId, entityId, calculatedFieldId, calculatedFieldIds, updatedTelemetry);
                    }
                });
            }
        } catch (Exception e) {
            log.trace("Failed to update telemetry.", e);
        }
    }

    private Map<String, String> getTelemetryKeysFromLink(CalculatedFieldLink link, AttributeScope scope) {
        return scope == null ? link.getConfiguration().getTimeSeries() : switch (scope) {
            case CLIENT_SCOPE -> link.getConfiguration().getClientAttributes();
            case SERVER_SCOPE -> link.getConfiguration().getServerAttributes();
            case SHARED_SCOPE -> link.getConfiguration().getSharedAttributes();
        };
    }

    private String getMappedKey(KvEntry entry, Map<String, String> telemetry) {
        return telemetry.entrySet().stream()
                .filter(kvEntry -> kvEntry.getValue().equals(entry.getKey()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(entry.getKey());
    }

    private void executeTelemetryUpdate(TenantId tenantId, EntityId entityId, CalculatedFieldId calculatedFieldId, List<CalculatedFieldId> calculatedFieldIds, Map<String, KvEntry> updatedTelemetry) {
        log.info("Received telemetry update msg: tenantId=[{}], entityId=[{}], calculatedFieldId=[{}]", tenantId, entityId, calculatedFieldId);
        CalculatedField calculatedField = calculatedFieldCache.getCalculatedField(tenantId, calculatedFieldId);
        CalculatedFieldCtx calculatedFieldCtx = calculatedFieldCache.getCalculatedFieldCtx(tenantId, calculatedFieldId, tbelInvokeService);
        Map<String, ArgumentEntry> argumentValues = updatedTelemetry.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> ArgumentEntry.createSingleValueArgument(entry.getValue())));

        EntityId cfEntityId = calculatedField.getEntityId();
        switch (cfEntityId.getEntityType()) {
            case ASSET_PROFILE, DEVICE_PROFILE -> {
                boolean isCommonEntity = calculatedField.getConfiguration().getReferencedEntities().contains(entityId);
                if (isCommonEntity) {
                    calculatedFieldCache.getEntitiesByProfile(tenantId, cfEntityId).forEach(id -> updateOrInitializeState(calculatedFieldCtx, id, argumentValues, calculatedFieldIds));
                } else {
                    updateOrInitializeState(calculatedFieldCtx, entityId, argumentValues, calculatedFieldIds);
                }
            }
            default -> updateOrInitializeState(calculatedFieldCtx, cfEntityId, argumentValues, calculatedFieldIds);
        }
        log.info("Successfully updated telemetry for calculatedFieldId: [{}]", calculatedFieldId);
    }

    @Override
    public void onCalculatedFieldStateMsg(TransportProtos.CalculatedFieldStateMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(proto.getCalculatedFieldIdMSB(), proto.getCalculatedFieldIdLSB()));
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));

            if (proto.getClear()) {
                clearState(tenantId, calculatedFieldId, entityId);
                return;
            }

            List<CalculatedFieldId> calculatedFieldIds = proto.getCalculatedFieldsList().stream()
                    .map(cfIdProto -> new CalculatedFieldId(new UUID(cfIdProto.getCalculatedFieldIdMSB(), cfIdProto.getCalculatedFieldIdLSB())))
                    .toList();
            Map<String, ArgumentEntry> argumentsMap = proto.getArgumentsMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> fromArgumentEntryProto(entry.getValue())));

            CalculatedFieldCtx calculatedFieldCtx = calculatedFieldCache.getCalculatedFieldCtx(tenantId, calculatedFieldId, tbelInvokeService);
            updateOrInitializeState(calculatedFieldCtx, entityId, argumentsMap, calculatedFieldIds);
        } catch (Exception e) {
            log.trace("Failed to process calculated field update state msg: [{}]", proto, e);
        }
    }

    @Override
    public void onEntityProfileChangedMsg(TransportProtos.EntityProfileUpdateMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            EntityId oldProfileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getOldProfileIdMSB(), proto.getOldProfileIdLSB()));
            EntityId newProfileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getNewProfileIdMSB(), proto.getNewProfileIdLSB()));
            log.info("Received EntityProfileUpdateMsgProto for processing: tenantId=[{}], entityId=[{}]", tenantId, entityId);

            calculatedFieldCache.getEntitiesByProfile(tenantId, oldProfileId).remove(entityId);
            calculatedFieldCache.getEntitiesByProfile(tenantId, newProfileId).add(entityId);

            calculatedFieldService.findCalculatedFieldIdsByEntityId(tenantId, oldProfileId)
                    .forEach(cfId -> clearState(tenantId, cfId, entityId));

            initializeStateForEntityByProfile(tenantId, entityId, newProfileId, callback);
        } catch (Exception e) {
            log.trace("Failed to process entity type update msg: [{}]", proto, e);
        }
    }

    @Override
    public void onProfileEntityMsg(TransportProtos.ProfileEntityMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            EntityId profileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getProfileIdMSB(), proto.getProfileIdLSB()));
            log.info("Received ProfileEntityMsgProto for processing: tenantId=[{}], entityId=[{}]", tenantId, entityId);
            if (proto.getDeleted()) {
                log.info("Executing profile entity deleted msg,  tenantId=[{}], entityId=[{}]", tenantId, entityId);
                calculatedFieldCache.getEntitiesByProfile(tenantId, profileId).remove(entityId);
                List<CalculatedFieldId> calculatedFieldIds = Stream.concat(
                        calculatedFieldCache.getCalculatedFieldLinksByEntityId(tenantId, entityId).stream().map(CalculatedFieldLink::getCalculatedFieldId),
                        calculatedFieldCache.getCalculatedFieldLinksByEntityId(tenantId, profileId).stream().map(CalculatedFieldLink::getCalculatedFieldId)
                ).toList();
                calculatedFieldIds.forEach(cfId -> clearState(tenantId, cfId, entityId));
            } else {
                log.info("Executing profile entity added msg,  tenantId=[{}], entityId=[{}]", tenantId, entityId);
                calculatedFieldCache.getEntitiesByProfile(tenantId, profileId).add(entityId);
                initializeStateForEntityByProfile(tenantId, entityId, profileId, callback);
            }
        } catch (Exception e) {
            log.trace("Failed to process profile entity msg: [{}]", proto, e);
        }
    }

    private void clearState(TenantId tenantId, CalculatedFieldId calculatedFieldId, EntityId entityId) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, calculatedFieldId);
        if (tpi.isMyPartition()) {
            log.warn("Executing clearState, calculatedFieldId=[{}], entityId=[{}]", calculatedFieldId, entityId);
            CalculatedFieldEntityCtxId ctxId = new CalculatedFieldEntityCtxId(calculatedFieldId.getId(), entityId.getId());
            states.remove(ctxId);
            rocksDBService.delete(JacksonUtil.writeValueAsString(ctxId));
        } else {
            sendClearCalculatedFieldStateMsg(tenantId, calculatedFieldId, entityId);
        }
    }

    private void initializeStateForEntityByProfile(TenantId tenantId, EntityId entityId, EntityId profileId, TbCallback callback) {
        calculatedFieldService.findCalculatedFieldIdsByEntityId(tenantId, profileId)
                .stream()
                .map(cfId -> calculatedFieldCache.getCalculatedFieldCtx(tenantId, cfId, tbelInvokeService))
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
                updateOrInitializeState(calculatedFieldCtx, entityId, argumentValues, new ArrayList<>());
                callback.onSuccess();
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to initialize state for entity: [{}]", entityId, t);
                callback.onFailure(t);
            }
        }, calculatedFieldCallbackExecutor);
    }

    private void updateOrInitializeState(CalculatedFieldCtx calculatedFieldCtx, EntityId entityId, Map<String, ArgumentEntry> argumentValues, List<CalculatedFieldId> calculatedFieldIds) {
        TenantId tenantId = calculatedFieldCtx.getTenantId();
        CalculatedFieldId cfId = calculatedFieldCtx.getCfId();
        Map<String, ArgumentEntry> argumentsMap = new HashMap<>(argumentValues);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, cfId);
        if (tpi.isMyPartition()) {
            CalculatedFieldEntityCtxId entityCtxId = new CalculatedFieldEntityCtxId(cfId.getId(), entityId.getId());

            states.compute(entityCtxId, (ctxId, ctx) -> {
                CalculatedFieldEntityCtx calculatedFieldEntityCtx = ctx != null ? ctx : fetchCalculatedFieldEntityState(ctxId, calculatedFieldCtx.getCfType());

                Consumer<CalculatedFieldState> performUpdateState = (state) -> {
                    if (state.updateState(argumentsMap)) {
                        calculatedFieldEntityCtx.setState(state);
                        rocksDBService.put(JacksonUtil.writeValueAsString(entityCtxId), JacksonUtil.writeValueAsString(calculatedFieldEntityCtx));
                        Map<String, ArgumentEntry> arguments = state.getArguments();
                        boolean allArgsPresent = arguments.keySet().containsAll(calculatedFieldCtx.getArguments().keySet()) &&
                                !arguments.containsValue(SingleValueArgumentEntry.EMPTY) && !arguments.containsValue(TsRollingArgumentEntry.EMPTY);
                        if (allArgsPresent) {
                            performCalculation(calculatedFieldCtx, state, entityId, calculatedFieldIds);
                        }
                    }
                };

                CalculatedFieldState state = calculatedFieldEntityCtx.getState();

                boolean allKeysPresent = argumentsMap.keySet().containsAll(calculatedFieldCtx.getArguments().keySet());
                boolean requiresTsRollingUpdate = calculatedFieldCtx.getArguments().values().stream()
                        .anyMatch(argument -> ArgumentType.TS_ROLLING.equals(argument.getType()) && state.getArguments().get(argument.getKey()) == null);

                if (!allKeysPresent || requiresTsRollingUpdate) {

                    Map<String, Argument> missingArguments = calculatedFieldCtx.getArguments().entrySet().stream()
                            .filter(entry -> !argumentsMap.containsKey(entry.getKey()) || (ArgumentType.TS_ROLLING.equals(entry.getValue().getType()) && state.getArguments().get(entry.getKey()) == null))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    fetchArguments(calculatedFieldCtx.getTenantId(), entityId, missingArguments, argumentsMap::putAll)
                            .addListener(() -> performUpdateState.accept(state),
                                    calculatedFieldCallbackExecutor);
                } else {
                    performUpdateState.accept(state);
                }
                return calculatedFieldEntityCtx;
            });
        } else {
            sendUpdateCalculatedFieldStateMsg(tenantId, cfId, entityId, calculatedFieldIds, argumentsMap);
        }
    }

    private void performCalculation(CalculatedFieldCtx calculatedFieldCtx, CalculatedFieldState state, EntityId entityId, List<CalculatedFieldId> calculatedFieldIds) {
        ListenableFuture<CalculatedFieldResult> resultFuture = state.performCalculation(calculatedFieldCtx);
        Futures.addCallback(resultFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(CalculatedFieldResult result) {
                if (result != null) {
                    pushMsgToRuleEngine(calculatedFieldCtx.getTenantId(), calculatedFieldCtx.getCfId(), entityId, result, calculatedFieldIds);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to perform calculation. entityId: [{}]", calculatedFieldCtx.getCfId(), entityId, t);
            }
        }, MoreExecutors.directExecutor());
    }

    private void pushMsgToRuleEngine(TenantId tenantId, CalculatedFieldId calculatedFieldId, EntityId originatorId, CalculatedFieldResult calculatedFieldResult, List<CalculatedFieldId> calculatedFieldIds) {
        try {
            OutputType type = calculatedFieldResult.getType();
            TbMsgType msgType = OutputType.ATTRIBUTES.equals(type) ? TbMsgType.POST_ATTRIBUTES_REQUEST : TbMsgType.POST_TELEMETRY_REQUEST;
            TbMsgMetaData md = OutputType.ATTRIBUTES.equals(type) ? new TbMsgMetaData(Map.of(SCOPE, calculatedFieldResult.getScope().name())) : TbMsgMetaData.EMPTY;
            ObjectNode payload = createJsonPayload(calculatedFieldResult);
            if (calculatedFieldIds == null) {
                calculatedFieldIds = new ArrayList<>();
            }
            if (calculatedFieldIds.contains(calculatedFieldId)) {
                throw new IllegalArgumentException("Calculated field [" + calculatedFieldId.getId() + "] refers to itself, causing an infinite loop.");
            }
            calculatedFieldIds.add(calculatedFieldId);
            TbMsg msg = TbMsg.newMsg().type(msgType).originator(originatorId).calculatedFieldIds(calculatedFieldIds).metaData(md).data(JacksonUtil.writeValueAsString(payload)).build();
            clusterService.pushMsgToRuleEngine(tenantId, originatorId, msg, null);
        } catch (Exception e) {
            log.warn("[{}] Failed to push message to rule engine. CalculatedFieldResult: {}", originatorId, calculatedFieldResult, e);
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
        EntityId argumentEntityId = argument.getEntityId();
        EntityId entityId = isProfileEntity(argumentEntityId)
                ? targetEntityId
                : argumentEntityId;
        return fetchKvEntry(tenantId, entityId, argument);
    }

    private ListenableFuture<ArgumentEntry> fetchKvEntry(TenantId tenantId, EntityId entityId, Argument argument) {
        return switch (argument.getType()) {
            case TS_ROLLING -> fetchTsRolling(tenantId, entityId, argument);
            case ATTRIBUTE -> transformSingleValueArgument(
                    Futures.transform(
                            attributesService.find(tenantId, entityId, argument.getScope(), argument.getKey()),
                            result -> result.or(() -> Optional.of(new BaseAttributeKvEntry(createDefaultKvEntry(argument), System.currentTimeMillis(), 0L))),
                            calculatedFieldCallbackExecutor)
            );
            case TS_LATEST -> transformSingleValueArgument(
                    Futures.transform(
                            timeseriesService.findLatest(tenantId, entityId, argument.getKey()),
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

        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getKey(), startTs, currentTime, 0, limit, Aggregation.NONE);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsRollingFuture, tsRolling -> tsRolling == null ? TsRollingArgumentEntry.EMPTY : ArgumentEntry.createTsRollingArgument(tsRolling), calculatedFieldCallbackExecutor);
    }

    private void sendUpdateCalculatedFieldStateMsg(TenantId tenantId, CalculatedFieldId calculatedFieldId, EntityId entityId, List<CalculatedFieldId> calculatedFieldIds, Map<String, ArgumentEntry> argumentValues) {
        TransportProtos.CalculatedFieldStateMsgProto.Builder msgBuilder = createBaseCalculatedFieldStateMsg(tenantId, calculatedFieldId, entityId);
        if (argumentValues != null) {
            argumentValues.forEach((key, argumentEntry) -> msgBuilder.putArguments(key, toArgumentEntryProto(argumentEntry)));
        }
        if (calculatedFieldIds != null) {
            calculatedFieldIds.forEach(cfId -> msgBuilder.addCalculatedFields(
                    TransportProtos.CalculatedFieldIdProto.newBuilder()
                            .setCalculatedFieldIdMSB(cfId.getId().getMostSignificantBits())
                            .setCalculatedFieldIdLSB(cfId.getId().getLeastSignificantBits())
                            .build()
            ));
        }

        clusterService.pushMsgToCore(tenantId, calculatedFieldId, TransportProtos.ToCoreMsg.newBuilder().setCalculatedFieldStateMsg(msgBuilder).build(), null);
    }

    private void sendClearCalculatedFieldStateMsg(TenantId tenantId, CalculatedFieldId calculatedFieldId, EntityId entityId) {
        TransportProtos.CalculatedFieldStateMsgProto msg = createBaseCalculatedFieldStateMsg(tenantId, calculatedFieldId, entityId)
                .setClear(true)
                .build();

        clusterService.pushMsgToCore(tenantId, calculatedFieldId, TransportProtos.ToCoreMsg.newBuilder().setCalculatedFieldStateMsg(msg).build(), null);
    }

    private TransportProtos.CalculatedFieldStateMsgProto.Builder createBaseCalculatedFieldStateMsg(
            TenantId tenantId,
            CalculatedFieldId calculatedFieldId,
            EntityId entityId
    ) {
        return TransportProtos.CalculatedFieldStateMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setCalculatedFieldIdMSB(calculatedFieldId.getId().getMostSignificantBits())
                .setCalculatedFieldIdLSB(calculatedFieldId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name())
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits());
    }

    private TransportProtos.ArgumentEntryProto toArgumentEntryProto(ArgumentEntry argumentEntry) {
        TransportProtos.ArgumentEntryProto.Builder argumentProtoBuilder = TransportProtos.ArgumentEntryProto.newBuilder();

        if (argumentEntry instanceof TsRollingArgumentEntry tsRollingArgumentEntry) {
            TransportProtos.TsRollingProto.Builder tsRollingProtoBuilder = TransportProtos.TsRollingProto.newBuilder();
            tsRollingArgumentEntry.getTsRecords().forEach((ts, value) ->
                    tsRollingProtoBuilder.putTsRecords(ts, toObjectProto(value))
            );
            argumentProtoBuilder.setTsRecords(tsRollingProtoBuilder.build());
        } else if (argumentEntry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            argumentProtoBuilder.setSingleValue(
                    TransportProtos.SingleValueProto.newBuilder()
                            .setTs(singleValueArgumentEntry.getTs())
                            .setValue(toObjectProto(singleValueArgumentEntry.getValue()))
                            .build()
            );
        }

        return argumentProtoBuilder.build();
    }

    private ArgumentEntry fromArgumentEntryProto(TransportProtos.ArgumentEntryProto entryProto) {
        if (entryProto.hasTsRecords()) {
            TsRollingArgumentEntry tsRollingArgumentEntry = new TsRollingArgumentEntry();
            entryProto.getTsRecords().getTsRecordsMap().forEach((ts, objectProto) ->
                    tsRollingArgumentEntry.getTsRecords().put(ts, fromObjectProto(objectProto))
            );
            return tsRollingArgumentEntry;
        } else if (entryProto.hasSingleValue()) {
            TransportProtos.SingleValueProto singleValueProto = entryProto.getSingleValue();
            return new SingleValueArgumentEntry(singleValueProto.getTs(), fromObjectProto(singleValueProto.getValue()), singleValueProto.getVersion());
        } else {
            throw new IllegalArgumentException("Unsupported ArgumentEntryProto type");
        }
    }

    private KvEntry createDefaultKvEntry(Argument argument) {
        String key = argument.getKey();
        String defaultValue = argument.getDefaultValue();
        if (NumberUtils.isParsable(defaultValue)) {
            return new DoubleDataEntry(key, Double.parseDouble(defaultValue));
        }
        if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
            return new BooleanDataEntry(key, Boolean.parseBoolean(defaultValue));
        }
        return new StringDataEntry(key, defaultValue);
    }

    private CalculatedFieldEntityCtx fetchCalculatedFieldEntityState(CalculatedFieldEntityCtxId entityCtxId, CalculatedFieldType cfType) {
        String stateStr = rocksDBService.get(JacksonUtil.writeValueAsString(entityCtxId));
        if (stateStr == null) {
            return new CalculatedFieldEntityCtx(entityCtxId, createStateByType(cfType));
        }
        return JacksonUtil.fromString(stateStr, CalculatedFieldEntityCtx.class);
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

}
