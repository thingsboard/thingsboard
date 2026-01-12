/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.config.WebSocketConfiguration;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;
import org.thingsboard.server.service.ws.AuthCmd;
import org.thingsboard.server.service.ws.SessionEvent;
import org.thingsboard.server.service.ws.WebSocketMsgEndpoint;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.WebSocketSessionType;
import org.thingsboard.server.service.ws.WsCommandsWrapper;
import org.thingsboard.server.service.ws.notification.cmd.NotificationCmdsWrapper;
import org.thingsboard.server.service.ws.telemetry.cmd.TelemetryCmdsWrapper;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.service.ws.DefaultWebSocketService.NUMBER_OF_PING_ATTEMPTS;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class TbWebSocketHandler extends TextWebSocketHandler implements WebSocketMsgEndpoint {

    private final ConcurrentMap<String, SessionMetaData> internalSessionMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> externalSessionMap = new ConcurrentHashMap<>();

    @Autowired @Lazy
    private WebSocketService webSocketService;
    @Autowired
    private TbTenantProfileCache tenantProfileCache;
    @Autowired
    private RateLimitService rateLimitService;
    @Autowired
    private JwtAuthenticationProvider authenticationProvider;

    @Value("${server.ws.send_timeout:5000}")
    private long sendTimeout;
    @Value("${server.ws.ping_timeout:30000}")
    private long pingTimeout;
    @Value("${server.ws.max_queue_messages_per_session:1000}")
    private int wsMaxQueueMessagesPerSession;
    @Value("${server.ws.auth_timeout_ms:10000}")
    private int authTimeoutMs;

    private final ConcurrentMap<String, WebSocketSessionRef> blacklistedSessions = new ConcurrentHashMap<>();

    private final ConcurrentMap<TenantId, Set<String>> tenantSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<CustomerId, Set<String>> customerSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, Set<String>> regularUserSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, Set<String>> publicUserSessionsMap = new ConcurrentHashMap<>();

    private Cache<String, SessionMetaData> pendingSessions;

    @PostConstruct
    private void init() {
        pendingSessions = Caffeine.newBuilder()
                .expireAfterWrite(authTimeoutMs, TimeUnit.MILLISECONDS)
                .<String, SessionMetaData>removalListener((sessionId, sessionMd, removalCause) -> {
                    if (removalCause == RemovalCause.EXPIRED && sessionMd != null) {
                        try {
                            close(sessionMd.sessionRef, CloseStatus.POLICY_VIOLATION);
                        } catch (IOException e) {
                            log.warn("IO error", e);
                        }
                    }
                })
                .build();
    }

    @PreDestroy
    private void stop() {
        internalSessionMap.clear();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SessionMetaData sessionMd = getSessionMd(session.getId());
            if (sessionMd == null) {
                log.trace("[{}] Failed to find session", session.getId());
                session.close(CloseStatus.SERVER_ERROR.withReason("Session not found!"));
                return;
            }
            sessionMd.onMsg(message.getPayload());
        } catch (IOException e) {
            log.warn("IO error", e);
        }
    }

    void processMsg(SessionMetaData sessionMd, String msg) throws IOException {
        WebSocketSessionRef sessionRef = sessionMd.sessionRef;
        WsCommandsWrapper cmdsWrapper;
        try {
            switch (sessionRef.getSessionType()) {
                case GENERAL:
                    cmdsWrapper = JacksonUtil.fromString(msg, WsCommandsWrapper.class);
                    break;
                case TELEMETRY:
                    cmdsWrapper = JacksonUtil.fromString(msg, TelemetryCmdsWrapper.class).toCommonCmdsWrapper();
                    break;
                case NOTIFICATIONS:
                    cmdsWrapper = JacksonUtil.fromString(msg, NotificationCmdsWrapper.class).toCommonCmdsWrapper();
                    break;
                default:
                    return;
            }
        } catch (Exception e) {
            log.debug("{} Failed to decode subscription cmd: {}", sessionRef, e.getMessage(), e);
            if (sessionRef.getSecurityCtx() != null) {
                webSocketService.sendError(sessionRef, 1, SubscriptionErrorCode.BAD_REQUEST, "Failed to parse the payload");
            } else {
                close(sessionRef, CloseStatus.BAD_DATA.withReason(e.getMessage()));
            }
            return;
        }

        if (sessionRef.getSecurityCtx() != null) {
            log.trace("{} Processing {}", sessionRef, msg);
            webSocketService.handleCommands(sessionRef, cmdsWrapper);
        } else {
            AuthCmd authCmd = cmdsWrapper.getAuthCmd();
            if (authCmd == null) {
                close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Auth cmd is missing"));
                return;
            }
            log.trace("{} Authenticating session", sessionRef);
            SecurityUser securityCtx;
            try {
                securityCtx = authenticationProvider.authenticate(authCmd.getToken());
            } catch (Exception e) {
                close(sessionRef, CloseStatus.BAD_DATA.withReason(e.getMessage()));
                return;
            }
            sessionRef.setSecurityCtx(securityCtx);
            pendingSessions.invalidate(sessionMd.session.getId());
            establishSession(sessionMd.session, sessionRef, sessionMd);

            webSocketService.handleCommands(sessionRef, cmdsWrapper);
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        try {
            SessionMetaData sessionMd = getSessionMd(session.getId());
            if (sessionMd != null) {
                log.trace("{} Processing pong response {}", sessionMd.sessionRef, message.getPayload());
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
            WebSocketSessionRef sessionRef = toRef(session);
            log.debug("[{}][{}] Session opened from address: {}", sessionRef.getSessionId(), session.getId(), session.getRemoteAddress());
            establishSession(session, sessionRef, null);
        } catch (InvalidParameterException e) {
            log.warn("[{}] Failed to start session", session.getId(), e);
            session.close(CloseStatus.BAD_DATA.withReason(e.getMessage()));
        } catch (JwtExpiredTokenException e) {
            log.trace("[{}] Failed to start session", session.getId(), e);
            session.close(CloseStatus.SERVER_ERROR.withReason(e.getMessage()));
        } catch (Exception e) {
            log.warn("[{}] Failed to start session", session.getId(), e);
            session.close(CloseStatus.SERVER_ERROR.withReason(e.getMessage()));
        }
    }

    private void establishSession(WebSocketSession session, WebSocketSessionRef sessionRef, SessionMetaData sessionMd) throws IOException {
        if (sessionRef.getSecurityCtx() != null) {
            if (!checkLimits(session, sessionRef)) {
                return;
            }
            int maxMsgQueueSize = Optional.ofNullable(getTenantProfileConfiguration(sessionRef))
                    .map(DefaultTenantProfileConfiguration::getWsMsgQueueLimitPerSession)
                    .filter(profileLimit -> profileLimit > 0 && profileLimit < wsMaxQueueMessagesPerSession)
                    .orElse(wsMaxQueueMessagesPerSession);
            if (sessionMd == null) {
                sessionMd = new SessionMetaData(session, sessionRef);
            }
            sessionMd.setMaxMsgQueueSize(maxMsgQueueSize);

            internalSessionMap.put(session.getId(), sessionMd);
            externalSessionMap.put(sessionRef.getSessionId(), session.getId());
            processInWebSocketService(sessionRef, SessionEvent.onEstablished());
            log.info("[{}][{}][{}][{}] Session established from address: {}", sessionRef.getSecurityCtx().getTenantId(),
                    sessionRef.getSecurityCtx().getId(), sessionRef.getSessionId(), session.getId(), session.getRemoteAddress());
        } else {
            sessionMd = new SessionMetaData(session, sessionRef);
            pendingSessions.put(session.getId(), sessionMd);
            externalSessionMap.put(sessionRef.getSessionId(), session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable tError) throws Exception {
        super.handleTransportError(session, tError);
        SessionMetaData sessionMd = getSessionMd(session.getId());
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
        if (sessionMd == null) {
            sessionMd = pendingSessions.asMap().remove(session.getId());
        }
        if (sessionMd != null) {
            externalSessionMap.remove(sessionMd.sessionRef.getSessionId());
            if (sessionMd.sessionRef.getSecurityCtx() != null) {
                cleanupLimits(session, sessionMd.sessionRef);
                processInWebSocketService(sessionMd.sessionRef, SessionEvent.onClosed());
            }
            log.info("{} Session is closed", sessionMd.sessionRef);
        } else {
            log.info("[{}] Session is closed", session.getId());
        }
    }

    private void processInWebSocketService(WebSocketSessionRef sessionRef, SessionEvent event) {
        if (sessionRef.getSecurityCtx() == null) {
            return;
        }
        try {
            webSocketService.handleSessionEvent(sessionRef, event);
        } catch (BeanCreationNotAllowedException e) {
            log.warn("{} Failed to close session due to possible shutdown state", sessionRef);
        }
    }

    private WebSocketSessionRef toRef(WebSocketSession session) {
        String path = session.getUri().getPath();
        WebSocketSessionType sessionType;
        if (path.equals(WebSocketConfiguration.WS_API_ENDPOINT)) {
            sessionType = WebSocketSessionType.GENERAL;
        } else {
            String type = StringUtils.substringAfter(path, WebSocketConfiguration.WS_PLUGINS_ENDPOINT);
            sessionType = WebSocketSessionType.forName(type)
                    .orElseThrow(() -> new InvalidParameterException("Unknown session type"));
        }

        SecurityUser securityCtx = null;
        String token = StringUtils.substringAfter(session.getUri().getQuery(), "token=");
        if (StringUtils.isNotEmpty(token)) {
            securityCtx = authenticationProvider.authenticate(token);
        }
        return WebSocketSessionRef.builder()
                .sessionId(UUID.randomUUID().toString())
                .securityCtx(securityCtx)
                .localAddress(session.getLocalAddress())
                .remoteAddress(session.getRemoteAddress())
                .sessionType(sessionType)
                .build();
    }

    private SessionMetaData getSessionMd(String internalSessionId) {
        SessionMetaData sessionMd = internalSessionMap.get(internalSessionId);
        if (sessionMd == null) {
            sessionMd = pendingSessions.getIfPresent(internalSessionId);
        }
        return sessionMd;
    }

    class SessionMetaData implements SendHandler {
        private final WebSocketSession session;
        private final RemoteEndpoint.Async asyncRemote;
        private final WebSocketSessionRef sessionRef;

        final AtomicBoolean isSending = new AtomicBoolean(false);
        private final Queue<TbWebSocketMsg<?>> outboundMsgQueue = new ConcurrentLinkedQueue<>();
        private final AtomicInteger outboundMsgQueueSize = new AtomicInteger();
        @Setter
        private int maxMsgQueueSize = wsMaxQueueMessagesPerSession;

        private final Queue<String> inboundMsgQueue = new ConcurrentLinkedQueue<>();
        private final Lock inboundMsgQueueProcessorLock = new ReentrantLock();

        private volatile long lastActivityTime;

        SessionMetaData(WebSocketSession session, WebSocketSessionRef sessionRef) {
            super();
            this.session = session;
            Session nativeSession = ((NativeWebSocketSession) session).getNativeSession(Session.class);
            this.asyncRemote = nativeSession.getAsyncRemote();
            this.sessionRef = sessionRef;
            this.lastActivityTime = System.currentTimeMillis();
        }

        void sendPing(long currentTime) {
            try {
                long timeSinceLastActivity = currentTime - lastActivityTime;
                if (timeSinceLastActivity >= pingTimeout) {
                    log.warn("{} Closing session due to ping timeout", sessionRef);
                    closeSession(CloseStatus.SESSION_NOT_RELIABLE);
                } else if (timeSinceLastActivity >= pingTimeout / NUMBER_OF_PING_ATTEMPTS) {
                    sendMsg(TbWebSocketPingMsg.INSTANCE);
                }
            } catch (Exception e) {
                log.trace("{} Failed to send ping msg", sessionRef, e);
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
            }
        }

        void closeSession(CloseStatus reason) {
            try {
                close(this.sessionRef, reason);
            } catch (IOException ioe) {
                log.trace("{} Session transport error", sessionRef, ioe);
            } finally {
                outboundMsgQueue.clear();
            }
        }

        void processPongMessage(long currentTime) {
            lastActivityTime = currentTime;
        }

        void sendMsg(String msg) {
            sendMsg(new TbWebSocketTextMsg(msg));
        }

        void sendMsg(TbWebSocketMsg<?> msg) {
            if (outboundMsgQueueSize.get() < maxMsgQueueSize) {
                outboundMsgQueue.add(msg);
                outboundMsgQueueSize.incrementAndGet();
                processNextMsg();
            } else {
                log.info("{} Session closed due to updates queue size exceeded", sessionRef);
                closeSession(CloseStatus.POLICY_VIOLATION.withReason("Max pending updates limit reached!"));
            }
        }

        private void sendMsgInternal(TbWebSocketMsg<?> msg) {
            try {
                if (TbWebSocketMsgType.TEXT.equals(msg.getType())) {
                    TbWebSocketTextMsg textMsg = (TbWebSocketTextMsg) msg;
                    this.asyncRemote.sendText(textMsg.getMsg(), this);
                    // isSending status will be reset in the onResult method by call back
                } else {
                    TbWebSocketPingMsg pingMsg = (TbWebSocketPingMsg) msg;
                    this.asyncRemote.sendPing(pingMsg.getMsg()); // blocking call
                    isSending.set(false);
                    processNextMsg();
                }
            } catch (Exception e) {
                log.trace("{} Failed to send msg", sessionRef, e);
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
            }
        }

        @Override
        public void onResult(SendResult result) {
            if (!result.isOK()) {
                log.trace("{} Failed to send msg", sessionRef, result.getException());
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
                return;
            }

            isSending.set(false);
            processNextMsg();
        }

        private void processNextMsg() {
            if (outboundMsgQueue.isEmpty() || !isSending.compareAndSet(false, true)) {
                return;
            }
            TbWebSocketMsg<?> msg = outboundMsgQueue.poll();
            if (msg != null) {
                outboundMsgQueueSize.decrementAndGet();
                sendMsgInternal(msg);
            } else {
                isSending.set(false);
            }
        }

        public void onMsg(String msg) throws IOException {
            inboundMsgQueue.add(msg);
            tryProcessInboundMsgs();
        }

        void tryProcessInboundMsgs() throws IOException {
            while (!inboundMsgQueue.isEmpty()) {
                if (inboundMsgQueueProcessorLock.tryLock()) {
                    try {
                        String msg;
                        while ((msg = inboundMsgQueue.poll()) != null) {
                            processMsg(this, msg);
                        }
                    } finally {
                        inboundMsgQueueProcessorLock.unlock();
                    }
                } else {
                    return;
                }
            }
        }
    }

    @Override
    public void send(WebSocketSessionRef sessionRef, int subscriptionId, String msg) throws IOException {
        log.debug("{} Sending {}", sessionRef, msg);
        String externalId = sessionRef.getSessionId();
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                TenantId tenantId = sessionRef.getSecurityCtx().getTenantId();
                if (!rateLimitService.checkRateLimit(LimitedApi.WS_UPDATES_PER_SESSION, tenantId, (Object) sessionRef.getSessionId())) {
                    if (blacklistedSessions.putIfAbsent(externalId, sessionRef) == null) {
                        log.info("{} Failed to process session update. Max session updates limit reached", sessionRef);
                        sessionMd.sendMsg("{\"subscriptionId\":" + subscriptionId + ", \"errorCode\":" + ThingsboardErrorCode.TOO_MANY_UPDATES.getErrorCode() + ", \"errorMsg\":\"Too many updates!\"}");
                    }
                    return;
                } else {
                    log.debug("{} Session is no longer blacklisted.", sessionRef);
                    blacklistedSessions.remove(externalId);
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
        log.debug("{} Processing close request", sessionRef.toString());
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = getSessionMd(internalId);
            if (sessionMd != null) {
                sessionMd.session.close(reason);
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

    @Override
    public boolean isOpen(String externalId) {
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = getSessionMd(internalId);
            if (sessionMd != null) {
                return sessionMd.session.isOpen();
            }
        }
        return false;
    }

    private boolean checkLimits(WebSocketSession session, WebSocketSessionRef sessionRef) throws IOException {
        var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration == null) {
            return true;
        }
        boolean limitAllowed;
        String sessionId = session.getId();
        if (tenantProfileConfiguration.getMaxWsSessionsPerTenant() > 0) {
            Set<String> tenantSessions = tenantSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
            synchronized (tenantSessions) {
                limitAllowed = tenantSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerTenant();
                if (limitAllowed) {
                    tenantSessions.add(sessionId);
                }
            }
            if (!limitAllowed) {
                log.info("{} Failed to start session. Max tenant sessions limit reached", sessionRef.toString());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Max tenant sessions limit reached!"));
                return false;
            }
        }

        if (sessionRef.getSecurityCtx().isCustomerUser()) {
            if (tenantProfileConfiguration.getMaxWsSessionsPerCustomer() > 0) {
                Set<String> customerSessions = customerSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (customerSessions) {
                    limitAllowed = customerSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerCustomer();
                    if (limitAllowed) {
                        customerSessions.add(sessionId);
                    }
                }
                if (!limitAllowed) {
                    log.info("{} Failed to start session. Max customer sessions limit reached", sessionRef.toString());
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Max customer sessions limit reached"));
                    return false;
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerRegularUser() > 0
                    && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> regularUserSessions = regularUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (regularUserSessions) {
                    limitAllowed = regularUserSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerRegularUser();
                    if (limitAllowed) {
                        regularUserSessions.add(sessionId);
                    }
                }
                if (!limitAllowed) {
                    log.info("{} Failed to start session. Max regular user sessions limit reached", sessionRef.toString());
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Max regular user sessions limit reached"));
                    return false;
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerPublicUser() > 0
                    && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> publicUserSessions = publicUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (publicUserSessions) {
                    limitAllowed = publicUserSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerPublicUser();
                    if (limitAllowed) {
                        publicUserSessions.add(sessionId);
                    }
                }
                if (!limitAllowed) {
                    log.info("{} Failed to start session. Max public user sessions limit reached", sessionRef.toString());
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Max public user sessions limit reached"));
                    return false;
                }
            }
        }
        return true;
    }

    private void cleanupLimits(WebSocketSession session, WebSocketSessionRef sessionRef) {
        var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration == null) return;

        String sessionId = session.getId();
        rateLimitService.cleanUp(LimitedApi.WS_UPDATES_PER_SESSION, sessionRef.getSessionId());
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
