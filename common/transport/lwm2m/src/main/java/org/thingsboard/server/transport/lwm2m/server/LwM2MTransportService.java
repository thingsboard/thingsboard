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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.*;
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
import org.thingsboard.server.transport.lwm2m.server.client.ModelClient;
import org.thingsboard.server.transport.lwm2m.server.client.ModelObject;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;
import org.thingsboard.server.transport.lwm2m.server.adaptors.ReadResultAttrTel;


import java.security.KeyStoreException;
import java.util.*;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;

import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.*;


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
    private LwM2MTransportContextServer context;

    @Autowired
    private LwM2MTransportRequest lwM2MTransportRequest;

    @Autowired
    LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    public void onRegistered(LeshanServer lwServer, Registration registration, Collection<Observation> previousObsersations) {
        String endPoint = registration.getEndpoint();
        String lwm2mVersion = registration.getLwM2mVersion();
//        lwM2MTransportRequest.doPutResource(lwServer, registration, 1, 0, 1, "400");
        log.info("Received endpoint registration \ncontext: {}", context);
        log.info("[{}] [{}] Received endpoint registration version event", endPoint, lwm2mVersion);
//        log.info("[{}] Received endpoint registration previousObsersations", previousObsersations);
//        ModelClient modelClient = getClientModelWithValue(lwServer, registration);
        ModelClient modelClient = setCancelObservationObjects(lwServer, registration);
        getCObservationObjectsTest(lwServer, registration);
//        log.info("[{}] Received endpoint registration modelClient", modelClient);
//        startTriggerServer(lwServer, endPoint);
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
        if (msg == null || msg.getDeviceInfo() == null) {
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

    public void updatedReg(LeshanServer lwServer, Registration registration) {
        String endpointId = registration.getEndpoint();
//        String smsNumber = registration.getSmsNumber() == null ? "" : registration.getSmsNumber();
        String lwm2mVersion = registration.getLwM2mVersion();
        log.info("[{}] [{}] Received endpoint updated registration version event (next observe1)", endpointId, lwm2mVersion);
//        Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
//        log.info("[{}] [{}] Received endpoint observations updatedReg", registration.getEndpoint(), observations);

//        getObservResource(lwServer, registration);

//        ReadResultAttrTel readResultAttrTel = doGetAttributsTelemetry(lwServer, endpointId);
//        processDevicePublish(readResultAttrTel.getPostAttribute(), DEVICE_ATTRIBUTES_TOPIC, -1, endpointId);
//        processDevicePublish(readResultAttrTel.getPostTelemetry(), DEVICE_TELEMETRY_TOPIC, -1, endpointId);
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
        String lwm2mVersion = registration.getLwM2mVersion();
        log.info("[{}] [{}] Received endpoint Awake version event", endpointId, lwm2mVersion);
        //TODO: associate endpointId with device information.
    }

    private ObserveResponse setObservResource(LeshanServer lwServer, Registration registration, Integer objectId, Integer instanceId, Integer resourceId) {
        log.info("Endpoint: [{}] Object: [{}] Instnce: [{}] Resoutce: [{}] Received endpoint ObserveResponse", registration.getEndpoint(), objectId, instanceId, resourceId);

        ObserveResponse observeResponse = null;
        try {
            // observe device timezone
            observeResponse = lwServer.send(registration, new ObserveRequest(objectId, instanceId, resourceId));
            Observation observation = observeResponse.getObservation();
            Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
            LwM2mSingleResource resource = (LwM2mSingleResource) observeResponse.getContent();
            Object value = (resource != null) ? resource.getValue() : null;
            LwM2mPath path = (observeResponse.getObservation() != null) ? observeResponse.getObservation().getPath() : null;
            log.info("Endpoint: [{}] Path: [{}] Value: [{}] Received endpoint ObserveResponse", registration.getEndpoint(), path, value);
//            log.info("[{}] [{}] Received endpoint observation", registration.getEndpoint(), observation);
//            log.info("[{}] [{}] Received endpoint observations", registration.getEndpoint(), observations);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return observeResponse;
    }

    private ObserveResponse setObservInstance(LeshanServer lwServer, Registration registration, Integer objectId, Integer instanceId, Integer resourceId) {
        log.info("Endpoint: [{}] Object: [{}] Instnce: [{}] Resoutce: [{}] Received endpoint ObserveResponse", registration.getEndpoint(), objectId, instanceId, resourceId);

        ObserveResponse observeResponse = null;
        try {
            // observe device timezone
            observeResponse = lwServer.send(registration, new ObserveRequest(objectId, instanceId));
            Observation observation = observeResponse.getObservation();
            Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
//            log.info("Endpoint: [{}] Path: [{}] Value: [{}] Received endpoint ObserveResponse", registration.getEndpoint(), path, value);
//            log.info("[{}] [{}] Received endpoint observation", registration.getEndpoint(), observation);
            log.info("[{}] [{}] Received endpoint observations", registration.getEndpoint(), observations);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return observeResponse;
    }

    private ModelClient setCancelObservationObjects(LeshanServer lwServer, Registration registration) {
        if (registration != null) {
            Arrays.stream(registration.getObjectLinks()).forEach(url -> {
                String[] objects = url.getUrl().split("/");
                if (objects.length > 2) {
                    String target = "/" + objects[1];
                    lwServer.getObservationService().cancelObservations(registration, target);
                }
            });
        }
        ModelClient modelClient = new ModelClient(registration.getAdditionalRegistrationAttributes());
        return modelClient;
    }

    private void getCObservationObjectsTest(LeshanServer lwServer, Registration registration) {
        if (registration != null) {
            lwServer.send(registration, new ObserveRequest(3303), new ResponseCallback() {
                @Override
                public void onResponse(LwM2mResponse response) {
                   log.info("getCObservationObjectsTest: \nresponse: {}", response);
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    log.error("getCObservationObjectsTest: \nssh lwm2mError observe response: {}", e.toString());
                }
            });
        }
    }

    private ModelClient getClientModelWithValue(LeshanServer lwServer, Registration registration) {
//        try {
//            ObserveResponse observeResponse = lwServer.send(registration, new ObserveRequest(3303));
//            log.info("getClientModelWithValue: \ntest 3303 observeResponse: {}", observeResponse);
//            log.info("getClientModelWithValue: \nendPoint: {} \n oblectId: {}", registration.getEndpoint(), 3303);
//            LwM2mResponse cResponse = lwM2MTransportRequest.doGet(lwServer, registration.getEndpoint(), "/" + 3303, GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
//            log.info("getClientModelWithValue: \nGET cResponse: {} \n target: {}", cResponse, 3303);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        ModelClient modelClient = new ModelClient(registration.getAdditionalRegistrationAttributes());
//        log.info("getClientModelWithValue: \nmodelClient: {} \n context.getSessions(): {}", modelClient, context.getSessions());
//        log.info("getClientModelWithValue: \ncontext.getSessions().get(registration.getEndpoint()).getCredentialsBody(): {}", context.getSessions().get(registration.getEndpoint()).getCredentialsBody());
        String credentials = (context.getSessions() != null && context.getSessions().size() > 0) ? context.getSessions().get(registration.getEndpoint()).getCredentialsBody() : null;
//        log.info("getClientModelWithValue: \ncredentials: {}", credentials);

        JsonObject objectMsg = (credentials != null) ? adaptor.validateJson(credentials) : null;
        JsonArray clientObserves = (objectMsg != null &&
                !objectMsg.isJsonNull() &&
                objectMsg.has("observe") &&
                objectMsg.get("observe").isJsonArray() &&
                !objectMsg.get("observe").isJsonNull()) ? objectMsg.get("observe").getAsJsonArray() : null;
//        log.info("getClientModelWithValue: \nregistration.getObjectLinks().length: {}", registration.getObjectLinks().length);

        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            String[] objects = url.getUrl().split("/");


            try {
                if (objects.length > 2) {
                    int objectId = Integer.parseInt(objects[1]);
                    log.info("getClientModelWithValue: \nnew ObserveRequest: {}, \nobjectId: {}", new ObserveRequest(objectId), objectId);
                    log.info("getClientModelWithValue: \nregistration: {},", registration);
                    ObserveResponse observeResponse = lwServer.send(registration, new ObserveRequest(objectId));
                    log.info("getClientModelWithValue: \nobserveResponse: {}", observeResponse);
                    LwM2mNode content = observeResponse.getContent();
                    Map<Integer, LwM2mObjectInstance> instances = ((LwM2mObject) content).getInstances();
                    ObjectModel objectModel = lwServer.getModelProvider().getObjectModel(registration).getObjectModel(objectId);
                    if (objectModel != null) {
                        ModelObject modelObject = new ModelObject(objectModel.id, instances, objectModel, null);
                        modelClient.getModelObjects().add(modelObject);
                        if (clientObserves != null && clientObserves.size() > 0) {
                            clientObserves.forEach(val -> {
                                if (Integer.valueOf(val.getAsString().split("/")[1]) == objectId && val.getAsString().split("/").length > 3) {
                                    modelObject.getObjectObserves().add(val.getAsString());
                                }
                            });
                        }
                    } else {
                        log.error("[{}] - error getModelProvider", objects[1]);
                    }
//                      cancel observation : active way
                    Observation observation = observeResponse.getObservation();
                    CancelObservationResponse cancelResponse = lwServer.send(registration,
                            new CancelObservationRequest(observation));
                    log.info("cancelResponse: {}", cancelResponse);
                    /**
                     * active cancellation does not remove observation from store : it should be done manually using
                     */
                    lwServer.getObservationService().cancelObservation(observation);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        return modelClient;
    }

    private LwM2mNode getLvM2mNodeToObject(LwM2mNode content) {
        if (content instanceof LwM2mObject) {
            return (LwM2mObject) content;
        } else if (content instanceof LwM2mObjectInstance) {
            return (LwM2mObjectInstance) content;
        } else if (content instanceof LwM2mSingleResource) {
            return (LwM2mSingleResource) content;
        } else if (content instanceof LwM2mMultipleResource) {
            return (LwM2mMultipleResource) content;
        }
        return null;
    }

    public void observOnResponse(LeshanServer lhServer, Observation observation, Registration registration, ObserveResponse response) {
        if (response.getContent() instanceof LwM2mObject) {
            LwM2mObject content = (LwM2mObject) response.getContent();
            log.info("[{}] \nobject: {}", registration.getEndpoint(), content.getId());
        } else if (response.getContent() instanceof LwM2mObjectInstance) {
            LwM2mObjectInstance content = (LwM2mObjectInstance) response.getContent();
            log.info("[{}] \ninstanve: {}", registration.getEndpoint(), content.getId());
        } else if (response.getContent() instanceof LwM2mSingleResource) {
            LwM2mSingleResource content = (LwM2mSingleResource) response.getContent();
            log.info("[{}] \n LwM2mSingleResource: {}", registration.getEndpoint(), content.getId());
        } else if (response.getContent() instanceof LwM2mMultipleResource) {
            LwM2mMultipleResource content = (LwM2mMultipleResource) response.getContent();
            log.info("[{}] \n LwM2mMultipleResource: {}", registration.getEndpoint(), content.getId());
        }
    }

    /**
     * /clients/endPoint/LWRequest/discover : do LightWeight M2M discover request on a given client.
     */
    public DiscoverResponse getDiscover(LeshanServer lwServer, String target, String clientEndpoint, String timeoutParam) throws InterruptedException {
        DiscoverRequest request = new DiscoverRequest(target);
        return lwServer.send(lwServer.getRegistrationService().getByEndpoint(clientEndpoint), request, this.context.getTimeout());
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
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);

            }
        }
    }
//
//    public Registration getRegistration(LeshanServer lwServer, String clientEndpoint) {
//        return lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
//    }

    @SneakyThrows
    public ReadResultAttrTel doGetAttributsTelemetry(LeshanServer lwServer, String clientEndpoint) {
        ReadResultAttrTel readResultAttrTel = new ReadResultAttrTel();
        Registration registration = lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
        registration.getAdditionalRegistrationAttributes().entrySet().forEach(entry -> {
            log.info("Attributes: Key : [{}] Value : [{}]", entry.getKey(), entry.getValue());
            readResultAttrTel.getPostAttribute().addProperty(entry.getKey(), entry.getValue());
        });
        lwServer.getModelProvider().getObjectModel(registration).getObjectModels().forEach(om -> {
            String idObj = String.valueOf(om.id);
            LwM2mResponse cResponse = lwM2MTransportRequest.doGet(lwServer, clientEndpoint, "/" + idObj, GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
            log.info("GET cResponse: {} \n target: {}", cResponse, idObj);
            if (cResponse != null) {
                LwM2mNode content = ((ReadResponse) cResponse).getContent();
                ((LwM2mObject) content).getInstances().entrySet().stream().forEach(instance -> {
                    String instanceId = String.valueOf(instance.getValue().getId());
                    om.resources.entrySet().stream().forEach(resOm -> {
                        String attrTelName = om.name + "_" + instanceId + "_" + resOm.getValue().name;
                        /** Attributs: om.id: Security, Server, ACL & 'R' ? */
                        if (om.id <= 2) {
//                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, "");
                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, "");
                        } else {
                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, "");
//                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, "");
                        }
                    });

                    instance.getValue().getResources().entrySet().stream().forEach(resource -> {
                        int resourceId = resource.getValue().getId();
                        String resourceValue = getResourceValueToString(resource.getValue());
                        String attrTelName = om.name + "_" + instanceId + "_" + om.resources.get(resourceId).name;
                        log.info("resource.getValue() [{}] : [{}] -> [{}]", attrTelName, resourceValue, om.resources.get(resourceId).operations.name());
                        if (readResultAttrTel.getPostAttribute().has(attrTelName)) {
                            readResultAttrTel.getPostAttribute().remove(attrTelName);
                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, resourceValue);
                        } else if (readResultAttrTel.getPostTelemetry().has(attrTelName)) {
                            readResultAttrTel.getPostTelemetry().remove(attrTelName);
                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, resourceValue);
                        }
                    });

                });
            }
        });
        return readResultAttrTel;
    }


    /**
     * Trigger
     */
    private void startTriggerServer(LeshanServer lwServer, String endPoint) {
        LwM2mResponse cResponse = this.lwM2MTransportRequest.doGet(lwServer, endPoint, "/1", GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
        log.info("cResponse1: [{}]", cResponse);
        String target = "/1/0/8";
        cResponse = doTriggerServer(lwServer, endPoint, target, null);
        log.info("cResponse2: [{}]", cResponse);
        if (cResponse == null || cResponse.getCode().getCode() == 500) {
            target = "/3/0/5";
            cResponse = doTriggerServer(lwServer, endPoint, target, null);
            log.info("cResponse3: [{}]", cResponse);
        }
    }

    public LwM2mResponse doTriggerServer(LeshanServer lwServer, String clientEndpoint, String target, String param) {
        param = param != null ? param : "";
        return lwM2MTransportRequest.doPost(lwServer, clientEndpoint, target, POST_TYPE_OPER_EXECUTE, ContentFormat.TLV.getName(), param);
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
