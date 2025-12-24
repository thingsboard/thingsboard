/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy;
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
import org.thingsboard.server.transport.lwm2m.server.client.ResourceUpdateResult;
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
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MObserveCompositeCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MObserveCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MReadCompositeCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MReadCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfig;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfigService;
import org.thingsboard.server.transport.lwm2m.server.model.ParametersAnalyzeResult;
import org.thingsboard.server.transport.lwm2m.server.model.ParametersObserveAnalyzeResult;
import org.thingsboard.server.transport.lwm2m.server.model.ParametersUpdateAnalyzeResult;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_BY_OBJECT;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;
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
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.areArraysStringEqual;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertOtaUpdateValueToString;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.groupByObjectIdVersionedIds;


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
                String msgLogService = String.format("""
                %s: Endpoint [%s] Client registered with registration id: [%s] LwM2mVersion: [%s], SupportedObjectIdVer [%s] QueueMode [%s], BindingMode %s
                """, LOG_LWM2M_INFO,  registration.getEndpoint(), registration.getId(), registration.getLwM2mVersion(), registration.getSupportedObject(), registration.getQueueMode(), registration.getBindingMode());
                logService.log(lwM2MClient, msgLogService);
                log.debug(msgLogService);
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
                log.error("Endpoint [{}], Error Unable registration: [{}].", registration.getEndpoint(), t.getMessage(), t);
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
                ResourceUpdateResult updateResource = new ResourceUpdateResult(lwM2MClient);
                int responseCode = response.getCode().getCode();
                if (content instanceof LwM2mObject) {
                    this.updateObjectResourceValue(updateResource, (LwM2mObject) content, path, responseCode);
                } else if (content instanceof LwM2mObjectInstance) {
                    this.updateObjectInstanceResourceValue(updateResource, (LwM2mObjectInstance) content, path, responseCode);
                } else if (content instanceof LwM2mResource) {
                    this.updateResourcesValue(updateResource, (LwM2mResource) content, path, Mode.UPDATE, responseCode);
                }
                this.updateAttrTelemetry(updateResource, null);
            }
            tryAwake(lwM2MClient);
        }
    }

    public void onUpdateValueAfterReadCompositeResponse(Registration registration, ReadCompositeResponse response) {
        log.trace("ReadCompositeResponse before onUpdateValueAfterReadCompositeResponse: [{}]", response);
        if (response.getContent() != null) {
            LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
            ResourceUpdateResult updateResource = new ResourceUpdateResult(lwM2MClient);
            response.getContent().forEach((k, v) -> {
                if (v != null) {
                    int responseCode = response.getCode().getCode();
                    if (v instanceof LwM2mObject) {
                        this.updateObjectResourceValue(updateResource, (LwM2mObject) v, k.toString(), responseCode);
                    } else if (v instanceof LwM2mObjectInstance) {
                        this.updateObjectInstanceResourceValue(updateResource, (LwM2mObjectInstance) v, k.toString(), responseCode);
                    } else if (v instanceof LwM2mResource) {
                        this.updateResourcesValue(updateResource, (LwM2mResource) v, k.toString(), Mode.UPDATE, responseCode);
                    }
                } else {
                    this.onErrorObservation(registration, k + ": value in composite response is null");
                }
            });
            this.updateAttrTelemetry(updateResource, null);
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
                ObjectModel objectModelVersion = lwM2MClient.getObjectModel(convertObjectIdToVersionedId(path.toString(), lwM2MClient), modelProvider);
                if (objectModelVersion != null) {
                    ResourceUpdateResult updateResource = new ResourceUpdateResult(lwM2MClient);
                    if (node instanceof LwM2mObject) {
                        this.updateObjectResourceValue(updateResource, (LwM2mObject) node, path.toString(), 0);
                    } else if (node instanceof LwM2mObjectInstance) {
                        this.updateObjectInstanceResourceValue(updateResource, (LwM2mObjectInstance) node, path.toString(), 0);
                    } else if (node instanceof LwM2mResource) {
                        this.updateResourcesValue(updateResource, (LwM2mResource) node, path.toString(), Mode.UPDATE, 0);
                    }
                    this.updateAttrTelemetry(updateResource, ts);
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
            if (!clients.isEmpty()) {
                var oldProfile = clientContext.getProfile(clients.get(0).getRegistration());
                if (oldProfile != null) this.onDeviceProfileUpdate(clients, oldProfile, deviceProfile);
            }
        } catch (Exception e) {
            log.warn("[{}] failed to update profile: {} [{}]", deviceProfile.getId(), e.getMessage(), deviceProfile);
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
            log.warn("[{}] failed to update device: {} [{}]", device.getId(), e.getMessage(), device);
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
        Lwm2mDeviceProfileTransportConfiguration profile = clientContext.getProfile(lwM2MClient.getRegistration());
        if (profile != null) {
            Set<String> supportedObjects = clientContext.getSupportedIdVerInClient(lwM2MClient);
            if (supportedObjects != null && !supportedObjects.isEmpty()) {
                this.sendInitObserveRequests(lwM2MClient, profile, supportedObjects);
                this.sendReadRequests(lwM2MClient, profile, supportedObjects);
                this.sendWriteAttributeRequests(lwM2MClient, profile, supportedObjects);
//            Removed. Used only for debug.
//            this.sendDiscoverRequests(lwM2MClient, profile, supportedObjects);
            }
        } else {
            log.warn("[{}] Failed to process initClientTelemetry! Profile is null. Update procedure may not have completed after reboot yet", lwM2MClient.getEndpoint());
            logService.log(lwM2MClient, "Failed to process initClientTelemetry. Profile is null. Update procedure may not have completed after reboot yet");
        }
    }

    private void sendReadRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        try {
            Set<String> targetIds = new HashSet<>(profile.getObserveAttr().getAttribute());
            Boolean initAttrTelAsObsStrategy = profile.getObserveAttr().getInitAttrTelAsObsStrategy();
            targetIds.addAll(profile.getObserveAttr().getTelemetry());
            targetIds = diffSets(profile.getObserveAttr().getObserve(), targetIds);
            targetIds = targetIds.stream().filter(target -> isSupportedTargetId(supportedObjects, target)).collect(Collectors.toSet());
            if (!targetIds.isEmpty()) {
                TelemetryObserveStrategy observeStrategy = profile.getObserveAttr().getObserveStrategy();
                long timeoutMs = config.getTimeout();
                if (initAttrTelAsObsStrategy && observeStrategy != SINGLE) {
                    switch (observeStrategy) {
                        case COMPOSITE_ALL -> {
                            sendReadCompositeRequest(lwM2MClient, targetIds.toArray(new String[0]));
                        }
                        case COMPOSITE_BY_OBJECT -> {
                            Map<Integer, String[]> versionedObjectIds = groupByObjectIdVersionedIds(targetIds);
                            versionedObjectIds.forEach((k, v) -> sendReadCompositeRequest(lwM2MClient, v));
                        }
                    }
                } else {
                    CountDownLatch latch = new CountDownLatch(targetIds.size());
                    targetIds.forEach(versionedId -> sendReadSingleRequest(lwM2MClient, versionedId,
                            new TbLwM2MLatchCallback<>(latch, new TbLwM2MReadCallback(this, logService, lwM2MClient, versionedId))));
                    latch.await(timeoutMs, TimeUnit.MILLISECONDS);
                }
            }
        } catch (InterruptedException e) {
            log.error("[{}] Failed to await Read requests!", lwM2MClient.getEndpoint(), e);
        } catch (Exception e) {
            log.error("[{}] Failed to process read requests!", lwM2MClient.getEndpoint(), e);
            logService.log(lwM2MClient, "Failed to process read requests. Possible profile misconfiguration.");
        }
    }

    private void sendInitObserveRequests(LwM2mClient lwM2MClient, Lwm2mDeviceProfileTransportConfiguration profile, Set<String> supportedObjects) {
        try {
            Set<String> targetIds = profile.getObserveAttr().getObserve();
            targetIds = targetIds.stream().filter(target -> isSupportedTargetId(supportedObjects, target)).collect(Collectors.toSet());
            if (!targetIds.isEmpty()) {
                TelemetryObserveStrategy observeStrategy = profile.getObserveAttr().getObserveStrategy();
                long timeoutMs = config.getTimeout();
                switch (observeStrategy) {
                    case SINGLE -> {
                        CountDownLatch latch = new CountDownLatch(targetIds.size());
                        targetIds.forEach(targetId -> sendObserveRequest(
                                lwM2MClient, targetId,
                                new TbLwM2MLatchCallback<>(latch, new TbLwM2MObserveCallback(this, logService, lwM2MClient, targetId))
                        ));
                        boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
                        if (!completed) log.trace("[{}] Timeout occurred during SINGLE observe init", lwM2MClient.getEndpoint());
                    }
                    case COMPOSITE_ALL -> {
                        sendObserveCompositeRequest(lwM2MClient, targetIds.toArray(new String[0]));
                    }
                    case COMPOSITE_BY_OBJECT -> {
                        Map<Integer, String[]> versionedObjectIds = groupByObjectIdVersionedIds(targetIds);
                        versionedObjectIds.forEach((k, v) -> sendObserveCompositeRequest(lwM2MClient, v));
                    }
                }
            }
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

    private void sendReadSingleRequest(LwM2mClient lwM2MClient, String versionedId, DownlinkRequestCallback<ReadRequest, ReadResponse> callback) {
        TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
        defaultLwM2MDownlinkMsgHandler.sendReadRequest(lwM2MClient, request, callback);
    }

    private void sendReadCompositeRequest(LwM2mClient lwM2MClient, String[] versionedIds) {
        TbLwM2MReadCompositeRequest request = TbLwM2MReadCompositeRequest.builder().versionedIds(versionedIds).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
        var mainCallback = new TbLwM2MReadCompositeCallback(this, logService, lwM2MClient, versionedIds);
        defaultLwM2MDownlinkMsgHandler.sendReadCompositeRequest(lwM2MClient, request, mainCallback );
    }

    private void sendObserveRequest(LwM2mClient lwM2MClient, String versionedId, DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback) {
        TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
        defaultLwM2MDownlinkMsgHandler.sendObserveRequest(lwM2MClient, request, callback);
    }

    private void sendObserveCompositeRequest(LwM2mClient lwM2MClient, String[] versionedIds) {

        TbLwM2MObserveCompositeRequest request = TbLwM2MObserveCompositeRequest.builder().versionedIds(versionedIds).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
        var mainCallback = new TbLwM2MObserveCompositeCallback(this, logService, lwM2MClient, versionedIds);
        defaultLwM2MDownlinkMsgHandler.sendObserveCompositeRequest(lwM2MClient, request, mainCallback);
    }

    private void sendWriteAttributesRequest(LwM2mClient lwM2MClient, String targetId, ObjectAttributes params) {
        TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(targetId).attributes(params).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
        defaultLwM2MDownlinkMsgHandler.sendWriteAttributesRequest(lwM2MClient, request, new TbLwM2MWriteAttributesCallback(logService, lwM2MClient, targetId));
    }

    private void sendCancelObserveRequest(String versionedId, LwM2mClient client) {
        TbLwM2MCancelObserveRequest request = TbLwM2MCancelObserveRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(client)).build();
        defaultLwM2MDownlinkMsgHandler.sendCancelObserveRequest(client, request, new TbLwM2MCancelObserveCallback(logService, client, versionedId));
    }

    private void updateObjectResourceValue(ResourceUpdateResult updateResource, LwM2mObject lwM2mObject, String pathIdVer, int code) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer));
        lwM2mObject.getInstances().forEach((instanceId, instance) -> {
            String pathInstance = pathIds.toString() + "/" + instanceId;
            this.updateObjectInstanceResourceValue(updateResource, instance, pathInstance, code);
        });
    }

    private void updateObjectInstanceResourceValue(ResourceUpdateResult updateResource, LwM2mObjectInstance lwM2mObjectInstance, String pathIdVer, int code) {
        lwM2mObjectInstance.getResources().forEach((resourceId, resource) -> {
            String pathRez = pathIdVer + "/" + resourceId;
            this.updateResourcesValue(updateResource, resource, pathRez, Mode.UPDATE, code);
        });
    }

    /**
     * Sending observe value of resources to thingsboard
     * #1 Return old Value Resource from LwM2MClient
     * #2 Update new Resources (replace old Resource Value on new Resource Value)
     * #3 If fr_update -> UpdateFirmware
     * #4 updateAttrTelemetry
     * @param updateResource   - result update resource by LwM2M Client
     * @param lwM2mResource - LwM2mSingleResource response.getContent()
     * @param stringPath          - resource
     * @param mode          - Replace, Update
     */
    private void updateResourcesValue(ResourceUpdateResult updateResource, LwM2mResource lwM2mResource, String stringPath, Mode mode, int code) {
        LwM2mClient lwM2MClient = updateResource.getLwM2MClient();
        String path = convertObjectIdToVersionedId(stringPath, lwM2MClient);
        if (path != null && lwM2MClient.saveResourceValue(path, lwM2mResource, modelProvider, mode)) {
            this.updateOtaResource(lwM2MClient, lwM2mResource, path);
            if (ResponseCode.BAD_REQUEST.getCode() > code) {
                updateResource.getPaths().add(path);
            }
        } else {
            log.error("Fail update path [{}] Resource [{}]", path, lwM2mResource);
        }
    }

    private void updateOtaResource(LwM2mClient lwM2MClient, LwM2mResource lwM2mResource, String path) {
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
    }

    /**
     * send Attribute and Telemetry to Thingsboard
     * #1 - get AttrName/TelemetryName with value from LwM2MClient:
     * -- resourceId == path from LwM2MClientProfile.postAttributeProfile/postTelemetryProfile/postObserveProfile
     * -- AttrName/TelemetryName == resourceName from ModelObject.objectModel, value from ModelObject.instance.resource(resourceId)
     * #2 - set Attribute/Telemetry
     *
     * @param updateResource - updateResource resource of LwM2M Client
     */
    public void updateAttrTelemetry(ResourceUpdateResult updateResource, Instant ts) {
        log.trace("UpdateAttrTelemetry paths [{}]", updateResource.getPaths());
        try {
            ResultsAddKeyValueProto results = this.getParametersFromProfile(updateResource);
            SessionInfoProto sessionInfo = this.getSessionInfoOrCloseSession(updateResource.getLwM2MClient().getRegistration());
            if (results != null && sessionInfo != null) {
                if (results.getResultAttributes().size() > 0) {
                    log.trace("UpdateAttribute paths [{}] value [{}]", updateResource.getPaths(), results.getResultAttributes().get(0).toString());
                    this.helper.sendParametersOnThingsboardAttribute(results.getResultAttributes(), sessionInfo);
                }
                if (results.getResultTelemetries().size() > 0) {
                    log.trace("UpdateTelemetry paths [{}] value [{}] ts [{}]", updateResource.getPaths(), results.getResultTelemetries().get(0).toString(), ts == null ? "null" : ts.toEpochMilli());
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

    private void onDeviceUpdate(LwM2mClient lwM2MClient, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        var oldProfile = clientContext.getProfile(lwM2MClient.getRegistration());
        if (oldProfile != null) {
            deviceProfileOpt.ifPresent(deviceProfile -> this.onDeviceProfileUpdate(Collections.singletonList(lwM2MClient), oldProfile, deviceProfile));
            lwM2MClient.onDeviceUpdate(device, deviceProfileOpt);
        }
    }

    /**
     * //     * @param attributes   - new JsonObject
     * //     * @param telemetry    - new JsonObject
     *
     * @param updateResource - updateResource resource of LwM2M Client
     */
    private ResultsAddKeyValueProto getParametersFromProfile(ResourceUpdateResult updateResource) {
        Registration registration = updateResource.getLwM2MClient().getRegistration();
        Set<String> paths = updateResource.getPaths();
        ResultsAddKeyValueProto results = new ResultsAddKeyValueProto();
        var profile = clientContext.getProfile(registration);
        if (profile != null) {
            List<TransportProtos.KeyValueProto> resultAttributes = new ArrayList<>();
            Set<String> attributes = profile.getObserveAttr().getAttribute().stream()
                    .filter(paths::contains)
                    .collect(Collectors.toSet());
            if (!attributes.isEmpty()) {
                attributes.stream()
                        .map(attr -> this.getKvToThingsBoard(attr, registration))
                        .filter(Objects::nonNull)
                        .forEach(resultAttributes::add);
            }
            List<TransportProtos.KeyValueProto> resultTelemetries = new ArrayList<>();
            Set<String> telemetries = profile.getObserveAttr().getTelemetry().stream()
                    .filter(paths::contains)
                    .collect(Collectors.toSet());
            if (!telemetries.isEmpty()) {
                telemetries.stream()
                        .map(telemetry -> this.getKvToThingsBoard(telemetry, registration))
                        .filter(Objects::nonNull)
                        .forEach(resultTelemetries::add);
            }
            if (resultAttributes.size() > 0) {
                results.setResultAttributes(resultAttributes);
            }
            if (resultTelemetries.size() > 0) {
                results.setResultTelemetries(resultTelemetries);
            }
        }
        return results;
    }

    private TransportProtos.KeyValueProto getKvToThingsBoard(String pathIdVer, Registration registration) {
        LwM2mClient lwM2MClient = this.clientContext.getClientByEndpoint(registration.getEndpoint());
        var clientProfile = clientContext.getProfile(lwM2MClient.getRegistration());
        if (clientProfile == null) return null;
        Map<String, String> names = clientProfile.getObserveAttr().getKeyName();
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
    public void onWriteResponseOk(LwM2mClient lwM2MClient, String path, WriteRequest request, int code) {
        ResourceUpdateResult updateResource = new ResourceUpdateResult(lwM2MClient);
        if (request.getNode() instanceof LwM2mResource) {
            this.updateResourcesValue(updateResource, ((LwM2mResource) request.getNode()), path, request.isReplaceRequest() ? Mode.REPLACE : Mode.UPDATE, code);
        } else if (request.getNode() instanceof LwM2mObjectInstance) {
            ((LwM2mObjectInstance) request.getNode()).getResources().forEach((resId, resource) -> {
                this.updateResourcesValue(updateResource, resource, path + "/" + resId, request.isReplaceRequest() ? Mode.REPLACE : Mode.UPDATE, code);
            });
        }
        if (request.getNode() instanceof LwM2mResource || request.getNode() instanceof LwM2mObjectInstance) {
            clientContext.update(lwM2MClient);
        }
        this.updateAttrTelemetry(updateResource, null);
    }

    @Override
    public void onCreatebjectInstancesResponseOk(LwM2mClient lwM2MClient, String versionId, CreateRequest request) {
        if (request.getObjectInstances() != null && !request.getObjectInstances().isEmpty()) {
            ResourceUpdateResult updateResource = new ResourceUpdateResult(lwM2MClient);
            request.getObjectInstances().forEach(instance ->
                            instance.getResources().forEach((resId, lwM2mResource) ->{
                                String path = versionId.endsWith("/") ? versionId + resId : versionId + "/" + resId;
                                this.updateResourcesValue(updateResource, lwM2mResource, path, Mode.REPLACE, 0);
                            })
            );
            clientContext.update(lwM2MClient);
            this.updateAttrTelemetry(updateResource, null);
        }
    }

    @Override
    public void onWriteCompositeResponseOk(LwM2mClient lwM2MClient, WriteCompositeRequest request, int code) {
        log.trace("ReadCompositeResponse: [{}]", request.getNodes());
        ResourceUpdateResult updateResource = new ResourceUpdateResult(lwM2MClient);
        request.getNodes().forEach((k, v) -> {
            if (v instanceof LwM2mSingleResource) {
                this.updateResourcesValue(updateResource, (LwM2mResource) v, k.toString(), Mode.REPLACE, code);
            } else {
                LwM2mResourceInstance resourceInstance = (LwM2mResourceInstance) v;
                LwM2mMultipleResource multipleResource = new LwM2mMultipleResource(((LwM2mResourceInstance) v).getId(), resourceInstance.getType(), resourceInstance);
                this.updateResourcesValue(updateResource, multipleResource, k.toString(), Mode.REPLACE, code);
            }
        });
        this.updateAttrTelemetry(updateResource, null);
    }

    private void onDeviceProfileUpdate(List<LwM2mClient> clients, Lwm2mDeviceProfileTransportConfiguration oldProfileTransportConfiguration, DeviceProfile deviceProfile) {
        if (clientContext.profileUpdate(deviceProfile) != null) {
            var newProfileTransportConfiguration = clientContext.getProfile(clients.get(0).getRegistration());
            if (newProfileTransportConfiguration != null) {
                ParametersUpdateAnalyzeResult parametersUpdate = getParametersUpdate(oldProfileTransportConfiguration, newProfileTransportConfiguration);
                ParametersObserveAnalyzeResult parametersObserve = getParametersObserve(oldProfileTransportConfiguration.getObserveAttr(), newProfileTransportConfiguration.getObserveAttr(), deviceProfile.getId().getId());
                compareAndSetWriteAttributesObservations(clients, parametersUpdate, parametersObserve);
                updateValueOta(clients, newProfileTransportConfiguration, oldProfileTransportConfiguration);
            }
        }
    }

    private ParametersUpdateAnalyzeResult getParametersUpdate(Lwm2mDeviceProfileTransportConfiguration oldProfile, Lwm2mDeviceProfileTransportConfiguration newProfile){
        TelemetryMappingConfiguration newTelemetryParams = newProfile.getObserveAttr();
        Map<String, String> keyNameNew = newTelemetryParams.getKeyName();
        Map<String, ObjectAttributes> attributeLwm2mNew = newTelemetryParams.getAttributeLwm2m();
        Set<String> attributeSetNew = newTelemetryParams.getAttribute();
        Set<String> telemetrySetNew = newTelemetryParams.getTelemetry();

        TelemetryMappingConfiguration oldTelemetryParams = oldProfile.getObserveAttr();
        Map<String, String> keyNameOld = oldTelemetryParams.getKeyName();
        Map<String, ObjectAttributes> attributeLwm2mOld = oldTelemetryParams.getAttributeLwm2m();
        ParametersAnalyzeResult analyzerParameters = getAttributesAnalyzer(attributeLwm2mOld, attributeLwm2mNew);

            //         analyze Read
        Set<String> newObjectsToRead = new HashSet<>();
        Set<String> newObjectsToCancelRead = new HashSet<>();

        Set<String> attributeSetOld = oldTelemetryParams.getAttribute();
        Set<String> telemetrySetOld = oldTelemetryParams.getTelemetry();

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
        return new ParametersUpdateAnalyzeResult(analyzerParameters, newObjectsToRead, newObjectsToCancelRead, attributeLwm2mNew);
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

    private ParametersObserveAnalyzeResult getParametersObserve(TelemetryMappingConfiguration oldTelemetryParams, TelemetryMappingConfiguration newTelemetryParams, UUID profileId){
        try {
            TelemetryObserveStrategy observeStrategyOld = oldTelemetryParams.getObserveStrategy();
            TelemetryObserveStrategy observeStrategyNew = newTelemetryParams.getObserveStrategy();
            Set<String> observeOld = oldTelemetryParams.getObserve();
            Set<String> observeNew = newTelemetryParams.getObserve();
            Set<String> observeSingleToNew = diffSets(observeOld, observeNew);
            Set<String> observeSingleToCancel = diffSets(observeNew, observeOld);
            if (!observeSingleToNew.isEmpty() || !observeSingleToCancel.isEmpty()) {
                ParametersObserveAnalyzeResult observeAnalyzeResult = new ParametersObserveAnalyzeResult(observeSingleToCancel,
                        observeSingleToNew, observeStrategyOld, observeStrategyNew);
                if (SINGLE.equals(observeStrategyOld) && SINGLE.equals(observeStrategyNew)) {
                    return observeAnalyzeResult;
                } else if (COMPOSITE_BY_OBJECT.equals(observeStrategyOld) && COMPOSITE_BY_OBJECT.equals(observeStrategyNew)) {
                    Map<Integer, String[]> observeByObjectToCancel = new ConcurrentHashMap<>();
                    Map<Integer, String[]> observeByObjectToNew =  new ConcurrentHashMap<>();
                    Map<Integer, String[]> observeByObjectOld = groupByObjectIdVersionedIds(observeOld);
                    Map<Integer, String[]> observeByObjectNew = groupByObjectIdVersionedIds(observeNew);
                    for (Map.Entry<Integer, String[]> entry : observeByObjectNew.entrySet()) {
                        Integer key = entry.getKey();
                        String[] newValue = entry.getValue();
                        if (observeByObjectOld.containsKey(key)) {
                            String[] oldValue = observeByObjectOld.get(key);
                            if (!areArraysStringEqual(oldValue, newValue)) {
                                observeByObjectToCancel.put(key, oldValue);
                                observeByObjectToNew.put(key, newValue);
                            }
                        } else {
                            observeByObjectToNew.put(key, newValue);
                        }
                    }
                    observeAnalyzeResult.setObserveByObjectToCancel(observeByObjectToCancel);
                    observeAnalyzeResult.setObserveByObjectToNew(observeByObjectToNew);
                    return observeAnalyzeResult;
                } else {
                    // Observe Cancel All
                    observeAnalyzeResult.setObserveSingleToCancel(observeOld);
                    // Observe All new
                    observeAnalyzeResult.setObserveSingleToNew(observeNew);
                    return observeAnalyzeResult;
                }
            }
            return new ParametersObserveAnalyzeResult();
        } catch (IllegalArgumentException e) {
            log.error("Error lwm2m on Profile Update id: [{}]. Failed observe Strategy: [{}]", profileId, e.getMessage());
            return new ParametersObserveAnalyzeResult();
        }
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

    private void compareAndSetWriteAttributesObservations(List<LwM2mClient> clients, ParametersUpdateAnalyzeResult parametersUpdate, ParametersObserveAnalyzeResult parametersObserve) {
        clients.forEach(client -> {
            Set<String> clientObjects = clientContext.getSupportedIdVerInClient(client);
            Set<String> pathToAdd = parametersUpdate.getAnalyzerParameters().getPathPostParametersAdd().stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
                    .collect(Collectors.toUnmodifiableSet());
            Map<String, ObjectAttributes> attributesToAdd = pathToAdd.stream().collect(Collectors.toMap(t -> t, parametersUpdate.getAttributeLwm2mNew()::get));
            Set<String> attributesToRemove = parametersUpdate.getAnalyzerParameters().getPathPostParametersDel().stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
                    .collect(Collectors.toUnmodifiableSet());
            Set<String> toRead = diffSets(parametersObserve.getObserveSingleToNew(), parametersUpdate.getNewObjectsToRead());
            LwM2MModelConfig modelConfig = new LwM2MModelConfig(client.getEndpoint(),  attributesToAdd, attributesToRemove, parametersObserve.getObserveSingleToNew(),
                    parametersObserve.getObserveSingleToCancel(), parametersObserve.getObserveByObjectToNew(), parametersObserve.getObserveByObjectToCancel(),
                    toRead, parametersObserve.getObserveStrategyOld(), parametersObserve.getObserveStrategyNew());
            modelConfig.getToCancelRead().addAll(parametersUpdate.getNewObjectsToCancelRead());
            modelConfigService.sendUpdates(client, modelConfig);
        });
    }

    private void  updateValueOta(List<LwM2mClient> clients, Lwm2mDeviceProfileTransportConfiguration newProfile, Lwm2mDeviceProfileTransportConfiguration oldProfile) {
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
        } else {
            log.warn("[{}] Failed to process initAttributes! Profile is null. Update procedure may not have completed after reboot yet", lwM2MClient.getEndpoint());
            logService.log(lwM2MClient, "Failed to process initAttributes. Profile is null. Update procedure may not have completed after reboot yet");
        }
    }

    public LwM2mClientContext getClientContext() {
        return this.clientContext;
    }

    private Map<String, String> getNamesFromProfileForSharedAttributes(LwM2mClient lwM2MClient) {
        Lwm2mDeviceProfileTransportConfiguration profile = clientContext.getProfile(lwM2MClient.getRegistration());
        return profile != null ? profile.getObserveAttr().getKeyName() : Collections.emptyMap();
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
