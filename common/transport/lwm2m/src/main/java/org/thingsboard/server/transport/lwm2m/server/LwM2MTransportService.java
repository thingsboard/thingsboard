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
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.transport.lwm2m.server.client.*;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MJsonAdaptor;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.*;

@Slf4j
@Service("LwM2MTransportService")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportService {

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


    @PostConstruct
    public void init() {
        context.getScheduler().scheduleAtFixedRate(() -> checkInactivityAndReportActivity(), new Random().nextInt((int) context.getCtxServer().getSessionReportTimeout()), context.getCtxServer().getSessionReportTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * Start registration device
     * Create session: Map<String <registrationId >, LwM2MClient>
     * 1. replaceNewRegistration -> (solving the problem of incorrect termination of the previous session with this endpoint)
     * 1.1 When we initialize the registration, we register the session by endpoint.
     * 1.2 If the server has incomplete requests (canceling the registration of the previous session),
     * delete the previous session only by the previous registration.getId
     * 1.2 Add Model (Entity) for client (from registration & observe) by registration.getId
     * 1.2 Remove from sessions Model by enpPoint
     * Next ->  Create new LwM2MClient for current session -> setModelClient...
     *
     * @param lwServer             - LeshanServer
     * @param registration         - Registration LwM2M Client
     * @param previousObsersations - may be null
     */
    public void onRegistered(LeshanServer lwServer, Registration registration, Collection<Observation> previousObsersations) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getlwM2MClient(lwServer, registration, this);
        if (lwM2MClient != null) {
            lwM2MClient.setLwM2MTransportService(this);
            lwM2MClient.setLwM2MTransportService(this);
            lwM2MClient.setSessionUuid(UUID.randomUUID());
            this.setLwM2MClient(lwServer, registration, lwM2MClient);
            SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration.getId());
            if (sessionInfo != null) {
                lwM2MClient.setDeviceUuid(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
                lwM2MClient.setProfileUuid(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
                lwM2MClient.setDeviceName(sessionInfo.getDeviceName());
                lwM2MClient.setDeviceProfileName(sessionInfo.getDeviceType());

                transportService.registerAsyncSession(sessionInfo, new LwM2MSessionMsgListener(this));
                transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(SessionEvent.OPEN), null);
            } else {
                log.error("Client: [{}] onRegistered [{}] name  [{}] sessionInfo ", registration.getId(), registration.getEndpoint(), sessionInfo);
            }
        } else {
            log.error("Client: [{}] onRegistered [{}] name  [{}] lwM2MClient ", registration.getId(), registration.getEndpoint(), lwM2MClient);
        }
    }

    /**
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     */
    public void updatedReg(LeshanServer lwServer, Registration registration) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration.getId());
        if (sessionInfo != null) {
            log.info("Client: [{}] updatedReg [{}] name  [{}] profile ", registration.getId(), registration.getEndpoint(), sessionInfo.getDeviceType());
        } else {
            log.error("Client: [{}] updatedReg [{}] name  [{}] sessionInfo ", registration.getId(), registration.getEndpoint(), sessionInfo);
        }
    }

    /**
     * @param registration - Registration LwM2M Client
     * @param observations - All paths observations before unReg
     *                     !!! Warn: if have not finishing unReg, then this operation will be finished on next Client`s connect
     */
    public void unReg(Registration registration, Collection<Observation> observations) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration.getId());
        if (sessionInfo != null) {
            transportService.deregisterSession(sessionInfo);
            this.doCloseSession(sessionInfo);
            lwM2mInMemorySecurityStore.delRemoveSessionAndListener(registration.getId());
            if (lwM2mInMemorySecurityStore.getProfiles().size() > 0) {
                this.syncSessionsAndProfiles();
            }
            log.info("Client: [{}] unReg [{}] name  [{}] profile ", registration.getId(), registration.getEndpoint(), sessionInfo.getDeviceType());
        } else {
            log.error("Client: [{}] unReg [{}] name  [{}] sessionInfo ", registration.getId(), registration.getEndpoint(), sessionInfo);
        }
    }

    public void onSleepingDev(Registration registration) {
        log.info("[{}] [{}] Received endpoint Sleeping version event", registration.getId(), registration.getEndpoint());
        //TODO: associate endpointId with device information.
    }

    /**
     * Those methods are called by the protocol stage thread pool, this means that execution MUST be done in a short delay,
     * * if you need to do long time processing use a dedicated thread pool.
     *
     * @param registration
     */

    public void onAwakeDev(Registration registration) {
         log.info("[{}] [{}] Received endpoint Awake version event", registration.getId(), registration.getEndpoint());
        //TODO: associate endpointId with device information.
    }

    /**
     * This method is used to sync with sessions
     * Removes a profile if not used in sessions
     */
    private void syncSessionsAndProfiles() {
        Map<UUID, AttrTelemetryObserveValue> profilesClone = lwM2mInMemorySecurityStore.getProfiles().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue));
        profilesClone.forEach((k, v) -> {
            String registrationId = lwM2mInMemorySecurityStore.getSessions().entrySet()
                    .stream()
                    .filter(e -> e.getValue().getProfileUuid().equals(k))
                    .findFirst()
                    .map(Map.Entry::getKey) // return the key of the matching entry if found
                    .orElse("");
            if (registrationId.isEmpty()) {
                lwM2mInMemorySecurityStore.getProfiles().remove(k);
            }
        });
    }

    /**
     * Create new LwM2MClient for current session -> setModelClient...
     * #1   Add all ObjectLinks (instance) to control the process of executing requests to the client
     * to get the client model with current values
     * #2   Get the client model with current values. Analyze the response in -> lwM2MTransportRequest.sendResponse
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     * @param lwM2MClient  - object with All parameters off client
     */
    private void setLwM2MClient(LeshanServer lwServer, Registration registration, LwM2MClient lwM2MClient) {
        // #1
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            ResultIds pathIds = new ResultIds(url.getUrl());
            if (pathIds.instanceId > -1 && pathIds.resourceId == -1) {
                lwM2MClient.getPendingRequests().add(url.getUrl());
            }
        });
        // #2
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            ResultIds pathIds = new ResultIds(url.getUrl());
            if (pathIds.instanceId > -1 && pathIds.resourceId == -1) {
                lwM2MTransportRequest.sendAllRequest(lwServer, registration, url.getUrl(), GET_TYPE_OPER_READ,
                        ContentFormat.TLV.getName(), lwM2MClient, null, "", this.context.getCtxServer().getTimeout());
            }
        });
    }

    /**
     * @param registrationId - Id of Registration LwM2M Client
     * @return - sessionInfo after access connect client
     */
    private SessionInfoProto getValidateSessionInfo(String registrationId) {
        SessionInfoProto sessionInfo = null;
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registrationId);
        if (lwM2MClient != null) {
            ValidateDeviceCredentialsResponseMsg msg = lwM2MClient.getCredentialsResponse();
            if (msg == null || msg.getDeviceInfo() == null) {
                log.error("[{}] [{}]", lwM2MClient.getEndPoint(), CONNECTION_REFUSED_NOT_AUTHORIZED.toString());
            } else {
                sessionInfo = SessionInfoProto.newBuilder()
                        .setNodeId(this.context.getNodeId())
                        .setSessionIdMSB(lwM2MClient.getSessionUuid().getMostSignificantBits())
                        .setSessionIdLSB(lwM2MClient.getSessionUuid().getLeastSignificantBits())
                        .setDeviceIdMSB(msg.getDeviceInfo().getDeviceIdMSB())
                        .setDeviceIdLSB(msg.getDeviceInfo().getDeviceIdLSB())
                        .setTenantIdMSB(msg.getDeviceInfo().getTenantIdMSB())
                        .setTenantIdLSB(msg.getDeviceInfo().getTenantIdLSB())
                        .setDeviceName(msg.getDeviceInfo().getDeviceName())
                        .setDeviceType(msg.getDeviceInfo().getDeviceType())
                        .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileIdLSB())
                        .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileIdMSB())
                        .build();
            }
        }
        return sessionInfo;
    }

    /**
     * Add attribute/telemetry information from Client and credentials/Profile to client model and start observe
     * !!! if the resource has an observation, but no telemetry or attribute - the observation will not use
     * #1 Client`s starting info  to  send to thingsboard
     * #2 Sending Attribute Telemetry with value to thingsboard only once at the start of the connection
     * #3 Start observe
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     */
    @SneakyThrows
    public void updatesAndSentModelParameter(LeshanServer lwServer, Registration registration) {
        // #1
//        this.setParametersToModelClient(registration, modelClient, deviceProfile);
        // #2
        this.updateAttrTelemetry(registration, true, null);
        // #3
        this.onSentObserveToClient(lwServer, registration);
    }


    /**
     * Sent Attribute and Telemetry to Thingsboard
     * #1 - get AttrName/TelemetryName with value:
     * #1.1 from Client
     * #1.2 from LwM2MClient:
     * -- resourceId == path from AttrTelemetryObserveValue.postAttributeProfile/postTelemetryProfile/postObserveProfile
     * -- AttrName/TelemetryName == resourceName from ModelObject.objectModel, value from ModelObject.instance.resource(resourceId)
     * #2 - set Attribute/Telemetry
     *
     * @param registration - Registration LwM2M Client
     */
    private void updateAttrTelemetry(Registration registration, boolean start, Set<String> paths) {
        JsonObject attributes = new JsonObject();
        JsonObject telemetrys = new JsonObject();
        if (start) {
            // #1.1
            JsonObject attributeClient = this.getAttributeClient(registration);
            if (attributeClient != null) {
                attributeClient.entrySet().forEach(p -> {
                    attributes.add(p.getKey(), p.getValue());
                });
            }
        }
        // #1.2
        CountDownLatch cancelLatch = new CountDownLatch(1);
        this.getParametersFromProfile(attributes, telemetrys, registration, paths);
        cancelLatch.countDown();
        try {
            cancelLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        if (attributes.getAsJsonObject().entrySet().size() > 0)
            this.updateParametersOnThingsboard(attributes, DEVICE_ATTRIBUTES_TOPIC, registration.getId());
        if (telemetrys.getAsJsonObject().entrySet().size() > 0)
            this.updateParametersOnThingsboard(telemetrys, DEVICE_TELEMETRY_TOPIC, registration.getId());
    }

    /**
     * get AttrName/TelemetryName with value from Client
     * @param registration -
     * @return - JsonObject, format: {name: value}}
     */
    private JsonObject getAttributeClient(Registration registration) {
        if (registration.getAdditionalRegistrationAttributes().size() > 0) {
            JsonObject resNameValues = new JsonObject();
            registration.getAdditionalRegistrationAttributes().entrySet().forEach(entry -> {
                resNameValues.addProperty(entry.getKey(), entry.getValue());
            });
            return resNameValues;
        }
        return null;
    }

    /**
     * @param attributes   - new JsonObject
     * @param telemetry    - new JsonObject
     * @param registration - Registration LwM2M Client
     *                     result: add to JsonObject those resources to which the user is subscribed and they have a value
     *                     if path==null add All resources else only one
     *                     (attributes/telemetry): new {name(Attr/Telemetry):value}
     */
    private void getParametersFromProfile(JsonObject attributes, JsonObject telemetry, Registration registration, Set<String> path) {
        AttrTelemetryObserveValue attrTelemetryObserveValue = lwM2mInMemorySecurityStore.getProfiles().get(lwM2mInMemorySecurityStore.getSessions().get(registration.getId()).getProfileUuid());
        attrTelemetryObserveValue.getPostAttributeProfile().forEach(p -> {
            ResultIds pathIds = new ResultIds(p.getAsString().toString());
            if (pathIds.getResourceId() > -1) {
                if (path == null || path.contains(p.getAsString())) {
                    this.addParameters(pathIds, p.getAsString().toString(), attributes, registration);
                }
            }
        });
        attrTelemetryObserveValue.getPostTelemetryProfile().forEach(p -> {
            ResultIds pathIds = new ResultIds(p.getAsString().toString());
            if (pathIds.getResourceId() > -1) {
                if (path == null || path.contains(p.getAsString())) {
                    this.addParameters(pathIds, p.getAsString().toString(), telemetry, registration);
                }
            }
        });
    }

    /**
     * @param pathIds      - path resource
     * @param parameters   - JsonObject attributes/telemetry
     * @param registration - Registration LwM2M Client
     */
    private void addParameters(ResultIds pathIds, String path, JsonObject parameters, Registration registration) {
        ModelObject modelObject = lwM2mInMemorySecurityStore.getSessions().get(registration.getId()).getModelObjects().get(pathIds.getObjectId());
        JsonObject names = lwM2mInMemorySecurityStore.getProfiles().get(lwM2mInMemorySecurityStore.getSessions().get(registration.getId()).getProfileUuid()).getPostKeyNameProfile();
        String resName = String.valueOf(names.get(path));
        if (modelObject != null && resName != null && !resName.isEmpty()) {
            String resValue = this.getResourceValue(modelObject, pathIds);
            if (resValue != null) {
                parameters.addProperty(resName, resValue);
            }
        }
    }

    /**
     * @param modelObject - ModelObject of Client
     * @param pathIds     - path resource
     * @return - value of Resource or null
     */
    private String getResourceValue(ModelObject modelObject, ResultIds pathIds) {
        String resValue = null;
        if (modelObject.getInstances().get(pathIds.getInstanceId()) != null) {
            LwM2mObjectInstance instance = modelObject.getInstances().get(pathIds.getInstanceId());
            if (instance.getResource(pathIds.getResourceId()) != null) {
                resValue = instance.getResource(pathIds.getResourceId()).getType() == OPAQUE ?
                        Hex.encodeHexString((byte[]) instance.getResource(pathIds.getResourceId()).getValue()).toLowerCase() :
                        (instance.getResource(pathIds.getResourceId()).isMultiInstances()) ?
                                instance.getResource(pathIds.getResourceId()).getValues().toString() :
                                instance.getResource(pathIds.getResourceId()).getValue().toString();
            }
        }
        return resValue;
    }

    /**
     * Prepare Sent to Thigsboard callback - Attribute or Telemetry
     *
     * @param msg            - JsonArray: [{name: value}]
     * @param topicName      - Api Attribute or Telemetry
     * @param registrationId - Id of Registration LwM2M Client
     */
    public void updateParametersOnThingsboard(JsonElement msg, String topicName, String registrationId) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(registrationId);
        if (sessionInfo != null) {
            try {
                if (topicName.equals(LwM2MTransportHandler.DEVICE_ATTRIBUTES_TOPIC)) {
                    PostAttributeMsg postAttributeMsg = adaptor.convertToPostAttributes(msg);
                    TransportServiceCallback call = this.getPubAckCallbackSentAttrTelemetry(-1, postAttributeMsg);
                    transportService.process(sessionInfo, postAttributeMsg, call);
                } else if (topicName.equals(LwM2MTransportHandler.DEVICE_TELEMETRY_TOPIC)) {
                    PostTelemetryMsg postTelemetryMsg = adaptor.convertToPostTelemetry(msg);
                    TransportServiceCallback call = this.getPubAckCallbackSentAttrTelemetry(-1, postTelemetryMsg);
                    transportService.process(sessionInfo, postTelemetryMsg, this.getPubAckCallbackSentAttrTelemetry(-1, call));
                }
            } catch (AdaptorException e) {
                log.error("[{}] Failed to process publish msg [{}]", topicName, e);
                log.info("[{}] Closing current session due to invalid publish", topicName);
            }
        } else {
            log.error("Client: [{}] updateParametersOnThingsboard [{}] sessionInfo ", registrationId, sessionInfo);
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
                log.trace("Success to publish msg: {}, dummy: {}", msg, dummy);
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {}", msg, e);
            }
        };
    }

    /**
     * Start observe
     * #1 - Analyze:
     * #1.1 path in observe == (attribute or telemetry)
     * #1.2 recourseValue notNull
     * #2 Analyze after sent request (response):
     * #2.1 First: lwM2MTransportRequest.sendResponse -> ObservationListener.newObservation
     * #2.2 Next: ObservationListener.onResponse     *
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     */
    private void onSentObserveToClient(LeshanServer lwServer, Registration registration) {
        if (lwServer.getObservationService().getObservations(registration).size() > 0) {
            this.setCancelObservations(lwServer, registration);
        }
        UUID profileUUid = lwM2mInMemorySecurityStore.getSessions().get(registration.getId()).getProfileUuid();
        AttrTelemetryObserveValue attrTelemetryObserveValue = lwM2mInMemorySecurityStore.getProfiles().get(profileUUid);
        attrTelemetryObserveValue.getPostObserveProfile().forEach(p -> {
            // #1.1
            String target = (getValidateObserve(attrTelemetryObserveValue.getPostAttributeProfile(), p.getAsString().toString())) ?
                    p.getAsString().toString() : (getValidateObserve(attrTelemetryObserveValue.getPostTelemetryProfile(), p.getAsString().toString())) ?
                    p.getAsString().toString() : null;
            if (target != null) {
                // #1.2
                ResultIds pathIds = new ResultIds(target);
                ModelObject modelObject = lwM2mInMemorySecurityStore.getSessions().get(registration.getId()).getModelObjects().get(pathIds.getObjectId());
                // #2
                if (modelObject != null) {
                    if (getResourceValue(modelObject, pathIds) != null) {
                        lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, GET_TYPE_OPER_OBSERVE,
                                null, null, null, "", this.context.getCtxServer().getTimeout());
                    }
                }
            }
        });
    }

    public void setCancelObservations(LeshanServer lwServer, Registration registration) {
        if (registration != null) {
            Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
            observations.forEach(observation -> {
                this.setCancelObservationRecourse(lwServer, registration, observation.getPath().toString());
            });
        }
    }

    /**
     * lwM2MTransportRequest.sendAllRequest(lwServer, registration, path, POST_TYPE_OPER_OBSERVE_CANCEL, null, null, null, null, context.getTimeout());
     * At server side this will not remove the observation from the observation store, to do it you need to use
     * {@code ObservationService#cancelObservation()}
     */
    public void setCancelObservationRecourse(LeshanServer lwServer, Registration registration, String path) {
        CountDownLatch cancelLatch = new CountDownLatch(1);
        lwServer.getObservationService().cancelObservations(registration, path);
        cancelLatch.countDown();
        try {
            cancelLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    /**
     * @param parameters - JsonArray postAttributeProfile/postTelemetryProfile
     * @param path       - recourse from postObserveProfile
     * @return rez - true if path observe is in attribute/telemetry
     */
    private boolean getValidateObserve(JsonElement parameters, String path) {
        AtomicBoolean rez = new AtomicBoolean(false);
        if (parameters.isJsonArray()) {
            parameters.getAsJsonArray().forEach(p -> {
                        if (p.getAsString().toString().equals(path)) rez.set(true);
                    }
            );
        } else if (parameters.isJsonObject()) {
            rez.set((parameters.getAsJsonObject().entrySet()).stream().map(json -> json.toString())
                    .filter(path::equals).findAny().orElse(null) != null);
        }
        return rez.get();
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
        if (response.getContent() != null) {
            if (response.getContent() instanceof LwM2mObject) {
                LwM2mObject content = (LwM2mObject) response.getContent();
                String target = "/" + content.getId();
            } else if (response.getContent() instanceof LwM2mObjectInstance) {
                LwM2mObjectInstance content = (LwM2mObjectInstance) response.getContent();
            } else if (response.getContent() instanceof LwM2mSingleResource) {
                LwM2mSingleResource content = (LwM2mSingleResource) response.getContent();
                this.onObservationSetResourcesValue(registration, content.getValue(), null, path);
            } else if (response.getContent() instanceof LwM2mMultipleResource) {
                LwM2mSingleResource content = (LwM2mSingleResource) response.getContent();
                this.onObservationSetResourcesValue(registration, null, content.getValues(), path);
            }
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
     * @param registration - Registration LwM2M Client
     * @param value        - LwM2mSingleResource response.getContent()
     * @param values       - LwM2mSingleResource response.getContent()
     * @param path         - resource
     */
    @SneakyThrows
    private void onObservationSetResourcesValue(Registration registration, Object value, Map<Integer, ?> values, String path) {

        ResultIds resultIds = new ResultIds(path);
        // #1
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registration.getId());
        ModelObject modelObject = lwM2MClient.getModelObjects().get(resultIds.getObjectId());
        Map<Integer, LwM2mObjectInstance> instancesModelObject = modelObject.getInstances();
        LwM2mObjectInstance instanceOld = (instancesModelObject.get(resultIds.instanceId) != null) ? instancesModelObject.get(resultIds.instanceId) : null;
        Map<Integer, LwM2mResource> resourcesOld = (instanceOld != null) ? instanceOld.getResources() : null;
        LwM2mResource resourceOld = (resourcesOld != null && resourcesOld.get(resultIds.getResourceId()) != null) ? resourcesOld.get(resultIds.getResourceId()) : null;
        // #2
        LwM2mResource resourceNew;
        if (resourceOld.isMultiInstances()) {
            resourceNew = LwM2mMultipleResource.newResource(resultIds.getResourceId(), values, resourceOld.getType());
        } else {
            resourceNew = LwM2mSingleResource.newResource(resultIds.getResourceId(), value, resourceOld.getType());
        }
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
        lwM2MClient.getModelObjects().get(resultIds.getObjectId()).removeInstance(resultIds.instanceId);
        instancesModelObject.put(resultIds.instanceId, instanceNew);
        respLatch.countDown();
        try {
            respLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
        }
        Set<String> paths = new HashSet<String>();
        paths.add(path);
        this.updateAttrTelemetry(registration, false, paths);
    }

    /**
     * @param updateCredentials - Credentials include config only security Client (without config attr/telemetry...)
     *  config attr/telemetry... in profile
     */

    public void updateParametersInClientFomProfile(ToTransportUpdateCredentialsProto updateCredentials) {
        log.info("[{}] idList [{}] valueList updateCredentials", updateCredentials.getCredentialsIdList(), updateCredentials.getCredentialsValueList());
    }

    /**
     *
     * @param sessionInfo -
     * @param deviceProfile -
     */
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        UUID sessionUuId = new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB());
        String registrationId = lwM2mInMemorySecurityStore.getSessions().entrySet()
                .stream()
                .filter(e -> e.getValue().getDeviceUuid().equals(sessionUuId))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse("");
        if (!registrationId.isEmpty()) {
            this.onDeviceUpdateChangeProfile(registrationId, deviceProfile);
        }
    }

    /**
     * @param sessionInfo      -
     * @param device           -
     * @param deviceProfileOpt -
     */
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        Optional<String> registrationIdOpt = lwM2mInMemorySecurityStore.getSessions().entrySet().stream()
                .filter(e -> device.getUuidId().equals(e.getValue().getDeviceUuid()))
                .map(Map.Entry::getKey)
                .findFirst();
        registrationIdOpt.ifPresent(registrationId -> this.onDeviceUpdateLwM2MClient(registrationId, device, deviceProfileOpt));
    }

    /**
     * Update parameters device in LwM2MClient
     * If new deviceProfile != old deviceProfile => update deviceProfile
     * @param registrationId -
     * @param device         -
     */
    private void onDeviceUpdateLwM2MClient(String registrationId, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getSessions().get(registrationId);
        lwM2MClient.setDeviceName(device.getName());
        if (!lwM2MClient.getProfileUuid().equals(device.getDeviceProfileId().getId())) {
            deviceProfileOpt.ifPresent(deviceProfile -> this.onDeviceUpdateChangeProfile(registrationId, deviceProfile));
        }
    }

    /**
     * #1 Read new, old Value (Attribute, Telemetry, Observe, KeyName)
     * #2 Update in lwM2MClient: ...Profile
     * #3 Equivalence test: old <> new Value (Attribute, Telemetry, Observe, KeyName)
     * #3.1 Attribute isChange (add&del)
     * #3.2 Telemetry isChange (add&del)
     * #3.3 KeyName isChange (add)
     * #4 update
     * #4.1 add If #3 isChange, then analyze and update Value in Transport form Client and sent Value ti thingsboard
     * #4.2 del
     * -- if  add attributes includes del telemetry - result del for observe
     * #5
     * #5.1 Observe isChange (add&del)
     * #5.2 Observe.add
     * -- path Attr/Telemetry includes newObserve and does not include oldObserve: sent Request observe to Client
     * #5.3 Observe.del
     * -- different between newObserve and oldObserve: sent Request cancel observe to client
     *
     * @param registrationId -
     * @param deviceProfile  -
     */
    public void onDeviceUpdateChangeProfile(String registrationId, DeviceProfile deviceProfile) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registrationId);
        AttrTelemetryObserveValue attrTelemetryObserveValueOld = lwM2mInMemorySecurityStore.getProfiles().get(lwM2MClient.getProfileUuid());
        if (lwM2mInMemorySecurityStore.addUpdateProfileParameters(deviceProfile)) {
            LeshanServer lwServer = lwM2MClient.getLwServer();
            Registration registration = lwM2mInMemorySecurityStore.getByRegistration(registrationId);
            // #1
            JsonArray attributeOld = attrTelemetryObserveValueOld.getPostAttributeProfile();
            Set<String> attributeSetOld = new Gson().fromJson(attributeOld, Set.class);
            JsonArray telemetryOld = attrTelemetryObserveValueOld.getPostTelemetryProfile();
            Set<String> telemetrySetOld = new Gson().fromJson(telemetryOld, Set.class);
            JsonArray observeOld = attrTelemetryObserveValueOld.getPostObserveProfile();
            JsonObject keyNameOld = attrTelemetryObserveValueOld.getPostKeyNameProfile();

            AttrTelemetryObserveValue attrTelemetryObserveValueNew = lwM2mInMemorySecurityStore.getProfiles().get(deviceProfile.getUuidId());
            JsonArray attributeNew = attrTelemetryObserveValueNew.getPostAttributeProfile();
            Set<String> attributeSetNew = new Gson().fromJson(attributeNew, Set.class);
            JsonArray telemetryNew = attrTelemetryObserveValueNew.getPostTelemetryProfile();
            Set<String> telemetrySetNew = new Gson().fromJson(telemetryNew, Set.class);
            JsonArray observeNew = attrTelemetryObserveValueNew.getPostObserveProfile();
            JsonObject keyNameNew = attrTelemetryObserveValueNew.getPostKeyNameProfile();
            // #2
            lwM2MClient.setDeviceProfileName(deviceProfile.getName());
            lwM2MClient.setProfileUuid(deviceProfile.getUuidId());

            // #3
            ResultsAnalyzerParameters sentAttrToThingsboard = new ResultsAnalyzerParameters();
            // #3.1
            if (!attributeOld.equals(attributeNew)) {
                ResultsAnalyzerParameters postAttributeAnalyzer = this.getAnalyzerParameters(new Gson().fromJson(attributeOld, Set.class), attributeSetNew);
                sentAttrToThingsboard.getPathPostParametersAdd().addAll(postAttributeAnalyzer.getPathPostParametersAdd());
                sentAttrToThingsboard.getPathPostParametersDel().addAll(postAttributeAnalyzer.getPathPostParametersDel());
            }
            // #3.2
            if (!attributeOld.equals(attributeNew)) {
                ResultsAnalyzerParameters postTelemetryAnalyzer = this.getAnalyzerParameters(new Gson().fromJson(telemetryOld, Set.class), telemetrySetNew);
                sentAttrToThingsboard.getPathPostParametersAdd().addAll(postTelemetryAnalyzer.getPathPostParametersAdd());
                sentAttrToThingsboard.getPathPostParametersDel().addAll(postTelemetryAnalyzer.getPathPostParametersDel());
            }
            // #3.3
            if (!keyNameOld.equals(keyNameNew)) {
                ResultsAnalyzerParameters keyNameChange = this.getAnalyzerKeyName(new Gson().fromJson(keyNameOld.toString(), ConcurrentHashMap.class),
                        new Gson().fromJson(keyNameNew.toString(), ConcurrentHashMap.class));
                sentAttrToThingsboard.getPathPostParametersAdd().addAll(keyNameChange.getPathPostParametersAdd());
            }

            // #4.1 add
            if (sentAttrToThingsboard.getPathPostParametersAdd().size() > 0) {
                // update value in Resources
                this.updateResourceValueObserve(lwServer, registration, lwM2MClient, sentAttrToThingsboard.getPathPostParametersAdd(), GET_TYPE_OPER_READ);
                // sent attr/telemetry to tingsboard for new path
                this.updateAttrTelemetry(registration, false, sentAttrToThingsboard.getPathPostParametersAdd());
            }
            // #4.2 del
            if (sentAttrToThingsboard.getPathPostParametersDel().size() > 0) {
                ResultsAnalyzerParameters sentAttrToThingsboardDel = this.getAnalyzerParameters(sentAttrToThingsboard.getPathPostParametersAdd(), sentAttrToThingsboard.getPathPostParametersDel());
                sentAttrToThingsboard.setPathPostParametersDel(sentAttrToThingsboardDel.getPathPostParametersDel());
            }

            // #5.1
            if (!observeOld.equals(observeNew)) {
                Set<String> observeSetOld = new Gson().fromJson(observeOld, Set.class);
                Set<String> observeSetNew = new Gson().fromJson(observeNew, Set.class);
                //#5.2 add
                //  path Attr/Telemetry includes newObserve
                attributeSetOld.addAll(telemetrySetOld);
                ResultsAnalyzerParameters sentObserveToClientOld = this.getAnalyzerParametersIn(attributeSetOld, observeSetOld); // add observe
                attributeSetNew.addAll(telemetrySetNew);
                ResultsAnalyzerParameters sentObserveToClientNew = this.getAnalyzerParametersIn(attributeSetNew, observeSetNew); // add observe
                // does not include oldObserve
                ResultsAnalyzerParameters postObserveAnalyzer = this.getAnalyzerParameters(sentObserveToClientOld.getPathPostParametersAdd(), sentObserveToClientNew.getPathPostParametersAdd());
                //  sent Request observe to Client
                this.updateResourceValueObserve(lwServer, registration, lwM2MClient, postObserveAnalyzer.getPathPostParametersAdd(), GET_TYPE_OPER_OBSERVE);
                // 5.3 del
                //  sent Request cancel observe to Client
                this.cancelObserveIsValue(lwServer, registration, postObserveAnalyzer.getPathPostParametersDel());
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

    private ResultsAnalyzerParameters getAnalyzerKeyName(ConcurrentMap<String, String> keyNameOld, ConcurrentMap<String, String> keyNameNew) {
        ResultsAnalyzerParameters analyzerParameters = new ResultsAnalyzerParameters();
        Set<String> paths = keyNameNew.entrySet()
                .stream()
                .filter(e -> !e.getValue().equals(keyNameOld.get(e.getKey())))
                .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue())).keySet();
        analyzerParameters.setPathPostParametersAdd(paths);
        return analyzerParameters;
    }

    private ResultsAnalyzerParameters getAnalyzerParametersIn(Set<String> parametersObserve, Set<String> parameters) {
        ResultsAnalyzerParameters analyzerParameters = new ResultsAnalyzerParameters();
        analyzerParameters.setPathPostParametersAdd(parametersObserve
                .stream().filter(p -> parameters.contains(p)).collect(Collectors.toSet()));
        return analyzerParameters;
    }

    /**
     * Update Resource value after change RezAttrTelemetry in config Profile
     * sent response Read to Client and add path to pathResAttrTelemetry in LwM2MClient.getAttrTelemetryObserveValue()
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     * @param lwM2MClient  - object with All parameters off client
     * @param targets      - path Resources
     */
    private void updateResourceValueObserve(LeshanServer lwServer, Registration registration, LwM2MClient lwM2MClient, Set<String> targets, String typeOper) {
        targets.stream().forEach(target -> {
            ResultIds pathIds = new ResultIds(target);
            if (pathIds.resourceId >= 0 && lwM2MClient.getModelObjects().get(pathIds.getObjectId())
                    .getInstances().get(pathIds.getInstanceId()).getResource(pathIds.getResourceId()).getValue() != null) {
                if (GET_TYPE_OPER_READ.equals(typeOper)) {
                    lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, typeOper,
                            ContentFormat.TLV.getName(), null, null, "", this.context.getCtxServer().getTimeout());
                } else if (GET_TYPE_OPER_OBSERVE.equals(typeOper)) {
                    lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, typeOper,
                            null, null, null, "", this.context.getCtxServer().getTimeout());
                }
            }
        });
    }

    private void cancelObserveIsValue(LeshanServer lwServer, Registration registration, Set<String> paramAnallyzer) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getByRegistrationIdModelClient(registration.getId());
        paramAnallyzer.forEach(p -> {
                    if (this.getResourceValue(lwM2MClient, p) != null) {
                        this.setCancelObservationRecourse(lwServer, registration, p);
                    }
                }
        );
    }

    private ResourceValue getResourceValue(LwM2MClient lwM2MClient, String path) {
        ResourceValue resourceValue = null;
        ResultIds pathIds = new ResultIds(path);
        if (pathIds.getResourceId() > -1) {
            LwM2mResource resource = lwM2MClient.getModelObjects().get(pathIds.getObjectId()).getInstances().get(pathIds.getInstanceId()).getResource(pathIds.getResourceId());
            if (resource.isMultiInstances()) {
                Map<Integer, ?> values = resource.getValues();
                if (resource.getValues().size() > 0) {
                    resourceValue = new ResourceValue();
                    resourceValue.setMultiInstances(resource.isMultiInstances());
                    resourceValue.setValues(resource.getValues());
                }
            } else {
                if (resource.getValue() != null) {
                    resourceValue = new ResourceValue();
                    resourceValue.setMultiInstances(resource.isMultiInstances());
                    resourceValue.setValue(resource.getValue());
                }
            }
        }
        return resourceValue;
    }

    /**
     * Trigger Server path = "/1/0/8"
     *
     * Trigger bootStrap path = "/1/0/9" - have to implemented on client
     */
    public void doTrigger(LeshanServer lwServer, Registration registration, String path) {
        lwM2MTransportRequest.sendAllRequest(lwServer, registration, path, POST_TYPE_OPER_EXECUTE,
                ContentFormat.TLV.getName(), null, null, "", this.context.getCtxServer().getTimeout());
    }

    /**
     * Session device in thingsboard is closed
     *
     * @param sessionInfo - lwm2m client
     */
    private void doCloseSession(SessionInfoProto sessionInfo) {
        TransportProtos.SessionEvent event = SessionEvent.CLOSED;
        TransportProtos.SessionEventMsg msg = TransportProtos.SessionEventMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .setEvent(event).build();
        transportService.process(sessionInfo, msg, null);
    }

    /**
     * Deregister session in transport
     *
     * @param sessionInfo - lwm2m client
     */
    private void doDisconnect(SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(SessionEvent.CLOSED), null);
        transportService.deregisterSession(sessionInfo);
    }

    private void checkInactivityAndReportActivity() {
        lwM2mInMemorySecurityStore.getSessions().forEach((key, value) -> transportService.reportActivity(this.getValidateSessionInfo(key)));
    }

}
