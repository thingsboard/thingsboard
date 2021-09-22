package org.thingsboard.server;

import lombok.ToString;

@ToString
public enum TransportType {
    COAP,
    HTTP,
    LWM2M,
    MQTT,
    SNMP
}
