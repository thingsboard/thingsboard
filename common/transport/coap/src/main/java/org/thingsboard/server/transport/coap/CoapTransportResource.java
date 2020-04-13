/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.ExchangeObserver;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.springframework.util.ReflectionUtils;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class CoapTransportResource extends CoapResource {
    // coap://localhost:port/api/v1/DEVICE_TOKEN/[attributes|telemetry|rpc[/requestId]]
    private static final int ACCESS_TOKEN_POSITION = 3;
    private static final int FEATURE_TYPE_POSITION = 4;
    private static final int REQUEST_ID_POSITION = 5;

    private final CoapTransportContext transportContext;
    private final TransportService transportService;
    private final Field observerField;
    private final long timeout;
    private final ConcurrentMap<String, TransportProtos.SessionInfoProto> tokenToSessionIdMap = new ConcurrentHashMap<>();

    public CoapTransportResource(CoapTransportContext context, String name) {
        super(name);
        this.transportContext = context;
        this.transportService = context.getTransportService();
        this.timeout = context.getTimeout();
        // This is important to turn off existing observable logic in
        // CoapResource. We will have our own observe monitoring due to 1:1
        // observe relationship.
        this.setObservable(false);
        observerField = ReflectionUtils.findField(Exchange.class, "observer");
        observerField.setAccessible(true);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
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
        processRequest(exchange, sessionMsgType);
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
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
                case CLAIM:
                    processRequest(exchange, SessionMsgType.CLAIM_REQUEST);
                    break;
            }
        }
    }

    private void processRequest(CoapExchange exchange, SessionMsgType type) {
        log.trace("Processing {}", exchange.advanced().getRequest());
        exchange.accept();
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();

        Optional<DeviceTokenCredentials> credentials = decodeCredentials(request);
        if (!credentials.isPresent()) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        transportService.process(TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.get().getCredentialsId()).build(),
                new DeviceAuthCallback(transportContext, exchange, sessionInfo -> {
                    UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
                    try {
                        switch (type) {
                            case POST_ATTRIBUTES_REQUEST:
                                transportService.process(sessionInfo,
                                        transportContext.getAdaptor().convertToPostAttributes(sessionId, request),
                                        new CoapOkCallback(exchange));
                                break;
                            case POST_TELEMETRY_REQUEST:
                                transportService.process(sessionInfo,
                                        transportContext.getAdaptor().convertToPostTelemetry(sessionId, request),
                                        new CoapOkCallback(exchange));
                                break;
                            case CLAIM_REQUEST:
                                transportService.process(sessionInfo,
                                        transportContext.getAdaptor().convertToClaimDevice(sessionId, request, sessionInfo),
                                        new CoapOkCallback(exchange));
                                break;
                            case SUBSCRIBE_ATTRIBUTES_REQUEST:
                                advanced.setObserver(new CoapExchangeObserverProxy((ExchangeObserver) observerField.get(advanced),
                                        registerAsyncCoapSession(exchange, request, sessionInfo, sessionId)));
                                transportService.process(sessionInfo,
                                        TransportProtos.SubscribeToAttributeUpdatesMsg.getDefaultInstance(),
                                        new CoapNoOpCallback(exchange));
                                break;
                            case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                                TransportProtos.SessionInfoProto attrSession = lookupAsyncSessionInfo(request);
                                if (attrSession != null) {
                                    transportService.process(attrSession,
                                            TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setUnsubscribe(true).build(),
                                            new CoapOkCallback(exchange));
                                    closeAndDeregister(sessionInfo);
                                }
                                break;
                            case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                                advanced.setObserver(new CoapExchangeObserverProxy((ExchangeObserver) observerField.get(advanced),
                                        registerAsyncCoapSession(exchange, request, sessionInfo, sessionId)));
                                transportService.process(sessionInfo,
                                        TransportProtos.SubscribeToRPCMsg.getDefaultInstance(),
                                        new CoapNoOpCallback(exchange));
                                break;
                            case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                                TransportProtos.SessionInfoProto rpcSession = lookupAsyncSessionInfo(request);
                                if (rpcSession != null) {
                                    transportService.process(rpcSession,
                                            TransportProtos.SubscribeToRPCMsg.newBuilder().setUnsubscribe(true).build(),
                                            new CoapOkCallback(exchange));
                                    transportService.process(sessionInfo, getSessionEventMsg(TransportProtos.SessionEvent.CLOSED), null);
                                    transportService.deregisterSession(rpcSession);
                                }
                                break;
                            case TO_DEVICE_RPC_RESPONSE:
                                transportService.process(sessionInfo,
                                        transportContext.getAdaptor().convertToDeviceRpcResponse(sessionId, request),
                                        new CoapOkCallback(exchange));
                                break;
                            case TO_SERVER_RPC_REQUEST:
                                transportService.registerSyncSession(sessionInfo, new CoapSessionListener(sessionId, exchange), transportContext.getTimeout());
                                transportService.process(sessionInfo,
                                        transportContext.getAdaptor().convertToServerRpcRequest(sessionId, request),
                                        new CoapNoOpCallback(exchange));
                                break;
                            case GET_ATTRIBUTES_REQUEST:
                                transportService.registerSyncSession(sessionInfo, new CoapSessionListener(sessionId, exchange), transportContext.getTimeout());
                                transportService.process(sessionInfo,
                                        transportContext.getAdaptor().convertToGetAttributes(sessionId, request),
                                        new CoapNoOpCallback(exchange));
                                break;
                        }
                    } catch (AdaptorException e) {
                        log.trace("[{}] Failed to decode message: ", sessionId, e);
                        exchange.respond(ResponseCode.BAD_REQUEST);
                    } catch (IllegalAccessException e) {
                        log.trace("[{}] Failed to process message: ", sessionId, e);
                        exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
                    }
                }));
    }

    private TransportProtos.SessionInfoProto lookupAsyncSessionInfo(Request request) {
        String token = request.getSource().getHostAddress() + ":" + request.getSourcePort() + ":" + request.getTokenString();
        return tokenToSessionIdMap.remove(token);
    }

    private String registerAsyncCoapSession(CoapExchange exchange, Request request, TransportProtos.SessionInfoProto sessionInfo, UUID sessionId) {
        String token = request.getSource().getHostAddress() + ":" + request.getSourcePort() + ":" + request.getTokenString();
        tokenToSessionIdMap.putIfAbsent(token, sessionInfo);
        CoapSessionListener attrListener = new CoapSessionListener(sessionId, exchange);
        transportService.registerAsyncSession(sessionInfo, attrListener);
        transportService.process(sessionInfo, getSessionEventMsg(TransportProtos.SessionEvent.OPEN), null);
        return token;
    }

    private static TransportProtos.SessionEventMsg getSessionEventMsg(TransportProtos.SessionEvent event) {
        return TransportProtos.SessionEventMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .setEvent(event).build();
    }

    private Optional<DeviceTokenCredentials> decodeCredentials(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() >= ACCESS_TOKEN_POSITION) {
            return Optional.of(new DeviceTokenCredentials(uriPath.get(ACCESS_TOKEN_POSITION - 1)));
        } else {
            return Optional.empty();
        }
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

    private static class DeviceAuthCallback implements TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg> {
        private final TransportContext transportContext;
        private final CoapExchange exchange;
        private final Consumer<TransportProtos.SessionInfoProto> onSuccess;

        DeviceAuthCallback(TransportContext transportContext, CoapExchange exchange, Consumer<TransportProtos.SessionInfoProto> onSuccess) {
            this.transportContext = transportContext;
            this.exchange = exchange;
            this.onSuccess = onSuccess;
        }

        @Override
        public void onSuccess(TransportProtos.ValidateDeviceCredentialsResponseMsg msg) {
            if (msg.hasDeviceInfo()) {
                UUID sessionId = UUID.randomUUID();
                TransportProtos.DeviceInfoProto deviceInfoProto = msg.getDeviceInfo();
                TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                        .setNodeId(transportContext.getNodeId())
                        .setTenantIdMSB(deviceInfoProto.getTenantIdMSB())
                        .setTenantIdLSB(deviceInfoProto.getTenantIdLSB())
                        .setDeviceIdMSB(deviceInfoProto.getDeviceIdMSB())
                        .setDeviceIdLSB(deviceInfoProto.getDeviceIdLSB())
                        .setSessionIdMSB(sessionId.getMostSignificantBits())
                        .setSessionIdLSB(sessionId.getLeastSignificantBits())
                        .setDeviceName(msg.getDeviceInfo().getDeviceName())
                        .setDeviceType(msg.getDeviceInfo().getDeviceType())
                        .build();
                onSuccess.accept(sessionInfo);
            } else {
                exchange.respond(ResponseCode.UNAUTHORIZED);
            }
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private static class CoapOkCallback implements TransportServiceCallback<Void> {
        private final CoapExchange exchange;

        CoapOkCallback(CoapExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void onSuccess(Void msg) {
            exchange.respond(ResponseCode.VALID);
        }

        @Override
        public void onError(Throwable e) {
            exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private static class CoapNoOpCallback implements TransportServiceCallback<Void> {
        private final CoapExchange exchange;

        CoapNoOpCallback(CoapExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void onSuccess(Void msg) {

        }

        @Override
        public void onError(Throwable e) {
            exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    public class CoapSessionListener implements SessionMsgListener {

        private final CoapExchange exchange;
        private final AtomicInteger seqNumber = new AtomicInteger(2);

        CoapSessionListener(UUID sessionId, CoapExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg msg) {
            try {
                exchange.respond(transportContext.getAdaptor().convertToPublish(this, msg));
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Override
        public void onAttributeUpdate(TransportProtos.AttributeUpdateNotificationMsg msg) {
            try {
                exchange.respond(transportContext.getAdaptor().convertToPublish(this, msg));
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Override
        public void onRemoteSessionCloseCommand(TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
            exchange.respond(ResponseCode.SERVICE_UNAVAILABLE);
        }

        @Override
        public void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg msg) {
            try {
                exchange.respond(transportContext.getAdaptor().convertToPublish(this, msg));
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Override
        public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg msg) {
            try {
                exchange.respond(transportContext.getAdaptor().convertToPublish(this, msg));
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

        public int getNextSeqNumber() {
            return seqNumber.getAndIncrement();
        }
    }

    public class CoapExchangeObserverProxy implements ExchangeObserver {

        private final ExchangeObserver proxy;
        private final String token;

        CoapExchangeObserverProxy(ExchangeObserver proxy, String token) {
            super();
            this.proxy = proxy;
            this.token = token;
        }

        @Override
        public void completed(Exchange exchange) {
            proxy.completed(exchange);
            TransportProtos.SessionInfoProto session = tokenToSessionIdMap.remove(token);
            if (session != null) {
                closeAndDeregister(session);
            }
        }
    }

    private void closeAndDeregister(TransportProtos.SessionInfoProto session) {
        transportService.process(session, getSessionEventMsg(TransportProtos.SessionEvent.CLOSED), null);
        transportService.deregisterSession(session);
    }

}
