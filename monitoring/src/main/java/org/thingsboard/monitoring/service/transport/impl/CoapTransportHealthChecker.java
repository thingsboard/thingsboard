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
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.SystemConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.SslUtil;
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
        CoapConfig.register();
    }

    private CoapClient coapClient;
    private CoapEndpoint coapEndpoint;

    protected CoapTransportHealthChecker(CoapTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (coapClient != null) {
            if (isSessionExpired()) {
                log.info("Reconnecting {} client to {}", getTransportType(), target.getBaseUrl());
                shutdownCoapClient();
            } else {
                return;
            }
        }

        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        String uri = target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry";
        coapClient = new CoapClient(uri);
        if (target.getBaseUrl().startsWith("coaps")) {
            coapEndpoint = new CoapEndpoint.Builder().setConnector(SslUtil.defaultDtlsClientConnector()).build();
            coapClient.setEndpoint(coapEndpoint);
        }
        coapClient.setTimeout((long) config.getRequestTimeoutMs());
        recordSessionStart();
        log.debug("Connecting {} client to {}", getTransportType(), target.getBaseUrl());
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        CoapResponse response = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            throw new IOException(getTransportType() + " request timed out");
        }
        if (response.getCode().codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException(getTransportType() + " client didn't receive success response from transport");
        }
    }

    @Override
    protected void destroyClient() {
        if (coapClient != null) {
            shutdownCoapClient();
        }
    }

    private void shutdownCoapClient() {
        try {
            coapClient.shutdown();
        } catch (Exception e) {
            log.warn("Failed to shutdown CoAP client: {}", e.getMessage());
        } finally {
            if (coapEndpoint != null) {
                try {
                    coapEndpoint.destroy();
                } catch (Exception e) {
                    log.warn("Failed to destroy CoAP endpoint: {}", e.getMessage());
                }
                coapEndpoint = null;
            }
            coapClient = null;
            log.debug("Disconnected {} client", getTransportType());
        }
    }

    @Override
    protected TransportType getTransportType() {
        return config.getTransportType();
    }

}
