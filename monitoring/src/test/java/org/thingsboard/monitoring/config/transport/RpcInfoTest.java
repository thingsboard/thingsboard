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
package org.thingsboard.monitoring.config.transport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RpcInfoTest {

    @Test
    void shortNameAppendsRpcForDefaultQueue() {
        RpcInfo rpc = new RpcInfo(transportInfo("Main"));

        assertThat(rpc.getShortName()).isEqualTo("MQTT RPC");
    }

    @Test
    void shortNameAppendsRpcForCustomQueue() {
        RpcInfo rpc = new RpcInfo(transportInfo("Foo"));

        assertThat(rpc.getShortName()).isEqualTo("MQTT Foo RPC");
    }

    @Test
    void toStringIncludesUrlAndRpcMarker() {
        RpcInfo rpc = new RpcInfo(transportInfo("Main"));

        assertThat(rpc.toString())
                .contains("*MQTT*")
                .contains("(tcp://host:1883)")
                .endsWith(RpcInfo.RPC_SUFFIX);
    }

    @Test
    void equalsAndHashCodeAreStableAcrossInstances() {
        RpcInfo a = new RpcInfo(transportInfo("Main"));
        RpcInfo b = new RpcInfo(transportInfo("Main"));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentQueuesProduceDistinctKeys() {
        RpcInfo a = new RpcInfo(transportInfo("Main"));
        RpcInfo b = new RpcInfo(transportInfo("Foo"));

        assertThat(a).isNotEqualTo(b);
    }

    // IncidentManager dedups by toString() — RpcInfo for a target must produce a
    // different incident row than TransportInfo for the same target so the RPC
    // row and the telemetry row don't collapse into one.
    @Test
    void rpcInfoIncidentKeyDiffersFromTelemetryInfoForSameTarget() {
        TransportInfo telemetry = transportInfo("Main");
        RpcInfo rpc = new RpcInfo(telemetry);

        assertThat(rpc.toString()).isNotEqualTo(telemetry.toString());
        assertThat(rpc).isNotEqualTo(telemetry);
    }

    private static TransportInfo transportInfo(String queue) {
        TransportMonitoringTarget target = new TransportMonitoringTarget();
        target.setBaseUrl("tcp://host:1883");
        target.setQueue(queue);
        return new TransportInfo(TransportType.MQTT, target);
    }

}
