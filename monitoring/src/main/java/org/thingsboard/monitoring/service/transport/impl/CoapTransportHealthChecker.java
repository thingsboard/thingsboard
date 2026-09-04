// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.service.transport.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.config.SystemConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.transport.CoapTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import java.io.IOException;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CoapTransportHealthChecker extends TransportHealthChecker<CoapTransportMonitoringConfig> {

    static {
        SystemConfig.register();
    }

    private CoapClient coapClient;

    protected CoapTransportHealthChecker(CoapTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (coapClient == null) {
            String accessToken = target.getDevice().getCredentials().getCredentialsId();
            String uri = target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry";
            coapClient = new CoapClient(uri);
            coapClient.setTimeout((long) config.getRequestTimeoutMs());
            log.debug("Initialized CoAP client for URI {}", uri);
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        CoapResponse response = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        CoAP.ResponseCode code = response.getCode();
        if (code.codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException("COAP client didn't receive success response from transport");
        }
    }

    @Override
    protected void destroyClient() throws Exception {
        if (coapClient != null) {
            coapClient.shutdown();
            coapClient = null;
            log.info("Disconnected CoAP client");
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.COAP;
    }

}
