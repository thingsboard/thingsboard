// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.config.transport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.monitoring.service.transport.impl.CoapTransportHealthChecker;
import org.thingsboard.monitoring.service.transport.impl.HttpTransportHealthChecker;
import org.thingsboard.monitoring.service.transport.impl.Lwm2mTransportHealthChecker;
import org.thingsboard.monitoring.service.transport.impl.MqttTransportHealthChecker;

@AllArgsConstructor
@Getter
public enum TransportType {

    MQTT("MQTT", MqttTransportHealthChecker.class),
    COAP("CoAP",CoapTransportHealthChecker.class),
    HTTP("HTTP", HttpTransportHealthChecker.class),
    LWM2M("LwM2M", Lwm2mTransportHealthChecker.class);

    private final String name;
    private final Class<? extends TransportHealthChecker<?>> serviceClass;

}
