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
package org.thingsboard.server.transport.coap.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.springframework.stereotype.Service;
import org.thingsboard.server.coapserver.TbCoapServerComponent;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.CoapTransportContext;
import org.thingsboard.server.transport.coap.TbCoapMessageObserver;
import org.thingsboard.server.transport.coap.TransportConfigurationContainer;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;
import org.thingsboard.server.transport.coap.callback.AbstractSyncSessionCallback;
import org.thingsboard.server.transport.coap.callback.CoapNoOpCallback;
import org.thingsboard.server.transport.coap.callback.CoapOkCallback;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.californium.core.coap.Message.MAX_MID;
import static org.eclipse.californium.core.coap.Message.NONE;

@Slf4j
@Service
@RequiredArgsConstructor
@TbCoapServerComponent
public class DefaultCoapClientContext implements CoapClientContext {

    private final CoapTransportContext transportContext;
    private final TransportService transportService;
    private final ConcurrentMap<DeviceId, TbCoapClientState> clients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TbCoapClientState> clientsByToken = new ConcurrentHashMap<>();

    @Override
    public boolean registerAttributeObservation(TbCoapClientState clientState, String token, CoapExchange exchange) {
        return registerFeatureObservation(clientState, token, exchange, FeatureType.ATTRIBUTES);
    }

    @Override
    public boolean registerRpcObservation(TbCoapClientState clientState, String token, CoapExchange exchange) {
        return registerFeatureObservation(clientState, token, exchange, FeatureType.RPC);
    }

    @Override
    public void onUplink(TransportProtos.SessionInfoProto sessionInfo) {
        getClientState(toDeviceId(sessionInfo)).updateLastUplinkTime();
    }

    @Override
    public AtomicInteger getNotificationCounterByToken(String token) {
        TbCoapClientState state = clientsByToken.get(token);
        if (state == null) {
            log.trace("Failed to find state using token: {}", token);
            return null;
        }
        if (state.getAttrs() != null && state.getAttrs().getToken().equals(token)) {
            return state.getAttrs().getObserveCounter();
        } else {
            log.trace("Failed to find attr subscription using token: {}", token);
        }
        if (state.getRpc() != null && state.getRpc().getToken().equals(token)) {
            return state.getRpc().getObserveCounter();
        } else {
            log.trace("Failed to find rpc subscription using token: {}", token);
        }
        return null;
    }

    @Override
    public void registerObserveRelation(String token, ObserveRelation relation) {
        TbCoapClientState state = clientsByToken.get(token);
        if (state == null) {
            log.trace("Failed to find state using token: {}", token);
            return;
        }
        if (state.getAttrs() != null && state.getAttrs().getToken().equals(token)) {
            state.getAttrs().setObserveRelation(relation);
        } else {
            log.trace("Failed to find attr subscription using token: {}", token);
        }
        if (state.getRpc() != null && state.getRpc().getToken().equals(token)) {
            state.getRpc().setObserveRelation(relation);
        } else {
            log.trace("Failed to find rpc subscription using token: {}", token);
        }
    }

    @Override
    public void deregisterObserveRelation(String token) {
        TbCoapClientState state = clientsByToken.remove(token);
        if (state == null) {
            log.trace("Failed to find state using token: {}", token);
            return;
        }
        if (state.getAttrs() != null && state.getAttrs().getToken().equals(token)) {
            cancelAttributeSubscription(state);
        } else {
            log.trace("Failed to find attr subscription using token: {}", token);
        }
        if (state.getRpc() != null && state.getRpc().getToken().equals(token)) {
            cancelRpcSubscription(state);
        } else {
            log.trace("Failed to find rpc subscription using token: {}", token);
        }
    }

    @Override
    public void reportActivity() {
        for (TbCoapClientState state : clients.values()) {
            if (state.getSession() != null) {
                transportService.reportActivity(state.getSession());
            }
        }
    }

