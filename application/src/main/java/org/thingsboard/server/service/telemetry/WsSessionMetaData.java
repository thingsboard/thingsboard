package org.thingsboard.server.service.telemetry;

import org.thingsboard.server.extensions.api.plugins.ws.PluginWebsocketSessionRef;

/**
 * Created by ashvayka on 27.03.18.
 */
public class WsSessionMetaData {
    private TelemetryWebSocketSessionRef sessionRef;
    private long lastActivityTime;

    public WsSessionMetaData(TelemetryWebSocketSessionRef sessionRef) {
        super();
        this.sessionRef = sessionRef;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public TelemetryWebSocketSessionRef getSessionRef() {
        return sessionRef;
    }

    public void setSessionRef(TelemetryWebSocketSessionRef sessionRef) {
        this.sessionRef = sessionRef;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    @Override
    public String toString() {
        return "WsSessionMetaData [sessionRef=" + sessionRef + ", lastActivityTime=" + lastActivityTime + "]";
    }
}
