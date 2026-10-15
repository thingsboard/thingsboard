// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.agent.v1.ApplicationMetrics;
import org.thingsboard.server.service.agent.AgentMetricsTimeseriesService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComposeApplicationMetricsRecorderTest {

    @Mock
    private AgentMetricsTimeseriesService metricsTsService;

    @InjectMocks
    private ComposeApplicationMetricsRecorder recorder;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    @Test
    void record_savesBothCpuAndMemoryAgainstApplicationId() {
        ApplicationMetrics metrics = ApplicationMetrics.newBuilder()
                .setCpuPercent(42.0)
                .setMemoryBytes(8192L)
                .build();

        recorder.record(TENANT_ID, APP_ID, metrics);

        ArgumentCaptor<List<TsKvEntry>> captor = entriesCaptor();
        verify(metricsTsService).save(eq(TENANT_ID), eq(APP_ID), captor.capture(), anyString());

        List<TsKvEntry> entries = captor.getValue();
        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(TsKvEntry::getKey).containsExactlyInAnyOrder("cpuPercent", "memoryBytes");
    }

    @Test
    void record_persistsVolumeBytesWhenPresent() {
        ApplicationMetrics metrics = ApplicationMetrics.newBuilder()
                .setCpuPercent(1.0)
                .setMemoryBytes(1L)
                .setVolumeBytes(7_777L)
                .build();

        recorder.record(TENANT_ID, APP_ID, metrics);

        ArgumentCaptor<List<TsKvEntry>> captor = entriesCaptor();
        verify(metricsTsService).save(eq(TENANT_ID), eq(APP_ID), captor.capture(), anyString());

        assertThat(captor.getValue())
                .extracting(TsKvEntry::getKey)
                .containsExactlyInAnyOrder("cpuPercent", "memoryBytes", "volumeBytes");
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<TsKvEntry>> entriesCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
