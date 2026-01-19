/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.ctx.state.aggregation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.aggregation.function.AggEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.ScheduledRefreshSupported;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx.DISABLED_INTERVAL_VALUE;
import static org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState.ReadinessStatus.MISSING_AGGREGATION_ENTITIES_ERROR;

@Slf4j
public class RelatedEntitiesAggregationCalculatedFieldState extends BaseCalculatedFieldState implements ScheduledRefreshSupported {

    @Setter
    @Getter
    private long lastArgsRefreshTs = DEFAULT_LAST_UPDATE_TS;
    @Setter
    @Getter
    private long lastMetricsEvalTs = DEFAULT_LAST_UPDATE_TS;
    private long lastRelatedEntitiesRefreshTs = DEFAULT_LAST_UPDATE_TS;
    private long deduplicationIntervalMs = DISABLED_INTERVAL_VALUE;
    private Map<String, AggMetric> metrics;

    private ScheduledFuture<?> reevaluationFuture;

    private EntityService entityService;

    public RelatedEntitiesAggregationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        super.setCtx(ctx, actorCtx);
        var configuration = (RelatedEntitiesAggregationCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        metrics = configuration.getMetrics();
        deduplicationIntervalMs = SECONDS.toMillis(configuration.getDeduplicationIntervalInSec());
        entityService = ctx.getSystemContext().getEntityService();
    }

    @Override
    public void init(boolean restored) {
        super.init(restored);
        if (restored) {
            scheduleReevaluation();
        }
    }

    @Override
    public void close() {
        super.close();
        if (reevaluationFuture != null) {
            reevaluationFuture.cancel(true);
            reevaluationFuture = null;
        }
    }

    @Override
    public void reset() { // must reset everything dependent on arguments
        super.reset();
        resetScheduledRefreshTs();
        lastArgsRefreshTs = DEFAULT_LAST_UPDATE_TS;
        lastMetricsEvalTs = DEFAULT_LAST_UPDATE_TS;
        metrics = null;
    }

    @Override
    public void resetScheduledRefreshTs() {
        lastRelatedEntitiesRefreshTs = DEFAULT_LAST_UPDATE_TS;
    }

    @Override
    public long getLastScheduledRefreshTs() {
        return lastRelatedEntitiesRefreshTs;
    }

    @Override
    public void updateScheduledRefreshTs() {
        lastRelatedEntitiesRefreshTs = System.currentTimeMillis();
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.RELATED_ENTITIES_AGGREGATION;
    }

    @Override
    public Map<String, ArgumentEntry> update(Map<String, ArgumentEntry> argumentValues, CalculatedFieldCtx ctx) {
        lastArgsRefreshTs = System.currentTimeMillis();
        return super.update(argumentValues, ctx);
    }

    public List<EntityId> checkRelatedEntities(List<EntityId> relatedEntities) {
        Map<EntityId, Map<String, ArgumentEntry>> entityInputs = prepareInputs();
        findOutdatedEntities(entityInputs, relatedEntities).forEach(this::cleanupEntityData);
        updateScheduledRefreshTs();
        return findMissingEntities(entityInputs, relatedEntities);
    }

    private List<EntityId> findMissingEntities(Map<EntityId, Map<String, ArgumentEntry>> entityInputs, List<EntityId> relatedEntities) {
        List<EntityId> missing = new ArrayList<>();
        relatedEntities.forEach(entityId -> {
            if (!entityInputs.containsKey(entityId)) {
                missing.add(entityId);
                log.warn("[{}] Missing related entity inputs for {}", ctx.getCfId(), entityId);
            }
        });
        return missing;
    }

    private List<EntityId> findOutdatedEntities(Map<EntityId, Map<String, ArgumentEntry>> entityInputs, List<EntityId> relatedEntities) {
        List<EntityId> outdated = new ArrayList<>();
        entityInputs.keySet().forEach(entityId -> {
            if (!relatedEntities.contains(entityId)) {
                outdated.add(entityId);
                log.warn("[{}] CF state keeps outdated related entity {}", ctx.getCfId(), entityId);
            }
        });
        return outdated;
    }