    private boolean registerFeatureObservation(TbCoapClientState state, String token, CoapExchange exchange, FeatureType featureType) {
        state.lock();
        try {
            boolean newObservation;
            if (FeatureType.ATTRIBUTES.equals(featureType)) {
                if (state.getAttrs() == null) {
                    newObservation = true;
                    state.setAttrs(new TbCoapObservationState(exchange, token));
                } else {
                    newObservation = !state.getAttrs().getToken().equals(token);
                    if (newObservation) {
                        TbCoapObservationState old = state.getAttrs();
                        state.setAttrs(new TbCoapObservationState(exchange, token));
                        old.getExchange().respond(CoAP.ResponseCode.DELETED);
                    }
                }
            } else {
                if (state.getRpc() == null) {
                    newObservation = true;
                    state.setRpc(new TbCoapObservationState(exchange, token));
                } else {
                    newObservation = !state.getRpc().getToken().equals(token);
                    if (newObservation) {
                        TbCoapObservationState old = state.getRpc();
                        state.setRpc(new TbCoapObservationState(exchange, token));
                        old.getExchange().respond(CoAP.ResponseCode.DELETED);
                    }
                }
            }
            if (newObservation) {
                clientsByToken.put(token, state);
                if (state.getSession() == null) {
                    TransportProtos.SessionInfoProto session = SessionInfoCreator.create(state.getCredentials(), transportContext, UUID.randomUUID());
                    state.setSession(session);
                    transportService.registerAsyncSession(session, new CoapSessionListener(state));
                    transportService.process(session, getSessionEventMsg(TransportProtos.SessionEvent.OPEN), null);
                }
                if (FeatureType.ATTRIBUTES.equals(featureType)) {
                    transportService.process(state.getSession(),
                            TransportProtos.SubscribeToAttributeUpdatesMsg.getDefaultInstance(), new CoapNoOpCallback(exchange));
                    transportService.process(state.getSession(),
                            TransportProtos.GetAttributeRequestMsg.newBuilder().setOnlyShared(true).build(),
                            new CoapNoOpCallback(exchange));
                } else {
                    transportService.process(state.getSession(),
                            TransportProtos.SubscribeToRPCMsg.getDefaultInstance(),
                            new CoapOkCallback(exchange, CoAP.ResponseCode.VALID, CoAP.ResponseCode.INTERNAL_SERVER_ERROR)
                    );
                }
            }
            return newObservation;
        } finally {
            state.unlock();
        }
    }

    @Override
    public void deregisterAttributeObservation(TbCoapClientState state, String token, CoapExchange exchange) {
        state.lock();
        try {
            clientsByToken.remove(token);
            if (state.getSession() == null) {
                log.trace("[{}] Failed to delete attribute observation: {}. Session is not present.", state.getDeviceId(), token);
                return;
            }
            if (state.getAttrs() == null) {
                log.trace("[{}] Failed to delete attribute observation: {}. It is not registered.", state.getDeviceId(), token);
                return;
            }
            if (!state.getAttrs().getToken().equals(token)) {
                log.trace("[{}] Failed to delete attribute observation: {}. Token mismatch.", state.getDeviceId(), token);
                return;
            }
            cancelAttributeSubscription(state);
        } finally {
            state.unlock();
        }
    }

    @Override
    public void deregisterRpcObservation(TbCoapClientState state, String token, CoapExchange exchange) {
        state.lock();
        try {
            clientsByToken.remove(token);
            if (state.getSession() == null) {
                log.trace("[{}] Failed to delete rpc observation: {}. Session is not present.", state.getDeviceId(), token);
                return;
            }
            if (state.getRpc() == null) {
                log.trace("[{}] Failed to delete rpc observation: {}. It is not registered.", state.getDeviceId(), token);
                return;
            }
            if (!state.getRpc().getToken().equals(token)) {
                log.trace("[{}] Failed to delete rpc observation: {}. Token mismatch.", state.getDeviceId(), token);
                return;
            }
            cancelRpcSubscription(state);
        } finally {
            state.unlock();
        }
    }

