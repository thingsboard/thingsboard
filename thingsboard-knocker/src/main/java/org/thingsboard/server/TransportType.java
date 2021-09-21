package org.thingsboard.server;

import lombok.ToString;

@ToString
public enum TransportType {
    COAP,
    HTTP,
    LWM2M,
    MQTT,
    SNMP;

    private String info;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
