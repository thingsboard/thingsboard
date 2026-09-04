// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.config.transport;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "monitoring.transports.mqtt.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "monitoring.transports.mqtt")
@Data
@EqualsAndHashCode(callSuper = true)
public class MqttTransportMonitoringConfig extends TransportMonitoringConfig {

    private Integer qos;

    @Override
    public TransportType getTransportType() {
        return TransportType.MQTT;
    }

}
