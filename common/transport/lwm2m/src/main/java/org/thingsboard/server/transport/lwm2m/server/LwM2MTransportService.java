/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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


import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;

import static org.eclipse.leshan.server.bootstrap.DefaultBootstrapHandler.DEFAULT_TIMEOUT;
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
        /**
         * Info about start registration device
         */
        log.info("Received endpoint registration version event: \nendPoint: {}\nregistration.getId(): {}\npreviousObsersations {}", registration.getEndpoint(), registration.getId(), previousObsersations);
        /**
         * Create session: Map<String <registrationId >, ModelClient>
         * Add Model (Entity) for client (from registration & observe) by registration.getId
         * Remove from sessions Model by enpPoint
         * !! Update / add information based on query results later
         */

        ModelClient modelClient = lwM2mInMemorySecurityStore.replaceNewRegistration(lwServer, registration, this);
        if (modelClient != null) {
            setModelClient(lwServer, registration, modelClient);
        }

        /**
         * Get urls resources device only possible Observerable === or atrr or telemetry
         * Not Observe without  or atrr or telemetry
         */

        /**
         * set observation fromSessionInfoProto
         */

        /**
         * upgrade attr/telemetry to thingsboard -> device
         */

        /**
         * start observe to thingsboard -> device
         */
    }

    public void updatedReg(LeshanServer lwServer, Registration registration) {
        String endpointId = registration.getEndpoint();
//        String smsNumber = registration.getSmsNumber() == null ? "" : registration.getSmsNumber();
//        String lwm2mVersion = registration.getLwM2mVersion();
//        setCancelAllObservation(lwServer, registration);
        log.info("[{}]Received endpoint updated registration version event (next observe1), \nregistration.getId(): {}", endpointId, registration.getId());
    }

    public void unReg(Registration registration, Collection<Observation> observations) {
//        SecurityInfo securityInfo = lwM2mInMemorySecurityStore.remove(registration, false);
        lwM2mInMemorySecurityStore.setRemoveSessions(registration.getId());
        lwM2mInMemorySecurityStore.remove();
        log.info("[{}] Received endpoint un registration version event, registration.getId(): {}", registration.getEndpoint(), registration.getId());
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

    private void setModelClient(LeshanServer lwServer, Registration registration, ModelClient modelClient) {
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            String[] objects = url.getUrl().split("/");
            if (objects.length == 3) {
                int objectId = Integer.parseInt(objects[1]);
                modelClient.addPendingRequests((Integer) objectId);
            }
        });
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            String[] objects = url.getUrl().split("/");
            if (objects.length == 3) {
                String target = "/" + objects[1];
//                log.info("22) setModelClient: objectId {}", target);
                this.sendAllRequest(lwServer, registration, target, GET_TYPE_OPER_OBSERVE, null, modelClient, null);
            }
        });
    }

    public void sendAllRequest(LeshanServer lwServer, Registration registration, String target, String typeOper, String contentFormatParam, ModelClient modelClient, Observation observation) {
        lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, GET_TYPE_OPER_OBSERVE, null, modelClient, null);
    }

    @SneakyThrows
    public void getAttrTelemetryObserveFromModel(LeshanServer lwServer, String registrationId, ModelClient modelClient) {
        log.info("51) getAttrTelemetryObserveFromModelintegratioId: {}", registrationId);
        String credentials = (lwM2mInMemorySecurityStore.getSessions() != null && lwM2mInMemorySecurityStore.getSessions().size() > 0) ? lwM2mInMemorySecurityStore.getSessions().get(registrationId).getCredentialsResponse().getCredentialsBody() : null;
        JsonObject objectMsg = (credentials != null) ? adaptor.validateJson(credentials) : null;

        /**
         * Example: with pathResource (use only pathResource)
         *{"observe":["/2/0","/2/0/0","/4/0/2"],
         * "attribute":["/2/0/1","/3/0/9"],
         * "telemetry":["/1/0/1","/2/0/1","/6/0/1"]}
         */
        JsonObject clientObserveAttrTelemetry = (objectMsg != null &&
                !objectMsg.isJsonNull() &&
                objectMsg.has(OBSERVE_ATTRIBUTE_TELEMETRY) &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).isJsonObject() &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).isJsonNull()) ? objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject() : null;

        /**
         * Add info from clientObserveAttrTelemetry(credentials) to ModelClient
         */
        modelClient.setClientObserveAttrTelemetry(clientObserveAttrTelemetry);

        /**
         * Client`s starting info  to  send to thingsboard
         */
        ReadResultAttrTel readResultAttrTel = doGetAttributsTelemetryObserve(lwServer, modelClient);
        processDevicePublish(readResultAttrTel.getPostAttribute(), DEVICE_ATTRIBUTES_TOPIC, -1, registrationId);
        processDevicePublish(readResultAttrTel.getPostTelemetry(), DEVICE_TELEMETRY_TOPIC, -1, registrationId);
    }

    @SneakyThrows
    public ReadResultAttrTel doGetAttributsTelemetryObserve(LeshanServer lwServer, ModelClient modelClient) {
        ReadResultAttrTel readResultAttrTel = new ReadResultAttrTel();
        log.info("52) getAttrTelemetryObserve From ModelintegratioId: {}", modelClient.getClientObserveAttrTelemetry());
        JsonArray attributes = modelClient.getClientObserveAttrTelemetry().get(ATTRIBUTE).getAsJsonArray();
        JsonArray telemetrys = modelClient.getClientObserveAttrTelemetry().get(TELEMETRY).getAsJsonArray();
        JsonArray observes = modelClient.getClientObserveAttrTelemetry().get(OBSERVE).getAsJsonArray();
        Registration registration = lwServer.getRegistrationService().getByEndpoint(modelClient.getEndPoint());
        registration.getAdditionalRegistrationAttributes().entrySet().forEach(entry -> {
            log.info("Attributes: Key : [{}] Value : [{}]", entry.getKey(), entry.getValue());
            readResultAttrTel.getPostAttribute().addProperty(entry.getKey(), entry.getValue());
        });
        setAttrTelemtryResources(attributes, modelClient, readResultAttrTel, ATTRIBUTE);
        setAttrTelemtryResources(telemetrys, modelClient, readResultAttrTel, TELEMETRY);
        observes.forEach(path -> {
            if (getValidateObserve(readResultAttrTel, path.getAsString().toString()))
                readResultAttrTel.getPostObserve().add(path.getAsString().toString());
        });
        return readResultAttrTel;
    }

    @SneakyThrows
    private void setAttrTelemtryResources(JsonArray params, ModelClient modelClient, ReadResultAttrTel readResultAttrTel, String type) {
        params.forEach(param -> {
            String path = param.getAsString().toString();
            String[] paths = path.split("/");
            if (paths.length > 3) {
                int objId = Integer.parseInt(paths[1]);
                int insId = Integer.parseInt(paths[2]);
                int resId = Integer.parseInt(paths[3]);
                if (modelClient.getModelObjects().get(objId) != null) {
                    ModelObject modelObject = modelClient.getModelObjects().get(objId);
                    String resName = modelObject.getObjectModel().resources.get(resId).name;
//                String attrTelName = om.name + "_" + instanceId + "_" + om.resources.get(resourceId).name;
                    if (modelObject.getInstances().get(insId) != null) {
                        LwM2mObjectInstance instance = modelObject.getInstances().get(insId);
                        if (instance.getResource(resId) != null) {
                            String resValue = (instance.getResource(resId).isMultiInstances()) ?
                                    instance.getResource(resId).getValues().toString() :
                                    instance.getResource(resId).getValue().toString();
                            if (resName != null && !resName.isEmpty() && resValue != null) {
                                switch (type) {
                                    case ATTRIBUTE:
                                        readResultAttrTel.getPostAttribute().addProperty(resName, resValue);
                                        readResultAttrTel.getPathResAttrTelemetry().add(path);
                                        break;
                                    case TELEMETRY:
                                        readResultAttrTel.getPostTelemetry().addProperty(resName, resValue);
                                        readResultAttrTel.getPathResAttrTelemetry().add(path);
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private boolean getValidateObserve(ReadResultAttrTel readResultAttrTel, String path) {
        return (Arrays.stream(readResultAttrTel.getPathResAttrTelemetry().stream().toArray()).filter(pathRes ->
                path.equals(pathRes)).findAny().orElse(null) != null);

    }


    public void processDevicePublish(JsonElement msg, String topicName, int msgId, String registrationId) {
        TransportProtos.SessionInfoProto sessionInfo = getValidateSessionInfo(registrationId);
        if (sessionInfo != null) {
            try {
                if (topicName.equals(LwM2MTransportHandler.DEVICE_TELEMETRY_TOPIC)) {
                    TransportProtos.PostTelemetryMsg postTelemetryMsg = adaptor.convertToPostTelemetry(msg);
                    transportService.process(sessionInfo, postTelemetryMsg, getPubAckCallback(msgId, postTelemetryMsg));
                } else if (topicName.equals(LwM2MTransportHandler.DEVICE_ATTRIBUTES_TOPIC)) {
                    TransportProtos.PostAttributeMsg postAttributeMsg = adaptor.convertToPostAttributes(msg);
                    transportService.process(sessionInfo, postAttributeMsg, getPubAckCallback(msgId, postAttributeMsg));
                }
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);

            }
        }
    }

    private TransportProtos.SessionInfoProto getValidateSessionInfo(String registrationId) {
        TransportProtos.SessionInfoProto sessionInfo = null;
        ModelClient modelClient = lwM2mInMemorySecurityStore.getByModelClient(registrationId);
        TransportProtos.ValidateDeviceCredentialsResponseMsg msg = modelClient.getCredentialsResponse();
        if (msg == null || msg.getDeviceInfo() == null) {
            log.warn("[{}] [{}]", modelClient.getEndPoint(), CONNECTION_REFUSED_NOT_AUTHORIZED.toString());
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


    private void cancelAllObservation(LeshanServer lwServer, Registration registration) {
        /**
         * cancel observation : active way
         * forech HashMap observe response
         *
         */

        Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
        observations.forEach(observation -> {
            CancelObservationRequest request = new CancelObservationRequest((observation));
        });

//
//                });
//        Observation observation = observeResponse.getObservation();
//        CancelObservationResponse cancelResponse = lwServer.send(registration, new CancelObservationRequest(observation));
//        log.info("cancelResponse: {}", cancelResponse);
        /**
         * active cancellation does not remove observation from store : it should be done manually using
         */
//        lwServer.getObservationService().cancelObservation(observation);
    }

    private void getStartObsrerveObjects(LeshanServer lwServer, Registration registration) {
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            String[] objects = url.getUrl().split("/");
            if (objects.length > 2) {
                int objectId = Integer.parseInt(objects[1]);
                new ObserveRequest(objectId, 0);
//                lwM2MTransportRequest.sendRequest(lwServer, registration, new ObserveRequest(objectId));
                log.info("9) getStartObsrerveObjects: do \n objectId: {}", objectId);
//                if (objectId != 1 && objectId !=6) {
//                    DiscoverRequest request = new DiscoverRequest(objectId);
////                    ObserveRequest request = new ObserveRequest(objectId);
//                    LwM2mResponse oResponse = lwM2MTransportRequest.sendRequest(lwServer, registration, request);
                LwM2mResponse response = lwM2MTransportRequest.doGet(lwServer, registration, "/" + objectId, LwM2MTransportHandler.GET_TYPE_OPER_READ, ContentFormat.TLV.getName());

                log.info("10) getStartObsrerveObjects: \n objectId: {} \n oRequest : {}", objectId, response);
//                }

            }
        });
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


//    private ObserveResponse setObservResource(LeshanServer lwServer, Registration registration, Integer objectId, Integer instanceId, Integer resourceId) {
//        log.info("Endpoint: [{}] Object: [{}] Instnce: [{}] Resoutce: [{}] Received endpoint ObserveResponse", registration.getEndpoint(), objectId, instanceId, resourceId);
//
//        ObserveResponse observeResponse = null;
//        // observe device timezone
//        observeResponse = (ObserveResponse) this.getObserve(lwServer, registration, new ObserveRequest(objectId, instanceId, resourceId));
//        Observation observation = observeResponse.getObservation();
//        Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
//        LwM2mSingleResource resource = (LwM2mSingleResource) observeResponse.getContent();
//        Object value = (resource != null) ? resource.getValue() : null;
//        LwM2mPath path = (observeResponse.getObservation() != null) ? observeResponse.getObservation().getPath() : null;
//        log.info("Endpoint: [{}] Path: [{}] Value: [{}] Received endpoint ObserveResponse", registration.getEndpoint(), path, value);
////            log.info("[{}] [{}] Received endpoint observation", registration.getEndpoint(), observation);
////            log.info("[{}] [{}] Received endpoint observations", registration.getEndpoint(), observations);
//
//
//        return observeResponse;
//    }

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

    public void setCancelAllObservation(LeshanServer lwServer, Registration registration) {
        log.info("33)  setCancelObservationObjects endpoint: {}", registration.getEndpoint());
        int cancel = lwServer.getObservationService().cancelObservations(registration);
        Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
//        if (registration != null) {
//            observations.forEach(observation -> {
        log.info("33_1)  setCancelObservationObjects endpoint: {} cancel: {}", registration.getEndpoint(), cancel);
//                this.sendAllRequest(lwServer, registration, observation.getPath().toString(), POST_TYPE_OPER_OBSERVE_CANCEL, null, null, observation);
//                log.info("33_2)  setCancelObservationObjects endpoint: {}", registration.getEndpoint());
//                lwServer.getObservationService().cancelObservation(observation);
//                log.info("33_3)  setCancelObservationObjects endpoint: {}", registration.getEndpoint());
//                /**
//                 * active cancellation does not remove observation from store : it should be done manually using
//                 */
//                lwServer.getObservationService().cancelObservations(registration, observation.getPath().toString());
//                log.info("33_4)  setCancelObservationObjects endpoint: {}/\n target {}", registration.getEndpoint(), observation.getPath().toString());
//            });
//        }
//        if (registration != null) {
////            CountDownLatch cancelLatch = new CountDownLatch(1);
//            Arrays.stream(registration.getObjectLinks()).forEach(url -> {
//                String[] objects = url.getUrl().split("/");
//                if (objects.length > 2) {
//                    String target = "/" + objects[1];
//
////                    CancelObservationResponse cancelResponse = lwServer.send(registration, new CancelObservationRequest(observation));
////                    log.info("cancelResponse: {}", cancelResponse);
//                    /**
//                     * active cancellation does not remove observation from store : it should be done manually using
//                     */
//                    lwServer.getObservationService().cancelObservations(registration, target);
//                    log.info("33_1)  setCancelObservationObjects endpoint: {}/\n target {}", registration.getEndpoint(), target);
//                }
//            });
//            cancelLatch.countDown();
//            try {
//                cancelLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
//            } catch (InterruptedException e) {
//
//            }
//        }
    }

//    private ModelClient getClientModelWithValue(LeshanServer lwServer, Registration registration) {
//        log.info("1) getClientModelWithValue  start: \n registration: {}", registration);
//        ModelClient modelClient = new ModelClient(registration.getAdditionalRegistrationAttributes());
//        String credentials = (context.getSessions() != null && context.getSessions().size() > 0) ? context.getSessions().get(registration.getEndpoint()).getCredentialsBody() : null;
//        JsonObject objectMsg = (credentials != null) ? adaptor.validateJson(credentials) : null;
//        JsonArray clientObserves = (objectMsg != null &&
//                !objectMsg.isJsonNull() &&
//                objectMsg.has("observe") &&
//                objectMsg.get("observe").isJsonArray() &&
//                !objectMsg.get("observe").isJsonNull()) ? objectMsg.get("observe").getAsJsonArray() : null;
//
//        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
//            String[] objects = url.getUrl().split("/");
//            try {
//                if (objects.length > 2) {
//                    int objectId = Integer.parseInt(objects[1]);
//                    log.info("2) getClientModelWithValue  start: \n observeResponse do");
//                    ObserveResponse observeResponse = (ObserveResponse) this.getObserve(lwServer, registration, new ObserveRequest(objectId));
//                    log.info("3) getClientModelWithValue  start: \n observeResponse after");
//                    LwM2mNode content = observeResponse.getContent();
//
//                    Map<Integer, LwM2mObjectInstance> instances = ((LwM2mObject) content).getInstances();
//                    ObjectModel objectModel = lwServer.getModelProvider().getObjectModel(registration).getObjectModel(objectId);
//                    if (objectModel != null) {
//                        ModelObject modelObject = new ModelObject(objectModel.id, instances, objectModel, null);
//                        modelClient.getModelObjects().add(modelObject);
//                        if (clientObserves != null && clientObserves.size() > 0) {
//                            clientObserves.forEach(val -> {
//                                if (Integer.valueOf(val.getAsString().split("/")[1]) == objectId && val.getAsString().split("/").length > 3) {
//                                    modelObject.getObjectObserves().add(val.getAsString());
//                                }
//                            });
//                        }
//                    } else {
//                        log.error("[{}] - error getModelProvider", objects[1]);
//                    }
////                      cancel observation : active way
//                    Observation observation = observeResponse.getObservation();
//                    CancelObservationResponse cancelResponse = lwServer.send(registration, new CancelObservationRequest(observation));
//                    log.info("cancelResponse: {}", cancelResponse);
//                    /**
//                     * active cancellation does not remove observation from store : it should be done manually using
//                     */
//                    lwServer.getObservationService().cancelObservation(observation);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//        return modelClient;
//    }

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
            log.info("observOnResponse: \nEndpoint: [{}] object: {} \n observation: {} \n content: {}", registration.getEndpoint(), content.getId(), observation, content);
            String target = "/" + content.getId();
            log.info("observOnResponseCancel: \n target {}", target);
////            lhServer.getObservationService().cancelObservations(registration, target);
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


//
//    public Registration getRegistration(LeshanServer lwServer, String clientEndpoint) {
//        return lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
//    }


    @SneakyThrows
    public ReadResultAttrTel doGetAttributsTelemetryObserve1(LeshanServer lwServer, String clientEndpoint) {
        ReadResultAttrTel readResultAttrTel = new ReadResultAttrTel();
        Registration registration = lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
        registration.getAdditionalRegistrationAttributes().entrySet().forEach(entry -> {
            log.info("Attributes: Key : [{}] Value : [{}]", entry.getKey(), entry.getValue());
            readResultAttrTel.getPostAttribute().addProperty(entry.getKey(), entry.getValue());
        });
        lwServer.getModelProvider().getObjectModel(registration).getObjectModels().forEach(om -> {
            String idObj = String.valueOf(om.id);
            LwM2mResponse cResponse = this.getObserve(lwServer, registration, new ObserveRequest(om.id));
//            LwM2mResponse cResponse = lwM2MTransportRequest.doGet(lwServer, clientEndpoint, "/" + idObj, GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
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
            LwM2mResponse cResponse = lwM2MTransportRequest.doGet(lwServer, registration, "/" + idObj, LwM2MTransportHandler.GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
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
    private void startTriggerServer(LeshanServer lwServer, Registration registration) {
        LwM2mResponse cResponse = this.lwM2MTransportRequest.doGet(lwServer, registration, "/1", LwM2MTransportHandler.GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
        log.info("cResponse1: [{}]", cResponse);
        String target = "/1/0/8";
        cResponse = doTriggerServer(lwServer, registration, target, null);
        log.info("cResponse2: [{}]", cResponse);
        if (cResponse == null || cResponse.getCode().getCode() == 500) {
            target = "/3/0/5";
            cResponse = doTriggerServer(lwServer, registration, target, null);
            log.info("cResponse3: [{}]", cResponse);
        }
    }

    public LwM2mResponse doTriggerServer(LeshanServer lwServer, Registration registration, String target, String param) {
        param = param != null ? param : "";
        return lwM2MTransportRequest.doPost(lwServer, registration.getEndpoint(), target, LwM2MTransportHandler.POST_TYPE_OPER_EXECUTE, ContentFormat.TLV.getName(), param);
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

//    public LwM2mResponse getObserveObject1(LeshanServer lwServer, Registration registration, int objectId) {
//        final LwM2mResponse[] responseRez = new LwM2mResponse[1];
//        CountDownLatch respLatch = new CountDownLatch(1);
//        if (registration != null) {
//            log.info("2) getObserve  start: \n observeResponse do");
//            lwServer.send(registration, new ObserveRequest(objectId), (ResponseCallback) response -> {
//                log.info("5) getObserve: \nresponse: {}", response);
//                respLatch.countDown();
//            }, e -> {
//                log.error("6) getCObservationObjectsTest: \nssh lwm2mError observe response: {}", e.toString());
//                respLatch.countDown();
//            });
//            try {
//                respLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
//            } catch (InterruptedException e) {
//
//            }
//        }
//        return responseRez[0];
//    }

    public LwM2mResponse getObserve(LeshanServer lwServer, Registration registration, ObserveRequest observeRequest) {
        final LwM2mResponse[] responseRez = new LwM2mResponse[1];
        CountDownLatch respLatch = new CountDownLatch(1);
        if (registration != null) {
            lwServer.send(registration, observeRequest, (ResponseCallback) response -> {
                responseRez[0] = response;
                respLatch.countDown();
            }, e -> {
                log.error("6) getCObservationObjectsTest: \nssh lwm2mError observe response: {}", e.toString());
                respLatch.countDown();
            });
            try {
                respLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {

            }
        }
        return responseRez[0];
    }
}
