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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultKvEntry;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createStateByType;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformSingleValueArgument;

@Data
@Slf4j
public abstract class AbstractCalculatedFieldProcessingService {

    protected final AttributesService attributesService;
    protected final TimeseriesService timeseriesService;
    protected final ApiLimitService apiLimitService;
    protected final RelationService relationService;

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

    public ListenableFuture<CalculatedFieldState> fetchStateFromDb(CalculatedFieldCtx ctx, EntityId entityId) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = switch (ctx.getCalculatedField().getType()) {
            case GEOFENCING -> fetchGeofencingCalculatedFieldArguments(ctx, entityId, false);
            case SIMPLE, SCRIPT -> {
                Map<String, ListenableFuture<ArgumentEntry>> futures = new HashMap<>();
                for (var entry : ctx.getArguments().entrySet()) {
                    var argEntityId = resolveEntityId(entityId, entry.getValue());
                    var argValueFuture = fetchArgumentValue(ctx.getTenantId(), argEntityId, entry.getValue(), System.currentTimeMillis());
                    futures.put(entry.getKey(), argValueFuture);
                }
                yield futures;
            }
        };
        return Futures.whenAllComplete(argFutures.values()).call(() -> {
            var result = createStateByType(ctx);
            result.updateState(ctx, resolveArgumentFutures(argFutures));
            // TODO: move to state.init() method after merge with alarm rules 2.0
            if (ctx.hasRelationQueryDynamicArguments() && result instanceof GeofencingCalculatedFieldState geofencingCalculatedFieldState) {
                geofencingCalculatedFieldState.setLastDynamicArgumentsRefreshTs(System.currentTimeMillis());
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    protected EntityId resolveEntityId(EntityId entityId, Argument argument) {
        return argument.getRefEntityId() != null ? argument.getRefEntityId() : entityId;
    }

    protected Map<String, ArgumentEntry> resolveArgumentFutures(Map<String, ListenableFuture<ArgumentEntry>> argFutures) {
        return argFutures.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // Keep the key as is
                        entry -> {
                            try {
                                return entry.getValue().get();
                            } catch (ExecutionException e) {
                                Throwable cause = e.getCause();
                                throw new RuntimeException("Failed to fetch " + entry.getKey() + ": " + cause.getMessage(), cause);
                            } catch (InterruptedException e) {
                                throw new RuntimeException("Failed to fetch" + entry.getKey(), e);
                            }
                        }
                ));
    }

    protected Map<String, ListenableFuture<ArgumentEntry>> fetchGeofencingCalculatedFieldArguments(CalculatedFieldCtx ctx, EntityId entityId, boolean dynamicArgumentsOnly) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        Set<Map.Entry<String, Argument>> entries = ctx.getArguments().entrySet();
        if (dynamicArgumentsOnly) {
            entries = entries.stream()
                    .filter(entry -> entry.getValue().hasDynamicSource())
                    .collect(Collectors.toSet());
        }
        for (var entry : entries) {
            switch (entry.getKey()) {
                case ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY ->
                        argFutures.put(entry.getKey(), fetchArgumentValue(ctx.getTenantId(), entityId, entry.getValue(), System.currentTimeMillis()));
                default -> {
                    var resolvedEntityIdsFuture = resolveGeofencingEntityIds(ctx.getTenantId(), entityId, entry);
                    argFutures.put(entry.getKey(), Futures.transformAsync(resolvedEntityIdsFuture, resolvedEntityIds ->
                            fetchGeofencingKvEntry(ctx.getTenantId(), resolvedEntityIds, entry.getValue()), MoreExecutors.directExecutor()));
                }
            }
        }
        return argFutures;
    }

