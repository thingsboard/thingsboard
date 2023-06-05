/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.config.WebSocketConfiguration;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.ws.SessionEvent;
import org.thingsboard.server.service.ws.WebSocketMsgEndpoint;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.WebSocketSessionType;

import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.thingsboard.server.service.ws.DefaultWebSocketService.NUMBER_OF_PING_ATTEMPTS;

@Service
@TbCoreComponent
@Slf4j
public class TbWebSocketHandler extends TextWebSocketHandler implements WebSocketMsgEndpoint {

    private final ConcurrentMap<String, SessionMetaData> internalSessionMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> externalSessionMap = new ConcurrentHashMap<>();


    @Autowired @Lazy
    private WebSocketService webSocketService;

    @Autowired
    private TbTenantProfileCache tenantProfileCache;

    @Value("${server.ws.send_timeout:5000}")
    private long sendTimeout;
    @Value("${server.ws.ping_timeout:30000}")
    private long pingTimeout;
    @Value("${server.ws.max_queue_messages_per_session:1000}")
    private int wsMaxQueueMessagesPerSession;

    private final ConcurrentMap<String, WebSocketSessionRef> blacklistedSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TbRateLimits> perSessionUpdateLimits = new ConcurrentHashMap<>();

    private final ConcurrentMap<TenantId, Set<String>> tenantSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<CustomerId, Set<String>> customerSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, Set<String>> regularUserSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, Set<String>> publicUserSessionsMap = new ConcurrentHashMap<>();

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
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        try {
            SessionMetaData sessionMd = internalSessionMap.get(session.getId());
            if (sessionMd != null) {
                log.trace("[{}][{}] Processing pong response {}", sessionMd.sessionRef.getSecurityCtx().getTenantId(), session.getId(), message.getPayload());
                sessionMd.processPongMessage(System.currentTimeMillis());
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
            WebSocketSessionRef sessionRef = toRef(session);
            String externalSessionId = sessionRef.getSessionId();

            if (!checkLimits(session, sessionRef)) {
                return;
            }
            var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
            int wsTenantProfileQueueLimit = tenantProfileConfiguration != null ?
                    tenantProfileConfiguration.getWsMsgQueueLimitPerSession() : wsMaxQueueMessagesPerSession;
            internalSessionMap.put(internalSessionId, new SessionMetaData(session, sessionRef,
                    (wsTenantProfileQueueLimit > 0 && wsTenantProfileQueueLimit < wsMaxQueueMessagesPerSession) ?
                            wsTenantProfileQueueLimit : wsMaxQueueMessagesPerSession));

            externalSessionMap.put(externalSessionId, internalSessionId);
            processInWebSocketService(sessionRef, SessionEvent.onEstablished());
            log.info("[{}][{}][{}] Session is opened from address: {}", sessionRef.getSecurityCtx().getTenantId(), externalSessionId, session.getId(), session.getRemoteAddress());
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
            log.info("[{}][{}][{}] Session is closed", sessionMd.sessionRef.getSecurityCtx().getTenantId(), sessionMd.sessionRef.getSessionId(), session.getId());
        } else {
            log.info("[{}] Session is closed", session.getId());
        }
    }

    private void processInWebSocketService(WebSocketSessionRef sessionRef, SessionEvent event) {
        try {
            webSocketService.handleWebSocketSessionEvent(sessionRef, event);
        } catch (BeanCreationNotAllowedException e) {
            log.warn("[{}] Failed to close session due to possible shutdown state", sessionRef.getSessionId());
        }
    }

    private WebSocketSessionRef toRef(WebSocketSession session) throws IOException {
        URI sessionUri = session.getUri();
        String path = sessionUri.getPath();
        path = path.substring(WebSocketConfiguration.WS_PLUGIN_PREFIX.length());
        if (path.length() == 0) {
            throw new IllegalArgumentException("URL should contain plugin token!");
        }
        String[] pathElements = path.split("/");
        String serviceToken = pathElements[0];
        WebSocketSessionType sessionType = WebSocketSessionType.forName(serviceToken)
                .orElseThrow(() -> new InvalidParameterException("Can't find plugin with specified token!"));

        SecurityUser currentUser = (SecurityUser) ((Authentication) session.getPrincipal()).getPrincipal();
        return WebSocketSessionRef.builder()
                .sessionId(UUID.randomUUID().toString())
                .securityCtx(currentUser)
                .localAddress(session.getLocalAddress())
                .remoteAddress(session.getRemoteAddress())
                .sessionType(sessionType)
                .build();
    }

