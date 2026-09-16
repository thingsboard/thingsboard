// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.config.transport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "monitoring.transports.lwm2m.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "monitoring.transports.lwm2m")
public class Lwm2mTransportMonitoringConfig extends TransportMonitoringConfig {

    @Override
    public TransportType getTransportType() {
        return TransportType.LWM2M;
    }

}
