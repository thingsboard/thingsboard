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

import org.junit.jupiter.api.Test;
import org.thingsboard.monitoring.config.transport.TransportType;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class ProbeLabelResolverTest {

    @Test
    public void mqttPlain_mapsToMqttCheckAndConfiguredPort() {
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.MQTT, "tcp://acme.example.com:1883");
        assertThat(labels.check()).isEqualTo("mqtt");
        assertThat(labels.endpoint()).isEqualTo("acme.example.com:1883");
    }

    @Test
    public void mqttTls_mapsToMqttsCheck() {
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.MQTT, "ssl://acme.example.com:8883");
        assertThat(labels.check()).isEqualTo("mqtts");
    }

    @Test
    public void coapPlain_defaultPortWhenMissing() {
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.COAP, "coap://acme.example.com");
        assertThat(labels.check()).isEqualTo("coap");
        assertThat(labels.endpoint()).isEqualTo("acme.example.com:5683");
    }

    @Test
    public void coapSecure_mapsToCoapsCheck() {
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.COAP, "coaps://acme.example.com:5684");
        assertThat(labels.check()).isEqualTo("coaps");
    }

    @Test
    public void httpAndHttps_mapByScheme() {
        assertThat(ProbeLabelResolver.resolveTransportLabels(TransportType.HTTP, "http://acme.example.com").check())
                .isEqualTo("http");
        assertThat(ProbeLabelResolver.resolveTransportLabels(TransportType.HTTP, "https://acme.example.com").check())
                .isEqualTo("https");
    }

    @Test
    public void lwm2m_alwaysMapsToLwm2mRegardlessOfCoapScheme() {
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.LWM2M, "coap://acme.example.com:5685");
        assertThat(labels.check()).isEqualTo("lwm2m");
        assertThat(labels.endpoint()).isEqualTo("acme.example.com:5685");
    }

    @Test
    public void underscoreHostname_stillResolvesEndpoint() {
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.MQTT, "tcp://tb_mqtt:1883");
        assertThat(labels.endpoint()).isEqualTo("tb_mqtt:1883");
    }

    @Test
    public void underscoreHostname_noPort_fallsBackToDefaultPort() {
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.COAP, "coap://tb_coap");
        assertThat(labels.endpoint()).isEqualTo("tb_coap:5683");
    }

    @Test
    public void tryResolveEndpoint_appliesPostProcessOnValidUrl() {
        String endpoint = ProbeLabelResolver.tryResolveEndpoint("some.config.key", "https://acme.example.com",
                "login", s -> s + ProbeLabelResolver.LOGIN_PATH);
        assertThat(endpoint).isEqualTo("acme.example.com:443/api/auth/login");
    }

    @Test
    public void tryResolveEndpoint_wsScheme_defaultsToPort443ForWss() {
        String endpoint = ProbeLabelResolver.tryResolveEndpoint("monitoring.ws.base_url", "wss://acme.example.com",
                "ws", java.util.function.Function.identity());
        assertThat(endpoint).isEqualTo("acme.example.com:443");
    }

    @Test
    public void tryResolveEndpoint_invalidUrl_returnsNullInsteadOfThrowing() {
        String endpoint = ProbeLabelResolver.tryResolveEndpoint("some.config.key", "not a valid uri",
                "login", java.util.function.Function.identity());
        assertThat(endpoint).isNull();
    }

    @Test
    public void resolveHostPort_usesDefaultPortWhenUriHasNone() {
        assertThat(ProbeLabelResolver.resolveHostPort(URI.create("http://acme.example.com"), 8080))
                .isEqualTo("acme.example.com:8080");
    }

}