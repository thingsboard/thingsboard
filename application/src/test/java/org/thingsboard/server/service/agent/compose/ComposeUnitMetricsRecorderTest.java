// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.VolumeInfo;
import org.thingsboard.server.service.agent.AgentMetricsTimeseriesService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComposeUnitMetricsRecorderTest {

    @Mock
    private AgentMetricsTimeseriesService metricsTsService;

    @InjectMocks
    private ComposeUnitMetricsRecorder recorder;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    @Test
    void record_savesBothCpuAndMemory() {
        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);
        Map<String, ContainerInfo> containerStates = Map.of(
                "web", ContainerInfo.newBuilder().setCpuPercent(12.5).setMemoryBytes(2048L).build());

        recorder.record(TENANT_ID, Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), web), containerStates, Map.of());

        ArgumentCaptor<List<TsKvEntry>> captor = entriesCaptor();
        verify(metricsTsService).save(eq(TENANT_ID), eq(web.getId()), captor.capture(), anyString());
        assertThat(captor.getValue())
                .extracting(TsKvEntry::getKey).containsExactlyInAnyOrder("cpuPercent", "memoryBytes");
    }

    @Test
    void record_skipsAbsentFields() {
        AgentAppUnit cpuOnly = newUnit("cpuOnly", AgentAppUnitType.CONTAINER);
        AgentAppUnit memOnly = newUnit("memOnly", AgentAppUnitType.CONTAINER);
        Map<String, ContainerInfo> containerStates = Map.of(
                "cpuOnly", ContainerInfo.newBuilder().setCpuPercent(5.0).build(),
                "memOnly", ContainerInfo.newBuilder().setMemoryBytes(1024L).build());

        recorder.record(TENANT_ID, Map.of(
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "cpuOnly"), cpuOnly,
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "memOnly"), memOnly), containerStates, Map.of());

        ArgumentCaptor<List<TsKvEntry>> captor = entriesCaptor();
        verify(metricsTsService, times(2)).save(eq(TENANT_ID), any(), captor.capture(), anyString());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues()).allSatisfy(entries -> assertThat(entries).hasSize(1));
    }

    @Test
    void record_skipsContainerWithoutMetrics() {
        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);
        Map<String, ContainerInfo> containerStates = Map.of(
                "web", ContainerInfo.newBuilder().setState("running").build());

        recorder.record(TENANT_ID, Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), web), containerStates, Map.of());

        verify(metricsTsService, never()).save(any(), any(), any(), anyString());
    }

    @Test
    void record_skipsUnknownContainers() {
        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);
        Map<String, ContainerInfo> containerStates = Map.of(
                "web", ContainerInfo.newBuilder().setCpuPercent(1.0).build(),
                "ghost", ContainerInfo.newBuilder().setCpuPercent(2.0).build());

        recorder.record(TENANT_ID, Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), web), containerStates, Map.of());

        verify(metricsTsService, times(1)).save(eq(TENANT_ID), eq(web.getId()), any(), anyString());
    }

    @Test
    void record_savesVolumeSizeOnVolumeUnit() {
        AgentAppUnit data = newUnit("data", AgentAppUnitType.VOLUME);
        Map<String, VolumeInfo> volumeStates = Map.of(
                "data", VolumeInfo.newBuilder().setSizeBytes(1_000_000L).build());

        recorder.record(TENANT_ID, Map.of(new AgentAppUnitKey(AgentAppUnitType.VOLUME, "data"), data), Map.of(), volumeStates);

        ArgumentCaptor<List<TsKvEntry>> captor = entriesCaptor();
        verify(metricsTsService).save(eq(TENANT_ID), eq(data.getId()), captor.capture(), anyString());
        assertThat(captor.getValue())
                .extracting(TsKvEntry::getKey).containsExactly("sizeBytes");
        assertThat(captor.getValue().get(0).getLongValue()).hasValue(1_000_000L);
    }

    @Test
    void record_skipsVolumeWithoutSize() {
        AgentAppUnit data = newUnit("data", AgentAppUnitType.VOLUME);
        Map<String, VolumeInfo> volumeStates = Map.of(
                "data", VolumeInfo.newBuilder().build());

        recorder.record(TENANT_ID, Map.of(new AgentAppUnitKey(AgentAppUnitType.VOLUME, "data"), data), Map.of(), volumeStates);

        verify(metricsTsService, never()).save(any(), any(), any(), anyString());
    }

    @Test
    void record_skipsVolumeWhenUnitTypeIsContainer() {
        // Defensive: if identifiers collide between map types, never persist
        // the wrong metric on the wrong unit.
        AgentAppUnit confused = newUnit("data", AgentAppUnitType.CONTAINER);
        Map<String, VolumeInfo> volumeStates = Map.of(
                "data", VolumeInfo.newBuilder().setSizeBytes(42L).build());

        recorder.record(TENANT_ID, Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "data"), confused), Map.of(), volumeStates);

        verify(metricsTsService, never()).save(any(), any(), any(), anyString());
    }

    private AgentAppUnit newUnit(String identifier, AgentAppUnitType type) {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(UUID.randomUUID()));
        unit.setAgentApplicationId(APP_ID);
        unit.setIdentifier(identifier);
        unit.setType(type);
        return unit;
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<TsKvEntry>> entriesCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
