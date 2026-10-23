// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.stats;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.stats.EdgeStatsKey;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos.EdgeStatsMsgProto;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_LAG;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED;

/**
 * Runs on the TB_CORE partition owner of an edge and is the only writer of the edge's downlink stats.
 * Sums the per-node parts sent by {@link EdgeStatsService} for each report interval and saves the totals,
 * so stats of different nodes don't overwrite each other.
 */
@TbCoreComponent
@ConditionalOnProperty(prefix = "edges.stats", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Service
@Slf4j
public class EdgeStatsAggregationService {

    private static final List<String> COUNTER_KEYS = List.of(DOWNLINK_MSGS_ADDED.getKey(), DOWNLINK_MSGS_PUSHED.getKey(),
            DOWNLINK_MSGS_PERMANENTLY_FAILED.getKey(), DOWNLINK_MSGS_TMP_FAILED.getKey());

    private final TimeseriesService tsService;
    private final TopicService topicService;
    private final Optional<KafkaAdmin> kafkaAdmin;
    private final AttributesService attributesService;
    private final EdgeEventService edgeEventService;
    private final TbTransactionalCache<EdgeId, String> edgeIdServiceIdCache;

    @Value("${edges.stats.ttl:30}")
    private int edgesStatsTtlDays;
    @Value("${edges.stats.report-interval-millis:600000}")
    private long reportIntervalMillis;
    @Value("${edges.stats.max-lag-count:100000}")
    private int maxLagCount;
    @Value("${edges.storage.misordering_compensation_millis:60000}")
    private long misorderingCompensationMillis;

    private final ConcurrentMap<IntervalKey, EdgeStatsInterval> intervals = new ConcurrentHashMap<>();

    public void onEdgeStatsMsg(EdgeStatsMsgProto msg, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
            EdgeId edgeId = new EdgeId(new UUID(msg.getEdgeIdMSB(), msg.getEdgeIdLSB()));
            IntervalKey key = new IntervalKey(edgeId, msg.getTs());
            EdgeStatsInterval interval = intervals.get(key);
            if (interval == null) {
                Map<String, Long> alreadySavedTotals = findAlreadySavedIntervalTotals(tenantId, edgeId, msg.getTs());
                EdgeStatsInterval seeded = new EdgeStatsInterval(tenantId, alreadySavedTotals);
                interval = intervals.putIfAbsent(key, seeded);
                if (interval == null) {
                    interval = seeded;
                }
            }
            interval.add(msg);
            callback.onSuccess();
        } catch (Exception e) {
            log.warn("Failed to process edge stats msg: {}", msg, e);
            callback.onFailure(e);
        }
    }

    @Scheduled(
            fixedDelayString = "${edges.stats.flush-interval-millis:60000}",
            initialDelayString = "${edges.stats.flush-interval-millis:60000}"
    )
    public void flush() {
        long currentIntervalTs = currentIntervalTs();
        Map<IntervalKey, EdgeStatsSnapshot> snapshots = new HashMap<>();
        // lag is the current backlog, so it is saved only for the current interval; an older interval saved again
        // because of a late part keeps the lag it got while it was current
        Map<EdgeId, EdgeStatsSnapshot> currentSnapshots = new HashMap<>();
        intervals.forEach((key, interval) -> interval.takeSnapshotIfDirty().ifPresent(snapshot -> {
            snapshots.put(key, snapshot);
            if (key.ts() >= currentIntervalTs) {
                currentSnapshots.put(key.edgeId(), snapshot);
            }
        }));
        if (!snapshots.isEmpty()) {
            log.debug("Saving aggregated edge communication stats for {} edge intervals", snapshots.size());
            Map<EdgeId, Long> lagByEdgeId = getLagByEdgeId(currentSnapshots);
            snapshots.forEach((key, snapshot) -> save(key, snapshot, key.ts() >= currentIntervalTs ? lagByEdgeId.get(key.edgeId()) : null));
        }
        intervals.entrySet().removeIf(e -> e.getKey().ts() < currentIntervalTs && !e.getValue().isDirty());
    }

    private Map<EdgeId, Long> getLagByEdgeId(Map<EdgeId, EdgeStatsSnapshot> snapshots) {
        Map<EdgeId, Long> lagByEdgeId = new HashMap<>();
        if (kafkaAdmin.isPresent()) {
            Map<EdgeId, String> edgeToTopicMap = snapshots.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> topicService.buildEdgeEventNotificationsTopicPartitionInfo(e.getValue().tenantId(), e.getKey()).getTopic()
                    ));
            Map<String, Long> lagByTopic = edgeToTopicMap.isEmpty() ? Map.of() :
                    kafkaAdmin.get().getTotalLagForGroupsBulk(new HashSet<>(edgeToTopicMap.values()));
            edgeToTopicMap.forEach((edgeId, topic) -> lagByEdgeId.put(edgeId, lagByTopic.getOrDefault(topic, 0L)));
        } else {
            snapshots.forEach((edgeId, snapshot) -> {
                long lag = snapshot.lag();
                if (!snapshot.lagMeasured() || !hasSession(edgeId)) {
                    // no live session measured the lag, or the session that measured it is gone
                    // (e.g. edge went offline after its session node reported), so count the backlog in the db
                    lag = getPendingEdgeEventsCount(snapshot.tenantId(), edgeId).orElse(lag);
                }
                lagByEdgeId.put(edgeId, lag);
            });
        }
        return lagByEdgeId;
    }

    // the edge's session node is set on connect and evicted on disconnect, see EdgeGrpcService
    private boolean hasSession(EdgeId edgeId) {
        return edgeIdServiceIdCache.get(edgeId) != null;
    }

    // Counts events after the queue offset saved by PostgresGeneralEdgeEventsDispatcher, with the same bounds as GeneralEdgeEventFetcher.
    // Right after seq_id starts a new cycle, newer events have lower seq ids and are not counted until the session catches up.
    private Optional<Long> getPendingEdgeEventsCount(TenantId tenantId, EdgeId edgeId) {
        try {
            List<AttributeKvEntry> offset = attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE,
                    List.of(DataConstants.EDGE_QUEUE_START_TS_ATTR_KEY, DataConstants.EDGE_QUEUE_START_SEQ_ID_ATTR_KEY)).get();
            long queueStartTs = 0L;
            long queueStartSeqId = 0L;
            for (AttributeKvEntry entry : offset) {
                long value = entry.getLongValue().orElse(0L);
                if (DataConstants.EDGE_QUEUE_START_TS_ATTR_KEY.equals(entry.getKey())) {
                    queueStartTs = value;
                } else if (DataConstants.EDGE_QUEUE_START_SEQ_ID_ATTR_KEY.equals(entry.getKey())) {
                    queueStartSeqId = value;
                }
            }
            long startTime = queueStartTs > 0 ? queueStartTs - misorderingCompensationMillis : 0L;
            return Optional.of(edgeEventService.countEdgeEvents(tenantId, edgeId, queueStartSeqId, startTime, maxLagCount));
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to count pending edge events", tenantId, edgeId, e);
            return Optional.empty();
        }
    }

    /**
     * Returns the counter totals already saved for the interval, keyed by stats key, or an empty map if nothing was saved yet.
     * They exist when the interval was partly aggregated and saved by another owner of the edge partition (repartitioning)
     * or by this node before a restart; the new parts are added on top of them instead of overwriting them.
     * <ul>
     *     <li>latest values are all older than the interval: nothing saved for it yet, no more reads;</li>
     *     <li>latest values are at the interval: they are the saved totals;</li>
     *     <li>latest values are newer than the interval (a late part of an older interval): the interval itself is read.</li>
     * </ul>
     */
    private Map<String, Long> findAlreadySavedIntervalTotals(TenantId tenantId, EdgeId edgeId, long intervalTs) throws Exception {
        List<TsKvEntry> latest = tsService.findLatest(tenantId, edgeId, COUNTER_KEYS).get();
        if (latest.stream().allMatch(e -> e.getTs() < intervalTs)) {
            return Map.of();
        }
        List<TsKvEntry> saved = latest;
        if (latest.stream().anyMatch(e -> e.getTs() > intervalTs)) {
            List<ReadTsKvQuery> queries = COUNTER_KEYS.stream()
                    .<ReadTsKvQuery>map(key -> new BaseReadTsKvQuery(key, intervalTs, intervalTs + 1, 1, "DESC"))
                    .toList();
            saved = tsService.findAll(tenantId, edgeId, queries).get();
        }
        return saved.stream()
                .filter(e -> e.getTs() == intervalTs && e.getLongValue().isPresent())
                .collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().get(), (a, b) -> a));
    }

    // lag is null for an older interval: only its counters are saved
    private void save(IntervalKey key, EdgeStatsSnapshot snapshot, Long lag) {
        long ts = key.ts();
        List<TsKvEntry> statsEntries = new ArrayList<>(List.of(
                entry(ts, DOWNLINK_MSGS_ADDED, snapshot.added()),
                entry(ts, DOWNLINK_MSGS_PUSHED, snapshot.pushed()),
                entry(ts, DOWNLINK_MSGS_PERMANENTLY_FAILED, snapshot.permanentlyFailed()),
                entry(ts, DOWNLINK_MSGS_TMP_FAILED, snapshot.tmpFailed())
        ));
        if (lag != null) {
            statsEntries.add(entry(ts, DOWNLINK_MSGS_LAG, lag));
        }
        log.trace("Saving aggregated edge communication stats: {} tenantId - {}, edgeId - {}", statsEntries, snapshot.tenantId(), key.edgeId());
        try {
            Futures.addCallback(tsService.save(snapshot.tenantId(), key.edgeId(), statsEntries, TimeUnit.DAYS.toSeconds(edgesStatsTtlDays)),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(TimeseriesSaveResult result) {
                            log.debug("Successfully saved edge time-series stats: {} for edge: {}", statsEntries, key.edgeId());
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.warn("Failed to save edge time-series stats for edge: {}", key.edgeId(), t);
                            markDirty(key);
                        }
                    }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            log.warn("Failed to save edge time-series stats for edge: {}", key.edgeId(), e);
            markDirty(key);
        }
    }

    private void markDirty(IntervalKey key) {
        EdgeStatsInterval interval = intervals.get(key);
        if (interval != null) {
            interval.markDirty();
        }
    }

    private long currentIntervalTs() {
        long now = System.currentTimeMillis();
        return now - (now % reportIntervalMillis);
    }

    private BasicTsKvEntry entry(long ts, EdgeStatsKey key, long value) {
        return new BasicTsKvEntry(ts, new LongDataEntry(key.getKey(), value));
    }

    record IntervalKey(EdgeId edgeId, long ts) {}

    record EdgeStatsSnapshot(TenantId tenantId, long added, long pushed, long permanentlyFailed, long tmpFailed,
                             long lag, boolean lagMeasured) {}

    static class EdgeStatsInterval {

        private final TenantId tenantId;
        private long added;
        private long pushed;
        private long permanentlyFailed;
        private long tmpFailed;
        private long lag;
        private long lagTs;
        private boolean lagMeasured;
        private boolean dirty;

        EdgeStatsInterval(TenantId tenantId, Map<String, Long> alreadySavedTotals) {
            this.tenantId = tenantId;
            this.added = alreadySavedTotals.getOrDefault(DOWNLINK_MSGS_ADDED.getKey(), 0L);
            this.pushed = alreadySavedTotals.getOrDefault(DOWNLINK_MSGS_PUSHED.getKey(), 0L);
            this.permanentlyFailed = alreadySavedTotals.getOrDefault(DOWNLINK_MSGS_PERMANENTLY_FAILED.getKey(), 0L);
            this.tmpFailed = alreadySavedTotals.getOrDefault(DOWNLINK_MSGS_TMP_FAILED.getKey(), 0L);
        }

        synchronized void add(EdgeStatsMsgProto msg) {
            added += msg.getMsgsAdded();
            pushed += msg.getMsgsPushed();
            permanentlyFailed += msg.getMsgsPermanentlyFailed();
            tmpFailed += msg.getMsgsTmpFailed();
            // lag is a gauge, not a counter: only a node holding a live session measures it; parts may arrive in any order,
            // e.g. after the edge reconnected to another node, so the newest measurement wins
            if (msg.getMsgsLagMeasured() && (!lagMeasured || msg.getMsgsLagTs() >= lagTs)) {
                lag = msg.getMsgsLag();
                lagTs = msg.getMsgsLagTs();
                lagMeasured = true;
            }
            dirty = true;
        }

        synchronized Optional<EdgeStatsSnapshot> takeSnapshotIfDirty() {
            if (!dirty) {
                return Optional.empty();
            }
            dirty = false;
            return Optional.of(new EdgeStatsSnapshot(tenantId, added, pushed, permanentlyFailed, tmpFailed, lag, lagMeasured));
        }

        synchronized void markDirty() {
            dirty = true;
        }

        synchronized boolean isDirty() {
            return dirty;
        }

    }

}
