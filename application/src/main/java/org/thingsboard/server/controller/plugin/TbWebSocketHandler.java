/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.controller.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.thingsboard.server.config.WebSocketConfiguration;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.telemetry.SessionEvent;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketMsgEndpoint;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;

import java.io.IOException;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class TbWebSocketHandler extends TextWebSocketHandler implements TelemetryWebSocketMsgEndpoint {

    private static final ConcurrentMap<String, SessionMetaData> internalSessionMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> externalSessionMap = new ConcurrentHashMap<>();

    @Autowired
    private TelemetryWebSocketService webSocketService;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            log.info("[{}] Processing {}", session.getId(), message);
            SessionMetaData sessionMd = internalSessionMap.get(session.getId());
            if (sessionMd != null) {
                webSocketService.handleWebSocketMsg(sessionMd.sessionRef, message.getPayload());
            } else {
                log.warn("[{}] Failed to find session", session.getId());
                session.close(CloseStatus.SERVER_ERROR.withReason("Session not found!"));
            }
        } catch (IOException e) {
            log.warn("IO error", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        try {
            String internalSessionId = session.getId();
            TelemetryWebSocketSessionRef sessionRef = toRef(session);
            String externalSessionId = sessionRef.getSessionId();
            internalSessionMap.put(internalSessionId, new SessionMetaData(session, sessionRef));
            externalSessionMap.put(externalSessionId, internalSessionId);
            processInWebSocketService(sessionRef, SessionEvent.onEstablished());
            log.info("[{}][{}] Session is started", externalSessionId, session.getId());
        } catch (InvalidParameterException e) {
            log.warn("[[{}] Failed to start session", session.getId(), e);
            session.close(CloseStatus.BAD_DATA.withReason(e.getMessage()));
        } catch (Exception e) {
            log.warn("[{}] Failed to start session", session.getId(), e);
            session.close(CloseStatus.SERVER_ERROR.withReason(e.getMessage()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable tError) throws Exception {
        super.handleTransportError(session, tError);
        SessionMetaData sessionMd = internalSessionMap.get(session.getId());
        if (sessionMd != null) {
            processInWebSocketService(sessionMd.sessionRef, SessionEvent.onError(tError));
        } else {
            log.warn("[{}] Failed to find session", session.getId());
        }
        log.trace("[{}] Session transport error", session.getId(), tError);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        super.afterConnectionClosed(session, closeStatus);
        SessionMetaData sessionMd = internalSessionMap.remove(session.getId());
        if (sessionMd != null) {
            externalSessionMap.remove(sessionMd.sessionRef.getSessionId());
            processInWebSocketService(sessionMd.sessionRef, SessionEvent.onClosed());
        }
        log.info("[{}] Session is closed", session.getId());
    }

    private void processInWebSocketService(TelemetryWebSocketSessionRef sessionRef, SessionEvent event) {
        try {
            webSocketService.handleWebSocketSessionEvent(sessionRef, event);
        } catch (BeanCreationNotAllowedException e) {
            log.warn("[{}] Failed to close session due to possible shutdown state", sessionRef.getSessionId());
        }
    }

    private TelemetryWebSocketSessionRef toRef(WebSocketSession session) throws IOException {
        URI sessionUri = session.getUri();
        String path = sessionUri.getPath();
        path = path.substring(WebSocketConfiguration.WS_PLUGIN_PREFIX.length());
        if (path.length() == 0) {
            throw new IllegalArgumentException("URL should contain plugin token!");
        }
        String[] pathElements = path.split("/");
        String serviceToken = pathElements[0];
        if (!"telemetry".equalsIgnoreCase(serviceToken)) {
            throw new InvalidParameterException("Can't find plugin with specified token!");
        } else {
            SecurityUser currentUser = (SecurityUser) session.getAttributes().get(WebSocketConfiguration.WS_SECURITY_USER_ATTRIBUTE);
            return new TelemetryWebSocketSessionRef(UUID.randomUUID().toString(), currentUser, session.getLocalAddress(), session.getRemoteAddress());
        }
    }

    private static class SessionMetaData {
        private final WebSocketSession session;
        private final TelemetryWebSocketSessionRef sessionRef;

        public SessionMetaData(WebSocketSession session, TelemetryWebSocketSessionRef sessionRef) {
            super();
            this.session = session;
            this.sessionRef = sessionRef;
        }
    }

    @Override
    public void send(TelemetryWebSocketSessionRef sessionRef, String msg) throws IOException {
        String externalId = sessionRef.getSessionId();
        log.debug("[{}] Processing {}", externalId, msg);
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                synchronized (sessionMd) {
                    sessionMd.session.sendMessage(new TextMessage(msg));
                }
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

    @Override
    public void close(TelemetryWebSocketSessionRef sessionRef) throws IOException {
        String externalId = sessionRef.getSessionId();
        log.debug("[{}] Processing close request", externalId);
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                sessionMd.session.close(CloseStatus.NORMAL);
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

}
