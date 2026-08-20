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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.monitoring.config.transport.TransportInfo;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.data.MonitoredServiceKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ProbeMetricsRecorderTest {

    private SimpleMeterRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new SimpleMeterRegistry();
    }

    private ProbeMetricsRecorder recorder(boolean otlpEnabled) {
        // named locals, not adjacent positional literals, so a future constructor param reorder can't
        // silently transpose otlpEnabled/prometheusEnabled here without a compile error
        boolean prometheusEnabled = false;
        return new ProbeMetricsRecorder(registry, otlpEnabled, prometheusEnabled, "acme.example.com",
                "https://acme.example.com", "wss://acme.example.com", "acme-cluster-1");
    }

    private TransportInfo transportInfo(TransportType type, String baseUrl) {
        return transportInfo(type, baseUrl, null);
    }

    private TransportInfo transportInfo(TransportType type, String baseUrl, String queue) {
        TransportMonitoringTarget target = new TransportMonitoringTarget();
        target.setBaseUrl(baseUrl);
        target.setQueue(queue);
        return new TransportInfo(type, target);
    }

    @Test
    public void whenDisabled_noMetersRegistered() {
        ProbeMetricsRecorder recorder = recorder(false);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), true);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void mqttPlain_mapsToMqttProtocolAndConfiguredPort() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), true);

        assertThat(registry.get("probe_success")
                .tags("domain", "acme.example.com", "check", "mqtt", "endpoint", "acme.example.com:1883", "kind", "probe")
                .gauge().value()).isEqualTo(1d);
    }

    @Test
    public void mqttTls_mapsToMqttsProtocol() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "ssl://acme.example.com:8883"), true);

        assertThat(registry.get("probe_success").tags("check", "mqtts").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void coapPlain_mapsToCoapProtocol_defaultPortWhenMissing() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.COAP, "coap://acme.example.com"), true);

        assertThat(registry.get("probe_success")
                .tags("check", "coap", "endpoint", "acme.example.com:5683").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void coapSecure_mapsToCoapsProtocol() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.COAP, "coaps://acme.example.com:5684"), true);

        assertThat(registry.get("probe_success").tags("check", "coaps").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void http_mapsToHttpOrHttpsByScheme() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.HTTP, "http://acme.example.com"), true);
        recorder.recordProbe(transportInfo(TransportType.HTTP, "https://acme.example.com"), true);

        assertThat(registry.get("probe_success").tags("check", "http", "endpoint", "acme.example.com:80").gauge().value()).isEqualTo(1d);
        assertThat(registry.get("probe_success").tags("check", "https", "endpoint", "acme.example.com:443").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void lwm2m_alwaysMapsToLwm2mRegardlessOfCoapScheme() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.LWM2M, "coap://acme.example.com:5685"), true);

        assertThat(registry.get("probe_success")
                .tags("check", "lwm2m", "endpoint", "acme.example.com:5685").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void failedProbe_recordsZeroSuccess() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), false);

        assertThat(registry.get("probe_success").tags("check", "mqtt").gauge().value()).isEqualTo(0d);
    }

    @Test
    public void login_mapsToLoginProtocolWithHostPortApiPathEndpointFromRestBaseUrl() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(MonitoredServiceKey.LOGIN, true);

        assertThat(registry.get("probe_success")
                .tags("check", "login", "endpoint", "acme.example.com:443/api/auth/login").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void ws_mapsToWsProtocolWithHostPortEndpointFromWsBaseUrl() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(MonitoredServiceKey.WS, true);

        assertThat(registry.get("probe_success")
                .tags("check", "ws", "endpoint", "acme.example.com:443").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void unrecognizedServiceKey_isIgnored() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(MonitoredServiceKey.GENERAL, true);
        recorder.recordProbe(MonitoredServiceKey.EDQS, true);

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void secondCallWithSameLabels_updatesExistingGaugeInPlace() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), true);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), false);

        assertThat(registry.get("probe_success").tags("check", "mqtt").gauge().value()).isEqualTo(0d);
        assertThat(registry.getMeters()).hasSize(1); // no duplicate meter registered
    }

    @Test
    public void recordActionDuration_addsStageTagToSeparateSeries() {
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");

        recorder.recordActionDuration(target, "request", 8);
        recorder.recordActionDuration(target, "ws_update", 700);

        assertThat(registry.get("probe_duration_ms")
                .tags("check", "mqtt", "action", "request").gauge().value()).isEqualTo(8d);
        assertThat(registry.get("probe_duration_ms")
                .tags("check", "mqtt", "action", "ws_update").gauge().value()).isEqualTo(700d);
        assertThat(registry.getMeters()).hasSize(2); // one series per stage, correctly distinct
    }

    @Test
    public void recordActionDuration_whenDisabled_isNoOp() {
        ProbeMetricsRecorder recorder = recorder(false);
        recorder.recordActionDuration(MonitoredServiceKey.LOGIN, "request", 8);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void removeActionDuration_removesOnlyTheTargetedAction_leavesSuccessAndOtherActionDurationUntouched() {
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordProbe(target, true);
        recorder.recordActionDuration(target, "request", 8);
        recorder.recordActionDuration(target, "ws_update", 700);
        assertThat(registry.getMeters()).hasSize(3); // success + 2 stages

        recorder.startCycle(); // next cycle - this target's fresh-this-cycle protection no longer applies
        recorder.removeActionDuration(target, "request");

        assertThat(registry.find("probe_duration_ms").tags("action", "request").gauge()).isNull();
        assertThat(registry.get("probe_duration_ms").tags("action", "ws_update").gauge().value()).isEqualTo(700d);
        assertThat(registry.get("probe_success").tags("check", "mqtt").gauge().value()).isEqualTo(1d);
        assertThat(registry.getMeters()).hasSize(2);
    }

    @Test
    public void removeActionDuration_ofCollidingSibling_doesNotWipeFreshDurationRecordedThisCycle() {
        // A and B collide onto identical tags (same type+baseUrl, differing only by queue). If A
        // succeeds and records a fresh "request" duration this cycle, and B's own removeActionDuration
        // fires afterward (B's sendTestPayload threw), A's just-recorded duration must survive.
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo a = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883", "QueueA");
        TransportInfo b = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883", "QueueB");

        recorder.recordProbe(a, true);
        recorder.recordActionDuration(a, "request", 8);
        recorder.removeActionDuration(b, "request");

        assertThat(registry.get("probe_duration_ms").tags("action", "request").gauge().value()).isEqualTo(8d);
    }

    @Test
    public void removeStaleProbe_alsoRemovesStageDurationGauges() {
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordProbe(target, true);
        recorder.recordActionDuration(target, "request", 8);
        recorder.recordActionDuration(target, "ws_update", 700);
        assertThat(registry.getMeters()).hasSize(3); // success + 2 stages

        recorder.startCycle(); // next cycle - this target's fresh-this-cycle protection no longer applies
        recorder.removeStaleProbe(target);

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void removeProbe_thenRecordStageDurationAgain_reregistersCleanly() {
        // guards against the internal stagesByBaseTags bookkeeping leaking a stale entry that
        // would make removeProbe miss this series on a later, second removal
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordActionDuration(target, "request", 8);
        recorder.removeProbe(target);

        recorder.recordActionDuration(target, "request", 12);

        assertThat(registry.get("probe_duration_ms")
                .tags("check", "mqtt", "action", "request").gauge().value()).isEqualTo(12d);
        assertThat(registry.getMeters()).hasSize(1);

        recorder.removeProbe(target);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void removeProbe_removesGaugeForRetiredTarget() {
        // decommissioning (e.g. reconcileAssociates dropping a domain-IP associate) uses the plain,
        // unguarded removeProbe - it must remove the gauge unconditionally, even if the target's tags
        // got fresh data THIS SAME cycle (e.g. a successful check() just before being decommissioned)
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo retired = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordProbe(retired, true);
        assertThat(registry.getMeters()).hasSize(1);

        recorder.removeProbe(retired);

        assertThat(registry.find("probe_success").tags("check", "mqtt").gauge()).isNull();
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void removeStaleProbe_thenRecordProbeAgain_reregistersGaugeCleanly() {
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordProbe(target, true);
        recorder.startCycle(); // next cycle - this target's fresh-this-cycle protection no longer applies
        recorder.removeStaleProbe(target);

        recorder.recordProbe(target, false);

        assertThat(registry.get("probe_success").tags("check", "mqtt").gauge().value()).isEqualTo(0d);
        assertThat(registry.getMeters()).hasSize(1); // no duplicate/orphaned meter left over from before removal
    }

    @Test
    public void removeProbe_whenDisabled_isNoOp() {
        ProbeMetricsRecorder recorder = recorder(false);
        recorder.removeProbe(MonitoredServiceKey.LOGIN);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void underscoreHostname_stillResolvesEndpoint() {
        // URI.getHost()/getPort() return null/-1 for authorities Java doesn't treat as valid hostnames -
        // e.g. underscores, a common docker-compose service-naming convention
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://tb_mqtt:1883"), true);

        assertThat(registry.get("probe_success")
                .tags("check", "mqtt", "endpoint", "tb_mqtt:1883").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void underscoreHostname_noPort_fallsBackToDefaultPort() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.COAP, "coap://tb_coap"), true);

        assertThat(registry.get("probe_success")
                .tags("check", "coap", "endpoint", "tb_coap:5683").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void removeProbe_withFreshButEqualTransportInfo_stillRemovesGauge() {
        // recordProbe/removeProbe are called with independently-constructed TransportInfo instances in
        // production (BaseHealthChecker's cached field vs. a fresh BaseHealthChecker.getInfo() call) -
        // only their type+baseUrl need to match, not object identity. This is the decommissioning path
        // (reconcileAssociates uses getCachedInfo()), so it's unguarded and must remove unconditionally,
        // even though the tags are still fresh from the recordProbe call moments earlier this cycle.
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), true);

        recorder.removeProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"));

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void distinctServiceKeysWithSameLabels_lastWriteWins() {
        // two targets sharing a host:port but differing only by queue (which isn't part of the label
        // taxonomy) collide onto the same series - documents the known, logged limitation rather than
        // silently losing one target's data without a trace
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo first = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883", "QueueA");
        TransportInfo second = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883", "QueueB");
        assertThat(first).isNotEqualTo(second); // otherwise this test wouldn't actually exercise a collision

        recorder.recordProbe(first, true);
        recorder.recordProbe(second, false);

        assertThat(registry.get("probe_success").tags("check", "mqtt").gauge().value()).isEqualTo(0d);
        assertThat(registry.getMeters()).hasSize(1); // still one series, not two - the collision is real
    }

    @Test
    public void removeStaleProbe_ofCollidingSibling_doesNotWipeFreshDataRecordedThisCycle() {
        // A and B collide onto identical tags (same type+baseUrl, differing only by queue). If A
        // succeeds and records fresh data this cycle, and B's own removeStaleProbe fires afterward
        // (e.g. because B crashed and checkedCount-based clearing targets it), A's fresh data must
        // survive - otherwise a crashing sibling silently wipes a healthy target's just-recorded gauge.
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo a = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883", "QueueA");
        TransportInfo b = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883", "QueueB");

        recorder.recordProbe(a, true);
        recorder.removeStaleProbe(b);

        assertThat(registry.get("probe_success").tags("check", "mqtt").gauge().value()).isEqualTo(1d);
        assertThat(registry.getMeters()).hasSize(1);
    }

    @Test
    public void removeStaleAcceptedProbe_ofCollidingSibling_doesNotWipeFreshDataRecordedThisCycle() {
        // same collision protection, for the accepted-fallback series
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo a = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883", "QueueA");
        TransportInfo b = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883", "QueueB");

        recorder.recordAcceptedProbe(a, true);
        recorder.removeStaleAcceptedProbe(b);

        assertThat(registry.get("probe_success").tags("check", "mqtt", "kind", "accepted").gauge().value()).isEqualTo(1d);
        assertThat(registry.getMeters()).hasSize(1);
    }

    @Test
    public void removeStaleProbe_withoutPriorRecordThisCycle_stillRemovesGauge() {
        // normal, non-colliding stale removal - freshThisCycle is empty (either nothing was recorded,
        // or startCycle() reset it), so removal must proceed exactly as before this fix
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordProbe(target, true);
        recorder.startCycle();

        recorder.removeStaleProbe(target);

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void disabled_invalidWsBaseUrl_doesNotThrowAtConstruction() {
        boolean otlpEnabled = false;
        boolean prometheusEnabled = false;
        ProbeMetricsRecorder recorder = new ProbeMetricsRecorder(registry, otlpEnabled, prometheusEnabled, "acme.example.com",
                "https://acme.example.com", "not a valid uri", "acme-cluster-1");
        recorder.recordProbe(MonitoredServiceKey.WS, true);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void enabled_invalidWsBaseUrl_doesNotThrowAtConstruction_wsProbeSkipped() {
        // otlpEnabled=true so this recorder is "enabled" and would normally resolve wsEndpoint eagerly
        boolean otlpEnabled = true;
        boolean prometheusEnabled = false;
        ProbeMetricsRecorder recorder = new ProbeMetricsRecorder(registry, otlpEnabled, prometheusEnabled, "acme.example.com",
                "https://acme.example.com", "not a valid uri", "acme-cluster-1");

        recorder.recordProbe(MonitoredServiceKey.WS, true);

        assertThat(registry.getMeters()).isEmpty(); // ws is skipped, but construction didn't throw
    }

    @Test
    public void enabled_invalidRestBaseUrl_doesNotThrowAtConstruction_loginProbeSkipped() {
        boolean otlpEnabled = true;
        boolean prometheusEnabled = false;
        ProbeMetricsRecorder recorder = new ProbeMetricsRecorder(registry, otlpEnabled, prometheusEnabled, "acme.example.com",
                "not a valid uri", "wss://acme.example.com", "acme-cluster-1");

        recorder.recordProbe(MonitoredServiceKey.LOGIN, true);

        assertThat(registry.getMeters()).isEmpty(); // login is skipped, but construction didn't throw
    }

    @Test
    public void labelTag_appearsOnGeneratedMetrics() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), true);

        assertThat(registry.get("probe_success")
                .tags("domain", "acme.example.com", "check", "mqtt", "label", "acme-cluster-1")
                .gauge().value()).isEqualTo(1d);
    }

    @Test
    public void emptyLabel_stillProducesEmptyStringTag() {
        // as-is behavior, same as the other optional tags in this class - an unset env var
        // becomes an empty string tag rather than omitting the label tag entirely
        boolean otlpEnabled = true;
        boolean prometheusEnabled = false;
        ProbeMetricsRecorder recorder = new ProbeMetricsRecorder(registry, otlpEnabled, prometheusEnabled, "acme.example.com",
                "https://acme.example.com", "wss://acme.example.com", "");
        recorder.recordProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), true);

        assertThat(registry.get("probe_success").tags("label", "").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void removeAcceptedProbe_whenNothingWasRecorded_skipsRegistryScan() {
        // runs every healthy cycle now - must not scan the whole registry when there's nothing to remove
        SimpleMeterRegistry spyRegistry = spy(new SimpleMeterRegistry());
        boolean otlpEnabled = true;
        boolean prometheusEnabled = false;
        ProbeMetricsRecorder recorder = new ProbeMetricsRecorder(spyRegistry, otlpEnabled, prometheusEnabled, "acme.example.com",
                "https://acme.example.com", "wss://acme.example.com", "acme-cluster-1");

        recorder.removeAcceptedProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"));

        verify(spyRegistry, never()).find(anyString());
    }

    @Test
    public void recordAcceptedProbe_recordsSeparateSeriesFromE2eProbe() {
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordProbe(target, true);
        recorder.recordAcceptedProbe(target, false);

        assertThat(registry.get("probe_success").tags("kind", "probe").gauge().value()).isEqualTo(1d);
        assertThat(registry.get("probe_success").tags("kind", "accepted").gauge().value()).isEqualTo(0d);
        assertThat(registry.getMeters()).hasSize(2); // two distinct series, no collision
    }

    @Test
    public void recordAcceptedProbe_usesSameLabelResolutionAsE2eProbe() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordAcceptedProbe(transportInfo(TransportType.MQTT, "ssl://acme.example.com:8883"), true);

        assertThat(registry.get("probe_success")
                .tags("domain", "acme.example.com", "check", "mqtts", "endpoint", "acme.example.com:8883",
                        "kind", "accepted", "label", "acme-cluster-1")
                .gauge().value()).isEqualTo(1d);
    }

    @Test
    public void recordAcceptedProbe_whenDisabled_isNoOp() {
        ProbeMetricsRecorder recorder = recorder(false);
        recorder.recordAcceptedProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"), true);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void recordAcceptedProbe_ignoresNonTransportServiceKeys() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.recordAcceptedProbe(MonitoredServiceKey.LOGIN, true);
        recorder.recordAcceptedProbe(MonitoredServiceKey.WS, true);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void removeStaleProbe_noLongerRemovesAcceptedGauge() {
        // removeStaleProbe() no longer touches kind="accepted" - only the accepted variants do
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordProbe(target, true);
        recorder.recordAcceptedProbe(target, true);
        assertThat(registry.getMeters()).hasSize(2);

        recorder.startCycle(); // next cycle - this target's fresh-this-cycle protection no longer applies
        recorder.removeStaleProbe(target);

        assertThat(registry.getMeters()).hasSize(1);
        assertThat(registry.get("probe_success").tags("kind", "accepted").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void removeStaleAcceptedProbe_removesOnlyAcceptedGauge_leavesProbeGaugeUntouched() {
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883");
        recorder.recordProbe(target, true);
        recorder.recordAcceptedProbe(target, true);
        assertThat(registry.getMeters()).hasSize(2);

        recorder.startCycle(); // next cycle - this target's fresh-this-cycle protection no longer applies
        recorder.removeStaleAcceptedProbe(target);

        assertThat(registry.getMeters()).hasSize(1);
        assertThat(registry.get("probe_success").tags("kind", "probe").gauge().value()).isEqualTo(1d);
    }

    @Test
    public void removeAcceptedProbe_whenDisabled_isNoOp() {
        ProbeMetricsRecorder recorder = recorder(false);
        recorder.removeAcceptedProbe(transportInfo(TransportType.MQTT, "tcp://acme.example.com:1883"));
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void removeAcceptedProbe_ignoresNonTransportServiceKeys() {
        ProbeMetricsRecorder recorder = recorder(true);
        recorder.removeAcceptedProbe(MonitoredServiceKey.LOGIN);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void recordHeartbeat_enabled_registersCurrentTimestamp() {
        ProbeMetricsRecorder recorder = recorder(true);

        recorder.recordHeartbeat();

        double value = registry.get("tb_monitoring_last_run_timestamp_seconds")
                .tags("domain", "acme.example.com", "label", "acme-cluster-1").gauge().value();
        assertThat(value).isCloseTo(System.currentTimeMillis() / 1000.0, within(5.0));
    }

    @Test
    public void recordHeartbeat_disabled_registersNothing() {
        ProbeMetricsRecorder recorder = recorder(false);

        recorder.recordHeartbeat();

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void schemelessTransportBaseUrl_recordingMethodsDoNotThrowAndRegisterNoMeters() {
        // "acme.example.com:1883" without a scheme parses as opaque - neither a host nor an authority -
        // so resolveTransportLabels returns null; every recording method must treat that as "skip this
        // probe" rather than throwing or registering a "null:1883"-style gauge
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "acme.example.com:1883");

        recorder.recordProbe(target, true);
        recorder.recordActionDuration(target, "request", 8);
        recorder.recordAcceptedProbe(target, true);
        recorder.removeAcceptedProbe(target);

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void schemelessTransportBaseUrl_secondCallForSameTarget_alsoDoesNotThrow() {
        // the negative resolution is cached (an Optional.empty(), not a bare null computeIfAbsent
        // would skip caching), so resolveTransportLabels only actually runs once for this target -
        // this confirms the cached path stays side-effect-free on every later call too
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "acme.example.com:1883");

        recorder.recordProbe(target, true);
        recorder.recordProbe(target, true);

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    public void schemelessTransportBaseUrl_removedThenReAdded_warnsAgain() {
        // removeProbe evicts warnedUnresolvable for the retired target, so a target decommissioned
        // and later re-added with the same bad URL gets a fresh warning instead of permanent silence.
        // No log-capturing utility exists here, so this only confirms re-adding stays side-effect-free
        // (the actual re-warning is covered by inspection of removeProbe's warnedUnresolvable.remove).
        ProbeMetricsRecorder recorder = recorder(true);
        TransportInfo target = transportInfo(TransportType.MQTT, "acme.example.com:1883");

        recorder.recordProbe(target, true);
        recorder.removeProbe(target);
        recorder.recordProbe(target, true);

        assertThat(registry.getMeters()).isEmpty();
    }

}
