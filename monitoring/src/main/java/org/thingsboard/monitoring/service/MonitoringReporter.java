package org.thingsboard.monitoring.service;

import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.data.TransportInfo;

@Service
public class MonitoringReporter {

    public void reportTransportRequestLatency(TransportInfo transportInfo, long latencyInNanos) {
        double latencyInMs = (double) latencyInNanos / 1000_000;
    }

    public void reportTransportConnectLatency(TransportInfo transportInfo, long latencyInNanos) {

    }

    public void reportWsUpdateLatency(long latencyInNanos) {

    }

    public void reportWsConnectLatency(long latencyInNanos) {

    }

    public void reportLogInLatency(long latencyInNanos) {

    }

    public void reportFailure(TransportInfo transportInfo, Exception error) {

    }
}
