package org.thingsboard.server.common.transport;

import org.thingsboard.server.gen.transport.TransportProtos;

/**
 * Created by ashvayka on 04.10.18.
 */
public interface TransportService {

    void process(TransportProtos.SessionEventMsg msg);

    void process(TransportProtos.PostTelemetryMsg msg);

    void process(TransportProtos.PostAttributeMsg msg);

}
