// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.service.transport.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.monitoring.config.transport.HttpTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import java.time.Duration;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class HttpTransportHealthChecker extends TransportHealthChecker<HttpTransportMonitoringConfig> {

    private RestTemplate restTemplate;

    protected HttpTransportHealthChecker(HttpTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (restTemplate == null) {
            restTemplate = new RestTemplateBuilder()
                    .connectTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .readTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .build();
            log.debug("Initialized HTTP client");
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        restTemplate.postForObject(target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry", payload, String.class);
    }

    @Override
    protected void destroyClient() throws Exception {}

    @Override
    protected TransportType getTransportType() {
        return TransportType.HTTP;
    }

}
