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
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldException;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.CfReprocessingTask;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.TsRollingArgumentEntry;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultKvEntry;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createStateByType;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformSingleValueArgument;

@TbRuleEngineComponent
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

    private final TimeseriesService timeseriesService;
    private final AttributesService attributesService;
    private final TbelInvokeService tbelInvokeService;
    private final ApiLimitService apiLimitService;
    private final TbClusterService tbClusterService;

    private ListeningExecutorService calculatedFieldCallbackExecutor;

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
        log.debug("[{}] Received reprocess request for entityId [{}]", tenantId, entityId);

        if (!supportedReprocessingEntities.contains(entityId.getEntityType())) {
            throw new IllegalArgumentException("EntityType '" + entityId.getEntityType() + "' is not supported for reprocessing.");
        }

        long startTs = task.getStartTs();
        long endTs = task.getEndTs();

        CalculatedFieldCtx ctx = getCFCtx(task.getCalculatedField());
        if (OutputType.ATTRIBUTES.equals(ctx.getOutput().getType())) {
            throw new IllegalArgumentException("'ATTRIBUTES' output type is not supported for reprocessing");
        }
        CalculatedFieldState state = getOrInitState(tenantId, entityId, ctx, startTs);

        performInitialProcessing(tenantId, entityId, state, ctx, startTs);

        Map<String, Argument> arguments = ctx.getArguments();
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

            processArgumentValuesUpdate(tenantId, entityId, state, ctx, updatedArgs, minTs);
        }
    }

    private void performInitialProcessing(TenantId tenantId, EntityId entityId, CalculatedFieldState state, CalculatedFieldCtx ctx, long startTs) throws CalculatedFieldException {
        try {
            if (state.isSizeOk()) {
                processStateIfReady(tenantId, entityId, ctx, state, startTs);
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
                        pushMsgToRuleEngine(tenantId, entityId, checkAndSetTs(calculationResult, ts), Collections.emptyList(), TbCallback.EMPTY);
                    }
                }
            }
            if (!stateSizeChecked) {
                state.checkStateSize(ctxId, ctx.getMaxStateSize());
            }
            if (!state.isSizeOk()) {
                throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build();
            }
        } catch (Exception e) {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).msgId(null).msgType(null).arguments(state.getArguments()).cause(e).build();
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

    private void processArgumentValuesUpdate(TenantId tenantId, EntityId entityId, CalculatedFieldState state, CalculatedFieldCtx ctx, Map<String, ArgumentEntry> newArgValues, long ts) throws CalculatedFieldException {
        if (newArgValues.isEmpty()) {
            log.info("[{}] No argument values to process for CF.", ctx.getCfId());
        }
        if (state == null) {
            state = createStateByType(ctx);
        }
        if (state.isSizeOk()) {
            if (state.updateState(ctx, newArgValues)) {
                processStateIfReady(tenantId, entityId, ctx, state, ts);
            }
        } else {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build();
        }
    }

    @SneakyThrows
    private CalculatedFieldState getOrInitState(TenantId tenantId, EntityId entityId, CalculatedFieldCtx ctx, long startTs) {
        ListenableFuture<CalculatedFieldState> stateFuture = fetchStateFromDb(ctx, entityId, startTs);
        // Ugly but necessary. We do not expect to often fetch data from DB. Only once per <Entity, CalculatedField> pair lifetime.
        // This call happens while processing the CF pack from the queue consumer. So the timeout should be relatively low.
        // Alternatively, we can fetch the state outside the actor system and push separate command to create this actor,
        // but this will significantly complicate the code.
        CalculatedFieldState state = stateFuture.get(1, TimeUnit.MINUTES);
        state.checkStateSize(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId), ctx.getMaxStateSize());
        return state;
    }

    private ListenableFuture<CalculatedFieldState> fetchStateFromDb(CalculatedFieldCtx ctx, EntityId entityId, long startTs) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        for (var entry : ctx.getArguments().entrySet()) {
            var argEntityId = entry.getValue().getRefEntityId() != null ? entry.getValue().getRefEntityId() : entityId;
            var argValueFuture = fetchKvEntryForReprocessing(ctx.getTenantId(), argEntityId, entry.getValue(), startTs);
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

    private ListenableFuture<ArgumentEntry> fetchKvEntryForReprocessing(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        return switch (argument.getRefEntityKey().getType()) {
            case TS_ROLLING -> fetchTsRollingForReprocessing(tenantId, entityId, argument, startTs);
            case ATTRIBUTE -> fetchAttributeForReprocessing(tenantId, entityId, argument, startTs);
            case TS_LATEST -> fetchTsLatestForReprocessing(tenantId, entityId, argument, startTs);
        };
    }

    private ListenableFuture<ArgumentEntry> fetchAttributeForReprocessing(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        var attributeOptFuture = attributesService.find(tenantId, entityId, argument.getRefEntityKey().getScope(), argument.getRefEntityKey().getKey());

        ListenableFuture<Optional<? extends KvEntry>> attribute = Futures.transform(attributeOptFuture,
                attrOpt -> attrOpt.or(() -> Optional.of(new BaseAttributeKvEntry(createDefaultKvEntry(argument), startTs, 0L))),
                calculatedFieldCallbackExecutor);

        return transformSingleValueArgument(attribute, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchTsLatestForReprocessing(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), 0, startTs, 0, 1, Aggregation.NONE);
        ListenableFuture<List<TsKvEntry>> tsKvListFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        ListenableFuture<Optional<? extends KvEntry>> tsLatest = Futures.transform(tsKvListFuture, tsKvList -> {
            if (tsKvList.isEmpty() || tsKvList.get(0) == null || tsKvList.get(0).getValue() == null) {
                return Optional.of(new BasicTsKvEntry(startTs, createDefaultKvEntry(argument), 0L));
            }
            return Optional.of(tsKvList.get(0));
        }, calculatedFieldCallbackExecutor);

        return transformSingleValueArgument(tsLatest, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchTsRollingForReprocessing(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        long argTimeWindow = argument.getTimeWindow() == 0 ? startTs : argument.getTimeWindow();
        long startInterval = startTs - argTimeWindow;
        long maxDataPoints = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg);
        int argumentLimit = argument.getLimit();
        int limit = argumentLimit == 0 || argumentLimit > maxDataPoints ? (int) maxDataPoints : argument.getLimit();

        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startInterval, startTs, 0, limit, Aggregation.NONE);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsRollingFuture, tsRolling -> tsRolling == null ? new TsRollingArgumentEntry(limit, argTimeWindow) : ArgumentEntry.createTsRollingArgument(tsRolling, limit, argTimeWindow), calculatedFieldCallbackExecutor);
    }

    private List<TsKvEntry> fetchTelemetryBatch(TenantId tenantId, EntityId entityId, Argument argument, long startTs, long endTs, int limit) {
        EntityId sourceEntityId = argument.getRefEntityId() != null ? argument.getRefEntityId() : entityId;
        try {
            ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startTs, endTs, 0, limit, Aggregation.NONE, "ASC");
            return timeseriesService.findAll(tenantId, sourceEntityId, List.of(query)).get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.debug("Failed to fetch telemetry for [{}:{}]", sourceEntityId, argument.getRefEntityKey().getKey(), e);
            return Collections.emptyList();
        }
    }

    private CalculatedFieldCtx getCFCtx(CalculatedField calculatedField) {
        try {
            CalculatedFieldCtx ctx = new CalculatedFieldCtx(calculatedField, tbelInvokeService, apiLimitService);
            ctx.init();
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, CalculatedFieldResult calculatedFieldResult, List<CalculatedFieldId> cfIds, TbCallback callback) {
        try {
            OutputType type = calculatedFieldResult.getType();
            TbMsgType msgType = OutputType.ATTRIBUTES.equals(type) ? TbMsgType.POST_ATTRIBUTES_REQUEST : TbMsgType.POST_TELEMETRY_REQUEST;
            TbMsgMetaData md = OutputType.ATTRIBUTES.equals(type) ? new TbMsgMetaData(Map.of(SCOPE, calculatedFieldResult.getScope().name())) : TbMsgMetaData.EMPTY;
            TbMsg msg = TbMsg.newMsg().type(msgType).originator(entityId).previousCalculatedFieldIds(cfIds).metaData(md).data(JacksonUtil.writeValueAsString(calculatedFieldResult.getResult())).build();
            tbClusterService.pushMsgToRuleEngine(tenantId, entityId, msg, new TbQueueCallback() {
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

}
