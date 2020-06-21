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
package org.thingsboard.server.transport.lwm2m.server;

import com.google.gson.JsonElement;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MJsonAdaptor;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;
import org.thingsboard.server.transport.lwm2m.server.adaptors.ReadResultAttrTel;


import java.util.NoSuchElementException;
import java.util.UUID;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.DEVICE_TELEMETRY_TOPIC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.GET_TYPE_OPER_READ;


@Service("LwM2MTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Slf4j
public class LwM2MTransportService {

    private final UUID sessionId = UUID.randomUUID();

    @Autowired
    private LwM2MJsonAdaptor adaptor;

    @Autowired
    private TransportService transportService;

    @Autowired
    private LeshanServer lwServer;

    @Autowired
    private LwM2MTransportContext context;

    @Autowired
    private LwM2MTransportRequest lwM2MTransportRequest;

    @Autowired
    LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    public void onRegistered(Registration registration) {
        String endpointId = registration.getEndpoint();
        String lwm2mVersion = registration.getLwM2mVersion();
        log.info("[{}] [{}] Received endpoint registration version event", endpointId, lwm2mVersion);
////        String jReg = this.gson.toJson(registration);
//        TransportProtos.LwM2MRegistrationRequestMsg registrationRequestMsg = TransportProtos.LwM2MRegistrationRequestMsg.newBuilder()
//                .setTenantId(context.getTenantId())
//                .setEndpoint(endpointId)
//                .setLwM2MVersion(lwm2mVersion)
//                .setSmsNumber(smsNumber).build();
//        TransportProtos.LwM2MRequestMsg requestMsg = TransportProtos.LwM2MRequestMsg.newBuilder().setRegistrationMsg(registrationRequestMsg).build();
//        context.getTransportService().process(requestMsg, new TransportServiceCallback<TransportProtos.LwM2MResponseMsg>() {
//            @Override
//            public void onSuccess(TransportProtos.LwM2MResponseMsg msg) {
//                log.info("[{}][{}] Received endpoint registration response: {}", lwm2mVersion, endpointId, msg);
//                sessions.put(endpointId, msg);
                //Tests.
//                Collection<Registration> registrations = lwM2MTransportRequest.doGetRegistrations();
//                log.info("Ok process get registrations: [{}]", registrations);
//                String traget = "/3";
//                String typeOper = GET_TYPE_OPER_READ;
//                LwM2mResponse cResponse =  lwM2MTransportRequest.doGet(endpointId,  traget, typeOper, ContentFormat.TLV.getName());
//                log.info("[{}] [{}] [{}] Ok process get request: [{}]", endpointId, traget, typeOper, cResponse);
//                traget = "/3/0";
//                cResponse =  lwM2MTransportRequest.doGet(endpointId,  traget, typeOper, ContentFormat.TLV.getName());
//                log.info("[{}] [{}] [{}] Ok process get request: [{}]", endpointId, traget, typeOper, cResponse);
//                traget = "/3/0/14";
//                typeOper = GET_TYPE_OPER_READ;
//                cResponse =  lwM2MTransportRequest.doGet(endpointId,  traget, typeOper, ContentFormat.TLV.getName());
//                log.info("[{}] [{}] [{}] Ok process get request: [{}]", endpointId, traget, typeOper, cResponse);
//                traget = "/3/0/14";
//                typeOper = GET_TYPE_OPER_DISCOVER;
//                cResponse =  lwM2MTransportRequest.doGet(endpointId,  traget, typeOper, ContentFormat.TLV.getName());
//                log.info("[{}] [{}] [{}] Ok process get request: [{}]", endpointId, traget, typeOper, cResponse);
//                SecurityInfo info = getPSK(endpointId);
//                ReadResultAttrTel readResult = doGetAttributsTelemetry(endpointId);
//                processDevicePublish(readResult.getPostAttribute(), DEVICE_ATTRIBUTES_TOPIC, -1, endpointId);
//                processDevicePublish(readResult.getPostTelemetry(), DEVICE_TELEMETRY_TOPIC, -1, endpointId);
//            }

//            @Override
//            public void onError(Throwable e) {
//                log.warn("[{}][{}] Failed to process registration request [{}]", lwm2mVersion, endpointId, e);
//            }
//        });
    }

    public void processDevicePublish(JsonElement msg, String topicName, int msgId, String clientEndpoint) {
        TransportProtos.SessionInfoProto sessionInfo = getValidateSessionInfo(clientEndpoint);
        if (sessionInfo != null) {
            try {
                if (topicName.equals(DEVICE_TELEMETRY_TOPIC)) {
                    TransportProtos.PostTelemetryMsg postTelemetryMsg = adaptor.convertToPostTelemetry(msg);
                    transportService.process(sessionInfo, postTelemetryMsg, getPubAckCallback(msgId, postTelemetryMsg));
                } else if (topicName.equals(DEVICE_ATTRIBUTES_TOPIC)) {
                    TransportProtos.PostAttributeMsg postAttributeMsg = adaptor.convertToPostAttributes(msg);
                    transportService.process(sessionInfo, postAttributeMsg, getPubAckCallback(msgId, postAttributeMsg));
                }
//            else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX)) {
//                TransportProtos.GetAttributeRequestMsg getAttributeMsg = adaptor.convertToGetAttributes(deviceSessionCtx, mqttMsg);
//                transportService.process(sessionInfo, getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
//            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC)) {
//                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = adaptor.convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg);
//                transportService.process(sessionInfo, rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
//            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_TOPIC)) {
//                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = adaptor.convertToServerRpcRequest(deviceSessionCtx, mqttMsg);
//                transportService.process(sessionInfo, rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
//            } else if (topicName.equals(MqttTopics.DEVICE_CLAIM_TOPIC)) {
//                TransportProtos.ClaimDeviceMsg claimDeviceMsg = adaptor.convertToClaimDevice(deviceSessionCtx, mqttMsg);
//                transportService.process(sessionInfo, claimDeviceMsg, getPubAckCallback(ctx, msgId, claimDeviceMsg));
//            } else {
//                transportService.reportActivity(sessionInfo);
//            }
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);

            }
        }
    }

    private <T> TransportServiceCallback<Void> getPubAckCallback(final int msgId, final T msg) {
        return new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}] Published msg: {}", sessionId, msg);
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {}", sessionId, msg, e);

            }
        };
    }

    private TransportProtos.SessionInfoProto getValidateSessionInfo(String endpointId) {
        TransportProtos.SessionInfoProto sessionInfo = null;
        TransportProtos.ValidateDeviceCredentialsResponseMsg msg = context.getSessions().get(endpointId);
        if (msg == null || msg.getDeviceInfo()== null) {
            log.warn("[{}] [{}]", endpointId, CONNECTION_REFUSED_NOT_AUTHORIZED.toString());
        } else {
            sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                    .setNodeId(context.getNodeId())
                    .setSessionIdMSB(sessionId.getMostSignificantBits())
                    .setSessionIdLSB(sessionId.getLeastSignificantBits())
                    .setDeviceIdMSB(msg.getDeviceInfo().getDeviceIdMSB())
                    .setDeviceIdLSB(msg.getDeviceInfo().getDeviceIdLSB())
                    .setTenantIdMSB(msg.getDeviceInfo().getTenantIdMSB())
                    .setTenantIdLSB(msg.getDeviceInfo().getTenantIdLSB())
                    .setDeviceName(msg.getDeviceInfo().getDeviceName())
                    .setDeviceType(msg.getDeviceInfo().getDeviceType())
                    .build();
            transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(TransportProtos.SessionEvent.OPEN), null);
            log.info("[{}] Client connected!", sessionId);
        }
        return sessionInfo;
    }

    public void updatedReg(Registration registration) {
        String endpointId = registration.getEndpoint();
        String smsNumber = registration.getSmsNumber() == null ? "" : registration.getSmsNumber();
        String lwm2mVersion = registration.getLwM2mVersion();
        log.info("[{}] [{}] Received endpoint updated registration version event", endpointId, lwm2mVersion);
        ReadResultAttrTel readResultAttrTel = doGetAttributsTelemetry(endpointId);
        processDevicePublish(readResultAttrTel.getPostAttribute(), DEVICE_ATTRIBUTES_TOPIC, -1, endpointId);
        processDevicePublish(readResultAttrTel.getPostTelemetry(), DEVICE_TELEMETRY_TOPIC, -1, endpointId);
    }

    public void unReg(Registration registration) {
        String endpointId = registration.getEndpoint();
        log.info("[{}] Received endpoint un registration version event", endpointId);
        lwM2mInMemorySecurityStore.remove(endpointId, false);
        context.getSessions().remove(endpointId);
    }

    public void onSleepingDev(Registration registration) {
        String endpointId = registration.getEndpoint();
        String smsNumber = registration.getSmsNumber() == null ? "" : registration.getSmsNumber();
        String lwm2mVersion = registration.getLwM2mVersion();
        log.info("[{}] [{}] Received endpoint Sleeping version event", endpointId, lwm2mVersion);
        //TODO: associate endpointId with device information.
    }

    public void onAwakeDev(Registration registration) {
        String endpointId = registration.getEndpoint();
        String smsNumber = registration.getSmsNumber() == null ? "" : registration.getSmsNumber();
        String lwm2mVersion = registration.getLwM2mVersion();
        log.info("[{}] [{}] Received endpoint Awake version event", endpointId, lwm2mVersion);
        //TODO: associate endpointId with device information.
    }

    public void observOnResponse(Observation observation, Registration registration, ObserveResponse response) {
//        String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\",\"res\":\"")
//                .append(observation.getPath().toString()).append("\",\"val\":")
//                .append(this.gson.toJson(response.getContent())).append("}").toString();
//                sendEvent(EVENT_NOTIFICATION, data, registration.getEndpoint());
        //TODO: associate endpointId with device information.
    }

    //    // /clients/endPoint/LWRequest/discover : do LightWeight M2M discover request on a given client.
    public DiscoverResponse getDiscover(String target, String clientEndpoint, String timeoutParam) throws InterruptedException {
        DiscoverRequest request = new DiscoverRequest(target);
        return this.lwServer.send(getRegistration(clientEndpoint), request, this.context.getTimeout());
    }

    public Registration getRegistration(String clientEndpoint) {
        return this.lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
    }

    @SneakyThrows
    public ReadResultAttrTel doGetAttributsTelemetry(String clientEndpoint) {
        ReadResultAttrTel readResultAttrTel = new ReadResultAttrTel();
        Registration registration = lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
        registration.getAdditionalRegistrationAttributes().entrySet().forEach(entry -> {
            log.info("Attributes: Key : [{}] Value : [{}]", entry.getKey(), entry.getValue());
            readResultAttrTel.getPostAttribute().addProperty(entry.getKey(), entry.getValue());
        });
        lwServer.getModelProvider().getObjectModel(registration).getObjectModels().forEach(om -> {
            String idObj = String.valueOf(om.id);
            LwM2mResponse cResponse = lwM2MTransportRequest.doGet(clientEndpoint, "/" + idObj, GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
            if (cResponse != null) {
                LwM2mNode content = ((ReadResponse) cResponse).getContent();
                ((LwM2mObject) content).getInstances().entrySet().stream().forEach(instance -> {
                    String instanceId = String.valueOf(instance.getValue().getId());
                    instance.getValue().getResources().entrySet().stream().forEach(resource -> {
                        int resourceId = resource.getValue().getId();
                        String resourceValue = getResourceValueToString(resource.getValue());
                        String attrTelName = om.name + "_" + instanceId + "_" + om.resources.get(resourceId).name;
                        log.info("resource.getValue() [{}] : [{}] -> [{}]", attrTelName, resourceValue, om.resources.get(resourceId).operations.name());
                        if ((om.resources.get(resourceId).operations.name().equals("R"))) {
                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, resourceValue);
                        } else {
                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, resourceValue);
                        }
                    });
                });
            }
        });
        return readResultAttrTel;
    }

    private String getResourceValueToString(LwM2mResource resource) {
        Object resValue;
        try {
            resValue = resource.getValues();
        } catch (NoSuchElementException e) {
            resValue = resource.getValue();
        }
        return String.valueOf(resValue);
    }
}
