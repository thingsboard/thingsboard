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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MJsonAdaptor;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientStateException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientProfile;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientRpcRequest;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mFwSwUpdate;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceValue;
import org.thingsboard.server.transport.lwm2m.server.client.ResultsAddKeyValueProto;
import org.thingsboard.server.transport.lwm2m.server.client.ResultsAnalyzerParameters;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST;
import static org.eclipse.leshan.core.attributes.Attribute.OBJECT_VERSION;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.INITIATED;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper.getValueFromKvProto;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.DEVICE_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_5_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_TELEMETRY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_VALUE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_WARN;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.DISCOVER;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE_CANCEL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE_CANCEL_ALL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.READ;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_ATTRIBUTES;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_REPLACE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertJsonArrayToSet;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertOtaUpdateValueToString;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromIdVerToObjectId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromObjectIdToIdVer;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.getAckCallback;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.isFwSwWords;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.setValidTypeOper;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.validateObjectVerFromKey;


@Slf4j
@Service
@TbLwM2mTransportComponent
public class DefaultLwM2MTransportMsgHandler implements LwM2mTransportMsgHandler {

    private ExecutorService registrationExecutor;
    private ExecutorService updateRegistrationExecutor;
    private ExecutorService unRegistrationExecutor;
    public LwM2mValueConverterImpl converter;

    private final TransportService transportService;
    private final LwM2mTransportContext context;
    public final LwM2MTransportServerConfig config;
    public final OtaPackageDataCache otaPackageDataCache;
    public final LwM2mTransportServerHelper helper;
    private final LwM2MJsonAdaptor adaptor;
    private final TbLwM2MDtlsSessionStore sessionStore;
    public final LwM2mClientContext clientContext;
    public final LwM2mTransportRequest lwM2mTransportRequest;
    private final Map<UUID, Long> rpcSubscriptions;
    public final Map<String, Integer> firmwareUpdateState;

    public DefaultLwM2MTransportMsgHandler(TransportService transportService, LwM2MTransportServerConfig config, LwM2mTransportServerHelper helper,
                                           LwM2mClientContext clientContext,
                                           @Lazy LwM2mTransportRequest lwM2mTransportRequest,
                                           OtaPackageDataCache otaPackageDataCache,
                                           LwM2mTransportContext context, LwM2MJsonAdaptor adaptor, TbLwM2MDtlsSessionStore sessionStore) {
        this.transportService = transportService;
        this.config = config;
        this.helper = helper;
        this.clientContext = clientContext;
        this.lwM2mTransportRequest = lwM2mTransportRequest;
        this.otaPackageDataCache = otaPackageDataCache;
        this.context = context;
        this.adaptor = adaptor;
        this.rpcSubscriptions = new ConcurrentHashMap<>();
        this.firmwareUpdateState = new ConcurrentHashMap<>();
        this.sessionStore = sessionStore;
    }

