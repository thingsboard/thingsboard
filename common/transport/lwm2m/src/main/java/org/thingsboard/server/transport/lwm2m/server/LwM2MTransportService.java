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

import com.google.gson.Gson;
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
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.transport.lwm2m.server.client.AttrTelemetryObserveValue;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MJsonAdaptor;
import org.thingsboard.server.transport.lwm2m.server.client.ModelClient;
import org.thingsboard.server.transport.lwm2m.server.client.ModelObject;
import org.thingsboard.server.transport.lwm2m.server.client.ResultsAnalyzerParameters;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    public LwM2MTransportContextServer context;

    @Autowired
    private LwM2MTransportRequest lwM2MTransportRequest;

    @Autowired
    LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    /**
     * Start registration device
     * Create session: Map<String <registrationId >, ModelClient>
     * 1. replaceNewRegistration -> (solving the problem of incorrect termination of the previous session with this endpoint)
     * 1.1 When we initialize the registration, we register the session by endpoint.
     * 1.2 If the server has incomplete requests (canceling the registration of the previous session),
     * delete the previous session only by the previous registration.getId
     * 1.2 Add Model (Entity) for client (from registration & observe) by registration.getId
     * 1.2 Remove from sessions Model by enpPoint
     * Next ->  Create new ModelClient for current session -> setModelClient...
     *
     * @param lwServer             - LeshanServer
     * @param registration         - Registration LwM2M Client
     * @param previousObsersations - may be null
     */
    public void onRegistered(LeshanServer lwServer, Registration registration, Collection<Observation> previousObsersations) {
        log.info("Received endpoint registration version event: \nendPoint: {}\nregistration.getId(): {}\npreviousObsersations {}", registration.getEndpoint(), registration.getId(), previousObsersations);
        ModelClient modelClient = lwM2mInMemorySecurityStore.replaceNewRegistration(lwServer, registration, this);
        if (modelClient != null) {
            modelClient.setLwM2MTransportService(this);
            setModelClient(lwServer, registration, modelClient);
            SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration.getId());
            transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(SessionEvent.OPEN), null);
            transportService.registerAsyncSession(sessionInfo, new LwM2MSessionMsgListener(sessionId, this));
        }
    }

    /**
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     */

    public void updatedReg(LeshanServer lwServer, Registration registration) {
        log.info("[{}] endpoint updateReg, registration.getId(): {}", registration.getEndpoint(), registration.getId());
    }

    /**
     * @param registration - Registration LwM2M Client
     * @param observations - All paths observations before unReg
     *                     !!! Warn: if have not finishing unReg, then this operation will be finished on next Client`s connect
     */
    public void unReg(Registration registration, Collection<Observation> observations) {
        lwM2mInMemorySecurityStore.setRemoveSessions(registration.getId());
        lwM2mInMemorySecurityStore.remove();
        log.info("[{}] Received endpoint un registration version event, registration.getId(): {}", registration.getEndpoint(), registration.getId());

    }

    public void onSleepingDev(Registration registration) {
        String endpointId = registration.getEndpoint();
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

    /**
     * Create new ModelClient for current session -> setModelClient...
     * #1   Add all ObjectLinks (instance) to control the process of executing requests to the client
     * to get the client model with current values
     * #2   Get the client model with current values. Analyze the response in -> lwM2MTransportRequest.sendResponse
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     * @param modelClient  - object with All parameters off client
     */
    private void setModelClient(LeshanServer lwServer, Registration registration, ModelClient modelClient) {
        // #1
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            String[] objects = url.getUrl().split("/");
            if (objects.length == 3) {
                modelClient.addPendingRequests(url.getUrl());
            }
        });
        // #2
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            String[] objects = url.getUrl().split("/");
            if (objects.length == 3) {
                String target = url.getUrl();
                lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, GET_TYPE_OPER_READ,
                        ContentFormat.TLV.getName(), modelClient, null, "", context.getTimeout());
            }
        });
    }

    /**
     * Add attribute telemetry information from credentials to client model and start observe
     * !!! if the resource has an observation, but no telemetry or attribute - the observation will not use
     * #1 Client`s starting info  to  send to thingsboard
     * #2 Sending Attribute Telemetry with value to thingsboard only once at the start of the connection
     * #3 Start observe
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     * @param modelClient  - object with All parameters off client
     */
    @SneakyThrows
    public void getAttrTelemetryObserveFromModel(LeshanServer lwServer, Registration registration, ModelClient modelClient) {
        // #1
        this.setAttributsTelemetryObserveToModelClient(registration, modelClient);
        // #2
        this.setAttrTelemetryToThingsboard(registration.getId());
        // #3
        this.onSentObserveToClient(lwServer, registration);
    }

    /**
     * Sent All Attribute and Telemetry to Thingsboard
     *
     * @param registrationId - Id of Registration LwM2M Client
     */
    private void setAttrTelemetryToThingsboard(String registrationId) {
        AttrTelemetryObserveValue parametersAllWithValue = lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registrationId).getAttrTelemetryObserveValue();
        JsonObject telemetrys = this.getParametersFromModelClient(parametersAllWithValue.getPostAttribute());
        this.setToDeviceAttributeTelemetry(telemetrys, DEVICE_ATTRIBUTES_TOPIC, -1, registrationId);
        JsonObject attributes = this.getParametersFromModelClient(parametersAllWithValue.getPostTelemetry());
        this.setToDeviceAttributeTelemetry(attributes, DEVICE_TELEMETRY_TOPIC, -1, registrationId);
    }

    /**
     * Get Attribute or Telemetry in format  {name(Attr/Telemetry):value} for Thingsboard
     *
     * @param parametersAllWithValue - JsonObjects: {path: {name(Attr/Telemetry) : value}
     * @return result: new: JsonObject: {name(Attr/Telemetry):value}
     */
    private JsonObject getParametersFromModelClient(JsonObject parametersAllWithValue) {
        JsonObject parametersWithValue = new JsonObject();
        parametersAllWithValue.entrySet().forEach(value -> {
            Map.Entry<String, JsonElement> parameter = value.getValue().getAsJsonObject().entrySet().stream().findFirst().get();
            parametersWithValue.addProperty(parameter.getKey(), parameter.getValue().getAsString().toString());
        });
        return parametersWithValue;
    }


    /**
     * To Model Client: Attribute, Telemetry, Observe (update )
     * #1 Add Attribute from LWM2M Client (ATTRIBUTE+attrId == id) ==> {path: {name: value}}
     * #2 Add Attribute from credentials (path to resource == id)  ==> {path: {name: value}}
     * #3 Add Telemetry from credentials (path to resource == id)  ==> {path: {name: value}}
     * #4 Add Attribute && Telemetry from credentials ==> [path]
     * #5 Add Observe from credentials, if observe is in Attribute or Telemetry (path to resource == id)  ==> [path]
     *
     * @param registration - Registration LwM2M Client
     * @param modelClient  - object with All parameters off client
     */
    @SneakyThrows
    public void setAttributsTelemetryObserveToModelClient(Registration registration, ModelClient modelClient) {
        JsonObject credentialsBody = getObserveAttrTelemetryFromThingsboard(registration);
        if (credentialsBody != null) {
            modelClient.getAttrTelemetryObserveValue().setPostAttributeProfile(credentialsBody.get(ATTRIBUTE).getAsJsonArray());
            modelClient.getAttrTelemetryObserveValue().setPostTelemetryProfile(credentialsBody.get(TELEMETRY).getAsJsonArray());
            modelClient.getAttrTelemetryObserveValue().setPostObserveProfile(credentialsBody.get(OBSERVE).getAsJsonArray());
            AtomicInteger attrId = new AtomicInteger();
            // #1
            modelClient.getAttrTelemetryObserveValue().setPostAttribute(new JsonObject());
            registration.getAdditionalRegistrationAttributes().entrySet().forEach(entry -> {
                JsonObject resNameValue = new JsonObject();
                resNameValue.getAsJsonObject().addProperty(entry.getKey(), entry.getValue());
                modelClient.getAttrTelemetryObserveValue().getPostAttribute().getAsJsonObject().add(ATTRIBUTE + attrId, resNameValue);
                attrId.getAndIncrement();
            });
            // #2 #3 #4 #5
            this.updateAttrTelemetryObserveValue(modelClient);
        }
    }

    private void updateAttrTelemetryObserveValue(ModelClient modelClient) {        // #2 && #4
        modelClient.getAttrTelemetryObserveValue().setPostTelemetry(new JsonObject());
        modelClient.getAttrTelemetryObserveValue().setPostObserve(ConcurrentHashMap.newKeySet());
        modelClient.getAttrTelemetryObserveValue().setPathResAttrTelemetry(ConcurrentHashMap.newKeySet());
        this.setAttrTelemtryResources(modelClient.getAttrTelemetryObserveValue().getPostAttributeProfile(), modelClient, modelClient.getAttrTelemetryObserveValue(), ATTRIBUTE);
        // #3 && #4
        this.setAttrTelemtryResources(modelClient.getAttrTelemetryObserveValue().getPostTelemetryProfile(), modelClient, modelClient.getAttrTelemetryObserveValue(), TELEMETRY);
        // #5
        this.setObserveResources(modelClient);
    }

    /**
     * @param registration - Registration LwM2M Client
     * @return credentialsBody with Observe&Attribute&Telemetry From Thingsboard
     * Example: with pathResource (use only pathResource)
     * {"attribute":["/2/0/1","/3/0/9"],
     * "telemetry":["/1/0/1","/2/0/1","/6/0/1"],
     * "observe":["/2/0","/2/0/0","/4/0/2"]}
     */
    private JsonObject getObserveAttrTelemetryFromThingsboard(Registration registration) {
        String credentialsBody = (lwM2mInMemorySecurityStore.getSessions() != null && lwM2mInMemorySecurityStore.getSessions().size() > 0) ?
                lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registration.getId()).getCredentialsResponse().getCredentialsBody() : null;
        JsonObject objectMsg = (credentialsBody != null) ? adaptor.validateJson(credentialsBody) : null;
        return (getValidateCredentialsBodyFromThingsboard(objectMsg)) ? objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject() : null;
    }

    private boolean getValidateCredentialsBodyFromThingsboard(JsonObject objectMsg) {
        return (objectMsg != null &&
                !objectMsg.isJsonNull() &&
                objectMsg.has(OBSERVE_ATTRIBUTE_TELEMETRY) &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).isJsonObject() &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(ATTRIBUTE) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(ATTRIBUTE).isJsonArray() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(TELEMETRY) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(TELEMETRY).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(TELEMETRY).isJsonArray() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().has(OBSERVE) &&
                !objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(OBSERVE).isJsonNull() &&
                objectMsg.get(OBSERVE_ATTRIBUTE_TELEMETRY).getAsJsonObject().get(OBSERVE).isJsonArray());
    }

    /**
     * Add Attribute || Telemetry from credentials (#2 || #3)
     * Add Attribute && Telemetry from credentials ==> [path] (#4)
     *
     * @param params                    -
     * @param modelClient               - object with All parameters off client
     * @param attrTelemetryObserveValue -
     * @param type                      -
     */
    @SneakyThrows
    private void setAttrTelemtryResources(JsonArray params, ModelClient modelClient, AttrTelemetryObserveValue attrTelemetryObserveValue, String type) {
        params.forEach(param -> {
            String path = (param.isJsonPrimitive()) ? param.getAsString().toString() : null;
            if (path != null) {
                String[] paths = path.split("/");
                if (paths.length > 3) {
                    int objId = Integer.parseInt(paths[1]);
                    int insId = Integer.parseInt(paths[2]);
                    int resId = Integer.parseInt(paths[3]);
                    if (modelClient.getModelObjects().get(objId) != null) {
                        ModelObject modelObject = modelClient.getModelObjects().get(objId);
                        String resName = modelObject.getObjectModel().resources.get(resId).name;
                        if (modelObject.getInstances().get(insId) != null) {
                            LwM2mObjectInstance instance = modelObject.getInstances().get(insId);
                            if (instance.getResource(resId) != null) {
                                String resValue = (instance.getResource(resId).isMultiInstances()) ?
                                        instance.getResource(resId).getValues().toString() :
                                        instance.getResource(resId).getValue().toString();
                                JsonObject resNameValue = createResNameValue(resName, resValue);
                                if (resNameValue != null) {
                                    switch (type) {
                                        case ATTRIBUTE:
                                            // #2 {path: {name: value}}
                                            attrTelemetryObserveValue.getPostAttribute().getAsJsonObject().add(path, resNameValue);
                                            // #4 add Attribute to All list ==> [path] (#4)
                                            attrTelemetryObserveValue.getPathResAttrTelemetry().add(path);
                                            break;
                                        case TELEMETRY:
                                            // #3 {path: {name: value}}
                                            attrTelemetryObserveValue.getPostTelemetry().getAsJsonObject().add(path, resNameValue);
                                            // #4 add Telemetry to All list ==> [path]
                                            attrTelemetryObserveValue.getPathResAttrTelemetry().add(path);
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * @param resValue - String from Resource as isMultiInstances/SingleResource
     * @param resName  - name Resource
     * @return JsonObject {name: value}
     */
    private JsonObject createResNameValue(String resName, String resValue) {
        JsonObject resNameValue = null;
        //
        if (resName != null && !resName.isEmpty() && resValue != null) {
            resNameValue = new JsonObject();
            resNameValue.getAsJsonObject().addProperty(resName, resValue);
        }
        return resNameValue;
    }

    /**
     * #5 Add Observe from credentials, if observe is in Attribute or Telemetry (path to resource == id)  ==> [path]
     *
     * @param modelClient - object with All parameters off client
     */
    @SneakyThrows
    private void setObserveResources(ModelClient modelClient) {
        modelClient.getAttrTelemetryObserveValue().getPostObserveProfile().forEach(path -> {
            if (getValidateObserve(modelClient, path.getAsString().toString()))
                modelClient.getAttrTelemetryObserveValue().getPostObserve().add(path.getAsString().toString());
        });
    }

    /**
     * ValidateObserve: if ath observe is in Attribute || Telemetry return true
     *
     * @param modelClient - object with All parameters off client
     * @param path        - observe from Thingsboard
     * @return - false if path observe is not present in Attribute or Telemetry
     */
    private boolean getValidateObserve(ModelClient modelClient, String path) {
        return (Arrays.stream(modelClient.getAttrTelemetryObserveValue().getPathResAttrTelemetry().toArray())
                .filter(path::equals).findAny().orElse(null) != null);
    }

    /**
     * Prepare Sent to Thigsboard callback - Attribute or Telemetry
     *
     * @param msg            - JsonObject: [{name: value}]
     * @param topicName      - Api Attribute or Telemetry
     * @param msgId          - always == -1
     * @param registrationId - Id of Registration LwM2M Client
     */
    public void setToDeviceAttributeTelemetry(JsonElement msg, String topicName, int msgId, String registrationId) {
        SessionInfoProto sessionInfo = getValidateSessionInfo(registrationId);
        if (sessionInfo != null) {
            try {
                if (topicName.equals(LwM2MTransportHandler.DEVICE_ATTRIBUTES_TOPIC)) {
                    PostAttributeMsg postAttributeMsg = adaptor.convertToPostAttributes(msg);
                    transportService.process(sessionInfo, postAttributeMsg, this.getPubAckCallbackSentAttrTelemetry(msgId, postAttributeMsg));
                } else if (topicName.equals(LwM2MTransportHandler.DEVICE_TELEMETRY_TOPIC)) {
                    PostTelemetryMsg postTelemetryMsg = adaptor.convertToPostTelemetry(msg);
                    transportService.process(sessionInfo, postTelemetryMsg, this.getPubAckCallbackSentAttrTelemetry(msgId, postTelemetryMsg));
                }
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);

            }
        }
    }

    /**
     * Sent to Thingsboard Attribute || Telemetry
     *
     * @param msgId - always == -1
     * @param msg   - JsonObject: [{name: value}]
     * @return - dummy
     */
    private <T> TransportServiceCallback<Void> getPubAckCallbackSentAttrTelemetry(final int msgId, final T msg) {
        return new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}] Success to publish msg: {}, dummy: {}", sessionId, msg, dummy);
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {}", sessionId, msg, e);
            }
        };
    }

    /**
     * @param registrationId - Id of Registration LwM2M Client
     * @return - sessionInfo after access connect client
     */
    private SessionInfoProto getValidateSessionInfo(String registrationId) {
        SessionInfoProto sessionInfo = null;
        ModelClient modelClient = lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registrationId);
        ValidateDeviceCredentialsResponseMsg msg = modelClient.getCredentialsResponse();
        if (msg == null || msg.getDeviceInfo() == null) {
            log.warn("[{}] [{}]", modelClient.getEndPoint(), CONNECTION_REFUSED_NOT_AUTHORIZED.toString());
        } else {
            sessionInfo = SessionInfoProto.newBuilder()
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
        }
        return sessionInfo;
    }

    /**
     * Sending observe value to thingsboard from ObservationListener.onResponse: object, instance, SingleResource or MultipleResource
     *
     * @param registration - Registration LwM2M Client
     * @param path         - observe
     * @param response     - observe
     */
    @SneakyThrows
    public void onObservationResponse(Registration registration, String path, ReadResponse response) {
        if (response.getContent() instanceof LwM2mObject) {
            LwM2mObject content = (LwM2mObject) response.getContent();
            String target = "/" + content.getId();
            log.info("observOnResponse Object: \n target {}", target);
        } else if (response.getContent() instanceof LwM2mObjectInstance) {
            LwM2mObjectInstance content = (LwM2mObjectInstance) response.getContent();
            log.info("[{}] \n observOnResponse instance: {}", registration.getEndpoint(), content.getId());
        } else if (response.getContent() instanceof LwM2mSingleResource) {
            LwM2mSingleResource content = (LwM2mSingleResource) response.getContent();
            this.onObservationSetResourcesValue(registration.getId(), content.getValue(), null, path);
        } else if (response.getContent() instanceof LwM2mMultipleResource) {
            LwM2mSingleResource content = (LwM2mSingleResource) response.getContent();
            this.onObservationSetResourcesValue(registration.getId(), null, content.getValues(), path);
        }
    }

    /**
     * Sending observe value of resources to thingsboard
     * #1 Return old Resource from ModelObject
     * #2 Create new Resource with value from observation
     * #3 Create new Resources from old Resources
     * #4 Update new Resources (replace old Resource on new Resource)
     * #5 Remove old Instance from modelClient
     * #6 Create new Instance with new Resources values
     * #7 Update modelClient.getModelObjects(idObject) (replace old Instance on new Instance)
     *
     * @param registrationId - Id of Registration LwM2M Client
     * @param value          - LwM2mSingleResource response.getContent()
     * @param values         - LwM2mSingleResource response.getContent()
     * @param path           - resource
     */
    @SneakyThrows
    private void onObservationSetResourcesValue(String registrationId, Object value, Map<Integer, ?> values, String path) {

        ResultIds resultIds = new ResultIds(path);
        // #1 resourceOld == LwM2mSingleResource [id=9, value=21, type=INTEGER]
        ModelClient modelClient = lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registrationId);
        ModelObject modelObject = modelClient.getModelObjects().get(resultIds.getObjectId());
        Map<Integer, LwM2mObjectInstance> instancesModelObject = modelObject.getInstances();
        LwM2mObjectInstance instanceOld = (instancesModelObject.get(resultIds.instanceId) != null) ? instancesModelObject.get(resultIds.instanceId) : null;
        Map<Integer, LwM2mResource> resourcesOld = (instanceOld != null) ? instanceOld.getResources() : null;
        LwM2mResource resourceOld = (resourcesOld != null && resourcesOld.get(resultIds.getResourceId()) != null) ? resourcesOld.get(resultIds.getResourceId()) : null;
        // #2
        LwM2mResource resourceNew;
        JsonObject resNameValue;
        String resourceName = modelObject.getObjectModel().resources.get(resultIds.getResourceId()).name;
        if (resourceOld.isMultiInstances()) {
            resourceNew = LwM2mMultipleResource.newResource(resultIds.getResourceId(), values, resourceOld.getType());
            resNameValue = createResNameValue(resourceName, values.toString());
        } else {
            resourceNew = LwM2mSingleResource.newResource(resultIds.getResourceId(), value, resourceOld.getType());
            resNameValue = createResNameValue(resourceName, value.toString());
        }
        if (resNameValue != null) {
            if (modelClient.getAttrTelemetryObserveValue().getPostAttribute().getAsJsonObject().has(path))
                modelClient.getAttrTelemetryObserveValue().getPostAttribute().getAsJsonObject().add(path, resNameValue);
            if (modelClient.getAttrTelemetryObserveValue().getPostTelemetry().getAsJsonObject().has(path))
                modelClient.getAttrTelemetryObserveValue().getPostTelemetry().getAsJsonObject().add(path, resNameValue);
            //#3
            Map<Integer, LwM2mResource> resourcesNew = new HashMap<>(resourcesOld);
            // #4
            resourcesNew.remove(resourceOld);
            // #5
            resourcesNew.put(resultIds.getResourceId(), resourceNew);
            // #6
            LwM2mObjectInstance instanceNew = new LwM2mObjectInstance(resultIds.instanceId, resourcesNew.values());
            // #7
            CountDownLatch respLatch = new CountDownLatch(1);
            modelClient.getModelObjects().get(resultIds.getObjectId()).removeInstance(resultIds.instanceId);
            instancesModelObject.put(resultIds.instanceId, instanceNew);
            respLatch.countDown();
            try {
                respLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
            }
            this.setAttrTelemetryToThingsboard(registrationId);
        }
    }

    /**
     * Start observe
     * Analyze the response in ->
     * 1. First: lwM2MTransportRequest.sendResponse, ObservationListener.newObservation
     * 2. Next: ObservationListener.onResponse     *
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     */
    private void onSentObserveToClient(LeshanServer lwServer, Registration registration) {
        if (lwServer.getObservationService().getObservations(registration).size() > 0) {
            this.setCancelAllObservation(lwServer, registration);
        }
        lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registration.getId()).getAttrTelemetryObserveValue().getPostObserve().forEach(target -> {
            lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, GET_TYPE_OPER_OBSERVE,
                    null, null, null, "", context.getTimeout());
        });
    }

    /**
     * Get info about changeCredentials from Thingsboard
     * #1 Equivalence test: old <> new Value (Only Attribute, Telemetry, Observe)
     * #1.1 pathResAttrTelemetryProfile
     * #1.2 Attribute
     * #1.3 Telemetry
     * #1.4 Observe
     * #2 If #1 == change, then analyze and update Value in Transport
     * #2.1 ResAttrTelemetryProfile:
     * - add
     * -- if is value in modelClient: Get Read query to Client and add to ResAttrTelemetry
     * -- if is not value in modelClient: nothing
     * - del: - nothing
     * #2.2 Attribute:
     * - add:
     * -- if is value in modelClient: add modelClient.getAttrTelemetryObserveValue().getPostAttribute and Get Thingsboard (Attr)
     * -- if is not value in modelClient: nothing
     * - del:
     * -- del from modelClient.getAttrTelemetryObserveValue().getPostAttribute and update modelClient.getAttrTelemetryObserveValue().getPostAttributeProfile
     * #2.3 Telemetry
     * - add:
     * -- if is value in modelClient: add modelClient.getAttrTelemetryObserveValue().getPostTelemetry and Get Thingsboard (Telemetry)
     * -- if is not value in modelClient: nothing
     * - del
     * -- del from modelClient.getAttrTelemetryObserveValue().getPostTelemetry and update modelClient.getAttrTelemetryObserveValue().getPostTelemetryProfile
     * #2.4 Observe: if dell All Attr/Telemetry or change observe =>
     * - if dell All Attr/Telemetry and is observe => cancel observe
     * - change observe
     * -- add:
     * --- if path in All Attr/Telemetry: (without query to client) => observe
     * -- del: cancel observe
     * #3 Update in modelClient.getAttrTelemetryObserveValue(): ...Profile
     *
     * @param updateCredentials - Credentials include info about Attr/Telemetry/Observe (Profile)
     */
    @SneakyThrows
    public void onGetChangeCredentials(ToTransportUpdateCredentialsProto updateCredentials) {
        String credentialsId = (updateCredentials.getCredentialsIdCount() > 0) ? updateCredentials.getCredentialsId(0) : null;
        JsonObject credentialsValue = (updateCredentials.getCredentialsValueCount() > 0) ? adaptor.validateJson(updateCredentials.getCredentialsValue(0)) : null;
        if (credentialsValue != null && !credentialsValue.isJsonNull() && credentialsId != null && !credentialsId.isEmpty()) {
            String registrationId = lwM2mInMemorySecurityStore.getByRegistrationId(credentialsId);
            Registration registration = lwM2mInMemorySecurityStore.getByRegistration(registrationId);
            ModelClient modelClient = lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registrationId);
            LeshanServer lwServer = modelClient.getLwServer();
            log.info("updateCredentials -> registration: {}", registration);

            // #1
            ResultsAnalyzerParameters pathResAttrTelemetryAnalyzer;
            ResultsAnalyzerParameters postAttributeAnalyzer;
            ResultsAnalyzerParameters postTelemetryAnalyzer;
            ResultsAnalyzerParameters postObserveAnalyzer = null;

            JsonObject observeAttrNewJson = (credentialsValue.has("observeAttr") && !credentialsValue.get("observeAttr").isJsonNull()) ? credentialsValue.get("observeAttr").getAsJsonObject() : null;
            log.info("updateCredentials -> observeAttr: {}", observeAttrNewJson);
            JsonArray attributeNew = (observeAttrNewJson.has("attribute") && !observeAttrNewJson.get("attribute").isJsonNull()) ? observeAttrNewJson.get("attribute").getAsJsonArray() : null;
            JsonArray telemetryNew = (observeAttrNewJson.has("telemetry") && !observeAttrNewJson.get("telemetry").isJsonNull()) ? observeAttrNewJson.get("telemetry").getAsJsonArray() : null;
            JsonArray observeNew = (observeAttrNewJson.has("observe") && !observeAttrNewJson.get("observe").isJsonNull()) ? observeAttrNewJson.get("observe").getAsJsonArray() : null;
            if (!modelClient.getAttrTelemetryObserveValue().getPostAttributeProfile().equals(attributeNew) ||
                    !modelClient.getAttrTelemetryObserveValue().getPostTelemetryProfile().equals(telemetryNew) ||
                    !modelClient.getAttrTelemetryObserveValue().getPostObserveProfile().equals(observeNew)) {
                if (!modelClient.getAttrTelemetryObserveValue().getPostAttributeProfile().equals(attributeNew) ||
                        !modelClient.getAttrTelemetryObserveValue().getPostTelemetryProfile().equals(telemetryNew)) {
                    // #1.1 add
                    Set<String> pathResAttrTelemetryProfileNew = ConcurrentHashMap.newKeySet();
                    attributeNew.forEach(attr -> {
                        pathResAttrTelemetryProfileNew.add(attr.getAsString().toString());
                    });
                    telemetryNew.forEach(telemetry -> {
                        pathResAttrTelemetryProfileNew.add(telemetry.getAsString().toString());
                    });
                    Set<String> pathResAttrTelemetryProfileOld = ConcurrentHashMap.newKeySet();
                    modelClient.getAttrTelemetryObserveValue().getPostAttributeProfile().forEach(attr -> {
                        pathResAttrTelemetryProfileOld.add(attr.getAsString().toString());
                    });
                    modelClient.getAttrTelemetryObserveValue().getPostTelemetryProfile().forEach(telemetry -> {
                        pathResAttrTelemetryProfileOld.add(telemetry.getAsString().toString());
                    });
                    pathResAttrTelemetryAnalyzer = getAnalyzerParameters(pathResAttrTelemetryProfileOld, pathResAttrTelemetryProfileNew);
                    // #1.2 add
                    postAttributeAnalyzer = getAnalyzerParameters(new Gson().fromJson(modelClient.getAttrTelemetryObserveValue().getPostAttributeProfile(), Set.class),
                            new Gson().fromJson(attributeNew, Set.class));
                    // #1.3 add
                    postTelemetryAnalyzer = getAnalyzerParameters(new Gson().fromJson(modelClient.getAttrTelemetryObserveValue().getPostTelemetryProfile(), Set.class),
                            new Gson().fromJson(telemetryNew, Set.class));
                    // #2.1 add
                    if (pathResAttrTelemetryAnalyzer != null && pathResAttrTelemetryAnalyzer.getPathPostParametersAdd().size() > 0) {
                        this.updateResourceValueObserve(lwServer, registration, modelClient, pathResAttrTelemetryAnalyzer.getPathPostParametersAdd(), GET_TYPE_OPER_READ);
                    }

                    // #2.2 add
                    if (postAttributeAnalyzer != null && postAttributeAnalyzer.getPathPostParametersAdd().size() > 0) {
                        this.sentNewAttrTelemetryToThingsboard(registration.getId(), modelClient, postAttributeAnalyzer.getPathPostParametersAdd(), DEVICE_ATTRIBUTES_TOPIC);

                    }
                    // #2.3 add
                    if (postTelemetryAnalyzer != null && postTelemetryAnalyzer.getPathPostParametersAdd().size() > 0) {
                        this.sentNewAttrTelemetryToThingsboard(registration.getId(), modelClient, postTelemetryAnalyzer.getPathPostParametersAdd(), DEVICE_TELEMETRY_TOPIC);
                    }
                    //todo if is new parameters in observeProfileOld ?
                }

                if (!modelClient.getAttrTelemetryObserveValue().getPostObserveProfile().equals(observeNew)) {
                    // #1.4 add
                    postObserveAnalyzer = getAnalyzerParameters(new Gson().fromJson(modelClient.getAttrTelemetryObserveValue().getPostObserveProfile(), Set.class),
                            new Gson().fromJson(observeNew, Set.class));
                    // #2.4 add
                    if (postObserveAnalyzer != null && postObserveAnalyzer.getPathPostParametersAdd().size() > 0) {
                        this.updateResourceValueObserve(lwServer, registration, modelClient, postObserveAnalyzer.getPathPostParametersAdd(), GET_TYPE_OPER_OBSERVE);
                    }
                }
                // #3
                modelClient.getAttrTelemetryObserveValue().setPostAttributeProfile(attributeNew);
                modelClient.getAttrTelemetryObserveValue().setPostTelemetryProfile(telemetryNew);
                modelClient.getAttrTelemetryObserveValue().setPostObserveProfile(observeNew);
            }

        }
    }

    /**
     * Compare old list with new list  after change AttrTelemetryObserve in config Profile
     *
     * @param parametersOld -
     * @param parametersNew -
     * @return ResultsAnalyzerParameters: add && new
     */
    private ResultsAnalyzerParameters getAnalyzerParameters(Set<String> parametersOld, Set<String> parametersNew) {
        ResultsAnalyzerParameters analyzerParameters = null;
        if (!parametersOld.equals(parametersNew)) {
            analyzerParameters = new ResultsAnalyzerParameters();
            analyzerParameters.setPathPostParametersAdd(parametersNew
                    .stream().filter(p -> !parametersOld.contains(p)).collect(Collectors.toSet()));
            analyzerParameters.setPathPostParametersDel(parametersOld
                    .stream().filter(p -> !parametersNew.contains(p)).collect(Collectors.toSet()));
        }
        return analyzerParameters;
    }

    /**
     * Update Resource value after change RezAttrTelemetry in config Profile
     * sent response Read to Client and add path to pathResAttrTelemetry in ModelClient.getAttrTelemetryObserveValue()
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     * @param modelClient  - object with All parameters off client
     * @param targets      - path Resources
     */

    private void updateResourceValueObserve(LeshanServer lwServer, Registration registration, ModelClient modelClient, Set<String> targets, String typeOper) {
        targets.stream().forEach(target -> {
            ResultIds pathIds = new ResultIds(target);
            if (pathIds.resourceId >= 0 && modelClient.getModelObjects().get(pathIds.getObjectId())
                    .getInstances().get(pathIds.getInstanceId()).getResource(pathIds.getResourceId()).getValue() != null) {
                if (GET_TYPE_OPER_READ.equals(typeOper)) {
                    lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, typeOper,
                            ContentFormat.TLV.getName(), null, null, "", context.getTimeout());
                    modelClient.getAttrTelemetryObserveValue().getPathResAttrTelemetry().add(target);
                }
                else if (GET_TYPE_OPER_OBSERVE.equals(typeOper)) {
                    lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, typeOper,
                            null, null, null, "", context.getTimeout());
                    modelClient.getAttrTelemetryObserveValue().getPostObserve().add(target);
                }
            }
        });

    }


    /**
     * Save path/name/value to PostAttribute/PostTelemetry in pathResAttrTelemetry in ModelClient.getAttrTelemetryObserveValue()
     * Sent to thingsboard msg format: JsonObject: [{name: value}] Attribute/Telemetry
     *
     * @param registrationId - RegistrationId LwM2M Client
     * @param modelClient    - object with All parameters off client
     * @param targets        - paths Resource
     * @param typeTopic      - DEVICE_ATTRIBUTES_TOPIC, DEVICE_TELEMETRY_TOPIC
     */
    private void sentNewAttrTelemetryToThingsboard(String registrationId, ModelClient modelClient, Set<String> targets, String typeTopic) {
        JsonObject parametersWithValue = new JsonObject();
        targets.stream().forEach(target -> {
            ResultIds pathIds = new ResultIds(target);
            if (pathIds.resourceId >= 0 && modelClient.getModelObjects().get(pathIds.getObjectId())
                    .getInstances().get(pathIds.getInstanceId()).getResource(pathIds.getResourceId()).getValue().toString() != null) {
                JsonObject postParameter = new JsonObject();
                String name = modelClient.getModelObjects().get(pathIds.getObjectId()).getObjectModel().resources.get(pathIds.resourceId).name;
                String value = modelClient.getModelObjects().get(pathIds.getObjectId())
                        .getInstances().get(pathIds.getInstanceId()).getResource(pathIds.getResourceId()).getValue().toString();
                postParameter.addProperty(name, value);
                parametersWithValue.addProperty(name, value);
                if (DEVICE_ATTRIBUTES_TOPIC.equals(typeTopic)) {
                    modelClient.getAttrTelemetryObserveValue().getPostAttribute().getAsJsonObject().add(target, postParameter);
                } else if (DEVICE_TELEMETRY_TOPIC.equals(typeTopic)) {
                    modelClient.getAttrTelemetryObserveValue().getPostTelemetry().getAsJsonObject().add(target, postParameter);
                }
            }
        });
        if (!parametersWithValue.getAsJsonObject().isJsonNull()) {
            this.setToDeviceAttributeTelemetry(parametersWithValue, typeTopic, -1, registrationId);
        }
    }

    /**
     * Trigger Server path = "/1/0/8"
     * Trigger bootStrap path = "/1/0/9" - have to implemented on client
     */
    public void doTrigger(LeshanServer lwServer, Registration registration, String path) {
        lwM2MTransportRequest.sendAllRequest(lwServer, registration, path, POST_TYPE_OPER_EXECUTE,
                ContentFormat.TLV.getName(), null, null, "", context.getTimeout());
    }


