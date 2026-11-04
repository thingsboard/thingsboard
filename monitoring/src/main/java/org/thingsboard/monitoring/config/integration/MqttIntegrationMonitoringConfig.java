// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.config.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "monitoring.integrations.mqtt.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "monitoring.integrations.mqtt")
public class MqttIntegrationMonitoringConfig extends IntegrationMonitoringConfig {

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.MQTT;
    }

}
