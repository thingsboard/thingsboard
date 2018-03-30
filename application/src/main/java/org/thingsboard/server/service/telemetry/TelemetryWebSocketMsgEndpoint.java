package org.thingsboard.server.service.telemetry;

import java.io.IOException;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface TelemetryWebSocketMsgEndpoint {

    void send(TelemetryWebSocketSessionRef sessionRef, String msg) throws IOException;

    void close(TelemetryWebSocketSessionRef sessionRef) throws IOException;

}
