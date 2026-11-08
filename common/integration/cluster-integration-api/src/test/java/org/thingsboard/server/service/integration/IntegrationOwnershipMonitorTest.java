// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.service.integration.IntegrationOwnershipMonitor.TypeOwnership;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.integration.IntegrationOwnershipMonitor.PARTITIONS_APPLIED;
import static org.thingsboard.server.service.integration.IntegrationOwnershipMonitor.PARTITIONS_MISMATCH;
import static org.thingsboard.server.service.integration.IntegrationOwnershipMonitor.PARTITIONS_OWNED;
import static org.thingsboard.server.service.integration.IntegrationOwnershipMonitor.PARTITION_APPLIED;
import static org.thingsboard.server.service.integration.IntegrationOwnershipMonitor.PARTITION_OWNED;
import static org.thingsboard.server.service.integration.IntegrationOwnershipMonitor.SINGLETONS_NOT_MINE_RUNNING;
import static org.thingsboard.server.service.integration.IntegrationOwnershipMonitor.STATES;

class IntegrationOwnershipMonitorTest {

    private static final long PRINT_INTERVAL = 600_000;

    private StatsFactory statsFactory;
    private IntegrationOwnershipMonitor monitor;

    @BeforeEach
    void setUp() {
        statsFactory = mock(StatsFactory.class);
        when(statsFactory.createGauge(any(String.class), any(Number.class), any(String[].class))).thenAnswer(inv -> inv.getArgument(1));
        monitor = new IntegrationOwnershipMonitor(statsFactory, 3, PRINT_INTERVAL);
    }

    @Test
    void givenAppliedDiffersFromOwned_thenMismatchGaugesAndLineReportIt() {
        // Node whose partition changes were lost: PartitionService says [1], the last applied snapshot was [0, 2].
        IntegrationId stray = new IntegrationId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        TypeOwnership mqtt = new TypeOwnership(IntegrationType.MQTT, Set.of(1), Set.of(0, 2), 240, 17, 0, List.of(stray));

        String line = monitor.check(List.of(mqtt), 1000);

        assertThat(monitor.getGaugeValue(PARTITIONS_OWNED, "type", "MQTT")).isEqualTo(1);
        assertThat(monitor.getGaugeValue(PARTITIONS_APPLIED, "type", "MQTT")).isEqualTo(2);
        assertThat(monitor.getGaugeValue(PARTITIONS_MISMATCH, "type", "MQTT")).isEqualTo(1);
        assertThat(monitor.getGaugeValue(SINGLETONS_NOT_MINE_RUNNING, "type", "MQTT")).isEqualTo(1);
        assertThat(monitor.getGaugeValue(STATES, "type", "MQTT", "state", "STARTED")).isEqualTo(240);
        assertThat(monitor.getGaugeValue(STATES, "type", "MQTT", "state", "FAILED")).isEqualTo(17);
        assertThat(monitor.getGaugeValue(PARTITION_OWNED, "type", "MQTT", "partition", "0")).isZero();
        assertThat(monitor.getGaugeValue(PARTITION_OWNED, "type", "MQTT", "partition", "1")).isEqualTo(1);
        assertThat(monitor.getGaugeValue(PARTITION_APPLIED, "type", "MQTT", "partition", "0")).isEqualTo(1);
        assertThat(monitor.getGaugeValue(PARTITION_APPLIED, "type", "MQTT", "partition", "1")).isZero();
        assertThat(monitor.getGaugeValue(PARTITION_APPLIED, "type", "MQTT", "partition", "2")).isEqualTo(1);
        assertThat(line).isEqualTo("MQTT[owned=[1], applied=[0, 2], started=240, failed=17, other=0, notMineRunning=1, MISMATCH "
                + "[00000000-0000-0000-0000-000000000001]]");
        verify(statsFactory).createGauge(eq(PARTITIONS_MISMATCH), any(Number.class), eq("type"), eq("MQTT"));
        verify(statsFactory).createGauge(eq(PARTITION_APPLIED), any(Number.class), eq("type"), eq("MQTT"), eq("partition"), eq("2"));
    }

