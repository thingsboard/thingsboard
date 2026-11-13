// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.data.util.Pair;
import org.thingsboard.rule.engine.analytics.incoming.state.TbAvgIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbCountIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbCountUniqueIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbMaxIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbMinIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbSumIntervalState;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ashvayka on 07.06.18.
 */
class TbIntervalTable {

    private final TbContext ctx;
    private final Gson gson = new Gson();
    private final AggIntervalType aggIntervalType;
    private final ZoneId tz;
    private final long intervalDuration;
    private final long intervalTtl;
    private final MathFunction function;
    private final boolean autoCreateIntervals;
    private ConcurrentMap<EntityId, ConcurrentMap<Long, TbIntervalState>> states = new ConcurrentHashMap<>();

    TbIntervalTable(TbContext ctx, TbSimpleAggMsgNodeConfiguration config) {
        this.ctx = ctx;
        this.aggIntervalType = config.getAggIntervalType() == null ? AggIntervalType.CUSTOM : config.getAggIntervalType();
        long tmpIntervalDuration;
        if (this.aggIntervalType == AggIntervalType.CUSTOM) {
            this.tz = ZoneId.systemDefault();
            tmpIntervalDuration = TimeUnit.valueOf(config.getAggIntervalTimeUnit()).toMillis(config.getAggIntervalValue());
            this.intervalTtl = TimeUnit.valueOf(config.getAggIntervalTimeUnit()).toMillis(config.getAggIntervalValue() * 2);
        } else {
            this.tz = ZoneId.of(config.getTimeZoneId());
            tmpIntervalDuration = getDefaultIntervalDurationByAggType();
            this.intervalTtl = config.getAggIntervalType().getInterval() * 2;
        }
        this.intervalDuration = Math.max(tmpIntervalDuration, TimeUnit.MINUTES.toMillis(1));
        this.function = MathFunction.valueOf(config.getMathFunction());
        this.autoCreateIntervals = config.isAutoCreateIntervals();
    }

    void addEntities(TbContext ctx, TbMsg msg, List<EntityId> entities) {
        long ts = System.currentTimeMillis();
        entities.forEach(entityId -> {
            try {
                getByEntityIdAndTs(entityId, ts);
            } catch (Exception e) {
                if (msg != null) {
                    ctx.tellFailure(msg, e);
                }
            }
        });
    }

    void cleanupEntities(TbContext ctx) {
        Set<EntityId> keys = new HashSet<>(states.keySet());
        keys.stream().filter(entityId -> !ctx.getPeContext().isLocalEntity(entityId)).forEach(states::remove);
    }

