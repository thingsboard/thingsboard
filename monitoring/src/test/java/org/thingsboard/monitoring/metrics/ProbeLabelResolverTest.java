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
    public void userInfoInAuthority_isStrippedFromResolvedHost() {
        // exercises the authority.contains("@") branch in resolveHostPort - underscored host still
        // makes URI.getHost() return null, so the fallback must strip "user:pass@" before parsing
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.MQTT, "mqtt://user:pass@tb_mqtt:1883");
        assertThat(labels.endpoint()).isEqualTo("tb_mqtt:1883");
    }

    @Test
    public void schemelessBaseUrl_resolveTransportLabels_returnsNullInsteadOfNullHostLabel() {
        // "acme.example.com:1883" without a scheme parses as an opaque URI with neither a host nor an
        // authority - must not silently produce a "null:1883" endpoint label
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.MQTT, "acme.example.com:1883");
        assertThat(labels).isNull();
    }

    @Test
    public void resolveHostPort_nonNumericPortInAuthority_fallsBackToDefaultPort() {
        // exercises the catch (NumberFormatException) branch - a colon is present in the fallback-
        // parsed authority, but what follows it isn't a valid port number
        assertThat(ProbeLabelResolver.resolveHostPort(URI.create("mqtt://tb_mqtt:notaport"), 1883))
                .isEqualTo("tb_mqtt:1883");
    }

    @Test
    public void resolveHostPort_opaqueUriWithNoAuthority_returnsNull() {
        assertThat(ProbeLabelResolver.resolveHostPort(URI.create("acme.example.com:1883"), 1883)).isNull();
    }

    @Test
    public void resolveHostPort_bareIpv6Authority_keepsWholeAddressAndUsesDefaultPort() {
        // no brackets -> getHost() is null, getAuthority() is the bare literal - don't mangle it
        assertThat(ProbeLabelResolver.resolveHostPort(URI.create("tcp://2001:db8::1:1883"), 1883))
                .isEqualTo("2001:db8::1:1883:1883");
    }

    @Test
    public void resolveHostPort_bracketedIpv6WithPort_resolvedDirectlyByUri_notByFallback() {
        assertThat(ProbeLabelResolver.resolveHostPort(URI.create("tcp://[2001:db8::1]:1883"), 1883))
                .isEqualTo("[2001:db8::1]:1883");
    }

    @Test
    public void resolveTransportLabels_nullBaseUrl_returnsNullInsteadOfThrowing() {
        // URI.create(null) throws NullPointerException, not IllegalArgumentException
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.MQTT, null);
        assertThat(labels).isNull();
    }

    @Test
    public void resolveTransportLabels_malformedBaseUrl_returnsNullInsteadOfThrowing() {
        // unescaped space makes URI.create() throw - must not propagate
        ProbeLabelResolver.ProbeLabels labels =
                ProbeLabelResolver.resolveTransportLabels(TransportType.MQTT, "tcp://tb mqtt:1883");
        assertThat(labels).isNull();
    }

    @Test
    public void resolveLoginEndpoint_appliesLoginPathOnValidUrl() {
        String endpoint = ProbeLabelResolver.resolveLoginEndpoint("https://acme.example.com");
        assertThat(endpoint).isEqualTo("acme.example.com:443/api/auth/login");
    }

    @Test
    public void resolveLoginEndpoint_invalidUrl_returnsNullInsteadOfThrowing() {
        String endpoint = ProbeLabelResolver.resolveLoginEndpoint("not a valid uri");
        assertThat(endpoint).isNull();
    }

    @Test
    public void resolveWsEndpoint_wssScheme_defaultsToPort443() {
        String endpoint = ProbeLabelResolver.resolveWsEndpoint("wss://acme.example.com");
        assertThat(endpoint).isEqualTo("acme.example.com:443");
    }

    @Test
    public void resolveWsEndpoint_plainWsScheme_defaultsToPort80() {
        // the non-secure-scheme branch of tryResolve - previously only exercised via https/wss inputs
        String endpoint = ProbeLabelResolver.resolveWsEndpoint("ws://acme.example.com");
        assertThat(endpoint).isEqualTo("acme.example.com:80");
    }

    @Test
    public void resolveWsEndpoint_invalidUrl_returnsNullInsteadOfThrowing() {
        String endpoint = ProbeLabelResolver.resolveWsEndpoint("not a valid uri");
        assertThat(endpoint).isNull();
    }

    @Test
    public void resolveHostPort_usesDefaultPortWhenUriHasNone() {
        assertThat(ProbeLabelResolver.resolveHostPort(URI.create("http://acme.example.com"), 8080))
                .isEqualTo("acme.example.com:8080");
    }

    @Test
    public void resolveLoginEndpoint_schemelessBaseUrl_returnsNullInsteadOfNullLoginLabel() {
        // "acme.example.com:8080" without a scheme parses as an opaque URI with neither a host nor an
        // authority - resolveHostPort returns null, which tryResolve must not concatenate LOGIN_PATH
        // onto (that would silently produce the literal "null/api/auth/login")
        String endpoint = ProbeLabelResolver.resolveLoginEndpoint("acme.example.com:8080");
        assertThat(endpoint).isNull();
    }

}