    @Test
    void givenHealthyAndUnchanged_thenLineIsPrintedOnlyOncePerPrintInterval() {
        TypeOwnership mqtt = new TypeOwnership(IntegrationType.MQTT, Set.of(0), Set.of(0), 10, 1, 0, List.of());

        assertThat(monitor.check(List.of(mqtt), 1000)).isEqualTo("MQTT[owned=[0], applied=[0], started=10, failed=1, other=0, notMineRunning=0]");
        // counts alone do not trigger a print
        assertThat(monitor.check(List.of(new TypeOwnership(IntegrationType.MQTT, Set.of(0), Set.of(0), 12, 0, 0, List.of())), 61_000)).isNull();
        assertThat(monitor.check(List.of(mqtt), 1000 + PRINT_INTERVAL)).isNotNull();
        assertThat(monitor.getGaugeValue(PARTITIONS_MISMATCH, "type", "MQTT")).isZero();
    }

    @Test
    void givenPartitionsChange_thenLineIsPrintedImmediately() {
        monitor.check(List.of(new TypeOwnership(IntegrationType.MQTT, Set.of(0), Set.of(0), 10, 0, 0, List.of())), 1000);

        String line = monitor.check(List.of(new TypeOwnership(IntegrationType.MQTT, Set.of(0, 2), Set.of(0, 2), 10, 0, 0, List.of())), 2000);

        assertThat(line).startsWith("MQTT[owned=[0, 2], applied=[0, 2]");
    }

    @Test
    void givenAnomaly_thenLineIsPrintedOnEveryCheck() {
        TypeOwnership mqtt = new TypeOwnership(IntegrationType.MQTT, Set.of(2), Set.of(1), 10, 0, 0, List.of());

        assertThat(monitor.check(List.of(mqtt), 1000)).isNotNull();
        assertThat(monitor.check(List.of(mqtt), 61_000)).contains("MISMATCH");
    }

    @Test
    void givenManyNotMineSingletons_thenLoggedIdsAreCapped() {
        List<IntegrationId> ids = IntStream.range(0, 25).mapToObj(i -> new IntegrationId(UUID.randomUUID())).toList();

        String line = monitor.check(List.of(new TypeOwnership(IntegrationType.MQTT, Set.of(0), Set.of(0), 25, 0, 0, ids)), 1000);

        assertThat(line).contains("notMineRunning=25").endsWith(" +5 more]");
        assertThat(line.split(",").length).isLessThan(40);
    }

    @Test
    void givenNonSingletonOrEmptyType_thenNoPartitionGaugesAndTypeIsOmittedFromLine() {
        TypeOwnership http = new TypeOwnership(IntegrationType.HTTP, Set.of(0), Set.of(0), 5, 0, 0, List.of());
        TypeOwnership kafka = new TypeOwnership(IntegrationType.KAFKA, Set.of(), Set.of(), 0, 0, 0, List.of());

        String line = monitor.check(List.of(http, kafka), 1000);

        assertThat(line).isEqualTo("HTTP[owned=[0], applied=[0], started=5, failed=0, other=0, notMineRunning=0]");
        assertThat(monitor.getGaugeValue(PARTITION_OWNED, "type", "HTTP", "partition", "0")).isEqualTo(-1);
        assertThat(monitor.getGaugeValue(PARTITION_OWNED, "type", "KAFKA", "partition", "0")).isZero();
        verify(statsFactory, never()).createGauge(eq(PARTITION_OWNED), any(Number.class), eq("type"), eq("HTTP"), eq("partition"), any());
    }

    @Test
    void givenNoStatsFactory_thenCheckStillWorks() {
        IntegrationOwnershipMonitor noMetrics = new IntegrationOwnershipMonitor(null, 3, PRINT_INTERVAL);

        String line = noMetrics.check(List.of(new TypeOwnership(IntegrationType.MQTT, Set.of(0), Set.of(1), 0, 0, 0, List.of())), 1000);

        assertThat(line).contains("MISMATCH");
        assertThat(noMetrics.getGaugeValue(PARTITIONS_MISMATCH, "type", "MQTT")).isEqualTo(1);
    }

}