    @PostConstruct
    public void init() {
        this.context.getScheduler().scheduleAtFixedRate(this::reportActivity, new Random().nextInt((int) config.getSessionReportTimeout()), config.getSessionReportTimeout(), TimeUnit.MILLISECONDS);
        this.registrationExecutor = ThingsBoardExecutors.newWorkStealingPool(this.config.getRegisteredPoolSize(), "LwM2M registration");
        this.updateRegistrationExecutor = ThingsBoardExecutors.newWorkStealingPool(this.config.getUpdateRegisteredPoolSize(), "LwM2M update registration");
        this.unRegistrationExecutor = ThingsBoardExecutors.newWorkStealingPool(this.config.getUnRegisteredPoolSize(), "LwM2M unRegistration");
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
     * @param previousObservations - may be null
     */
    public void onRegistered(Registration registration, Collection<Observation> previousObservations) {
        registrationExecutor.submit(() -> {
            LwM2mClient lwM2MClient = this.clientContext.getClientByEndpoint(registration.getEndpoint());
            try {
                log.warn("[{}] [{{}] Client: create after Registration", registration.getEndpoint(), registration.getId());
                if (lwM2MClient != null) {
                    this.clientContext.register(lwM2MClient, registration);
                    this.sendLogsToThingsboard(lwM2MClient, LOG_LW2M_INFO + ": Client registered with registration id: " + registration.getId());
                    SessionInfoProto sessionInfo = lwM2MClient.getSession();
                    transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(this, sessionInfo));
                    log.warn("40) sessionId [{}] Registering rpc subscription after Registration client", new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
                    TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                            .setSessionInfo(sessionInfo)
                            .setSessionEvent(DefaultTransportService.getSessionEventMsg(SessionEvent.OPEN))
                            .setSubscribeToAttributes(TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setSessionType(TransportProtos.SessionType.ASYNC).build())
                            .setSubscribeToRPC(TransportProtos.SubscribeToRPCMsg.newBuilder().setSessionType(TransportProtos.SessionType.ASYNC).build())
                            .build();
                    transportService.process(msg, null);
                    this.getInfoFirmwareUpdate(lwM2MClient, null);
                    this.getInfoSoftwareUpdate(lwM2MClient, null);
                    this.initClientTelemetry(lwM2MClient);
                } else {
                    log.error("Client: [{}] onRegistered [{}] name [{}] lwM2MClient ", registration.getId(), registration.getEndpoint(), null);
                }
            } catch (LwM2MClientStateException stateException) {
                if (LwM2MClientState.UNREGISTERED.equals(stateException.getState())) {
                    log.info("[{}] retry registration due to race condition: [{}].", registration.getEndpoint(), stateException.getState());
                    // Race condition detected and the client was in progress of unregistration while new registration arrived. Let's try again.
                    onRegistered(registration, previousObservations);
                } else {
                    this.sendLogsToThingsboard(lwM2MClient, LOG_LW2M_WARN + ": Client registration failed due to invalid state: " + stateException.getState());
                }
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable registration.", registration.getEndpoint(), t);
                this.sendLogsToThingsboard(lwM2MClient, LOG_LW2M_WARN + ": Client registration failed due to: " + t.getMessage());
            }
        });
    }

    /**
     * if sessionInfo removed from sessions, then new registerAsyncSession
     *
     * @param registration - Registration LwM2M Client
     */
    public void updatedReg(Registration registration) {
        updateRegistrationExecutor.submit(() -> {
            LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
            try {
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
                this.sendLogsToThingsboard(lwM2MClient, LOG_LW2M_ERROR + String.format(": Client update Registration, %s", t.getMessage()));
            }
        });
    }

    /**
     * @param registration - Registration LwM2M Client
     * @param observations - !!! Warn: if have not finishing unReg, then this operation will be finished on next Client`s connect
     */
    public void unReg(Registration registration, Collection<Observation> observations) {
        unRegistrationExecutor.submit(() -> {
            LwM2mClient client = clientContext.getClientByEndpoint(registration.getEndpoint());
            try {
                this.sendLogsToThingsboard(client, LOG_LW2M_INFO + ": Client unRegistration");
                clientContext.unregister(client, registration);
                SessionInfoProto sessionInfo = client.getSession();
                if (sessionInfo != null) {
                    this.doCloseSession(sessionInfo);
                    transportService.deregisterSession(sessionInfo);
                    sessionStore.remove(registration.getEndpoint());
                    log.info("Client close session: [{}] unReg [{}] name  [{}] profile ", registration.getId(), registration.getEndpoint(), sessionInfo.getDeviceType());
                } else {
                    log.error("Client close session: [{}] unReg [{}] name  [{}] sessionInfo ", registration.getId(), registration.getEndpoint(), null);
                }
            } catch (LwM2MClientStateException stateException) {
                log.info("[{}] delete registration: [{}] {}.", registration.getEndpoint(), stateException.getState(), stateException.getMessage());
            } catch (Throwable t) {
                log.error("[{}] endpoint [{}] error Unable un registration.", registration.getEndpoint(), t);
                this.sendLogsToThingsboard(client, LOG_LW2M_ERROR + String.format(": Client Unable un Registration, %s", t.getMessage()));
            }
        });
    }

    @Override
    public void onSleepingDev(Registration registration) {
        log.info("[{}] [{}] Received endpoint Sleeping version event", registration.getId(), registration.getEndpoint());
        this.sendLogsToThingsboard(clientContext.getClientByEndpoint(registration.getEndpoint()), LOG_LW2M_INFO + ": Client is sleeping!");
        //TODO: associate endpointId with device information.
    }

    /**
     * Cancel observation for All objects for this registration
     */
    @Override
    public void setCancelObservationsAll(Registration registration) {
        if (registration != null) {
            LwM2mClient client = clientContext.getClientByEndpoint(registration.getEndpoint());
            if (client != null && client.getRegistration() != null && client.getRegistration().getId().equals(registration.getId())) {
                this.lwM2mTransportRequest.sendAllRequest(client, null, OBSERVE_CANCEL_ALL,
                        null, null, this.config.getTimeout(), null);
            }
        }
    }

    /**
     * Sending observe value to thingsboard from ObservationListener.onResponse: object, instance, SingleResource or MultipleResource
     *
     * @param registration - Registration LwM2M Client
     * @param path         - observe
     * @param response     - observe
     */
    @Override
    public void onUpdateValueAfterReadResponse(Registration registration, String path, ReadResponse response, LwM2mClientRpcRequest rpcRequest) {
        if (response.getContent() != null) {
            LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
            ObjectModel objectModelVersion = lwM2MClient.getObjectModel(path, this.config.getModelProvider());
            if (objectModelVersion != null) {
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
            if (rpcRequest != null) {
                this.sendRpcRequestAfterReadResponse(registration, lwM2MClient, path, response, rpcRequest);
            }
        }
    }

    private void sendRpcRequestAfterReadResponse(Registration registration, LwM2mClient lwM2MClient, String pathIdVer, ReadResponse response,
                                                 LwM2mClientRpcRequest rpcRequest) {
        Object value = null;
        if (response.getContent() instanceof LwM2mObject) {
            value = lwM2MClient.objectToString((LwM2mObject) response.getContent(), this.converter, pathIdVer);
        } else if (response.getContent() instanceof LwM2mObjectInstance) {
            value = lwM2MClient.instanceToString((LwM2mObjectInstance) response.getContent(), this.converter, pathIdVer);
        } else if (response.getContent() instanceof LwM2mResource) {
            value = lwM2MClient.resourceToString((LwM2mResource) response.getContent(), this.converter, pathIdVer);
        }
        String msg = String.format("%s: type operation %s path - %s value - %s", LOG_LW2M_INFO,
                READ, pathIdVer, value);
        this.sendLogsToThingsboard(lwM2MClient, msg);
        rpcRequest.setValueMsg(String.format("%s", value));
        this.sentRpcResponse(rpcRequest, response.getCode().getName(), (String) value, LOG_LW2M_VALUE);
    }

    /**
     * Update - send request in change value resources in Client
     * 1. FirmwareUpdate:
     * - If msg.getSharedUpdatedList().forEach(tsKvProto -> {tsKvProto.getKv().getKey().indexOf(FIRMWARE_UPDATE_PREFIX, 0) == 0
     * 2. Shared Other AttributeUpdate
     * -- Path to resources from profile equal keyName or from ModelObject equal name
     * -- Only for resources:  isWritable && isPresent as attribute in profile -> LwM2MClientProfile (format: CamelCase)
     * 3. Delete - nothing
     *
     * @param msg -
     */
    @Override
    public void onAttributeUpdate(AttributeUpdateNotificationMsg msg, TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2MClient = clientContext.getClientBySessionInfo(sessionInfo);
        if (msg.getSharedUpdatedCount() > 0 && lwM2MClient != null) {
            log.warn("2) OnAttributeUpdate, SharedUpdatedList() [{}]", msg.getSharedUpdatedList());
            msg.getSharedUpdatedList().forEach(tsKvProto -> {
                String pathName = tsKvProto.getKv().getKey();
                String pathIdVer = this.getPresentPathIntoProfile(sessionInfo, pathName);
                Object valueNew = getValueFromKvProto(tsKvProto.getKv());
                if ((OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION).equals(pathName)
                        && (!valueNew.equals(lwM2MClient.getFwUpdate().getCurrentVersion())))
                        || (OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TITLE).equals(pathName)
                        && (!valueNew.equals(lwM2MClient.getFwUpdate().getCurrentTitle())))) {
                    this.getInfoFirmwareUpdate(lwM2MClient, null);
                } else if ((OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.VERSION).equals(pathName)
                        && (!valueNew.equals(lwM2MClient.getSwUpdate().getCurrentVersion())))
                        || (OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TITLE).equals(pathName)
                        && (!valueNew.equals(lwM2MClient.getSwUpdate().getCurrentTitle())))) {
                    this.getInfoSoftwareUpdate(lwM2MClient, null);
                }
                if (pathIdVer != null) {
                    ResourceModel resourceModel = lwM2MClient.getResourceModel(pathIdVer, this.config
                            .getModelProvider());
                    if (resourceModel != null && resourceModel.operations.isWritable()) {
                        this.updateResourcesValueToClient(lwM2MClient, this.getResourceValueFormatKv(lwM2MClient, pathIdVer), valueNew, pathIdVer);
                    } else {
                        log.error("Resource path - [{}] value - [{}] is not Writable and cannot be updated", pathIdVer, valueNew);
                        String logMsg = String.format("%s: attributeUpdate: Resource path - %s value - %s is not Writable and cannot be updated",
                                LOG_LW2M_ERROR, pathIdVer, valueNew);
                        this.sendLogsToThingsboard(lwM2MClient, logMsg);
                    }
                } else if (!isFwSwWords(pathName)) {
                    log.error("Resource name name - [{}] value - [{}] is not present as attribute/telemetry in profile and cannot be updated", pathName, valueNew);
                    String logMsg = String.format("%s: attributeUpdate: attribute name - %s value - %s is not present as attribute in profile and cannot be updated",
                            LOG_LW2M_ERROR, pathName, valueNew);
                    this.sendLogsToThingsboard(lwM2MClient, logMsg);
                }

            });
        } else if (msg.getSharedDeletedCount() > 0 && lwM2MClient != null) {
            msg.getSharedUpdatedList().forEach(tsKvProto -> {
                String pathName = tsKvProto.getKv().getKey();
                Object valueNew = getValueFromKvProto(tsKvProto.getKv());
                if (OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION).equals(pathName) && !valueNew.equals(lwM2MClient.getFwUpdate().getCurrentVersion())) {
                    lwM2MClient.getFwUpdate().setCurrentVersion((String) valueNew);
                }
            });
            log.info("[{}] delete [{}]  onAttributeUpdate", msg.getSharedDeletedList(), sessionInfo);
        } else if (lwM2MClient == null) {
            log.error("OnAttributeUpdate, lwM2MClient is null");
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
     * #1 del from rpcSubscriptions by timeout
     * #2 if not present in rpcSubscriptions by requestId: create new LwM2mClientRpcRequest, after success - add requestId, timeout
     */
    @Override
    public void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg toDeviceRpcRequestMsg, SessionInfoProto sessionInfo) {
        // #1
        this.checkRpcRequestTimeout();
        log.warn("4) toDeviceRpcRequestMsg: [{}], sessionUUID: [{}]", toDeviceRpcRequestMsg, new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
        String bodyParams = StringUtils.trimToNull(toDeviceRpcRequestMsg.getParams()) != null ? toDeviceRpcRequestMsg.getParams() : "null";
        LwM2mTypeOper lwM2mTypeOper = setValidTypeOper(toDeviceRpcRequestMsg.getMethodName());
        UUID requestUUID = new UUID(toDeviceRpcRequestMsg.getRequestIdMSB(), toDeviceRpcRequestMsg.getRequestIdLSB());
        if (!this.rpcSubscriptions.containsKey(requestUUID)) {
            this.rpcSubscriptions.put(requestUUID, toDeviceRpcRequestMsg.getExpirationTime());
            LwM2mClientRpcRequest lwm2mClientRpcRequest = null;
            try {
                LwM2mClient client = clientContext.getClientBySessionInfo(sessionInfo);
                Registration registration = client.getRegistration();
                if(registration != null) {
                    lwm2mClientRpcRequest = new LwM2mClientRpcRequest(lwM2mTypeOper, bodyParams, toDeviceRpcRequestMsg.getRequestId(), sessionInfo, registration, this);
                    if (lwm2mClientRpcRequest.getErrorMsg() != null) {
                        lwm2mClientRpcRequest.setResponseCode(BAD_REQUEST.name());
                        this.onToDeviceRpcResponse(lwm2mClientRpcRequest.getDeviceRpcResponseResultMsg(), sessionInfo);
                    } else {
                        lwM2mTransportRequest.sendAllRequest(client, lwm2mClientRpcRequest.getTargetIdVer(), lwm2mClientRpcRequest.getTypeOper(),
                                null,
                                lwm2mClientRpcRequest.getValue() == null ? lwm2mClientRpcRequest.getParams() : lwm2mClientRpcRequest.getValue(),
                                this.config.getTimeout(), lwm2mClientRpcRequest);
                    }
                } else {
                    this.sendErrorRpcResponse(lwm2mClientRpcRequest, "registration == null", sessionInfo);
                }
            } catch (Exception e) {
                this.sendErrorRpcResponse(lwm2mClientRpcRequest, e.getMessage(), sessionInfo);
            }
        }
    }

    private void sendErrorRpcResponse(LwM2mClientRpcRequest lwm2mClientRpcRequest, String msgError, SessionInfoProto sessionInfo) {
        if (lwm2mClientRpcRequest == null) {
            lwm2mClientRpcRequest = new LwM2mClientRpcRequest();
        }
        lwm2mClientRpcRequest.setResponseCode(BAD_REQUEST.name());
        if (lwm2mClientRpcRequest.getErrorMsg() == null) {
            lwm2mClientRpcRequest.setErrorMsg(msgError);
        }
        this.onToDeviceRpcResponse(lwm2mClientRpcRequest.getDeviceRpcResponseResultMsg(), sessionInfo);
    }

    private void checkRpcRequestTimeout() {
        log.warn("4.1) before rpcSubscriptions.size(): [{}]", rpcSubscriptions.size());
        if (rpcSubscriptions.size() > 0) {
            Set<UUID> rpcSubscriptionsToRemove = rpcSubscriptions.entrySet().stream().filter(kv -> System.currentTimeMillis() > kv.getValue()).map(Map.Entry::getKey).collect(Collectors.toSet());
            log.warn("4.2) System.currentTimeMillis(): [{}]", System.currentTimeMillis());
            log.warn("4.3) rpcSubscriptionsToRemove: [{}]", rpcSubscriptionsToRemove);
            rpcSubscriptionsToRemove.forEach(rpcSubscriptions::remove);
        }
        log.warn("4.4) after rpcSubscriptions.size(): [{}]", rpcSubscriptions.size());
    }

    public void sentRpcResponse(LwM2mClientRpcRequest rpcRequest, String requestCode, String msg, String typeMsg) {
        rpcRequest.setResponseCode(requestCode);
        if (LOG_LW2M_ERROR.equals(typeMsg)) {
            rpcRequest.setInfoMsg(null);
            rpcRequest.setValueMsg(null);
            if (rpcRequest.getErrorMsg() == null) {
                msg = msg.isEmpty() ? null : msg;
                rpcRequest.setErrorMsg(msg);
            }
        } else if (LOG_LW2M_INFO.equals(typeMsg)) {
            if (rpcRequest.getInfoMsg() == null) {
                rpcRequest.setInfoMsg(msg);
            }
        } else if (LOG_LW2M_VALUE.equals(typeMsg)) {
            if (rpcRequest.getValueMsg() == null) {
                rpcRequest.setValueMsg(msg);
            }
        }
        this.onToDeviceRpcResponse(rpcRequest.getDeviceRpcResponseResultMsg(), rpcRequest.getSessionInfo());
    }

    @Override
    public void onToDeviceRpcResponse(TransportProtos.ToDeviceRpcResponseMsg toDeviceResponse, SessionInfoProto sessionInfo) {
        log.warn("5) onToDeviceRpcResponse: [{}], sessionUUID: [{}]", toDeviceResponse, new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
        transportService.process(sessionInfo, toDeviceResponse, null);
    }

    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        log.info("[{}] toServerRpcResponse", toServerResponse);
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
    @Override
    public void onAwakeDev(Registration registration) {
        log.trace("[{}] [{}] Received endpoint Awake version event", registration.getId(), registration.getEndpoint());
        this.sendLogsToThingsboard(clientContext.getClientByEndpoint(registration.getEndpoint()), LOG_LW2M_INFO + ": Client is awake!");
        //TODO: associate endpointId with device information.
    }

    /**
     * @param logMsg         - text msg
     * @param registrationId - Id of Registration LwM2M Client
     */
    @Override
    public void sendLogsToThingsboard(String registrationId, String logMsg) {
        sendLogsToThingsboard(clientContext.getClientByRegistrationId(registrationId), logMsg);
    }

    @Override
    public void sendLogsToThingsboard(LwM2mClient client, String logMsg) {
        if (logMsg != null && client != null && client.getSession() != null) {
            if (logMsg.length() > 1024) {
                logMsg = logMsg.substring(0, 1024);
            }
            this.helper.sendParametersOnThingsboardTelemetry(this.helper.getKvStringtoThingsboard(LOG_LW2M_TELEMETRY, logMsg), client.getSession());
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
     * @param lwM2MClient - object with All parameters off client
     */
    private void initClientTelemetry(LwM2mClient lwM2MClient) {
        LwM2mClientProfile lwM2MClientProfile = clientContext.getProfile(lwM2MClient.getProfileId());
        Set<String> clientObjects = clientContext.getSupportedIdVerInClient(lwM2MClient);
        if (clientObjects != null && clientObjects.size() > 0) {
            if (LwM2mTransportUtil.LwM2MClientStrategy.CLIENT_STRATEGY_2.code == lwM2MClientProfile.getClientStrategy()) {
                // #2
                lwM2MClient.getPendingReadRequests().addAll(clientObjects);
                clientObjects.forEach(path -> lwM2mTransportRequest.sendAllRequest(lwM2MClient, path, READ,
                        null, this.config.getTimeout(), null));
            }
            // #1
            this.initReadAttrTelemetryObserveToClient(lwM2MClient, READ, clientObjects);
            this.initReadAttrTelemetryObserveToClient(lwM2MClient, OBSERVE, clientObjects);
            this.initReadAttrTelemetryObserveToClient(lwM2MClient, WRITE_ATTRIBUTES, clientObjects);
            this.initReadAttrTelemetryObserveToClient(lwM2MClient, DISCOVER, clientObjects);
        }
    }

    /**
     * @param registration -
     * @param lwM2mObject  -
     * @param pathIdVer    -
     */
    private void updateObjectResourceValue(Registration registration, LwM2mObject lwM2mObject, String pathIdVer) {
        LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(pathIdVer));
        lwM2mObject.getInstances().forEach((instanceId, instance) -> {
            String pathInstance = pathIds.toString() + "/" + instanceId;
            this.updateObjectInstanceResourceValue(registration, instance, pathInstance);
        });
    }

    /**
     * @param registration        -
     * @param lwM2mObjectInstance -
     * @param pathIdVer           -
     */
    private void updateObjectInstanceResourceValue(Registration registration, LwM2mObjectInstance lwM2mObjectInstance, String pathIdVer) {
        LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(pathIdVer));
        lwM2mObjectInstance.getResources().forEach((resourceId, resource) -> {
            String pathRez = pathIds.toString() + "/" + resourceId;
            this.updateResourcesValue(registration, resource, pathRez);
        });
    }

    /**
     * Sending observe value of resources to thingsboard
     * #1 Return old Value Resource from LwM2MClient
     * #2 Update new Resources (replace old Resource Value on new Resource Value)
     * #3 If fr_update -> UpdateFirmware
     * #4 updateAttrTelemetry
     *
     * @param registration  - Registration LwM2M Client
     * @param lwM2mResource - LwM2mSingleResource response.getContent()
     * @param path          - resource
     */
    private void updateResourcesValue(Registration registration, LwM2mResource lwM2mResource, String path) {
        LwM2mClient lwM2MClient = clientContext.getClientByEndpoint(registration.getEndpoint());
        if (lwM2MClient.saveResourceValue(path, lwM2mResource, this.config.getModelProvider())) {
            /** version != null
             * set setClient_fw_info... = value
             **/
            if (lwM2MClient.getFwUpdate() != null && lwM2MClient.getFwUpdate().isInfoFwSwUpdate()) {
                lwM2MClient.getFwUpdate().initReadValue(this, this.lwM2mTransportRequest, path);
            }
            if (lwM2MClient.getSwUpdate() != null && lwM2MClient.getSwUpdate().isInfoFwSwUpdate()) {
                lwM2MClient.getSwUpdate().initReadValue(this, this.lwM2mTransportRequest, path);
            }

            if ((convertPathFromObjectIdToIdVer(FW_RESULT_ID, registration).equals(path)) ||
                    (convertPathFromObjectIdToIdVer(FW_STATE_ID, registration).equals(path))) {
                LwM2mFwSwUpdate fwUpdate = lwM2MClient.getFwUpdate(clientContext);
                log.warn("93) path: [{}] value: [{}]", path, lwM2mResource.getValue());
                fwUpdate.updateStateOta(this, lwM2mTransportRequest, registration, path, ((Long) lwM2mResource.getValue()).intValue());
            }
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

    private void initReadAttrTelemetryObserveToClient(LwM2mClient lwM2MClient, LwM2mTypeOper typeOper, Set<String> clientObjects) {
        LwM2mClientProfile lwM2MClientProfile = clientContext.getProfile(lwM2MClient.getProfileId());
        Set<String> result = null;
        ConcurrentHashMap<String, Object> params = null;
        if (READ.equals(typeOper)) {
            result = JacksonUtil.fromString(lwM2MClientProfile.getPostAttributeProfile().toString(),
                    new TypeReference<>() {
                    });
            result.addAll(JacksonUtil.fromString(lwM2MClientProfile.getPostTelemetryProfile().toString(),
                    new TypeReference<>() {
                    }));
        } else if (OBSERVE.equals(typeOper)) {
            result = JacksonUtil.fromString(lwM2MClientProfile.getPostObserveProfile().toString(),
                    new TypeReference<>() {
                    });
        } else if (DISCOVER.equals(typeOper)) {
            result = this.getPathForWriteAttributes(lwM2MClientProfile.getPostAttributeLwm2mProfile()).keySet();
        } else if (WRITE_ATTRIBUTES.equals(typeOper)) {
            params = this.getPathForWriteAttributes(lwM2MClientProfile.getPostAttributeLwm2mProfile());
            result = params.keySet();
        }
        sendRequestsToClient(lwM2MClient, typeOper, clientObjects, result, params);
    }

    private void sendRequestsToClient(LwM2mClient lwM2MClient, LwM2mTypeOper operationType, Set<String> supportedObjectIds, Set<String> desiredObjectIds, ConcurrentHashMap<String, Object> params) {
        if (desiredObjectIds != null && !desiredObjectIds.isEmpty()) {
            Set<String> targetObjectIds = desiredObjectIds.stream().filter(target -> isSupportedTargetId(supportedObjectIds, target)
            ).collect(Collectors.toUnmodifiableSet());
            if (!targetObjectIds.isEmpty()) {
                //TODO: remove this side effect?
                lwM2MClient.getPendingReadRequests().addAll(targetObjectIds);
                targetObjectIds.forEach(target -> {
                    Object additionalParams = params != null ? params.get(target) : null;
                    lwM2mTransportRequest.sendAllRequest(lwM2MClient, target, operationType, additionalParams, this.config.getTimeout(), null);
                });
                if (OBSERVE.equals(operationType)) {
                    lwM2MClient.initReadValue(this, null);
                }
            }
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
            LwM2mClientProfile lwM2MClientProfile = clientContext.getProfile(registration);
            List<TransportProtos.KeyValueProto> resultAttributes = new ArrayList<>();
            lwM2MClientProfile.getPostAttributeProfile().forEach(pathIdVer -> {
                if (path.contains(pathIdVer.getAsString())) {
                    TransportProtos.KeyValueProto kvAttr = this.getKvToThingsboard(pathIdVer.getAsString(), registration);
                    if (kvAttr != null) {
                        resultAttributes.add(kvAttr);
                    }
                }
            });
            List<TransportProtos.KeyValueProto> resultTelemetries = new ArrayList<>();
            lwM2MClientProfile.getPostTelemetryProfile().forEach(pathIdVer -> {
                if (path.contains(pathIdVer.getAsString())) {
                    TransportProtos.KeyValueProto kvAttr = this.getKvToThingsboard(pathIdVer.getAsString(), registration);
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
        JsonObject names = clientContext.getProfiles().get(lwM2MClient.getProfileId()).getPostKeyNameProfile();
        if (names != null && names.has(pathIdVer)) {
            String resourceName = names.get(pathIdVer).getAsString();
            if (resourceName != null && !resourceName.isEmpty()) {
                try {
                    LwM2mResource resourceValue = lwM2MClient != null ? getResourceValueFromLwM2MClient(lwM2MClient, pathIdVer) : null;
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
                                        new LwM2mPath(convertPathFromIdVerToObjectId(pathIdVer)));
                                JsonElement element = gson.toJsonTree(val, val.getClass());
                                ((JsonObject) finalvalueKvProto).add(String.valueOf(k), element);
                            });
                            valueKvProto = gson.toJson(valueKvProto);
                        } else {
                            valueKvProto = this.converter.convertValue(resourceValue.getValue(), currentType, expectedType,
                                    new LwM2mPath(convertPathFromIdVerToObjectId(pathIdVer)));
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

    /**
     * @param pathIdVer - path resource
     * @return - value of Resource into format KvProto or null
     */
    private Object getResourceValueFormatKv(LwM2mClient lwM2MClient, String pathIdVer) {
        LwM2mResource resourceValue = this.getResourceValueFromLwM2MClient(lwM2MClient, pathIdVer);
        if (resourceValue != null) {
            ResourceModel.Type currentType = resourceValue.getType();
            ResourceModel.Type expectedType = this.helper.getResourceModelTypeEqualsKvProtoValueType(currentType, pathIdVer);
            return this.converter.convertValue(resourceValue.getValue(), currentType, expectedType,
                    new LwM2mPath(convertPathFromIdVerToObjectId(pathIdVer)));
        } else {
            return null;
        }
    }

    /**
     * @param lwM2MClient -
     * @param path        -
     * @return - return value of Resource by idPath
     */
    private LwM2mResource getResourceValueFromLwM2MClient(LwM2mClient lwM2MClient, String path) {
        LwM2mResource lwm2mResourceValue = null;
        ResourceValue resourceValue = lwM2MClient.getResources().get(path);
        if (resourceValue != null) {
            if (new LwM2mPath(convertPathFromIdVerToObjectId(path)).isResource()) {
                lwm2mResourceValue = lwM2MClient.getResources().get(path).getLwM2mResource();
            }
        }
        return lwm2mResourceValue;
    }

    /**
     * Update resource (attribute) value  on thingsboard after update value in client
     *
     * @param registration -
     * @param path         -
     * @param request      -
     */
    public void onWriteResponseOk(Registration registration, String path, WriteRequest request) {
        if (request.getNode() instanceof LwM2mResource) {
            this.updateResourcesValue(registration, ((LwM2mResource) request.getNode()), path);
        } else if (request.getNode() instanceof LwM2mObjectInstance) {
            ((LwM2mObjectInstance) request.getNode()).getResources().forEach((resId, resource) -> {
                this.updateResourcesValue(registration, resource, path + "/" + resId);
            });
        }

    }

    /**
     * #1 Read new, old Value (Attribute, Telemetry, Observe, KeyName)
     * #2 Update in lwM2MClient: ...Profile if changes from update device
     * #3 Equivalence test: old <> new Value (Attribute, Telemetry, Observe, KeyName)
     * #3.1 Attribute isChange (add&del)
     * #3.2 Telemetry isChange (add&del)
     * #3.3 KeyName isChange (add)
     * #3.4 attributeLwm2m isChange (update WrightAttribute: add/update/del)
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
     * #6
     * #6.1 - update WriteAttribute
     * #6.2 - del WriteAttribute
     *
     * @param clients       -
     * @param deviceProfile -
     */
    private void onDeviceProfileUpdate(List<LwM2mClient> clients, DeviceProfile deviceProfile) {
        LwM2mClientProfile lwM2MClientProfileOld = clientContext.getProfiles().get(deviceProfile.getUuidId()).clone();
        if (clientContext.profileUpdate(deviceProfile) != null) {
            // #1
            JsonArray attributeOld = lwM2MClientProfileOld.getPostAttributeProfile();
            Set<String> attributeSetOld = convertJsonArrayToSet(attributeOld);
            JsonArray telemetryOld = lwM2MClientProfileOld.getPostTelemetryProfile();
            Set<String> telemetrySetOld = convertJsonArrayToSet(telemetryOld);
            JsonArray observeOld = lwM2MClientProfileOld.getPostObserveProfile();
            JsonObject keyNameOld = lwM2MClientProfileOld.getPostKeyNameProfile();
            JsonObject attributeLwm2mOld = lwM2MClientProfileOld.getPostAttributeLwm2mProfile();

            LwM2mClientProfile lwM2MClientProfileNew = clientContext.getProfiles().get(deviceProfile.getUuidId()).clone();
            JsonArray attributeNew = lwM2MClientProfileNew.getPostAttributeProfile();
            Set<String> attributeSetNew = convertJsonArrayToSet(attributeNew);
            JsonArray telemetryNew = lwM2MClientProfileNew.getPostTelemetryProfile();
            Set<String> telemetrySetNew = convertJsonArrayToSet(telemetryNew);
            JsonArray observeNew = lwM2MClientProfileNew.getPostObserveProfile();
            JsonObject keyNameNew = lwM2MClientProfileNew.getPostKeyNameProfile();
            JsonObject attributeLwm2mNew = lwM2MClientProfileNew.getPostAttributeLwm2mProfile();

            // #3
            ResultsAnalyzerParameters sendAttrToThingsboard = new ResultsAnalyzerParameters();
            // #3.1
            if (!attributeOld.equals(attributeNew)) {
                ResultsAnalyzerParameters postAttributeAnalyzer = this.getAnalyzerParameters(new Gson().fromJson(attributeOld,
                        new TypeToken<Set<String>>() {
                        }.getType()), attributeSetNew);
                sendAttrToThingsboard.getPathPostParametersAdd().addAll(postAttributeAnalyzer.getPathPostParametersAdd());
                sendAttrToThingsboard.getPathPostParametersDel().addAll(postAttributeAnalyzer.getPathPostParametersDel());
            }
            // #3.2
            if (!telemetryOld.equals(telemetryNew)) {
                ResultsAnalyzerParameters postTelemetryAnalyzer = this.getAnalyzerParameters(new Gson().fromJson(telemetryOld,
                        new TypeToken<Set<String>>() {
                        }.getType()), telemetrySetNew);
                sendAttrToThingsboard.getPathPostParametersAdd().addAll(postTelemetryAnalyzer.getPathPostParametersAdd());
                sendAttrToThingsboard.getPathPostParametersDel().addAll(postTelemetryAnalyzer.getPathPostParametersDel());
            }
            // #3.3
            if (!keyNameOld.equals(keyNameNew)) {
                ResultsAnalyzerParameters keyNameChange = this.getAnalyzerKeyName(new Gson().fromJson(keyNameOld.toString(),
                        new TypeToken<ConcurrentHashMap<String, String>>() {
                        }.getType()),
                        new Gson().fromJson(keyNameNew.toString(), new TypeToken<ConcurrentHashMap<String, String>>() {
                        }.getType()));
                sendAttrToThingsboard.getPathPostParametersAdd().addAll(keyNameChange.getPathPostParametersAdd());
            }

            // #3.4, #6
            if (!attributeLwm2mOld.equals(attributeLwm2mNew)) {
                this.getAnalyzerAttributeLwm2m(clients, attributeLwm2mOld, attributeLwm2mNew);
            }

            // #4.1 add
            if (sendAttrToThingsboard.getPathPostParametersAdd().size() > 0) {
                // update value in Resources
                clients.forEach(client -> {
                    this.readObserveFromProfile(client, sendAttrToThingsboard.getPathPostParametersAdd(), READ);
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
                clients.forEach(client -> {
                    Registration registration = client.getRegistration();
                    if (postObserveAnalyzer.getPathPostParametersAdd().size() > 0) {
                        this.readObserveFromProfile(client, postObserveAnalyzer.getPathPostParametersAdd(), OBSERVE);
                    }
                    // 5.3 del
                    //  send Request cancel observe to Client
                    if (postObserveAnalyzer.getPathPostParametersDel().size() > 0) {
                        this.cancelObserveFromProfile(client, postObserveAnalyzer.getPathPostParametersDel());
                    }
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
     * @param targets - path Resources == [ "/2/0/0", "/2/0/1"]
     */
    private void readObserveFromProfile(LwM2mClient client, Set<String> targets, LwM2mTypeOper typeOper) {
        targets.forEach(target -> {
            LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(target));
            if (pathIds.isResource()) {
                if (READ.equals(typeOper)) {
                    lwM2mTransportRequest.sendAllRequest(client, target, typeOper,
                            null, this.config.getTimeout(), null);
                } else if (OBSERVE.equals(typeOper)) {
                    lwM2mTransportRequest.sendAllRequest(client, target, typeOper,
                            null, this.config.getTimeout(), null);
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

    /**
     * #3.4, #6
     * #6
     * #6.1 - send update WriteAttribute
     * #6.2 - send empty WriteAttribute
     *
     * @param attributeLwm2mOld -
     * @param attributeLwm2mNew -
     * @return
     */
    private void getAnalyzerAttributeLwm2m(List<LwM2mClient> clients, JsonObject attributeLwm2mOld, JsonObject attributeLwm2mNew) {
        ResultsAnalyzerParameters analyzerParameters = new ResultsAnalyzerParameters();
        ConcurrentHashMap<String, Object> lwm2mAttributesOld = new Gson().fromJson(attributeLwm2mOld.toString(),
                new TypeToken<ConcurrentHashMap<String, Object>>() {
                }.getType());
        ConcurrentHashMap<String, Object> lwm2mAttributesNew = new Gson().fromJson(attributeLwm2mNew.toString(),
                new TypeToken<ConcurrentHashMap<String, Object>>() {
                }.getType());
        Set<String> pathOld = lwm2mAttributesOld.keySet();
        Set<String> pathNew = lwm2mAttributesNew.keySet();
        analyzerParameters.setPathPostParametersAdd(pathNew
                .stream().filter(p -> !pathOld.contains(p)).collect(Collectors.toSet()));
        analyzerParameters.setPathPostParametersDel(pathOld
                .stream().filter(p -> !pathNew.contains(p)).collect(Collectors.toSet()));
        Set<String> pathCommon = pathNew
                .stream().filter(p -> pathOld.contains(p)).collect(Collectors.toSet());
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
                    ConcurrentHashMap<String, Object> finalParams = lwm2mAttributesNew;
                    pathSend.forEach(target -> lwM2mTransportRequest.sendAllRequest(client, target, WRITE_ATTRIBUTES,
                            finalParams.get(target), this.config.getTimeout(), null));
                }
            });
        }
        // #6.2
        if (analyzerParameters.getPathPostParametersDel().size() > 0) {
            clients.forEach(client -> {
                Registration registration = client.getRegistration();
                Set<String> clientObjects = clientContext.getSupportedIdVerInClient(client);
                Set<String> pathSend = analyzerParameters.getPathPostParametersDel().stream().filter(target -> clientObjects.contains("/" + target.split(LWM2M_SEPARATOR_PATH)[1]))
                        .collect(Collectors.toUnmodifiableSet());
                if (!pathSend.isEmpty()) {
                    pathSend.forEach(target -> {
                        Map<String, Object> params = (Map<String, Object>) lwm2mAttributesOld.get(target);
                        params.clear();
                        params.put(OBJECT_VERSION, "");
                        lwM2mTransportRequest.sendAllRequest(client, target, WRITE_ATTRIBUTES, params, this.config.getTimeout(), null);
                    });
                }
            });
        }

    }

    private void cancelObserveFromProfile(LwM2mClient lwM2mClient, Set<String> paramAnallyzer) {
        paramAnallyzer.forEach(pathIdVer -> {
                    if (this.getResourceValueFromLwM2MClient(lwM2mClient, pathIdVer) != null) {
                        lwM2mTransportRequest.sendAllRequest(lwM2mClient, pathIdVer, OBSERVE_CANCEL, null, this.config.getTimeout(), null);
                    }
                }
        );
    }

    private void updateResourcesValueToClient(LwM2mClient lwM2MClient, Object valueOld, Object valueNew, String path) {
        if (valueNew != null && (valueOld == null || !valueNew.toString().equals(valueOld.toString()))) {
            lwM2mTransportRequest.sendAllRequest(lwM2MClient, path, WRITE_REPLACE, valueNew, this.config.getTimeout(), null);
        } else {
            log.error("Failed update resource [{}] [{}]", path, valueNew);
            String logMsg = String.format("%s: Failed update resource path - %s value - %s. Value is not changed or bad",
                    LOG_LW2M_ERROR, path, valueNew);
            this.sendLogsToThingsboard(lwM2MClient, logMsg);
            log.info("Failed update resource [{}] [{}]", path, valueNew);
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
     * Get path to resource from profile equal keyName
     *
     * @param sessionInfo -
     * @param name        -
     * @return -
     */
    public String getPresentPathIntoProfile(TransportProtos.SessionInfoProto sessionInfo, String name) {
        LwM2mClientProfile profile = clientContext.getProfile(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
        LwM2mClient lwM2mClient = clientContext.getClientBySessionInfo(sessionInfo);
        return profile.getPostKeyNameProfile().getAsJsonObject().entrySet().stream()
                .filter(e -> e.getValue().getAsString().equals(name) && validateResourceInModel(lwM2mClient, e.getKey(), false)).findFirst().map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 1. FirmwareUpdate:
     * - msg.getSharedUpdatedList().forEach(tsKvProto -> {tsKvProto.getKv().getKey().indexOf(FIRMWARE_UPDATE_PREFIX, 0) == 0
     * 2. Update resource value on client: if there is a difference in values between the current resource values and the shared attribute values
     * - Get path resource by result attributesResponse
     *
     * @param attributesResponse -
     * @param sessionInfo        -
     */
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg attributesResponse, TransportProtos.SessionInfoProto sessionInfo) {
        try {
            List<TransportProtos.TsKvProto> tsKvProtos = attributesResponse.getSharedAttributeListList();
            this.updateAttributeFromThingsboard(tsKvProtos, sessionInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * #1.1 If two names have equal path => last time attribute
     * #2.1 if there is a difference in values between the current resource values and the shared attribute values
     * => send to client Request Update of value (new value from shared attribute)
     * and LwM2MClient.delayedRequests.add(path)
     * #2.1 if there is not a difference in values between the current resource values and the shared attribute values
     *
     * @param tsKvProtos
     * @param sessionInfo
     */
    public void updateAttributeFromThingsboard(List<TransportProtos.TsKvProto> tsKvProtos, TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2MClient = clientContext.getClientBySessionInfo(sessionInfo);
        if (lwM2MClient != null) {
            log.warn("1) UpdateAttributeFromThingsboard, tsKvProtos [{}]", tsKvProtos);
            tsKvProtos.forEach(tsKvProto -> {
                String pathIdVer = this.getPresentPathIntoProfile(sessionInfo, tsKvProto.getKv().getKey());
                if (pathIdVer != null) {
                    // #1.1
                    if (lwM2MClient.getDelayedRequests().containsKey(pathIdVer) && tsKvProto.getTs() > lwM2MClient.getDelayedRequests().get(pathIdVer).getTs()) {
                        lwM2MClient.getDelayedRequests().put(pathIdVer, tsKvProto);
                    } else if (!lwM2MClient.getDelayedRequests().containsKey(pathIdVer)) {
                        lwM2MClient.getDelayedRequests().put(pathIdVer, tsKvProto);
                    }
                }
            });
            // #2.1
            lwM2MClient.getDelayedRequests().forEach((pathIdVer, tsKvProto) -> {
                this.updateResourcesValueToClient(lwM2MClient, this.getResourceValueFormatKv(lwM2MClient, pathIdVer),
                        getValueFromKvProto(tsKvProto.getKv()), pathIdVer);
            });
        } else {
            log.error("UpdateAttributeFromThingsboard, lwM2MClient is null");
        }
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
            transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(this, sessionInfo));
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
    public void putDelayedUpdateResourcesThingsboard(LwM2mClient lwM2MClient) {
        SessionInfoProto sessionInfo = this.getSessionInfo(lwM2MClient);
        if (sessionInfo != null) {
            //#1.1
            ConcurrentMap<String, String> keyNamesMap = this.getNamesFromProfileForSharedAttributes(lwM2MClient);
            if (keyNamesMap.values().size() > 0) {
                try {
                    //#1.2
                    TransportProtos.GetAttributeRequestMsg getAttributeMsg = adaptor.convertToGetAttributes(null, keyNamesMap.values());
                    transportService.process(sessionInfo, getAttributeMsg, getAckCallback(lwM2MClient, getAttributeMsg.getRequestId(), DEVICE_ATTRIBUTES_REQUEST));
                } catch (AdaptorException e) {
                    log.trace("Failed to decode get attributes request", e);
                }
            }

        }
    }

    public void getInfoFirmwareUpdate(LwM2mClient lwM2MClient, LwM2mClientRpcRequest rpcRequest) {
        if (lwM2MClient.getRegistration().getSupportedVersion(FW_5_ID) != null) {
            SessionInfoProto sessionInfo = this.getSessionInfo(lwM2MClient);
            if (sessionInfo != null) {
                DefaultLwM2MTransportMsgHandler handler = this;
                this.transportService.process(sessionInfo, createOtaPackageRequestMsg(sessionInfo, OtaPackageType.FIRMWARE.name()),
                        new TransportServiceCallback<>() {
                            @Override
                            public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
                                if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())
                                        && response.getType().equals(OtaPackageType.FIRMWARE.name())) {
                                    LwM2mFwSwUpdate fwUpdate = lwM2MClient.getFwUpdate(clientContext);
                                    if (rpcRequest != null) {
                                        fwUpdate.setStateUpdate(INITIATED.name());
                                    }
                                    if (!FAILED.name().equals(fwUpdate.getStateUpdate())) {
                                        log.warn("7) firmware start with ver: [{}]", response.getVersion());
                                        fwUpdate.setRpcRequest(rpcRequest);
                                        fwUpdate.setCurrentVersion(response.getVersion());
                                        fwUpdate.setCurrentTitle(response.getTitle());
                                        fwUpdate.setCurrentId(new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB()));
                                        if (rpcRequest == null) {
                                            fwUpdate.sendReadObserveInfo(lwM2mTransportRequest);
                                        } else {
                                            fwUpdate.writeFwSwWare(handler, lwM2mTransportRequest);
                                        }
                                    } else {
                                        String msgError = String.format("OtaPackage device: %s, version: %s, stateUpdate: %s",
                                                lwM2MClient.getDeviceName(), response.getVersion(), fwUpdate.getStateUpdate());
                                        log.warn("7_1 [{}]", msgError);
                                    }
                                } else {
                                    String msgError = String.format("OtaPackage device: %s, responseStatus: %s",
                                            lwM2MClient.getDeviceName(), response.getResponseStatus().toString());
                                    log.trace(msgError);
                                    if (rpcRequest != null) {
                                        sendErrorRpcResponse(rpcRequest, msgError, sessionInfo);
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                log.trace("Failed to process firmwareUpdate ", e);
                            }
                        });
            }
        }
    }

    public void getInfoSoftwareUpdate(LwM2mClient lwM2MClient, LwM2mClientRpcRequest rpcRequest) {
        if (lwM2MClient.getRegistration().getSupportedVersion(SW_ID) != null) {
            SessionInfoProto sessionInfo = this.getSessionInfo(lwM2MClient);
            if (sessionInfo != null) {
                DefaultLwM2MTransportMsgHandler handler = this;
                transportService.process(sessionInfo, createOtaPackageRequestMsg(sessionInfo, OtaPackageType.SOFTWARE.name()),
                        new TransportServiceCallback<>() {
                            @Override
                            public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
                                if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())
                                        && response.getType().equals(OtaPackageType.SOFTWARE.name())) {
                                    lwM2MClient.getSwUpdate().setRpcRequest(rpcRequest);
                                    lwM2MClient.getSwUpdate().setCurrentVersion(response.getVersion());
                                    lwM2MClient.getSwUpdate().setCurrentTitle(response.getTitle());
                                    lwM2MClient.getSwUpdate().setCurrentId(new OtaPackageId(new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB())).getId());
                                    lwM2MClient.getSwUpdate().sendReadObserveInfo(lwM2mTransportRequest);
                                    if (rpcRequest == null) {
                                        lwM2MClient.getSwUpdate().sendReadObserveInfo(lwM2mTransportRequest);
                                    } else {
                                        lwM2MClient.getSwUpdate().writeFwSwWare(handler, lwM2mTransportRequest);
                                    }
                                } else {
                                    log.trace("Software [{}] [{}]", lwM2MClient.getDeviceName(), response.getResponseStatus().toString());
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                log.trace("Failed to process softwareUpdate ", e);
                            }
                        });
            }
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

    /**
     * !!!  sharedAttr === profileAttr  !!!
     * Get names or keyNames from profile:  resources IsWritable
     *
     * @param lwM2MClient -
     * @return ArrayList  keyNames from profile profileAttr && IsWritable
     */
    private ConcurrentMap<String, String> getNamesFromProfileForSharedAttributes(LwM2mClient lwM2MClient) {

        LwM2mClientProfile profile = clientContext.getProfile(lwM2MClient.getProfileId());
        return new Gson().fromJson(profile.getPostKeyNameProfile().toString(),
                new TypeToken<ConcurrentHashMap<String, String>>() {
                }.getType());
    }

    private boolean validateResourceInModel(LwM2mClient lwM2mClient, String pathIdVer, boolean isWritableNotOptional) {
        ResourceModel resourceModel = lwM2mClient.getResourceModel(pathIdVer, this.config
                .getModelProvider());
        Integer objectId = new LwM2mPath(convertPathFromIdVerToObjectId(pathIdVer)).getObjectId();
        String objectVer = validateObjectVerFromKey(pathIdVer);
        return resourceModel != null && (isWritableNotOptional ?
                objectId != null && objectVer != null && objectVer.equals(lwM2mClient.getRegistration().getSupportedVersion(objectId)) && resourceModel.operations.isWritable() :
                objectId != null && objectVer != null && objectVer.equals(lwM2mClient.getRegistration().getSupportedVersion(objectId)));
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
