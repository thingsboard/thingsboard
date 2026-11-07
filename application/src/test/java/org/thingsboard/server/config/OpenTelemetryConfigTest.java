// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.trace.BufferedTraceExporter;
import org.thingsboard.server.service.trace.TraceExportPressureGate;

import static org.assertj.core.api.Assertions.assertThat;

class OpenTelemetryConfigTest {

    private static final AttributeKey<String> MSG_DATA = AttributeKey.stringKey("input.msg.data");

    @Test
    void givenMaxAttributeValueLength_whenLongPayloadAttributeIsSet_thenValueIsTruncated() {
        TbServiceInfoProvider serviceInfoProvider = Mockito.mock(TbServiceInfoProvider.class);
        Mockito.when(serviceInfoProvider.getServiceId()).thenReturn("tb-node");
        OpenTelemetryConfig config = new OpenTelemetryConfig(serviceInfoProvider);
        ReflectionTestUtils.setField(config, "maxAttributeValueLength", 4096);

        OpenTelemetry openTelemetry = config.customOpenTelemetry(Mockito.mock(BufferedTraceExporter.class),
                Mockito.mock(TraceExportPressureGate.class), null);

        // The sampler is parent-based, so traced messages always start from a sampled parent.
        SpanContext parent = SpanContext.create("0af7651916cd43dd8448eb211c80319c", "b7ad6b7169203331",
                TraceFlags.getSampled(), TraceState.getDefault());
        Span span = openTelemetry.getTracer("test").spanBuilder("Rule Node")
                .setParent(Context.root().with(Span.wrap(parent)))
                .startSpan();
        span.setAttribute(MSG_DATA, "x".repeat(100_000));

        assertThat(((ReadableSpan) span).getAttribute(MSG_DATA)).hasSize(4096);
        span.end();
    }

}
