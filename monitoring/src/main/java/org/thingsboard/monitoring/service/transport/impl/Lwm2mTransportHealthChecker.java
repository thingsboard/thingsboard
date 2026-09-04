// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    protected String createTestPayload(String testValue) {
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
