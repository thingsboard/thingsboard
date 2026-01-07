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
package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbUtils;
import org.thingsboard.script.api.tbel.TbelCfArg;
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
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultMetricArgumentEntry;

public class EntityAggregationCalculatedFieldState extends BaseCalculatedFieldState {

    private AggInterval interval;
    private long watermarkDuration;
    private Map<String, AggMetric> metrics;

    private boolean produceIntermediateResult;

    private EntityAggregationDebugArgumentsTracker debugTracker;

    private CalculatedFieldProcessingService cfProcessingService;

    public EntityAggregationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        super.setCtx(ctx, actorCtx);
        this.cfProcessingService = ctx.getCfProcessingService();
        var configuration = (EntityAggregationCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        Watermark watermark = configuration.getWatermark();
        watermarkDuration = watermark == null ? 0 : TimeUnit.SECONDS.toMillis(watermark.getDuration());
        interval = configuration.getInterval();
        metrics = configuration.getMetrics();
        produceIntermediateResult = configuration.isProduceIntermediateResult();
    }

    @Override
    public void init(boolean restored) {
        super.init(restored);
        if (restored) {
            fillMissingIntervals();
        }
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ENTITY_AGGREGATION;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) throws Exception {
        createIntervalIfNotExist();
        long now = System.currentTimeMillis();

        if (DebugModeUtil.isDebugFailuresAvailable(ctx.getCalculatedField())) {
            if (debugTracker == null) {
                debugTracker = new EntityAggregationDebugArgumentsTracker(new HashMap<>());
            } else {
                debugTracker.reset();
            }
            debugTracker.recordUpdatedArgs(updatedArgs, arguments);
        }

        Map<AggIntervalEntry, Map<String, ArgumentEntry>> results = new HashMap<>();
        List<AggIntervalEntry> expiredIntervals = new ArrayList<>();
        getIntervals().forEach((intervalEntry, argIntervalStatuses) -> {
            processInterval(now, intervalEntry, argIntervalStatuses, expiredIntervals, results);
        });
        removeExpiredIntervals(expiredIntervals);

        Output output = ctx.getOutput();
        ArrayNode result = toResult(results, output.getDecimalsByDefault());
        if (result.isEmpty()) {
            return Futures.immediateFuture(TelemetryCalculatedFieldResult.EMPTY);
        }
        return Futures.immediateFuture(TelemetryCalculatedFieldResult.builder()
                .outputStrategy(output.getStrategy())
                .type(output.getType())
                .scope(output.getScope())
                .result(result)
                .build());
    }

    @Override
    public Map<String, ArgumentEntry> update(Map<String, ArgumentEntry> argumentValues, CalculatedFieldCtx ctx) {
        createIntervalIfNotExist();
        return super.update(argumentValues, ctx);
    }

    private void removeExpiredIntervals(List<AggIntervalEntry> expiredIntervals) {
        expiredIntervals.forEach(expiredInterval -> {
            arguments.values().stream()
                    .map(EntityAggregationArgumentEntry.class::cast)
                    .forEach(arg -> arg.getAggIntervals().remove(expiredInterval));
        });
    }

    private void createIntervalIfNotExist() {
        AggIntervalEntry currentInterval = new AggIntervalEntry(interval.getCurrentIntervalStartTs(), interval.getCurrentIntervalEndTs());
        arguments.forEach((argName, argumentEntry) -> {
            var entityAggEntry = (EntityAggregationArgumentEntry) argumentEntry;
            entityAggEntry.getAggIntervals().computeIfAbsent(currentInterval, current -> new AggIntervalEntryStatus());
        });
    }

    private void fillMissingIntervals() {
        long now = System.currentTimeMillis();
        ZoneId zoneId = interval.getZoneId();
        long currentIntervalEndTs = interval.getCurrentIntervalEndTs();
        long watermarkThresholdTs = now - watermarkDuration;

        Map<AggIntervalEntry, Map<String, AggIntervalEntryStatus>> intervals = getIntervals();
        AggIntervalEntry lastIntervalEntry = intervals.keySet().stream().max(Comparator.comparing(AggIntervalEntry::getEndTs)).orElse(null);
        if (lastIntervalEntry == null) {
            return;
        }

        ZonedDateTime nextStart = Instant.ofEpochMilli(lastIntervalEntry.getEndTs()).atZone(zoneId);
        ZonedDateTime nextEnd = interval.getNextIntervalStart(nextStart);

        while (nextEnd.toInstant().toEpochMilli() <= currentIntervalEndTs) {
            long nextStartTs = nextStart.toInstant().toEpochMilli();
            long nextEndTs = nextEnd.toInstant().toEpochMilli();

            if (nextEndTs < watermarkThresholdTs) {
                nextStart = nextEnd;
                nextEnd = interval.getNextIntervalStart(nextStart);
                continue;
            }

            AggIntervalEntry missing = new AggIntervalEntry(nextStartTs, nextEndTs);

            arguments.forEach((argName, argumentEntry) -> {
                var entityAggEntry = (EntityAggregationArgumentEntry) argumentEntry;
                AggIntervalEntryStatus intervalEntryStatus = new AggIntervalEntryStatus(System.currentTimeMillis());
                entityAggEntry.getAggIntervals().computeIfAbsent(missing, missingInterval -> intervalEntryStatus);
            });

            nextStart = nextEnd;
            nextEnd = interval.getNextIntervalStart(nextStart);
        }
    }

