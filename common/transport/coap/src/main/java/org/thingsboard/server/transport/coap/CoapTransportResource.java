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
package org.thingsboard.server.transport.coap;

import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceObserver;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.coapserver.TbCoapDtlsSessionKey;
import org.thingsboard.server.coapserver.TbCoapDtlsSessionInfo;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.callback.CoapDeviceAuthCallback;
import org.thingsboard.server.transport.coap.callback.CoapNoOpCallback;
import org.thingsboard.server.transport.coap.callback.CoapResponseCodeCallback;
import org.thingsboard.server.transport.coap.callback.GetAttributesSyncSessionCallback;
import org.thingsboard.server.transport.coap.callback.ToServerRpcSyncSessionCallback;
import org.thingsboard.server.transport.coap.client.CoapClientContext;
import org.thingsboard.server.transport.coap.client.TbCoapClientState;

import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.security.cert.X509Certificate;

import static org.eclipse.californium.elements.DtlsEndpointContext.KEY_SESSION_ID;

@Slf4j
public class CoapTransportResource extends AbstractCoapTransportResource {
    private static final int ACCESS_TOKEN_POSITION = 3;
    private static final int FEATURE_TYPE_POSITION = 4;
    private static final int REQUEST_ID_POSITION = 5;

    private static final int FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST = 3;
    private static final int REQUEST_ID_POSITION_CERTIFICATE_REQUEST = 4;

    private final ConcurrentMap<TbCoapDtlsSessionKey, TbCoapDtlsSessionInfo> dtlsSessionsMap;
    private final long timeout;
    private final long piggybackTimeout;
    private final CoapClientContext clients;