    private class SessionMetaData implements SendHandler {
        private final WebSocketSession session;
        private final RemoteEndpoint.Async asyncRemote;
        private final WebSocketSessionRef sessionRef;

        private final AtomicBoolean isSending = new AtomicBoolean(false);
        private final Queue<TbWebSocketMsg<?>> msgQueue;

        private volatile long lastActivityTime;

        SessionMetaData(WebSocketSession session, WebSocketSessionRef sessionRef, int maxMsgQueuePerSession) {
            super();
            this.session = session;
            Session nativeSession = ((NativeWebSocketSession) session).getNativeSession(Session.class);
            this.asyncRemote = nativeSession.getAsyncRemote();
            this.sessionRef = sessionRef;
            this.msgQueue = new LinkedBlockingQueue<>(maxMsgQueuePerSession);
            this.lastActivityTime = System.currentTimeMillis();
        }

        synchronized void sendPing(long currentTime) {
            try {
                long timeSinceLastActivity = currentTime - lastActivityTime;
                if (timeSinceLastActivity >= pingTimeout) {
                    log.warn("[{}] Closing session due to ping timeout", session.getId());
                    closeSession(CloseStatus.SESSION_NOT_RELIABLE);
                } else if (timeSinceLastActivity >= pingTimeout / NUMBER_OF_PING_ATTEMPTS) {
                    sendMsg(TbWebSocketPingMsg.INSTANCE);
                }
            } catch (Exception e) {
                log.trace("[{}] Failed to send ping msg", session.getId(), e);
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
            }
        }

        private void closeSession(CloseStatus reason) {
            try {
                close(this.sessionRef, reason);
            } catch (IOException ioe) {
                log.trace("[{}] Session transport error", session.getId(), ioe);
            }
        }

        synchronized void processPongMessage(long currentTime) {
            lastActivityTime = currentTime;
        }

        synchronized void sendMsg(String msg) {
            sendMsg(new TbWebSocketTextMsg(msg));
        }

        synchronized void sendMsg(TbWebSocketMsg<?> msg) {
            if (isSending.compareAndSet(false, true)) {
                sendMsgInternal(msg);
            } else {
                try {
                    msgQueue.add(msg);
                } catch (RuntimeException e) {
                    if (log.isTraceEnabled()) {
                        log.trace("[{}][{}] Session closed due to queue error", sessionRef.getSecurityCtx().getTenantId(), session.getId(), e);
                    } else {
                        log.info("[{}][{}] Session closed due to queue error", sessionRef.getSecurityCtx().getTenantId(), session.getId());
                    }
                    closeSession(CloseStatus.POLICY_VIOLATION.withReason("Max pending updates limit reached!"));
                }
            }
        }

        private void sendMsgInternal(TbWebSocketMsg<?> msg) {
            try {
                if (TbWebSocketMsgType.TEXT.equals(msg.getType())) {
                    TbWebSocketTextMsg textMsg = (TbWebSocketTextMsg) msg;
                    this.asyncRemote.sendText(textMsg.getMsg(), this);
                } else {
                    TbWebSocketPingMsg pingMsg = (TbWebSocketPingMsg) msg;
                    this.asyncRemote.sendPing(pingMsg.getMsg());
                    processNextMsg();
                }
            } catch (Exception e) {
                log.trace("[{}] Failed to send msg", session.getId(), e);
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
            }
        }

        @Override
        public void onResult(SendResult result) {
            if (!result.isOK()) {
                log.trace("[{}] Failed to send msg", session.getId(), result.getException());
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
            } else {
                processNextMsg();
            }
        }

