// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BufferedTraceExporterTest {
    private StatsFactory statsFactory;
    private TraceBuffer buffer;
    private TraceSpanMapper spanMapper;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        statsFactory = mock(StatsFactory.class);
        when(statsFactory.createGauge(anyString(), any(AtomicLong.class), any(String[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(statsFactory.createDefaultCounter(anyString(), any(String[].class)))
                .thenAnswer(invocation -> new DefaultCounter(new AtomicInteger(0),
                        registry.counter(invocation.getArgument(0))));
        when(statsFactory.createTimer(anyString(), any(String[].class)))
                .thenAnswer(invocation -> registry.timer(invocation.getArgument(0)));

        buffer = mock(TraceBuffer.class);
        spanMapper = mock(TraceSpanMapper.class);
        when(spanMapper.toBufferedSpan(any(SpanData.class), anyLong())).thenReturn(mock(BufferedSpan.class));
    }

    private static List<SpanData> spans(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> mock(SpanData.class))
                .collect(java.util.stream.Collectors.toList());
    }

    private TraceExportPressureGate gate(int capacity) {
        TraceExportPressureGate gate = new TraceExportPressureGate(statsFactory, capacity, 50, 100);
        for (int i = 0; i < capacity; i++) {
            gate.admitEndedSpan();
        }
        return gate;
    }

    @Test
    void givenSuccessfulAppend_whenExported_thenSlotsReleased() {
        TraceExportPressureGate gate = gate(10);
        BufferedTraceExporter exporter = new BufferedTraceExporter(buffer, spanMapper, gate, statsFactory);

        exporter.export(spans(10));

        assertThat(gate.getInFlight()).isZero();
    }

    @Test
    void givenFailingAppend_whenExported_thenSlotsStillReleased() {
        TraceExportPressureGate gate = gate(10);
        doThrow(new RuntimeException("redis is down")).when(buffer).append(any());
        BufferedTraceExporter exporter = new BufferedTraceExporter(buffer, spanMapper, gate, statsFactory);

        exporter.export(spans(10));

        assertThat(gate.getInFlight()).isZero();
    }

    @Test
    void givenRepeatedFailures_whenExported_thenGateStaysOpen() {
        TraceExportPressureGate gate = gate(100);
        doThrow(new RuntimeException("redis is down")).when(buffer).append(any());
        BufferedTraceExporter exporter = new BufferedTraceExporter(buffer, spanMapper, gate, statsFactory);

        for (int i = 0; i < 10; i++) {
            exporter.export(spans(10));
        }

        assertThat(gate.getInFlight()).isZero();
        assertThat(gate.canIssueTraceId()).isTrue();
        assertThat(gate.canCreateSpan()).isTrue();
    }

    @Test
    void givenEmptyBatch_whenExported_thenNothingReleased() {
        TraceExportPressureGate gate = gate(10);
        BufferedTraceExporter exporter = new BufferedTraceExporter(buffer, spanMapper, gate, statsFactory);

        exporter.export(List.of());

        assertThat(gate.getInFlight()).isEqualTo(10);
    }
}