    //TODO: make this async
    Pair<Long, TbIntervalState> getByEntityIdAndTs(EntityId entityId, long ts) throws ExecutionException, InterruptedException {
        long intervalStartTs = calculateIntervalStart(ts);

        Map<Long, TbIntervalState> tsStates = states.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>());
        TbIntervalState state = tsStates.get(intervalStartTs);
        if (state == null) {
            state = fetchIntervalState(entityId, intervalStartTs).get();
            tsStates.put(intervalStartTs, state);
        }
        return Pair.of(intervalStartTs, state);
    }

    ListenableFuture<TimeseriesSaveResult> saveIntervalState(EntityId entityId, long ts, TbIntervalState state) {
        KvEntry kvEntry = new StringDataEntry(DataConstants.RULE_NODE_STATE_PREFIX + ctx.getSelfId(), state.toStateJson(gson));
        TsKvEntry tsKvEntry = new BasicTsKvEntry(calculateIntervalStart(ts), kvEntry);
        return ctx.getTimeseriesService().saveWithoutLatest(ctx.getTenantId(), entityId, List.of(tsKvEntry), 0L);
    }

    Map<EntityId, Map<Long, TbIntervalState>> getStatesToReport(IntervalPersistPolicy intervalPersistPolicy) {
        long ts = System.currentTimeMillis();
        Map<EntityId, Map<Long, TbIntervalState>> updatedStates = new HashMap<>();

        if (autoCreateIntervals) {
            states.forEach((entityId, intervals) -> {
                Optional<Long> maxIntervalTs = intervals.keySet().stream().max(Comparator.comparingLong(Long::valueOf));
                if (maxIntervalTs.isPresent()) {
                    for (long tmpTs = maxIntervalTs.get() + intervalDuration; tmpTs < ts; tmpTs = tmpTs + intervalDuration) {
                        intervals.put(calculateIntervalStart(tmpTs), createDefaultTbIntervalState());
                    }
                } else {
                    intervals.put(calculateIntervalStart(ts), createDefaultTbIntervalState());
                }
            });
        }

        states.forEach((entityId, intervals) -> {
            Stream<Map.Entry<Long, TbIntervalState>> entryStream = intervals.entrySet().stream().filter(e -> e.getValue().hasChangesToReport());
            if (intervalPersistPolicy == IntervalPersistPolicy.ON_EACH_CHECK_AFTER_INTERVAL_END) {
                entryStream = entryStream.filter(e -> (e.getKey() + intervalDuration) < ts);
            }
            Map<Long, TbIntervalState> updatedIntervals = entryStream
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!updatedIntervals.isEmpty()) {
                updatedIntervals.values().forEach(TbIntervalState::clearChangesToReport);
                updatedStates.put(entityId, updatedIntervals);
            }
        });

        return updatedStates;
    }

    Map<EntityId, Map<Long, TbIntervalState>> getStatesToPersist() {
        Map<EntityId, Map<Long, TbIntervalState>> updatedStates = new HashMap<>();

        states.forEach((entityId, intervals) -> {
            Map<Long, TbIntervalState> updatedIntervals = intervals.entrySet().stream().filter(e -> e.getValue().hasChangesToPersist())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!updatedIntervals.isEmpty()) {
                updatedIntervals.values().forEach(TbIntervalState::clearChangesToPersist);
                updatedStates.put(entityId, updatedIntervals);
            }
        });

        return updatedStates;
    }


    void cleanupStatesUsingTTL() {
        long expTime = System.currentTimeMillis() - intervalTtl;
        states.forEach((entityId, intervals) -> {
            List<Long> keysToRemove = intervals.keySet().stream().filter(ts -> ts < expTime).collect(Collectors.toList());
            keysToRemove.forEach(intervals::remove);
        });
    }

    private ListenableFuture<TbIntervalState> fetchIntervalState(EntityId entityId, long intervalStartTs) {
        ListenableFuture<TsKvEntry> f = ctx.getTimeseriesService().findOne(ctx.getTenantId(), entityId, intervalStartTs, DataConstants.RULE_NODE_STATE_PREFIX + ctx.getSelfId());
        return Futures.transform(f, input -> {
            String value = null;
            if (input != null) {
                value = input.getStrValue().orElse(null);
            }
            if (StringUtils.isEmpty(value)) {
                return createDefaultTbIntervalState();
            } else {
                return readTbIntervalState(value);
            }
        }, MoreExecutors.directExecutor());
    }

    private TbIntervalState readTbIntervalState(String value) {
        JsonElement stateJson = JsonParser.parseString(value);
        switch (function) {
            case MIN:
                return new TbMinIntervalState(stateJson);
            case MAX:
                return new TbMaxIntervalState(stateJson);
            case SUM:
                return new TbSumIntervalState(stateJson);
            case AVG:
                return new TbAvgIntervalState(stateJson);
            case COUNT:
                return new TbCountIntervalState(stateJson);
            case COUNT_UNIQUE:
                return new TbCountUniqueIntervalState(stateJson);
            default:
                throw new IllegalArgumentException("Unsupported incoming function: " + function.name() + "!");
        }
    }

    private TbIntervalState createDefaultTbIntervalState() {
        switch (function) {
            case MIN:
                return new TbMinIntervalState();
            case MAX:
                return new TbMaxIntervalState();
            case SUM:
                return new TbSumIntervalState();
            case AVG:
                return new TbAvgIntervalState();
            case COUNT:
                return new TbCountIntervalState();
            case COUNT_UNIQUE:
                return new TbCountUniqueIntervalState();
            default:
                throw new IllegalArgumentException("Unsupported incoming function: " + function.name() + "!");
        }
    }

    private long calculateIntervalStart(long ts) {
        if (AggIntervalType.CUSTOM.equals(aggIntervalType)) {
            return (ts / intervalDuration) * intervalDuration;
        } else {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), tz);
            switch (aggIntervalType) {
                case HOUR:
                    return zdt.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
                case DAY:
                    return zdt.truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
                case WEEK:
                    return zdt.truncatedTo(ChronoUnit.DAYS).with(DayOfWeek.MONDAY).toInstant().toEpochMilli();
                case WEEK_SUN_SAT:
                    return zdt.truncatedTo(ChronoUnit.DAYS).with(DayOfWeek.SUNDAY).toInstant().toEpochMilli();
                case MONTH:
                    return zdt.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant().toEpochMilli();
                case YEAR:
                    return zdt.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1).toInstant().toEpochMilli();
                default:
                    return (ts / intervalDuration) * intervalDuration;
            }
        }
    }

    private long getDefaultIntervalDurationByAggType() {
        switch (aggIntervalType) {
            case HOUR:
                return TimeUnit.HOURS.toMillis(1);
            case DAY:
                return TimeUnit.DAYS.toMillis(1);
            case WEEK:
            case WEEK_SUN_SAT:
                return TimeUnit.DAYS.toMillis(7);
            case MONTH:
                return TimeUnit.DAYS.toMillis(30);
            case YEAR:
            default:
                return TimeUnit.HOURS.toMillis(365);
        }
    }

}
