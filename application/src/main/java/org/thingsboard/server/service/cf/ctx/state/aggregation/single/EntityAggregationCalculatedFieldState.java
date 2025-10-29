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
package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.HashMap;
import java.util.Map;

public class EntityAggregationCalculatedFieldState extends BaseCalculatedFieldState {

    private AggInterval interval;
    private long intervalDuration;
    private long watermarkDuration;
    private long checkInterval;

    private Map<String, AggMetric> metrics;

    private CalculatedFieldProcessingService cfProcessingService;

    public EntityAggregationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        super.setCtx(ctx, actorCtx);
        this.cfProcessingService = ctx.getCfProcessingService();
        var configuration = (EntityAggregationCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        intervalDuration = configuration.getInterval().getIntervalDurationMillis();
        watermarkDuration = configuration.getWatermark().getDuration();
        checkInterval = configuration.getWatermark().getCheckInterval();
        interval = configuration.getInterval();
        metrics = configuration.getMetrics();
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ENTITY_AGGREGATION;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) throws Exception {
        long now = System.currentTimeMillis();
        AggIntervalEntry aggIntervalEntry = new AggIntervalEntry(interval.getCurrentIntervalStartTs(), interval.getCurrentIntervalEndTs());
        boolean exists = false;
        for (Map.Entry<String, ArgumentEntry> entry : arguments.entrySet()) {
            ArgumentEntry argumentEntry = entry.getValue();
            EntityAggregationArgumentEntry entityAggEntry = (EntityAggregationArgumentEntry) argumentEntry;
            Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals = entityAggEntry.getAggIntervals();
            exists |= aggIntervals.containsKey(aggIntervalEntry);
        }
        if (!exists) {
            arguments.forEach((argName, argumentEntry) -> {
                EntityAggregationArgumentEntry entityAggEntry = (EntityAggregationArgumentEntry) argumentEntry;
                entityAggEntry.getAggIntervals().put(aggIntervalEntry, new AggIntervalEntryStatus());
            });
            ctx.scheduleReevaluation(interval.getDelayUntilIntervalEnd(), actorCtx);
        }

        Map<AggIntervalEntry, Map<String, ArgumentEntry>> results = new HashMap<>();
        for (Map.Entry<String, ArgumentEntry> entry : arguments.entrySet()) {
            String argName = entry.getKey();
            ArgumentEntry argumentEntry = entry.getValue();

            EntityAggregationArgumentEntry entityAggEntry = (EntityAggregationArgumentEntry) argumentEntry;
            Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals = entityAggEntry.getAggIntervals();
            for (Map.Entry<AggIntervalEntry, AggIntervalEntryStatus> aggInterval : aggIntervals.entrySet()) {
                AggIntervalEntry intervalEntry = aggInterval.getKey();
                AggIntervalEntryStatus entryStatus = aggInterval.getValue();

                Long startTs = intervalEntry.getStartTs();
                Long endTs = intervalEntry.getEndTs();
                if (now - endTs > watermarkDuration) {
                    if (entryStatus.getLastArgsRefreshTs() > entryStatus.getLastMetricsEvalTs()) {
                        String metricName = null;
                        for (Map.Entry<String, AggMetric> metricEntry : metrics.entrySet()) {
                            if (((AggKeyInput) metricEntry.getValue().getInput()).getKey().equals(argName)) {
                                metricName = metricEntry.getKey();
                            }
                        }
                        ArgumentEntry metric = cfProcessingService.fetchMetricDuringInterval(entityId, intervalEntry, metricName, ctx);
                        if (!metric.isEmpty()) {
                            results.computeIfAbsent(intervalEntry, i -> new HashMap<>()).put(argName, metric);
                        }
                    }
                    aggIntervals.remove(intervalEntry);
                    continue;
                } else if (now - startTs >= intervalDuration) {
                    if (entryStatus.shouldRecalculate(checkInterval)) {
                        String metricName = null;
                        for (Map.Entry<String, AggMetric> metricEntry : metrics.entrySet()) {
                            if (((AggKeyInput) metricEntry.getValue().getInput()).getKey().equals(argName)) {
                                metricName = metricEntry.getKey();
                            }
                        }
                        ArgumentEntry metric = cfProcessingService.fetchMetricDuringInterval(entityId, intervalEntry, metricName, ctx);
                        if (!metric.isEmpty()) {
                            results.computeIfAbsent(intervalEntry, i -> new HashMap<>()).put(argName, metric);
                        }
                    }
                }
            }
        }
        ArrayNode result = toResult(results);
        if (result.isEmpty()) {
            return Futures.immediateFuture(TelemetryCalculatedFieldResult.EMPTY);
        }
        Output output = ctx.getOutput();
        return Futures.immediateFuture(TelemetryCalculatedFieldResult.builder()
                .type(output.getType())
                .scope(output.getScope())
                .result(result)
                .build());

//        long now = System.currentTimeMillis();
//        AggIntervalEntry aggIntervalEntry = new AggIntervalEntry(interval.getCurrentIntervalStartTs(), interval.getCurrentIntervalEndTs(), false);
//        if (!intervals.containsKey(aggIntervalEntry)) {
//            intervals.put(aggIntervalEntry, new AggIntervalEntryStatus());
//            ctx.scheduleReevaluation(interval.getDelayUntilIntervalEnd(), actorCtx);
//        }
//        ArrayNode results = JacksonUtil.newArrayNode();
//        for (Map.Entry<AggIntervalEntry, AggIntervalEntryStatus> entry : intervals.entrySet()) {
//            AggIntervalEntry intervalEntry = entry.getKey();
//            AggIntervalEntryStatus entryStatus = entry.getValue();
//
//            Long startTs = intervalEntry.getStartTs();
//            Long endTs = intervalEntry.getEndTs();
//            if (now - endTs > watermarkDuration) {
//                if (entryStatus.getLastArgsRefreshTs() > entryStatus.getLastMetricsEvalTs()) {
//                    ArgumentEntry metric = cfProcessingService.fetchMetricDuringInterval(entityId, intervalEntry, metricName, ctx);
//                    ObjectNode result = fetchMetrics(intervalEntry);
//                    if (result != null) {
//                        results.add(result);
//                    }
//                }
//                intervals.remove(intervalEntry);
//                continue;
//            } else if (now - startTs >= intervalDuration) {
//                if (entryStatus.shouldRecalculate(checkInterval)) {
//                    ObjectNode result = fetchMetrics(intervalEntry);
//                    if (result != null) {
//                        results.add(result);
//                    }
//                }
//            }
//        }
//        if (results.isEmpty()) {
//            return Futures.immediateFuture(TelemetryCalculatedFieldResult.EMPTY);
//        }
//        Output output = ctx.getOutput();
//        return Futures.immediateFuture(TelemetryCalculatedFieldResult.builder()
//                .type(output.getType())
//                .scope(output.getScope())
//                .result(results)
//                .build());
    }

    protected ArrayNode toResult(Map<AggIntervalEntry, Map<String, ArgumentEntry>> results) {
        ArrayNode result = JacksonUtil.newArrayNode();
        results.forEach((interval, args) -> {
            ObjectNode metricsNode = JacksonUtil.newObjectNode();
            for (Map.Entry<String, ArgumentEntry> entry : args.entrySet()) {
                String metricName = entry.getKey();
                ArgumentEntry argumentEntry = entry.getValue();
                if (!argumentEntry.isEmpty()) {
                    metricsNode.put(metricName, JacksonUtil.toString(argumentEntry.getValue()));
                }
            }
            ObjectNode resultNode = JacksonUtil.newObjectNode();
            if (!metricsNode.isEmpty()) {
                resultNode.put("ts", interval.getEndTs());
                resultNode.set("values", metricsNode);
            }
            result.add(resultNode);
        });
        return result;
    }

    @Override
    public boolean isReady() {
        return true;
    }

}
