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
package org.thingsboard.server.service.cf.ctx.state.aggregation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.aggregation.function.AggEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
@Getter
public class RelatedEntitiesAggregationCalculatedFieldState extends BaseCalculatedFieldState {

    @Setter
    private long lastArgsRefreshTs = -1;
    @Setter
    private long lastMetricsEvalTs = -1;
    private long deduplicationInterval = -1;
    private Map<String, AggMetric> metrics;

    public RelatedEntitiesAggregationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        super.setCtx(ctx, actorCtx);
        var configuration = (RelatedEntitiesAggregationCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        metrics = configuration.getMetrics();
        deduplicationInterval = configuration.getDeduplicationIntervalInSec();
    }

    @Override
    public void reset() { // must reset everything dependent on arguments
        super.reset();
        lastArgsRefreshTs = -1;
        lastMetricsEvalTs = -1;
        metrics = null;
    }

    @Override
    public void init() {
        super.init();
        ctx.scheduleReevaluation(deduplicationInterval, actorCtx);
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

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) throws Exception {
        boolean cfUpdated = updatedArgs != null && updatedArgs.isEmpty();
        if (shouldRecalculate() || cfUpdated) {
            Output output = ctx.getOutput();
            ObjectNode aggResult = aggregateMetrics(output);
            lastMetricsEvalTs = System.currentTimeMillis();
            ctx.scheduleReevaluation(deduplicationInterval, actorCtx);
            return Futures.immediateFuture(TelemetryCalculatedFieldResult.builder()
                    .type(output.getType())
                    .scope(output.getScope())
                    .result(toSimpleResult(ctx.isUseLatestTs(), aggResult))
                    .build());
        } else {
            return Futures.immediateFuture(TelemetryCalculatedFieldResult.builder()
                    .result(null)
                    .build());
        }
    }

    public Map<String, ArgumentEntry> updateEntityData(Map<String, ArgumentEntry> fetchedArgs) {
        lastMetricsEvalTs = -1;
        return update(fetchedArgs, ctx);
    }

    public void cleanupEntityData(EntityId relatedEntityId) {
        arguments.values().forEach(argEntry -> {
            RelatedEntitiesArgumentEntry aggEntry = (RelatedEntitiesArgumentEntry) argEntry;
            aggEntry.getAggInputs().remove(relatedEntityId);
        });
        lastMetricsEvalTs = -1;
        lastArgsRefreshTs = System.currentTimeMillis();
    }

    private boolean shouldRecalculate() {
        boolean intervalPassed = lastMetricsEvalTs <= System.currentTimeMillis() - deduplicationInterval;
        boolean argsUpdatedDuringInterval = lastArgsRefreshTs > lastMetricsEvalTs;
        return intervalPassed && argsUpdatedDuringInterval;
    }

    private Map<EntityId, Map<String, ArgumentEntry>> prepareInputs() {
        Map<EntityId, Map<String, ArgumentEntry>> inputs = new HashMap<>();
        for (Map.Entry<String, ArgumentEntry> argEntry : arguments.entrySet()) {
            String key = argEntry.getKey();
            RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry = (RelatedEntitiesArgumentEntry) argEntry.getValue();
            relatedEntitiesArgumentEntry.getAggInputs().forEach((entityId, argumentEntry) -> {
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

}
