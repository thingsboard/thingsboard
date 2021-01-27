/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.config.WebSocketConfiguration;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.telemetry.SessionEvent;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketMsgEndpoint;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@TbCoreComponent
@Slf4j
public class TbWebSocketHandler extends TextWebSocketHandler implements TelemetryWebSocketMsgEndpoint {

    private static final ConcurrentMap<String, SessionMetaData> internalSessionMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> externalSessionMap = new ConcurrentHashMap<>();

    @Autowired
    private TelemetryWebSocketService webSocketService;

    @Value("${server.ws.send_timeout:5000}")
    private long sendTimeout;
    @Value("${server.ws.limits.max_sessions_per_tenant:0}")
    private int maxSessionsPerTenant;
    @Value("${server.ws.limits.max_sessions_per_customer:0}")
    private int maxSessionsPerCustomer;
    @Value("${server.ws.limits.max_sessions_per_regular_user:0}")
    private int maxSessionsPerRegularUser;
    @Value("${server.ws.limits.max_sessions_per_public_user:0}")
    private int maxSessionsPerPublicUser;
    @Value("${server.ws.limits.max_queue_per_ws_session:1000}")
    private int maxMsgQueuePerSession;

    @Value("${server.ws.limits.max_updates_per_session:}")
    private String perSessionUpdatesConfiguration;

    private ConcurrentMap<String, TelemetryWebSocketSessionRef> blacklistedSessions = new ConcurrentHashMap<>();
    private ConcurrentMap<String, TbRateLimits> perSessionUpdateLimits = new ConcurrentHashMap<>();

