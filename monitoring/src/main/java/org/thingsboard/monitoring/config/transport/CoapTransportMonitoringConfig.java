// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.config.transport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "monitoring.transports.coap.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "monitoring.transports.coap")
public class CoapTransportMonitoringConfig extends TransportMonitoringConfig {

    @Override
    public TransportType getTransportType() {
        return TransportType.COAP;
    }

}
