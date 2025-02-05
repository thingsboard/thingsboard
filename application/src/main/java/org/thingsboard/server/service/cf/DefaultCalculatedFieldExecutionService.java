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
import org.thingsboard.server.actors.calculatedField.CalculatedFieldTelemetryMsg;
import org.thingsboard.server.actors.calculatedField.MultipleTbCallback;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
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
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeScopeProto;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityCtxIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto.Builder;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
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
import static org.thingsboard.server.common.util.ProtoUtils.toTsKvProto;
import static org.thingsboard.server.queue.discovery.HashPartitionService.CALCULATED_FIELD_QUEUE_KEY;

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
    private final ApiLimitService apiLimitService;

    private ListeningExecutorService calculatedFieldExecutor;
    private ListeningExecutorService calculatedFieldCallbackExecutor;

    private final ConcurrentMap<CalculatedFieldEntityCtxId, CalculatedFieldEntityCtx> states = new ConcurrentHashMap<>();

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
            var result = createStateByType(ctx);
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
    public void deleteStateFromStorage(CalculatedFieldEntityCtxId calculatedFieldEntityCtxId, TbCallback callback) {
        stateService.removeState(calculatedFieldEntityCtxId, callback);
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        var result = new HashMap<TopicPartitionInfo, List<ListenableFuture<?>>>();
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
            callback.onFailure(e);
        }
    }

    @Override
    public void pushMsgToLinks(CalculatedFieldTelemetryMsg msg, List<CalculatedFieldEntityCtxId> linkedCalculatedFields, TbCallback callback) {
        Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> unicasts = new HashMap<>();
        List<CalculatedFieldEntityCtxId> broadcasts = new ArrayList<>();
        for (CalculatedFieldEntityCtxId link : linkedCalculatedFields) {
            var linkEntityId = link.entityId();
            var linkEntityType = linkEntityId.getEntityType();
            // Let's assume number of entities in profile is N, and number of partitions is P. If N > P, we save by broadcasting to all partitions. Usually N >> P.
            boolean broadcast = EntityType.DEVICE_PROFILE.equals(linkEntityType) || EntityType.ASSET_PROFILE.equals(linkEntityType);
            if (broadcast) {
                broadcasts.add(link);
            } else {
                TopicPartitionInfo tpi = partitionService.resolve(CALCULATED_FIELD_QUEUE_KEY, link.entityId());
                unicasts.computeIfAbsent(tpi, k -> new ArrayList<>()).add(link);
            }
        }
        MultipleTbCallback linkCallback = new MultipleTbCallback(2, callback);
        if (!broadcasts.isEmpty()) {
            broadcast(broadcasts, msg, linkCallback);
        } else {
            linkCallback.onSuccess();
        }
        if (!unicasts.isEmpty()) {
            unicast(unicasts, msg, linkCallback);
        } else {
            linkCallback.onSuccess();
        }
    }

    private void unicast(Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> unicasts, CalculatedFieldTelemetryMsg msg, MultipleTbCallback mainCallback) {
        TbQueueCallback callback = new TbCallbackWrapper(new MultipleTbCallback(unicasts.size(), mainCallback));
        unicasts.forEach((topicPartitionInfo, ctxIds) -> {
            CalculatedFieldLinkedTelemetryMsgProto linkedTelemetryMsgProto = buildLinkedTelemetryMsgProto(msg.getProto(), ctxIds);
            clusterService.pushMsgToCalculatedFields(topicPartitionInfo, UUID.randomUUID(),
                    ToCalculatedFieldMsg.newBuilder().setLinkedTelemetryMsg(linkedTelemetryMsgProto).build(), callback);
        });
    }

    private void broadcast(List<CalculatedFieldEntityCtxId> broadcasts, CalculatedFieldTelemetryMsg msg, MultipleTbCallback mainCallback) {
        TbQueueCallback callback = new TbCallbackWrapper(mainCallback);
        CalculatedFieldLinkedTelemetryMsgProto linkedTelemetryMsgProto = buildLinkedTelemetryMsgProto(msg.getProto(), broadcasts);
        clusterService.broadcastToCalculatedFields(ToCalculatedFieldNotificationMsg.newBuilder().setLinkedTelemetryMsg(linkedTelemetryMsgProto).build(), callback);
    }

    private CalculatedFieldLinkedTelemetryMsgProto buildLinkedTelemetryMsgProto(CalculatedFieldTelemetryMsgProto telemetryProto, List<CalculatedFieldEntityCtxId> links) {
        Builder builder = CalculatedFieldLinkedTelemetryMsgProto.newBuilder();
        builder.setMsg(telemetryProto);
        for (CalculatedFieldEntityCtxId link : links) {
            builder.addLinks(toProto(link));
        }
        return builder.build();
    }

    //TODO: IM: move to utils;
    private CalculatedFieldEntityCtxIdProto toProto(CalculatedFieldEntityCtxId ctxId) {
        return CalculatedFieldEntityCtxIdProto.newBuilder()
                .setTenantIdMSB(ctxId.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(ctxId.tenantId().getId().getLeastSignificantBits())
                .setCalculatedFieldIdMSB(ctxId.cfId().getId().getMostSignificantBits())
                .setCalculatedFieldIdLSB(ctxId.cfId().getId().getLeastSignificantBits())
                .setEntityType(ctxId.entityId().getEntityType().name())
                .setEntityIdMSB(ctxId.entityId().getId().getMostSignificantBits())
                .setEntityIdLSB(ctxId.entityId().getId().getLeastSignificantBits())
                .build();
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
        long maxDataPoints = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg);
        int limit = argument.getLimit() == 0 ? (int) maxDataPoints : argument.getLimit();

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

    private CalculatedFieldState createStateByType(CalculatedFieldCtx ctx) {
        return switch (ctx.getCfType()) {
            case SIMPLE -> new SimpleCalculatedFieldState(ctx.getArgNames());
            case SCRIPT -> new ScriptCalculatedFieldState(ctx.getArgNames());
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

        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = buildTelemetryMsgProto(request.getTenantId(), request.getEntityId(), request.getPreviousCalculatedFieldIds(), request.getTbMsgId(), request.getTbMsgType());
        List<TsKvEntry> entries = request.getEntries();
        List<Long> versions = result.getVersions();
        for (int i = 0; i < entries.size(); i++) {
            long tsVersion = versions.get(i);
            TsKvProto tsProto = toTsKvProto(entries.get(i)).toBuilder().setVersion(tsVersion).build();
            telemetryMsg.addTsData(tsProto);
        }
        msg.setTelemetryMsg(telemetryMsg.build());

        return msg.build();
    }

    private ToCalculatedFieldMsg toCalculatedFieldTelemetryMsgProto(AttributesSaveRequest request, List<Long> versions) {
        ToCalculatedFieldMsg.Builder msg = ToCalculatedFieldMsg.newBuilder();

        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = buildTelemetryMsgProto(request.getTenantId(), request.getEntityId(), request.getPreviousCalculatedFieldIds(), request.getTbMsgId(), request.getTbMsgType());
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

    private CalculatedFieldTelemetryMsgProto.Builder buildTelemetryMsgProto(TenantId tenantId, EntityId entityId, List<CalculatedFieldId> calculatedFieldIds, UUID tbMsgId, TbMsgType tbMsgType) {
        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = CalculatedFieldTelemetryMsgProto.newBuilder();

        telemetryMsg.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        telemetryMsg.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());

        telemetryMsg.setEntityType(entityId.getEntityType().name());
        telemetryMsg.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        telemetryMsg.setEntityIdLSB(entityId.getId().getLeastSignificantBits());

        if (calculatedFieldIds != null) {
            for (CalculatedFieldId cfId : calculatedFieldIds) {
                telemetryMsg.addPreviousCalculatedFields(toProto(cfId));
            }
        }

        if (tbMsgId != null) {
            telemetryMsg.setTbMsgIdMSB(tbMsgId.getMostSignificantBits());
            telemetryMsg.setTbMsgIdLSB(tbMsgId.getLeastSignificantBits());
        }

        if (tbMsgType != null) {
            telemetryMsg.setTbMsgType(tbMsgType.name());
        }

        return telemetryMsg;
    }

    private CalculatedFieldIdProto toProto(CalculatedFieldId cfId) {
        return CalculatedFieldIdProto.newBuilder()
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

    private static class TbCallbackWrapper implements TbQueueCallback {
        private final TbCallback callback;

        public TbCallbackWrapper(TbCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            callback.onSuccess();
        }

        @Override
        public void onFailure(Throwable t) {
            callback.onFailure(t);
        }
    }

}
