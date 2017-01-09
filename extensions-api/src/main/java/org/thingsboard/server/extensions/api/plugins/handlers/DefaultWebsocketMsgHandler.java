/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.extensions.api.plugins.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.ws.PluginWebsocketSessionRef;
import org.thingsboard.server.extensions.api.plugins.ws.SessionEvent;
import org.thingsboard.server.extensions.api.plugins.ws.WsSessionMetaData;
import org.thingsboard.server.extensions.api.plugins.ws.msg.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DefaultWebsocketMsgHandler implements WebsocketMsgHandler {

    protected final ObjectMapper jsonMapper = new ObjectMapper();

    protected final Map<String, WsSessionMetaData> wsSessionsMap = new HashMap<>();

    @Override
    public void process(PluginContext ctx, PluginWebsocketMsg<?> wsMsg) {
        PluginWebsocketSessionRef sessionRef = wsMsg.getSessionRef();
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing: {}", sessionRef.getSessionId(), wsMsg);
        } else {
            log.debug("[{}] Processing: {}", sessionRef.getSessionId(), wsMsg.getClass().getSimpleName());
        }
        if (wsMsg instanceof SessionEventPluginWebSocketMsg) {
            handleWebSocketSessionEvent(ctx, sessionRef, (SessionEventPluginWebSocketMsg) wsMsg);
        } else if (wsMsg instanceof TextPluginWebSocketMsg || wsMsg instanceof BinaryPluginWebSocketMsg) {
            handleWebSocketMsg(ctx, sessionRef, wsMsg);
        } else if (wsMsg instanceof PongPluginWebsocketMsg) {
            handleWebSocketPongEvent(ctx, sessionRef);
        }
    }

    protected void handleWebSocketMsg(PluginContext ctx, PluginWebsocketSessionRef sessionRef, PluginWebsocketMsg<?> wsMsg) {
        throw new RuntimeException("Web-sockets are not supported by current plugin!");
    }

    protected void cleanupWebSocketSession(PluginContext ctx, String sessionId) {

    }

    protected void handleWebSocketSessionEvent(PluginContext ctx, PluginWebsocketSessionRef sessionRef, SessionEventPluginWebSocketMsg wsMsg) {
        String sessionId = sessionRef.getSessionId();
        SessionEvent event = wsMsg.getPayload();
        log.debug("[{}] Processing: {}", sessionId, event);
        switch (event.getEventType()) {
            case ESTABLISHED:
                wsSessionsMap.put(sessionId, new WsSessionMetaData(sessionRef));
                break;
            case ERROR:
                log.debug("[{}] Unknown websocket session error: {}. ", sessionId, event.getError().get());
                break;
            case CLOSED:
                wsSessionsMap.remove(sessionId);
                cleanupWebSocketSession(ctx, sessionId);
                break;
        }
    }

    protected void handleWebSocketPongEvent(PluginContext ctx, PluginWebsocketSessionRef sessionRef) {
        String sessionId = sessionRef.getSessionId();
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD != null) {
            log.debug("[{}] Updating session metadata: {}", sessionId, sessionRef);
            sessionMD.setSessionRef(sessionRef);
            sessionMD.setLastActivityTime(System.currentTimeMillis());
        }
    }

    public void clear(PluginContext ctx) {
        wsSessionsMap.values().forEach(v -> {
            try {
                ctx.close(v.getSessionRef());
            } catch (IOException e) {
                log.debug("[{}] Failed to close session: {}", v.getSessionRef().getSessionId(), e.getMessage(), e);
            }
        });
        wsSessionsMap.clear();
    }
}