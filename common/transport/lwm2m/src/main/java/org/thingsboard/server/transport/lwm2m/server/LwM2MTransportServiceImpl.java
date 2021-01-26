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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.server.client.AttrTelemetryObserveValue;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClient;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceValue;
import org.thingsboard.server.transport.lwm2m.server.client.ResultsAnalyzerParameters;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.transport.util.JsonUtils.getJsonObject;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.CLIENT_NOT_AUTHORIZED;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.DEVICE_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.DEVICE_TELEMETRY_TOPIC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.GET_TYPE_OPER_OBSERVE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.GET_TYPE_OPER_READ;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.LOG_LW2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.LOG_LW2M_TELEMETRY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.POST_TYPE_OPER_EXECUTE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.POST_TYPE_OPER_WRITE_REPLACE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.SERVICE_CHANNEL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.getAckCallback;

@Slf4j
@Service("LwM2MTransportService")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServiceImpl implements LwM2MTransportService {

    private ExecutorService executorRegistered;
    private ExecutorService executorUpdateRegistered;
    private ExecutorService executorUnRegistered;
    private LwM2mValueConverterImpl converter;
    protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final Lock writeLock = readWriteLock.writeLock();


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
        this.context.getScheduler().scheduleAtFixedRate(this::checkInactivityAndReportActivity, new Random().nextInt((int) context.getCtxServer().getSessionReportTimeout()), context.getCtxServer().getSessionReportTimeout(), TimeUnit.MILLISECONDS);
        this.executorRegistered = Executors.newFixedThreadPool(this.context.getCtxServer().getRegisteredPoolSize(),
                new NamedThreadFactory(String.format("LwM2M %s channel registered", SERVICE_CHANNEL)));
        this.executorUpdateRegistered = Executors.newFixedThreadPool(this.context.getCtxServer().getUpdateRegisteredPoolSize(),
                new NamedThreadFactory(String.format("LwM2M %s channel update registered", SERVICE_CHANNEL)));
        this.executorUnRegistered = Executors.newFixedThreadPool(this.context.getCtxServer().getUnRegisteredPoolSize(),
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
     * @param lwServer             - LeshanServer
     * @param registration         - Registration LwM2M Client
     * @param previousObsersations - may be null
     */
    public void onRegistered(LeshanServer lwServer, Registration registration, Collection<Observation> previousObsersations) {
        executorRegistered.submit(() -> {
            try {
//                log.warn("[{}] [{{}] Client: create after Registration", registration.getEndpoint(), registration.getId());
                LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.updateInSessionsLwM2MClient(lwServer, registration);
                if (lwM2MClient != null) {
                    lwM2MClient.setLwM2MTransportServiceImpl(this);
                    lwM2MClient.setSessionUuid(UUID.randomUUID());
                    this.sentLogsToThingsboard(LOG_LW2M_INFO + ": Client  Registered", registration);
                    this.setLwM2mFromClientValue(lwServer, registration, lwM2MClient);
                    SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration);
                    if (sessionInfo != null) {
                        lwM2MClient.setDeviceUuid(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
                        lwM2MClient.setProfileUuid(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
                        lwM2MClient.setDeviceName(sessionInfo.getDeviceName());
                        lwM2MClient.setDeviceProfileName(sessionInfo.getDeviceType());
                        transportService.registerAsyncSession(sessionInfo, new LwM2MSessionMsgListener(this, sessionInfo));
                        transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(SessionEvent.OPEN), null);
                        transportService.process(sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().build(), null);
                        this.sentLogsToThingsboard(LOG_LW2M_INFO + ": Client  create after Registration", registration);
                        this.putDelayedUpdateResourcesThingsboard(lwM2MClient);
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
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     */
    public void updatedReg(LeshanServer lwServer, Registration registration) {
        executorUpdateRegistered.submit(() -> {
            try {
                SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration);
                if (sessionInfo != null) {
                    this.checkInactivity(sessionInfo);
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
    public void unReg(LeshanServer lwServer, Registration registration, Collection<Observation> observations) {
        executorUnRegistered.submit(() -> {
            try {
                this.setCancelObservations(lwServer, registration);
                this.sentLogsToThingsboard(LOG_LW2M_INFO + ": Client unRegistration", registration);
                this.closeClientSession(registration);
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable un registration.", registration.getEndpoint(), t);
            }
        });
    }

    private void closeClientSession(Registration registration) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration);
        if (sessionInfo != null) {
            transportService.deregisterSession(sessionInfo);
            this.doCloseSession(sessionInfo);
            lwM2mInMemorySecurityStore.delRemoveSessionAndListener(registration.getId());
            if (lwM2mInMemorySecurityStore.getProfiles().size() > 0) {
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
        Map<UUID, AttrTelemetryObserveValue> profilesClone = lwM2mInMemorySecurityStore.getProfiles().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
     * #0 Add new ObjectModel to context
     * Create new LwM2MClient for current session -> setModelClient...
     * #1   Add all ObjectLinks (instance) to control the process of executing requests to the client
     * to get the client model with current values
     * #2   Get the client model with current values. Analyze the response in -> lwM2MTransportRequest.sendResponse
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     * @param lwM2MClient  - object with All parameters off client
     */
    private void setLwM2mFromClientValue(LeshanServer lwServer, Registration registration, LwM2MClient lwM2MClient) {
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            LwM2mPath pathIds = new LwM2mPath(url.getUrl());
            if (pathIds.isObjectInstance() && !pathIds.isResource()) {
                // #1
                lwM2MClient.getPendingRequests().add(url.getUrl());
                // #2
                lwM2MTransportRequest.sendAllRequest(lwServer, registration, url.getUrl(), GET_TYPE_OPER_READ, ContentFormat.TLV.getName(),
                        lwM2MClient, null, null, this.context.getCtxServer().getTimeout(), false);
            }
        });

        // #1
        for (Link url : registration.getObjectLinks()) {
            LwM2mPath pathIds = new LwM2mPath(url.getUrl());
            if (pathIds.isObjectInstance() && !pathIds.isResource()) {
                lwM2MClient.getPendingRequests().add(url.getUrl());
            }
        }
        // #2
        for (Link url : registration.getObjectLinks()) {
            LwM2mPath pathIds = new LwM2mPath(url.getUrl());
            if (pathIds.isObjectInstance() && !pathIds.isResource()) {
                lwM2MTransportRequest.sendAllRequest(lwServer, registration, url.getUrl(), GET_TYPE_OPER_READ, ContentFormat.TLV.getName(),
                        lwM2MClient, null, null, this.context.getCtxServer().getTimeout(), false);
            }
        }

        // #1
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            LwM2mPath pathIds = new LwM2mPath(url.getUrl());
            if (pathIds.isObjectInstance() && !pathIds.isResource()) {
                lwM2MClient.getPendingRequests().add(url.getUrl());
            }
        });

        // #2
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            LwM2mPath pathIds = new LwM2mPath(url.getUrl());
            if (pathIds.isObjectInstance() && !pathIds.isResource()) {
                lwM2MTransportRequest.sendAllRequest(lwServer, registration, url.getUrl(), GET_TYPE_OPER_READ, ContentFormat.TLV.getName(),
                        lwM2MClient, null, null, this.context.getCtxServer().getTimeout(), false);
            }
        });
    }

    /**
     * @param registration - Registration LwM2M Client
     * @return - sessionInfo after access connect client
     */
    private SessionInfoProto getValidateSessionInfo(Registration registration) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getLwM2MClientWithReg(registration, null);
        return getNewSessionInfoProto(lwM2MClient);

    }

    /**
     * @param registrationId -
     * @return -
     */
    private SessionInfoProto getValidateSessionInfo(String registrationId) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getLwM2MClientWithReg(null, registrationId);
        return getNewSessionInfoProto(lwM2MClient);
    }

    private SessionInfoProto getNewSessionInfoProto(LwM2MClient lwM2MClient) {
        if (lwM2MClient != null) {
            ValidateDeviceCredentialsResponseMsg msg = lwM2MClient.getCredentialsResponse();
            if (msg == null || msg.getDeviceInfo() == null) {
                log.error("[{}] [{}]", lwM2MClient.getEndPoint(), CLIENT_NOT_AUTHORIZED);
                this.closeClientSession(lwM2MClient.getRegistration());
                return null;
            } else {
                return SessionInfoProto.newBuilder()
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
        return null;
    }

    /**
     * Add attribute/telemetry information from Client and credentials/Profile to client model and start observe
     * !!! if the resource has an observation, but no telemetry or attribute - the observation will not use
     * #1 Sending Attribute Telemetry with value to thingsboard only once at the start of the connection
     * #2 Start observe
     *
     * @param lwM2MClient - LwM2M Client
     */

    public void updatesAndSentModelParameter(LwM2MClient lwM2MClient) {
        // #1
        this.updateAttrTelemetry(lwM2MClient.getRegistration(), true, null);
        // #2
        this.onSentObserveToClient(lwM2MClient.getLwServer(), lwM2MClient.getRegistration());

    }

    /**
     * If there is a difference in values between the current resource values and the shared attribute values
     * when the client connects to the server
     * #1 get attributes name from profile include name resources in ModelObject if resource  isWritable
     * #2.1 #1 size > 0 => send Request getAttributes to thingsboard
     * #2.2 #1 size == 0 => continue normal process
     *
     * @param lwM2MClient - LwM2M Client
     */
    public void putDelayedUpdateResourcesThingsboard(LwM2MClient lwM2MClient) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(lwM2MClient.getRegistration());
        if (sessionInfo != null) {
            //#1.1 + #1.2
            List<String> attrSharedNames = this.getNamesAttrFromProfileIsWritable(lwM2MClient);
            if (attrSharedNames.size() > 0) {
                //#2.1
                try {
                    TransportProtos.GetAttributeRequestMsg getAttributeMsg = context.getAdaptor().convertToGetAttributes(null, attrSharedNames);
                    lwM2MClient.getDelayedRequestsId().add(getAttributeMsg.getRequestId());
                    transportService.process(sessionInfo, getAttributeMsg, getAckCallback(lwM2MClient, getAttributeMsg.getRequestId(), DEVICE_ATTRIBUTES_REQUEST));
                } catch (AdaptorException e) {
                    log.warn("Failed to decode get attributes request", e);
                }
            }
            // #2.2
            else {
                lwM2MClient.onSuccessOrErrorDelayedRequests(null);
            }
        }
    }

    /**
     * Update resource value on client: if there is a difference in values between the current resource values and the shared attribute values
     * #1 Get path resource by result attributesResponse
     * #1.1 If two names have equal path => last time attribute
     * #2.1 if there is a difference in values between the current resource values and the shared attribute values
     * => sent to client Request Update of value (new value from shared attribute)
     * and LwM2MClient.delayedRequests.add(path)
     * #2.1 if there is not a difference in values between the current resource values and the shared attribute values
     *
     * @param attributesResponse -
     * @param sessionInfo        -
     */
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg attributesResponse, TransportProtos.SessionInfoProto sessionInfo) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getLwM2MClient(sessionInfo);
        if (lwM2MClient.getDelayedRequestsId().contains(attributesResponse.getRequestId())) {
            attributesResponse.getSharedAttributeListList().forEach(attr -> {
                String path = this.getPathAttributeUpdate(sessionInfo, attr.getKv().getKey());
                // #1.1
                if (lwM2MClient.getDelayedRequests().containsKey(path) && attr.getTs() > lwM2MClient.getDelayedRequests().get(path).getTs()) {
                    lwM2MClient.getDelayedRequests().put(path, attr);
                } else {
                    lwM2MClient.getDelayedRequests().put(path, attr);
                }
            });
            // #2.1
            lwM2MClient.getDelayedRequests().forEach((k, v) -> {
                ArrayList<TransportProtos.KeyValueProto> listV = new ArrayList<>();
                listV.add(v.getKv());
                this.putDelayedUpdateResourcesClient(lwM2MClient, this.getResourceValueToString(lwM2MClient, k), getJsonObject(listV).get(v.getKv().getKey()), k);
            });
            lwM2MClient.getDelayedRequestsId().remove(attributesResponse.getRequestId());
            if (lwM2MClient.getDelayedRequests().size() == 0) {
                lwM2MClient.onSuccessOrErrorDelayedRequests(null);
            }
        }
    }

    private void putDelayedUpdateResourcesClient(LwM2MClient lwM2MClient, Object valueOld, Object valueNew, String path) {
        if (valueNew != null && !valueNew.toString().equals(valueOld.toString())) {
            lwM2MTransportRequest.sendAllRequest(lwM2MClient.getLwServer(), lwM2MClient.getRegistration(), path, POST_TYPE_OPER_WRITE_REPLACE,
                    ContentFormat.TLV.getName(), lwM2MClient, null, valueNew, this.context.getCtxServer().getTimeout(),
                    true);
        }
    }

    /**
     * Get names and keyNames from profile shared!!!! attr resources IsWritable
     *
     * @param lwM2MClient -
     * @return ArrayList  keyNames from profile attr resources shared!!!! && IsWritable
     */
    private List<String> getNamesAttrFromProfileIsWritable(LwM2MClient lwM2MClient) {
        AttrTelemetryObserveValue profile = lwM2mInMemorySecurityStore.getProfile(lwM2MClient.getProfileUuid());
        Set attrSet = new Gson().fromJson(profile.getPostAttributeProfile(), Set.class);
        ConcurrentMap<String, String> keyNamesMap = new Gson().fromJson(profile.getPostKeyNameProfile().toString(), ConcurrentHashMap.class);

        ConcurrentMap<String, String> keyNamesIsWritable = keyNamesMap.entrySet()
                .stream()
                .filter(e -> (attrSet.contains(e.getKey()) && context.getCtxServer().getResourceModel(lwM2MClient.getRegistration(), new LwM2mPath(e.getKey())) != null &&
                        context.getCtxServer().getResourceModel(lwM2MClient.getRegistration(), new LwM2mPath(e.getKey())).operations.isWritable()))
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> namesIsWritable = ConcurrentHashMap.newKeySet();
        namesIsWritable.addAll(new HashSet<>(keyNamesIsWritable.values()));
        return new ArrayList<>(namesIsWritable);
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
        JsonObject telemetries = new JsonObject();
        if (start) {
            // #1.1
            JsonObject attributeClient = this.getAttributeClient(registration);
            if (attributeClient != null) {
                attributeClient.entrySet().forEach(p -> attributes.add(p.getKey(), p.getValue()));
            }
        }
        // #1.2
        try {
            writeLock.lock();
            this.getParametersFromProfile(attributes, telemetries, registration, paths);
        } catch (Exception e) {
            log.error("UpdateAttrTelemetry", e);
        } finally {
            writeLock.unlock();
        }
        if (attributes.getAsJsonObject().entrySet().size() > 0)
            this.updateParametersOnThingsboard(attributes, DEVICE_ATTRIBUTES_TOPIC, registration);
        if (telemetries.getAsJsonObject().entrySet().size() > 0)
            this.updateParametersOnThingsboard(telemetries, DEVICE_TELEMETRY_TOPIC, registration);
    }

    /**
     * get AttrName/TelemetryName with value from Client
     *
     * @param registration -
     * @return - JsonObject, format: {name: value}}
     */
    private JsonObject getAttributeClient(Registration registration) {
        if (registration.getAdditionalRegistrationAttributes().size() > 0) {
            JsonObject resNameValues = new JsonObject();
            registration.getAdditionalRegistrationAttributes().forEach(resNameValues::addProperty);
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
            LwM2mPath pathIds = new LwM2mPath(p.getAsString().toString());
            if (pathIds.isResource()) {
                if (path == null || path.contains(p.getAsString())) {
                    this.addParameters(p.getAsString().toString(), attributes, registration);
                }
            }
        });
        attrTelemetryObserveValue.getPostTelemetryProfile().forEach(p -> {
            LwM2mPath pathIds = new LwM2mPath(p.getAsString().toString());
            if (pathIds.isResource()) {
                if (path == null || path.contains(p.getAsString())) {
                    this.addParameters(p.getAsString().toString(), telemetry, registration);
                }
            }
        });
    }

    /**
     * @param parameters   - JsonObject attributes/telemetry
     * @param registration - Registration LwM2M Client
     */
    private void addParameters(String path, JsonObject parameters, Registration registration) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getSessions().get(registration.getId());
        JsonObject names = lwM2mInMemorySecurityStore.getProfiles().get(lwM2MClient.getProfileUuid()).getPostKeyNameProfile();
        String resName = String.valueOf(names.get(path));
        if (resName != null && !resName.isEmpty()) {
            try {
                String resValue = this.getResourceValueToString(lwM2MClient, path);
                if (resValue != null) {
                    parameters.addProperty(resName, resValue);
                }
            } catch (Exception e) {
                log.error(e.getStackTrace().toString());
            }
        }
    }

    /**
     * Prepare Sent to Thigsboard callback - Attribute or Telemetry
     *
     * @param msg          - JsonArray: [{name: value}]
     * @param topicName    - Api Attribute or Telemetry
     * @param registration - Id of Registration LwM2M Client
     */
    public void updateParametersOnThingsboard(JsonElement msg, String topicName, Registration registration) {
        SessionInfoProto sessionInfo = this.getValidateSessionInfo(registration);
        if (sessionInfo != null) {
            context.sentParametersOnThingsboard(msg, topicName, sessionInfo);
        } else {
            log.error("Client: [{}] updateParametersOnThingsboard [{}] sessionInfo ", registration, null);
        }
    }

    /**
     * Start observe
     * #1 - Analyze:
     * #1.1 path in observe == (attribute or telemetry)
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
                // #2
                if (this.getResourceValueToString(lwM2mInMemorySecurityStore.getSessions().get(registration.getId()), target) != null) {
                    lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, GET_TYPE_OPER_OBSERVE,
                            null, null, null, null, this.context.getCtxServer().getTimeout(),
                            false);
                }
            }
        });
    }

    public void setCancelObservations(LeshanServer lwServer, Registration registration) {
        if (registration != null) {
            Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
            observations.forEach(observation -> this.setCancelObservationRecourse(lwServer, registration, observation.getPath().toString()));
        }
    }

    /**
     * lwM2MTransportRequest.sendAllRequest(lwServer, registration, path, POST_TYPE_OPER_OBSERVE_CANCEL, null, null, null, null, context.getTimeout());
     * At server side this will not remove the observation from the observation store, to do it you need to use
     * {@code ObservationService#cancelObservation()}
     */
    public void setCancelObservationRecourse(LeshanServer lwServer, Registration registration, String path) {
        lwServer.getObservationService().cancelObservations(registration, path);
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

    public void onObservationResponse(Registration registration, String path, ReadResponse response) {
        if (response.getContent() != null) {
            if (response.getContent() instanceof LwM2mObject) {
//                LwM2mObject content = (LwM2mObject) response.getContent();
            } else if (response.getContent() instanceof LwM2mObjectInstance) {
//                LwM2mObjectInstance content = (LwM2mObjectInstance) response.getContent();
            } else if (response.getContent() instanceof LwM2mSingleResource) {
                LwM2mSingleResource content = (LwM2mSingleResource) response.getContent();
                this.onObservationSetResourcesValue(registration, content.getValue(), null, path);
            } else if (response.getContent() instanceof LwM2mMultipleResource) {
                LwM2mMultipleResource content = (LwM2mMultipleResource) response.getContent();
                this.onObservationSetResourcesValue(registration, null, content.getValues(), path);
            }
        }
    }

    /**
     * Sending observe value of resources to thingsboard
     * #1 Return old Value Resource from LwM2MClient
     * #2 Update new Resources (replace old Resource Value on new Resource Value)
     *
     * @param registration - Registration LwM2M Client
     * @param value        - LwM2mSingleResource response.getContent()
     * @param values       - LwM2mSingleResource response.getContent()
     * @param path         - resource
     */
    private void onObservationSetResourcesValue(Registration registration, Object value, Map<Integer, ?>  values, String path) {
        boolean isChange = false;
        try {
            writeLock.lock();
            // #1
            LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getLwM2MClientWithReg(registration, null);
            LwM2mPath pathIds = new LwM2mPath(path);
//            log.warn("#0 nameDevice: [{}] resultIds: [{}] value: [{}], values: [{}] ", lwM2MClient.getDeviceName(), pathIds, value, values);
            ResourceModel.Type resModelType = context.getCtxServer().getResourceModelType(registration, pathIds);
            ResourceValue resValueOld = lwM2MClient.getResources().get(path);
            // #2
            if (resValueOld.isMultiInstances() && !values.toString().equals(resValueOld.getResourceValue().toString())) {
                lwM2MClient.getResources().get(path).setValues(values);
//                ResourceValue resourceValue = new ResourceValue(values, null, true);
//                lwM2MClient.getResources().put(path, resourceValue);
                isChange = true;
            } else if (!LwM2MTransportHandler.equalsResourceValue(resValueOld.getValue(), value, resModelType, pathIds)) {
                lwM2MClient.getResources().get(path).setValue(value);
//                ResourceValue resourceValueOld = lwM2MClient.getResources().get(path);
//                lwM2MClient.getResources().remove(resourceValueOld);
//                ResourceValue resourceValue = new ResourceValue(null, value, false);
//                lwM2MClient.getResources().put(path, resourceValue);
                log.warn("upDateResize: [{}] [{}] [{}] [{}]", lwM2MClient.getEndPoint(), lwM2MClient.getResources().size(), value, path);
                isChange = true;
            }
        } catch (Exception e) {
            log.error("#1_1 Update ResourcesValue after Observation is unsuccessfully  path: [{}] value: [{}] [{}]", path, value, e.toString());
        } finally {
            writeLock.unlock();
        }

        if (isChange) {
            Set<String> paths = new HashSet<>();
            paths.add(path);
            this.updateAttrTelemetry(registration, false, paths);
        }
    }

    /**
     * @param updateCredentials - Credentials include config only security Client (without config attr/telemetry...)
     *                          config attr/telemetry... in profile
     */
    public void onToTransportUpdateCredentials(ToTransportUpdateCredentialsProto updateCredentials) {
        log.info("[{}] idList [{}] valueList updateCredentials", updateCredentials.getCredentialsIdList(), updateCredentials.getCredentialsValueList());
    }

    /**
     * Update - sent request in change value resources in Client
     * Path to resources from profile equal keyName or from ModelObject equal name
     * Only for resources:  isWritable && isPresent as attribute in profile -> AttrTelemetryObserveValue (format: CamelCase)
     * Delete - nothing     *
     *
     * @param msg -
     */
    public void onAttributeUpdate(TransportProtos.AttributeUpdateNotificationMsg msg, TransportProtos.SessionInfoProto sessionInfo) {
        if (msg.getSharedUpdatedCount() > 0) {
            JsonElement el = JsonConverter.toJson(msg);
            el.getAsJsonObject().entrySet().forEach(de -> {
                String path = this.getPathAttributeUpdate(sessionInfo, de.getKey());
                String value = de.getValue().getAsString();
                LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getSession(new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB())).entrySet().iterator().next().getValue();
                AttrTelemetryObserveValue profile = lwM2mInMemorySecurityStore.getProfile(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
                ResourceModel resourceModel = context.getCtxServer().getResourceModel(lwM2MClient.getRegistration(), new LwM2mPath(path));
                if (!path.isEmpty() && (this.validatePathInAttrProfile(profile, path) || this.validatePathInTelemetryProfile(profile, path))) {
                    if (resourceModel != null && resourceModel.operations.isWritable()) {
                        lwM2MTransportRequest.sendAllRequest(lwM2MClient.getLwServer(), lwM2MClient.getRegistration(), path, POST_TYPE_OPER_WRITE_REPLACE,
                                ContentFormat.TLV.getName(), lwM2MClient, null, value, this.context.getCtxServer().getTimeout(),
                                false);
                    } else {
                        log.error("Resource path - [{}] value - [{}] is not Writable and cannot be updated", path, value);
                        String logMsg = String.format(LOG_LW2M_ERROR + ": attributeUpdate: Resource path - %s value - %s is not Writable and cannot be updated", path, value);
                        this.sentLogsToThingsboard(logMsg, lwM2MClient.getRegistration());
                    }
                } else {
                    log.error("Attribute name - [{}] value - [{}] is not present as attribute in profile and cannot be updated", de.getKey(), value);
                    String logMsg = String.format(LOG_LW2M_ERROR + ": attributeUpdate: attribute name - %s value - %s is not present as attribute in profile and cannot be updated", de.getKey(), value);
                    this.sentLogsToThingsboard(logMsg, lwM2MClient.getRegistration());
                }
            });
        } else if (msg.getSharedDeletedCount() > 0) {
            log.info("[{}] delete [{}]  onAttributeUpdate", msg.getSharedDeletedList(), sessionInfo);
        }
    }

    /**
     * Get path to resource from profile equal keyName or from ModelObject equal name
     * Only for resource:  isWritable && isPresent as attribute in profile -> AttrTelemetryObserveValue (format: CamelCase)
     *
     * @param sessionInfo -
     * @param name        -
     * @return path if path isPresent in postProfile
     */
    private String getPathAttributeUpdate(TransportProtos.SessionInfoProto sessionInfo, String name) {
        String profilePath = this.getPathAttributeUpdateProfile(sessionInfo, name);
//        return !profilePath.isEmpty() ? profilePath : this.getPathAttributeUpdateModelObject(name);
        return !profilePath.isEmpty() ? profilePath : null;
    }

    /**
     * @param profile -
     * @param path    -
     * @return true if path isPresent in postAttributeProfile
     */
    private boolean validatePathInAttrProfile(AttrTelemetryObserveValue profile, String path) {
        Set<String> attributesSet = new Gson().fromJson(profile.getPostAttributeProfile(), Set.class);
        return attributesSet.stream().filter(p -> p.equals(path)).findFirst().isPresent();
    }

    /**
     * @param profile -
     * @param path    -
     * @return true if path isPresent in postAttributeProfile
     */
    private boolean validatePathInTelemetryProfile(AttrTelemetryObserveValue profile, String path) {
        Set<String> telemetriesSet = new Gson().fromJson(profile.getPostTelemetryProfile(), Set.class);
        return telemetriesSet.stream().filter(p -> p.equals(path)).findFirst().isPresent();
    }


    /**
     * Get path to resource from profile equal keyName
     *
     * @param sessionInfo -
     * @param name        -
     * @return -
     */
    private String getPathAttributeUpdateProfile(TransportProtos.SessionInfoProto sessionInfo, String name) {
        AttrTelemetryObserveValue profile = lwM2mInMemorySecurityStore.getProfile(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
        return profile.getPostKeyNameProfile().getAsJsonObject().entrySet().stream()
                .filter(e -> e.getValue().getAsString().equals(name)).findFirst().map(Map.Entry::getKey)
                .orElse("");
    }

    /**
     * Update resource (attribute) value  on thingsboard after update value in client
     *
     * @param registration -
     * @param path         -
     * @param request      -
     */
    public void onAttributeUpdateOk(Registration registration, String path, WriteRequest request, boolean isDelayedUpdate) {
        ResourceModel resource = context.getCtxServer().getResourceModel(registration, new LwM2mPath(path));
        if (resource.multiple) {
            this.onObservationSetResourcesValue(registration, null, ((LwM2mSingleResource) request.getNode()).getValues(), path);
        } else {
            this.onObservationSetResourcesValue(registration, ((LwM2mSingleResource) request.getNode()).getValue(), null, path);
        }
        if (isDelayedUpdate) lwM2mInMemorySecurityStore.getLwM2MClientWithReg(registration, null)
                .onSuccessOrErrorDelayedRequests(request.getPath().toString());
    }

    /**
     * @param sessionInfo   -
     * @param deviceProfile -
     */
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        Set<String> registrationIds = lwM2mInMemorySecurityStore.getSessions().entrySet()
                .stream()
                .filter(e -> e.getValue().getProfileUuid().equals(deviceProfile.getUuidId()))
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
     *
     * @param registrationId -
     * @param device         -
     */
    private void onDeviceUpdateLwM2MClient(String registrationId, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getSessions().get(registrationId);
        lwM2MClient.setDeviceName(device.getName());
        if (!lwM2MClient.getProfileUuid().equals(device.getDeviceProfileId().getId())) {
            Set<String> registrationIds = new HashSet<>();
            registrationIds.add(registrationId);
            deviceProfileOpt.ifPresent(deviceProfile -> this.onDeviceUpdateChangeProfile(registrationIds, deviceProfile));
        }

        lwM2MClient.setProfileUuid(device.getDeviceProfileId().getId());
    }

    /**
     * #1 Read new, old Value (Attribute, Telemetry, Observe, KeyName)
     * #2 Update in lwM2MClient: ...Profile if changes from update device
     * #3 Equivalence test: old <> new Value (Attribute, Telemetry, Observe, KeyName)
     * #3.1 Attribute isChange (add&del)
     * #3.2 Telemetry isChange (add&del)
     * #3.3 KeyName isChange (add)
     * #4 update
     * #4.1 add If #3 isChange, then analyze and update Value in Transport form Client and sent Value to thingsboard
     * #4.2 del
     * -- if  add attributes includes del telemetry - result del for observe
     * #5
     * #5.1 Observe isChange (add&del)
     * #5.2 Observe.add
     * -- path Attr/Telemetry includes newObserve and does not include oldObserve: sent Request observe to Client
     * #5.3 Observe.del
     * -- different between newObserve and oldObserve: sent Request cancel observe to client
     *
     * @param registrationIds -
     * @param deviceProfile   -
     */
    private void onDeviceUpdateChangeProfile(Set<String> registrationIds, DeviceProfile deviceProfile) {

        AttrTelemetryObserveValue attrTelemetryObserveValueOld = lwM2mInMemorySecurityStore.getProfiles().get(deviceProfile.getUuidId());
        if (lwM2mInMemorySecurityStore.addUpdateProfileParameters(deviceProfile)) {

            // #1
            JsonArray attributeOld = attrTelemetryObserveValueOld.getPostAttributeProfile();
            Set attributeSetOld = new Gson().fromJson(attributeOld, Set.class);
            JsonArray telemetryOld = attrTelemetryObserveValueOld.getPostTelemetryProfile();
            Set telemetrySetOld = new Gson().fromJson(telemetryOld, Set.class);
            JsonArray observeOld = attrTelemetryObserveValueOld.getPostObserveProfile();
            JsonObject keyNameOld = attrTelemetryObserveValueOld.getPostKeyNameProfile();

            AttrTelemetryObserveValue attrTelemetryObserveValueNew = lwM2mInMemorySecurityStore.getProfiles().get(deviceProfile.getUuidId());
            JsonArray attributeNew = attrTelemetryObserveValueNew.getPostAttributeProfile();
            Set attributeSetNew = new Gson().fromJson(attributeNew, Set.class);
            JsonArray telemetryNew = attrTelemetryObserveValueNew.getPostTelemetryProfile();
            Set telemetrySetNew = new Gson().fromJson(telemetryNew, Set.class);
            JsonArray observeNew = attrTelemetryObserveValueNew.getPostObserveProfile();
            JsonObject keyNameNew = attrTelemetryObserveValueNew.getPostKeyNameProfile();

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
                registrationIds.forEach(registrationId -> {
                    LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getLwM2MClientWithReg(null, registrationId);
                    LeshanServer lwServer = lwM2MClient.getLwServer();
                    Registration registration = lwM2mInMemorySecurityStore.getByRegistration(registrationId);
                    log.warn("[{}] # 4.1", registration.getEndpoint());
                    this.updateResourceValueObserve(lwServer, registration, sentAttrToThingsboard.getPathPostParametersAdd(), GET_TYPE_OPER_READ);
                    // sent attr/telemetry to tingsboard for new path
                    this.updateAttrTelemetry(registration, false, sentAttrToThingsboard.getPathPostParametersAdd());
                });
            }
            // #4.2 del
            if (sentAttrToThingsboard.getPathPostParametersDel().size() > 0) {
                ResultsAnalyzerParameters sentAttrToThingsboardDel = this.getAnalyzerParameters(sentAttrToThingsboard.getPathPostParametersAdd(), sentAttrToThingsboard.getPathPostParametersDel());
                sentAttrToThingsboard.setPathPostParametersDel(sentAttrToThingsboardDel.getPathPostParametersDel());
            }

            // #5.1
            if (!observeOld.equals(observeNew)) {
                Set observeSetOld = new Gson().fromJson(observeOld, Set.class);
                Set observeSetNew = new Gson().fromJson(observeNew, Set.class);
                //#5.2 add
                //  path Attr/Telemetry includes newObserve
                attributeSetOld.addAll(telemetrySetOld);
                ResultsAnalyzerParameters sentObserveToClientOld = this.getAnalyzerParametersIn(attributeSetOld, observeSetOld); // add observe
                attributeSetNew.addAll(telemetrySetNew);
                ResultsAnalyzerParameters sentObserveToClientNew = this.getAnalyzerParametersIn(attributeSetNew, observeSetNew); // add observe
                // does not include oldObserve
                ResultsAnalyzerParameters postObserveAnalyzer = this.getAnalyzerParameters(sentObserveToClientOld.getPathPostParametersAdd(), sentObserveToClientNew.getPathPostParametersAdd());
                //  sent Request observe to Client
                registrationIds.forEach(registrationId -> {
                    LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getLwM2MClient(null, registrationId);
                    LeshanServer lwServer = lwM2MClient.getLwServer();
                    Registration registration = lwM2mInMemorySecurityStore.getByRegistration(registrationId);
                    log.warn("[{}] # 5.1", registration.getEndpoint());
                    this.updateResourceValueObserve(lwServer, registration, postObserveAnalyzer.getPathPostParametersAdd(), GET_TYPE_OPER_OBSERVE);
                    // 5.3 del
                    //  sent Request cancel observe to Client
                    this.cancelObserveIsValue(lwServer, registration, postObserveAnalyzer.getPathPostParametersDel());
                });
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
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).keySet();
        analyzerParameters.setPathPostParametersAdd(paths);
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
     * sent response Read to Client and add path to pathResAttrTelemetry in LwM2MClient.getAttrTelemetryObserveValue()
     *
     * @param lwServer     - LeshanServer
     * @param registration - Registration LwM2M Client
     * @param targets      - path Resources == [ "/2/0/0", "/2/0/1"]
     */
    private void updateResourceValueObserve(LeshanServer lwServer, Registration registration, Set<String> targets, String typeOper) {
        targets.forEach(target -> {
            LwM2mPath pathIds = new LwM2mPath(target);
            if (pathIds.isResource()) {
                if (GET_TYPE_OPER_READ.equals(typeOper)) {
                    lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, typeOper,
                            ContentFormat.TLV.getName(), null, null, null, this.context.getCtxServer().getTimeout(),
                            false);
                } else if (GET_TYPE_OPER_OBSERVE.equals(typeOper)) {
                    lwM2MTransportRequest.sendAllRequest(lwServer, registration, target, typeOper,
                            null, null, null, null, this.context.getCtxServer().getTimeout(),
                            false);
                }
            }
        });
    }

    private void cancelObserveIsValue(LeshanServer lwServer, Registration registration, Set<String> paramAnallyzer) {
        LwM2MClient lwM2MClient = lwM2mInMemorySecurityStore.getLwM2MClientWithReg(registration, null);
        paramAnallyzer.forEach(p -> {
                    if (this.getResourceValue(lwM2MClient, new LwM2mPath(p)) != null) {
                        this.setCancelObservationRecourse(lwServer, registration, p);
                    }
                }
        );
    }

    private ResourceValue getResourceValue(LwM2MClient lwM2MClient, LwM2mPath pathIds) {
        ResourceValue resourceValue = null;
        if (pathIds.isResource()) {
            resourceValue = lwM2MClient.getResources().get(pathIds.toString());
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
                ContentFormat.TLV.getName(), null, null, null, this.context.getCtxServer().getTimeout(),
                false);
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
    public void doDisconnect(SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(SessionEvent.CLOSED), null);
        transportService.deregisterSession(sessionInfo);
    }

    private void checkInactivityAndReportActivity() {
        lwM2mInMemorySecurityStore.getSessions().forEach((key, value) -> this.checkInactivity(this.getValidateSessionInfo(key)));
    }

    /**
     * if sessionInfo removed from sessions, then new registerAsyncSession
     *
     * @param sessionInfo -
     */
    private void checkInactivity(SessionInfoProto sessionInfo) {
        if (transportService.reportActivity(sessionInfo) == null) {
            transportService.registerAsyncSession(sessionInfo, new LwM2MSessionMsgListener(this, sessionInfo));
        }
    }

    public void sentLogsToThingsboard(String msg, Registration registration) {
        if (msg != null) {
            JsonObject telemetries = new JsonObject();
            telemetries.addProperty(LOG_LW2M_TELEMETRY, msg);
            this.updateParametersOnThingsboard(telemetries, LwM2MTransportHandler.DEVICE_TELEMETRY_TOPIC, registration);
        }
    }

    /**
     * @param path - path resource
     * @return - value of Resource or null
     */
    private String getResourceValueToString(LwM2MClient lwM2MClient, String path) {
        LwM2mPath pathIds = new LwM2mPath(path);
        ResourceValue resourceValue = this.getResourceValue(lwM2MClient, pathIds);
        return (resourceValue == null) ? null :
                (String) this.converter.convertValue(resourceValue.getResourceValue(), this.context.getCtxServer().getResourceModelType(lwM2MClient.getRegistration(), pathIds), ResourceModel.Type.STRING, pathIds);
    }
}
