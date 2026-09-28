// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringConfig;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringTarget;
import org.thingsboard.monitoring.service.BaseHealthChecker;
import org.thingsboard.monitoring.service.BaseMonitoringService;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationsMonitoringService extends BaseMonitoringService<IntegrationMonitoringConfig, IntegrationMonitoringTarget> {

    @Override
    protected BaseHealthChecker<?, ?> createHealthChecker(IntegrationMonitoringConfig config, IntegrationMonitoringTarget target) {
        return applicationContext.getBean(config.getIntegrationType().getServiceClass(), config, target);
    }

    @Override
    protected IntegrationMonitoringTarget createTarget(String baseUrl) {
        IntegrationMonitoringTarget target = new IntegrationMonitoringTarget();
        target.setBaseUrl(baseUrl);
        return target;
    }

    @Override
    protected String getName() {
        return "integrations check";
    }

}