    private ListenableFuture<List<EntityId>> resolveGeofencingEntityIds(TenantId tenantId, EntityId entityId, Map.Entry<String, Argument> entry) {
        Argument value = entry.getValue();
        if (value.getRefEntityId() != null) {
            return Futures.immediateFuture(List.of(value.getRefEntityId()));
        }
        if (!value.hasDynamicSource()) {
            return Futures.immediateFuture(List.of(entityId));
        }
        var refDynamicSourceConfiguration = value.getRefDynamicSourceConfiguration();
        return switch (refDynamicSourceConfiguration.getType()) {
            case RELATION_PATH_QUERY -> {
                var configuration = (RelationPathQueryDynamicSourceConfiguration) refDynamicSourceConfiguration;
                yield Futures.transform(relationService.findByRelationPathQueryAsync(tenantId, configuration.toRelationPathQuery(entityId)),
                        configuration::resolveEntityIds, calculatedFieldCallbackExecutor);
            }
        };
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
                                    Map.entry(entityId, resultOpt.orElseGet(() ->
                                            new BaseAttributeKvEntry(createDefaultKvEntry(argument), System.currentTimeMillis(), 0L))),
                            calculatedFieldCallbackExecutor
                    );
                }).collect(Collectors.toList());

        ListenableFuture<List<Map.Entry<EntityId, AttributeKvEntry>>> allFutures = Futures.allAsList(kvFutures);

        return Futures.transform(allFutures, entries -> ArgumentEntry.createGeofencingValueArgument(entries.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))), MoreExecutors.directExecutor());
    }

    protected ListenableFuture<ArgumentEntry> fetchArgumentValue(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        return switch (argument.getRefEntityKey().getType()) {
            case TS_ROLLING -> fetchTsRolling(tenantId, entityId, argument, startTs);
            case ATTRIBUTE -> fetchAttribute(tenantId, entityId, argument, startTs);
            case TS_LATEST -> fetchTsLatest(tenantId, entityId, argument, startTs);
        };
    }

    private ListenableFuture<ArgumentEntry> fetchTsRolling(TenantId tenantId, EntityId entityId, Argument argument, long queryEndTs) {
        long argTimeWindow = argument.getTimeWindow() == 0 ? queryEndTs : argument.getTimeWindow();
        long startInterval = queryEndTs - argTimeWindow;
        ReadTsKvQuery query = buildTsRollingQuery(tenantId, argument, startInterval, queryEndTs);

        log.trace("[{}][{}] Fetching timeseries for query {}", tenantId, entityId, query);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));
        return Futures.transform(tsRollingFuture, tsRolling -> {
            log.debug("[{}][{}] Fetched {} timeseries for query {}", tenantId, entityId, tsRolling == null ? 0 : tsRolling.size(), query);
            return ArgumentEntry.createTsRollingArgument(tsRolling, query.getLimit(), argTimeWindow);
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchAttribute(TenantId tenantId, EntityId entityId, Argument argument, long defaultLastUpdateTs) {
        log.trace("[{}][{}] Fetching attribute for key {}", tenantId, entityId, argument.getRefEntityKey());
        var attributeOptFuture = attributesService.find(tenantId, entityId, argument.getRefEntityKey().getScope(), argument.getRefEntityKey().getKey());

        return Futures.transform(attributeOptFuture, attrOpt -> {
            log.debug("[{}][{}] Fetched attribute for key {}: {}", tenantId, entityId, argument.getRefEntityKey(), attrOpt);
            AttributeKvEntry attributeKvEntry = attrOpt.orElseGet(() -> new BaseAttributeKvEntry(createDefaultKvEntry(argument), defaultLastUpdateTs, 0L));
            return transformSingleValueArgument(Optional.of(attributeKvEntry));
        }, calculatedFieldCallbackExecutor);
    }

    protected ListenableFuture<ArgumentEntry> fetchTsLatest(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        String timeseriesKey = argument.getRefEntityKey().getKey();
        log.trace("[{}][{}] Fetching latest timeseries {}", tenantId, entityId, timeseriesKey);
        return transformSingleValueArgument(
                Futures.transform(
                        timeseriesService.findLatest(tenantId, entityId, timeseriesKey),
                        result -> {
                            log.debug("[{}][{}] Fetched latest timeseries {}: {}", tenantId, entityId, timeseriesKey, result);
                            return result.or(() -> Optional.of(new BasicTsKvEntry(System.currentTimeMillis(), createDefaultKvEntry(argument), 0L)));
                        }, calculatedFieldCallbackExecutor));
    }

    private ReadTsKvQuery buildTsRollingQuery(TenantId tenantId, Argument argument, long startTs, long endTs) {
        long maxDataPoints = apiLimitService.getLimit(
                tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg);
        int argumentLimit = argument.getLimit();
        int limit = argumentLimit == 0 || argumentLimit > maxDataPoints ? (int) maxDataPoints : argumentLimit;
        return new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startTs, endTs, 0, limit, Aggregation.NONE);
    }

}
