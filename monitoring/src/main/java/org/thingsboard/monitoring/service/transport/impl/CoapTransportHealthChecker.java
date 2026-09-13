// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.service.transport.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.transport.CoapTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.monitoring.util.DtlsClientConnectorFactory;

import java.io.IOException;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CoapTransportHealthChecker extends TransportHealthChecker<CoapTransportMonitoringConfig> {

    static {
        SystemConfig.register();
        CoapConfig.register();
        DtlsConfig.register();
    }

    private CoapClient coapClient;
    private CoapEndpoint coapEndpoint;

    protected CoapTransportHealthChecker(CoapTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (coapClient != null && !isSessionExpired()) {
            return;
        }
        reconnectIfNeeded(coapClient != null, this::shutdownCoapClient, () -> {
            String accessToken = target.getDevice().getCredentials().getCredentialsId();
            String uri = target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry";
            coapClient = new CoapClient(uri);
            if (isSecure()) {
                coapEndpoint = new CoapEndpoint.Builder().setConnector(DtlsClientConnectorFactory.jvmTrustedDtlsClientConnector()).build();
                coapClient.setEndpoint(coapEndpoint);
            }
            coapClient.setTimeout((long) config.getRequestTimeoutMs());
        });
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
            log.warn("Failed to shutdown CoAP client", e);
        } finally {
            if (coapEndpoint != null) {
                try {
                    coapEndpoint.destroy();
                } catch (Exception e) {
                    log.warn("Failed to destroy CoAP endpoint", e);
                }
                coapEndpoint = null;
            }
            coapClient = null;
            log.debug("Disconnected {} client", getTransportType());
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.COAP;
    }

    private boolean isSecure() {
        return CoAP.isSecureScheme(CoAP.getSchemeFromUri(target.getBaseUrl()));
    }

}
