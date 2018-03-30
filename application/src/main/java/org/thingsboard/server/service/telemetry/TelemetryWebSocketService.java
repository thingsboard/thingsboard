package org.thingsboard.server.service.telemetry;

import org.thingsboard.server.extensions.api.plugins.ws.SessionEvent;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface TelemetryWebSocketService {

    void handleWebSocketSessionEvent(TelemetryWebSocketSessionRef sessionRef, SessionEvent sessionEvent);

    void handleWebSocketMsg(TelemetryWebSocketSessionRef sessionRef, String msg);
}
