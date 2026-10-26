// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.trace.MicrometerOpenTelemetryMetricExporter;
import org.thingsboard.server.service.trace.BufferedTraceExporter;
import org.thingsboard.server.service.trace.PressureAwareSpanProcessor;
import org.thingsboard.server.service.trace.TraceConditions;
import org.thingsboard.server.service.trace.TraceExportPressureGate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnExpression(TraceConditions.TRACE_ENABLED)
public class OpenTelemetryConfig {
    private final TbServiceInfoProvider serviceInfoProvider;

    @Value("${otel.tracing.span-limits.max-attribute-value-length:4096}")
    private int maxAttributeValueLength;

    @Value("${otel.processor.trace.batch.schedule-delay-ms:2000}")
    private long postgresScheduleDelayMs;
    @Value("${otel.processor.trace.batch.max-queue-size:100000}")
    private int postgresMaxQueueSize;
    @Value("${otel.processor.trace.batch.max-export-batch-size:10000}")
    private int postgresMaxExportBatchSize;
    @Value("${otel.processor.trace.batch.exporter-timeout-ms:120000}")
    private long postgresExporterTimeoutMs;
    @Value("${otel.processor.trace.metrics.interval-ms:5000}")
    private long postgresMetricsIntervalMs;

    @Value("${otel.exporter.otlp.enabled:false}")
    private boolean otlpEnabled;
    @Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;
    @Value("${otel.exporter.otlp.batch.schedule-delay-ms:2000}")
    private long otlpScheduleDelayMs;
    @Value("${otel.exporter.otlp.batch.max-queue-size:10000}")
    private int otlpMaxQueueSize;
    @Value("${otel.exporter.otlp.batch.max-export-batch-size:10000}")
    private int otlpMaxExportBatchSize;
    @Value("${otel.exporter.otlp.batch.exporter-timeout-ms:10000}")
    private long otlpExporterTimeoutMs;
    @Value("${otel.exporter.otlp.metrics.interval-ms:5000}")
    private long otlpMetricsIntervalMs;

    @Bean
    @ConditionalOnProperty(name = "otel.processor.trace.enabled", havingValue = "true")
    public SdkMeterProvider postgresMeterProvider(MeterRegistry meterRegistry) {
        Gauge.builder("otel.trace.queueMaxSize", () -> postgresMaxQueueSize)
                .tag("processorType", "BatchSpanProcessor")
                .description("Configured BatchSpanProcessor max queue size")
                .register(meterRegistry);
        return SdkMeterProvider.builder()
                .registerMetricReader(
                        PeriodicMetricReader.builder(new MicrometerOpenTelemetryMetricExporter(meterRegistry, "otel.trace."))
                                .setInterval(Duration.ofMillis(postgresMetricsIntervalMs))
                                .build())
                .build();
    }

    @Bean
    @Primary
    public OpenTelemetry customOpenTelemetry(
            BufferedTraceExporter bufferedTraceExporter,
            TraceExportPressureGate pressureGate,
            @Autowired(required = false) SdkMeterProvider postgresMeterProvider) {
        String serviceId = serviceInfoProvider.getServiceId();

        var providerBuilder = SdkTracerProvider.builder()
                .setResource(Resource.getDefault()
                        .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.id"), serviceId))));

        providerBuilder.setSampler(Sampler.parentBased(Sampler.alwaysOff()));
        providerBuilder.setSpanLimits(SpanLimits.builder().setMaxAttributeValueLength(maxAttributeValueLength).build());

        var sdkBuilder = OpenTelemetrySdk.builder();

        if (postgresMeterProvider != null) {
            providerBuilder.addSpanProcessor(new PressureAwareSpanProcessor(
                    BatchSpanProcessor.builder(bufferedTraceExporter)
                            .setMeterProvider(postgresMeterProvider)
                            .setScheduleDelay(postgresScheduleDelayMs, TimeUnit.MILLISECONDS)
                            .setMaxQueueSize(postgresMaxQueueSize)
                            .setMaxExportBatchSize(postgresMaxExportBatchSize)
                            .setExporterTimeout(postgresExporterTimeoutMs, TimeUnit.MILLISECONDS)
                            .build(),
                    pressureGate));
        }
        if (otlpEnabled) {
            MetricExporter metricExporter =
                    OtlpGrpcMetricExporter.builder()
                            .setEndpoint(otlpEndpoint)
                            .build();

            MetricReader reader =
                    PeriodicMetricReader.builder(metricExporter).setInterval(Duration.ofMillis(otlpMetricsIntervalMs)).build();

            SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                    .registerMetricReader(reader)
                    .build();

            sdkBuilder.setMeterProvider(meterProvider);

            OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(otlpEndpoint)
                    .build();

            providerBuilder.addSpanProcessor(
                    BatchSpanProcessor.builder(spanExporter)
                            .setMeterProvider(meterProvider)
                            .setScheduleDelay(otlpScheduleDelayMs, TimeUnit.MILLISECONDS)
                            .setMaxQueueSize(otlpMaxQueueSize)
                            .setMaxExportBatchSize(otlpMaxExportBatchSize)
                            .setExporterTimeout(otlpExporterTimeoutMs, TimeUnit.MILLISECONDS)
                            .build());
        }

        sdkBuilder.setTracerProvider(providerBuilder.build());
        return sdkBuilder.build();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceInfoProvider.getServiceId());
    }
}
