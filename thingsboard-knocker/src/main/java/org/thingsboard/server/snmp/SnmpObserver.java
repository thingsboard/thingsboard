package org.thingsboard.server.snmp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TransportObserver;
import org.thingsboard.server.TransportType;

@Component
public class SnmpObserver implements TransportObserver {

    @Value("${snmp.monitoring_rate}")
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
        return TransportType.SNMP;
    }
}
