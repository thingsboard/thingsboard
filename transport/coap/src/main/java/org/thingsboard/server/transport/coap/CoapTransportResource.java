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
package org.thingsboard.server.transport.coap;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.ExchangeObserver;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.security.DeviceCredentialsFilter;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.*;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.common.transport.quota.QuotaService;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;
import org.thingsboard.server.transport.coap.session.CoapExchangeObserverProxy;
import org.thingsboard.server.transport.coap.session.CoapSessionCtx;
import org.springframework.util.ReflectionUtils;

@Slf4j
public class CoapTransportResource extends CoapResource {
    // coap://localhost:port/api/v1/DEVICE_TOKEN/[attributes|telemetry|rpc[/requestId]]
    private static final int ACCESS_TOKEN_POSITION = 3;
    private static final int FEATURE_TYPE_POSITION = 4;
    private static final int REQUEST_ID_POSITION = 5;

    private final CoapTransportAdaptor adaptor;
    private final SessionMsgProcessor processor;
    private final DeviceAuthService authService;
    private final QuotaService quotaService;
    private final Field observerField;
    private final long timeout;

    public CoapTransportResource(SessionMsgProcessor processor, DeviceAuthService authService, CoapTransportAdaptor adaptor, String name,
                                 long timeout, QuotaService quotaService) {
        super(name);
        this.processor = processor;
        this.authService = authService;
        this.quotaService = quotaService;
        this.adaptor = adaptor;
        this.timeout = timeout;
        // This is important to turn off existing observable logic in
        // CoapResource. We will have our own observe monitoring due to 1:1
        // observe relationship.
        this.setObservable(false);
        observerField = ReflectionUtils.findField(Exchange.class, "observer");
        observerField.setAccessible(true);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        if(quotaService.isQuotaExceeded(exchange.getSourceAddress().getHostAddress())) {
            log.warn("COAP Quota exceeded for [{}:{}] . Disconnect", exchange.getSourceAddress().getHostAddress(), exchange.getSourcePort());
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        Optional<FeatureType> featureType = getFeatureType(exchange.advanced().getRequest());
        if (!featureType.isPresent()) {
            log.trace("Missing feature type parameter");
            exchange.respond(ResponseCode.BAD_REQUEST);
        } else if (featureType.get() == FeatureType.TELEMETRY) {
            log.trace("Can't fetch/subscribe to timeseries updates");
            exchange.respond(ResponseCode.BAD_REQUEST);
        } else if (exchange.getRequestOptions().hasObserve()) {
            processExchangeGetRequest(exchange, featureType.get());
        } else if (featureType.get() == FeatureType.ATTRIBUTES) {
            processRequest(exchange, SessionMsgType.GET_ATTRIBUTES_REQUEST);
        } else {
            log.trace("Invalid feature type parameter");
            exchange.respond(ResponseCode.BAD_REQUEST);
        }
    }

    private void processExchangeGetRequest(CoapExchange exchange, FeatureType featureType) {
        boolean unsubscribe = exchange.getRequestOptions().getObserve() == 1;
        SessionMsgType sessionMsgType;
        if (featureType == FeatureType.RPC) {
            sessionMsgType = unsubscribe ? SessionMsgType.UNSUBSCRIBE_RPC_COMMANDS_REQUEST : SessionMsgType.SUBSCRIBE_RPC_COMMANDS_REQUEST;
        } else {
            sessionMsgType = unsubscribe ? SessionMsgType.UNSUBSCRIBE_ATTRIBUTES_REQUEST : SessionMsgType.SUBSCRIBE_ATTRIBUTES_REQUEST;
        }
        Optional<SessionId> sessionId = processRequest(exchange, sessionMsgType);
        if (sessionId.isPresent()) {
            if (exchange.getRequestOptions().getObserve() == 1) {
                exchange.respond(ResponseCode.VALID);
            }
        }
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        if(quotaService.isQuotaExceeded(exchange.getSourceAddress().getHostAddress())) {
            log.warn("COAP Quota exceeded for [{}:{}] . Disconnect", exchange.getSourceAddress().getHostAddress(), exchange.getSourcePort());
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        Optional<FeatureType> featureType = getFeatureType(exchange.advanced().getRequest());
        if (!featureType.isPresent()) {
            log.trace("Missing feature type parameter");
            exchange.respond(ResponseCode.BAD_REQUEST);
        } else {
            switch (featureType.get()) {
                case ATTRIBUTES:
                    processRequest(exchange, SessionMsgType.POST_ATTRIBUTES_REQUEST);
                    break;
                case TELEMETRY:
                    processRequest(exchange, SessionMsgType.POST_TELEMETRY_REQUEST);
                    break;
                case RPC:
                    Optional<Integer> requestId = getRequestId(exchange.advanced().getRequest());
                    if (requestId.isPresent()) {
                        processRequest(exchange, SessionMsgType.TO_DEVICE_RPC_RESPONSE);
                    } else {
                        processRequest(exchange, SessionMsgType.TO_SERVER_RPC_REQUEST);
                    }
                    break;
            }
        }
    }

    private Optional<SessionId> processRequest(CoapExchange exchange, SessionMsgType type) {
        log.trace("Processing {}", exchange.advanced().getRequest());
        exchange.accept();
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();

        Optional<DeviceCredentialsFilter> credentials = decodeCredentials(request);
        if (!credentials.isPresent()) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return Optional.empty();
        }

        CoapSessionCtx ctx = new CoapSessionCtx(exchange, adaptor, processor, authService, timeout);

        if (!ctx.login(credentials.get())) {
            exchange.respond(ResponseCode.UNAUTHORIZED);
            return Optional.empty();
        }

        AdaptorToSessionActorMsg msg;
        try {
            switch (type) {
                case GET_ATTRIBUTES_REQUEST:
                case POST_ATTRIBUTES_REQUEST:
                case POST_TELEMETRY_REQUEST:
                case TO_DEVICE_RPC_RESPONSE:
                case TO_SERVER_RPC_REQUEST:
                    ctx.setSessionType(SessionType.SYNC);
                    msg = adaptor.convertToActorMsg(ctx, type, request);
                    break;
                case SUBSCRIBE_ATTRIBUTES_REQUEST:
                case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                    ExchangeObserver systemObserver = (ExchangeObserver) observerField.get(advanced);
                    advanced.setObserver(new CoapExchangeObserverProxy(systemObserver, ctx));
                    ctx.setSessionType(SessionType.ASYNC);
                    msg = adaptor.convertToActorMsg(ctx, type, request);
                    break;
                case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                    ctx.setSessionType(SessionType.ASYNC);
                    msg = adaptor.convertToActorMsg(ctx, type, request);
                    break;
                default:
                    log.trace("[{}] Unsupported msg type: {}", ctx.getSessionId(), type);
                    throw new IllegalArgumentException("Unsupported msg type: " + type);
            }
            log.trace("Processing msg: {}", msg);
            processor.process(new BasicTransportToDeviceSessionActorMsg(ctx.getDevice(), msg));
        } catch (AdaptorException e) {
            log.debug("Failed to decode payload {}", e);
            exchange.respond(ResponseCode.BAD_REQUEST, e.getMessage());
            return Optional.empty();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            log.debug("Failed to process payload {}", e);
            exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return Optional.of(ctx.getSessionId());
    }

    private Optional<DeviceCredentialsFilter> decodeCredentials(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        DeviceCredentialsFilter credentials = null;
        if (uriPath.size() >= ACCESS_TOKEN_POSITION) {
            credentials = new DeviceTokenCredentials(uriPath.get(ACCESS_TOKEN_POSITION - 1));
        }
        return Optional.ofNullable(credentials);
    }

    private Optional<FeatureType> getFeatureType(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        try {
            if (uriPath.size() >= FEATURE_TYPE_POSITION) {
                return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION - 1).toUpperCase()));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to decode feature type: {}", uriPath);
        }
        return Optional.empty();
    }

    public static Optional<Integer> getRequestId(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        try {
            if (uriPath.size() >= REQUEST_ID_POSITION) {
                return Optional.of(Integer.valueOf(uriPath.get(REQUEST_ID_POSITION - 1)));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to decode feature type: {}", uriPath);
        }
        return Optional.empty();
    }

    @Override
    public Resource getChild(String name) {
        return this;
    }

}