    private Map<AggIntervalEntry, Map<String, AggIntervalEntryStatus>> getIntervals() {
        Map<AggIntervalEntry, Map<String, AggIntervalEntryStatus>> intervals = new HashMap<>();
        arguments.forEach((argName, entry) -> {
            var argEntry = (EntityAggregationArgumentEntry) entry;
            argEntry.getAggIntervals().forEach((intervalEntry, status) ->
                    intervals.computeIfAbsent(intervalEntry, i -> new HashMap<>()).put(argName, status)
            );
        });
        return intervals;
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
        } else if (now - startTs >= intervalEntry.getIntervalDuration()) {
            handleActiveInterval(ctx.getCfCheckReevaluationIntervalMillis(), intervalEntry, args, results);
            if (watermarkDuration == 0) {
                expiredIntervals.add(intervalEntry);
            }
        } else if (produceIntermediateResult) {
            handleActiveInterval(ctx.getIntermediateAggregationIntervalMillis(), intervalEntry, args, results);
        }
    }

    private void handleExpiredInterval(AggIntervalEntry intervalEntry,
                                       Map<String, AggIntervalEntryStatus> args,
                                       Map<AggIntervalEntry, Map<String, ArgumentEntry>> results) {
        args.forEach((argName, argEntryIntervalStatus) -> {
            if (argEntryIntervalStatus.getLastArgsRefreshTs() > argEntryIntervalStatus.getLastMetricsEvalTs()) {
                argEntryIntervalStatus.setLastMetricsEvalTs(System.currentTimeMillis());
                processArgument(intervalEntry, argName, false, results);
            } else if (argEntryIntervalStatus.getLastMetricsEvalTs() == DEFAULT_LAST_UPDATE_TS) {
                argEntryIntervalStatus.setLastMetricsEvalTs(System.currentTimeMillis());
                processArgument(intervalEntry, argName, true, results);
            }
        });
    }

    private void handleActiveInterval(long cfCheckInterval,
                                      AggIntervalEntry intervalEntry,
                                      Map<String, AggIntervalEntryStatus> args,
                                      Map<AggIntervalEntry, Map<String, ArgumentEntry>> results) {
        args.forEach((argName, argEntryIntervalStatus) -> {
            if (argEntryIntervalStatus.intervalPassed(cfCheckInterval)) {
                if (argEntryIntervalStatus.argsUpdated()) {
                    argEntryIntervalStatus.setLastMetricsEvalTs(System.currentTimeMillis());
                    argEntryIntervalStatus.setLastArgsRefreshTs(DEFAULT_LAST_UPDATE_TS);
                    processArgument(intervalEntry, argName, false, results);
                } else if (argEntryIntervalStatus.getLastMetricsEvalTs() == DEFAULT_LAST_UPDATE_TS) {
                    argEntryIntervalStatus.setLastMetricsEvalTs(System.currentTimeMillis());
                    processArgument(intervalEntry, argName, true, results);
                }
            }
        });
    }

    private void processArgument(AggIntervalEntry intervalEntry,
                                 String argName,
                                 boolean useDefault,
                                 Map<AggIntervalEntry, Map<String, ArgumentEntry>> results) {
        Set<String> metrics = findMetrics(argName);
        if (!metrics.isEmpty()) {
            metrics.forEach(metricName -> {
                AggMetric metric = this.metrics.get(metricName);
                String argKey = ctx.getArguments().get(argName).getRefEntityKey().getKey();
                ArgumentEntry metricEntry = useDefault
                        ? createDefaultMetricArgumentEntry(argKey, metric)
                        : cfProcessingService.fetchMetricDuringInterval(ctx.getTenantId(), entityId, argKey, metric, intervalEntry);
                if (!metricEntry.isEmpty()) {
                    results.computeIfAbsent(intervalEntry, i -> new HashMap<>()).put(metricName, metricEntry);
                }
            });
        }
    }

    private Set<String> findMetrics(String argName) {
        return metrics.entrySet().stream()
                .filter(e -> ((AggKeyInput) e.getValue().getInput()).getKey().equals(argName))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    protected ArrayNode toResult(Map<AggIntervalEntry, Map<String, ArgumentEntry>> results, Integer precision) {
        ArrayNode result = JacksonUtil.newArrayNode();
        results.forEach((interval, args) -> {
            ObjectNode metricsNode = JacksonUtil.newObjectNode();
            for (Map.Entry<String, ArgumentEntry> entry : args.entrySet()) {
                String metricName = entry.getKey();
                ArgumentEntry argumentEntry = entry.getValue();
                if (!argumentEntry.isEmpty()) {
                    Object resultValue = argumentEntry.getValue() instanceof Number number
                            ? TbUtils.roundResult(number.doubleValue(), precision)
                            : argumentEntry.getValue();
                    metricsNode.put(metricName, JacksonUtil.toString(resultValue));
                }
            }
            if (!metricsNode.isEmpty()) {
                ObjectNode resultNode = JacksonUtil.newObjectNode();
                resultNode.put("ts", interval.getStartTs());
                resultNode.set("values", metricsNode);
                result.add(resultNode);

                if (DebugModeUtil.isDebugFailuresAvailable(ctx.getCalculatedField())) {
                    if (debugTracker != null) {
                        debugTracker.addInterval(interval);
                    }
                }
            }
        });
        return result;
    }

    @Override
    public JsonNode getArgumentsJson() {
        if (debugTracker == null) {
            return null;
        }
        EntityAggregationDebugArguments debugArguments = debugTracker.toDebugArguments();
        return debugArguments == null ? null : JacksonUtil.valueToTree(debugArguments);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    record EntityAggregationDebugArgumentsTracker(Map<AggIntervalEntry, Map<String, TbelCfArg>> processedIntervals) {

        public void reset() {
            processedIntervals.clear();
        }

        public void addInterval(AggIntervalEntry interval) {
            processedIntervals.computeIfAbsent(interval, k -> new HashMap<>());
        }

        public void recordUpdatedArgs(Map<String, ArgumentEntry> updatedArgs, Map<String, ArgumentEntry> arguments) {
            if (updatedArgs != null && !updatedArgs.isEmpty()) {
                updatedArgs.forEach((argName, argEntry) -> {
                    ArgumentEntry argumentEntry = arguments.get(argName);
                    if (argumentEntry instanceof EntityAggregationArgumentEntry entityAggEntry && argEntry instanceof SingleValueArgumentEntry singleEntry) {
                        entityAggEntry.getAggIntervals().forEach((aggIntervalEntry, aggIntervalEntryStatus) -> {
                            boolean match = singleEntry.isForceResetPrevious() || aggIntervalEntry.belongsToInterval(singleEntry.getTs());
                            if (match) {
                                recordArg(aggIntervalEntry, argName, singleEntry.toTbelCfArg());
                            }
                        });
                    }
                });
            }
        }

        public void recordArg(AggIntervalEntry interval, String argName, TbelCfArg value) {
            processedIntervals.computeIfAbsent(interval, k -> new HashMap<>()).put(argName, value);
        }

        public EntityAggregationDebugArguments toDebugArguments() {
            if (processedIntervals.isEmpty()) {
                return null;
            }
            return EntityAggregationDebugArguments.toDebugArguments(processedIntervals);
        }

    }

    record EntityAggregationDebugArguments(List<IntervalDebugArgument> processedIntervals) {

        public static EntityAggregationDebugArguments toDebugArguments(Map<AggIntervalEntry, Map<String, TbelCfArg>> processedIntervals) {
            List<IntervalDebugArgument> result = new ArrayList<>();
            processedIntervals.forEach((interval, args) -> {
                result.add(new IntervalDebugArgument(interval.getStartTs(), interval.getEndTs(), args));
            });
            return new EntityAggregationDebugArguments(result);
        }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IntervalDebugArgument(Long intervalStartTs, Long intervalEndTs, JsonNode updatedArguments) {

        public IntervalDebugArgument(Long intervalStartTs, Long intervalEndTs, Map<String, TbelCfArg> updatedArguments) {
            this(intervalStartTs, intervalEndTs, updatedArguments == null || updatedArguments.isEmpty() ? null : JacksonUtil.valueToTree(updatedArguments));
        }

    }

}
