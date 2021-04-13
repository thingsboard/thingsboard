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
package org.thingsboard.server.transport.lwm2m.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientProfile;
import org.thingsboard.server.transport.lwm2m.server.client.ResultsAnalyzerParameters;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.common.transport.util.JsonUtils.getJsonObject;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.CLIENT_NOT_AUTHORIZED;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.DEVICE_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.DEVICE_TELEMETRY_TOPIC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.GET_TYPE_OPER_OBSERVE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.GET_TYPE_OPER_READ;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.LOG_LW2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.LOG_LW2M_TELEMETRY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.LWM2M_STRATEGY_2;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.POST_TYPE_OPER_EXECUTE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.POST_TYPE_OPER_WRITE_REPLACE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.PUT_TYPE_OPER_WRITE_ATTRIBUTES;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.SERVICE_CHANNEL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.convertToIdVerFromObjectId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.convertToObjectIdFromIdVer;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.getAckCallback;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.validateObjectIdFromKey;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.validateObjectVerFromKey;

@Slf4j
@Service
@TbLwM2mTransportComponent
public class LwM2mTransportServiceImpl implements LwM2mTransportService {

    private ExecutorService executorRegistered;
    private ExecutorService executorUpdateRegistered;
    private ExecutorService executorUnRegistered;
    private LwM2mValueConverterImpl converter;

    private final TransportService transportService;

    public final LwM2mTransportContextServer lwM2mTransportContextServer;

    private final LwM2mClientContext lwM2mClientContext;

    private final LeshanServer leshanServer;

    private final LwM2mTransportRequest lwM2mTransportRequest;

    public LwM2mTransportServiceImpl(TransportService transportService, LwM2mTransportContextServer lwM2mTransportContextServer, LwM2mClientContext lwM2mClientContext, LeshanServer leshanServer, @Lazy LwM2mTransportRequest lwM2mTransportRequest) {
        this.transportService = transportService;
        this.lwM2mTransportContextServer = lwM2mTransportContextServer;
        this.lwM2mClientContext = lwM2mClientContext;
        this.leshanServer = leshanServer;
        this.lwM2mTransportRequest = lwM2mTransportRequest;
    }

