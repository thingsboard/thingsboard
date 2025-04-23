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
package org.thingsboard.server.actors.calculatedField;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.CfReprocessingTask;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.cf.CalculatedFieldCache;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.CalculatedFieldStateService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.TsRollingArgumentEntry;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldReprocessingService implements CalculatedFieldReprocessingService {

    private static final Set<EntityType> supportedReprocessingEntities = EnumSet.of(
            EntityType.DEVICE, EntityType.ASSET
    );

    @Value("${actors.calculated_fields.calculation_timeout:5}")
    private long cfCalculationResultTimeout;

    @Value("${queue.calculated_fields.telemetry_fetch_pack_size:1000}")
    private int telemetryFetchPackSize;

    private final CalculatedFieldCache calculatedFieldCache;
    private final TimeseriesService timeseriesService;
    private final ApiLimitService apiLimitService;
    private final CalculatedFieldProcessingService cfProcessingService;
    private final CalculatedFieldStateService cfStateService;

    private ListeningExecutorService calculatedFieldCallbackExecutor;

    private final ConcurrentMap<CalculatedFieldEntityCtxId, CalculatedFieldState> states = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        calculatedFieldCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field-reprocessing-callback"));
    }

    @PreDestroy
    public void stop() {
        if (calculatedFieldCallbackExecutor != null) {
            calculatedFieldCallbackExecutor.shutdownNow();
        }
    }

    @Override
    public void reprocess(CfReprocessingTask task, TbCallback callback) throws CalculatedFieldException {
        TenantId tenantId = task.getTenantId();
        EntityId entityId = task.getEntityId();

        if (!supportedReprocessingEntities.contains(entityId.getEntityType())) {
            throw new IllegalArgumentException("EntityType '" + entityId.getEntityType() + "' is not supported for reprocessing.");
        }

        CalculatedFieldCtx ctx = getCalculatedFieldCtx(task.getCalculatedField().getId());// fixme: use calculated field from the task
        Map<String, Argument> arguments = ctx.getArguments();

        boolean containsAttributes = arguments.values().stream()
                .map(Argument::getRefEntityKey)
                .anyMatch(key -> ArgumentType.ATTRIBUTE.equals(key.getType()));

        if (containsAttributes) {
            throw new IllegalArgumentException("Calculated Fields do not support reprocessing of `ATTRIBUTE` arguments.");
        }

        long startTs = task.getStartTs();
        long endTs = task.getEndTs();

        performInitialProcessing(tenantId, entityId, ctx, startTs);

        Map<String, List<TsKvEntry>> telemetryBuffers = arguments.entrySet().stream()
                .collect(Collectors.toMap(
                                Entry::getKey,
                                entry -> fetchTelemetryBatch(tenantId, entityId, entry.getValue(), startTs, endTs, telemetryFetchPackSize)
                        )
                );

        while (true) {
            long minTs = telemetryBuffers.values().stream()
                    .filter(buffer -> !buffer.isEmpty())
                    .mapToLong(buffer -> buffer.get(0).getTs())
                    .min().orElse(Long.MAX_VALUE);

            if (minTs == Long.MAX_VALUE) {
                callback.onSuccess();
                break;
            }

            Map<String, ArgumentEntry> updatedArgs = new HashMap<>();

            for (Map.Entry<String, List<TsKvEntry>> entry : telemetryBuffers.entrySet()) {
                String argName = entry.getKey();
                List<TsKvEntry> buffer = entry.getValue();

                if (!buffer.isEmpty() && buffer.get(0).getTs() == minTs) {
                    TsKvEntry tsEntry = buffer.remove(0);
                    updatedArgs.put(argName, ArgumentEntry.createSingleValueArgument(tsEntry));

                    if (buffer.isEmpty()) {
                        Argument arg = ctx.getArguments().get(argName);
                        List<TsKvEntry> nextBatch = fetchTelemetryBatch(tenantId, entityId, arg, tsEntry.getTs() + 1, endTs, telemetryFetchPackSize);
                        if (!nextBatch.isEmpty()) {
                            telemetryBuffers.put(argName, nextBatch);
                        }
                    }
                }
            }

            processArgumentValuesUpdate(tenantId, entityId, ctx, updatedArgs, minTs);
        }
    }

    private CalculatedFieldCtx getCalculatedFieldCtx(CalculatedFieldId calculatedFieldId) throws CalculatedFieldException {
        CalculatedFieldCtx ctx = calculatedFieldCache.getCalculatedFieldCtx(calculatedFieldId);
        if (ctx == null) {
            log.debug("No calculated field found for id {}", calculatedFieldId);
            throw new IllegalArgumentException("No calculated field found for id " + calculatedFieldId);
        }
        try {
            ctx.init();
            return ctx;
        } catch (Exception e) {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(ctx.getEntityId()).cause(e).errorMessage("Failed to initialize CF context").build();
        }
    }

    private void performInitialProcessing(TenantId tenantId, EntityId entityId, CalculatedFieldCtx ctx, long cfExecutionTs) throws CalculatedFieldException {
        try {
            var state = getOrInitState(tenantId, entityId, ctx, cfExecutionTs);
            if (state.isSizeOk()) {
                processStateIfReady(tenantId, entityId, ctx, state, cfExecutionTs);
            } else {
                throw new RuntimeException(ctx.getSizeExceedsLimitMessage());
            }
        } catch (Exception e) {
            if (e instanceof CalculatedFieldException cfe) {
                throw cfe;
            }
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    private List<TsKvEntry> fetchTelemetryBatch(TenantId tenantId, EntityId entityId, Argument argument, long startTs, long endTs, int limit) {
        EntityId sourceEntityId = argument.getRefEntityId() != null ? argument.getRefEntityId() : entityId;
        try {
            ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startTs, endTs, 0, limit, Aggregation.NONE, "ASC");
            return timeseriesService.findAll(tenantId, sourceEntityId, List.of(query)).get();
        } catch (Exception e) {
            log.warn("Failed to fetch telemetry for [{}:{}]", sourceEntityId, argument.getRefEntityKey().getKey(), e);
            return Collections.emptyList();
        }
    }

    private void processStateIfReady(TenantId tenantId, EntityId entityId, CalculatedFieldCtx ctx, CalculatedFieldState state, long ts) throws CalculatedFieldException {
        CalculatedFieldEntityCtxId ctxId = new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId);
        boolean stateSizeChecked = false;
        try {
            if (ctx.isInitialized() && state.isReady()) {
                CalculatedFieldResult calculationResult = state.performCalculation(ctx).get(cfCalculationResultTimeout, TimeUnit.SECONDS);
                state.checkStateSize(ctxId, ctx.getMaxStateSize());
                stateSizeChecked = true;
                if (state.isSizeOk()) {
                    if (!calculationResult.isEmpty()) {
                        cfProcessingService.pushMsgToRuleEngine(tenantId, entityId, checkAndSetTs(calculationResult, ts), Collections.emptyList(), TbCallback.EMPTY);
                    }
                }
            }
        } catch (Exception e) {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).msgId(null).msgType(null).arguments(state.getArguments()).cause(e).build();
        } finally {
            if (!stateSizeChecked) {
                state.checkStateSize(ctxId, ctx.getMaxStateSize());
            }
            if (state.isSizeOk()) {
                cfStateService.persistState(ctxId, state, TbCallback.EMPTY);
            } else {
                removeStateAndRaiseSizeException(ctxId, CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build(), TbCallback.EMPTY);
            }
        }
    }

    private CalculatedFieldResult checkAndSetTs(CalculatedFieldResult result, long ts) {
        JsonNode resultJson = result.getResult();
        JsonNode newResultJson = resultJson.deepCopy();
        if (newResultJson.isObject() && !newResultJson.has("ts")) {
            if (!newResultJson.has("values")) {
                ObjectNode newResult = JacksonUtil.newObjectNode();
                newResult.put("ts", ts);
                newResult.set("values", newResultJson);
                newResultJson = newResult;
            }
        }
        if (newResultJson.isArray()) {
            for (JsonNode entry : newResultJson) {
                if (!entry.has("ts") && entry.isObject()) {
                    if (!entry.has("values")) {
                        ObjectNode newEntry = JacksonUtil.newObjectNode();
                        newEntry.put("ts", ts);
                        newEntry.set("values", entry);
                        entry = newEntry;
                    }
                    ((ObjectNode) entry).put("ts", ts);
                }
            }
        }
        return new CalculatedFieldResult(result.getType(), result.getScope(), newResultJson);
    }

    private void processArgumentValuesUpdate(TenantId tenantId, EntityId entityId, CalculatedFieldCtx ctx, Map<String, ArgumentEntry> newArgValues, long ts) throws CalculatedFieldException {
        if (newArgValues.isEmpty()) {
            log.info("[{}] No argument values to process for CF.", ctx.getCfId());
        }
        CalculatedFieldEntityCtxId ctxId = new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId);
        CalculatedFieldState state = states.get(ctxId);
        if (state == null) {
            state = createStateByType(ctx);
            states.put(ctxId, state);
        }
        if (state.isSizeOk()) {
            if (state.updateState(ctx, newArgValues)) {
                processStateIfReady(tenantId, entityId, ctx, state, ts);
            }
        } else {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build();
        }
    }

    private void removeStateAndRaiseSizeException(CalculatedFieldEntityCtxId ctxId, CalculatedFieldException ex, TbCallback callback) throws CalculatedFieldException {
        // We remove the state, but remember that it is over-sized in a local map.
        cfStateService.removeState(ctxId, new TbCallback() {
            @Override
            public void onSuccess() {
                callback.onFailure(ex);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(ex);
            }
        });
        throw ex;
    }

    @SneakyThrows
    private CalculatedFieldState getOrInitState(TenantId tenantId, EntityId entityId, CalculatedFieldCtx ctx, long cfExecutionTS) {
        CalculatedFieldEntityCtxId entityCtxId = new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId);
        CalculatedFieldState state = states.get(entityCtxId);
        if (state != null) {
            return state;
        } else {
            ListenableFuture<CalculatedFieldState> stateFuture = fetchStateFromDb(ctx, entityId, cfExecutionTS);
            // Ugly but necessary. We do not expect to often fetch data from DB. Only once per <Entity, CalculatedField> pair lifetime.
            // This call happens while processing the CF pack from the queue consumer. So the timeout should be relatively low.
            // Alternatively, we can fetch the state outside the actor system and push separate command to create this actor,
            // but this will significantly complicate the code.
            state = stateFuture.get(1, TimeUnit.MINUTES);
            state.checkStateSize(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId), ctx.getMaxStateSize());
            states.put(entityCtxId, state);
        }
        return state;
    }

    private ListenableFuture<CalculatedFieldState> fetchStateFromDb(CalculatedFieldCtx ctx, EntityId entityId, long cfExecutionTs) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        for (var entry : ctx.getArguments().entrySet()) {
            var argEntityId = entry.getValue().getRefEntityId() != null ? entry.getValue().getRefEntityId() : entityId;
            var argValueFuture = fetchKvEntryForReprocessing(ctx.getTenantId(), argEntityId, entry.getValue(), cfExecutionTs);
            argFutures.put(entry.getKey(), argValueFuture);
        }
        return Futures.whenAllComplete(argFutures.values()).call(() -> {
            var result = createStateByType(ctx);
            result.updateState(ctx, argFutures.entrySet().stream()
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

    private ListenableFuture<ArgumentEntry> fetchKvEntryForReprocessing(TenantId tenantId, EntityId entityId, Argument argument, long cfExecutionTs) {
        return switch (argument.getRefEntityKey().getType()) {
            case TS_ROLLING -> fetchTsRollingForReprocessing(tenantId, entityId, argument, cfExecutionTs);
            case ATTRIBUTE ->
                    throw new RuntimeException("Reprocessing is not supported for argument type 'ATTRIBUTE'.");
            case TS_LATEST ->
                    fetchTsLatestForReprocessing(tenantId, entityId, argument.getRefEntityKey().getKey(), cfExecutionTs);
        };
    }

    private ListenableFuture<ArgumentEntry> fetchTsLatestForReprocessing(TenantId tenantId, EntityId entityId, String key, long cfExecutionTs) {
        ReadTsKvQuery query = new BaseReadTsKvQuery(key, 0, cfExecutionTs, 0, 1, Aggregation.NONE);
        ListenableFuture<List<TsKvEntry>> tsKvListFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsKvListFuture, tsKvList -> {
            if (tsKvList.isEmpty()) {
                return new SingleValueArgumentEntry();
            }
            TsKvEntry tsKvEntry = tsKvList.get(0);
            if (tsKvEntry == null || tsKvEntry.getValue() == null) {
                return new SingleValueArgumentEntry();
            }
            return ArgumentEntry.createSingleValueArgument(tsKvEntry);
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchTsRollingForReprocessing(TenantId tenantId, EntityId entityId, Argument argument, long cfExecutionTs) {
        long argTimeWindow = argument.getTimeWindow() == 0 ? System.currentTimeMillis() : argument.getTimeWindow();
        long startInterval = cfExecutionTs - argTimeWindow;
        long maxDataPoints = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg);
        int argumentLimit = argument.getLimit();
        int limit = argumentLimit == 0 || argumentLimit > maxDataPoints ? (int) maxDataPoints : argument.getLimit();

        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startInterval, cfExecutionTs, 0, limit, Aggregation.NONE);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsRollingFuture, tsRolling -> tsRolling == null ? new TsRollingArgumentEntry(limit, argTimeWindow) : ArgumentEntry.createTsRollingArgument(tsRolling, limit, argTimeWindow), calculatedFieldCallbackExecutor);
    }

    private CalculatedFieldState createStateByType(CalculatedFieldCtx ctx) {
        return switch (ctx.getCfType()) {
            case SIMPLE -> new SimpleCalculatedFieldState(ctx.getArgNames());
            case SCRIPT -> new ScriptCalculatedFieldState(ctx.getArgNames());
        };
    }

}