    @Override
    public TbCoapClientState getOrCreateClient(SessionMsgType type, ValidateDeviceCredentialsResponse deviceCredentials, DeviceProfile deviceProfile) throws AdaptorException {
        DeviceId deviceId = deviceCredentials.getDeviceInfo().getDeviceId();
        TbCoapClientState state = getClientState(deviceId);
        state.lock();
        try {
            if (state.getConfiguration() == null || state.getAdaptor() == null) {
                state.setConfiguration(getTransportConfigurationContainer(deviceProfile));
                state.setAdaptor(getCoapTransportAdaptor(state.getConfiguration().isJsonPayload()));
            }
            if (state.getCredentials() == null) {
                state.setCredentials(deviceCredentials);
            }
        } finally {
            state.unlock();
        }
        return state;
    }

    @Override
    public TransportProtos.SessionInfoProto getNewSyncSession(TbCoapClientState state) {
        return SessionInfoCreator.create(state.getCredentials(), transportContext, UUID.randomUUID());
    }

    private TbCoapClientState getClientState(DeviceId deviceId) {
        return clients.computeIfAbsent(deviceId, TbCoapClientState::new);
    }

    private static DeviceId toDeviceId(TransportProtos.SessionInfoProto s) {
        return new DeviceId(new UUID(s.getDeviceIdMSB(), s.getDeviceIdLSB()));
    }

    private static TransportProtos.SessionEventMsg getSessionEventMsg(TransportProtos.SessionEvent event) {
        return TransportProtos.SessionEventMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .setEvent(event).build();
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
                    String deviceRpcRequestProtoSchema = protoTransportPayloadConfiguration.getDeviceRpcRequestProtoSchema();
                    String deviceRpcResponseProtoSchema = protoTransportPayloadConfiguration.getDeviceRpcResponseProtoSchema();
                    return new TransportConfigurationContainer(false,
                            protoTransportPayloadConfiguration.getTelemetryDynamicMessageDescriptor(deviceTelemetryProtoSchema),
                            protoTransportPayloadConfiguration.getAttributesDynamicMessageDescriptor(deviceAttributesProtoSchema),
                            protoTransportPayloadConfiguration.getRpcResponseDynamicMessageDescriptor(deviceRpcResponseProtoSchema),
                            protoTransportPayloadConfiguration.getRpcRequestDynamicMessageBuilder(deviceRpcRequestProtoSchema)
                    );
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

    @RequiredArgsConstructor
    private class CoapSessionListener implements SessionMsgListener {

        private final TbCoapClientState state;

        @Override
        public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg msg) {
            TbCoapObservationState attrs = state.getAttrs();
            if (attrs != null) {
                try {
                    boolean conRequest = AbstractSyncSessionCallback.isConRequest(state.getAttrs());
                    Response response = state.getAdaptor().convertToPublish(conRequest, msg);
                    attrs.getExchange().respond(response);
                } catch (AdaptorException e) {
                    log.trace("Failed to reply due to error", e);
                    cancelObserveRelation(attrs);
                    cancelAttributeSubscription(state);
                }
            } else {
                log.debug("[{}] Get Attrs exchange is empty", state.getDeviceId());
            }
        }

        @Override
        public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg msg) {
            log.trace("[{}] Received attributes update notification to device", sessionId);
            TbCoapObservationState attrs = state.getAttrs();
            if (attrs != null) {
                try {
                    boolean conRequest = AbstractSyncSessionCallback.isConRequest(state.getAttrs());
                    Response response = state.getAdaptor().convertToPublish(conRequest, msg);
                    attrs.getExchange().respond(response);
                } catch (AdaptorException e) {
                    log.trace("[{}] Failed to reply due to error", state.getDeviceId(), e);
                    cancelObserveRelation(attrs);
                    cancelAttributeSubscription(state);
                }
            } else {
                log.debug("[{}] Get Attrs exchange is empty", state.getDeviceId());
            }
        }