    public Map<String, ArgumentEntry> updateEntityData(Map<String, ArgumentEntry> fetchedArgs) {
        lastMetricsEvalTs = DEFAULT_LAST_UPDATE_TS;
        return update(fetchedArgs, ctx);
    }

    public void cleanupEntityData(EntityId relatedEntityId) {
        arguments.values().forEach(argEntry -> {
            RelatedEntitiesArgumentEntry aggEntry = (RelatedEntitiesArgumentEntry) argEntry;
            aggEntry.getEntityInputs().remove(relatedEntityId);
        });
        lastMetricsEvalTs = DEFAULT_LAST_UPDATE_TS;
        lastArgsRefreshTs = System.currentTimeMillis();
        readinessStatus = checkReadiness();
    }

    public void scheduleReevaluation() {
        ScheduledFuture<?> future = ctx.scheduleReevaluation(getEnforcedDeduplicationIntervalMillis(), actorCtx);
        if (future != null) {
            reevaluationFuture = future;
        }
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) throws Exception {
        boolean cfUpdated = updatedArgs != null && updatedArgs.isEmpty();
        if (shouldRecalculate() || cfUpdated) {
            Output output = ctx.getOutput();
            ObjectNode aggResult = aggregateMetrics(output);
            lastMetricsEvalTs = System.currentTimeMillis();
            scheduleReevaluation();
            return Futures.immediateFuture(TelemetryCalculatedFieldResult.builder()
                    .outputStrategy(output.getStrategy())
                    .type(output.getType())
                    .scope(output.getScope())
                    .result(toSimpleResult(ctx.isUseLatestTs(), aggResult))
                    .build());
        } else {
            return Futures.immediateFuture(TelemetryCalculatedFieldResult.EMPTY);
        }
    }

    private boolean shouldRecalculate() {
        boolean intervalPassed = lastMetricsEvalTs <= System.currentTimeMillis() - getEnforcedDeduplicationIntervalMillis();
        boolean argsUpdatedDuringInterval = lastArgsRefreshTs > lastMetricsEvalTs;
        return intervalPassed && argsUpdatedDuringInterval;
    }

    private long getEnforcedDeduplicationIntervalMillis() {
        return Math.max(deduplicationIntervalMs, ctx.getMinDeduplicationIntervalMillis());
    }

    private Map<EntityId, Map<String, ArgumentEntry>> prepareInputs() {
        Map<EntityId, Map<String, ArgumentEntry>> inputs = new HashMap<>();
        for (Map.Entry<String, ArgumentEntry> argEntry : arguments.entrySet()) {
            String key = argEntry.getKey();
            RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry = (RelatedEntitiesArgumentEntry) argEntry.getValue();
            relatedEntitiesArgumentEntry.getEntityInputs().forEach((entityId, argumentEntry) -> {
                inputs.computeIfAbsent(entityId, k -> new HashMap<>()).put(key, argumentEntry);
            });
        }
        return inputs;
    }

    private ObjectNode aggregateMetrics(Output output) throws Exception {
        ObjectNode aggResult = JacksonUtil.newObjectNode();
        Map<EntityId, Map<String, ArgumentEntry>> inputs = prepareInputs();
        for (Entry<String, AggMetric> entry : metrics.entrySet()) {
            String metricKey = entry.getKey();
            AggMetric metric = entry.getValue();

            AggEntry aggMetricEntry = AggEntry.createAggFunction(metric.getFunction());
            aggregateMetric(metric, aggMetricEntry, inputs);
            aggMetricEntry.result(output.getDecimalsByDefault()).ifPresent(result -> {
                aggResult.set(metricKey, JacksonUtil.valueToTree(result));
            });
        }
        return aggResult;
    }

