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
package org.thingsboard.server.transport.lwm2m.server.uplink;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.lwm2m.ObjectAttributes;
import org.thingsboard.server.common.data.device.data.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mOtaConvert;
import org.thingsboard.server.transport.lwm2m.server.LwM2mQueuedRequest;
import org.thingsboard.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientStateException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.client.ParametersAnalyzeResult;
import org.thingsboard.server.transport.lwm2m.server.client.ResultsAddKeyValueProto;
import org.thingsboard.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDiscoverCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDiscoverRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MLatchCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.rpc.LwM2MRpcRequestHandler;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_3_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_5_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_DELIVERY_METHOD;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_NAME_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_WARN;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertObjectIdToVersionedId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertOtaUpdateValueToString;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.fromVersionedIdToObjectId;


@Slf4j
@Service
@TbLwM2mTransportComponent
public class DefaultLwM2MUplinkMsgHandler extends LwM2MExecutorAwareService implements LwM2mUplinkMsgHandler {

    public LwM2mValueConverterImpl converter;

    private final TransportService transportService;
    private final LwM2mTransportContext context;
    private final LwM2MAttributesService attributesService;
    private final LwM2MOtaUpdateService otaService;
    public final LwM2MTransportServerConfig config;
    private final LwM2MTelemetryLogService logService;
    public final OtaPackageDataCache otaPackageDataCache;
    public final LwM2mTransportServerHelper helper;
    private final TbLwM2MDtlsSessionStore sessionStore;
    public final LwM2mClientContext clientContext;
    private final LwM2MRpcRequestHandler rpcHandler;
    public final LwM2mDownlinkMsgHandler defaultLwM2MDownlinkMsgHandler;

    public final Map<String, Integer> firmwareUpdateState;

    public DefaultLwM2MUplinkMsgHandler(TransportService transportService,
                                        LwM2MTransportServerConfig config,
                                        LwM2mTransportServerHelper helper,
                                        LwM2mClientContext clientContext,
                                        LwM2MTelemetryLogService logService,
                                        @Lazy LwM2MOtaUpdateService otaService,
                                        @Lazy LwM2MAttributesService attributesService,
                                        @Lazy LwM2MRpcRequestHandler rpcHandler,
                                        @Lazy LwM2mDownlinkMsgHandler defaultLwM2MDownlinkMsgHandler,
                                        OtaPackageDataCache otaPackageDataCache,
                                        LwM2mTransportContext context, TbLwM2MDtlsSessionStore sessionStore) {
        this.transportService = transportService;
        this.attributesService = attributesService;
        this.otaService = otaService;
        this.config = config;
        this.helper = helper;
        this.clientContext = clientContext;
        this.logService = logService;
        this.rpcHandler = rpcHandler;
        this.defaultLwM2MDownlinkMsgHandler = defaultLwM2MDownlinkMsgHandler;
        this.otaPackageDataCache = otaPackageDataCache;
        this.context = context;
        this.firmwareUpdateState = new ConcurrentHashMap<>();
        this.sessionStore = sessionStore;
    }

