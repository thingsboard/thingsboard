// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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
            assertThat(gate.admitEndedSpan()).isTrue();
        }
    }

    @Test
    void givenEmptyQueue_whenAsked_thenBothGatesOpen() {
        TraceExportPressureGate gate = gate(128, 50, 100);
        assertThat(gate.canIssueTraceId()).isTrue();
        assertThat(gate.canCreateSpan()).isTrue();
    }

    @Test
    void givenQueueJustBelowHalf_whenAsked_thenTraceIdAllowed() {
        TraceExportPressureGate gate = gate(128, 50, 100);
        admit(gate, 63);
        assertThat(gate.canIssueTraceId()).isTrue();
    }

    @Test
    void givenQueueExactlyHalf_whenAsked_thenTraceIdDeniedButSpanAllowed() {
        TraceExportPressureGate gate = gate(128, 50, 100);
        admit(gate, 64);
        assertThat(gate.canIssueTraceId()).isFalse();
        assertThat(gate.canCreateSpan()).isTrue();
    }

    @Test
    void givenFullQueue_whenAsked_thenBothGatesClosed() {
        TraceExportPressureGate gate = gate(128, 50, 100);
        admit(gate, 128);
        assertThat(gate.canIssueTraceId()).isFalse();
        assertThat(gate.canCreateSpan()).isFalse();
    }

    @Test
    void givenQueueOneBelowCapacity_whenSpanEnds_thenAdmitted() {
        TraceExportPressureGate gate = gate(16, 50, 100);
        admit(gate, 15);
        assertThat(gate.admitEndedSpan()).isTrue();
        assertThat(gate.getInFlight()).isEqualTo(16);
    }

    @Test
    void givenFullQueue_whenSpanEnds_thenShedAndCountUnchanged() {
        TraceExportPressureGate gate = gate(16, 50, 100);
        admit(gate, 16);
        assertThat(gate.admitEndedSpan()).isFalse();
        assertThat(gate.getInFlight()).isEqualTo(16);
    }

    @Test
    void givenFullQueue_whenExported_thenGatesReopen() {
        TraceExportPressureGate gate = gate(128, 50, 100);
        admit(gate, 128);
        gate.onExported(80);
        assertThat(gate.getInFlight()).isEqualTo(48);
        assertThat(gate.canIssueTraceId()).isTrue();
        assertThat(gate.canCreateSpan()).isTrue();
    }

    @Test
    void givenAdmitExportCycles_whenRepeated_thenInFlightDoesNotDrift() {
        TraceExportPressureGate gate = gate(128, 50, 100);
        for (int i = 0; i < 1000; i++) {
            admit(gate, 20);
            gate.onExported(20);
        }
        assertThat(gate.getInFlight()).isZero();
    }

    @Test
    void givenSampledSpan_whenStarted_thenNotCountedUntilItEnds() {
        SpanProcessor delegate = mock(SpanProcessor.class);
        TraceExportPressureGate gate = gate(128, 50, 100);
        PressureAwareSpanProcessor processor = new PressureAwareSpanProcessor(delegate, gate);

        ReadWriteSpan started = startedSpan(true);
        processor.onStart(Context.root(), started);

        verify(delegate).onStart(Context.root(), started);
        assertThat(gate.getInFlight()).isZero();
    }

    @Test
    void givenSpansStartedButNeverEnded_whenManyExceedCapacity_thenGatesStayOpen() {
        SpanProcessor delegate = mock(SpanProcessor.class);
        TraceExportPressureGate gate = gate(128, 50, 100);
        PressureAwareSpanProcessor processor = new PressureAwareSpanProcessor(delegate, gate);

        for (int i = 0; i < 1000; i++) {
            processor.onStart(Context.root(), startedSpan(true));
        }

        assertThat(gate.getInFlight()).isZero();
        assertThat(gate.canIssueTraceId()).isTrue();
        assertThat(gate.canCreateSpan()).isTrue();
    }

    @Test
    void givenStartedSpan_whenItEnds_thenDelegatedAndCountedUntilExport() {
        SpanProcessor delegate = mock(SpanProcessor.class);
        TraceExportPressureGate gate = gate(128, 50, 100);
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

        ReadableSpan first = sampledSpan(true);
        processor.onEnd(first);
        assertThat(gate.getInFlight()).isEqualTo(1);

        ReadableSpan ended = sampledSpan(true);
        processor.onEnd(ended);

        verify(delegate).onEnd(first);
        verify(delegate, never()).onEnd(ended);
        assertThat(gate.getInFlight()).isEqualTo(1);
    }

    @Test
    void givenUnsampledSpan_whenStartedAndEnded_thenNeverCounted() {
        SpanProcessor delegate = mock(SpanProcessor.class);
        TraceExportPressureGate gate = gate(128, 50, 100);
        PressureAwareSpanProcessor processor = new PressureAwareSpanProcessor(delegate, gate);

        processor.onStart(Context.root(), startedSpan(false));
        ReadableSpan ended = sampledSpan(false);
        processor.onEnd(ended);

        verify(delegate).onEnd(ended);
        assertThat(gate.getInFlight()).isZero();
    }

    @Test
    void givenZeroSpanThreshold_whenAsked_thenSpanGateAlwaysClosed() {
        TraceExportPressureGate gate = gate(128, 50, 0);
        assertThat(gate.canCreateSpan()).isFalse();
    }

    @Test
    void givenQueueSizeNotPowerOfTwo_whenCreated_thenRoundedUpLikeOpenTelemetry() {
        TraceExportPressureGate gate = gate(100, 50, 100);
        assertThat(gate.getMaxQueueSize()).isEqualTo(128);
        admit(gate, 63);
        assertThat(gate.canIssueTraceId()).isTrue();
        admit(gate, 1);
        assertThat(gate.canIssueTraceId()).isFalse();

        assertThat(gate(100000, 50, 100).getMaxQueueSize()).isEqualTo(131072);
        assertThat(gate(131072, 50, 100).getMaxQueueSize()).isEqualTo(131072);
        assertThat(gate(1, 50, 100).getMaxQueueSize()).isEqualTo(1);
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
