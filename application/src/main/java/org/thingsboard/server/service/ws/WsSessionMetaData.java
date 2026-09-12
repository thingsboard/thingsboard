// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws;


/**
 * Created by ashvayka on 27.03.18.
 */
public class WsSessionMetaData {
    private WebSocketSessionRef sessionRef;
    private long lastActivityTime;

    public WsSessionMetaData(WebSocketSessionRef sessionRef) {
        super();
        this.sessionRef = sessionRef;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public WebSocketSessionRef getSessionRef() {
        return sessionRef;
    }

    public void setSessionRef(WebSocketSessionRef sessionRef) {
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
