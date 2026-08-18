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
package org.thingsboard.monitoring.service.transport.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.client.Lwm2mClient;
import org.thingsboard.monitoring.config.transport.Lwm2mTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class Lwm2mTransportHealthChecker extends TransportHealthChecker<Lwm2mTransportMonitoringConfig> {

    private Lwm2mClient lwm2mClient;

    protected Lwm2mTransportHealthChecker(Lwm2mTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (lwm2mClient == null || lwm2mClient.getLeshanClient() == null || lwm2mClient.isDestroyed()) {
            String endpoint = target.getDevice().getCredentials().getCredentialsId();
            lwm2mClient = new Lwm2mClient(target.getBaseUrl(), endpoint);
            lwm2mClient.initClient();
            log.debug("Initialized LwM2M client for endpoint '{}'", endpoint);
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        lwm2mClient.send(payload, 0);
    }

    @Override
    protected void checkAccepted() {
        // send() never waits for a server ack (just fires a local Leshan event), so this can't
        // produce a meaningful signal for LwM2M - no-op rather than always reporting "success".
    }

    @Override
    protected String createTestPayload(String testValue, String telemetryKey) {
        // LwM2M payload is the raw value itself (no JSON envelope), so there's no telemetry key
        // to embed here; checkAccepted() is a no-op for LwM2M (see above), so in practice this is
        // only ever invoked by check() with TEST_TELEMETRY_KEY.
        return testValue;
    }

    @Override
    protected void destroyClient() throws Exception {
        if (lwm2mClient != null) {
            lwm2mClient.destroy();
            lwm2mClient = null;
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.LWM2M;
    }

}
