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

class RpcCheckConfigTest {

    @Test
    void defaultsAreDisabledAndUnset() {
        RpcCheckConfig config = new RpcCheckConfig();

        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getRequestTimeoutMs()).isNull();
    }

    @Test
    void settersRoundTrip() {
        RpcCheckConfig config = new RpcCheckConfig();
        config.setEnabled(true);
        config.setRequestTimeoutMs(7500);

        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getRequestTimeoutMs()).isEqualTo(7500);
    }

    @Test
    void targetIsRpcEnabledFalseWhenSubBlockMissing() {
        TransportMonitoringTarget target = new TransportMonitoringTarget();
        target.setBaseUrl("tcp://host:1883");

        assertThat(target.isRpcEnabled()).isFalse();
    }

    @Test
    void targetIsRpcEnabledFalseWhenDisabled() {
        TransportMonitoringTarget target = new TransportMonitoringTarget();
        target.setBaseUrl("tcp://host:1883");
        target.setRpc(new RpcCheckConfig());

        assertThat(target.isRpcEnabled()).isFalse();
    }

    @Test
    void targetIsRpcEnabledTrueWhenEnabled() {
        TransportMonitoringTarget target = new TransportMonitoringTarget();
        target.setBaseUrl("tcp://host:1883");
        RpcCheckConfig rpc = new RpcCheckConfig();
        rpc.setEnabled(true);
        target.setRpc(rpc);

        assertThat(target.isRpcEnabled()).isTrue();
    }

}
