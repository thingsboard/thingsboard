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
package org.thingsboard.server.transport.coap;

import com.google.gson.JsonParseException;
import com.google.protobuf.Descriptors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceObserver;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class CoapTransportResource extends AbstractCoapTransportResource {
    private static final int ACCESS_TOKEN_POSITION = 3;
    private static final int FEATURE_TYPE_POSITION = 4;
    private static final int REQUEST_ID_POSITION = 5;

    private final ConcurrentMap<String, TransportProtos.SessionInfoProto> tokenToSessionIdMap = new ConcurrentHashMap<>();
    private final Set<UUID> rpcSubscriptions = ConcurrentHashMap.newKeySet();
    private final Set<UUID> attributeSubscriptions = ConcurrentHashMap.newKeySet();

    public CoapTransportResource(CoapTransportContext context, String name) {
        super(context, name);
//        this.setObservable(false); // disable observing
        this.setObservable(true); // enable observing
//        this.setObserveType(CoAP.Type.CON); // configure the notification type to CONs
//        this.getAttributes().setObservable(); // mark observable in the Link-Format
        this.addObserver(new CoapResourceObserver());
    }

    @Override
    protected void processHandleGet(CoapExchange exchange) {
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
    protected void processHandlePost(CoapExchange exchange) {
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
                case PROVISION:
                    processProvision(exchange);
                    break;
            }
        }
    }

    private void processProvision(CoapExchange exchange) {
        exchange.accept();
        try {
            UUID sessionId = UUID.randomUUID();
            log.trace("[{}] Processing provision publish msg [{}]!", sessionId, exchange.advanced().getRequest());
            TransportProtos.ProvisionDeviceRequestMsg provisionRequestMsg;
            try {
                provisionRequestMsg = transportContext.getJsonCoapAdaptor().convertToProvisionRequestMsg(sessionId, exchange.advanced().getRequest());
            } catch (Exception e) {
                if (e instanceof JsonParseException || (e.getCause() != null && e.getCause() instanceof JsonParseException)) {
                    provisionRequestMsg = transportContext.getProtoCoapAdaptor().convertToProvisionRequestMsg(sessionId, exchange.advanced().getRequest());
                } else {
                    throw new AdaptorException(e);
                }
            }
            transportService.process(provisionRequestMsg, new DeviceProvisionCallback(exchange));
        } catch (AdaptorException e) {
            log.trace("Failed to decode message: ", e);
            exchange.respond(ResponseCode.BAD_REQUEST);
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

        transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.get().getCredentialsId()).build(),
                new CoapDeviceAuthCallback(transportContext, exchange, (sessionInfo, deviceProfile) -> {
                    UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
                    try {
                        TransportConfigurationContainer transportConfigurationContainer = getTransportConfigurationContainer(deviceProfile);
                        CoapTransportAdaptor coapTransportAdaptor = getCoapTransportAdaptor(transportConfigurationContainer.isJsonPayload());
                        switch (type) {
                            case POST_ATTRIBUTES_REQUEST:
                                transportService.process(sessionInfo,
                                        coapTransportAdaptor.convertToPostAttributes(sessionId, request,
                                                transportConfigurationContainer.getAttributesMsgDescriptor()),
                                        new CoapOkCallback( exchange, CoAP.ResponseCode.VALID, ResponseCode.INTERNAL_SERVER_ERROR));
                                reportActivity(sessionInfo, attributeSubscriptions.contains(sessionId), rpcSubscriptions.contains(sessionId));
                                break;
                            case POST_TELEMETRY_REQUEST:
                                transportService.process(sessionInfo,
                                        coapTransportAdaptor.convertToPostTelemetry(sessionId, request,
                                                transportConfigurationContainer.getTelemetryMsgDescriptor()),
                                        new CoapOkCallback(exchange, CoAP.ResponseCode.VALID, ResponseCode.INTERNAL_SERVER_ERROR));
                                reportActivity(sessionInfo, attributeSubscriptions.contains(sessionId), rpcSubscriptions.contains(sessionId));
                                break;
                            case CLAIM_REQUEST:
                                transportService.process(sessionInfo,
                                        coapTransportAdaptor.convertToClaimDevice(sessionId, request, sessionInfo),
                                        new CoapOkCallback(exchange, CoAP.ResponseCode.VALID, ResponseCode.INTERNAL_SERVER_ERROR));
                                break;
                            case SUBSCRIBE_ATTRIBUTES_REQUEST:
                                String tokenFromRequest = getTokenFromRequest(request);
                                TransportProtos.SessionInfoProto currentSession = tokenToSessionIdMap.get(tokenFromRequest);
                                if (currentSession == null) {
                                    attributeSubscriptions.add(sessionId);
                                    registerAsyncCoapSession(exchange, sessionInfo, coapTransportAdaptor, tokenFromRequest);
                                    transportService.process(sessionInfo,
                                            TransportProtos.SubscribeToAttributeUpdatesMsg.getDefaultInstance(), new CoapNoOpCallback(exchange));
                                } else {
                                    UUID attrSessionId = new UUID(currentSession.getSessionIdMSB(), currentSession.getSessionIdLSB());
                                    reportActivity(currentSession, attributeSubscriptions.contains(attrSessionId), rpcSubscriptions.contains(attrSessionId));
                                }
                                break;
                            case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                                TransportProtos.SessionInfoProto attrSession = lookupAsyncSessionInfo(getTokenFromRequest(request));
                                if (attrSession != null) {
                                    UUID attrSessionId = new UUID(attrSession.getSessionIdMSB(), attrSession.getSessionIdLSB());
                                    attributeSubscriptions.remove(attrSessionId);
                                    transportService.process(attrSession,
                                            TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setUnsubscribe(true).build(),
                                            new CoapOkCallback(exchange, CoAP.ResponseCode.VALID, ResponseCode.INTERNAL_SERVER_ERROR));
                                    closeAndDeregister(sessionInfo, sessionId);
                                }
                                break;
                            case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                                rpcSubscriptions.add(sessionId);
                                registerAsyncCoapSession(exchange, sessionInfo, coapTransportAdaptor, getTokenFromRequest(request));
                                transportService.process(sessionInfo,
                                        TransportProtos.SubscribeToRPCMsg.getDefaultInstance(),
                                        new CoapNoOpCallback(exchange));
                                break;
                            case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                                TransportProtos.SessionInfoProto rpcSession = lookupAsyncSessionInfo(getTokenFromRequest(request));
                                if (rpcSession != null) {
                                    UUID rpcSessionId = new UUID(rpcSession.getSessionIdMSB(), rpcSession.getSessionIdLSB());
                                    rpcSubscriptions.remove(rpcSessionId);
                                    transportService.process(rpcSession,
                                            TransportProtos.SubscribeToRPCMsg.newBuilder().setUnsubscribe(true).build(),
                                            new CoapOkCallback(exchange, CoAP.ResponseCode.VALID, ResponseCode.INTERNAL_SERVER_ERROR));
                                    closeAndDeregister(sessionInfo, sessionId);
                                }
                                break;
                            case TO_DEVICE_RPC_RESPONSE:
                                transportService.process(sessionInfo,
                                        coapTransportAdaptor.convertToDeviceRpcResponse(sessionId, request),
                                        new CoapOkCallback(exchange, CoAP.ResponseCode.VALID, ResponseCode.INTERNAL_SERVER_ERROR));
                                break;
                            case TO_SERVER_RPC_REQUEST:
                                transportService.registerSyncSession(sessionInfo, new CoapSessionListener(exchange, coapTransportAdaptor), transportContext.getTimeout());
                                transportService.process(sessionInfo,
                                        coapTransportAdaptor.convertToServerRpcRequest(sessionId, request),
                                        new CoapNoOpCallback(exchange));
                                break;
                            case GET_ATTRIBUTES_REQUEST:
                                transportService.registerSyncSession(sessionInfo, new CoapSessionListener(exchange, coapTransportAdaptor), transportContext.getTimeout());
                                transportService.process(sessionInfo,
                                        coapTransportAdaptor.convertToGetAttributes(sessionId, request),
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

    private TransportProtos.SessionInfoProto lookupAsyncSessionInfo(String token) {
        return tokenToSessionIdMap.remove(token);
    }

    private void registerAsyncCoapSession(CoapExchange exchange, TransportProtos.SessionInfoProto sessionInfo, CoapTransportAdaptor coapTransportAdaptor, String token) {
        tokenToSessionIdMap.putIfAbsent(token, sessionInfo);
        CoapSessionListener attrListener = new CoapSessionListener(exchange, coapTransportAdaptor);
        transportService.registerAsyncSession(sessionInfo, attrListener);
        transportService.process(sessionInfo, getSessionEventMsg(TransportProtos.SessionEvent.OPEN), null);
    }

    private String getTokenFromRequest(Request request) {
        return request.getSource().getHostAddress() + ":" + request.getSourcePort() + ":" + request.getTokenString();
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
            } else if (uriPath.size() == 3 && uriPath.contains(DataConstants.PROVISION)) {
                return Optional.of(FeatureType.valueOf(DataConstants.PROVISION.toUpperCase()));
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
    protected Resource getChildResource() {
        return this;
    }

    private static class DeviceProvisionCallback implements TransportServiceCallback<ProvisionDeviceResponseMsg> {
        private final CoapExchange exchange;

        DeviceProvisionCallback(CoapExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void onSuccess(TransportProtos.ProvisionDeviceResponseMsg msg) {
            exchange.respond(JsonConverter.toJson(msg).toString());
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    public static class CoapSessionListener implements SessionMsgListener {

        private final CoapExchange exchange;
        private final CoapTransportAdaptor coapTransportAdaptor;

        CoapSessionListener(CoapExchange exchange, CoapTransportAdaptor coapTransportAdaptor) {
            this.exchange = exchange;
            this.coapTransportAdaptor = coapTransportAdaptor;
        }

        @Override
        public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg msg) {
            try {
                exchange.respond(coapTransportAdaptor.convertToPublish(this, msg));
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Override
        public void onAttributeUpdate(TransportProtos.AttributeUpdateNotificationMsg msg) {
            try {
                exchange.respond(coapTransportAdaptor.convertToPublish(this, msg));
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
                exchange.respond(coapTransportAdaptor.convertToPublish(this, msg));
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Override
        public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg msg) {
            try {
                exchange.respond(coapTransportAdaptor.convertToPublish(this, msg));
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

        public CoapExchange getExchange() {
            return exchange;
        }
    }

    public class CoapResourceObserver implements ResourceObserver {

        @Override
        public void changedName(String old) {
            //do nothing
        }

        @Override
        public void changedPath(String old) {
            //do nothing
        }

        @Override
        public void addedChild(Resource child) {
            //do nothing
        }

        @Override
        public void removedChild(Resource child) {
            //do nothing
        }

        @Override
        public void addedObserveRelation(ObserveRelation relation) {
            if (log.isTraceEnabled()) {
                Request request = relation.getExchange().getRequest();
                log.trace("Added Observe relation for token: {}", getTokenFromRequest(request));
            }
        }

        @Override
        public void removedObserveRelation(ObserveRelation relation) {
            Request request = relation.getExchange().getRequest();
            String tokenFromRequest = getTokenFromRequest(request);
            log.info("Relation removed for token: {}", tokenFromRequest);
            TransportProtos.SessionInfoProto sessionInfoToRemove = lookupAsyncSessionInfo(tokenFromRequest);
            if (sessionInfoToRemove != null) {
                closeAndDeregister(sessionInfoToRemove, new UUID(sessionInfoToRemove.getSessionIdMSB(), sessionInfoToRemove.getDeviceIdLSB()));
            }
        }
    }

    private void closeAndDeregister(TransportProtos.SessionInfoProto session, UUID sessionId) {
        transportService.process(session, getSessionEventMsg(TransportProtos.SessionEvent.CLOSED), null);
        transportService.deregisterSession(session);
        rpcSubscriptions.remove(sessionId);
        attributeSubscriptions.remove(sessionId);
    }

    private TransportConfigurationContainer getTransportConfigurationContainer(DeviceProfile deviceProfile) throws AdaptorException {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration instanceof DefaultDeviceProfileTransportConfiguration) {
            return new TransportConfigurationContainer(true);
        } else if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration =
                    (CoapDeviceProfileTransportConfiguration) transportConfiguration;
            CoapDeviceTypeConfiguration coapDeviceTypeConfiguration =
                    coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
            if (coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration) {
                DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration =
                        (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
                TransportPayloadTypeConfiguration transportPayloadTypeConfiguration =
                        defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
                if (transportPayloadTypeConfiguration instanceof JsonTransportPayloadConfiguration) {
                    return new TransportConfigurationContainer(true);
                } else {
                    ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration =
                            (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
                    String deviceTelemetryProtoSchema = protoTransportPayloadConfiguration.getDeviceTelemetryProtoSchema();
                    String deviceAttributesProtoSchema = protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema();
                    return new TransportConfigurationContainer(false,
                            protoTransportPayloadConfiguration.getTelemetryDynamicMessageDescriptor(deviceTelemetryProtoSchema),
                            protoTransportPayloadConfiguration.getAttributesDynamicMessageDescriptor(deviceAttributesProtoSchema));
                }
            } else {
                throw new AdaptorException("Invalid CoapDeviceTypeConfiguration type: " + coapDeviceTypeConfiguration.getClass().getSimpleName() + "!");
            }
        } else {
            throw new AdaptorException("Invalid DeviceProfileTransportConfiguration type" + transportConfiguration.getClass().getSimpleName() + "!");
        }
    }

    private CoapTransportAdaptor getCoapTransportAdaptor(boolean jsonPayloadType) {
        return jsonPayloadType ? transportContext.getJsonCoapAdaptor() : transportContext.getProtoCoapAdaptor();
    }

    @Data
    public static class TransportConfigurationContainer {

        private boolean jsonPayload;
        private Descriptors.Descriptor telemetryMsgDescriptor;
        private Descriptors.Descriptor attributesMsgDescriptor;

        public TransportConfigurationContainer(boolean jsonPayload, Descriptors.Descriptor telemetryMsgDescriptor, Descriptors.Descriptor attributesMsgDescriptor) {
            this.jsonPayload = jsonPayload;
            this.telemetryMsgDescriptor = telemetryMsgDescriptor;
            this.attributesMsgDescriptor = attributesMsgDescriptor;
        }

        public TransportConfigurationContainer(boolean jsonPayload) {
            this.jsonPayload = jsonPayload;
        }
    }
}