    @PostConstruct
    public void init() {
        this.lwM2mTransportContextServer.getScheduler().scheduleAtFixedRate(this::checkInactivityAndReportActivity, new Random().nextInt((int) lwM2mTransportContextServer.getLwM2MTransportConfigServer().getSessionReportTimeout()), lwM2mTransportContextServer.getLwM2MTransportConfigServer().getSessionReportTimeout(), TimeUnit.MILLISECONDS);
        this.executorRegistered = Executors.newFixedThreadPool(this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getRegisteredPoolSize(),
                new NamedThreadFactory(String.format("LwM2M %s channel registered", SERVICE_CHANNEL)));
        this.executorUpdateRegistered = Executors.newFixedThreadPool(this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getUpdateRegisteredPoolSize(),
                new NamedThreadFactory(String.format("LwM2M %s channel update registered", SERVICE_CHANNEL)));
        this.executorUnRegistered = Executors.newFixedThreadPool(this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getUnRegisteredPoolSize(),
                new NamedThreadFactory(String.format("LwM2M %s channel un registered", SERVICE_CHANNEL)));
        this.converter = LwM2mValueConverterImpl.getInstance();
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
     * @param registration         - Registration LwM2M Client
     * @param previousObsersations - may be null
     */
    public void onRegistered(Registration registration, Collection<Observation> previousObsersations) {
        executorRegistered.submit(() -> {
            try {
                log.warn("[{}] [{{}] Client: create after Registration", registration.getEndpoint(), registration.getId());
                LwM2mClient lwM2MClient = this.lwM2mClientContext.updateInSessionsLwM2MClient(registration);
                if (lwM2MClient != null) {
                    SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration);
                    if (sessionInfo != null) {
                        this.initLwM2mClient(lwM2MClient, sessionInfo);
                        transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(this, sessionInfo));
                        transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(SessionEvent.OPEN), null);
                        transportService.process(sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().build(), null);
                        this.initLwM2mFromClientValue(registration, lwM2MClient);
                        this.sendLogsToThingsboard(LOG_LW2M_INFO + ": Client create after Registration", registration);
                    } else {
                        log.error("Client: [{}] onRegistered [{}] name  [{}] sessionInfo ", registration.getId(), registration.getEndpoint(), null);
                    }
                } else {
                    log.error("Client: [{}] onRegistered [{}] name  [{}] lwM2MClient ", registration.getId(), registration.getEndpoint(), null);
                }
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable registration.", registration.getEndpoint(), t);
            }
        });
    }

    /**
     * if sessionInfo removed from sessions, then new registerAsyncSession
     *
     * @param registration - Registration LwM2M Client
     */
    public void updatedReg(Registration registration) {
        executorUpdateRegistered.submit(() -> {
            try {
                SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration);
                if (sessionInfo != null) {
                    this.checkInactivity(sessionInfo);
                    LwM2mClient lwM2MClient = this.lwM2mClientContext.getLwM2MClient(sessionInfo);
                    if (lwM2MClient.getDeviceId() == null && lwM2MClient.getProfileId() == null) {
                        initLwM2mClient(lwM2MClient, sessionInfo);
                    }

                    log.info("Client: [{}] updatedReg [{}] name  [{}] profile ", registration.getId(), registration.getEndpoint(), sessionInfo.getDeviceType());
                } else {
                    log.error("Client: [{}] updatedReg [{}] name  [{}] sessionInfo ", registration.getId(), registration.getEndpoint(), null);
                }
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable update registration.", registration.getEndpoint(), t);
            }
        });
    }

    /**
     * @param registration - Registration LwM2M Client
     * @param observations - All paths observations before unReg
     *                     !!! Warn: if have not finishing unReg, then this operation will be finished on next Client`s connect
     */
    public void unReg(Registration registration, Collection<Observation> observations) {
        executorUnRegistered.submit(() -> {
            try {
                this.setCancelObservations(registration);
                this.sendLogsToThingsboard(LOG_LW2M_INFO + ": Client unRegistration", registration);
                this.closeClientSession(registration);
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable un registration.", registration.getEndpoint(), t);
            }
        });
    }

    private void initLwM2mClient(LwM2mClient lwM2MClient, SessionInfoProto sessionInfo) {
        lwM2MClient.setDeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        lwM2MClient.setProfileId(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
        lwM2MClient.setDeviceName(sessionInfo.getDeviceName());
        lwM2MClient.setDeviceProfileName(sessionInfo.getDeviceType());
    }

    private void closeClientSession(Registration registration) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration);
        if (sessionInfo != null) {
            transportService.deregisterSession(sessionInfo);
            this.doCloseSession(sessionInfo);
            lwM2mClientContext.delRemoveSessionAndListener(registration.getId());
            if (lwM2mClientContext.getProfiles().size() > 0) {
                this.syncSessionsAndProfiles();
            }
            log.info("Client close session: [{}] unReg [{}] name  [{}] profile ", registration.getId(), registration.getEndpoint(), sessionInfo.getDeviceType());
        } else {
            log.error("Client close session: [{}] unReg [{}] name  [{}] sessionInfo ", registration.getId(), registration.getEndpoint(), null);
        }
    }

    public void onSleepingDev(Registration registration) {
        log.info("[{}] [{}] Received endpoint Sleeping version event", registration.getId(), registration.getEndpoint());
        //TODO: associate endpointId with device information.
    }

    @Override
    public void setCancelObservations(Registration registration) {
        if (registration != null) {
            Set<Observation> observations = leshanServer.getObservationService().getObservations(registration);
            observations.forEach(observation -> this.setCancelObservationRecourse(registration, observation.getPath().toString()));
        }
    }

    /**
     * lwM2MTransportRequest.sendAllRequest(lwServer, registration, path, POST_TYPE_OPER_OBSERVE_CANCEL, null, null, null, null, context.getTimeout());
     * At server side this will not remove the observation from the observation store, to do it you need to use
     * {@code ObservationService#cancelObservation()}
     */
    @Override
    public void setCancelObservationRecourse(Registration registration, String path) {
        leshanServer.getObservationService().cancelObservations(registration, path);
    }

    /**
     * Sending observe value to thingsboard from ObservationListener.onResponse: object, instance, SingleResource or MultipleResource
     *
     * @param registration - Registration LwM2M Client
     * @param path         - observe
     * @param response     - observe
     */
    @Override
    public void onObservationResponse(Registration registration, String path, ReadResponse response) {
        if (response.getContent() != null) {
            if (response.getContent() instanceof LwM2mObject) {
                LwM2mObject lwM2mObject = (LwM2mObject) response.getContent();
                this.updateObjectResourceValue(registration, lwM2mObject, path);
            } else if (response.getContent() instanceof LwM2mObjectInstance) {
                LwM2mObjectInstance lwM2mObjectInstance = (LwM2mObjectInstance) response.getContent();
                this.updateObjectInstanceResourceValue(registration, lwM2mObjectInstance, path);
            } else if (response.getContent() instanceof LwM2mResource) {
                LwM2mResource lwM2mResource = (LwM2mResource) response.getContent();
                this.updateResourcesValue(registration, lwM2mResource, path);
            }
        }
    }

    /**
     * Update - send request in change value resources in Client
     * Path to resources from profile equal keyName or from ModelObject equal name
     * Only for resources:  isWritable && isPresent as attribute in profile -> LwM2MClientProfile (format: CamelCase)
     * Delete - nothing     *
     *
     * @param msg -
     */
    @Override
    public void onAttributeUpdate(AttributeUpdateNotificationMsg msg, TransportProtos.SessionInfoProto sessionInfo) {
        if (msg.getSharedUpdatedCount() > 0) {
            JsonElement el = JsonConverter.toJson(msg);
            el.getAsJsonObject().entrySet().forEach(de -> {
                String pathIdVer = this.getPathAttributeUpdate(sessionInfo, de.getKey());
                String value = de.getValue().getAsString();
                LwM2mClient lwM2MClient = lwM2mClientContext.getLwM2mClient(new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
                LwM2mClientProfile clientProfile = lwM2mClientContext.getProfile(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
                if (pathIdVer != null && !pathIdVer.isEmpty() && (this.validatePathInAttrProfile(clientProfile, pathIdVer) || this.validatePathInTelemetryProfile(clientProfile, pathIdVer))) {
                    ResourceModel resourceModel = lwM2MClient.getResourceModel(pathIdVer);
                    if (resourceModel != null && resourceModel.operations.isWritable()) {
                        lwM2mTransportRequest.sendAllRequest(lwM2MClient.getRegistration(), pathIdVer, POST_TYPE_OPER_WRITE_REPLACE,
                                ContentFormat.TLV.getName(), null, value, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getTimeout());
                    } else {
                        log.error("Resource path - [{}] value - [{}] is not Writable and cannot be updated", pathIdVer, value);
                        String logMsg = String.format("%s: attributeUpdate: Resource path - %s value - %s is not Writable and cannot be updated",
                                LOG_LW2M_ERROR, pathIdVer, value);
                        this.sendLogsToThingsboard(logMsg, lwM2MClient.getRegistration());
                    }
                } else {
                    log.error("Attribute name - [{}] value - [{}] is not present as attribute in profile and cannot be updated", de.getKey(), value);
                    String logMsg = String.format("%s: attributeUpdate: attribute name - %s value - %s is not present as attribute in profile and cannot be updated",
                            LOG_LW2M_ERROR, de.getKey(), value);
                    this.sendLogsToThingsboard(logMsg, lwM2MClient.getRegistration());
                }
            });
        } else if (msg.getSharedDeletedCount() > 0) {
            log.info("[{}] delete [{}]  onAttributeUpdate", msg.getSharedDeletedList(), sessionInfo);
        }
    }

    /**
     * @param sessionInfo   -
     * @param deviceProfile -
     */
    @Override
    public void onDeviceProfileUpdate(SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        Set<String> registrationIds = lwM2mClientContext.getLwM2mClients().entrySet()
                .stream()
                .filter(e -> e.getValue().getProfileId().equals(deviceProfile.getUuidId()))
                .map(Map.Entry::getKey).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        if (registrationIds.size() > 0) {
            this.onDeviceUpdateChangeProfile(registrationIds, deviceProfile);
        }
    }

    /**
     * @param sessionInfo      -
     * @param device           -
     * @param deviceProfileOpt -
     */
    @Override
    public void onDeviceUpdate(SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        Optional<String> registrationIdOpt = lwM2mClientContext.getLwM2mClients().entrySet().stream()
                .filter(e -> device.getUuidId().equals(e.getValue().getDeviceId()))
                .map(Map.Entry::getKey)
                .findFirst();
        registrationIdOpt.ifPresent(registrationId -> this.onDeviceUpdateLwM2MClient(registrationId, device, deviceProfileOpt));
    }

    /**
     *
     * @param resourceUpdateMsgOpt -
     */
    @Override
    public void onResourceUpdate(Optional<TransportProtos.ResourceUpdateMsg> resourceUpdateMsgOpt) {
        String idVer = resourceUpdateMsgOpt.get().getResourceKey();
        lwM2mClientContext.getLwM2mClients().values().stream().forEach(e -> e.updateResourceModel(idVer, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getModelProvider()));
    }

    /**
     *
     * @param resourceDeleteMsgOpt -
     */
    @Override
    public void onResourceDelete(Optional<TransportProtos.ResourceDeleteMsg> resourceDeleteMsgOpt) {
        String pathIdVer = resourceDeleteMsgOpt.get().getResourceKey();
        lwM2mClientContext.getLwM2mClients().values().stream().forEach(e -> e.deleteResources(pathIdVer, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getModelProvider()));
    }

    public void  onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg toDeviceRequest) {
        log.info("[{}] toDeviceRpcRequest", toDeviceRequest);
    }

    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        log.info("[{}] toServerRpcResponse", toServerResponse);
    }

    /**
     * Trigger Server path = "/1/0/8"
     * <p>
     * Trigger bootStrap path = "/1/0/9" - have to implemented on client
     */
    @Override
    public void doTrigger(Registration registration, String path) {
        lwM2mTransportRequest.sendAllRequest(registration, path, POST_TYPE_OPER_EXECUTE,
                ContentFormat.TLV.getName(), null, null, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getTimeout());
    }

    /**
     * Deregister session in transport
     *
     * @param sessionInfo - lwm2m client
     */
    @Override
    public void doDisconnect(SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(SessionEvent.CLOSED), null);
        transportService.deregisterSession(sessionInfo);
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
     * Those methods are called by the protocol stage thread pool, this means that execution MUST be done in a short delay,
     * * if you need to do long time processing use a dedicated thread pool.
     *
     * @param registration -
     */
    protected void onAwakeDev(Registration registration) {
        log.info("[{}] [{}] Received endpoint Awake version event", registration.getId(), registration.getEndpoint());
        //TODO: associate endpointId with device information.
    }

    /**
     * This method is used to sync with sessions
     * Removes a profile if not used in sessions
     */
    private void syncSessionsAndProfiles() {
        Map<UUID, LwM2mClientProfile> profilesClone = lwM2mClientContext.getProfiles().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        profilesClone.forEach((k, v) -> {
            String registrationId = lwM2mClientContext.getLwM2mClients().entrySet()
                    .stream()
                    .filter(e -> e.getValue().getProfileId().equals(k))
                    .findFirst()
                    .map(Map.Entry::getKey) // return the key of the matching entry if found
                    .orElse("");
            if (registrationId.isEmpty()) {
                lwM2mClientContext.getProfiles().remove(k);
            }
        });
    }

    /**
     * @param msg          - text msg
     * @param registration - Id of Registration LwM2M Client
     */
    public void sendLogsToThingsboard(String msg, Registration registration) {
        if (msg != null) {
            JsonObject telemetries = new JsonObject();
            telemetries.addProperty(LOG_LW2M_TELEMETRY, msg);
            this.updateParametersOnThingsboard(telemetries, DEVICE_TELEMETRY_TOPIC, registration);
        }
    }


    /**
     * // !!! Ok
     * Prepare send to Thigsboard callback - Attribute or Telemetry
     *
     * @param msg          - JsonArray: [{name: value}]
     * @param topicName    - Api Attribute or Telemetry
     * @param registration - Id of Registration LwM2M Client
     */
    public void updateParametersOnThingsboard(JsonElement msg, String topicName, Registration registration) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration);
        if (sessionInfo != null) {
            lwM2mTransportContextServer.sendParametersOnThingsboard(msg, topicName, sessionInfo);
        } else {
            log.error("Client: [{}] updateParametersOnThingsboard [{}] sessionInfo ", registration, null);
        }
    }

    /**
     * #1 clientOnlyObserveAfterConnect == true
     * - Only Observe Request to the client marked as observe from the profile configuration.
     * #2. clientOnlyObserveAfterConnect == false
     * После регистрации отправляю запрос на read  всех ресурсов, которые после регистрации есть у клиента,
     * а затем запрос на observe (edited)
     * - Read Request to the client after registration to read all resource values for all objects
     * - then Observe Request to the client marked as observe from the profile configuration.
     *
     * @param registration - Registration LwM2M Client
     * @param lwM2MClient  - object with All parameters off client
     */
    private void initLwM2mFromClientValue(Registration registration, LwM2mClient lwM2MClient) {
        LwM2mClientProfile lwM2MClientProfile = lwM2mClientContext.getProfile(registration);
        Set<String> clientObjects = lwM2mClientContext.getSupportedIdVerInClient(registration);
        if (clientObjects != null && clientObjects.size() > 0) {
            if (LWM2M_STRATEGY_2 == LwM2mTransportHandler.getClientOnlyObserveAfterConnect(lwM2MClientProfile)) {
                // #2
                lwM2MClient.getPendingRequests().addAll(clientObjects);
                clientObjects.forEach(path -> lwM2mTransportRequest.sendAllRequest(registration, path, GET_TYPE_OPER_READ, ContentFormat.TLV.getName(),
                        null, null, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getTimeout()));
            }
            // #1
            this.initReadAttrTelemetryObserveToClient(registration, lwM2MClient, GET_TYPE_OPER_OBSERVE, clientObjects);
            this.initReadAttrTelemetryObserveToClient(registration, lwM2MClient, PUT_TYPE_OPER_WRITE_ATTRIBUTES, clientObjects);
        }
    }

    /**
     * @param registration -
     * @param lwM2mObject  -
     * @param path         -
     */
    private void updateObjectResourceValue(Registration registration, LwM2mObject lwM2mObject, String path) {
        LwM2mPath pathIds = new LwM2mPath(path);
        lwM2mObject.getInstances().forEach((instanceId, instance) -> {
            String pathInstance = pathIds.toString() + "/" + instanceId;
            this.updateObjectInstanceResourceValue(registration, instance, pathInstance);
        });
    }

    /**
     * @param registration        -
     * @param lwM2mObjectInstance -
     * @param path                -
     */
    private void updateObjectInstanceResourceValue(Registration registration, LwM2mObjectInstance lwM2mObjectInstance, String path) {
        LwM2mPath pathIds = new LwM2mPath(path);
        lwM2mObjectInstance.getResources().forEach((resourceId, resource) -> {
            String pathRez = pathIds.toString() + "/" + resourceId;
            this.updateResourcesValue(registration, resource, pathRez);
        });
    }

    /**
     * Sending observe value of resources to thingsboard
     * #1 Return old Value Resource from LwM2MClient
     * #2 Update new Resources (replace old Resource Value on new Resource Value)
     *
     * @param registration  - Registration LwM2M Client
     * @param lwM2mResource - LwM2mSingleResource response.getContent()
     * @param path          - resource
     */
    private void updateResourcesValue(Registration registration, LwM2mResource lwM2mResource, String path) {
        LwM2mClient lwM2MClient = lwM2mClientContext.getLwM2mClientWithReg(registration, null);
        if (lwM2MClient.saveResourceValue(path, lwM2mResource, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getModelProvider())) {
            Set<String> paths = new HashSet<>();
            paths.add(path);
            this.updateAttrTelemetry(registration, paths);
        } else {
            log.error("Fail update Resource [{}]", lwM2mResource);
        }
    }

    /**
     * send Attribute and Telemetry to Thingsboard
     * #1 - get AttrName/TelemetryName with value from LwM2MClient:
     * -- resourceId == path from LwM2MClientProfile.postAttributeProfile/postTelemetryProfile/postObserveProfile
     * -- AttrName/TelemetryName == resourceName from ModelObject.objectModel, value from ModelObject.instance.resource(resourceId)
     * #2 - set Attribute/Telemetry
     *
     * @param registration - Registration LwM2M Client
     */
    private void updateAttrTelemetry(Registration registration, Set<String> paths) {
        JsonObject attributes = new JsonObject();
        JsonObject telemetries = new JsonObject();
        try {
            this.getParametersFromProfile(attributes, telemetries, registration, paths);
        } catch (Exception e) {
            log.error("UpdateAttrTelemetry", e);
        }
        if (attributes.getAsJsonObject().entrySet().size() > 0)
            this.updateParametersOnThingsboard(attributes, DEVICE_ATTRIBUTES_TOPIC, registration);
        if (telemetries.getAsJsonObject().entrySet().size() > 0)
            this.updateParametersOnThingsboard(telemetries, DEVICE_TELEMETRY_TOPIC, registration);
    }

    /**
     * @param clientProfile -
     * @param path          -
     * @return true if path isPresent in postAttributeProfile
     */
    private boolean validatePathInAttrProfile(LwM2mClientProfile clientProfile, String path) {
        try {
            List<String> attributesSet = new Gson().fromJson(clientProfile.getPostAttributeProfile(),
                    new TypeToken<List<String>>() {
                    }.getType());
            return attributesSet.stream().anyMatch(p -> p.equals(path));
        } catch (Exception e) {
            log.error("Fail Validate Path [{}] ClientProfile.Attribute", path, e);
            return false;
        }
    }

    /**
     * @param clientProfile -
     * @param path          -
     * @return true if path isPresent in postAttributeProfile
     */
    private boolean validatePathInTelemetryProfile(LwM2mClientProfile clientProfile, String path) {
        try {
            List<String> telemetriesSet = new Gson().fromJson(clientProfile.getPostTelemetryProfile(), new TypeToken<List<String>>() {
            }.getType());
            return telemetriesSet.stream().anyMatch(p -> p.equals(path));
        } catch (Exception e) {
            log.error("Fail Validate Path [{}] ClientProfile.Telemetry", path, e);
            return false;
        }
    }

    /**
     * Start observe/read: Attr/Telemetry
     * #1 - Analyze: path in resource profile == client resource
     * @param registration -
     */
    private void initReadAttrTelemetryObserveToClient(Registration registration, LwM2mClient lwM2MClient,
                                                      String typeOper, Set<String> clientObjects) {
        LwM2mClientProfile lwM2MClientProfile = lwM2mClientContext.getProfile(registration);
        Set<String> result = null;
        ConcurrentHashMap<String, Object> params = null;
        if (GET_TYPE_OPER_READ.equals(typeOper)) {
            result = JacksonUtil.fromString(lwM2MClientProfile.getPostAttributeProfile().toString(),
                    new TypeReference<>() {});
            result.addAll(JacksonUtil.fromString(lwM2MClientProfile.getPostTelemetryProfile().toString(),
                    new TypeReference<>() {}));
        } else if (GET_TYPE_OPER_OBSERVE.equals(typeOper)) {
            result = JacksonUtil.fromString(lwM2MClientProfile.getPostObserveProfile().toString(),
                    new TypeReference<>() {});
        } else if (PUT_TYPE_OPER_WRITE_ATTRIBUTES.equals(typeOper)) {
            params =  this.getPathForWriteAttributes (lwM2MClientProfile.getPostAttributeLwm2mProfile());
            result = params.keySet();
        }
        if (!result.isEmpty()) {
            // #1
            Set<String> pathSend = result.stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
                    .collect(Collectors.toUnmodifiableSet());
            if (!pathSend.isEmpty()) {
                lwM2MClient.getPendingRequests().addAll(pathSend);
                ConcurrentHashMap<String, Object> finalParams = params;
                pathSend.forEach(target -> lwM2mTransportRequest.sendAllRequest(registration, target, typeOper, ContentFormat.TLV.getName(),
                        null, finalParams != null ? finalParams.get(target) : null, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getTimeout()));
                if (GET_TYPE_OPER_OBSERVE.equals(typeOper)) {
                    lwM2MClient.initValue(this, null);
                }
            }
        }
    }

    private ConcurrentHashMap<String, Object> getPathForWriteAttributes (JsonObject objectJson) {
        ConcurrentHashMap<String, Object> writeAttributes = new Gson().fromJson(objectJson.toString(),
                new TypeToken<ConcurrentHashMap<String, Object>>() {}.getType());
        return writeAttributes;
    }

