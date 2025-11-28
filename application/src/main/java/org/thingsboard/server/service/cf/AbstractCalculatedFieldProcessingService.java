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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest.Strategy;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.AttributesImmediateOutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.OutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesImmediateOutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.AggIntervalEntry;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.cf.CalculatedFieldType.PROPAGATION;
import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.dao.util.KvUtils.filterChangedAttr;
import static org.thingsboard.server.dao.util.KvUtils.toTsKvEntryList;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultAttributeEntry;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultKvEntry;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformAggMetricArgument;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformAggregationArgument;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformSingleValueArgument;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformTsRollingArgument;

@Data
@Slf4j
public abstract class AbstractCalculatedFieldProcessingService {

    protected final AttributesService attributesService;
    protected final TimeseriesService timeseriesService;
    protected final TelemetrySubscriptionService tsSubService;
    protected final ApiLimitService apiLimitService;
    protected final RelationService relationService;
    protected final OwnerService ownerService;

    protected ListeningExecutorService calculatedFieldCallbackExecutor;

    @PostConstruct
    public void init() {
        calculatedFieldCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), getExecutorNamePrefix()));
    }

    @PreDestroy
    public void stop() {
        if (calculatedFieldCallbackExecutor != null) {
            calculatedFieldCallbackExecutor.shutdownNow();
        }
    }

    protected abstract String getExecutorNamePrefix();

    protected ListenableFuture<Map<String, ArgumentEntry>> fetchArguments(CalculatedFieldCtx ctx, EntityId entityId, long ts) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = switch (ctx.getCfType()) {
            case GEOFENCING -> fetchGeofencingCalculatedFieldArguments(ctx, entityId, false, ts);
            case SIMPLE, SCRIPT, ALARM, PROPAGATION -> getBaseCalculatedFieldArguments(ctx, entityId, ts);
            case RELATED_ENTITIES_AGGREGATION -> fetchRelatedEntitiesAggArguments(ctx, entityId, ts);
            case ENTITY_AGGREGATION -> fetchEntityAggArguments(ctx, entityId, ts);
        };
        if (ctx.getCfType() == PROPAGATION) {
            argFutures.put(PROPAGATION_CONFIG_ARGUMENT, fetchPropagationCalculatedFieldArgument(ctx, entityId));
        }
        return Futures.whenAllComplete(argFutures.values())
                .call(() -> resolveArgumentFutures(argFutures),
                        MoreExecutors.directExecutor());
    }

    private Map<String, ListenableFuture<ArgumentEntry>> getBaseCalculatedFieldArguments(CalculatedFieldCtx ctx, EntityId entityId, long ts) {
        Map<String, ListenableFuture<ArgumentEntry>> futures = new HashMap<>();
        for (var entry : ctx.getArguments().entrySet()) {
            var argEntityId = resolveEntityId(ctx.getTenantId(), entityId, entry.getValue());
            var argValueFuture = fetchArgumentValue(ctx.getTenantId(), argEntityId, entry.getValue(), ts);
            futures.put(entry.getKey(), argValueFuture);
        }
        return futures;
    }

    protected EntityId resolveEntityId(TenantId tenantId, EntityId entityId, Argument argument) {
        if (argument.getRefEntityId() != null) {
            return argument.getRefEntityId();
        }
        if (!argument.hasOwnerSource()) {
            return entityId;
        }
        return resolveOwnerArgument(tenantId, entityId);
    }

    protected Map<String, ArgumentEntry> resolveArgumentFutures(Map<String, ListenableFuture<ArgumentEntry>> argFutures) {
        return argFutures.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // Keep the key as is
                        entry -> resolveArgumentValue(entry.getKey(), entry.getValue())
                ));
    }

    protected ArgumentEntry resolveArgumentValue(String key, ListenableFuture<ArgumentEntry> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException("Failed to fetch '" + key + "' argument: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to fetch '" + key + "' argument!", e);
        }
    }

    protected ListenableFuture<ArgumentEntry> fetchPropagationCalculatedFieldArgument(CalculatedFieldCtx ctx, EntityId entityId) {
        ListenableFuture<List<EntityId>> propagationEntityIds = fromDynamicSource(ctx.getTenantId(), entityId, ctx.getPropagationArgument());
        return Futures.transform(propagationEntityIds, ArgumentEntry::createPropagationArgument, MoreExecutors.directExecutor());
    }

    protected Map<String, ListenableFuture<ArgumentEntry>> fetchGeofencingCalculatedFieldArguments(CalculatedFieldCtx ctx, EntityId entityId, boolean dynamicArgumentsOnly, long startTs) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        Set<Map.Entry<String, Argument>> entries = ctx.getArguments().entrySet();
        if (dynamicArgumentsOnly) {
            entries = entries.stream()
                    .filter(entry -> entry.getValue().hasRelationQuerySource())
                    .collect(Collectors.toSet());
        }
        for (var entry : entries) {
            switch (entry.getKey()) {
                case ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY ->
                        argFutures.put(entry.getKey(), fetchArgumentValue(ctx.getTenantId(), entityId, entry.getValue(), startTs));
                default -> {
                    var resolvedEntityIdsFuture = resolveGeofencingEntityIds(ctx.getTenantId(), entityId, entry);
                    argFutures.put(entry.getKey(), Futures.transformAsync(resolvedEntityIdsFuture, resolvedEntityIds ->
                            fetchGeofencingKvEntry(ctx.getTenantId(), resolvedEntityIds, entry.getValue()), MoreExecutors.directExecutor()));
                }
            }
        }
        return argFutures;
    }

    protected Map<String, ListenableFuture<ArgumentEntry>> fetchRelatedEntitiesAggArguments(CalculatedFieldCtx ctx, EntityId entityId, long ts) {
        if (!(ctx.getCalculatedField().getConfiguration() instanceof RelatedEntitiesAggregationCalculatedFieldConfiguration config)) {
            return Collections.emptyMap();
        }
        ListenableFuture<List<EntityId>> relatedEntitiesFut = resolveRelatedEntities(ctx.getTenantId(), entityId, config.getRelation());

        return config.getArguments().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Futures.transformAsync(relatedEntitiesFut, relatedEntities -> fetchRelatedEntitiesArgumentEntry(ctx.getTenantId(), relatedEntities, entry.getValue(), ts), MoreExecutors.directExecutor())
                ));
    }

    protected Map<String, ListenableFuture<ArgumentEntry>> fetchEntityAggArguments(CalculatedFieldCtx ctx, EntityId entityId, long ts) {
        if (!(ctx.getCalculatedField().getConfiguration() instanceof EntityAggregationCalculatedFieldConfiguration config)) {
            return Collections.emptyMap();
        }
        return config.getArguments().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> fetchTimeSeries(ctx.getTenantId(), entityId, entry.getValue(), config.getInterval(), ts)
                ));
    }

    protected ListenableFuture<List<EntityId>> resolveRelatedEntities(TenantId tenantId, EntityId entityId, RelationPathLevel relation) {
        Predicate<EntityRelation> filter = entityRelation -> CalculatedField.isSupportedRefEntity(entityRelation.getFrom()) && CalculatedField.isSupportedRefEntity(entityRelation.getTo());
        ListenableFuture<List<EntityRelation>> relationsFut = relationService.findFilteredRelationsByPathQueryAsync(tenantId, new EntityRelationPathQuery(entityId, List.of(relation)), filter);

        return Futures.transform(relationsFut, relations -> {
            if (relations == null) {
                return Collections.emptyList();
            }

            return switch (relation.direction()) {
                case FROM -> relations.stream()
                        .map(EntityRelation::getTo)
                        .toList();
                case TO -> relations.stream()
                        .map(EntityRelation::getFrom)
                        .findFirst()
                        .map(List::of)
                        .orElseGet(Collections::emptyList);
            };
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<List<EntityId>> resolveGeofencingEntityIds(TenantId tenantId, EntityId entityId, Map.Entry<String, Argument> entry) {
        Argument value = entry.getValue();
        if (value.getRefEntityId() != null) {
            return Futures.immediateFuture(List.of(value.getRefEntityId()));
        }
        if (!value.hasDynamicSource()) {
            return Futures.immediateFuture(List.of(entityId));
        }
        return fromDynamicSource(tenantId, entityId, value);
    }

    private ListenableFuture<List<EntityId>> fromDynamicSource(TenantId tenantId, EntityId entityId, Argument value) {
        var refDynamicSourceConfiguration = value.getRefDynamicSourceConfiguration();
        return switch (refDynamicSourceConfiguration.getType()) {
            case CURRENT_OWNER -> Futures.immediateFuture(List.of(resolveOwnerArgument(tenantId, entityId)));
            case RELATION_PATH_QUERY -> {
                var configuration = (RelationPathQueryDynamicSourceConfiguration) refDynamicSourceConfiguration;
                Predicate<EntityRelation> filter = entityRelation -> CalculatedField.isSupportedRefEntity(entityRelation.getFrom()) && CalculatedField.isSupportedRefEntity(entityRelation.getTo());
                yield Futures.transform(relationService.findFilteredRelationsByPathQueryAsync(tenantId, configuration.toRelationPathQuery(entityId), filter),
                        configuration::resolveEntityIds, calculatedFieldCallbackExecutor);
            }
        };
    }

    private EntityId resolveOwnerArgument(TenantId tenantId, EntityId entityId) {
        return ownerService.getOwner(tenantId, entityId);
    }

    private ListenableFuture<ArgumentEntry> fetchGeofencingKvEntry(TenantId tenantId, List<EntityId> geofencingEntities, Argument argument) {
        if (argument.getRefEntityKey().getType() != ArgumentType.ATTRIBUTE) {
            throw new IllegalStateException("Unsupported argument key type: " + argument.getRefEntityKey().getType());
        }
        List<ListenableFuture<Map.Entry<EntityId, AttributeKvEntry>>> kvFutures = geofencingEntities.stream()
                .map(entityId -> {
                    var attributesFuture = attributesService.find(
                            tenantId,
                            entityId,
                            argument.getRefEntityKey().getScope(),
                            argument.getRefEntityKey().getKey()
                    );
                    return Futures.transform(attributesFuture, resultOpt ->
                                    Map.entry(entityId, resultOpt.orElseGet(() -> createDefaultAttributeEntry(argument, System.currentTimeMillis()))),
                            calculatedFieldCallbackExecutor
                    );
                }).collect(Collectors.toList());

        ListenableFuture<List<Map.Entry<EntityId, AttributeKvEntry>>> allFutures = Futures.allAsList(kvFutures);

        return Futures.transform(allFutures, entries -> ArgumentEntry.createGeofencingValueArgument(entries.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))), MoreExecutors.directExecutor());
    }

    public ListenableFuture<ArgumentEntry> fetchRelatedEntitiesArgumentEntry(TenantId tenantId, List<EntityId> aggEntities, Argument argument, long startTs) {
        List<ListenableFuture<Map.Entry<EntityId, ArgumentEntry>>> futures = aggEntities.stream()
                .map(entityId -> {
                    ListenableFuture<ArgumentEntry> argumentEntryFut = fetchArgumentValue(tenantId, entityId, argument, startTs);
                    return Futures.transform(argumentEntryFut, argumentEntry -> Map.entry(entityId, ArgumentEntry.createSingleValueArgument(entityId, argumentEntry)), MoreExecutors.directExecutor());
                })
                .toList();

        ListenableFuture<List<Map.Entry<EntityId, ? extends ArgumentEntry>>> allFutures = Futures.allAsList(futures);

        return Futures.transform(allFutures,
                entries -> ArgumentEntry.createAggArgument(
                        entries.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                ),
                MoreExecutors.directExecutor());
    }

    protected ListenableFuture<ArgumentEntry> fetchArgumentValue(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        return switch (argument.getRefEntityKey().getType()) {
            case TS_ROLLING -> fetchTsRolling(tenantId, entityId, argument, startTs);
            case ATTRIBUTE -> fetchAttribute(tenantId, entityId, argument, startTs);
            case TS_LATEST -> fetchTsLatest(tenantId, entityId, argument, startTs);
        };
    }

    protected ArgumentEntry fetchMetricDuringInterval(TenantId tenantId, EntityId entityId, String argKey, AggMetric metric, AggIntervalEntry interval) {
        AggFunction function = metric.getFunction();
        long intervalMs = interval.getEndTs() - interval.getStartTs();
        BaseReadTsKvQuery query = new BaseReadTsKvQuery(argKey, interval.getStartTs(), interval.getEndTs(), intervalMs, 1, Aggregation.valueOf(function.name()));
        ListenableFuture<ArgumentEntry> argumentEntryFut = fetchTimeSeriesInternal(tenantId, entityId, query, timeSeries -> transformAggMetricArgument(timeSeries, argKey, metric));
        return resolveArgumentValue(argKey, argumentEntryFut);
    }

    private ListenableFuture<ArgumentEntry> fetchTimeSeries(TenantId tenantId, EntityId entityId, Argument argument, AggInterval interval, long queryEndTs) {
        long intervalStartTs = interval.getCurrentIntervalStartTs();
        long intervalEndTs = interval.getCurrentIntervalEndTs();
        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), intervalStartTs, queryEndTs, 0, 1, Aggregation.NONE);
        return fetchTimeSeriesInternal(tenantId, entityId, query, timeSeries -> transformAggregationArgument(timeSeries, intervalStartTs, intervalEndTs));
    }

    private ListenableFuture<ArgumentEntry> fetchTsRolling(TenantId tenantId, EntityId entityId, Argument argument, long queryEndTs) {
        long argTimeWindow = argument.getTimeWindow() == 0 ? queryEndTs : argument.getTimeWindow();
        long startInterval = queryEndTs - argTimeWindow;
        ReadTsKvQuery query = buildTsRollingQuery(tenantId, argument, startInterval, queryEndTs);
        return fetchTimeSeriesInternal(tenantId, entityId, query, tsRolling -> transformTsRollingArgument(tsRolling, query.getLimit(), argTimeWindow));
    }

    private ListenableFuture<ArgumentEntry> fetchAttribute(TenantId tenantId, EntityId entityId, Argument argument, long defaultLastUpdateTs) {
        log.trace("[{}][{}] Fetching attribute for key {}", tenantId, entityId, argument.getRefEntityKey());
        var attributeOptFuture = attributesService.find(tenantId, entityId, argument.getRefEntityKey().getScope(), argument.getRefEntityKey().getKey());

        return Futures.transform(attributeOptFuture, attrOpt -> {
            log.debug("[{}][{}] Fetched attribute for key {}: {}", tenantId, entityId, argument.getRefEntityKey(), attrOpt);
            AttributeKvEntry attributeKvEntry = attrOpt.orElseGet(() -> new BaseAttributeKvEntry(createDefaultKvEntry(argument), defaultLastUpdateTs, SingleValueArgumentEntry.DEFAULT_VERSION));
            return transformSingleValueArgument(Optional.of(attributeKvEntry));
        }, calculatedFieldCallbackExecutor);
    }

    protected ListenableFuture<ArgumentEntry> fetchTsLatest(TenantId tenantId, EntityId entityId, Argument argument, long defaultTs) {
        String timeseriesKey = argument.getRefEntityKey().getKey();
        log.trace("[{}][{}] Fetching latest timeseries {}", tenantId, entityId, timeseriesKey);
        return transformSingleValueArgument(
                Futures.transform(
                        timeseriesService.findLatest(tenantId, entityId, timeseriesKey),
                        result -> {
                            log.debug("[{}][{}] Fetched latest timeseries {}: {}", tenantId, entityId, timeseriesKey, result);
                            return result.or(() -> Optional.of(new BasicTsKvEntry(defaultTs, createDefaultKvEntry(argument), SingleValueArgumentEntry.DEFAULT_VERSION)));
                        }, calculatedFieldCallbackExecutor));
    }

    private ListenableFuture<ArgumentEntry> fetchTimeSeriesInternal(TenantId tenantId, EntityId entityId, ReadTsKvQuery query, Function<List<TsKvEntry>, ArgumentEntry> transformArgument) {
        log.trace("[{}][{}] Fetching timeseries for query {}", tenantId, entityId, query);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));
        return Futures.transform(tsRollingFuture, tsRolling -> {
            log.debug("[{}][{}] Fetched {} timeseries for query {}", tenantId, entityId, tsRolling == null ? 0 : tsRolling.size(), query);
            return transformArgument.apply(tsRolling);
        }, calculatedFieldCallbackExecutor);
    }

    private ReadTsKvQuery buildTsRollingQuery(TenantId tenantId, Argument argument, long startTs, long endTs) {
        long maxDataPoints = apiLimitService.getLimit(
                tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg);
        int argumentLimit = argument.getLimit();
        int limit = argumentLimit == 0 || argumentLimit > maxDataPoints ? (int) maxDataPoints : argumentLimit;
        return new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startTs, endTs, 0, limit, Aggregation.NONE);
    }

    protected void saveTelemetryResult(TenantId tenantId, EntityId entityId, TelemetryCalculatedFieldResult cfResult, List<CalculatedFieldId> cfIds, TbCallback callback) {
        OutputType type = cfResult.getType();
        JsonElement jsonResult = JsonParser.parseString(Objects.requireNonNull(cfResult.stringValue()));

        log.trace("[{}][{}] Saving CF result: {}", tenantId, entityId, jsonResult);

        SettableFuture<Void> future = SettableFuture.create();
        switch (type) {
            case ATTRIBUTES -> saveAttributes(tenantId, entityId, jsonResult, cfResult.getOutputStrategy(), cfResult.getScope(), cfIds, future);
            case TIME_SERIES -> saveTimeSeries(tenantId, entityId, jsonResult, cfResult.getOutputStrategy(), cfIds, System.currentTimeMillis(), future);
        }

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(Void v) {
                callback.onSuccess();
                log.debug("[{}][{}] Saved CF result: {}", tenantId, entityId, cfResult);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
                log.error("[{}][{}] Failed to save CF result {}", tenantId, entityId, cfResult, t);
            }
        }, MoreExecutors.directExecutor());
    }

    private void saveAttributes(TenantId tenantId, EntityId entityId, JsonElement jsonResult, OutputStrategy outputStrategy, AttributeScope scope, List<CalculatedFieldId> cfIds, SettableFuture<Void> future) {
        if (!(outputStrategy instanceof AttributesImmediateOutputStrategy attOutputStrategy)) {
            future.setException(new IllegalArgumentException("Only AttributeImmediateOutputStrategy is supported."));
        } else {
            AttributesSaveRequest.Strategy strategy = new Strategy(attOutputStrategy.isSaveAttribute(), attOutputStrategy.isSendWsUpdate(), attOutputStrategy.isProcessCfs());
            List<AttributeKvEntry> newAttributes = JsonConverter.convertToAttributes(jsonResult);

            if (!attOutputStrategy.isUpdateAttributesOnlyOnValueChange()) {
                saveAttributesInternal(tenantId, entityId, scope, cfIds, newAttributes, strategy, future);
                return;
            }

            List<String> keys = newAttributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
            ListenableFuture<List<AttributeKvEntry>> findFuture = attributesService.find(tenantId, entityId, scope, keys);

            DonAsynchron.withCallback(findFuture,
                    existingAttributes -> {
                        List<AttributeKvEntry> changed = filterChangedAttr(existingAttributes, newAttributes);
                        if (changed.isEmpty()) {
                            future.set(null);
                            return;
                        }
                        saveAttributesInternal(tenantId, entityId, scope, cfIds, changed, strategy, future);
                    },
                    future::setException,
                    MoreExecutors.directExecutor());
        }
    }

    private void saveAttributesInternal(TenantId tenantId, EntityId entityId,
                                        AttributeScope scope,
                                        List<CalculatedFieldId> cfIds,
                                        List<AttributeKvEntry> entries,
                                        AttributesSaveRequest.Strategy strategy,
                                        SettableFuture<Void> future) {
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .scope(scope)
                .entries(entries)
                .strategy(strategy)
                .previousCalculatedFieldIds(cfIds)
                .future(future)
                .build());
    }

    private void saveTimeSeries(TenantId tenantId, EntityId entityId, JsonElement jsonResult, OutputStrategy outputStrategy, List<CalculatedFieldId> cfIds, long ts, SettableFuture<Void> future) {
        if (!(outputStrategy instanceof TimeSeriesImmediateOutputStrategy tsOutputStrategy)) {
            future.setException(new IllegalArgumentException("Only TimeSeriesImmediateOutputStrategy is supported."));
        } else {
            TimeseriesSaveRequest.Strategy strategy = new TimeseriesSaveRequest.Strategy(tsOutputStrategy.isSaveTimeSeries(), tsOutputStrategy.isSaveLatest(), tsOutputStrategy.isSendWsUpdate(), tsOutputStrategy.isProcessCfs());
            saveTimeSeriesInternal(tenantId, entityId, jsonResult, tsOutputStrategy.getTtl(), cfIds, ts, strategy, future);
        }
    }

    private void saveTimeSeriesInternal(TenantId tenantId, EntityId entityId, JsonElement jsonResult, Long ttl, List<CalculatedFieldId> cfIds, long ts, TimeseriesSaveRequest.Strategy strategy, SettableFuture<Void> future) {
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(jsonResult, ts);
        if (tsKvMap.isEmpty()) {
            future.set(null);
            return;
        }
        List<TsKvEntry> tsEntries = toTsKvEntryList(tsKvMap);
        TimeseriesSaveRequest.Builder builder = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .entries(tsEntries)
                .strategy(strategy)
                .future(future);
        if (ttl != null) {
            builder.ttl(ttl);
        }
        if (cfIds != null && !cfIds.isEmpty()) {
            builder.previousCalculatedFieldIds(cfIds);
        }
        tsSubService.saveTimeseries(builder.build());
    }

}