//
//    private void cancelAllObservation(LeshanServer lwServer, Registration registration) {
//        /**
//         * cancel observation : active way
//         * forech HashMap observe response
//         *
//         */
//
//        Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
//        observations.forEach(observation -> {
//            CancelObservationRequest request = new CancelObservationRequest((observation));
//        });

//
//                });
//        Observation observation = observeResponse.getObservation();
//        CancelObservationResponse cancelResponse = lwServer.send(registration, new CancelObservationRequest(observation));
//        log.info("cancelResponse: {}", cancelResponse);
//        /**
//         * active cancellation does not remove observation from store : it should be done manually using
//         */
//        lwServer.getObservationService().cancelObservation(observation);
//    }
//
//    private void getStartObsrerveObjects(LeshanServer lwServer, Registration registration) {
//        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
//            String[] objects = url.getUrl().split("/");
//            if (objects.length > 2) {
//                int objectId = Integer.parseInt(objects[1]);
//                new ObserveRequest(objectId, 0);
////                lwM2MTransportRequest.sendRequest(lwServer, registration, new ObserveRequest(objectId));
//                log.info("9) getStartObsrerveObjects: do \n objectId: {}", objectId);
////                if (objectId != 1 && objectId !=6) {
////                    DiscoverRequest request = new DiscoverRequest(objectId);
//////                    ObserveRequest request = new ObserveRequest(objectId);
////                    LwM2mResponse oResponse = lwM2MTransportRequest.sendRequest(lwServer, registration, request);
//                lwM2MTransportRequest.doGet(lwServer, registration, "/" + objectId, LwM2MTransportHandler.GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
//
//                log.info("10) getStartObsrerveObjects: \n objectId: {} \n oRequest : {}", objectId);
////                }
//
//            }
//        });
//    }


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

