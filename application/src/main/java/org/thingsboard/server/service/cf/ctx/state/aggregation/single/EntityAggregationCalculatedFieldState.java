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
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultMetricArgumentEntry;

public class EntityAggregationCalculatedFieldState extends BaseCalculatedFieldState {

    private AggInterval interval;
    private long intervalDuration;
    private long watermarkDuration;
    private long checkInterval;
    private Map<String, AggMetric> metrics;

    private final Map<AggIntervalEntry, Map<String, AggIntervalEntryStatus>> intervals = new HashMap<>();

    private CalculatedFieldProcessingService cfProcessingService;

    public EntityAggregationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    public void scheduleReevaluation() {
        prepareIntervals();
        fillMissingIntervals(interval.getCurrentIntervalEndTs(), intervalDuration);
        long now = System.currentTimeMillis();
        intervals.forEach((intervalEntry, argumentIntervalStatuses) -> {
            if (intervalEntry.belongsToInterval(now)) {
                ctx.scheduleReevaluation(interval.getDelayUntilIntervalEnd(), actorCtx);
            } else {
                if (intervalEntry.getEndTs() <= now) {
                    ctx.scheduleReevaluation(checkInterval, actorCtx);
                }
            }
        });
    }

    private void fillMissingIntervals(long currentIntervalEndTs, long intervalDuration) {
        AggIntervalEntry lastIntervalEntry = intervals.keySet().stream().max(Comparator.comparing(AggIntervalEntry::getEndTs)).orElse(null);
        if (lastIntervalEntry == null) {
            return;
        }

        long nextStartTs = lastIntervalEntry.getEndTs();
        long nextEndTs = nextStartTs + intervalDuration;

        while (nextEndTs <= currentIntervalEndTs) {
            AggIntervalEntry missingAggIntervalEntry = new AggIntervalEntry(nextStartTs, nextEndTs);

            arguments.forEach((argName, argumentEntry) -> {
                var entityAggEntry = (EntityAggregationArgumentEntry) argumentEntry;
                AggIntervalEntryStatus intervalEntryStatus = new AggIntervalEntryStatus(System.currentTimeMillis());
                entityAggEntry.getAggIntervals().put(missingAggIntervalEntry, intervalEntryStatus);
                intervals.computeIfAbsent(missingAggIntervalEntry, i -> new HashMap<>()).put(argName, intervalEntryStatus);
            });

            nextStartTs = nextEndTs;
            nextEndTs += intervalDuration;
        }
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        super.setCtx(ctx, actorCtx);
        this.cfProcessingService = ctx.getCfProcessingService();
        var configuration = (EntityAggregationCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        intervalDuration = configuration.getInterval().getIntervalDurationMillis();
        Watermark watermark = configuration.getWatermark();
        watermarkDuration = watermark == null ? 0 : TimeUnit.SECONDS.toMillis(watermark.getDuration());
        checkInterval = watermark == null ? 0 : TimeUnit.SECONDS.toMillis(watermark.getCheckInterval());
        interval = configuration.getInterval();
        metrics = configuration.getMetrics();
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ENTITY_AGGREGATION;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) throws Exception {
        createIntervalIfNotExist();
        prepareIntervals();
        long now = System.currentTimeMillis();

        Map<AggIntervalEntry, Map<String, ArgumentEntry>> results = new HashMap<>();
        List<AggIntervalEntry> expiredIntervals = new ArrayList<>();
        intervals.forEach((intervalEntry, argIntervalStatuses) -> {
            processInterval(now, intervalEntry, argIntervalStatuses, expiredIntervals, results);
        });
        removeExpiredIntervals(expiredIntervals);

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
    }

    private void prepareIntervals() {
        arguments.forEach((argName, entry) -> {
            var argEntry = (EntityAggregationArgumentEntry) entry;
            argEntry.getAggIntervals().forEach((intervalEntry, status) ->
                    intervals.computeIfAbsent(intervalEntry, i -> new HashMap<>()).put(argName, status)
            );
        });
    }

    private void removeExpiredIntervals(List<AggIntervalEntry> expiredIntervals) {
        expiredIntervals.forEach(expiredInterval -> {
            arguments.values().stream()
                    .map(EntityAggregationArgumentEntry.class::cast)
                    .forEach(arg -> arg.getAggIntervals().remove(expiredInterval));
            intervals.remove(expiredInterval);
        });
    }

    private void createIntervalIfNotExist() {
        AggIntervalEntry currentInterval = new AggIntervalEntry(interval.getCurrentIntervalStartTs(), interval.getCurrentIntervalEndTs());
        if (intervals.containsKey(currentInterval)) {
            return;
        }
        arguments.forEach((argName, argumentEntry) -> {
            var entityAggEntry = (EntityAggregationArgumentEntry) argumentEntry;
            if (!entityAggEntry.getAggIntervals().containsKey(currentInterval)) {
                entityAggEntry.getAggIntervals().put(currentInterval, new AggIntervalEntryStatus());
                intervals.computeIfAbsent(currentInterval, i -> new HashMap<>()).put(argName, new AggIntervalEntryStatus());
            }
        });
        ctx.scheduleReevaluation(interval.getDelayUntilIntervalEnd(), actorCtx);
    }

    private void processInterval(long now,
                                 AggIntervalEntry intervalEntry,
                                 Map<String, AggIntervalEntryStatus> args,
                                 List<AggIntervalEntry> expiredIntervals,
                                 Map<AggIntervalEntry, Map<String, ArgumentEntry>> results) {
        long startTs = intervalEntry.getStartTs();
        long endTs = intervalEntry.getEndTs();

        if (now - endTs > watermarkDuration) {
            handleExpiredInterval(intervalEntry, args, results);
            expiredIntervals.add(intervalEntry);
        } else if (now - startTs >= intervalDuration) {
            handleActiveInterval(intervalEntry, args, results);
        }
    }

    private void handleExpiredInterval(AggIntervalEntry intervalEntry,
                                       Map<String, AggIntervalEntryStatus> args,
                                       Map<AggIntervalEntry, Map<String, ArgumentEntry>> results) {
        args.forEach((argName, argEntryIntervalStatus) -> {
            if (argEntryIntervalStatus.getLastArgsRefreshTs() > argEntryIntervalStatus.getLastMetricsEvalTs()) {
                processMetric(intervalEntry, argName, false, results);
            }
        });
    }

    private void handleActiveInterval(AggIntervalEntry intervalEntry,
                                      Map<String, AggIntervalEntryStatus> args,
                                      Map<AggIntervalEntry, Map<String, ArgumentEntry>> results) {
        args.forEach((argName, argEntryIntervalStatus) -> {
            if (argEntryIntervalStatus.shouldRecalculate(checkInterval)) {
                processMetric(intervalEntry, argName, false, results);
                ctx.scheduleReevaluation(checkInterval, actorCtx);
            } else if (argEntryIntervalStatus.intervalPassed(checkInterval)) {
                processMetric(intervalEntry, argName, true, results);
            }
        });
    }

    private void processMetric(AggIntervalEntry intervalEntry,
                               String argName,
                               boolean useDefault,
                               Map<AggIntervalEntry, Map<String, ArgumentEntry>> results) {
        String metricName = findMetricName(argName);
        if (metricName != null) {
            AggMetric metric = metrics.get(metricName);
            String argKey = ctx.getArguments().get(argName).getRefEntityKey().getKey();
            ArgumentEntry metricEntry = useDefault
                    ? createDefaultMetricArgumentEntry(argKey, metric)
                    : cfProcessingService.fetchMetricDuringInterval(ctx.getTenantId(), entityId, argKey, metric, intervalEntry);
            if (!metricEntry.isEmpty()) {
                results.computeIfAbsent(intervalEntry, i -> new HashMap<>()).put(metricName, metricEntry);
            }
        }
    }

    private String findMetricName(String argName) {
        return metrics.entrySet().stream()
                .filter(e -> ((AggKeyInput) e.getValue().getInput()).getKey().equals(argName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
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
