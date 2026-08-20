/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.monitoring.metrics;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpHttpMetricsSender;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.service.MonitoringReporter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Slf4j
public class ProbeMetricsRegistryConfig {

    // small fixed pool for the /metrics scrape endpoint - PrometheusMeterRegistry.scrape() is thread-safe
    private static final int PROMETHEUS_SCRAPE_THREAD_POOL_SIZE = 4;

    static {
        // sun.net.httpserver.ServerConfig reads these once, on the JVM's first HttpServer use - must be
        // set before that (a static initializer, not @Value, since Spring isn't up yet at that point)
        String timeoutS = System.getenv().getOrDefault("METRICS_PROMETHEUS_HTTP_TIMEOUT_S", "30");
        System.setProperty("sun.net.httpserver.maxReqTime", timeoutS);
        System.setProperty("sun.net.httpserver.maxRspTime", timeoutS);
    }

    private HttpServer prometheusServer;
    private ExecutorService prometheusExecutor;

    @Bean
    public MeterRegistry probeMeterRegistry(
            @Value("${monitoring.metrics.otlp.enabled:false}") boolean otlpEnabled,
            @Value("${monitoring.metrics.otlp.endpoint:http://localhost:4318/v1/metrics}") String otlpEndpoint,
            @Value("${monitoring.metrics.otlp.step_ms:60000}") long otlpStepMs,
            @Value("${monitoring.metrics.otlp.alerting_enabled:false}") boolean otlpAlertingEnabled,
            @Value("${monitoring.metrics.prometheus.enabled:false}") boolean prometheusEnabled,
            @Value("${monitoring.metrics.prometheus.port:9100}") int prometheusPort,
            @Value("${monitoring.metrics.prometheus.bind_address:0.0.0.0}") String prometheusBindAddress,
            MonitoringReporter reporter) throws IOException {
        CompositeMeterRegistry composite = new CompositeMeterRegistry();
        try {
            if (otlpEnabled) {
                composite.add(createOtlpRegistry(otlpEndpoint, otlpStepMs, otlpAlertingEnabled, reporter));
                log.info("Probe metrics: OTLP export enabled, pushing to {}", otlpEndpoint);
            }
            if (prometheusEnabled) {
                composite.add(createPrometheusRegistry(prometheusPort, prometheusBindAddress));
                log.info("Probe metrics: Prometheus scrape endpoint enabled on port {}", prometheusPort);
            }
        } catch (Exception e) {
            // an already-started OTLP registry's internal publish thread would otherwise leak and
            // block JVM exit, defeating the intended "fail loud" crash on Prometheus bind failure
            composite.close();
            throw e;
        }
        return composite;
    }

    private OtlpMeterRegistry createOtlpRegistry(String endpoint, long stepMs, boolean alertingEnabled, MonitoringReporter reporter) {
        OtlpConfig otlpConfig = new OtlpConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String url() {
                return endpoint;
            }

            @Override
            public Duration step() {
                return Duration.ofMillis(stepMs);
            }

            @Override
            public Map<String, String> resourceAttributes() {
                Map<String, String> attributes = new HashMap<>(OtlpConfig.super.resourceAttributes());
                attributes.putIfAbsent("service.name", "tb-monitoring");
                return attributes;
            }
        };
        OtlpMetricsSender sender = new OtlpHttpMetricsSender(new HttpUrlConnectionSender());
        if (alertingEnabled) {
            sender = withAlerting(sender, reporter);
        }
        return OtlpMeterRegistry.builder(otlpConfig).clock(Clock.SYSTEM).metricsSender(sender).build();
    }

    // alerts on real OTLP push failures via Slack/incident, not as a probe metric - a broken
    // metrics pipe can't be expected to report its own breakage
    private static OtlpMetricsSender withAlerting(OtlpMetricsSender delegate, MonitoringReporter reporter) {
        return request -> {
            try {
                delegate.send(request);
                reporter.serviceIsOk(MonitoredServiceKey.OTLP_EXPORT);
            } catch (Exception e) {
                reporter.serviceFailure(MonitoredServiceKey.OTLP_EXPORT, e);
                throw e; // preserve OtlpMeterRegistry's own "Failed to publish metrics" warning log
            }
        };
    }

    private PrometheusMeterRegistry createPrometheusRegistry(int port, String bindAddress) throws IOException {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheusServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        prometheusServer.createContext("/metrics", exchange -> {
            byte[] response = registry.scrape().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        prometheusExecutor = Executors.newFixedThreadPool(PROMETHEUS_SCRAPE_THREAD_POOL_SIZE);
        prometheusServer.setExecutor(prometheusExecutor);
        prometheusServer.start();
        return registry;
    }

    @PreDestroy
    public void shutdown() {
        if (prometheusServer != null) {
            prometheusServer.stop(0);
        }
        if (prometheusExecutor != null) {
            // HttpServer.stop() doesn't shut down a caller-supplied executor - we own its lifecycle
            prometheusExecutor.shutdownNow();
        }
    }

}
