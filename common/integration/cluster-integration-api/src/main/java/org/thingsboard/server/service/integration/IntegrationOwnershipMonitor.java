// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Exposes, per integration type, which partitions this node owns according to the PartitionService, which partition
 * snapshot was actually applied by the last refresh, how many integrations run in each state and which singleton
 * integrations run although they are not mine. Gauges are labelled by integration type (and partition), never by
 * integration id, so their cardinality stays bounded.
 */
@Slf4j
class IntegrationOwnershipMonitor {

    static final String PARTITIONS_OWNED = "integration_partitions_owned";
    static final String PARTITIONS_APPLIED = "integration_partitions_applied";
    static final String PARTITIONS_MISMATCH = "integration_partitions_mismatch";
    static final String PARTITION_OWNED = "integration_partition_owned";
    static final String PARTITION_APPLIED = "integration_partition_applied";
    static final String SINGLETONS_NOT_MINE_RUNNING = "integration_singletons_not_mine_running";
    static final String STATES = "integration_states";

    static final String STATE_STARTED = "STARTED";
    static final String STATE_FAILED = "FAILED";
    static final String STATE_OTHER = "OTHER";

    // Per-partition gauges are registered for singleton types only, and only while the partition count stays small.
    static final int MAX_PARTITION_GAUGES = 32;
    static final int MAX_LOGGED_IDS = 20;

    record TypeOwnership(IntegrationType type, Set<Integer> owned, Set<Integer> applied,
                         int started, int failed, int other, List<IntegrationId> notMineRunning) {

        boolean isMismatch() {
            return !owned.equals(applied);
        }

        boolean isEmpty() {
            return owned.isEmpty() && applied.isEmpty() && started == 0 && failed == 0 && other == 0 && notMineRunning.isEmpty();
        }

    }

    private final StatsFactory statsFactory;
    private final int totalPartitions;
    private final long printIntervalMs;
    private final Map<String, AtomicInteger> gauges = new ConcurrentHashMap<>();

    private String lastSignature;
    private long lastPrintTs;

    IntegrationOwnershipMonitor(StatsFactory statsFactory, int totalPartitions, long printIntervalMs) {
        this.statsFactory = statsFactory;
        this.totalPartitions = totalPartitions;
        this.printIntervalMs = printIntervalMs;
    }

    /**
     * Updates the gauges and returns the stats line if it is due: something is wrong (applied != owned, or a singleton
     * that is not mine is running), the partition picture changed since the last print, or printIntervalMs passed.
     * Returns null otherwise.
     */
    synchronized String check(List<TypeOwnership> snapshot, long now) {
        snapshot.forEach(this::updateGauges);
        boolean anomaly = snapshot.stream().anyMatch(t -> t.isMismatch() || !t.notMineRunning().isEmpty());
        String signature = signature(snapshot);
        if (anomaly || !signature.equals(lastSignature) || now - lastPrintTs >= printIntervalMs) {
            lastSignature = signature;
            lastPrintTs = now;
            String line = format(snapshot);
            log.info("Integration Ownership Stats: {}", line);
            return line;
        }
        return null;
    }

    int getGaugeValue(String name, String... tags) {
        AtomicInteger gauge = gauges.get(gaugeKey(name, tags));
        return gauge != null ? gauge.get() : -1;
    }

    private void updateGauges(TypeOwnership t) {
        String type = t.type().name();
        gauge(PARTITIONS_OWNED, "type", type).set(t.owned().size());
        gauge(PARTITIONS_APPLIED, "type", type).set(t.applied().size());
        gauge(PARTITIONS_MISMATCH, "type", type).set(t.isMismatch() ? 1 : 0);
        gauge(SINGLETONS_NOT_MINE_RUNNING, "type", type).set(t.notMineRunning().size());
        gauge(STATES, "type", type, "state", STATE_STARTED).set(t.started());
        gauge(STATES, "type", type, "state", STATE_FAILED).set(t.failed());
        gauge(STATES, "type", type, "state", STATE_OTHER).set(t.other());
        if (t.type().isSingleton() && totalPartitions > 0 && totalPartitions <= MAX_PARTITION_GAUGES) {
            for (int p = 0; p < totalPartitions; p++) {
                String partition = Integer.toString(p);
                gauge(PARTITION_OWNED, "type", type, "partition", partition).set(t.owned().contains(p) ? 1 : 0);
                gauge(PARTITION_APPLIED, "type", type, "partition", partition).set(t.applied().contains(p) ? 1 : 0);
            }
        }
    }

    private AtomicInteger gauge(String name, String... tags) {
        // The map keeps a strong reference: Micrometer only holds the gauge's number weakly.
        return gauges.computeIfAbsent(gaugeKey(name, tags), k -> statsFactory != null
                ? statsFactory.createGauge(name, new AtomicInteger(0), tags)
                : new AtomicInteger(0));
    }

    private static String gaugeKey(String name, String... tags) {
        return name + String.join(",", tags);
    }

    private static String signature(List<TypeOwnership> snapshot) {
        return snapshot.stream()
                .map(t -> t.type() + ":" + new TreeSet<>(t.owned()) + new TreeSet<>(t.applied()) + t.notMineRunning().size())
                .collect(Collectors.joining(";"));
    }

    static String format(List<TypeOwnership> snapshot) {
        String line = snapshot.stream()
                .filter(t -> !t.isEmpty())
                .map(IntegrationOwnershipMonitor::format)
                .collect(Collectors.joining(", "));
        return line.isEmpty() ? "no integrations" : line;
    }

    private static String format(TypeOwnership t) {
        StringBuilder sb = new StringBuilder(t.type().name())
                .append("[owned=").append(new TreeSet<>(t.owned()))
                .append(", applied=").append(new TreeSet<>(t.applied()))
                .append(", started=").append(t.started())
                .append(", failed=").append(t.failed())
                .append(", other=").append(t.other())
                .append(", notMineRunning=").append(t.notMineRunning().size());
        if (t.isMismatch()) {
            sb.append(", MISMATCH");
        }
        if (!t.notMineRunning().isEmpty()) {
            sb.append(" ").append(t.notMineRunning().stream().limit(MAX_LOGGED_IDS)
                    .map(id -> id.getId().toString()).collect(Collectors.joining(", ", "[", "]")));
            if (t.notMineRunning().size() > MAX_LOGGED_IDS) {
                sb.append(" +").append(t.notMineRunning().size() - MAX_LOGGED_IDS).append(" more");
            }
        }
        return sb.append("]").toString();
    }

}
