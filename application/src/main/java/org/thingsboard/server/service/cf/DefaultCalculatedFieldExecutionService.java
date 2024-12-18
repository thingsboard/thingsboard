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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
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
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceService;
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
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldExecutionService extends AbstractPartitionBasedService<CalculatedFieldId> implements CalculatedFieldExecutionService {

    private final CalculatedFieldService calculatedFieldService;
    private final AssetService assetService;
    private final DeviceService deviceService;
    private final TbAssetProfileCache assetProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;
    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private final RocksDBService rocksDBService;
    private final TbClusterService clusterService;
    private final TbelInvokeService tbelInvokeService;

    private ListeningExecutorService calculatedFieldExecutor;
    private ListeningExecutorService calculatedFieldCallbackExecutor;

    private final ConcurrentMap<CalculatedFieldId, CalculatedField> calculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, List<CalculatedFieldLink>> calculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, CalculatedFieldCtx> calculatedFieldsCtx = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldEntityCtxId, CalculatedFieldEntityCtx> states = new ConcurrentHashMap<>();

    private final ConcurrentMap<EntityId, Set<EntityId>> profileEntities = new ConcurrentHashMap<>();

    private static final int MAX_LAST_RECORDS_VALUE = 1024;

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
        scheduledExecutor.submit(this::fetchCalculatedFields);
    }

    private void fetchCalculatedFields() {
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        cfs.forEach(cf -> calculatedFields.putIfAbsent(cf.getId(), cf));
        PageDataIterable<CalculatedFieldLink> cfls = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFieldLinks, initFetchPackSize);
        cfls.forEach(link -> calculatedFieldLinks.computeIfAbsent(link.getCalculatedFieldId(), id -> new ArrayList<>()).add(link));
        rocksDBService.getAll().forEach((ctxId, ctx) -> states.put(JacksonUtil.fromString(ctxId, CalculatedFieldEntityCtxId.class), JacksonUtil.fromString(ctx, CalculatedFieldEntityCtx.class)));
        states.keySet().removeIf(ctxId -> calculatedFields.keySet().stream().noneMatch(id -> ctxId.cfId().equals(id.getId())));
    }

    @PreDestroy
    public void stop() {
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
                log.warn("Failed to resolve partition for CalculatedField [{}], tenant [{}]. Reason: {}",
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
                            if (EntityType.ASSET_PROFILE.equals(cf.getEntityId().getEntityType()) || EntityType.DEVICE_PROFILE.equals(cf.getEntityId().getEntityType())) {
                                getOrFetchFromDBProfileEntities(cf.getTenantId(), cf.getEntityId())
                                        .forEach(entityId -> restoreState(cf, entityId));
                            } else {
                                restoreState(cf, cf.getEntityId());
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
            calculatedFieldsCtx.putIfAbsent(cf.getId(), new CalculatedFieldCtx(cf, tbelInvokeService));
            states.put(ctxId, restoredCtx);
            log.info("Restored state for CalculatedField [{}]", cf.getId());
        } else {
            log.warn("No state found for CalculatedField [{}], entity [{}].", cf.getId(), entityId);
        }
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(CalculatedFieldId entityId) {
        calculatedFields.remove(entityId);
        states.keySet().removeIf(ctxId -> ctxId.cfId().equals(entityId.getId()));
    }

    @Override
    public void onCalculatedFieldMsg(TransportProtos.CalculatedFieldMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(proto.getCalculatedFieldIdMSB(), proto.getCalculatedFieldIdLSB()));
            log.info("Received CalculatedFieldMsgProto for processing: tenantId=[{}], calculatedFieldId=[{}]", tenantId, calculatedFieldId);
            if (proto.getDeleted()) {
                log.warn("Executing onCalculatedFieldDelete, calculatedFieldId=[{}]", calculatedFieldId);
                onCalculatedFieldDelete(calculatedFieldId, callback);
                callback.onSuccess();
            }
            CalculatedField cf = getOrFetchFromDb(tenantId, calculatedFieldId);
            if (proto.getUpdated()) {
                log.info("Executing onCalculatedFieldUpdate, calculatedFieldId=[{}]", calculatedFieldId);
                boolean shouldReinit = onCalculatedFieldUpdate(cf, callback);
                if (!shouldReinit) {
                    return;
                }
            }
            if (cf != null) {
                EntityId entityId = cf.getEntityId();
                CalculatedFieldCtx calculatedFieldCtx = new CalculatedFieldCtx(cf, tbelInvokeService);
                calculatedFieldsCtx.put(calculatedFieldId, calculatedFieldCtx);
                switch (entityId.getEntityType()) {
                    case ASSET, DEVICE -> {
                        log.info("Initializing state for entity: tenantId=[{}], entityId=[{}]", tenantId, entityId);
                        initializeStateForEntity(calculatedFieldCtx, entityId, callback);
                    }
                    case ASSET_PROFILE, DEVICE_PROFILE -> {
                        log.info("Initializing state for all entities in profile: tenantId=[{}], profileId=[{}]", tenantId, entityId);
                        fetchCommonArguments(calculatedFieldCtx, callback, commonArguments -> {
                            getOrFetchFromDBProfileEntities(tenantId, entityId).forEach(targetEntityId -> {
                                initializeStateForEntity(calculatedFieldCtx, targetEntityId, commonArguments, callback);
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

    @Override
    public void onTelemetryUpdate(TenantId tenantId, EntityId entityId, List<? extends KvEntry> telemetry) {
        try {
            EntityType entityType = entityId.getEntityType();
            if (EntityType.DEVICE.equals(entityType) || EntityType.ASSET.equals(entityType) || EntityType.CUSTOMER.equals(entityType) || EntityType.TENANT.equals(entityType)) {
                EntityId profileId = null;
                if (EntityType.ASSET.equals(entityType)) {
                    profileId = assetProfileCache.get(tenantId, (AssetId) entityId).getId();
                } else if (EntityType.DEVICE.equals(entityType)) {
                    profileId = deviceProfileCache.get(tenantId, (DeviceId) entityId).getId();
                }
                List<CalculatedFieldLink> cfLinks = new ArrayList<>(calculatedFieldService.findAllCalculatedFieldLinksByEntityId(tenantId, entityId));
                Optional.ofNullable(profileId).ifPresent(id -> cfLinks.addAll(calculatedFieldService.findAllCalculatedFieldLinksByEntityId(tenantId, id)));
                cfLinks.forEach(link -> {
                    CalculatedFieldId calculatedFieldId = link.getCalculatedFieldId();
                    Map<String, String> attributes = link.getConfiguration().getAttributes();
                    Map<String, String> timeSeries = link.getConfiguration().getTimeSeries();
                    Map<String, KvEntry> updatedTelemetry = telemetry.stream()
                            .filter(entry -> attributes.containsValue(entry.getKey()) || timeSeries.containsValue(entry.getKey()))
                            .collect(Collectors.toMap(
                                    entry -> getMappedKey(entry, attributes, timeSeries),
                                    entry -> entry,
                                    (v1, v2) -> v1
                            ));

                    if (!updatedTelemetry.isEmpty()) {
                        executeTelemetryUpdate(tenantId, entityId, calculatedFieldId, updatedTelemetry);
                    }
                });
            }
        } catch (Exception e) {
            log.trace("Failed to update telemetry entityId: [{}]", entityId, e);
        }
    }

    private void executeTelemetryUpdate(TenantId tenantId, EntityId entityId, CalculatedFieldId calculatedFieldId, Map<String, KvEntry> updatedTelemetry) {
        log.info("Received telemetry update msg: tenantId=[{}], entityId=[{}], calculatedFieldId=[{}]", tenantId, entityId, calculatedFieldId);
        CalculatedField calculatedField = getOrFetchFromDb(tenantId, calculatedFieldId);
        CalculatedFieldCtx calculatedFieldCtx = calculatedFieldsCtx.computeIfAbsent(calculatedFieldId, id -> new CalculatedFieldCtx(calculatedField, tbelInvokeService));
        Map<String, ArgumentEntry> argumentValues = updatedTelemetry.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> ArgumentEntry.createSingleValueArgument(entry.getValue())));

        EntityId cfEntityId = calculatedField.getEntityId();
        switch (cfEntityId.getEntityType()) {
            case ASSET_PROFILE, DEVICE_PROFILE -> {
                boolean isCommonEntity = calculatedField.getConfiguration().getReferencedEntities().contains(entityId);
                if (isCommonEntity) {
                    getOrFetchFromDBProfileEntities(tenantId, cfEntityId).forEach(id -> updateOrInitializeState(calculatedFieldCtx, id, argumentValues));
                } else {
                    updateOrInitializeState(calculatedFieldCtx, entityId, argumentValues);
                }
            }
            default -> updateOrInitializeState(calculatedFieldCtx, cfEntityId, argumentValues);
        }
        log.info("Successfully updated telemetry for calculatedFieldId: [{}]", calculatedFieldId);
    }

    private String getMappedKey(KvEntry entry, Map<String, String> attributes, Map<String, String> timeSeries) {
        if (entry instanceof AttributeKvEntry) {
            return attributes.entrySet().stream()
                    .filter(attr -> attr.getValue().equals(entry.getKey()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(entry.getKey());
        } else if (entry instanceof TsKvEntry) {
            return timeSeries.entrySet().stream()
                    .filter(ts -> ts.getValue().equals(entry.getKey()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(entry.getKey());
        }
        return entry.getKey();
    }

    @Override
    public void onEntityProfileChanged(TransportProtos.EntityProfileUpdateMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            EntityId oldProfileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getOldProfileIdMSB(), proto.getOldProfileIdLSB()));
            EntityId newProfileId = EntityIdFactory.getByTypeAndUuid(proto.getEntityProfileType(), new UUID(proto.getNewProfileIdMSB(), proto.getNewProfileIdLSB()));
            log.info("Received EntityProfileUpdateMsgProto for processing: tenantId=[{}], entityId=[{}]", tenantId, entityId);

            profileEntities.get(oldProfileId).remove(entityId);
            profileEntities.computeIfAbsent(newProfileId, id -> new HashSet<>()).add(entityId);

            calculatedFieldService.findCalculatedFieldIdsByEntityId(tenantId, oldProfileId)
                    .forEach(cfId -> {
                        CalculatedFieldEntityCtxId ctxId = new CalculatedFieldEntityCtxId(cfId.getId(), entityId.getId());
                        states.remove(ctxId);
                        rocksDBService.delete(JacksonUtil.writeValueAsString(ctxId));
                    });

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
                profileEntities.get(profileId).remove(entityId);
                List<String> statesToRemove = states.keySet().stream()
                        .filter(ctxEntityId -> ctxEntityId.entityId().equals(entityId.getId()))
                        .map(JacksonUtil::writeValueAsString)
                        .toList();
                states.keySet().removeIf(ctxEntityId -> ctxEntityId.entityId().equals(entityId.getId()));
                rocksDBService.deleteAll(statesToRemove);
            } else {
                log.info("Executing profile entity added msg,  tenantId=[{}], entityId=[{}]", tenantId, entityId);
                profileEntities.computeIfAbsent(profileId, id -> new HashSet<>()).add(entityId);
                initializeStateForEntityByProfile(tenantId, entityId, profileId, callback);
            }
        } catch (Exception e) {
            log.trace("Failed to process profile entity msg: [{}]", proto, e);
        }
    }

    private boolean onCalculatedFieldUpdate(CalculatedField updatedCalculatedField, TbCallback callback) {
        CalculatedField oldCalculatedField = getOrFetchFromDb(updatedCalculatedField.getTenantId(), updatedCalculatedField.getId());
        boolean shouldReinit = true;
        if (hasSignificantChanges(oldCalculatedField, updatedCalculatedField)) {
            onCalculatedFieldDelete(updatedCalculatedField.getId(), callback);
        } else {
            calculatedFields.put(updatedCalculatedField.getId(), updatedCalculatedField);
            calculatedFieldsCtx.put(updatedCalculatedField.getId(), new CalculatedFieldCtx(updatedCalculatedField, tbelInvokeService));
            callback.onSuccess();
            shouldReinit = false;
        }
        return shouldReinit;
    }

    private void onCalculatedFieldDelete(CalculatedFieldId calculatedFieldId, TbCallback callback) {
        try {
            calculatedFields.remove(calculatedFieldId);
            calculatedFieldsCtx.remove(calculatedFieldId);
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

    private CalculatedField getOrFetchFromDb(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        return calculatedFields.computeIfAbsent(calculatedFieldId, cfId -> calculatedFieldService.findById(tenantId, calculatedFieldId));
    }

    private Set<EntityId> getOrFetchFromDBProfileEntities(TenantId tenantId, EntityId entityProfileId) {
        return switch (entityProfileId.getEntityType()) {
            case ASSET_PROFILE -> profileEntities.computeIfAbsent(entityProfileId, profileId -> {
                Set<EntityId> assetIds = new HashSet<>();
                (new PageDataIterable<>(pageLink ->
                        assetService.findAssetIdsByTenantIdAndAssetProfileId(tenantId, (AssetProfileId) profileId, pageLink), initFetchPackSize)).forEach(assetIds::add);
                return assetIds;
            });
            case DEVICE_PROFILE -> profileEntities.computeIfAbsent(entityProfileId, profileId -> {
                Set<EntityId> deviceIds = new HashSet<>();
                (new PageDataIterable<>(pageLink ->
                        deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(tenantId, (DeviceProfileId) entityProfileId, pageLink), initFetchPackSize)).forEach(deviceIds::add);
                return deviceIds;
            });
            default -> throw new IllegalArgumentException("Entity type should be ASSET_PROFILE or DEVICE_PROFILE.");
        };
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

    private void initializeStateForEntityByProfile(TenantId tenantId, EntityId entityId, EntityId profileId, TbCallback callback) {
        calculatedFieldService.findCalculatedFieldIdsByEntityId(tenantId, profileId)
                .stream()
                .map(cfId -> calculatedFieldsCtx.computeIfAbsent(cfId, id -> new CalculatedFieldCtx(calculatedFieldService.findById(tenantId, id), tbelInvokeService)))
                .forEach(cfCtx -> initializeStateForEntity(cfCtx, entityId, callback));
    }

    private void fetchCommonArguments(CalculatedFieldCtx calculatedFieldCtx, TbCallback callback, Consumer<Map<String, ArgumentEntry>> onComplete) {
        Map<String, ArgumentEntry> argumentValues = new HashMap<>();
        List<ListenableFuture<ArgumentEntry>> futures = new ArrayList<>();

        calculatedFieldCtx.getArguments().forEach((key, argument) -> {
            if (!EntityType.DEVICE_PROFILE.equals(argument.getEntityId().getEntityType()) &&
                    !EntityType.ASSET_PROFILE.equals(argument.getEntityId().getEntityType())) {
                futures.add(Futures.transform(fetchKvEntry(calculatedFieldCtx.getTenantId(), argument.getEntityId(), argument),
                        result -> {
                            argumentValues.put(key, result);
                            return result;
                        }, calculatedFieldCallbackExecutor));
            }
        });

        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
            @Override
            public void onSuccess(List<ArgumentEntry> results) {
                onComplete.accept(argumentValues);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to fetch common arguments", t);
                callback.onFailure(t);
            }
        }, calculatedFieldCallbackExecutor);
    }

    private void initializeStateForEntity(CalculatedFieldCtx calculatedFieldCtx, EntityId entityId, TbCallback callback) {
        initializeStateForEntity(calculatedFieldCtx, entityId, new HashMap<>(), callback);
    }

    private void initializeStateForEntity(CalculatedFieldCtx calculatedFieldCtx, EntityId entityId, Map<String, ArgumentEntry> commonArguments, TbCallback callback) {
        Map<String, ArgumentEntry> argumentValues = new HashMap<>(commonArguments);
        List<ListenableFuture<ArgumentEntry>> futures = new ArrayList<>();

        calculatedFieldCtx.getArguments().forEach((key, argument) -> {
            if (!commonArguments.containsKey(key)) {
                futures.add(Futures.transform(fetchArgumentValue(calculatedFieldCtx, entityId, argument),
                        result -> {
                            argumentValues.put(key, result);
                            return result;
                        }, calculatedFieldCallbackExecutor));
            }
        });

        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
            @Override
            public void onSuccess(List<ArgumentEntry> results) {
                updateOrInitializeState(calculatedFieldCtx, entityId, argumentValues);
                callback.onSuccess();
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to initialize state for entity: [{}]", entityId, t);
                callback.onFailure(t);
            }
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchArgumentValue(CalculatedFieldCtx calculatedFieldCtx, EntityId targetEntityId, Argument argument) {
        TenantId tenantId = calculatedFieldCtx.getTenantId();
        EntityId argumentEntityId = argument.getEntityId();
        EntityId entityId = EntityType.DEVICE_PROFILE.equals(argumentEntityId.getEntityType()) || EntityType.ASSET_PROFILE.equals(argumentEntityId.getEntityType())
                ? targetEntityId
                : argumentEntityId;
        return fetchKvEntry(tenantId, entityId, argument);
    }

    private ListenableFuture<ArgumentEntry> fetchKvEntry(TenantId tenantId, EntityId entityId, Argument argument) {
        return switch (argument.getType()) {
            case "TS_ROLLING" -> fetchTsRolling(tenantId, entityId, argument);
            case "ATTRIBUTE" -> transformSingleValueArgument(
                    Futures.transform(
                            attributesService.find(tenantId, entityId, argument.getScope(), argument.getKey()),
                            result -> result.or(() -> Optional.of(new BaseAttributeKvEntry(System.currentTimeMillis(), createDefaultKvEntry(argument)))),
                            calculatedFieldCallbackExecutor)
            );
            case "TS_LATEST" -> transformSingleValueArgument(
                    Futures.transform(
                            timeseriesService.findLatest(tenantId, entityId, argument.getKey()),
                            result -> result.or(() -> Optional.of(new BasicTsKvEntry(System.currentTimeMillis(), createDefaultKvEntry(argument)))),
                            calculatedFieldCallbackExecutor));
            default -> throw new IllegalArgumentException("Invalid argument type '" + argument.getType() + "'.");
        };
    }

    private ListenableFuture<ArgumentEntry> fetchTsRolling(TenantId tenantId, EntityId entityId, Argument argument) {
        long currentTime = System.currentTimeMillis();
        long timeWindow = argument.getTimeWindow() == 0 ? System.currentTimeMillis() : argument.getTimeWindow();
        long startTs = currentTime - timeWindow;
        int limit = argument.getLimit() == 0 ? MAX_LAST_RECORDS_VALUE : argument.getLimit();

        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getKey(), startTs, currentTime, 0, limit, Aggregation.NONE);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsRollingFuture, tsRolling -> tsRolling == null ? ArgumentEntry.createTsRollingArgument(Collections.emptyList()) : ArgumentEntry.createTsRollingArgument(tsRolling), calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> transformSingleValueArgument(ListenableFuture<Optional<? extends KvEntry>> kvEntryFuture) {
        return Futures.transform(kvEntryFuture, kvEntry -> ArgumentEntry.createSingleValueArgument(kvEntry.orElse(null)), calculatedFieldCallbackExecutor);
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

    private void updateOrInitializeState(CalculatedFieldCtx calculatedFieldCtx, EntityId entityId, Map<String, ArgumentEntry> argumentValues) {
        CalculatedFieldEntityCtxId entityCtxId = new CalculatedFieldEntityCtxId(calculatedFieldCtx.getCfId().getId(), entityId.getId());
        CalculatedFieldEntityCtx calculatedFieldEntityCtx = states.computeIfAbsent(entityCtxId, this::fetchCalculatedFieldEntityState);

        CalculatedFieldState state = calculatedFieldEntityCtx.getState();

        if (state == null) {
            state = createStateByType(calculatedFieldCtx.getCfType());
        }
        if (state.updateState(argumentValues)) {
            calculatedFieldEntityCtx.setState(state);
            states.put(entityCtxId, calculatedFieldEntityCtx);
            rocksDBService.put(JacksonUtil.writeValueAsString(entityCtxId), JacksonUtil.writeValueAsString(calculatedFieldEntityCtx));

            boolean allArgsPresent = calculatedFieldCtx.getArguments().keySet().containsAll(state.getArguments().keySet());
            if (allArgsPresent) {
                ListenableFuture<CalculatedFieldResult> resultFuture = state.performCalculation(calculatedFieldCtx);
                Futures.addCallback(resultFuture, new FutureCallback<>() {
                    @Override
                    public void onSuccess(CalculatedFieldResult result) {
                        if (result != null) {
                            pushMsgToRuleEngine(calculatedFieldCtx.getTenantId(), entityId, result);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}] Failed to perform calculation. entityId: [{}]", calculatedFieldCtx.getCfId(), entityId, t);
                    }
                }, MoreExecutors.directExecutor());
            }
        }
    }

    private CalculatedFieldEntityCtx fetchCalculatedFieldEntityState(CalculatedFieldEntityCtxId entityCtxId) {
        String stateStr = rocksDBService.get(JacksonUtil.writeValueAsString(entityCtxId));
        if (stateStr == null) {
            return new CalculatedFieldEntityCtx(entityCtxId, null);
        }
        return JacksonUtil.fromString(stateStr, CalculatedFieldEntityCtx.class);
    }

    private void pushMsgToRuleEngine(TenantId tenantId, EntityId originatorId, CalculatedFieldResult calculatedFieldResult) {
        try {
            String type = calculatedFieldResult.getType();
            TbMsgType msgType = "ATTRIBUTE".equals(type) ? TbMsgType.POST_ATTRIBUTES_REQUEST : TbMsgType.POST_TELEMETRY_REQUEST;
            TbMsgMetaData md = "ATTRIBUTE".equals(type) ? new TbMsgMetaData(Map.of(SCOPE, calculatedFieldResult.getScope().name())) : TbMsgMetaData.EMPTY;
            ObjectNode payload = createJsonPayload(calculatedFieldResult);
            TbMsg msg = TbMsg.newMsg(msgType, originatorId, md, JacksonUtil.writeValueAsString(payload));
            clusterService.pushMsgToRuleEngine(tenantId, originatorId, msg, null);
        } catch (Exception e) {
            log.warn("[{}] Failed to push message to rule engine. CalculatedFieldResult: {}", originatorId, calculatedFieldResult, e);
        }
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

}