    @PostConstruct
    public void init() {
        super.init();
        this.context.getScheduler().scheduleAtFixedRate(this::reportActivity, new Random().nextInt((int) config.getSessionReportTimeout()), config.getSessionReportTimeout(), TimeUnit.MILLISECONDS);
        this.converter = LwM2mValueConverterImpl.getInstance();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    @Override
    protected String getExecutorName() {
        return "LwM2M uplink";
    }

    @Override
    protected int getExecutorSize() {
        return config.getUplinkPoolSize();
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
     * @param previousObservations - may be null
     */
    public void onRegistered(Registration registration, Collection<Observation> previousObservations) {
        executor.submit(() -> {
            LwM2mClient lwM2MClient = this.clientContext.getClientByEndpoint(registration.getEndpoint());
            try {
                log.warn("[{}] [{{}] Client: create after Registration", registration.getEndpoint(), registration.getId());
                if (lwM2MClient != null) {
                    Optional<SessionInfoProto> oldSessionInfo = this.clientContext.register(lwM2MClient, registration);
                    if (oldSessionInfo.isPresent()) {
                        log.info("[{}] Closing old session: {}", registration.getEndpoint(), new UUID(oldSessionInfo.get().getSessionIdMSB(), oldSessionInfo.get().getSessionIdLSB()));
                        closeSession(oldSessionInfo.get());
                    }
                    logService.log(lwM2MClient, LOG_LWM2M_INFO + ": Client registered with registration id: " + registration.getId());
                    SessionInfoProto sessionInfo = lwM2MClient.getSession();
                    transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(this, attributesService, rpcHandler, sessionInfo));
                    log.warn("40) sessionId [{}] Registering rpc subscription after Registration client", new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
                    TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                            .setSessionInfo(sessionInfo)
                            .setSessionEvent(DefaultTransportService.getSessionEventMsg(SessionEvent.OPEN))
                            .setSubscribeToAttributes(TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setSessionType(TransportProtos.SessionType.ASYNC).build())
                            .setSubscribeToRPC(TransportProtos.SubscribeToRPCMsg.newBuilder().setSessionType(TransportProtos.SessionType.ASYNC).build())
                            .build();
                    transportService.process(msg, null);
                    this.initClientTelemetry(lwM2MClient);
                    this.initAttributes(lwM2MClient);
                    otaService.init(lwM2MClient);
                } else {
                    log.error("Client: [{}] onRegistered [{}] name [{}] lwM2MClient ", registration.getId(), registration.getEndpoint(), null);
                }
            } catch (LwM2MClientStateException stateException) {
                if (LwM2MClientState.UNREGISTERED.equals(stateException.getState())) {
                    log.info("[{}] retry registration due to race condition: [{}].", registration.getEndpoint(), stateException.getState());
                    // Race condition detected and the client was in progress of unregistration while new registration arrived. Let's try again.
                    onRegistered(registration, previousObservations);
                } else {
                    logService.log(lwM2MClient, LOG_LWM2M_WARN + ": Client registration failed due to invalid state: " + stateException.getState());
                }
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable registration.", registration.getEndpoint(), t);
                logService.log(lwM2MClient, LOG_LWM2M_WARN + ": Client registration failed due to: " + t.getMessage());
            }
        });
    }

    /**
     * if sessionInfo removed from sessions, then new registerAsyncSession
     *
     * @param registration - Registration LwM2M Client
     */
    public void updatedReg(Registration registration) {
        executor.submit(() -> {
            LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
            try {
                log.warn("[{}] [{{}] Client: update after Registration", registration.getEndpoint(), registration.getId());
                logService.log(lwM2MClient, String.format("[%s][%s] Updated registration.", registration.getId(), registration.getSocketAddress()));
                clientContext.updateRegistration(lwM2MClient, registration);
                TransportProtos.SessionInfoProto sessionInfo = lwM2MClient.getSession();
                this.reportActivityAndRegister(sessionInfo);
                if (registration.usesQueueMode()) {
                    LwM2mQueuedRequest request;
                    while ((request = lwM2MClient.getQueuedRequests().poll()) != null) {
                        request.send();
                    }
                }
            } catch (LwM2MClientStateException stateException) {
                if (LwM2MClientState.REGISTERED.equals(stateException.getState())) {
                    log.info("[{}] update registration failed because client has different registration id: [{}] {}.", registration.getEndpoint(), stateException.getState(), stateException.getMessage());
                } else {
                    onRegistered(registration, Collections.emptyList());
                }
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable update registration.", registration.getEndpoint(), t);
                logService.log(lwM2MClient, LOG_LWM2M_ERROR + String.format(": Client update Registration, %s", t.getMessage()));
            }
        });
    }

    /**
     * @param registration - Registration LwM2M Client
     * @param observations - !!! Warn: if have not finishing unReg, then this operation will be finished on next Client`s connect
     */
    public void unReg(Registration registration, Collection<Observation> observations) {
        executor.submit(() -> {
            LwM2mClient client = clientContext.getClientByEndpoint(registration.getEndpoint());
            try {
                logService.log(client, LOG_LWM2M_INFO + ": Client unRegistration");
                clientContext.unregister(client, registration);
                SessionInfoProto sessionInfo = client.getSession();
                if (sessionInfo != null) {
                    closeSession(sessionInfo);
                    sessionStore.remove(registration.getEndpoint());
                    log.info("Client close session: [{}] unReg [{}] name  [{}] profile ", registration.getId(), registration.getEndpoint(), sessionInfo.getDeviceType());
                } else {
                    log.error("Client close session: [{}] unReg [{}] name  [{}] sessionInfo ", registration.getId(), registration.getEndpoint(), null);
                }
            } catch (LwM2MClientStateException stateException) {
                log.info("[{}] delete registration: [{}] {}.", registration.getEndpoint(), stateException.getState(), stateException.getMessage());
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable un registration.", registration.getEndpoint(), t);
                logService.log(client, LOG_LWM2M_ERROR + String.format(": Client Unable un Registration, %s", t.getMessage()));
            }
        });
    }

    public void closeSession(SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, DefaultTransportService.getSessionEventMsg(SessionEvent.CLOSED), null);
        transportService.deregisterSession(sessionInfo);
    }

    @Override
    public void onSleepingDev(Registration registration) {
        log.info("[{}] [{}] Received endpoint Sleeping version event", registration.getId(), registration.getEndpoint());
        logService.log(clientContext.getClientByEndpoint(registration.getEndpoint()), LOG_LWM2M_INFO + ": Client is sleeping!");
        //TODO: associate endpointId with device information.
    }

//    /**
//     * Cancel observation for All objects for this registration
//     */
//    @Override
//    public void setCancelObservationsAll(Registration registration) {
//        if (registration != null) {
//            LwM2mClient client = clientContext.getClientByEndpoint(registration.getEndpoint());
//            if (client != null && client.getRegistration() != null && client.getRegistration().getId().equals(registration.getId())) {
//                defaultLwM2MDownlinkMsgHandler.sendCancelAllRequest(client, TbLwM2MCancelAllRequest.builder().build(), new TbLwM2MCancelAllObserveRequestCallback(this, client));
//            }
//        }
//    }

    /**
     * Sending observe value to thingsboard from ObservationListener.onResponse: object, instance, SingleResource or MultipleResource
     *
     * @param registration - Registration LwM2M Client
     * @param path         - observe
     * @param response     - observe
     */
    @Override
    public void onUpdateValueAfterReadResponse(Registration registration, String path, ReadResponse response) {
        if (response.getContent() != null) {
            LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
            ObjectModel objectModelVersion = lwM2MClient.getObjectModel(path, this.config.getModelProvider());
            if (objectModelVersion != null) {
                if (response.getContent() instanceof LwM2mObject) {
                    LwM2mObject lwM2mObject = (LwM2mObject) response.getContent();
                    this.updateObjectResourceValue(lwM2MClient, lwM2mObject, path);
                } else if (response.getContent() instanceof LwM2mObjectInstance) {
                    LwM2mObjectInstance lwM2mObjectInstance = (LwM2mObjectInstance) response.getContent();
                    this.updateObjectInstanceResourceValue(lwM2MClient, lwM2mObjectInstance, path);
                } else if (response.getContent() instanceof LwM2mResource) {
                    LwM2mResource lwM2mResource = (LwM2mResource) response.getContent();
                    this.updateResourcesValue(lwM2MClient, lwM2mResource, path);
                }
            }
        }
    }

    /**
     * @param sessionInfo   -
     * @param deviceProfile -
     */
    @Override
    public void onDeviceProfileUpdate(SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        List<LwM2mClient> clients = clientContext.getLwM2mClients()
                .stream().filter(e -> e.getProfileId().equals(deviceProfile.getUuidId())).collect(Collectors.toList());
        clients.forEach(client -> client.onDeviceProfileUpdate(deviceProfile));
        if (clients.size() > 0) {
            this.onDeviceProfileUpdate(clients, deviceProfile);
        }
    }

    @Override
    public void onDeviceUpdate(SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        //TODO: check, maybe device has multiple sessions/registrations? Is this possible according to the standard.
        LwM2mClient client = clientContext.getClientByDeviceId(device.getUuidId());
        if (client != null) {
            this.onDeviceUpdate(client, device, deviceProfileOpt);
        }
    }

    @Override
    public void onResourceUpdate(Optional<TransportProtos.ResourceUpdateMsg> resourceUpdateMsgOpt) {
        String idVer = resourceUpdateMsgOpt.get().getResourceKey();
        clientContext.getLwM2mClients().forEach(e -> e.updateResourceModel(idVer, this.config.getModelProvider()));
    }

    @Override
    public void onResourceDelete(Optional<TransportProtos.ResourceDeleteMsg> resourceDeleteMsgOpt) {
        String pathIdVer = resourceDeleteMsgOpt.get().getResourceKey();
        clientContext.getLwM2mClients().forEach(e -> e.deleteResources(pathIdVer, this.config.getModelProvider()));
    }

    /**
     * Deregister session in transport
     *
     * @param sessionInfo - lwm2m client
     */
    @Override
    public void doDisconnect(SessionInfoProto sessionInfo) {
        closeSession(sessionInfo);
    }

    /**
     * Those methods are called by the protocol stage thread pool, this means that execution MUST be done in a short delay,
     * * if you need to do long time processing use a dedicated thread pool.
     *
     * @param registration -
     */
    @Override
    public void onAwakeDev(Registration registration) {
        log.trace("[{}] [{}] Received endpoint Awake version event", registration.getId(), registration.getEndpoint());
        logService.log(clientContext.getClientByEndpoint(registration.getEndpoint()), LOG_LWM2M_INFO + ": Client is awake!");
        //TODO: associate endpointId with device information.
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
     * @param lwM2MClient - object with All parameters off client
     */
    private void initClientTelemetry(LwM2mClient lwM2MClient) {
        Lwm2mDeviceProfileTransportConfiguration profile = clientContext.getProfile(lwM2MClient.getProfileId());
        Set<String> supportedObjects = clientContext.getSupportedIdVerInClient(lwM2MClient);
        if (supportedObjects != null && supportedObjects.size() > 0) {
            // #1
            this.sendReadRequests(lwM2MClient, profile, supportedObjects);
            this.sendObserveRequests(lwM2MClient, profile, supportedObjects);
            this.sendWriteAttributeRequests(lwM2MClient, profile, supportedObjects);
//            Removed. Used only for debug.
//            this.sendDiscoverRequests(lwM2MClient, profile, supportedObjects);
        }
    }

    private void sendReadRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        Set<String> targetIds = new HashSet<>(profile.getObserveAttr().getAttribute());
        targetIds.addAll(profile.getObserveAttr().getTelemetry());
        targetIds = targetIds.stream().filter(target -> isSupportedTargetId(supportedObjects, target)).collect(Collectors.toSet());

        CountDownLatch latch = new CountDownLatch(targetIds.size());
        targetIds.forEach(versionedId -> sendReadRequest(lwM2MClient, versionedId,
                new TbLwM2MLatchCallback<>(latch, new TbLwM2MReadCallback(this, logService, lwM2MClient, versionedId))));
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("[{}] Failed to await Read requests!", lwM2MClient.getEndpoint());
        }
    }

