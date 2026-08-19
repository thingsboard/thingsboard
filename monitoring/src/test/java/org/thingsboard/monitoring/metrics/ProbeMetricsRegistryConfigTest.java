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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.service.MonitoringReporter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ProbeMetricsRegistryConfigTest {

    private final ProbeMetricsRegistryConfig config = new ProbeMetricsRegistryConfig();
    private final MonitoringReporter reporter = mock(MonitoringReporter.class);
    private MeterRegistry registry;
    private HttpServer fakeCollector;

    @AfterEach
    public void tearDown() {
        if (registry != null) {
            registry.close(); // stops the OtlpMeterRegistry's publisher thread, if one was created
        }
        config.shutdown(); // stops the Prometheus HttpServer, if one was started
        if (fakeCollector != null) {
            fakeCollector.stop(0);
        }
    }

    private OtlpMeterRegistry otlpRegistryOf(MeterRegistry composite) {
        return (OtlpMeterRegistry) ((CompositeMeterRegistry) composite).getRegistries().iterator().next();
    }

    @Test
    public void whenBothDisabled_registryHasNoChildren() throws IOException {
        registry = config.probeMeterRegistry(false, "http://localhost:4318/v1/metrics", 60000, false,
                false, 19100, "0.0.0.0", reporter);
        assertThat(registry).isInstanceOf(CompositeMeterRegistry.class);
        assertThat(((CompositeMeterRegistry) registry).getRegistries()).isEmpty();
    }

    @Test
    public void whenOtlpEnabled_compositeContainsOtlpRegistry() throws IOException {
        registry = config.probeMeterRegistry(true, "http://localhost:4318/v1/metrics", 60000, false,
                false, 19101, "0.0.0.0", reporter);
        assertThat(((CompositeMeterRegistry) registry).getRegistries())
                .hasOnlyElementsOfType(OtlpMeterRegistry.class);
    }

    @Test
    public void whenPrometheusEnabled_metricsEndpointServesScrapeOutput() throws Exception {
        registry = config.probeMeterRegistry(false, "http://localhost:4318/v1/metrics", 60000, false,
                true, 19102, "0.0.0.0", reporter);
        assertThat(((CompositeMeterRegistry) registry).getRegistries())
                .hasOnlyElementsOfType(PrometheusMeterRegistry.class);

        registry.counter("test_probe_metrics_registry_counter").increment();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:19102/metrics")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("test_probe_metrics_registry_counter");
    }

    @Test
    public void whenBindAddressConfigured_metricsEndpointStillServesOnThatAddress() throws Exception {
        registry = config.probeMeterRegistry(false, "http://localhost:4318/v1/metrics", 60000, false,
                true, 19107, "127.0.0.1", reporter);

        registry.counter("test_probe_metrics_registry_counter").increment();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:19107/metrics")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("test_probe_metrics_registry_counter");
    }

    @Test
    public void otlpAlertingDisabled_pushFailure_neverReportsToReporter() throws IOException {
        // port 1 is not listening - the push is guaranteed to fail
        registry = config.probeMeterRegistry(true, "http://localhost:1/v1/metrics", 60000, false,
                false, 19103, "0.0.0.0", reporter);
        registry.counter("test_counter").increment(); // publish() has nothing to send otherwise

        ReflectionTestUtils.invokeMethod(otlpRegistryOf(registry), "publish");

        verifyNoInteractions(reporter);
    }

    @Test
    public void otlpAlertingEnabled_pushFailure_reportsServiceFailure() throws IOException {
        registry = config.probeMeterRegistry(true, "http://localhost:1/v1/metrics", 60000, true,
                false, 19104, "0.0.0.0", reporter);
        registry.counter("test_counter").increment(); // publish() has nothing to send otherwise

        ReflectionTestUtils.invokeMethod(otlpRegistryOf(registry), "publish");

        verify(reporter).serviceFailure(eq(MonitoredServiceKey.OTLP_EXPORT), any());
    }

    @Test
    public void otlpAlertingEnabled_pushSucceeds_reportsServiceIsOk() throws Exception {
        fakeCollector = HttpServer.create(new InetSocketAddress(19205), 0);
        fakeCollector.createContext("/v1/metrics", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        fakeCollector.start();

        registry = config.probeMeterRegistry(true, "http://localhost:19205/v1/metrics", 60000, true,
                false, 19106, "0.0.0.0", reporter);
        registry.counter("test_counter").increment(); // publish() has nothing to send otherwise

        ReflectionTestUtils.invokeMethod(otlpRegistryOf(registry), "publish");

        verify(reporter).serviceIsOk(MonitoredServiceKey.OTLP_EXPORT);
    }

}
