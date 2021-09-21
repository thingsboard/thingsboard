package org.thingsboard.server.coap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TransportObserver;
import org.thingsboard.server.TransportType;

@Component
public class CoapObserver implements TransportObserver {

    @Value("${mqtt.monitoring_rate}")
    private int monitoringRate;

    @Override
    public String pingTransport(String payload) {
        return "";
    }

    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    @Override
    public TransportType getTransportType(String msg) {
        TransportType mqtt = TransportType.COAP;
        mqtt.setInfo(msg);
        return mqtt;
    }
}