//    /**
//     * #1 - Analyze: path in resource profile == client resource
//     * @param result
//     * @param clientObjects
//     * @return
//     */
//    private Set<String> isPathInClient(Set<String> result, Set<String> clientObjects) {
////        Set<String> pathSend = ConcurrentHashMap.newKeySet();
//        return result.stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
//                .collect(Collectors.toUnmodifiableSet());
////        if (!result.isEmpty()) {
////            result.forEach(target -> {
////                // #1
////                 if (clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1])) {
////                    pathSend.add(target);
////                }
////            });
////
////        }
////        return pathSend;
//    }

    /**
     * Update parameters device in LwM2MClient
     * If new deviceProfile != old deviceProfile => update deviceProfile
     *
     * @param registrationId -
     * @param device         -
     */
    private void onDeviceUpdateLwM2MClient(String registrationId, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        LwM2mClient lwM2MClient = lwM2mClientContext.getLwM2mClients().get(registrationId);
        lwM2MClient.setDeviceName(device.getName());
        if (!lwM2MClient.getProfileId().equals(device.getDeviceProfileId().getId())) {
            Set<String> registrationIds = new HashSet<>();
            registrationIds.add(registrationId);
            deviceProfileOpt.ifPresent(deviceProfile -> this.onDeviceUpdateChangeProfile(registrationIds, deviceProfile));
        }

        lwM2MClient.setProfileId(device.getDeviceProfileId().getId());
    }

    /**
     * @param registration -
     * @return all instances in client
     */
    private Set<String> getAllInstancesInClient(Registration registration) {
        Set<String> clientInstances = ConcurrentHashMap.newKeySet();
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            LwM2mPath pathIds = new LwM2mPath(url.getUrl());
            if (pathIds.isObjectInstance()) {
                clientInstances.add(convertToIdVerFromObjectId(url.getUrl(), registration));
            }
        });
        return (clientInstances.size() > 0) ? clientInstances : null;
    }

    /**
     * @param attributes   - new JsonObject
     * @param telemetry    - new JsonObject
     * @param registration - Registration LwM2M Client
     * @param path         -
     */
    private void getParametersFromProfile(JsonObject attributes, JsonObject telemetry, Registration registration, Set<String> path) {
        if (path != null && path.size() > 0) {
            LwM2mClientProfile lwM2MClientProfile = lwM2mClientContext.getProfile(registration);
            lwM2MClientProfile.getPostAttributeProfile().forEach(idVer -> {
                if (path.contains(idVer.getAsString())) {
                    this.addParameters(idVer.getAsString(), attributes, registration);
                }
            });
            lwM2MClientProfile.getPostTelemetryProfile().forEach(idVer -> {
                if (path.contains(idVer.getAsString())) {
                    this.addParameters(idVer.getAsString(), telemetry, registration);
                }
            });
        }
    }

    /**
     * @param parameters   - JsonObject attributes/telemetry
     * @param registration - Registration LwM2M Client
     */
    private void addParameters(String path, JsonObject parameters, Registration registration) {
        LwM2mClient lwM2MClient = lwM2mClientContext.getLwM2mClientWithReg(registration, null);
        JsonObject names = lwM2mClientContext.getProfiles().get(lwM2MClient.getProfileId()).getPostKeyNameProfile();
        if (names != null && names.has(path)) {
            String resName = names.get(path).getAsString();
            if (resName != null && !resName.isEmpty()) {
                try {
                    String resValue = this.getResourceValueToString(lwM2MClient, path);
                    if (resValue != null) {
                        parameters.addProperty(resName, resValue);
                    }
                } catch (Exception e) {
                    log.error("Failed to add parameters.", e);
                }
            }
        }
        else {
            log.error("Failed to add parameters. path: [{}], names: [{}]", path, names);
        }
    }

    /**
     * @param path - path resource
     * @return - value of Resource or null
     */
    private String getResourceValueToString(LwM2mClient lwM2MClient, String path) {
        LwM2mPath pathIds = new LwM2mPath(convertToObjectIdFromIdVer(path));
        LwM2mResource resourceValue = this.returnResourceValueFromLwM2MClient(lwM2MClient, path);
        return resourceValue == null ? null :
                this.converter.convertValue(resourceValue.isMultiInstances() ? resourceValue.getValues() : resourceValue.getValue(), resourceValue.getType(), ResourceModel.Type.STRING, pathIds).toString();
    }

    /**
     * @param lwM2MClient -
     * @param path     -
     * @return - return value of Resource by idPath
     */
    private LwM2mResource returnResourceValueFromLwM2MClient(LwM2mClient lwM2MClient, String path) {
        LwM2mResource resourceValue = null;
        if (new LwM2mPath(convertToObjectIdFromIdVer(path)).isResource()) {
            resourceValue = lwM2MClient.getResources().get(path).getLwM2mResource();
        }
        return resourceValue;
    }

    /**
     * Update resource (attribute) value  on thingsboard after update value in client
     *
     * @param registration -
     * @param path         -
     * @param request      -
     */
    public void onWriteResponseOk(Registration registration, String path, WriteRequest request) {
        this.updateResourcesValue(registration, ((LwM2mResource) request.getNode()), path);
    }

    /**
     * #1 Read new, old Value (Attribute, Telemetry, Observe, KeyName)
     * #2 Update in lwM2MClient: ...Profile if changes from update device
     * #3 Equivalence test: old <> new Value (Attribute, Telemetry, Observe, KeyName)
     * #3.1 Attribute isChange (add&del)
     * #3.2 Telemetry isChange (add&del)
     * #3.3 KeyName isChange (add)
     * #4 update
     * #4.1 add If #3 isChange, then analyze and update Value in Transport form Client and send Value to thingsboard
     * #4.2 del
     * -- if  add attributes includes del telemetry - result del for observe
     * #5
     * #5.1 Observe isChange (add&del)
     * #5.2 Observe.add
     * -- path Attr/Telemetry includes newObserve and does not include oldObserve: send Request observe to Client
     * #5.3 Observe.del
     * -- different between newObserve and oldObserve: send Request cancel observe to client
     *
     * @param registrationIds -
     * @param deviceProfile   -
     */
    private void onDeviceUpdateChangeProfile(Set<String> registrationIds, DeviceProfile deviceProfile) {
        LwM2mClientProfile lwM2MClientProfileOld = lwM2mClientContext.getProfiles().get(deviceProfile.getUuidId()).clone();
        if (lwM2mClientContext.addUpdateProfileParameters(deviceProfile)) {
            // #1
            JsonArray attributeOld = lwM2MClientProfileOld.getPostAttributeProfile();
            Set<String> attributeSetOld = this.convertJsonArrayToSet(attributeOld);
            JsonArray telemetryOld = lwM2MClientProfileOld.getPostTelemetryProfile();
            Set<String> telemetrySetOld = this.convertJsonArrayToSet(telemetryOld);
            JsonArray observeOld = lwM2MClientProfileOld.getPostObserveProfile();
            JsonObject keyNameOld = lwM2MClientProfileOld.getPostKeyNameProfile();

            LwM2mClientProfile lwM2MClientProfileNew = lwM2mClientContext.getProfiles().get(deviceProfile.getUuidId());
            JsonArray attributeNew = lwM2MClientProfileNew.getPostAttributeProfile();
            Set<String> attributeSetNew = this.convertJsonArrayToSet(attributeNew);
            JsonArray telemetryNew = lwM2MClientProfileNew.getPostTelemetryProfile();
            Set<String> telemetrySetNew = this.convertJsonArrayToSet(telemetryNew);
            JsonArray observeNew = lwM2MClientProfileNew.getPostObserveProfile();
            JsonObject keyNameNew = lwM2MClientProfileNew.getPostKeyNameProfile();

            // #3
            ResultsAnalyzerParameters sendAttrToThingsboard = new ResultsAnalyzerParameters();
            // #3.1
            if (!attributeOld.equals(attributeNew)) {
                ResultsAnalyzerParameters postAttributeAnalyzer = this.getAnalyzerParameters(new Gson().fromJson(attributeOld,
                        new TypeToken<Set<String>>() {}.getType()), attributeSetNew);
                sendAttrToThingsboard.getPathPostParametersAdd().addAll(postAttributeAnalyzer.getPathPostParametersAdd());
                sendAttrToThingsboard.getPathPostParametersDel().addAll(postAttributeAnalyzer.getPathPostParametersDel());
            }
            // #3.2
            if (!telemetryOld.equals(telemetryNew)) {
                ResultsAnalyzerParameters postTelemetryAnalyzer = this.getAnalyzerParameters(new Gson().fromJson(telemetryOld,
                        new TypeToken<Set<String>>() {}.getType()), telemetrySetNew);
                sendAttrToThingsboard.getPathPostParametersAdd().addAll(postTelemetryAnalyzer.getPathPostParametersAdd());
                sendAttrToThingsboard.getPathPostParametersDel().addAll(postTelemetryAnalyzer.getPathPostParametersDel());
            }
            // #3.3
            if (!keyNameOld.equals(keyNameNew)) {
                ResultsAnalyzerParameters keyNameChange = this.getAnalyzerKeyName(new Gson().fromJson(keyNameOld.toString(),
                        new TypeToken<ConcurrentHashMap<String, String>>() {}.getType()),
                        new Gson().fromJson(keyNameNew.toString(), new TypeToken<ConcurrentHashMap<String, String>>() {}.getType()));
                sendAttrToThingsboard.getPathPostParametersAdd().addAll(keyNameChange.getPathPostParametersAdd());
            }

            // #4.1 add
            if (sendAttrToThingsboard.getPathPostParametersAdd().size() > 0) {
                // update value in Resources
                registrationIds.forEach(registrationId -> {
                    Registration registration = lwM2mClientContext.getRegistration(registrationId);
                    this.readResourceValueObserve(registration, sendAttrToThingsboard.getPathPostParametersAdd(), GET_TYPE_OPER_READ);
                    // send attr/telemetry to tingsboard for new path
                    this.updateAttrTelemetry(registration, sendAttrToThingsboard.getPathPostParametersAdd());
                });
            }
            // #4.2 del
            if (sendAttrToThingsboard.getPathPostParametersDel().size() > 0) {
                ResultsAnalyzerParameters sendAttrToThingsboardDel = this.getAnalyzerParameters(sendAttrToThingsboard.getPathPostParametersAdd(), sendAttrToThingsboard.getPathPostParametersDel());
                sendAttrToThingsboard.setPathPostParametersDel(sendAttrToThingsboardDel.getPathPostParametersDel());
            }

            // #5.1
            if (!observeOld.equals(observeNew)) {
                Set<String> observeSetOld = new Gson().fromJson(observeOld, new TypeToken<Set<String>>() {
                }.getType());
                Set<String> observeSetNew = new Gson().fromJson(observeNew, new TypeToken<Set<String>>() {
                }.getType());
                //#5.2 add
                //  path Attr/Telemetry includes newObserve
                attributeSetOld.addAll(telemetrySetOld);
                ResultsAnalyzerParameters sendObserveToClientOld = this.getAnalyzerParametersIn(attributeSetOld, observeSetOld); // add observe
                attributeSetNew.addAll(telemetrySetNew);
                ResultsAnalyzerParameters sendObserveToClientNew = this.getAnalyzerParametersIn(attributeSetNew, observeSetNew); // add observe
                // does not include oldObserve
                ResultsAnalyzerParameters postObserveAnalyzer = this.getAnalyzerParameters(sendObserveToClientOld.getPathPostParametersAdd(), sendObserveToClientNew.getPathPostParametersAdd());
                //  send Request observe to Client
                registrationIds.forEach(registrationId -> {
                    Registration registration = lwM2mClientContext.getRegistration(registrationId);
                    this.readResourceValueObserve(registration, postObserveAnalyzer.getPathPostParametersAdd(), GET_TYPE_OPER_OBSERVE);
                    // 5.3 del
                    //  send Request cancel observe to Client
                    this.cancelObserveIsValue(registration, postObserveAnalyzer.getPathPostParametersDel());
                });
            }
        }
    }

    private Set<String> convertJsonArrayToSet(JsonArray jsonArray) {
        List<String> attributeListOld = new Gson().fromJson(jsonArray, new TypeToken<List<String>>() {
        }.getType());
        return Sets.newConcurrentHashSet(attributeListOld);
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

    private ResultsAnalyzerParameters getAnalyzerParametersIn(Set<String> parametersObserve, Set<String> parameters) {
        ResultsAnalyzerParameters analyzerParameters = new ResultsAnalyzerParameters();
        analyzerParameters.setPathPostParametersAdd(parametersObserve
                .stream().filter(parameters::contains).collect(Collectors.toSet()));
        return analyzerParameters;
    }

    /**
     * Update Resource value after change RezAttrTelemetry in config Profile
     * send response Read to Client and add path to pathResAttrTelemetry in LwM2MClient.getAttrTelemetryObserveValue()
     *
     * @param registration - Registration LwM2M Client
     * @param targets      - path Resources == [ "/2/0/0", "/2/0/1"]
     */
    private void readResourceValueObserve(Registration registration, Set<String> targets, String typeOper) {
        targets.forEach(target -> {
            LwM2mPath pathIds = new LwM2mPath(convertToObjectIdFromIdVer(target));
            if (pathIds.isResource()) {
                if (GET_TYPE_OPER_READ.equals(typeOper)) {
                    lwM2mTransportRequest.sendAllRequest(registration, target, typeOper,
                            ContentFormat.TLV.getName(), null, null, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getTimeout());
                } else if (GET_TYPE_OPER_OBSERVE.equals(typeOper)) {
                    lwM2mTransportRequest.sendAllRequest(registration, target, typeOper,
                            null, null, null, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getTimeout());
                }
            }
        });
    }

    private ResultsAnalyzerParameters getAnalyzerKeyName(ConcurrentHashMap<String, String> keyNameOld, ConcurrentHashMap<String, String> keyNameNew) {
        ResultsAnalyzerParameters analyzerParameters = new ResultsAnalyzerParameters();
        Set<String> paths = keyNameNew.entrySet()
                .stream()
                .filter(e -> !e.getValue().equals(keyNameOld.get(e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).keySet();
        analyzerParameters.setPathPostParametersAdd(paths);
        return analyzerParameters;
    }

    private void cancelObserveIsValue(Registration registration, Set<String> paramAnallyzer) {
        LwM2mClient lwM2MClient = lwM2mClientContext.getLwM2mClientWithReg(registration, null);
        paramAnallyzer.forEach(p -> {
                    if (this.returnResourceValueFromLwM2MClient(lwM2MClient, p) != null) {
                        this.setCancelObservationRecourse(registration, convertToObjectIdFromIdVer(p));
                    }
                }
        );
    }

    private void putDelayedUpdateResourcesClient(LwM2mClient lwM2MClient, Object valueOld, Object valueNew, String path) {
        if (valueNew != null && (valueOld == null || !valueNew.toString().equals(valueOld.toString()))) {
            lwM2mTransportRequest.sendAllRequest(lwM2MClient.getRegistration(), path, POST_TYPE_OPER_WRITE_REPLACE,
                    ContentFormat.TLV.getName(), null, valueNew, this.lwM2mTransportContextServer.getLwM2MTransportConfigServer().getTimeout());
        } else {
            log.error("05 delayError");
        }
    }

    /**
     * @param updateCredentials - Credentials include config only security Client (without config attr/telemetry...)
     *                          config attr/telemetry... in profile
     */
    public void onToTransportUpdateCredentials(TransportProtos.ToTransportUpdateCredentialsProto updateCredentials) {
        log.info("[{}] idList [{}] valueList updateCredentials", updateCredentials.getCredentialsIdList(), updateCredentials.getCredentialsValueList());
    }

    /**
     * Get path to resource from profile equal keyName or from ModelObject equal name
     * Only for resource:  isWritable && isPresent as attribute in profile -> LwM2MClientProfile (format: CamelCase)
     *
     * @param sessionInfo -
     * @param name        -
     * @return path if path isPresent in postProfile
     */
    private String getPathAttributeUpdate(TransportProtos.SessionInfoProto sessionInfo, String name) {
        String profilePath = this.getPathAttributeUpdateProfile(sessionInfo, name);
        return !profilePath.isEmpty() ? profilePath : null;
    }

    /**
     * Get path to resource from profile equal keyName
     *
     * @param sessionInfo -
     * @param name        -
     * @return -
     */
    private String getPathAttributeUpdateProfile(TransportProtos.SessionInfoProto sessionInfo, String name) {
        LwM2mClientProfile profile = lwM2mClientContext.getProfile(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
        LwM2mClient lwM2mClient = lwM2mClientContext.getLwM2MClient(sessionInfo);
        return profile.getPostKeyNameProfile().getAsJsonObject().entrySet().stream()
                .filter(e -> e.getValue().getAsString().equals(name) && validateResourceInModel(lwM2mClient, e.getKey(), false)).findFirst().map(Map.Entry::getKey)
                .orElse("");
    }

    /**
     * Update resource value on client: if there is a difference in values between the current resource values and the shared attribute values
     * #1 Get path resource by result attributesResponse
     * #1.1 If two names have equal path => last time attribute
     * #2.1 if there is a difference in values between the current resource values and the shared attribute values
     * => send to client Request Update of value (new value from shared attribute)
     * and LwM2MClient.delayedRequests.add(path)
     * #2.1 if there is not a difference in values between the current resource values and the shared attribute values
     *
     * @param attributesResponse -
     * @param sessionInfo        -
     */
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg attributesResponse, TransportProtos.SessionInfoProto sessionInfo) {
        try {
            LwM2mClient lwM2MClient = lwM2mClientContext.getLwM2MClient(sessionInfo);
            attributesResponse.getSharedAttributeListList().forEach(attr -> {
                String path = this.getPathAttributeUpdate(sessionInfo, attr.getKv().getKey());
                if (path != null) {
                    // #1.1
                    if (lwM2MClient.getDelayedRequests().containsKey(path) && attr.getTs() > lwM2MClient.getDelayedRequests().get(path).getTs()) {
                        lwM2MClient.getDelayedRequests().put(path, attr);
                    } else {
                        lwM2MClient.getDelayedRequests().put(path, attr);
                    }
                }
            });
            // #2.1
            lwM2MClient.getDelayedRequests().forEach((k, v) -> {
                ArrayList<TransportProtos.KeyValueProto> listV = new ArrayList<>();
                listV.add(v.getKv());
                this.putDelayedUpdateResourcesClient(lwM2MClient, this.getResourceValueToString(lwM2MClient, k), getJsonObject(listV).get(v.getKv().getKey()), k);
            });
        } catch (Exception e) {
            log.error(String.valueOf(e));
        }
    }

    /**
     * @param lwM2MClient -
     * @return SessionInfoProto -
     */
    private SessionInfoProto getNewSessionInfoProto(LwM2mClient lwM2MClient) {
        if (lwM2MClient != null) {
            TransportProtos.ValidateDeviceCredentialsResponseMsg msg = lwM2MClient.getCredentialsResponse();
            if (msg == null) {
                log.error("[{}] [{}]", lwM2MClient.getEndpoint(), CLIENT_NOT_AUTHORIZED);
                this.closeClientSession(lwM2MClient.getRegistration());
                return null;
            } else {
                return SessionInfoProto.newBuilder()
                        .setNodeId(this.lwM2mTransportContextServer.getNodeId())
                        .setSessionIdMSB(lwM2MClient.getSessionId().getMostSignificantBits())
                        .setSessionIdLSB(lwM2MClient.getSessionId().getLeastSignificantBits())
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
        return null;
    }

    /**
     * @param registration - Registration LwM2M Client
     * @return - sessionInfo after access connect client
     */
    private SessionInfoProto getValidateSessionInfo(Registration registration) {
        LwM2mClient lwM2MClient = lwM2mClientContext.getLwM2mClientWithReg(registration, null);
        return getNewSessionInfoProto(lwM2MClient);
    }

    /**
     * @param registrationId -
     * @return -
     */
    private SessionInfoProto getValidateSessionInfo(String registrationId) {
        LwM2mClient lwM2MClient = lwM2mClientContext.getLwM2mClientWithReg(null, registrationId);
        return getNewSessionInfoProto(lwM2MClient);
    }

    /**
     * if sessionInfo removed from sessions, then new registerAsyncSession
     *
     * @param sessionInfo -
     */
    private void checkInactivity(SessionInfoProto sessionInfo) {
        if (transportService.reportActivity(sessionInfo) == null) {
            transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(this, sessionInfo));
        }
    }

    private void checkInactivityAndReportActivity() {
        lwM2mClientContext.getLwM2mClients().forEach((key, value) -> this.checkInactivity(this.getValidateSessionInfo(key)));
    }

    /**
     * !!!  sharedAttr === profileAttr  !!!
     * If there is a difference in values between the current resource values and the shared attribute values
     * when the client connects to the server
     * #1 get attributes name from profile include name resources in ModelObject if resource  isWritable
     * #2.1 #1 size > 0 => send Request getAttributes to thingsboard
     *
     * @param lwM2MClient - LwM2M Client
     */
    public void putDelayedUpdateResourcesThingsboard(LwM2mClient lwM2MClient) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(lwM2MClient.getRegistration());
        if (sessionInfo != null) {
            //#1.1 + #1.2
            List<String> attrSharedNames = this.getNamesAttrFromProfileIsWritable(lwM2MClient);
            if (attrSharedNames.size() > 0) {
                //#2.1
                try {
                    TransportProtos.GetAttributeRequestMsg getAttributeMsg = lwM2mTransportContextServer.getAdaptor().convertToGetAttributes(null, attrSharedNames);
                    transportService.process(sessionInfo, getAttributeMsg, getAckCallback(lwM2MClient, getAttributeMsg.getRequestId(), DEVICE_ATTRIBUTES_REQUEST));
                } catch (AdaptorException e) {
                    log.warn("Failed to decode get attributes request", e);
                }
            }
        }
    }


    /**
     * !!!  sharedAttr === profileAttr  !!!
     * Get names or keyNames from profile:  resources IsWritable
     *
     * @param lwM2MClient -
     * @return ArrayList  keyNames from profile profileAttr && IsWritable
     */
    private List<String> getNamesAttrFromProfileIsWritable(LwM2mClient lwM2MClient) {
        LwM2mClientProfile profile = lwM2mClientContext.getProfile(lwM2MClient.getProfileId());
        Set<String> attrSet = new Gson().fromJson(profile.getPostAttributeProfile(),
                new TypeToken<HashSet<String>>() {
                }.getType());
        ConcurrentMap<String, String> keyNamesMap = new Gson().fromJson(profile.getPostKeyNameProfile().toString(),
                new TypeToken<ConcurrentHashMap<String, String>>() {
                }.getType());

        ConcurrentMap<String, String> keyNamesIsWritable = keyNamesMap.entrySet()
                .stream()
                .filter(e -> (attrSet.contains(e.getKey()) && validateResourceInModel(lwM2MClient, e.getKey(), true)))
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> namesIsWritable = ConcurrentHashMap.newKeySet();
        namesIsWritable.addAll(new HashSet<>(keyNamesIsWritable.values()));
        return new ArrayList<>(namesIsWritable);
    }

    private boolean validateResourceInModel(LwM2mClient lwM2mClient, String pathKey, boolean isWritable) {
        ResourceModel resourceModel = lwM2mClient.getResourceModel(pathKey);
        Integer objectId = validateObjectIdFromKey(pathKey);
        String objectVer = validateObjectVerFromKey(pathKey);
        return resourceModel != null && (isWritable ?
                objectId != null && objectVer != null && objectVer.equals(lwM2mClient.getRegistration().getSupportedVersion(objectId)) && resourceModel.operations.isWritable() :
                objectId != null && objectVer != null && objectVer.equals(lwM2mClient.getRegistration().getSupportedVersion(objectId)));
    }
}
