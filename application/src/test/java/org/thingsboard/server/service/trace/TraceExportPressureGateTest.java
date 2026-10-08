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

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceExportPressureGateTest {
    private static final String TRACE_ID = "00000000000000000000000000000001";
    private static final String SPAN_ID = "0000000000000001";

    private StatsFactory statsFactory;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        statsFactory = mock(StatsFactory.class);
        when(statsFactory.createGauge(anyString(), any(AtomicLong.class), any(String[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(statsFactory.createDefaultCounter(anyString(), any(String[].class)))
                .thenAnswer(invocation -> new DefaultCounter(new AtomicInteger(0),
                        registry.counter(invocation.getArgument(0))));
    }

    private TraceExportPressureGate gate(int capacity, int traceIdPct, int spanPct) {
        return new TraceExportPressureGate(statsFactory, capacity, traceIdPct, spanPct);
    }

    private static void admit(TraceExportPressureGate gate, int count) {
        for (int i = 0; i < count; i++) {
            gate.onSpanStarted();
        }
    }

    @Test
    void givenEmptyQueue_whenAsked_thenBothGatesOpen() {
        TraceExportPressureGate gate = gate(100, 50, 100);
        assertThat(gate.canIssueTraceId()).isTrue();
        assertThat(gate.canCreateSpan()).isTrue();
    }

    @Test
    void givenQueueJustBelowHalf_whenAsked_thenTraceIdAllowed() {
        TraceExportPressureGate gate = gate(100, 50, 100);
        admit(gate, 49);
        assertThat(gate.canIssueTraceId()).isTrue();
    }

    @Test
    void givenQueueExactlyHalf_whenAsked_thenTraceIdDeniedButSpanAllowed() {
        TraceExportPressureGate gate = gate(100, 50, 100);
        admit(gate, 50);
        assertThat(gate.canIssueTraceId()).isFalse();
        assertThat(gate.canCreateSpan()).isTrue();
    }

    @Test
    void givenFullQueue_whenAsked_thenBothGatesClosed() {
        TraceExportPressureGate gate = gate(100, 50, 100);
        admit(gate, 100);
        assertThat(gate.canIssueTraceId()).isFalse();
        assertThat(gate.canCreateSpan()).isFalse();
    }

    @Test
    void givenPipelineAtCapacity_whenSpanEnds_thenAdmitted() {
        TraceExportPressureGate gate = gate(10, 50, 100);
        admit(gate, 10);
        assertThat(gate.admitEndedSpan()).isTrue();
        assertThat(gate.getInFlight()).isEqualTo(10);
    }

    @Test
    void givenPipelineOverCapacity_whenSpanEnds_thenShedAndSlotReleased() {
        TraceExportPressureGate gate = gate(10, 50, 100);
        admit(gate, 11);
        assertThat(gate.admitEndedSpan()).isFalse();
        assertThat(gate.getInFlight()).isEqualTo(10);
    }

    @Test
    void givenFullQueue_whenExported_thenGatesReopen() {
        TraceExportPressureGate gate = gate(100, 50, 100);
        admit(gate, 100);
        gate.onExported(60);
        assertThat(gate.getInFlight()).isEqualTo(40);
        assertThat(gate.canIssueTraceId()).isTrue();
        assertThat(gate.canCreateSpan()).isTrue();
    }

    @Test
    void givenAdmitExportCycles_whenRepeated_thenInFlightDoesNotDrift() {
        TraceExportPressureGate gate = gate(100, 50, 100);
        for (int i = 0; i < 1000; i++) {
            admit(gate, 20);
            gate.onExported(20);
        }
        assertThat(gate.getInFlight()).isZero();
    }

    @Test
    void givenSampledSpan_whenStarted_thenCountedBeforeItEnds() {
        SpanProcessor delegate = mock(SpanProcessor.class);
        TraceExportPressureGate gate = gate(100, 50, 100);
        PressureAwareSpanProcessor processor = new PressureAwareSpanProcessor(delegate, gate);

        processor.onStart(Context.root(), startedSpan(true));

        assertThat(gate.getInFlight()).isEqualTo(1);
        assertThat(processor.isStartRequired()).isTrue();
    }

    @Test
    void givenStartedSpan_whenItEnds_thenDelegatedAndStillCountedUntilExport() {
        SpanProcessor delegate = mock(SpanProcessor.class);
        TraceExportPressureGate gate = gate(100, 50, 100);
        PressureAwareSpanProcessor processor = new PressureAwareSpanProcessor(delegate, gate);

        processor.onStart(Context.root(), startedSpan(true));
        ReadableSpan ended = sampledSpan(true);
        processor.onEnd(ended);

        verify(delegate).onEnd(ended);
        assertThat(gate.getInFlight()).isEqualTo(1);
        gate.onExported(1);
        assertThat(gate.getInFlight()).isZero();
    }

    @Test
    void givenPipelineOverCapacity_whenSpanEnds_thenShedInsteadOfDelegated() {
        SpanProcessor delegate = mock(SpanProcessor.class);
        TraceExportPressureGate gate = gate(1, 50, 100);
        PressureAwareSpanProcessor processor = new PressureAwareSpanProcessor(delegate, gate);

        processor.onStart(Context.root(), startedSpan(true));
        processor.onStart(Context.root(), startedSpan(true));
        assertThat(gate.getInFlight()).isEqualTo(2);

        ReadableSpan ended = sampledSpan(true);
        processor.onEnd(ended);

        verify(delegate, never()).onEnd(ended);
        assertThat(gate.getInFlight()).isEqualTo(1);
    }

    @Test
    void givenUnsampledSpan_whenStartedAndEnded_thenNeverCounted() {
        SpanProcessor delegate = mock(SpanProcessor.class);
        TraceExportPressureGate gate = gate(100, 50, 100);
        PressureAwareSpanProcessor processor = new PressureAwareSpanProcessor(delegate, gate);

        processor.onStart(Context.root(), startedSpan(false));
        ReadableSpan ended = sampledSpan(false);
        processor.onEnd(ended);

        verify(delegate).onEnd(ended);
        assertThat(gate.getInFlight()).isZero();
    }

    @Test
    void givenZeroSpanThreshold_whenAsked_thenSpanGateAlwaysClosed() {
        TraceExportPressureGate gate = gate(100, 50, 0);
        assertThat(gate.canCreateSpan()).isFalse();
    }

    private static ReadWriteSpan startedSpan(boolean sampled) {
        ReadWriteSpan span = mock(ReadWriteSpan.class);
        when(span.getSpanContext()).thenReturn(spanContext(sampled));
        return span;
    }

    private static ReadableSpan sampledSpan(boolean sampled) {
        ReadableSpan span = mock(ReadableSpan.class);
        when(span.getSpanContext()).thenReturn(spanContext(sampled));
        return span;
    }

    private static SpanContext spanContext(boolean sampled) {
        return SpanContext.create(TRACE_ID, SPAN_ID,
                sampled ? TraceFlags.getSampled() : TraceFlags.getDefault(), TraceState.getDefault());
    }
}
