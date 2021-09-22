package org.thingsboard.server.lwm2m;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TransportObserver;
import org.thingsboard.server.TransportType;

@Component
public class Lwm2MObserver implements TransportObserver {

    @Value("${lwm2m.monitoring_rate}")
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
    public TransportType getTransportType() {
        return TransportType.LWM2M;
    }
}