//    private ObserveResponse setObservInstance(LeshanServer lwServer, Registration registration, Integer objectId, Integer instanceId, Integer resourceId) {
//        log.info("Endpoint: [{}] Object: [{}] Instnce: [{}] Resoutce: [{}] Received endpoint ObserveResponse", registration.getEndpoint(), objectId, instanceId, resourceId);
//
//        ObserveResponse observeResponse = null;
//        try {
//            // observe device timezone
//            observeResponse = lwServer.send(registration, new ObserveRequest(objectId, instanceId));
//            Observation observation = observeResponse.getObservation();
//            Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
////            log.info("Endpoint: [{}] Path: [{}] Value: [{}] Received endpoint ObserveResponse", registration.getEndpoint(), path, value);
////            log.info("[{}] [{}] Received endpoint observation", registration.getEndpoint(), observation);
//            log.info("[{}] [{}] Received endpoint observations", registration.getEndpoint(), observations);
//
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return observeResponse;
//    }

    public void setCancelAllObservation(LeshanServer lwServer, Registration registration) {
        log.info("33)  setCancelObservationObjects endpoint: {}", registration.getEndpoint());
        int cancel = lwServer.getObservationService().cancelObservations(registration);
        Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
        if (registration != null) {
            observations.forEach(observation -> {
                log.info("33_1)  setCancelObservationObjects endpoint: {} cancel: {}", registration.getEndpoint(), cancel);
                lwM2MTransportRequest.sendAllRequest(lwServer, registration, observation.getPath().toString(), POST_TYPE_OPER_OBSERVE_CANCEL, null, null, null, null, context.getTimeout());
                log.info("33_2)  setCancelObservationObjects endpoint: {}", registration.getEndpoint());
                lwServer.getObservationService().cancelObservation(observation);
                log.info("33_3)  setCancelObservationObjects endpoint: {}", registration.getEndpoint());
                /**
                 * active cancellation does not remove observation from store : it should be done manually using
                 */
                lwServer.getObservationService().cancelObservations(registration, observation.getPath().toString());
                log.info("33_4)  setCancelObservationObjects endpoint: {}/\n target {}", registration.getEndpoint(), observation.getPath().toString());
            });
        }
        if (registration != null) {
            CountDownLatch cancelLatch = new CountDownLatch(1);
            Arrays.stream(registration.getObjectLinks()).forEach(url -> {
                String[] objects = url.getUrl().split("/");
                if (objects.length > 2) {
                    String target = "/" + objects[1];

//                    CancelObservationResponse cancelResponse = lwServer.send(registration, new CancelObservationRequest(observation));
//                    log.info("cancelResponse: {}", cancelResponse);
                    /**
                     * active cancellation does not remove observation from store : it should be done manually using
                     */
                    lwServer.getObservationService().cancelObservations(registration, target);
                    log.info("33_1)  setCancelObservationObjects endpoint: {}/\n target {}", registration.getEndpoint(), target);
                }
            });
            cancelLatch.countDown();
            try {
                cancelLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {

            }
        }
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


//
//    public Registration getRegistration(LeshanServer lwServer, String clientEndpoint) {
//        return lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
//    }


//    public AttrTelemetryObserveValue doGetAttributsTelemetryObserve1(LeshanServer lwServer, String clientEndpoint) {
//        AttrTelemetryObserveValue readResultAttrTel = new AttrTelemetryObserveValue();
//        Registration registration = lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
//        registration.getAdditionalRegistrationAttributes().entrySet().forEach(entry -> {
//            log.info("Attributes: Key : [{}] Value : [{}]", entry.getKey(), entry.getValue());
//            readResultAttrTel.getPostAttribute().addProperty(entry.getKey(), entry.getValue());
//        });
//        lwServer.getModelProvider().getObjectModel(registration).getObjectModels().forEach(om -> {
//            String idObj = String.valueOf(om.id);
//            LwM2mResponse cResponse = this.getObserve(lwServer, registration, new ObserveRequest(om.id));
////            LwM2mResponse cResponse = lwM2MTransportRequest.doGet(lwServer, clientEndpoint, "/" + idObj, GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
//            log.info("GET cResponse: {} \n target: {}", cResponse, idObj);
//            if (cResponse != null) {
//                LwM2mNode content = ((ReadResponse) cResponse).getContent();
//                ((LwM2mObject) content).getInstances().entrySet().stream().forEach(instance -> {
//                    String instanceId = String.valueOf(instance.getValue().getId());
//                    om.resources.entrySet().stream().forEach(resOm -> {
//                        String attrTelName = om.name + "_" + instanceId + "_" + resOm.getValue().name;
//                        /** Attributs: om.id: Security, Server, ACL & 'R' ? */
//                        if (om.id <= 2) {
////                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, "");
//                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, "");
//                        } else {
//                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, "");
////                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, "");
//                        }
//                    });
//
//                    instance.getValue().getResources().entrySet().stream().forEach(resource -> {
//                        int resourceId = resource.getValue().getId();
//                        String resourceValue = getResourceValueToString(resource.getValue());
//                        String attrTelName = om.name + "_" + instanceId + "_" + om.resources.get(resourceId).name;
//                        log.info("resource.getValue() [{}] : [{}] -> [{}]", attrTelName, resourceValue, om.resources.get(resourceId).operations.name());
//                        if (readResultAttrTel.getPostAttribute().has(attrTelName)) {
//                            readResultAttrTel.getPostAttribute().remove(attrTelName);
//                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, resourceValue);
//                        } else if (readResultAttrTel.getPostTelemetry().has(attrTelName)) {
//                            readResultAttrTel.getPostTelemetry().remove(attrTelName);
//                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, resourceValue);
//                        }
//                    });
//
//                });
//            }
//        });
//
//
//        return readResultAttrTel;
//    }


//    public AttrTelemetryObserveValue doGetAttributsTelemetry(LeshanServer lwServer, String clientEndpoint) {
//        AttrTelemetryObserveValue readResultAttrTel = new AttrTelemetryObserveValue();
//        Registration registration = lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
//        registration.getAdditionalRegistrationAttributes().entrySet().forEach(entry -> {
//            log.info("Attributes: Key : [{}] Value : [{}]", entry.getKey(), entry.getValue());
//            readResultAttrTel.getPostAttribute().addProperty(entry.getKey(), entry.getValue());
//        });
//        lwServer.getModelProvider().getObjectModel(registration).getObjectModels().forEach(om -> {
//            String idObj = String.valueOf(om.id);
//            lwM2MTransportRequest.doGet(lwServer, registration, "/" + idObj, LwM2MTransportHandler.GET_TYPE_OPER_READ, ContentFormat.TLV.getName());
//            log.info("GET cResponse: {} \n target: {}", cResponse, idObj);
//            if (cResponse != null) {
//                LwM2mNode content = ((ReadResponse) cResponse).getContent();
//                ((LwM2mObject) content).getInstances().entrySet().stream().forEach(instance -> {
//                    String instanceId = String.valueOf(instance.getValue().getId());
//                    om.resources.entrySet().stream().forEach(resOm -> {
//                        String attrTelName = om.name + "_" + instanceId + "_" + resOm.getValue().name;
//                        /** Attributs: om.id: Security, Server, ACL & 'R' ? */
//                        if (om.id <= 2) {
////                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, "");
//                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, "");
//                        } else {
//                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, "");
////                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, "");
//                        }
//                    });
//
//                    instance.getValue().getResources().entrySet().stream().forEach(resource -> {
//                        int resourceId = resource.getValue().getId();
//                        String resourceValue = getResourceValueToString(resource.getValue());
//                        String attrTelName = om.name + "_" + instanceId + "_" + om.resources.get(resourceId).name;
//                        log.info("resource.getValue() [{}] : [{}] -> [{}]", attrTelName, resourceValue, om.resources.get(resourceId).operations.name());
//                        if (readResultAttrTel.getPostAttribute().has(attrTelName)) {
//                            readResultAttrTel.getPostAttribute().remove(attrTelName);
//                            readResultAttrTel.getPostAttribute().addProperty(attrTelName, resourceValue);
//                        } else if (readResultAttrTel.getPostTelemetry().has(attrTelName)) {
//                            readResultAttrTel.getPostTelemetry().remove(attrTelName);
//                            readResultAttrTel.getPostTelemetry().addProperty(attrTelName, resourceValue);
//                        }
//                    });
//
//                });
//            }
//        });
//        return readResultAttrTel;
//    }

//
//
//    private String getResourceValueToString(LwM2mResource resource) {
//        Object resValue;
//        try {
//            resValue = resource.getValues();
//        } catch (NoSuchElementException e) {
//            resValue = resource.getValue();
//        }
//        return String.valueOf(resValue);
//    }

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

//    public LwM2mResponse getObserve(LeshanServer lwServer, Registration registration, ObserveRequest observeRequest) {
//        final LwM2mResponse[] responseRez = new LwM2mResponse[1];
//        CountDownLatch respLatch = new CountDownLatch(1);
//        if (registration != null) {
//            lwServer.send(registration, observeRequest, (ResponseCallback) response -> {
//                responseRez[0] = response;
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
}
