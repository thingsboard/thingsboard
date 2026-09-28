// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentMetricsTimeseriesServiceTest {

    private static final long CONFIGURED_TTL_SECONDS = 600L;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final EntityId UNIT_ID = new AgentAppUnitId(UUID.randomUUID());
    private static final String IDENTIFIER = "unit:test-service";

    @Mock
    private TelemetrySubscriptionService tsSubService;

    @InjectMocks
    private AgentMetricsTimeseriesService service;

    @Test
    void save_propagatesConfiguredTtlAndPayload_toTimeseriesRequest() {
        ReflectionTestUtils.setField(service, "metricsTtlSeconds", CONFIGURED_TTL_SECONDS);
        long ts = System.currentTimeMillis();
        List<TsKvEntry> entries = List.of(
                new BasicTsKvEntry(ts, new DoubleDataEntry("cpuPercent", 1.5)),
                new BasicTsKvEntry(ts, new LongDataEntry("memoryBytes", 1024L)));

        service.save(TENANT_ID, UNIT_ID, entries, IDENTIFIER);

        ArgumentCaptor<TimeseriesSaveRequest> captor = ArgumentCaptor.forClass(TimeseriesSaveRequest.class);
        verify(tsSubService).saveTimeseries(captor.capture());
        TimeseriesSaveRequest captured = captor.getValue();

        assertThat(captured.getTtl()).isEqualTo(CONFIGURED_TTL_SECONDS);
        assertThat(captured.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captured.getEntityId()).isEqualTo(UNIT_ID);
        assertThat(captured.getEntries()).isEqualTo(entries);
        assertThat(captured.getStrategy()).isEqualTo(TimeseriesSaveRequest.Strategy.PROCESS_ALL);
    }
}
