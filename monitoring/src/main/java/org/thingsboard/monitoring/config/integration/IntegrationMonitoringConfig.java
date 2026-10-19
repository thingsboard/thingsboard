// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.config.integration;

import lombok.Data;
import org.thingsboard.monitoring.config.MonitoringConfig;

import java.util.List;

@Data
public abstract class IntegrationMonitoringConfig implements MonitoringConfig<IntegrationMonitoringTarget> {

    private int requestTimeoutMs;
    private IntegrationMonitoringTarget target;

    @Override
    public List<IntegrationMonitoringTarget> getTargets() {
        return List.of(target);
    }

    public abstract IntegrationType getIntegrationType();

}
