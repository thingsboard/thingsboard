/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mOtaConvert;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;
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
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MLatchCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfig;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfigService;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.session.LwM2MSessionManager;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mSecurityStore;
import org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.time.Instant;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.common.data.util.CollectionsUtil.diffSets;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_3_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_DELIVERY_METHOD;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_NAME_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_3_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_NAME_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_VER_ID;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_WARN;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertOtaUpdateValueToString;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;


@Slf4j
@Service("lwM2mUplinkMsgHandler")
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mUplinkMsgHandler extends LwM2MExecutorAwareService implements LwM2mUplinkMsgHandler {

    @Getter
    private final LwM2mValueConverter converter = LwM2mValueConverterImpl.getInstance();

    private final TransportService transportService;
    private final LwM2mTransportContext context;
    @Lazy
    private final LwM2MAttributesService attributesService;
    private final LwM2MSessionManager sessionManager;
    @Lazy
    private final LwM2MOtaUpdateService otaService;
    private final LwM2MTransportServerConfig config;
    private final LwM2MTelemetryLogService logService;
    private final LwM2mTransportServerHelper helper;
    private final TbLwM2MDtlsSessionStore sessionStore;
    private final LwM2mClientContext clientContext;
    private final LwM2mDownlinkMsgHandler defaultLwM2MDownlinkMsgHandler; //Do not use Lazy because we need live executor to handle msgs
    private final LwM2mVersionedModelProvider modelProvider;
    private final RegistrationStore registrationStore;
    private final TbLwM2mSecurityStore securityStore;
    private final LwM2MModelConfigService modelConfigService;

    @PostConstruct
    public void init() {
        super.init();
        this.context.getScheduler().scheduleAtFixedRate(this::reportActivity, new Random().nextInt((int) config.getSessionReportTimeout()), config.getSessionReportTimeout(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        log.trace("Destroying {}", getClass().getSimpleName());
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
                log.debug("[{}] [{{}] Client: create after Registration", registration.getEndpoint(), registration.getId());
                Optional<SessionInfoProto> oldSessionInfo = this.clientContext.register(lwM2MClient, registration);
                if (oldSessionInfo.isPresent()) {
                    log.info("[{}] Closing old session: {}", registration.getEndpoint(), new UUID(oldSessionInfo.get().getSessionIdMSB(), oldSessionInfo.get().getSessionIdLSB()));
                    sessionManager.deregister(oldSessionInfo.get());
                }
                logService.log(lwM2MClient, LOG_LWM2M_INFO + ": Client registered with registration id: " + registration.getId() + " version: "
                        + registration.getLwM2mVersion() + " and modes: " + registration.getQueueMode() + ", " + registration.getBindingMode());
                sessionManager.register(lwM2MClient.getSession());
                this.initClientTelemetry(lwM2MClient);
                this.initAttributes(lwM2MClient, true);
                otaService.init(lwM2MClient);
                lwM2MClient.getRetryAttempts().set(0);
            } catch (LwM2MClientStateException stateException) {
                if (LwM2MClientState.UNREGISTERED.equals(stateException.getState())) {
                    log.info("[{}] retry registration due to race condition: [{}].", registration.getEndpoint(), stateException.getState());
                    // Race condition detected and the client was in progress of unregistration while new registration arrived. Let's try again.
                    if (lwM2MClient.getRetryAttempts().incrementAndGet() <= 5) {
                        context.getScheduler().schedule(() -> onRegistered(registration, previousObservations), 1, TimeUnit.SECONDS);
                    } else {
                        logService.log(lwM2MClient, LOG_LWM2M_WARN + ": Client registration failed due to retry attempts: " + lwM2MClient.getRetryAttempts().get());
                    }
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
                log.info("[{}] [{{}] Client: update after Registration", registration.getEndpoint(), registration.getId());
                logService.log(lwM2MClient, String.format("[%s][%s] Updated registration.", registration.getId(), registration.getSocketAddress()));
                clientContext.updateRegistration(lwM2MClient, registration);
                this.reportActivityAndRegister(lwM2MClient.getSession());
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
        executor.submit(() -> doUnReg(registration, clientContext.getClientByEndpoint(registration.getEndpoint())));
    }

    private void doUnReg(Registration registration, LwM2mClient client) {
        try {
            logService.log(client, LOG_LWM2M_INFO + ": Client unRegistration");
            clientContext.unregister(client, registration);
            SessionInfoProto sessionInfo = client.getSession();
            if (sessionInfo != null) {
                securityStore.remove(client.getEndpoint(), client.getRegistration().getId());
                sessionManager.deregister(sessionInfo);
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
    }

    @Override
    public void onSleepingDev(Registration registration) {
        log.debug("[{}] [{}] Received endpoint sleeping event", registration.getId(), registration.getEndpoint());
        clientContext.asleep(clientContext.getClientByEndpoint(registration.getEndpoint()));
    }

    /**
     * Sending observe value to thingsboard from ObservationListener.onResponse: object, instance, SingleResource or MultipleResource
     *
     * @param registration - Registration LwM2M Client
     * @param path         - observe
     * @param response     - observe
     */
    @Override
    public void onUpdateValueAfterReadResponse(Registration registration, String path, ReadResponse response) {
        LwM2mNode content = response.getContent();
        if (content != null) {
            LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
            ObjectModel objectModelVersion = lwM2MClient.getObjectModel(path, modelProvider);
            if (objectModelVersion != null) {
                int responseCode = response.getCode().getCode();
                if (content instanceof LwM2mObject) {
                    LwM2mObject lwM2mObject = (LwM2mObject) content;
                    this.updateObjectResourceValue(lwM2MClient, lwM2mObject, path, responseCode);
                } else if (content instanceof LwM2mObjectInstance) {
                    LwM2mObjectInstance lwM2mObjectInstance = (LwM2mObjectInstance) content;
                    this.updateObjectInstanceResourceValue(lwM2MClient, lwM2mObjectInstance, path, responseCode);
                } else if (content instanceof LwM2mResource) {
                    LwM2mResource lwM2mResource = (LwM2mResource) content;
                    this.updateResourcesValue(lwM2MClient, lwM2mResource, path, Mode.UPDATE, responseCode);
                }
            }
            tryAwake(lwM2MClient);
        }
    }

    public void onUpdateValueAfterReadCompositeResponse(Registration registration, ReadCompositeResponse response) {
        log.trace("ReadCompositeResponse: [{}]", response);
        if (response.getContent() != null) {
            LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
            response.getContent().forEach((k, v) -> {
                if (v != null) {
                    int responseCode = response.getCode().getCode();
                    if (v instanceof LwM2mObject) {
                        this.updateObjectResourceValue(lwM2MClient, (LwM2mObject) v, k.toString(), responseCode);
                    } else if (v instanceof LwM2mObjectInstance) {
                        this.updateObjectInstanceResourceValue(lwM2MClient, (LwM2mObjectInstance) v, k.toString(), responseCode);
                    } else if (v instanceof LwM2mResource) {
                        this.updateResourcesValue(lwM2MClient, (LwM2mResource) v, k.toString(), Mode.UPDATE, responseCode);
                    }
                } else {
                    this.onErrorObservation(registration, k + ": value in composite response is null");
                }
            });
            clientContext.update(lwM2MClient);
            tryAwake(lwM2MClient);
        }
    }

    public void onErrorObservation(Registration registration, String errorMsg) {
        LwM2mClient lwM2MClient = this.clientContext.getClientByEndpoint(registration.getEndpoint());
        logService.log(lwM2MClient, LOG_LWM2M_ERROR + ": " + errorMsg);
    }


    /**
     * Sending updated value to thingsboard from SendListener.dataReceived: object, instance, SingleResource or MultipleResource
     *
     * @param registration - Registration LwM2M Client
     * @param data  - TimestampedLwM2mNodes (send From Client CollectedValue)
     */
    @Override
    public void onUpdateValueWithSendRequest(Registration registration, TimestampedLwM2mNodes data) {
        for (Instant ts : data.getTimestamps()) {
            Map<LwM2mPath, LwM2mNode> nodesAt = data.getNodesAt(ts);
            for (var instant : nodesAt.entrySet()) {
                LwM2mPath path = instant.getKey();
                LwM2mNode node = instant.getValue();
                LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
                ObjectModel objectModelVersion = lwM2MClient.getObjectModel(path.toString(), modelProvider);
                if (objectModelVersion != null) {
                    if (node instanceof LwM2mObject) {
                        LwM2mObject lwM2mObject = (LwM2mObject) node;
                        this.updateObjectResourceValue(lwM2MClient, lwM2mObject, path.toString(), 0);
                    } else if (node instanceof LwM2mObjectInstance) {
                        LwM2mObjectInstance lwM2mObjectInstance = (LwM2mObjectInstance) node;
                        this.updateObjectInstanceResourceValue(lwM2MClient, lwM2mObjectInstance, path.toString(), 0);
                    } else if (node instanceof LwM2mResource) {
                        LwM2mResource lwM2mResource = (LwM2mResource) node;
                        this.updateResourcesValueWithTs(lwM2MClient, lwM2mResource, path.toString(), Mode.UPDATE, ts);
                    }
                }
                tryAwake(lwM2MClient);
            }
        }
    }

    /**
     * @param sessionInfo   -
     * @param deviceProfile -
     */
    @Override
    public void onDeviceProfileUpdate(SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        try {
            List<LwM2mClient> clients = clientContext.getLwM2mClients()
                    .stream().filter(e -> e.getProfileId() != null)
                    .filter(e -> e.getProfileId().equals(deviceProfile.getUuidId())).collect(Collectors.toList());
            clients.forEach(client -> {
                client.onDeviceProfileUpdate(deviceProfile);
            });
            if (clients.size() > 0) {
                var oldProfile = clientContext.getProfile(deviceProfile.getUuidId());
                this.onDeviceProfileUpdate(clients, oldProfile, deviceProfile);
            }
        } catch (Exception e) {
            log.warn("[{}] failed to update profile: {}", deviceProfile.getId(), deviceProfile);
        }
    }

    @Override
    public void onDeviceUpdate(SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> newDeviceProfileOpt) {
        try {
            LwM2mClient client = clientContext.getClientByDeviceId(device.getUuidId());
            if (client != null) {
                if (newDeviceProfileOpt.isPresent()) {
                    this.securityStore.remove(client.getEndpoint(), client.getRegistration().getId());
                }
                this.onDeviceUpdate(client, device, newDeviceProfileOpt);
            }
        } catch (Exception e) {
            log.warn("[{}] failed to update device: {}", device.getId(), device);
        }
    }

    @Override
    public void onDeviceDelete(DeviceId deviceId) {
        clearAndUnregister(clientContext.getClientByDeviceId(deviceId.getId()));
    }

    @Override
    public void onResourceUpdate(TransportProtos.ResourceUpdateMsg resourceUpdateMsgOpt) {
        String idVer = resourceUpdateMsgOpt.getResourceKey();
        TenantId tenantId = TenantId.fromUUID(new UUID(resourceUpdateMsgOpt.getTenantIdMSB(), resourceUpdateMsgOpt.getTenantIdLSB()));
        modelProvider.evict(tenantId, idVer);
        clientContext.getLwM2mClients().forEach(e -> e.updateResourceModel(idVer, modelProvider));
    }

    @Override
    public void onResourceDelete(TransportProtos.ResourceDeleteMsg resourceDeleteMsgOpt) {
        String pathIdVer = resourceDeleteMsgOpt.getResourceKey();
        TenantId tenantId = TenantId.fromUUID(new UUID(resourceDeleteMsgOpt.getTenantIdMSB(), resourceDeleteMsgOpt.getTenantIdLSB()));
        modelProvider.evict(tenantId, pathIdVer);
        clientContext.getLwM2mClients().forEach(e -> e.deleteResources(pathIdVer, modelProvider));
    }

    /**
     * Those methods are called by the protocol stage thread pool, this means that execution MUST be done in a short delay,
     * * if you need to do long time processing use a dedicated thread pool.
     *
     * @param registration -
     */
    @Override
    public void onAwakeDev(Registration registration) {
        log.debug("[{}] [{}] Received endpoint awake event", registration.getId(), registration.getEndpoint());
        clientContext.awake(clientContext.getClientByEndpoint(registration.getEndpoint()));
    }

    /**
     * #1 clientOnlyObserveAfterConnect == true
     * - Only Observe Request to the client marked as observe from the profile configuration.
     * #2. clientOnlyObserveAfterConnect == false
     * - Read Request to the client after registration to read all resource values for all objects
     * - then Observe Request to the client marked as observe from the profile configuration.
     *
     * @param lwM2MClient - object with All parameters off client
     */
    private void initClientTelemetry(LwM2mClient lwM2MClient) {
        Lwm2mDeviceProfileTransportConfiguration profile = clientContext.getProfile(lwM2MClient.getProfileId());
        Set<String> supportedObjects = clientContext.getSupportedIdVerInClient(lwM2MClient);
        if (supportedObjects != null && supportedObjects.size() > 0) {
            this.sendReadRequests(lwM2MClient, profile, supportedObjects);
            this.sendObserveRequests(lwM2MClient, profile, supportedObjects);
            this.sendWriteAttributeRequests(lwM2MClient, profile, supportedObjects);
//            Removed. Used only for debug.
//            this.sendDiscoverRequests(lwM2MClient, profile, supportedObjects);
        }
    }

    private void sendReadRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        try {
            Set<String> targetIds = new HashSet<>(profile.getObserveAttr().getAttribute());
            targetIds.addAll(profile.getObserveAttr().getTelemetry());
            targetIds = diffSets(profile.getObserveAttr().getObserve(), targetIds);
            targetIds = targetIds.stream().filter(target -> isSupportedTargetId(supportedObjects, target)).collect(Collectors.toSet());

            CountDownLatch latch = new CountDownLatch(targetIds.size());
            targetIds.forEach(versionedId -> sendReadRequest(lwM2MClient, versionedId,
                    new TbLwM2MLatchCallback<>(latch, new TbLwM2MReadCallback(this, logService, lwM2MClient, versionedId))));
            latch.await(config.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("[{}] Failed to await Read requests!", lwM2MClient.getEndpoint(), e);
        } catch (Exception e) {
            log.error("[{}] Failed to process read requests!", lwM2MClient.getEndpoint(), e);
            logService.log(lwM2MClient, "Failed to process read requests. Possible profile misconfiguration.");
        }
    }

    private void sendObserveRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        try {
            Set<String> targetIds = profile.getObserveAttr().getObserve();
            targetIds = targetIds.stream().filter(target -> isSupportedTargetId(supportedObjects, target)).collect(Collectors.toSet());

            CountDownLatch latch = new CountDownLatch(targetIds.size());
            targetIds.forEach(targetId -> sendObserveRequest(lwM2MClient, targetId,
                    new TbLwM2MLatchCallback<>(latch, new TbLwM2MObserveCallback(this, logService, lwM2MClient, targetId))));

            latch.await(config.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("[{}] Failed to await Observe requests!", lwM2MClient.getEndpoint(), e);
        } catch (Exception e) {
            log.error("[{}] Failed to process observe requests!", lwM2MClient.getEndpoint(), e);
            logService.log(lwM2MClient, "Failed to process observe requests. Possible profile misconfiguration.");
        }
    }

    private void sendWriteAttributeRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        try {
            Map<String, ObjectAttributes> attributesMap = profile.getObserveAttr().getAttributeLwm2m();
            attributesMap = attributesMap.entrySet().stream().filter(target -> isSupportedTargetId(supportedObjects, target.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            attributesMap.forEach((targetId, params) -> sendWriteAttributesRequest(lwM2MClient, targetId, params));
        } catch (Exception e) {
            log.error("[{}] Failed to process write attribute requests!", lwM2MClient.getEndpoint(), e);
            logService.log(lwM2MClient, "Failed to process write attribute requests. Possible profile misconfiguration.");
        }
    }

    private void sendReadRequest(LwM2mClient lwM2MClient, String versionedId) {
        sendReadRequest(lwM2MClient, versionedId, new TbLwM2MReadCallback(this, logService, lwM2MClient, versionedId));
    }

    private void sendReadRequest(LwM2mClient lwM2MClient, String versionedId, DownlinkRequestCallback<ReadRequest, ReadResponse> callback) {
        TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
        defaultLwM2MDownlinkMsgHandler.sendReadRequest(lwM2MClient, request, callback);
    }

    private void sendObserveRequest(LwM2mClient lwM2MClient, String versionedId) {
        sendObserveRequest(lwM2MClient, versionedId, new TbLwM2MObserveCallback(this, logService, lwM2MClient, versionedId));
    }

    private void sendObserveRequest(LwM2mClient lwM2MClient, String versionedId, DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback) {
        TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
        defaultLwM2MDownlinkMsgHandler.sendObserveRequest(lwM2MClient, request, callback);
    }

    private void sendWriteAttributesRequest(LwM2mClient lwM2MClient, String targetId, ObjectAttributes params) {
        TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(targetId).attributes(params).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
        defaultLwM2MDownlinkMsgHandler.sendWriteAttributesRequest(lwM2MClient, request, new TbLwM2MWriteAttributesCallback(logService, lwM2MClient, targetId));
    }

    private void sendCancelObserveRequest(String versionedId, LwM2mClient client) {
        TbLwM2MCancelObserveRequest request = TbLwM2MCancelObserveRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(client)).build();
        defaultLwM2MDownlinkMsgHandler.sendCancelObserveRequest(client, request, new TbLwM2MCancelObserveCallback(logService, client, versionedId));
    }

    private void updateObjectResourceValue(LwM2mClient client, LwM2mObject lwM2mObject, String pathIdVer, int code) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer));
        lwM2mObject.getInstances().forEach((instanceId, instance) -> {
            String pathInstance = pathIds.toString() + "/" + instanceId;
            this.updateObjectInstanceResourceValue(client, instance, pathInstance, code);
        });
    }

    private void updateObjectInstanceResourceValue(LwM2mClient client, LwM2mObjectInstance lwM2mObjectInstance, String pathIdVer, int code) {
        lwM2mObjectInstance.getResources().forEach((resourceId, resource) -> {
            String pathRez = pathIdVer + "/" + resourceId;
            this.updateResourcesValue(client, resource, pathRez, Mode.UPDATE, code);
        });
    }

    /**
     * Sending observe value of resources to thingsboard
     * #1 Return old Value Resource from LwM2MClient
     * #2 Update new Resources (replace old Resource Value on new Resource Value)
     * #3 If fr_update -> UpdateFirmware
     * #4 updateAttrTelemetry
     *  @param lwM2MClient   - Registration LwM2M Client
     * @param lwM2mResource - LwM2mSingleResource response.getContent()
     * @param stringPath          - resource
     * @param mode          - Replace, Update
     */
    private void updateResourcesValue(LwM2mClient lwM2MClient, LwM2mResource lwM2mResource, String stringPath, Mode mode, int code) {
        Registration registration = lwM2MClient.getRegistration();
        String path = convertObjectIdToVersionedId(stringPath, lwM2MClient);
        if (lwM2MClient.saveResourceValue(path, lwM2mResource, modelProvider, mode)) {
            if (path.equals(convertObjectIdToVersionedId(FW_NAME_ID, lwM2MClient))) {
                otaService.onCurrentFirmwareNameUpdate(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_3_VER_ID, lwM2MClient))) {
                otaService.onCurrentFirmwareVersion3Update(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_VER_ID, lwM2MClient))) {
                otaService.onCurrentFirmwareVersionUpdate(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_STATE_ID, lwM2MClient))) {
                otaService.onCurrentFirmwareStateUpdate(lwM2MClient, (Long) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_RESULT_ID, lwM2MClient))) {
                otaService.onCurrentFirmwareResultUpdate(lwM2MClient, (Long) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(FW_DELIVERY_METHOD, lwM2MClient))) {
                otaService.onCurrentFirmwareDeliveryMethodUpdate(lwM2MClient, (Long) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(SW_NAME_ID, lwM2MClient))) {
                otaService.onCurrentSoftwareNameUpdate(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(SW_VER_ID, lwM2MClient))) {
                otaService.onCurrentSoftwareVersionUpdate(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(SW_3_VER_ID, lwM2MClient))) {
                otaService.onCurrentSoftwareVersion3Update(lwM2MClient, (String) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(SW_STATE_ID, lwM2MClient))) {
                otaService.onCurrentSoftwareStateUpdate(lwM2MClient, (Long) lwM2mResource.getValue());
            } else if (path.equals(convertObjectIdToVersionedId(SW_RESULT_ID, lwM2MClient))) {
                otaService.onCurrentSoftwareResultUpdate(lwM2MClient, (Long) lwM2mResource.getValue());
            }
            if (ResponseCode.BAD_REQUEST.getCode() > code) {
                this.updateAttrTelemetry(registration, path, null);
            }
        } else {
            log.error("Fail update path [{}] Resource [{}]", path, lwM2mResource);
        }
    }
    private void updateResourcesValueWithTs(LwM2mClient lwM2MClient, LwM2mResource lwM2mResource, String stringPath, Mode mode, Instant ts) {
        Registration registration = lwM2MClient.getRegistration();
        String path = convertObjectIdToVersionedId(stringPath, lwM2MClient);
        if (lwM2MClient.saveResourceValue(path, lwM2mResource, modelProvider, mode)) {
            this.updateAttrTelemetry(registration, path, ts);
        } else {
            log.error("Fail update path [{}] Resource [{}] with ts.", path, lwM2mResource);
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
    public void updateAttrTelemetry(Registration registration, String path, Instant ts) {
        log.trace("UpdateAttrTelemetry paths [{}]", path);
        try {
            ResultsAddKeyValueProto results = this.getParametersFromProfile(registration, path);
            SessionInfoProto sessionInfo = this.getSessionInfoOrCloseSession(registration);
            if (results != null && sessionInfo != null) {
                if (results.getResultAttributes().size() > 0) {
                    log.trace("UpdateAttribute paths [{}] value [{}]", path, results.getResultAttributes().get(0).toString());
                    this.helper.sendParametersOnThingsboardAttribute(results.getResultAttributes(), sessionInfo);
                }
                if (results.getResultTelemetries().size() > 0) {
                    log.trace("UpdateTelemetry paths [{}] value [{}] ts [{}]", path, results.getResultTelemetries().get(0).toString(), ts == null ? "null" : ts.toEpochMilli());
                    this.helper.sendParametersOnThingsboardTelemetry(results.getResultTelemetries(), sessionInfo, null, ts);
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
        var oldProfile = clientContext.getProfile(lwM2MClient.getProfileId());
        deviceProfileOpt.ifPresent(deviceProfile -> this.onDeviceProfileUpdate(Collections.singletonList(lwM2MClient), oldProfile, deviceProfile));
        lwM2MClient.onDeviceUpdate(device, deviceProfileOpt);
    }

    /**
     * //     * @param attributes   - new JsonObject
     * //     * @param telemetry    - new JsonObject
     *
     * @param registration - Registration LwM2M Client
     * @param path         -
     */
    private ResultsAddKeyValueProto getParametersFromProfile(Registration registration, String path) {
        if (!path.isEmpty()) {
            ResultsAddKeyValueProto results = new ResultsAddKeyValueProto();
            var profile = clientContext.getProfile(registration);
            List<TransportProtos.KeyValueProto> resultAttributes = new ArrayList<>();
            profile.getObserveAttr().getAttribute().forEach(pathIdVer -> {
                if (path.equals(pathIdVer)) {
                    TransportProtos.KeyValueProto kvAttr = this.getKvToThingsBoard(pathIdVer, registration);
                    if (kvAttr != null) {
                        resultAttributes.add(kvAttr);
                    }
                }
            });
            List<TransportProtos.KeyValueProto> resultTelemetries = new ArrayList<>();
            profile.getObserveAttr().getTelemetry().forEach(pathIdVer -> {
                if (path.contains(pathIdVer)) {
                    TransportProtos.KeyValueProto kvAttr = this.getKvToThingsBoard(pathIdVer, registration);
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

    private TransportProtos.KeyValueProto getKvToThingsBoard(String pathIdVer, Registration registration) {
        LwM2mClient lwM2MClient = this.clientContext.getClientByEndpoint(registration.getEndpoint());
        Map<String, String> names = clientContext.getProfile(lwM2MClient.getProfileId()).getObserveAttr().getKeyName();
        if (names != null && names.containsKey(pathIdVer)) {
            String resourceName = names.get(pathIdVer);
            if (resourceName != null && !resourceName.isEmpty()) {
                try {
                    LwM2mResource resourceValue = LwM2MTransportUtil.getResourceValueFromLwM2MClient(lwM2MClient, pathIdVer);
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
                                Object val = this.converter.convertValue(v.getValue(), finalCurrentType, expectedType,
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
    public void onWriteResponseOk(LwM2mClient client, String path, WriteRequest request, int code) {
        if (request.getNode() instanceof LwM2mResource) {
            this.updateResourcesValue(client, ((LwM2mResource) request.getNode()), path, request.isReplaceRequest() ? Mode.REPLACE : Mode.UPDATE, code);
        } else if (request.getNode() instanceof LwM2mObjectInstance) {
            ((LwM2mObjectInstance) request.getNode()).getResources().forEach((resId, resource) -> {
                this.updateResourcesValue(client, resource, path + "/" + resId, request.isReplaceRequest() ? Mode.REPLACE : Mode.UPDATE, code);
            });
        }
        if (request.getNode() instanceof LwM2mResource || request.getNode() instanceof LwM2mObjectInstance) {
            clientContext.update(client);
        }
    }

    @Override
    public void onCreateResponseOk(LwM2mClient client, String path, CreateRequest request) {
        if (request.getObjectInstances() != null && request.getObjectInstances().size() > 0) {
            request.getObjectInstances().forEach(instance ->
                    instance.getResources()
            );
            clientContext.update(client);
        }
    }

    @Override
    public void onWriteCompositeResponseOk(LwM2mClient client, WriteCompositeRequest request, int code) {
        log.trace("ReadCompositeResponse: [{}]", request.getNodes());
        request.getNodes().forEach((k, v) -> {
            if (v instanceof LwM2mSingleResource) {
                this.updateResourcesValue(client, (LwM2mResource) v, k.toString(), Mode.REPLACE, code);
            } else {
                LwM2mResourceInstance resourceInstance = (LwM2mResourceInstance) v;
                LwM2mMultipleResource multipleResource = new LwM2mMultipleResource(((LwM2mResourceInstance) v).getId(), resourceInstance.getType(), resourceInstance);
                this.updateResourcesValue(client, multipleResource, k.toString(), Mode.REPLACE, code);
            }
        });
    }

    //TODO: review and optimize the logic to minimize number of the requests to device.
    private void onDeviceProfileUpdate(List<LwM2mClient> clients, Lwm2mDeviceProfileTransportConfiguration oldProfile, DeviceProfile deviceProfile) {
        if (clientContext.profileUpdate(deviceProfile) != null) {
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
            Set<String> newObjectsToCancelRead = new HashSet<>();

            if (!attributeSetOld.equals(attributeSetNew)) {
                newObjectsToRead.addAll(diffSets(attributeSetOld, attributeSetNew));
                newObjectsToCancelRead.addAll(diffSets(attributeSetNew, attributeSetOld));

            }
            if (!telemetrySetOld.equals(telemetrySetNew)) {
                newObjectsToRead.addAll(diffSets(telemetrySetOld, telemetrySetNew));
                newObjectsToCancelRead.addAll(diffSets(telemetrySetNew, telemetrySetOld));
            }
            if (!keyNameOld.equals(keyNameNew)) {
                ParametersAnalyzeResult keyNameChange = this.getAnalyzerKeyName(keyNameOld, keyNameNew);
                newObjectsToRead.addAll(keyNameChange.getPathPostParametersAdd());
            }

            ParametersAnalyzeResult analyzerParameters = getAttributesAnalyzer(attributeLwm2mOld, attributeLwm2mNew);

            clients.forEach(client -> {
                LwM2MModelConfig modelConfig = new LwM2MModelConfig(client.getEndpoint());
                modelConfig.getToRead().addAll(diffSets(observeToAdd, newObjectsToRead));
                modelConfig.getToCancelRead().addAll(newObjectsToCancelRead);
                modelConfig.getToCancelObserve().addAll(observeToRemove);
                modelConfig.getToObserve().addAll(observeToAdd);

                Set<String> clientObjects = clientContext.getSupportedIdVerInClient(client);
                Set<String> pathToAdd = analyzerParameters.getPathPostParametersAdd().stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
                        .collect(Collectors.toUnmodifiableSet());
                modelConfig.getAttributesToAdd().putAll(pathToAdd.stream().collect(Collectors.toMap(t -> t, attributeLwm2mNew::get)));

                Set<String> pathToRemove = analyzerParameters.getPathPostParametersDel().stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
                        .collect(Collectors.toUnmodifiableSet());
                modelConfig.getAttributesToRemove().addAll(pathToRemove);

                modelConfigService.sendUpdates(client, modelConfig);
            });

            // update value in fwInfo
            OtherConfiguration newLwM2mSettings = newProfile.getClientLwM2mSettings();
            OtherConfiguration oldLwM2mSettings = oldProfile.getClientLwM2mSettings();
            if (!newLwM2mSettings.getFwUpdateStrategy().equals(oldLwM2mSettings.getFwUpdateStrategy())
                    || (StringUtils.isNotEmpty(newLwM2mSettings.getFwUpdateResource()) &&
                    !newLwM2mSettings.getFwUpdateResource().equals(oldLwM2mSettings.getFwUpdateResource()))) {
                clients.forEach(lwM2MClient -> otaService.onFirmwareStrategyUpdate(lwM2MClient, newLwM2mSettings));
            }

            if (!newLwM2mSettings.getSwUpdateStrategy().equals(oldLwM2mSettings.getSwUpdateStrategy())
                    || (StringUtils.isNotEmpty(newLwM2mSettings.getSwUpdateResource()) &&
                    !newLwM2mSettings.getSwUpdateResource().equals(oldLwM2mSettings.getSwUpdateResource()))) {
                clients.forEach(lwM2MClient -> otaService.onCurrentSoftwareStrategyUpdate(lwM2MClient, newLwM2mSettings));
            }
        }
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

    private ParametersAnalyzeResult getAttributesAnalyzer(Map<String, ObjectAttributes> attributeLwm2mOld, Map<String, ObjectAttributes> attributeLwm2mNew) {
        ParametersAnalyzeResult analyzerParameters = new ParametersAnalyzeResult();
        Set<String> pathOld = attributeLwm2mOld.keySet();
        Set<String> pathNew = attributeLwm2mNew.keySet();
        analyzerParameters.setPathPostParametersAdd(pathNew
                .stream().filter(p -> !pathOld.contains(p)).collect(Collectors.toSet()));
        analyzerParameters.setPathPostParametersDel(pathOld
                .stream().filter(p -> !pathNew.contains(p)).collect(Collectors.toSet()));
        Set<String> pathCommon = pathNew
                .stream().filter(pathOld::contains).collect(Collectors.toSet());
        Set<String> pathCommonChange = pathCommon
                .stream().filter(p -> !attributeLwm2mOld.get(p).equals(attributeLwm2mNew.get(p))).collect(Collectors.toSet());
        analyzerParameters.getPathPostParametersAdd().addAll(pathCommonChange);
        return analyzerParameters;
    }

    private void compareAndSetWriteAttributes(LwM2mClient client, ParametersAnalyzeResult analyzerParameters, Map<String, ObjectAttributes> lwm2mAttributesNew, LwM2MModelConfig modelConfig) {

    }

    /**
     * @param sessionInfo
     * @param updateCredentials - Credentials include config only security Client (without config attr/telemetry...)
     */
    @Override
    public void onToTransportUpdateCredentials(SessionInfoProto sessionInfo, TransportProtos.ToTransportUpdateCredentialsProto updateCredentials) {
        log.info("[{}] updateCredentials", sessionInfo);
        clearAndUnregister(clientContext.getClientBySessionInfo(sessionInfo));
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
        if (sessionInfo != null && !transportService.hasSession(sessionInfo)) {
            sessionManager.register(sessionInfo);
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
    @Override
    public void initAttributes(LwM2mClient lwM2MClient, boolean logFailedUpdateOfNonChangedValue) {
        Map<String, String> keyNamesMap = this.getNamesFromProfileForSharedAttributes(lwM2MClient);
        if (!keyNamesMap.isEmpty()) {
            Set<String> keysToFetch = new HashSet<>(keyNamesMap.values());
            keysToFetch.removeAll(OtaPackageUtil.ALL_FW_ATTRIBUTE_KEYS);
            keysToFetch.removeAll(OtaPackageUtil.ALL_SW_ATTRIBUTE_KEYS);
            DonAsynchron.withCallback(attributesService.getSharedAttributes(lwM2MClient, keysToFetch),
                    v -> attributesService.onAttributesUpdate(lwM2MClient, v, logFailedUpdateOfNonChangedValue),
                    t -> log.error("[{}] Failed to get attributes", lwM2MClient.getEndpoint(), t),
                    executor);
        }
    }

    public LwM2mClientContext getClientContext() {
        return this.clientContext;
    }

    private Map<String, String> getNamesFromProfileForSharedAttributes(LwM2mClient lwM2MClient) {
        Lwm2mDeviceProfileTransportConfiguration profile = clientContext.getProfile(lwM2MClient.getProfileId());
        return profile.getObserveAttr().getKeyName();
    }

    public LwM2MTransportServerConfig getConfig() {
        return this.config;
    }

    private void reportActivitySubscription(SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(true)
                .setRpcSubscription(true)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
    }

    private void clearAndUnregister(LwM2mClient client) {
        client.lock();
        try {
            Registration registration = client.getRegistration();
            doUnReg(registration, client);
            securityStore.remove(registration.getEndpoint(), registration.getId());
            registrationStore.removeRegistration(registration.getId());
        } finally {
            client.unlock();
        }
    }

    private void tryAwake(LwM2mClient lwM2MClient) {
        if (clientContext.awake(lwM2MClient)) {
            // clientContext.awake calls clientContext.update
            log.debug("[{}] Device is awake", lwM2MClient.getEndpoint());
        } else {
            clientContext.update(lwM2MClient);
        }
    }

}