        private void processNextMsg() {
            TbWebSocketMsg<?> msg = msgQueue.poll();
            if (msg != null) {
                sendMsgInternal(msg);
            } else {
                isSending.set(false);
            }
        }
    }

    @Override
    public void send(WebSocketSessionRef sessionRef, int subscriptionId, String msg) throws IOException {
        String externalId = sessionRef.getSessionId();
        log.debug("[{}] Processing {}", externalId, msg);
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
                if (tenantProfileConfiguration != null) {
                    if (StringUtils.isNotEmpty(tenantProfileConfiguration.getWsUpdatesPerSessionRateLimit())) {
                        TbRateLimits rateLimits = perSessionUpdateLimits.computeIfAbsent(sessionRef.getSessionId(), sid -> new TbRateLimits(tenantProfileConfiguration.getWsUpdatesPerSessionRateLimit()));
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
                    } else {
                        perSessionUpdateLimits.remove(sessionRef.getSessionId());
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
    public void sendPing(WebSocketSessionRef sessionRef, long currentTime) throws IOException {
        String externalId = sessionRef.getSessionId();
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                sessionMd.sendPing(currentTime);
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

    @Override
    public void close(WebSocketSessionRef sessionRef, CloseStatus reason) throws IOException {
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

    private boolean checkLimits(WebSocketSession session, WebSocketSessionRef sessionRef) throws Exception {
        var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration == null) {
            return true;
        }

        String sessionId = session.getId();
        if (tenantProfileConfiguration.getMaxWsSessionsPerTenant() > 0) {
            Set<String> tenantSessions = tenantSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
            synchronized (tenantSessions) {
                if (tenantSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerTenant()) {
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
            if (tenantProfileConfiguration.getMaxWsSessionsPerCustomer() > 0) {
                Set<String> customerSessions = customerSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (customerSessions) {
                    if (customerSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerCustomer()) {
                        customerSessions.add(sessionId);
                    } else {
                        log.info("[{}][{}][{}] Failed to start session. Max customer sessions limit reached"
                                , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), sessionId);
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Max customer sessions limit reached"));
                        return false;
                    }
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerRegularUser() > 0
                    && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> regularUserSessions = regularUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (regularUserSessions) {
                    if (regularUserSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerRegularUser()) {
                        regularUserSessions.add(sessionId);
                    } else {
                        log.info("[{}][{}][{}] Failed to start session. Max regular user sessions limit reached"
                                , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), sessionId);
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Max regular user sessions limit reached"));
                        return false;
                    }
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerPublicUser() > 0
                    && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> publicUserSessions = publicUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (publicUserSessions) {
                    if (publicUserSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerPublicUser()) {
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

    private void cleanupLimits(WebSocketSession session, WebSocketSessionRef sessionRef) {
        var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration == null) return;

        String sessionId = session.getId();
        perSessionUpdateLimits.remove(sessionRef.getSessionId());
        blacklistedSessions.remove(sessionRef.getSessionId());
        if (tenantProfileConfiguration.getMaxWsSessionsPerTenant() > 0) {
            Set<String> tenantSessions = tenantSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
            synchronized (tenantSessions) {
                tenantSessions.remove(sessionId);
            }
        }
        if (sessionRef.getSecurityCtx().isCustomerUser()) {
            if (tenantProfileConfiguration.getMaxWsSessionsPerCustomer() > 0) {
                Set<String> customerSessions = customerSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (customerSessions) {
                    customerSessions.remove(sessionId);
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerRegularUser() > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> regularUserSessions = regularUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (regularUserSessions) {
                    regularUserSessions.remove(sessionId);
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerPublicUser() > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> publicUserSessions = publicUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (publicUserSessions) {
                    publicUserSessions.remove(sessionId);
                }
            }
        }
    }

    private DefaultTenantProfileConfiguration getTenantProfileConfiguration(WebSocketSessionRef sessionRef) {
        return Optional.ofNullable(tenantProfileCache.get(sessionRef.getSecurityCtx().getTenantId()))
                .map(TenantProfile::getDefaultProfileConfiguration).orElse(null);
    }

}
