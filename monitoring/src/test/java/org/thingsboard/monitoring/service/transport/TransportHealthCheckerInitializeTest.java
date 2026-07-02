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
package org.thingsboard.monitoring.service.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.HttpTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.RpcCheckConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.MonitoringEntityService;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class TransportHealthCheckerInitializeTest {

    private HttpTransportMonitoringConfig config;
    private TransportMonitoringTarget target;
    private MonitoringEntityService entityService;

    @BeforeEach
    void setUp() {
        config = new HttpTransportMonitoringConfig();
        ReflectionTestUtils.invokeSetterMethod(config, "setRequestTimeoutMs", 3000, int.class);

        DeviceConfig device = new DeviceConfig();
        device.setId(UUID.randomUUID().toString());
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId("tok");
        device.setCredentials(credentials);

        target = new TransportMonitoringTarget();
        target.setBaseUrl("http://localhost:8080");
        target.setDevice(device);

        entityService = mock(MonitoringEntityService.class);
        doNothing().when(entityService).checkEntities(config, target);
    }

    @Test
    void initializePassesWhenRpcDisabled() {
        StubTransportHealthChecker checker = newChecker(5000);

        assertThatCode(checker::initializePublic).doesNotThrowAnyException();
    }

    @Test
    void initializePassesWhenRpcTimeoutBelowRest() {
        enableRpc(2000);
        StubTransportHealthChecker checker = newChecker(5000);

        assertThatCode(checker::initializePublic).doesNotThrowAnyException();
    }

    @Test
    void initializeFailsWhenRpcTimeoutEqualsRest() {
        enableRpc(5000);
        StubTransportHealthChecker checker = newChecker(5000);

        assertThatThrownBy(checker::initializePublic)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RPC request timeout")
                .hasMessageContaining("must be <");
    }

    @Test
    void initializeFailsWhenRpcTimeoutExceedsRest() {
        enableRpc(7000);
        StubTransportHealthChecker checker = newChecker(5000);

        assertThatThrownBy(checker::initializePublic)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RPC request timeout (7000 ms)");
    }

    @Test
    void initializeFallsBackToTransportTimeoutAndStillValidates() {
        enableRpc(null);
        StubTransportHealthChecker checker = newChecker(2000);

        assertThatThrownBy(checker::initializePublic)
                .isInstanceOf(IllegalStateException.class);
    }

    private StubTransportHealthChecker newChecker(int restTimeoutMs) {
        StubTransportHealthChecker checker = new StubTransportHealthChecker(config, target);
        ReflectionTestUtils.setField(checker, "entityService", entityService);
        ReflectionTestUtils.setField(checker, "restRequestTimeoutMs", restTimeoutMs);
        return checker;
    }

    private void enableRpc(Integer rpcTimeoutMs) {
        RpcCheckConfig rpc = new RpcCheckConfig();
        rpc.setEnabled(true);
        rpc.setRequestTimeoutMs(rpcTimeoutMs);
        target.setRpc(rpc);
    }

    private static final class StubTransportHealthChecker extends TransportHealthChecker<HttpTransportMonitoringConfig> {
        StubTransportHealthChecker(HttpTransportMonitoringConfig config, TransportMonitoringTarget target) {
            super(config, target);
        }

        void initializePublic() {
            initialize();
        }

        @Override protected TransportType getTransportType() { return TransportType.HTTP; }
        @Override protected void initClient() {}
        @Override protected void sendTestPayload(String payload) {}
        @Override protected void destroyClient() {}
    }
}
