package org.thingsboard.server;

public interface TransportObserver {

    String pingTransport(String payload) throws Exception;

    int getMonitoringRate();

    TransportType getTransportType();
}