    private void sendObserveRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        Set<String> targetIds = profile.getObserveAttr().getObserve();
        targetIds = targetIds.stream().filter(target -> isSupportedTargetId(supportedObjects, target)).collect(Collectors.toSet());

        CountDownLatch latch = new CountDownLatch(targetIds.size());
        targetIds.forEach(targetId -> sendObserveRequest(lwM2MClient, targetId,
                new TbLwM2MLatchCallback<>(latch, new TbLwM2MObserveCallback(this, logService, lwM2MClient, targetId))));
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("[{}] Failed to await Observe requests!", lwM2MClient.getEndpoint());
        }
    }

    private void sendWriteAttributeRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        Map<String, ObjectAttributes> attributesMap = profile.getObserveAttr().getAttributeLwm2m();
        attributesMap = attributesMap.entrySet().stream().filter(target -> isSupportedTargetId(supportedObjects, target.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//        TODO: why do we need to put observe into pending read requests?
//        lwM2MClient.getPendingReadRequests().addAll(targetIds);
        attributesMap.forEach((targetId, params) -> sendWriteAttributesRequest(lwM2MClient, targetId, params));
    }

    private void sendDiscoverRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        Set<String> targetIds = profile.getObserveAttr().getAttributeLwm2m().keySet();
        targetIds = targetIds.stream().filter(target -> isSupportedTargetId(supportedObjects, target)).collect(Collectors.toSet());
//        TODO: why do we need to put observe into pending read requests?
//        lwM2MClient.getPendingReadRequests().addAll(targetIds);
        targetIds.forEach(targetId -> sendDiscoverRequest(lwM2MClient, targetId));
    }

    private void sendDiscoverRequest(LwM2mClient lwM2MClient, String targetId) {
        TbLwM2MDiscoverRequest request = TbLwM2MDiscoverRequest.builder().versionedId(targetId).timeout(this.config.getTimeout()).build();
        defaultLwM2MDownlinkMsgHandler.sendDiscoverRequest(lwM2MClient, request, new TbLwM2MDiscoverCallback(logService, lwM2MClient, targetId));
    }

    private void sendReadRequest(LwM2mClient lwM2MClient, String versionedId) {
        sendReadRequest(lwM2MClient, versionedId, new TbLwM2MReadCallback(this, logService, lwM2MClient, versionedId));
    }

    private void sendReadRequest(LwM2mClient lwM2MClient, String versionedId, DownlinkRequestCallback<ReadRequest, ReadResponse> callback) {
        TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        defaultLwM2MDownlinkMsgHandler.sendReadRequest(lwM2MClient, request, callback);
    }

    private void sendObserveRequest(LwM2mClient lwM2MClient, String versionedId) {
        sendObserveRequest(lwM2MClient, versionedId, new TbLwM2MObserveCallback(this, logService, lwM2MClient, versionedId));
    }

    private void sendObserveRequest(LwM2mClient lwM2MClient, String versionedId, DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback) {
        TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        defaultLwM2MDownlinkMsgHandler.sendObserveRequest(lwM2MClient, request, callback);
    }

    private void sendWriteAttributesRequest(LwM2mClient lwM2MClient, String targetId, ObjectAttributes params) {
        TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(targetId).attributes(params).timeout(this.config.getTimeout()).build();
        defaultLwM2MDownlinkMsgHandler.sendWriteAttributesRequest(lwM2MClient, request, new TbLwM2MWriteAttributesCallback(logService, lwM2MClient, targetId));
    }

    private void sendCancelObserveRequest(String versionedId, LwM2mClient client) {
        TbLwM2MCancelObserveRequest request = TbLwM2MCancelObserveRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        defaultLwM2MDownlinkMsgHandler.sendCancelObserveRequest(client, request, new TbLwM2MCancelObserveCallback(logService, client, versionedId));
    }

    private void updateObjectResourceValue(LwM2mClient client, LwM2mObject lwM2mObject, String pathIdVer) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer));
        lwM2mObject.getInstances().forEach((instanceId, instance) -> {
            String pathInstance = pathIds.toString() + "/" + instanceId;
            this.updateObjectInstanceResourceValue(client, instance, pathInstance);
        });
    }

    private void updateObjectInstanceResourceValue(LwM2mClient client, LwM2mObjectInstance lwM2mObjectInstance, String pathIdVer) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer));
        lwM2mObjectInstance.getResources().forEach((resourceId, resource) -> {
            String pathRez = pathIds.toString() + "/" + resourceId;
            this.updateResourcesValue(client, resource, pathRez);
        });
    }

    /**
     * Sending observe value of resources to thingsboard
     * #1 Return old Value Resource from LwM2MClient
     * #2 Update new Resources (replace old Resource Value on new Resource Value)
     * #3 If fr_update -> UpdateFirmware
     * #4 updateAttrTelemetry
     *
     * @param lwM2MClient   - Registration LwM2M Client
     * @param lwM2mResource - LwM2mSingleResource response.getContent()
     * @param path          - resource
     */
    private void updateResourcesValue(LwM2mClient lwM2MClient, LwM2mResource lwM2mResource, String path) {
        Registration registration = lwM2MClient.getRegistration();
        if (lwM2MClient.saveResourceValue(path, lwM2mResource, this.config.getModelProvider())) {
            if (path.equals(convertObjectIdToVersionedId(FW_NAME_ID, registration))) {
                otaService.onCurrentFirmwareNameUpdate(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_3_VER_ID, registration))) {
                otaService.onCurrentFirmwareVersion3Update(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_5_VER_ID, registration))) {
                otaService.onCurrentFirmwareVersion5Update(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_STATE_ID, registration))) {
                otaService.onCurrentFirmwareStateUpdate(lwM2MClient, (Long) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_RESULT_ID, registration))) {
                otaService.onCurrentFirmwareResultUpdate(lwM2MClient, (Long) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_DELIVERY_METHOD, registration))) {
                otaService.onCurrentFirmwareDeliveryMethodUpdate(lwM2MClient, (Long) lwM2mResource.getValue());
            }
            this.updateAttrTelemetry(registration, Collections.singleton(path));
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
        try {
            ResultsAddKeyValueProto results = this.getParametersFromProfile(registration, paths);
            SessionInfoProto sessionInfo = this.getSessionInfoOrCloseSession(registration);
            if (results != null && sessionInfo != null) {
                if (results.getResultAttributes().size() > 0) {
                    this.helper.sendParametersOnThingsboardAttribute(results.getResultAttributes(), sessionInfo);
                }
                if (results.getResultTelemetries().size() > 0) {
                    this.helper.sendParametersOnThingsboardTelemetry(results.getResultTelemetries(), sessionInfo);
                }
            }
        } catch (Exception e) {
            log.error("UpdateAttrTelemetry", e);
        }
    }

    private boolean isSupportedTargetId(Set<String> supportedIds, String targetId) {
        String[] targetIdParts = targetId.split(LWM2M_SEPARATOR_PATH);
        if (targetIdParts.length <= 1) {
            return false;
        }
        String targetIdSearch = targetIdParts[0];
        for (int i = 1; i < targetIdParts.length; i++) {
            targetIdSearch += "/" + targetIdParts[i];
            if (supportedIds.contains(targetIdSearch)) {
                return true;
            }
        }
        return false;
    }

    private ConcurrentHashMap<String, Object> getPathForWriteAttributes(JsonObject objectJson) {
        ConcurrentHashMap<String, Object> pathAttributes = new Gson().fromJson(objectJson.toString(),
                new TypeToken<ConcurrentHashMap<String, Object>>() {
                }.getType());
        return pathAttributes;
    }

    private void onDeviceUpdate(LwM2mClient lwM2MClient, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        deviceProfileOpt.ifPresent(deviceProfile -> this.onDeviceProfileUpdate(Collections.singletonList(lwM2MClient), deviceProfile));
        lwM2MClient.onDeviceUpdate(device, deviceProfileOpt);
    }

    /**
     * //     * @param attributes   - new JsonObject
     * //     * @param telemetry    - new JsonObject
     *
     * @param registration - Registration LwM2M Client
     * @param path         -
     */
    private ResultsAddKeyValueProto getParametersFromProfile(Registration registration, Set<String> path) {
        if (path != null && path.size() > 0) {
            ResultsAddKeyValueProto results = new ResultsAddKeyValueProto();
            var profile = clientContext.getProfile(registration);
            List<TransportProtos.KeyValueProto> resultAttributes = new ArrayList<>();
            profile.getObserveAttr().getAttribute().forEach(pathIdVer -> {
                if (path.contains(pathIdVer)) {
                    TransportProtos.KeyValueProto kvAttr = this.getKvToThingsboard(pathIdVer, registration);
                    if (kvAttr != null) {
                        resultAttributes.add(kvAttr);
                    }
                }
            });
            List<TransportProtos.KeyValueProto> resultTelemetries = new ArrayList<>();
            profile.getObserveAttr().getTelemetry().forEach(pathIdVer -> {
                if (path.contains(pathIdVer)) {
                    TransportProtos.KeyValueProto kvAttr = this.getKvToThingsboard(pathIdVer, registration);
                    if (kvAttr != null) {
                        resultTelemetries.add(kvAttr);
                    }
                }
            });
            if (resultAttributes.size() > 0) {
                results.setResultAttributes(resultAttributes);
            }
            if (resultTelemetries.size() > 0) {
                results.setResultTelemetries(resultTelemetries);
            }
            return results;
        }
        return null;
    }

    private TransportProtos.KeyValueProto getKvToThingsboard(String pathIdVer, Registration registration) {
        LwM2mClient lwM2MClient = this.clientContext.getClientByEndpoint(registration.getEndpoint());
        Map<String, String> names = clientContext.getProfile(lwM2MClient.getProfileId()).getObserveAttr().getKeyName();
        if (names != null && names.containsKey(pathIdVer)) {
            String resourceName = names.get(pathIdVer);
            if (resourceName != null && !resourceName.isEmpty()) {
                try {
                    LwM2mResource resourceValue = LwM2mTransportUtil.getResourceValueFromLwM2MClient(lwM2MClient, pathIdVer);
                    if (resourceValue != null) {
                        ResourceModel.Type currentType = resourceValue.getType();
                        ResourceModel.Type expectedType = this.helper.getResourceModelTypeEqualsKvProtoValueType(currentType, pathIdVer);
                        Object valueKvProto = null;
                        if (resourceValue.isMultiInstances()) {
                            valueKvProto = new JsonObject();
                            Object finalvalueKvProto = valueKvProto;
                            Gson gson = new GsonBuilder().create();
                            ResourceModel.Type finalCurrentType = currentType;
                            resourceValue.getInstances().forEach((k, v) -> {
                                Object val = this.converter.convertValue(v, finalCurrentType, expectedType,
                                        new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)));
                                JsonElement element = gson.toJsonTree(val, val.getClass());
                                ((JsonObject) finalvalueKvProto).add(String.valueOf(k), element);
                            });
                            valueKvProto = gson.toJson(valueKvProto);
                        } else {
                            valueKvProto = this.converter.convertValue(resourceValue.getValue(), currentType, expectedType,
                                    new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)));
                        }
                        LwM2mOtaConvert lwM2mOtaConvert = convertOtaUpdateValueToString(pathIdVer, valueKvProto, currentType);
                        valueKvProto = lwM2mOtaConvert.getValue();
                        currentType = lwM2mOtaConvert.getCurrentType();
                        return valueKvProto != null ? this.helper.getKvAttrTelemetryToThingsboard(currentType, resourceName, valueKvProto, resourceValue.isMultiInstances()) : null;
                    }
                } catch (Exception e) {
                    log.error("Failed to add parameters.", e);
                }
            }
        } else {
            log.error("Failed to add parameters. path: [{}], names: [{}]", pathIdVer, names);
        }
        return null;
    }

    @Override
    public void onWriteResponseOk(LwM2mClient client, String path, WriteRequest request) {
        if (request.getNode() instanceof LwM2mResource) {
            this.updateResourcesValue(client, ((LwM2mResource) request.getNode()), path);
        } else if (request.getNode() instanceof LwM2mObjectInstance) {
            ((LwM2mObjectInstance) request.getNode()).getResources().forEach((resId, resource) -> {
                this.updateResourcesValue(client, resource, path + "/" + resId);
            });
        }

    }

    //TODO: review and optimize the logic to minimize number of the requests to device.
    private void onDeviceProfileUpdate(List<LwM2mClient> clients, DeviceProfile deviceProfile) {
        var oldProfile = clientContext.getProfile(deviceProfile.getUuidId());
        if (clientContext.profileUpdate(deviceProfile) != null) {
            // #1
            TelemetryMappingConfiguration oldTelemetryParams = oldProfile.getObserveAttr();
            Set<String> attributeSetOld = oldTelemetryParams.getAttribute();
            Set<String> telemetrySetOld = oldTelemetryParams.getTelemetry();
            Set<String> observeOld = oldTelemetryParams.getObserve();
            Map<String, String> keyNameOld = oldTelemetryParams.getKeyName();
            Map<String, ObjectAttributes> attributeLwm2mOld = oldTelemetryParams.getAttributeLwm2m();

            var newProfile = clientContext.getProfile(deviceProfile.getUuidId());
            TelemetryMappingConfiguration newTelemetryParams = newProfile.getObserveAttr();
            Set<String> attributeSetNew = newTelemetryParams.getAttribute();
            Set<String> telemetrySetNew = newTelemetryParams.getTelemetry();
            Set<String> observeNew = newTelemetryParams.getObserve();
            Map<String, String> keyNameNew = newTelemetryParams.getKeyName();
            Map<String, ObjectAttributes> attributeLwm2mNew = newTelemetryParams.getAttributeLwm2m();

            Set<String> observeToAdd = diffSets(observeOld, observeNew);
            Set<String> observeToRemove = diffSets(observeNew, observeOld);

            Set<String> newObjectsToRead = new HashSet<>();

            // #3.1
            if (!attributeSetOld.equals(attributeSetNew)) {
                newObjectsToRead.addAll(diffSets(attributeSetOld, attributeSetNew));
            }
            // #3.2
            if (!telemetrySetOld.equals(telemetrySetNew)) {
                newObjectsToRead.addAll(diffSets(telemetrySetOld, telemetrySetNew));
            }
            // #3.3
            if (!keyNameOld.equals(keyNameNew)) {
                ParametersAnalyzeResult keyNameChange = this.getAnalyzerKeyName(keyNameOld, keyNameNew);
                newObjectsToRead.addAll(keyNameChange.getPathPostParametersAdd());
            }

            // #3.4, #6
            if (!attributeLwm2mOld.equals(attributeLwm2mNew)) {
                this.compareAndSendWriteAttributes(clients, attributeLwm2mOld, attributeLwm2mNew);
            }

            // #4.1 add
            if (!newObjectsToRead.isEmpty()) {
                Set<String> newObjectsToReadButNotNewInObserve = diffSets(observeToAdd, newObjectsToRead);
                // update value in Resources
                for (String versionedId : newObjectsToReadButNotNewInObserve) {
                    clients.forEach(client -> sendReadRequest(client, versionedId));
                }
            }

            // Calculating difference between old and new flags.
            if (!observeToAdd.isEmpty()) {
                for (String targetId : observeToAdd) {
                    clients.forEach(client -> sendObserveRequest(client, targetId));
                }
            }
            if (!observeToRemove.isEmpty()) {
                for (String targetId : observeToRemove) {
                    clients.forEach(client -> sendCancelObserveRequest(targetId, client));
                }
            }
        }
    }

    /**
     * Returns new set with elements that are present in set B(new) but absent in set A(old).
     */
    private static <T> Set<T> diffSets(Set<T> a, Set<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toSet());
    }

    private ParametersAnalyzeResult getAnalyzerKeyName(Map<String, String> keyNameOld, Map<String, String> keyNameNew) {
        ParametersAnalyzeResult analyzerParameters = new ParametersAnalyzeResult();
        Set<String> paths = keyNameNew.entrySet()
                .stream()
                .filter(e -> !e.getValue().equals(keyNameOld.get(e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).keySet();
        analyzerParameters.setPathPostParametersAdd(paths);
        return analyzerParameters;
    }

    /**
     * #6.1 - send update WriteAttribute
     * #6.2 - send empty WriteAttribute
     */
    private void compareAndSendWriteAttributes(List<LwM2mClient> clients, Map<String, ObjectAttributes> lwm2mAttributesOld, Map<String, ObjectAttributes> lwm2mAttributesNew) {
        ParametersAnalyzeResult analyzerParameters = new ParametersAnalyzeResult();
        Set<String> pathOld = lwm2mAttributesOld.keySet();
        Set<String> pathNew = lwm2mAttributesNew.keySet();
        analyzerParameters.setPathPostParametersAdd(pathNew
                .stream().filter(p -> !pathOld.contains(p)).collect(Collectors.toSet()));
        analyzerParameters.setPathPostParametersDel(pathOld
                .stream().filter(p -> !pathNew.contains(p)).collect(Collectors.toSet()));
        Set<String> pathCommon = pathNew
                .stream().filter(pathOld::contains).collect(Collectors.toSet());
        Set<String> pathCommonChange = pathCommon
                .stream().filter(p -> !lwm2mAttributesOld.get(p).equals(lwm2mAttributesNew.get(p))).collect(Collectors.toSet());
        analyzerParameters.getPathPostParametersAdd().addAll(pathCommonChange);
        // #6
        // #6.2
        if (analyzerParameters.getPathPostParametersAdd().size() > 0) {
            clients.forEach(client -> {
                Set<String> clientObjects = clientContext.getSupportedIdVerInClient(client);
                Set<String> pathSend = analyzerParameters.getPathPostParametersAdd().stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
                        .collect(Collectors.toUnmodifiableSet());
                if (!pathSend.isEmpty()) {
                    pathSend.forEach(target -> sendWriteAttributesRequest(client, target, lwm2mAttributesNew.get(target)));
                }
            });
        }
        // #6.2
        if (analyzerParameters.getPathPostParametersDel().size() > 0) {
            clients.forEach(client -> {
                Set<String> clientObjects = clientContext.getSupportedIdVerInClient(client);
                Set<String> pathSend = analyzerParameters.getPathPostParametersDel().stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
                        .collect(Collectors.toUnmodifiableSet());
                if (!pathSend.isEmpty()) {
                    pathSend.forEach(target -> sendWriteAttributesRequest(client, target, new ObjectAttributes()));
                }
            });
        }
    }

    /**
     * @param updateCredentials - Credentials include config only security Client (without config attr/telemetry...)
     *                          config attr/telemetry... in profile
     */
    @Override
    public void onToTransportUpdateCredentials(TransportProtos.ToTransportUpdateCredentialsProto updateCredentials) {
        log.info("[{}] idList [{}] valueList updateCredentials", updateCredentials.getCredentialsIdList(), updateCredentials.getCredentialsValueList());
    }

    /**
     * @param lwM2MClient -
     * @return SessionInfoProto -
     */
    private SessionInfoProto getSessionInfo(LwM2mClient lwM2MClient) {
        if (lwM2MClient != null && lwM2MClient.getSession() != null) {
            return lwM2MClient.getSession();
        }
        return null;
    }

    /**
     * @param registration - Registration LwM2M Client
     * @return - sessionInfo after access connect client
     */
    public SessionInfoProto getSessionInfoOrCloseSession(Registration registration) {
        return getSessionInfo(clientContext.getClientByEndpoint(registration.getEndpoint()));
    }

    /**
     * if sessionInfo removed from sessions, then new registerAsyncSession
     *
     * @param sessionInfo -
     */
    private void reportActivityAndRegister(SessionInfoProto sessionInfo) {
        if (sessionInfo != null && transportService.reportActivity(sessionInfo) == null) {
            transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(this, attributesService, rpcHandler, sessionInfo));
            this.reportActivitySubscription(sessionInfo);
        }
    }

    private void reportActivity() {
        clientContext.getLwM2mClients().forEach(client -> reportActivityAndRegister(client.getSession()));
    }

    /**
     * #1. !!!  sharedAttr === profileAttr  !!!
     * - If there is a difference in values between the current resource values and the shared attribute values
     * - when the client connects to the server
     * #1.1 get attributes name from profile include name resources in ModelObject if resource  isWritable
     * #1.2 #1 size > 0 => send Request getAttributes to thingsboard
     * #2. FirmwareAttribute subscribe:
     *
     * @param lwM2MClient - LwM2M Client
     */
    public void initAttributes(LwM2mClient lwM2MClient) {
        Map<String, String> keyNamesMap = this.getNamesFromProfileForSharedAttributes(lwM2MClient);
        if (!keyNamesMap.isEmpty()) {
            Set<String> keysToFetch = new HashSet<>(keyNamesMap.values());
            keysToFetch.removeAll(OtaPackageUtil.ALL_FW_ATTRIBUTE_KEYS);
            keysToFetch.removeAll(OtaPackageUtil.ALL_SW_ATTRIBUTE_KEYS);
            DonAsynchron.withCallback(attributesService.getSharedAttributes(lwM2MClient, keysToFetch),
                    v -> attributesService.onAttributesUpdate(lwM2MClient, v),
                    t -> log.error("[{}] Failed to get attributes", lwM2MClient.getEndpoint(), t),
                    executor);
        }
    }

    private TransportProtos.GetOtaPackageRequestMsg createOtaPackageRequestMsg(SessionInfoProto sessionInfo, String nameFwSW) {
        return TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                .setDeviceIdMSB(sessionInfo.getDeviceIdMSB())
                .setDeviceIdLSB(sessionInfo.getDeviceIdLSB())
                .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                .setType(nameFwSW)
                .build();
    }

    private Map<String, String> getNamesFromProfileForSharedAttributes(LwM2mClient lwM2MClient) {
        Lwm2mDeviceProfileTransportConfiguration profile = clientContext.getProfile(lwM2MClient.getProfileId());
        return profile.getObserveAttr().getKeyName();
    }

    public LwM2MTransportServerConfig getConfig() {
        return this.config;
    }

    private void reportActivitySubscription(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(true)
                .setRpcSubscription(true)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
    }
}
