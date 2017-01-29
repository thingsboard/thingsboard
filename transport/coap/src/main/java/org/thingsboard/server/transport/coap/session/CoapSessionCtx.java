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
package org.thingsboard.server.transport.coap.session;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.session.SessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.SessionCtrlMsg;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.msg.session.ex.SessionAuthException;
import org.thingsboard.server.common.msg.session.ex.SessionException;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CoapSessionCtx extends DeviceAwareSessionContext {

    private final SessionId sessionId;
    private final CoapExchange exchange;
    private final CoapTransportAdaptor adaptor;
    private final String token;
    private final long timeout;
    private SessionType sessionType;
    private final AtomicInteger seqNumber = new AtomicInteger(2);

    public CoapSessionCtx(CoapExchange exchange, CoapTransportAdaptor adaptor, SessionMsgProcessor processor, DeviceAuthService authService, long timeout) {
        super(processor, authService);
        Request request = exchange.advanced().getRequest();
        this.token = request.getTokenString();
        this.sessionId = new CoapSessionId(request.getSource().getHostAddress(), request.getSourcePort(), this.token);
        this.exchange = exchange;
        this.adaptor = adaptor;
        this.timeout = timeout;
    }


    @Override
    public void onMsg(SessionActorToAdaptorMsg msg) throws SessionException {
        try {
            adaptor.convertToAdaptorMsg(this, msg).ifPresent(this::pushToNetwork);
        } catch (AdaptorException e) {
            logAndWrap(e);
        }
    }

    private void pushToNetwork(Response response) {
        exchange.respond(response);
    }

    private void logAndWrap(AdaptorException e) throws SessionException {
        log.warn("Failed to convert msg: {}", e.getMessage(), e);
        throw new SessionException(e);
    }

    @Override
    public void onMsg(SessionCtrlMsg msg) throws SessionException {
        log.debug("[{}] onCtrl: {}", sessionId, msg);
        if (msg instanceof SessionCloseMsg) {
            onSessionClose((SessionCloseMsg) msg);
        }
    }

    private void onSessionClose(SessionCloseMsg msg) {
        if (msg.isTimeout()) {
            exchange.respond(ResponseCode.SERVICE_UNAVAILABLE);
        } else if (msg.isCredentialsRevoked()) {
            exchange.respond(ResponseCode.UNAUTHORIZED);
        } else {
            exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return "CoapSessionCtx [sessionId=" + sessionId + "]";
    }

    @Override
    public boolean isClosed() {
        return exchange.advanced().isComplete() || exchange.advanced().isTimedOut();
    }

    public void close() {
        log.info("[{}] Closing processing context. Timeout: {}", sessionId, exchange.advanced().isTimedOut());
        processor.process(exchange.advanced().isTimedOut() ? SessionCloseMsg.onTimeout(sessionId) : SessionCloseMsg.onError(sessionId));
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    @Override
    public SessionType getSessionType() {
        return sessionType;
    }

    public int nextSeqNumber() {
        return seqNumber.getAndIncrement();
    }
}