        @Override
        public void onRemoteSessionCloseCommand(UUID sessionId, TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
            log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
            cancelRpcSubscription(state);
            cancelAttributeSubscription(state);
        }

        @Override
        public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg msg) {
            log.trace("[{}] Received RPC command to device", sessionId);
            boolean sent = false;
            boolean conRequest = AbstractSyncSessionCallback.isConRequest(state.getRpc());
            try {
                Response response = state.getAdaptor().convertToPublish(conRequest, msg, state.getConfiguration().getRpcRequestDynamicMessageBuilder());
                int requestId = getNextMsgId();
                response.setMID(requestId);
                if (msg.getPersisted() && conRequest) {
                    transportContext.getRpcAwaitingAck().put(requestId, msg);
                    transportContext.getScheduler().schedule(() -> {
                        TransportProtos.ToDeviceRpcRequestMsg awaitingAckMsg = transportContext.getRpcAwaitingAck().remove(requestId);
                        if (awaitingAckMsg != null) {
                            transportService.process(state.getSession(), msg, true, TransportServiceCallback.EMPTY);
                        }
                    }, Math.max(0, msg.getExpirationTime() - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                    response.addMessageObserver(new TbCoapMessageObserver(requestId, id -> {
                        TransportProtos.ToDeviceRpcRequestMsg rpcRequestMsg = transportContext.getRpcAwaitingAck().remove(id);
                        if (rpcRequestMsg != null) {
                            transportService.process(state.getSession(), rpcRequestMsg, false, TransportServiceCallback.EMPTY);
                        }
                    }));
                }
                state.getRpc().getExchange().respond(response);
                sent = true;
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                cancelObserveRelation(state.getRpc());
                cancelRpcSubscription(state);
            } finally {
                if (msg.getPersisted() && !conRequest) {
                    transportService.process(state.getSession(), msg, sent, TransportServiceCallback.EMPTY);
                }
            }
        }

        @Override
        public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg msg) {

        }

        private void cancelObserveRelation(TbCoapObservationState attrs) {
            if (attrs.getObserveRelation() != null) {
                attrs.getObserveRelation().cancel();
            }
        }
    }

    protected int getNextMsgId() {
        return ThreadLocalRandom.current().nextInt(NONE, MAX_MID + 1);
    }

    private void cancelRpcSubscription(TbCoapClientState state) {
        if (state.getRpc() != null) {
            clientsByToken.remove(state.getRpc().getToken());
            CoapExchange exchange = state.getAttrs().getExchange();
            state.setRpc(null);
            transportService.process(state.getSession(),
                    TransportProtos.SubscribeToRPCMsg.newBuilder().setUnsubscribe(true).build(),
                    new CoapOkCallback(exchange, CoAP.ResponseCode.DELETED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
            if (state.getAttrs() == null) {
                transportService.process(state.getSession(), getSessionEventMsg(TransportProtos.SessionEvent.CLOSED), null);
                transportService.deregisterSession(state.getSession());
                state.setSession(null);
            }
        }
    }

    private void cancelAttributeSubscription(TbCoapClientState state) {
        if (state.getAttrs() != null) {
            clientsByToken.remove(state.getAttrs().getToken());
            CoapExchange exchange = state.getAttrs().getExchange();
            state.setAttrs(null);
            transportService.process(state.getSession(),
                    TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setUnsubscribe(true).build(),
                    new CoapOkCallback(exchange, CoAP.ResponseCode.DELETED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
            if (state.getRpc() == null) {
                transportService.process(state.getSession(), getSessionEventMsg(TransportProtos.SessionEvent.CLOSED), null);
                transportService.deregisterSession(state.getSession());
                state.setSession(null);
            }
        }
    }
}
