/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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
            gate.onSpanStarted();
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
