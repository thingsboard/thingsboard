package org.thingsboard.server.common.transport;

import org.thingsboard.server.gen.transport.TransportProtos;

/**
 * Created by ashvayka on 04.10.18.
 */
public interface TransportService {

    void process(TransportProtos.ValidateDeviceTokenRequestMsg msg,
                 TransportServiceCallback<TransportProtos.ValidateDeviceTokenResponseMsg> callback);

    void process(TransportProtos.SessionEventMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.PostTelemetryMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.PostAttributeMsg msg, TransportServiceCallback<Void> callback);

}
