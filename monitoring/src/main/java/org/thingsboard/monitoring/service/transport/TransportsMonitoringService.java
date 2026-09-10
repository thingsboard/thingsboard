// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.service.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.config.transport.TransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.service.BaseHealthChecker;
import org.thingsboard.monitoring.service.BaseMonitoringService;

@Service
@RequiredArgsConstructor
@Slf4j
public final class TransportsMonitoringService extends BaseMonitoringService<TransportMonitoringConfig, TransportMonitoringTarget> {

    @Override
    protected BaseHealthChecker<?, ?> createHealthChecker(TransportMonitoringConfig config, TransportMonitoringTarget target) {
        return applicationContext.getBean(config.getTransportType().getServiceClass(), config, target);
    }

    @Override
    protected TransportMonitoringTarget createTarget(String baseUrl) {
        TransportMonitoringTarget target = new TransportMonitoringTarget();
        target.setBaseUrl(baseUrl);
        return target;
    }

    @Override
    protected String getName() {
        return "transports check";
    }

}