    private void aggregateMetric(AggMetric metric, AggEntry aggEntry, Map<EntityId, Map<String, ArgumentEntry>> inputs) throws Exception {
        for (Map<String, ArgumentEntry> entityInputs : inputs.values()) {
            if (applyAggregation(metric.getFilter(), entityInputs)) {
                Object arg = resolveAggregationInput(metric.getInput(), entityInputs);
                if (arg != null) {
                    aggEntry.update(arg);
                }
            }
        }
    }

    private boolean applyAggregation(String filter, Map<String, ArgumentEntry> entityInputs) throws Exception {
        if (filter == null || filter.isEmpty()) {
            return true;
        } else {
            Object filterResult = ctx.evaluateTbelExpression(filter, entityInputs, getLatestTimestamp()).get();
            return filterResult instanceof Boolean booleanResult && booleanResult;
        }
    }

    private Object resolveAggregationInput(AggInput aggInput, Map<String, ArgumentEntry> entityInputs) throws Exception {
        if (aggInput instanceof AggFunctionInput functionInput) {
            return ctx.evaluateTbelExpression(functionInput.getFunction(), entityInputs, getLatestTimestamp()).get();
        } else {
            String inputKey = ((AggKeyInput) aggInput).getKey();
            return entityInputs.get(inputKey).getValue();
        }
    }

    @Override
    public JsonNode getArgumentsJson() {
        Map<EntityId, Map<String, ArgumentEntry>> inputs = prepareInputs();
        Map<EntityId, EntityInfo> entityIdEntityInfos = entityService.fetchEntityInfos(ctx.getTenantId(), null, inputs.keySet());
        List<EntityArgument> entitiesArguments = new ArrayList<>();
        inputs.forEach((entityId, entityArguments) -> {
            EntityInfo entityInfo = entityIdEntityInfos.get(entityId);
            if (entityInfo != null) {
                JsonNode entityArgumentsJson = JacksonUtil.valueToTree(entityArguments.entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().jsonValue())));
                entitiesArguments.add(new EntityArgument(entityInfo, entityArgumentsJson));
            }
        });
        return JacksonUtil.valueToTree(new RelatedEntitiesArgument(ArgumentEntryType.RELATED_ENTITIES, entitiesArguments));
    }

    record RelatedEntitiesArgument(ArgumentEntryType type, List<EntityArgument> entitiesArguments) {}

    record EntityArgument(EntityInfo entity, JsonNode entityArguments) {}

    @Override
    protected ReadinessStatus checkReadiness() {
        if (arguments == null) {
            return ReadinessStatus.notReady(MISSING_AGGREGATION_ENTITIES_ERROR);
        }
        for (String requiredArgumentKey : requiredArguments) {
            ArgumentEntry argumentEntry = arguments.get(requiredArgumentKey);
            if (argumentEntry == null || argumentEntry.isEmpty()) {
                return ReadinessStatus.notReady(MISSING_AGGREGATION_ENTITIES_ERROR);
            }
            if (argumentEntry instanceof RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry) {
                try {
                    checkConstraintByDirection(relatedEntitiesArgumentEntry);
                } catch (Exception e) {
                    return ReadinessStatus.notReady(e.getMessage());
                }
            }
        }
        return ReadinessStatus.READY;
    }

    public void checkConstraintByDirection(RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry) {
        if (ctx.getCalculatedField().getConfiguration() instanceof RelatedEntitiesAggregationCalculatedFieldConfiguration config) {
            if (EntitySearchDirection.TO == config.getRelation().direction()) {
                if (relatedEntitiesArgumentEntry.getEntityInputs().size() > 1) {
                    throw new IllegalArgumentException("More than one related entity is not supported for relation direction 'TO'. Found: " + relatedEntitiesArgumentEntry.getEntityInputs().size() + ".");
                }
            }
        }
    }

}
