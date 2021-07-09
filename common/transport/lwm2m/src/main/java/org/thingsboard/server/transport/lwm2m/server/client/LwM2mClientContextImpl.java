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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2mVersion;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.session.LwM2MSessionManager;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MClientStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.DefaultLwM2MUplinkMsgHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.eclipse.leshan.core.SecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertObjectIdToVersionedId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.validateObjectVerFromKey;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mClientContextImpl implements LwM2mClientContext {

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    private final TbMainSecurityStore securityStore;
    private final TbLwM2MClientStore clientStore;
    private final LwM2MSessionManager sessionManager;
    private final TransportDeviceProfileCache deviceProfileCache;

    @Autowired
    @Lazy
    private DefaultLwM2MUplinkMsgHandler defaultLwM2MUplinkMsgHandler;
    @Autowired
    @Lazy
    private LwM2MOtaUpdateService otaUpdateService;

    private final Map<String, LwM2mClient> lwM2mClientsByEndpoint = new ConcurrentHashMap<>();
    private final Map<String, LwM2mClient> lwM2mClientsByRegistrationId = new ConcurrentHashMap<>();
    private final Map<UUID, Lwm2mDeviceProfileTransportConfiguration> profiles = new ConcurrentHashMap<>();

    @AfterStartUp
    public void init() {
        String nodeId = context.getNodeId();
        Set<LwM2mClient> fetchedClients = clientStore.getAll();
        log.debug("Fetched clients from store: {}", fetchedClients);
        fetchedClients.forEach(client -> {
            lwM2mClientsByEndpoint.put(client.getEndpoint(), client);
            updateFetchedClient(nodeId, client);
        });
    }

    @Override
    public LwM2mClient getClientByEndpoint(String endpoint) {
        return lwM2mClientsByEndpoint.computeIfAbsent(endpoint, ep -> {
            LwM2mClient client = clientStore.get(ep);
            String nodeId = context.getNodeId();
            if (client == null) {
                log.info("[{}] initialized new client.", endpoint);
                client = new LwM2mClient(nodeId, ep);
            } else {
                log.debug("[{}] fetched client from store: {}", endpoint, client);
                updateFetchedClient(nodeId, client);
            }
            return client;
        });
    }

    private void updateFetchedClient(String nodeId, LwM2mClient client) {
        boolean updated = false;
        if (client.getRegistration() != null) {
            lwM2mClientsByRegistrationId.put(client.getRegistration().getId(), client);
        }
        if (client.getSession() != null) {
            client.refreshSessionId(nodeId);
            sessionManager.register(client.getSession());
            updated = true;
        }
        if (updated) {
            clientStore.put(client);
        }
    }

    @Override
    public Optional<TransportProtos.SessionInfoProto> register(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        TransportProtos.SessionInfoProto oldSession = null;
        client.lock();
        try {
            if (LwM2MClientState.UNREGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            oldSession = client.getSession();
            TbLwM2MSecurityInfo securityInfo = securityStore.getTbLwM2MSecurityInfoByEndpoint(client.getEndpoint());
            if (securityInfo.getSecurityMode() != null) {
                if (SecurityMode.X509.equals(securityInfo.getSecurityMode())) {
                    securityStore.registerX509(registration.getEndpoint(), registration.getId());
                }
                if (securityInfo.getDeviceProfile() != null) {
                    profileUpdate(securityInfo.getDeviceProfile());
                    if (securityInfo.getSecurityInfo() != null) {
                        client.init(securityInfo.getMsg(), UUID.randomUUID());
                    } else if (NO_SEC.equals(securityInfo.getSecurityMode())) {
                        client.init(securityInfo.getMsg(), UUID.randomUUID());
                    } else {
                        throw new RuntimeException(String.format("Registration failed: device %s not found.", client.getEndpoint()));
                    }
                } else {
                    throw new RuntimeException(String.format("Registration failed: device %s not found.", client.getEndpoint()));
                }
            } else {
                throw new RuntimeException(String.format("Registration failed: FORBIDDEN, endpointId: %s", client.getEndpoint()));
            }
            client.setRegistration(registration);
            this.lwM2mClientsByRegistrationId.put(registration.getId(), client);
            client.setState(LwM2MClientState.REGISTERED);
            clientStore.put(client);
        } finally {
            client.unlock();
        }
        return Optional.ofNullable(oldSession);
    }

    @Override
    public void updateRegistration(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        client.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            client.setRegistration(registration);
            clientStore.put(client);
            sendMsgsAfterSleeping(client);
        } finally {
            client.unlock();
        }
    }

    @Override
    public void unregister(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        client.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            lwM2mClientsByRegistrationId.remove(registration.getId());
            Registration currentRegistration = client.getRegistration();
            if (currentRegistration.getId().equals(registration.getId())) {
                client.setState(LwM2MClientState.UNREGISTERED);
                lwM2mClientsByEndpoint.remove(client.getEndpoint());
//                TODO: change tests to use new certificate.
//                this.securityStore.remove(client.getEndpoint(), registration.getId());
                clientStore.remove(client.getEndpoint());
                UUID profileId = client.getProfileId();
                if (profileId != null) {
                    Optional<LwM2mClient> otherClients = lwM2mClientsByRegistrationId.values().stream().filter(e -> e.getProfileId().equals(profileId)).findFirst();
                    if (otherClients.isEmpty()) {
                        profiles.remove(profileId);
                    }
                }
            } else {
                throw new LwM2MClientStateException(client.getState(), "Client has different registration.");
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    public LwM2mClient getClientBySessionInfo(TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2mClient = null;
        UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
        Predicate<LwM2mClient> isClientFilter = c ->
                sessionId.equals((new UUID(c.getSession().getSessionIdMSB(), c.getSession().getSessionIdLSB())));
        if (this.lwM2mClientsByEndpoint.size() > 0) {
            lwM2mClient = this.lwM2mClientsByEndpoint.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null && this.lwM2mClientsByRegistrationId.size() > 0) {
            lwM2mClient = this.lwM2mClientsByRegistrationId.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null) {
            log.error("[{}] Failed to lookup client by session id.", sessionId);
        }
        return lwM2mClient;
    }

    @Override
    public String getObjectIdByKeyNameFromProfile(LwM2mClient client, String keyName) {
        Lwm2mDeviceProfileTransportConfiguration profile = getProfile(client.getProfileId());

        return profile.getObserveAttr().getKeyName().entrySet().stream()
                .filter(e -> e.getValue().equals(keyName) && validateResourceInModel(client, e.getKey(), false)).findFirst().orElseThrow(
                        () -> new IllegalArgumentException(keyName + " is not configured in the device profile!")
                ).getKey();
    }

    public Registration getRegistration(String registrationId) {
        return this.lwM2mClientsByRegistrationId.get(registrationId).getRegistration();
    }

    @Override
    public void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials) {
        LwM2mClient client = getClientByEndpoint(registration.getEndpoint());
        client.init(credentials, UUID.randomUUID());
        lwM2mClientsByRegistrationId.put(registration.getId(), client);
        profileUpdate(credentials.getDeviceProfile());
    }

    @Override
    public void update(LwM2mClient client) {
        client.lock();
        try {
            clientStore.put(client);
        } finally {
            client.unlock();
        }
    }

    @Override
    public void removeCredentials(TransportProtos.SessionInfoProto sessionInfo) {
        //TODO: implement
    }

    @Override
    public void sendMsgsAfterSleeping(LwM2mClient lwM2MClient) {
        if (LwM2MClientState.REGISTERED.equals(lwM2MClient.getState())) {
            PowerMode powerMode = lwM2MClient.getPowerMode();
            if (powerMode == null) {
                Lwm2mDeviceProfileTransportConfiguration deviceProfile = getProfile(lwM2MClient.getProfileId());
                powerMode = deviceProfile.getClientLwM2mSettings().getPowerMode();
            }

            if (PowerMode.PSM.equals(powerMode) || PowerMode.E_DRX.equals(powerMode)) {
                defaultLwM2MUplinkMsgHandler.initAttributes(lwM2MClient);
                TransportProtos.TransportToDeviceActorMsg persistentRpcRequestMsg = TransportProtos.TransportToDeviceActorMsg
                        .newBuilder()
                        .setSessionInfo(lwM2MClient.getSession())
                        .setSendPendingRPC(TransportProtos.SendPendingRPCMsg.newBuilder().build())
                        .build();
                context.getTransportService().process(persistentRpcRequestMsg, TransportServiceCallback.EMPTY);
                otaUpdateService.init(lwM2MClient);
            }
        }
    }

    @Override
    public Collection<LwM2mClient> getLwM2mClients() {
        return lwM2mClientsByEndpoint.values();
    }

    @Override
    public Lwm2mDeviceProfileTransportConfiguration getProfile(UUID profileId) {
        return doGetAndCache(profileId);
    }

    @Override
    public Lwm2mDeviceProfileTransportConfiguration getProfile(Registration registration) {
        UUID profileId = getClientByEndpoint(registration.getEndpoint()).getProfileId();
        return doGetAndCache(profileId);
    }

    private Lwm2mDeviceProfileTransportConfiguration doGetAndCache(UUID profileId) {
        Lwm2mDeviceProfileTransportConfiguration result = profiles.get(profileId);
        if (result == null) {
            log.debug("Fetching profile [{}]", profileId);
            DeviceProfile deviceProfile = deviceProfileCache.get(new DeviceProfileId(profileId));
            if (deviceProfile != null) {
                result = profileUpdate(deviceProfile);
            } else {
                log.info("Device profile was not found! Most probably device profile [{}] has been removed from the database.", profileId);
            }
        }
        return result;
    }

    @Override
    public Lwm2mDeviceProfileTransportConfiguration profileUpdate(DeviceProfile deviceProfile) {
        Lwm2mDeviceProfileTransportConfiguration clientProfile = LwM2mTransportUtil.toLwM2MClientProfile(deviceProfile);
        profiles.put(deviceProfile.getUuidId(), clientProfile);
        return clientProfile;
    }

    @Override
    public Set<String> getSupportedIdVerInClient(LwM2mClient client) {
        Set<String> clientObjects = ConcurrentHashMap.newKeySet();
        Arrays.stream(client.getRegistration().getObjectLinks()).forEach(link -> {
            LwM2mPath pathIds = new LwM2mPath(link.getUrl());
            if (!pathIds.isRoot()) {
                clientObjects.add(convertObjectIdToVersionedId(link.getUrl(), client.getRegistration()));
            }
        });
        return (clientObjects.size() > 0) ? clientObjects : null;
    }

    @Override
    public LwM2mClient getClientByDeviceId(UUID deviceId) {
        return lwM2mClientsByRegistrationId.values().stream().filter(e -> deviceId.equals(e.getDeviceId())).findFirst().orElse(null);
    }

    @Override
    public boolean isComposite(LwM2mClient client) {
        return LwM2mVersion.fromVersionStr(client.getRegistration().getLwM2mVersion()).isComposite() &
                getProfile(client.getProfileId()).getClientLwM2mSettings().isCompositeOperationsSupport();
    }

    @Override
    public Long getRequestTimeout(LwM2mClient client) {
        var clientProfile = getProfile(client.getProfileId());
        OtherConfiguration clientLwM2mSettings = clientProfile.getClientLwM2mSettings();
        Long timeout = null;
        if (PowerMode.E_DRX.equals(clientLwM2mSettings.getPowerMode())) {
            timeout = clientLwM2mSettings.getEdrxCycle();
        }
        if (timeout == null || timeout == 0L) {
            timeout = this.config.getTimeout();
        }
        return timeout;
    }

    private boolean validateResourceInModel(LwM2mClient lwM2mClient, String pathIdVer, boolean isWritableNotOptional) {
        ResourceModel resourceModel = lwM2mClient.getResourceModel(pathIdVer, this.config
                .getModelProvider());
        Integer objectId = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)).getObjectId();
        String objectVer = validateObjectVerFromKey(pathIdVer);
        return resourceModel != null && (isWritableNotOptional ?
                objectId != null && objectVer != null && objectVer.equals(lwM2mClient.getRegistration().getSupportedVersion(objectId)) && resourceModel.operations.isWritable() :
                objectId != null && objectVer != null && objectVer.equals(lwM2mClient.getRegistration().getSupportedVersion(objectId)));
    }

}