    private ConcurrentMap<TenantId, Set<String>> tenantSessionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<CustomerId, Set<String>> customerSessionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<UserId, Set<String>> regularUserSessionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<UserId, Set<String>> publicUserSessionsMap = new ConcurrentHashMap<>();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SessionMetaData sessionMd = internalSessionMap.get(session.getId());
            if (sessionMd != null) {
                log.trace("[{}][{}] Processing {}", sessionMd.sessionRef.getSecurityCtx().getTenantId(), session.getId(), message.getPayload());
                webSocketService.handleWebSocketMsg(sessionMd.sessionRef, message.getPayload());
            } else {
                log.trace("[{}] Failed to find session", session.getId());
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
            if (session instanceof NativeWebSocketSession) {
                Session nativeSession = ((NativeWebSocketSession) session).getNativeSession(Session.class);
                if (nativeSession != null) {
                    nativeSession.getAsyncRemote().setSendTimeout(sendTimeout);
                }
            }
            String internalSessionId = session.getId();
            TelemetryWebSocketSessionRef sessionRef = toRef(session);
            String externalSessionId = sessionRef.getSessionId();
            if (!checkLimits(session, sessionRef)) {
                return;
            }
            internalSessionMap.put(internalSessionId, new SessionMetaData(session, sessionRef, maxMsgQueuePerSession));
            externalSessionMap.put(externalSessionId, internalSessionId);
            processInWebSocketService(sessionRef, SessionEvent.onEstablished());
            log.info("[{}][{}][{}] Session is opened", sessionRef.getSecurityCtx().getTenantId(), externalSessionId, session.getId());
        } catch (InvalidParameterException e) {
            log.warn("[{}] Failed to start session", session.getId(), e);
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
            log.trace("[{}] Failed to find session", session.getId());
        }
        log.trace("[{}] Session transport error", session.getId(), tError);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        super.afterConnectionClosed(session, closeStatus);
        SessionMetaData sessionMd = internalSessionMap.remove(session.getId());
        if (sessionMd != null) {
            cleanupLimits(session, sessionMd.sessionRef);
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
            SecurityUser currentUser = (SecurityUser) ((Authentication) session.getPrincipal()).getPrincipal();
            return new TelemetryWebSocketSessionRef(UUID.randomUUID().toString(), currentUser, session.getLocalAddress(), session.getRemoteAddress());
        }
    }

    private class SessionMetaData implements SendHandler {
        private final WebSocketSession session;
        private final RemoteEndpoint.Async asyncRemote;
        private final TelemetryWebSocketSessionRef sessionRef;

        private volatile boolean isSending = false;
        private final Queue<String> msgQueue;

        SessionMetaData(WebSocketSession session, TelemetryWebSocketSessionRef sessionRef, int maxMsgQueuePerSession) {
            super();
            this.session = session;
            Session nativeSession = ((NativeWebSocketSession) session).getNativeSession(Session.class);
            this.asyncRemote = nativeSession.getAsyncRemote();
            this.sessionRef = sessionRef;
            this.msgQueue = new LinkedBlockingQueue<>(maxMsgQueuePerSession);
        }

        synchronized void sendMsg(String msg) {
            if (isSending) {
                try {
                    msgQueue.add(msg);
                } catch (RuntimeException e) {
                    if (log.isTraceEnabled()) {
                        log.trace("[{}][{}] Session closed due to queue error", sessionRef.getSecurityCtx().getTenantId(), session.getId(), e);
                    } else {
                        log.info("[{}][{}] Session closed due to queue error", sessionRef.getSecurityCtx().getTenantId(), session.getId());
                    }
                    try {
                        close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max pending updates limit reached!"));
                    } catch (IOException ioe) {
                        log.trace("[{}] Session transport error", session.getId(), ioe);
                    }
                }
            } else {
                isSending = true;
                sendMsgInternal(msg);
            }
        }

        private void sendMsgInternal(String msg) {
            try {
                this.asyncRemote.sendText(msg, this);
            } catch (Exception e) {
                log.trace("[{}] Failed to send msg", session.getId(), e);
                try {
                    close(this.sessionRef, CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException ioe) {
                    log.trace("[{}] Session transport error", session.getId(), ioe);
                }
            }
        }

        @Override
        public void onResult(SendResult result) {
            if (!result.isOK()) {
                log.trace("[{}] Failed to send msg", session.getId(), result.getException());
                try {
                    close(this.sessionRef, CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException ioe) {
                    log.trace("[{}] Session transport error", session.getId(), ioe);
                }
            } else {
                String msg = msgQueue.poll();
                if (msg != null) {
                    sendMsgInternal(msg);
                } else {
                    isSending = false;
                }
            }
        }
    }

    @Override
    public void send(TelemetryWebSocketSessionRef sessionRef, int subscriptionId, String msg) throws IOException {
        String externalId = sessionRef.getSessionId();
        log.debug("[{}] Processing {}", externalId, msg);
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                if (!StringUtils.isEmpty(perSessionUpdatesConfiguration)) {
                    TbRateLimits rateLimits = perSessionUpdateLimits.computeIfAbsent(sessionRef.getSessionId(), sid -> new TbRateLimits(perSessionUpdatesConfiguration));
                    if (!rateLimits.tryConsume()) {
                        if (blacklistedSessions.putIfAbsent(externalId, sessionRef) == null) {
                            log.info("[{}][{}][{}] Failed to process session update. Max session updates limit reached"
                                    , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), externalId);
                            sessionMd.sendMsg("{\"subscriptionId\":" + subscriptionId + ", \"errorCode\":" + ThingsboardErrorCode.TOO_MANY_UPDATES.getErrorCode() + ", \"errorMsg\":\"Too many updates!\"}");
                        }
                        return;
                    } else {
                        log.debug("[{}][{}][{}] Session is no longer blacklisted.", sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), externalId);
                        blacklistedSessions.remove(externalId);
                    }
                }
                sessionMd.sendMsg(msg);
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

    @Override
    public void close(TelemetryWebSocketSessionRef sessionRef, CloseStatus reason) throws IOException {
        String externalId = sessionRef.getSessionId();
        log.debug("[{}] Processing close request", externalId);
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                sessionMd.session.close(reason);
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

    private boolean checkLimits(WebSocketSession session, TelemetryWebSocketSessionRef sessionRef) throws Exception {
        String sessionId = session.getId();
        if (maxSessionsPerTenant > 0) {
            Set<String> tenantSessions = tenantSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
            synchronized (tenantSessions) {
                if (tenantSessions.size() < maxSessionsPerTenant) {
                    tenantSessions.add(sessionId);
                } else {
                    log.info("[{}][{}][{}] Failed to start session. Max tenant sessions limit reached"
                            , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), sessionId);
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Max tenant sessions limit reached!"));
                    return false;
                }
            }
        }

        if (sessionRef.getSecurityCtx().isCustomerUser()) {
            if (maxSessionsPerCustomer > 0) {
                Set<String> customerSessions = customerSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (customerSessions) {
                    if (customerSessions.size() < maxSessionsPerCustomer) {
                        customerSessions.add(sessionId);
                    } else {
                        log.info("[{}][{}][{}] Failed to start session. Max customer sessions limit reached"
                                , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), sessionId);
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Max customer sessions limit reached"));
                        return false;
                    }
                }
            }
            if (maxSessionsPerRegularUser > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> regularUserSessions = regularUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (regularUserSessions) {
                    if (regularUserSessions.size() < maxSessionsPerRegularUser) {
                        regularUserSessions.add(sessionId);
                    } else {
                        log.info("[{}][{}][{}] Failed to start session. Max regular user sessions limit reached"
                                , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), sessionId);
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Max regular user sessions limit reached"));
                        return false;
                    }
                }
            }
            if (maxSessionsPerPublicUser > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> publicUserSessions = publicUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (publicUserSessions) {
                    if (publicUserSessions.size() < maxSessionsPerPublicUser) {
                        publicUserSessions.add(sessionId);
                    } else {
                        log.info("[{}][{}][{}] Failed to start session. Max public user sessions limit reached"
                                , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), sessionId);
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Max public user sessions limit reached"));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void cleanupLimits(WebSocketSession session, TelemetryWebSocketSessionRef sessionRef) {
        String sessionId = session.getId();
        perSessionUpdateLimits.remove(sessionRef.getSessionId());
        blacklistedSessions.remove(sessionRef.getSessionId());
        if (maxSessionsPerTenant > 0) {
            Set<String> tenantSessions = tenantSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
            synchronized (tenantSessions) {
                tenantSessions.remove(sessionId);
            }
        }
        if (sessionRef.getSecurityCtx().isCustomerUser()) {
            if (maxSessionsPerCustomer > 0) {
                Set<String> customerSessions = customerSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (customerSessions) {
                    customerSessions.remove(sessionId);
                }
            }
            if (maxSessionsPerRegularUser > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> regularUserSessions = regularUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (regularUserSessions) {
                    regularUserSessions.remove(sessionId);
                }
            }
            if (maxSessionsPerPublicUser > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> publicUserSessions = publicUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (publicUserSessions) {
                    publicUserSessions.remove(sessionId);
                }
            }
        }
    }

}