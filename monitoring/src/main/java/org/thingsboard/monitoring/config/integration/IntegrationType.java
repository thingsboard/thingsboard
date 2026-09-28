// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.config.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.monitoring.service.integration.IntegrationHealthChecker;
import org.thingsboard.monitoring.service.integration.impl.CoapIntegrationHealthChecker;
import org.thingsboard.monitoring.service.integration.impl.HttpIntegrationHealthChecker;
import org.thingsboard.monitoring.service.integration.impl.MqttIntegrationHealthChecker;

@AllArgsConstructor
@Getter
public enum IntegrationType {

    HTTP("HTTP", HttpIntegrationHealthChecker.class),
    COAP("CoAP", CoapIntegrationHealthChecker.class),
    MQTT("MQTT", MqttIntegrationHealthChecker.class);

    private final String name;
    private final Class<? extends IntegrationHealthChecker<?>> serviceClass;

}