    public CoapTransportResource(CoapTransportContext ctx, CoapServerService coapServerService, String name) {
        super(ctx, name);
        this.setObservable(true); // enable observing
        this.addObserver(new CoapResourceObserver());
        this.dtlsSessionsMap = coapServerService.getDtlsSessionsMap();
        this.timeout = ctx.getTimeout();
        this.piggybackTimeout = ctx.getPiggybackTimeout();
        this.clients = ctx.getClientContext();
        long sessionReportTimeout = ctx.getSessionReportTimeout();
        ctx.getScheduler().scheduleAtFixedRate(clients::reportActivity, new Random().nextInt((int) sessionReportTimeout), sessionReportTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void processHandleGet(CoapExchange exchange) {
        Optional<FeatureType> featureType = getFeatureType(exchange.advanced().getRequest());
        if (featureType.isEmpty()) {
            log.trace("Missing feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else if (featureType.get() == FeatureType.TELEMETRY) {
            log.trace("Can't fetch/subscribe to timeseries updates");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else if (exchange.getRequestOptions().hasObserve()) {
            processExchangeGetRequest(exchange, featureType.get());
        } else if (featureType.get() == FeatureType.ATTRIBUTES) {
            processRequest(exchange, CoapSessionMsgType.GET_ATTRIBUTES_REQUEST);
        } else {
            log.trace("Invalid feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    private void processExchangeGetRequest(CoapExchange exchange, FeatureType featureType) {
        boolean unsubscribe = exchange.getRequestOptions().getObserve() == 1;
        CoapSessionMsgType coapSessionMsgType;
        if (featureType == FeatureType.RPC) {
            coapSessionMsgType = unsubscribe ? CoapSessionMsgType.UNSUBSCRIBE_RPC_COMMANDS_REQUEST : CoapSessionMsgType.SUBSCRIBE_RPC_COMMANDS_REQUEST;
        } else {
            coapSessionMsgType = unsubscribe ? CoapSessionMsgType.UNSUBSCRIBE_ATTRIBUTES_REQUEST : CoapSessionMsgType.SUBSCRIBE_ATTRIBUTES_REQUEST;
        }
        processRequest(exchange, coapSessionMsgType);
    }

    @Override
    protected void processHandlePost(CoapExchange exchange) {
        Optional<FeatureType> featureType = getFeatureType(exchange.advanced().getRequest());
        if (featureType.isEmpty()) {
            log.trace("Missing feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else {
            switch (featureType.get()) {
                case ATTRIBUTES:
                    processRequest(exchange, CoapSessionMsgType.POST_ATTRIBUTES_REQUEST);
                    break;
                case TELEMETRY:
                    processRequest(exchange, CoapSessionMsgType.POST_TELEMETRY_REQUEST);
                    break;
                case RPC:
                    Optional<Integer> requestId = getRequestId(exchange.advanced().getRequest());
                    if (requestId.isPresent()) {
                        processRequest(exchange, CoapSessionMsgType.TO_DEVICE_RPC_RESPONSE);
                    } else {
                        processRequest(exchange, CoapSessionMsgType.TO_SERVER_RPC_REQUEST);
                    }
                    break;
                case CLAIM:
                    processRequest(exchange, CoapSessionMsgType.CLAIM_REQUEST);
                    break;
                case PROVISION:
                    processProvision(exchange);
                    break;
            }
        }
    }

    private void processProvision(CoapExchange exchange) {
        deferAccept(exchange);
        try {
            UUID sessionId = UUID.randomUUID();
            log.trace("[{}] Processing provision publish msg [{}]!", sessionId, exchange.advanced().getRequest());
            TransportProtos.ProvisionDeviceRequestMsg provisionRequestMsg;
            TransportPayloadType payloadType;
            try {
                provisionRequestMsg = transportContext.getJsonCoapAdaptor().convertToProvisionRequestMsg(sessionId, exchange.advanced().getRequest());
                payloadType = TransportPayloadType.JSON;
            } catch (Exception e) {
                if (e instanceof JsonParseException || (e.getCause() != null && e.getCause() instanceof JsonParseException)) {
                    provisionRequestMsg = transportContext.getProtoCoapAdaptor().convertToProvisionRequestMsg(sessionId, exchange.advanced().getRequest());
                    payloadType = TransportPayloadType.PROTOBUF;
                } else {
                    throw new AdaptorException(e);
                }
            }
            transportService.process(provisionRequestMsg, new DeviceProvisionCallback(exchange, payloadType));
        } catch (AdaptorException e) {
            log.trace("Failed to decode message: ", e);
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    private void processRequest(CoapExchange exchange, CoapSessionMsgType type) {
        log.trace("Processing {}", exchange.advanced().getRequest());
        deferAccept(exchange);
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();

        var dtlsSessionId = request.getSourceContext().get(KEY_SESSION_ID);
        if (dtlsSessionsMap != null && dtlsSessionId != null && !dtlsSessionId.isEmpty()) {
            TbCoapDtlsSessionInfo tbCoapDtlsSessionInfo = this.getCoapDtlsSessionInfo(request.getSourceContext());
            if (tbCoapDtlsSessionInfo != null) {
                processRequest(exchange, type, request, tbCoapDtlsSessionInfo.getMsg(), tbCoapDtlsSessionInfo.getDeviceProfile());
            } else {
                processAccessTokenRequest(exchange, type, request);
            }
        } else {
            processAccessTokenRequest(exchange, type, request);
        }
    }

    private void processAccessTokenRequest(CoapExchange exchange, CoapSessionMsgType type, Request request) {
        Optional<DeviceTokenCredentials> credentials = decodeCredentials(request);
        if (credentials.isEmpty()) {
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }
        transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.get().getCredentialsId()).build(),
                new CoapDeviceAuthCallback(exchange, (deviceCredentials, deviceProfile) -> processRequest(exchange, type, request, deviceCredentials, deviceProfile)));
    }

    private void processRequest(CoapExchange exchange, CoapSessionMsgType type, Request request, ValidateDeviceCredentialsResponse deviceCredentials, DeviceProfile deviceProfile) {
        TbCoapClientState clientState = null;
        try {
            clientState = clients.getOrCreateClient(type, deviceCredentials, deviceProfile);
            clients.awake(clientState);
            switch (type) {
                case POST_ATTRIBUTES_REQUEST:
                    handlePostAttributesRequest(clientState, exchange, request);
                    break;
                case POST_TELEMETRY_REQUEST:
                    handlePostTelemetryRequest(clientState, exchange, request);
                    break;
                case CLAIM_REQUEST:
                    handleClaimRequest(clientState, exchange, request);
                    break;
                case SUBSCRIBE_ATTRIBUTES_REQUEST:
                    handleAttributeSubscribeRequest(clientState, exchange, request);
                    break;
                case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                    handleAttributeUnsubscribeRequest(clientState, exchange, request);
                    break;
                case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                    handleRpcSubscribeRequest(clientState, exchange, request);
                    break;
                case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                    handleRpcUnsubscribeRequest(clientState, exchange, request);
                    break;
                case TO_DEVICE_RPC_RESPONSE:
                    handleToDeviceRpcResponse(clientState, exchange, request);
                    break;
                case TO_SERVER_RPC_REQUEST:
                    handleToServerRpcRequest(clientState, exchange, request);
                    break;
                case GET_ATTRIBUTES_REQUEST:
                    handleGetAttributesRequest(clientState, exchange, request);
                    break;
            }
        } catch (AdaptorException e) {
            if (clientState != null) {
                log.trace("[{}] Failed to decode message: ", clientState.getDeviceId(), e);
            }
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    private void handlePostAttributesRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo, clientState.getAdaptor().convertToPostAttributes(sessionId, request,
                        clientState.getConfiguration().getAttributesMsgDescriptor()),
                new CoapResponseCodeCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    private void handlePostTelemetryRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo, clientState.getAdaptor().convertToPostTelemetry(sessionId, request,
                        clientState.getConfiguration().getTelemetryMsgDescriptor()),
                new CoapResponseCodeCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    private void handleClaimRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToClaimDevice(sessionId, request, sessionInfo),
                new CoapResponseCodeCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    private void handleAttributeSubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        String attrSubToken = getTokenFromRequest(request);
        if (!clients.registerAttributeObservation(clientState, attrSubToken, exchange)) {
            log.warn("[{}] Received duplicate attribute subscribe request for token: {}", clientState.getDeviceId(), attrSubToken);
        }
    }

    private void handleAttributeUnsubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        clients.deregisterAttributeObservation(clientState, getTokenFromRequest(request), exchange);
    }

    private void handleRpcUnsubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        clients.deregisterRpcObservation(clientState, getTokenFromRequest(request), exchange);
    }

    private void handleToDeviceRpcResponse(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto session = clientState.getSession();
        if (session == null) {
            session = clients.getNewSyncSession(clientState);
        }
        UUID sessionId = toSessionId(session);
        transportService.process(session,
                clientState.getAdaptor().convertToDeviceRpcResponse(sessionId, request, clientState.getConfiguration().getRpcResponseMsgDescriptor()),
                new CoapResponseCodeCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    private void handleRpcSubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        String rpcSubToken = getTokenFromRequest(request);
        if (!clients.registerRpcObservation(clientState, rpcSubToken, exchange)) {
            log.warn("[{}] Received duplicate rpc subscribe request.", rpcSubToken);
        }
    }

    private void handleGetAttributesRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.registerSyncSession(sessionInfo, new GetAttributesSyncSessionCallback(clientState, exchange, request), timeout);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToGetAttributes(sessionId, request),
                new CoapNoOpCallback(exchange));
    }

    private void handleToServerRpcRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.registerSyncSession(sessionInfo, new ToServerRpcSyncSessionCallback(clientState, exchange, request), timeout);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToServerRpcRequest(sessionId, request),
                new CoapNoOpCallback(exchange));
    }

    /**
     * Send an empty ACK if we are unable to send the full response within the timeout.
     * If the full response is transmitted before the timeout this will not do anything.
     * If this is triggered the full response will be sent in a separate CON/NON message.
     * Essentially this allows the use of piggybacked responses.
     */
    private void deferAccept(CoapExchange exchange) {
        if (piggybackTimeout > 0) {
            transportContext.getScheduler().schedule(exchange::accept, piggybackTimeout, TimeUnit.MILLISECONDS);
        } else {
            exchange.accept();
        }
    }

    private UUID toSessionId(TransportProtos.SessionInfoProto sessionInfoProto) {
        return new UUID(sessionInfoProto.getSessionIdMSB(), sessionInfoProto.getSessionIdLSB());
    }

    private String getTokenFromRequest(Request request) {
        return (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getAddress().getHostAddress() : "null")
                + ":" + (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getPort() : -1) + ":" + request.getTokenString();
    }

    private Optional<DeviceTokenCredentials> decodeCredentials(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() > ACCESS_TOKEN_POSITION) {
            return Optional.of(new DeviceTokenCredentials(uriPath.get(ACCESS_TOKEN_POSITION - 1)));
        } else {
            return Optional.empty();
        }
    }

    protected Optional<FeatureType> getFeatureType(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        try {
            int size = uriPath.size();
            if (size >= FEATURE_TYPE_POSITION) {
                if (size == FEATURE_TYPE_POSITION && StringUtils.isNumeric(uriPath.get(size - 1))) {
                    return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION - 2).toUpperCase()));
                }
                return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION - 1).toUpperCase()));
            } else if (size == FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST) {
                if (uriPath.contains(DataConstants.PROVISION)) {
                    return Optional.of(FeatureType.valueOf(DataConstants.PROVISION.toUpperCase()));
                }
                return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST - 1).toUpperCase()));
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
            } else {
                return Optional.of(Integer.valueOf(uriPath.get(REQUEST_ID_POSITION_CERTIFICATE_REQUEST - 1)));
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

    private static class DeviceProvisionCallback implements TransportServiceCallback<TransportProtos.ProvisionDeviceResponseMsg> {
        private final CoapExchange exchange;
        private final TransportPayloadType payloadType;

        DeviceProvisionCallback(CoapExchange exchange, TransportPayloadType payloadType) {
            this.exchange = exchange;
            this.payloadType = payloadType;
        }

        @Override
        public void onSuccess(TransportProtos.ProvisionDeviceResponseMsg msg) {
            CoAP.ResponseCode responseCode = CoAP.ResponseCode.CREATED;
            if (!msg.getStatus().equals(TransportProtos.ResponseStatus.SUCCESS)) {
                responseCode = CoAP.ResponseCode.BAD_REQUEST;
            }
            if (payloadType.equals(TransportPayloadType.JSON)) {
                exchange.respond(responseCode, JsonConverter.toJson(msg).toString());
            } else {
                exchange.respond(responseCode, msg.toByteArray());
            }
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    public class CoapResourceObserver implements ResourceObserver {

        @Override
        public void changedName(String old) {
        }

        @Override
        public void changedPath(String old) {
        }

        @Override
        public void addedChild(Resource child) {
        }

        @Override
        public void removedChild(Resource child) {
        }

        @Override
        public void addedObserveRelation(ObserveRelation relation) {
            Request request = relation.getExchange().getRequest();
            String token = getTokenFromRequest(request);
            clients.registerObserveRelation(token, relation);
            log.trace("Added Observe relation for token: {}", token);
        }

        @Override
        public void removedObserveRelation(ObserveRelation relation) {
            Request request = relation.getExchange().getRequest();
            String token = getTokenFromRequest(request);
            clients.deregisterObserveRelation(token);
            log.trace("Relation removed for token: {}", token);
        }
    }

    private TbCoapDtlsSessionInfo getCoapDtlsSessionInfo(EndpointContext endpointContext) {
        InetSocketAddress peerAddress = endpointContext.getPeerAddress();
        String certPemStr = getCertPem(endpointContext);
        TbCoapDtlsSessionKey tbCoapDtlsSessionKey = StringUtils.isNotBlank(certPemStr) ? new TbCoapDtlsSessionKey(peerAddress, certPemStr) : null;
        TbCoapDtlsSessionInfo tbCoapDtlsSessionInfo;
        if (tbCoapDtlsSessionKey != null) {
            tbCoapDtlsSessionInfo = dtlsSessionsMap
                    .computeIfPresent(tbCoapDtlsSessionKey, (dtlsSessionIdStr, dtlsSessionInfo) -> {
                        dtlsSessionInfo.setLastActivityTime(System.currentTimeMillis());
                        return dtlsSessionInfo;
                    });
        } else {
            tbCoapDtlsSessionInfo = null;
        }
        return tbCoapDtlsSessionInfo;
    }

    private String getCertPem(EndpointContext endpointContext) {
        try {
            X509CertPath certPath = (X509CertPath) endpointContext.getPeerIdentity();
            X509Certificate x509Certificate = (X509Certificate) certPath.getPath().getCertificates().get(0);
            return Base64.getEncoder().encodeToString(x509Certificate.getEncoded());
        } catch (Exception e) {
            log.error("Failed to get cert PEM: [{}]", endpointContext.getPeerAddress(), e);
            return null;
        }
    }
}

