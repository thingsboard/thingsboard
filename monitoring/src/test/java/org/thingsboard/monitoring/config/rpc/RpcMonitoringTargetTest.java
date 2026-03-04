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
package org.thingsboard.monitoring.config.rpc;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RpcMonitoringTargetTest {

    @Test
    void getLabel_returnsConfiguredLabel() {
        RpcMonitoringTarget target = new RpcMonitoringTarget();
        target.setDeviceId(UUID.randomUUID().toString());
        target.setLabel("shard0");

        assertThat(target.getLabel()).isEqualTo("shard0");
    }

    @Test
    void getLabel_fallsBackToDeviceIdPrefix_whenLabelIsNull() {
        String deviceId = "a1b2c3d4-0000-0000-0000-000000000000";
        RpcMonitoringTarget target = new RpcMonitoringTarget();
        target.setDeviceId(deviceId);
        target.setLabel(null);

        assertThat(target.getLabel()).isEqualTo("a1b2c3d4");
    }

    @Test
    void getLabel_fallsBackToDeviceIdPrefix_whenLabelIsBlank() {
        String deviceId = "ffee1122-0000-0000-0000-000000000000";
        RpcMonitoringTarget target = new RpcMonitoringTarget();
        target.setDeviceId(deviceId);
        target.setLabel("   ");

        assertThat(target.getLabel()).isEqualTo("ffee1122");
    }

    @Test
    void getDeviceId_parsesUuidString() {
        UUID expected = UUID.randomUUID();
        RpcMonitoringTarget target = new RpcMonitoringTarget();
        target.setDeviceId(expected.toString());

        assertThat(target.getDeviceId()).isEqualTo(expected);
    }

    @Test
    void isCheckDomainIps_alwaysReturnsFalse() {
        assertThat(new RpcMonitoringTarget().isCheckDomainIps()).isFalse();
    }

}
