/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldTelemetryMsg;
import org.thingsboard.server.actors.calculatedField.MultipleTbCallback;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.RelationQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto.Builder;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.TsRollingArgumentEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultKvEntry;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createStateByType;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformSingleValueArgument;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toProto;

@TbRuleEngineComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldProcessingService implements CalculatedFieldProcessingService {

    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private final TbClusterService clusterService;
    private final ApiLimitService apiLimitService;
    private final PartitionService partitionService;
    private final RelationService relationService;

    private ListeningExecutorService calculatedFieldCallbackExecutor;

    @PostConstruct
    public void init() {
        calculatedFieldCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field-callback"));
    }

    @PreDestroy
    public void stop() {
        if (calculatedFieldCallbackExecutor != null) {
            calculatedFieldCallbackExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<CalculatedFieldState> fetchStateFromDb(CalculatedFieldCtx ctx, EntityId entityId) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = switch (ctx.getCalculatedField().getType()) {
            case GEOFENCING -> fetchGeofencingCalculatedFieldArguments(ctx, entityId, false);
            case SIMPLE, SCRIPT -> {
                Map<String, ListenableFuture<ArgumentEntry>> futures = new HashMap<>();
                for (var entry : ctx.getArguments().entrySet()) {
                    var argEntityId = resolveEntityId(entityId, entry);
                    var argValueFuture = fetchKvEntry(ctx.getTenantId(), argEntityId, entry.getValue());
                    futures.put(entry.getKey(), argValueFuture);
                }
                yield futures;
            }
        };
        return Futures.whenAllComplete(argFutures.values()).call(() -> {
            var result = createStateByType(ctx);
            result.updateState(ctx, resolveArgumentFutures(argFutures));
            return result;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public Map<String, ArgumentEntry> fetchDynamicArgsFromDb(CalculatedFieldCtx ctx, EntityId entityId) {
        // only geofencing calculated fields supports dynamic arguments scheduled updates
        if (!ctx.getCalculatedField().getType().equals(CalculatedFieldType.GEOFENCING)) {
            return Map.of();
        }
        return resolveArgumentFutures(fetchGeofencingCalculatedFieldArguments(ctx, entityId, true));
    }

    private Map<String, ListenableFuture<ArgumentEntry>> fetchGeofencingCalculatedFieldArguments(CalculatedFieldCtx ctx, EntityId entityId, boolean dynamicArgumentsOnly) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        Set<Entry<String, Argument>> entries = ctx.getArguments().entrySet();
        if (dynamicArgumentsOnly) {
            entries = entries.stream()
                    .filter(entry -> entry.getValue().hasDynamicSource())
                    .collect(Collectors.toSet());
        }
        for (var entry : entries) {
            switch (entry.getKey()) {
                case ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY ->
                        argFutures.put(entry.getKey(), fetchKvEntry(ctx.getTenantId(), entityId, entry.getValue()));
                default -> {
                    var resolvedEntityIdsFuture = resolveGeofencingEntityIds(ctx.getTenantId(), entityId, entry);
                    argFutures.put(entry.getKey(), Futures.transformAsync(resolvedEntityIdsFuture, resolvedEntityIds ->
                            fetchGeofencingKvEntry(ctx.getTenantId(), resolvedEntityIds, entry.getValue()), MoreExecutors.directExecutor()));
                }
            }
        }
        return argFutures;
    }

    @Override
    public Map<String, ArgumentEntry> fetchArgsFromDb(TenantId tenantId, EntityId entityId, Map<String, Argument> arguments) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        for (var entry : arguments.entrySet()) {
            if (entry.getValue().hasDynamicSource()) {
                continue;
            }
            var argEntityId = resolveEntityId(entityId, entry);
            var argValueFuture = fetchKvEntry(tenantId, argEntityId, entry.getValue());
            argFutures.put(entry.getKey(), argValueFuture);
        }
        return resolveArgumentFutures(argFutures);
    }

    private Map<String, ArgumentEntry> resolveArgumentFutures(Map<String, ListenableFuture<ArgumentEntry>> argFutures) {
        return argFutures.entrySet().stream()
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
                ));
    }

    @Override
    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, CalculatedFieldResult calculatedFieldResult, List<CalculatedFieldId> cfIds, TbCallback callback) {
        try {
            OutputType type = calculatedFieldResult.getType();
            TbMsgType msgType = OutputType.ATTRIBUTES.equals(type) ? TbMsgType.POST_ATTRIBUTES_REQUEST : TbMsgType.POST_TELEMETRY_REQUEST;
            TbMsgMetaData md = OutputType.ATTRIBUTES.equals(type) ? new TbMsgMetaData(Map.of(SCOPE, calculatedFieldResult.getScope().name())) : TbMsgMetaData.EMPTY;
            TbMsg msg = TbMsg.newMsg().type(msgType).originator(entityId).previousCalculatedFieldIds(cfIds).metaData(md).data(calculatedFieldResult.toStringOrElseNull()).build();
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
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, link.tenantId(), link.entityId());
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


    private EntityId resolveEntityId(EntityId entityId, Entry<String, Argument> entry) {
        return entry.getValue().getRefEntityId() != null ? entry.getValue().getRefEntityId() : entityId;
    }

    private ListenableFuture<List<EntityId>> resolveGeofencingEntityIds(TenantId tenantId, EntityId entityId, Entry<String, Argument> entry) {
        Argument value = entry.getValue();
        if (value.getRefEntityId() != null) {
            return Futures.immediateFuture(List.of(value.getRefEntityId()));
        }
        if (!value.hasDynamicSource()) {
            return Futures.immediateFuture(List.of(entityId));
        }
        var refDynamicSourceConfiguration = value.getRefDynamicSourceConfiguration();
        return switch (refDynamicSourceConfiguration.getType()) {
            case RELATION_QUERY -> {
                var configuration = (RelationQueryDynamicSourceConfiguration) refDynamicSourceConfiguration;
                if (configuration.isSimpleRelation()) {
                    yield switch (configuration.getDirection()) {
                        case FROM ->
                                Futures.transform(relationService.findByFromAndTypeAsync(tenantId, entityId, configuration.getRelationType(), RelationTypeGroup.COMMON),
                                        configuration::resolveEntityIds, calculatedFieldCallbackExecutor);
                        case TO ->
                                Futures.transform(relationService.findByToAndTypeAsync(tenantId, entityId, configuration.getRelationType(), RelationTypeGroup.COMMON),
                                        configuration::resolveEntityIds, calculatedFieldCallbackExecutor);
                    };
                }
                yield Futures.transform(relationService.findByQuery(tenantId, configuration.toEntityRelationsQuery(entityId)),
                        configuration::resolveEntityIds, calculatedFieldCallbackExecutor);
            }
        };
    }

    private ListenableFuture<ArgumentEntry> fetchGeofencingKvEntry(TenantId tenantId, List<EntityId> geofencingEntities, Argument argument) {
        if (argument.getRefEntityKey().getType() != ArgumentType.ATTRIBUTE) {
            throw new IllegalStateException("Unsupported argument key type: " + argument.getRefEntityKey().getType());
        }
        List<ListenableFuture<Entry<EntityId, AttributeKvEntry>>> kvFutures = geofencingEntities.stream()
                .map(entityId -> {
                    var attributesFuture = attributesService.find(
                            tenantId,
                            entityId,
                            argument.getRefEntityKey().getScope(),
                            argument.getRefEntityKey().getKey()
                    );
                    return Futures.transform(attributesFuture, resultOpt ->
                                    Map.entry(entityId, resultOpt.orElseGet(() ->
                                            new BaseAttributeKvEntry(createDefaultKvEntry(argument), System.currentTimeMillis(), 0L))),
                            calculatedFieldCallbackExecutor
                    );
                }).collect(Collectors.toList());

        ListenableFuture<List<Entry<EntityId, AttributeKvEntry>>> allFutures = Futures.allAsList(kvFutures);

        return Futures.transform(allFutures, entries -> ArgumentEntry.createGeofencingValueArgument(entries.stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue))), MoreExecutors.directExecutor());
    }

    private ListenableFuture<ArgumentEntry> fetchKvEntry(TenantId tenantId, EntityId entityId, Argument argument) {
        return switch (argument.getRefEntityKey().getType()) {
            case TS_ROLLING -> fetchTsRolling(tenantId, entityId, argument);
            case ATTRIBUTE -> transformSingleValueArgument(
                    Futures.transform(attributesService.find(tenantId, entityId, argument.getRefEntityKey().getScope(), argument.getRefEntityKey().getKey()),
                            result -> result.or(() -> Optional.of(new BaseAttributeKvEntry(createDefaultKvEntry(argument), System.currentTimeMillis(), 0L))),
                            calculatedFieldCallbackExecutor));
            case TS_LATEST -> transformSingleValueArgument(
                    Futures.transform(
                            timeseriesService.findLatest(tenantId, entityId, argument.getRefEntityKey().getKey()),
                            result -> result.or(() -> Optional.of(new BasicTsKvEntry(System.currentTimeMillis(), createDefaultKvEntry(argument), 0L))),
                            calculatedFieldCallbackExecutor));
        };
    }

    private ListenableFuture<ArgumentEntry> fetchTsRolling(TenantId tenantId, EntityId entityId, Argument argument) {
        long currentTime = System.currentTimeMillis();
        long timeWindow = argument.getTimeWindow() == 0 ? System.currentTimeMillis() : argument.getTimeWindow();
        long startTs = currentTime - timeWindow;
        long maxDataPoints = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg);
        int argumentLimit = argument.getLimit();
        int limit = argumentLimit == 0 || argumentLimit > maxDataPoints ? (int) maxDataPoints : argument.getLimit();

        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startTs, currentTime, 0, limit, Aggregation.NONE);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsRollingFuture, tsRolling -> tsRolling == null ? new TsRollingArgumentEntry(limit, timeWindow) : ArgumentEntry.createTsRollingArgument(tsRolling, limit, timeWindow), calculatedFieldCallbackExecutor);
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
