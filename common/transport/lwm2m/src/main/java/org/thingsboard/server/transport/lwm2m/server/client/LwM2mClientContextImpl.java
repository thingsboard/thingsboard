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
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;
import org.thingsboard.server.transport.lwm2m.server.store.TbEditableSecurityStore;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.eclipse.leshan.core.SecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromObjectIdToIdVer;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mClientContextImpl implements LwM2mClientContext {

    private final LwM2mTransportContext context;
    private final TbEditableSecurityStore securityStore;
    private final Map<String, LwM2mClient> lwM2mClientsByEndpoint = new ConcurrentHashMap<>();
    private final Map<String, LwM2mClient> lwM2mClientsByRegistrationId = new ConcurrentHashMap<>();
    private Map<UUID, LwM2mClientProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public LwM2mClient getClientByEndpoint(String endpoint) {
        return lwM2mClientsByEndpoint.computeIfAbsent(endpoint, ep -> new LwM2mClient(context.getNodeId(), ep));
    }

    @Override
    public void register(LwM2mClient lwM2MClient, Registration registration) throws LwM2MClientStateException {
        lwM2MClient.lock();
        try {
            if (LwM2MClientState.UNREGISTERED.equals(lwM2MClient.getState())) {
                throw new LwM2MClientStateException(lwM2MClient.getState(), "Client is in invalid state.");
            }
            TbLwM2MSecurityInfo securityInfo = securityStore.getTbLwM2MSecurityInfoByEndpoint(lwM2MClient.getEndpoint());
            if (securityInfo.getSecurityMode() != null) {
                if (securityInfo.getDeviceProfile() != null) {
                    UUID profileUuid = profileUpdate(securityInfo.getDeviceProfile()) != null ? securityInfo.getDeviceProfile().getUuidId() : null;
                    if (securityInfo.getSecurityInfo() != null) {
                        lwM2MClient.init(securityInfo.getSecurityInfo().getIdentity(), securityInfo.getSecurityInfo(), securityInfo.getMsg(), profileUuid, UUID.randomUUID());
                    } else if (NO_SEC.equals(securityInfo.getSecurityMode())) {
                        lwM2MClient.init(null, null, securityInfo.getMsg(), profileUuid, UUID.randomUUID());
                    } else {
                        throw new RuntimeException(String.format("Registration failed: device %s not found.", lwM2MClient.getEndpoint()));
                    }
                } else {
                    throw new RuntimeException(String.format("Registration failed: device %s not found.", lwM2MClient.getEndpoint()));
                }
            } else {
                throw new RuntimeException(String.format("Registration failed: FORBIDDEN, endpointId: %s", lwM2MClient.getEndpoint()));
            }
            lwM2MClient.setRegistration(registration);
            this.lwM2mClientsByRegistrationId.put(registration.getId(), lwM2MClient);
            lwM2MClient.setState(LwM2MClientState.REGISTERED);
        } finally {
            lwM2MClient.unlock();
        }
    }

    @Override
    public void updateRegistration(LwM2mClient lwM2MClient, Registration registration) throws LwM2MClientStateException {
        lwM2MClient.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(lwM2MClient.getState())) {
                throw new LwM2MClientStateException(lwM2MClient.getState(), "Client is in invalid state.");
            }
            lwM2MClient.setRegistration(registration);
        } finally {
            lwM2MClient.unlock();
        }
    }

    @Override
    public void unregister(LwM2mClient lwM2MClient, Registration registration) throws LwM2MClientStateException {
        lwM2MClient.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(lwM2MClient.getState())) {
                throw new LwM2MClientStateException(lwM2MClient.getState(), "Client is in invalid state.");
            }
            lwM2mClientsByRegistrationId.remove(registration.getId());
            Registration currentRegistration = lwM2MClient.getRegistration();
            if (currentRegistration.getId().equals(registration.getId())) {
                lwM2MClient.setState(LwM2MClientState.UNREGISTERED);
                lwM2mClientsByEndpoint.remove(lwM2MClient.getEndpoint());
                this.securityStore.remove(lwM2MClient.getEndpoint());
                UUID profileId = lwM2MClient.getProfileId();
                if (profileId != null) {
                    Optional<LwM2mClient> otherClients = lwM2mClientsByRegistrationId.values().stream().filter(e -> e.getProfileId().equals(profileId)).findFirst();
                    if (otherClients.isEmpty()) {
                        profiles.remove(profileId);
                    }
                }
            } else {
                throw new LwM2MClientStateException(lwM2MClient.getState(), "Client has different registration.");
            }
        } finally {
            lwM2MClient.unlock();
        }
    }

    @Override
    public LwM2mClient getClientByRegistrationId(String registrationId) {
        return lwM2mClientsByRegistrationId.get(registrationId);
    }

    @Override
    public LwM2mClient getClientBySessionInfo(TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2mClient = null;
        Predicate<LwM2mClient> isClientFilter = c ->
                (new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()))
                        .equals((new UUID(c.getSession().getSessionIdMSB(), c.getSession().getSessionIdLSB())));
        if (this.lwM2mClientsByEndpoint.size() > 0) {
            lwM2mClient = this.lwM2mClientsByEndpoint.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null && this.lwM2mClientsByRegistrationId.size() > 0) {
            lwM2mClient = this.lwM2mClientsByRegistrationId.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null) {
            log.warn("Device TimeOut? lwM2mClient is null.");
            log.warn("SessionInfo input [{}], lwM2mClientsByEndpoint size: [{}] lwM2mClientsByRegistrationId: [{}]", sessionInfo, lwM2mClientsByEndpoint.values(), lwM2mClientsByRegistrationId.values());
            log.error("", new RuntimeException());
        }
        return lwM2mClient;
    }

    public Registration getRegistration(String registrationId) {
        return this.lwM2mClientsByRegistrationId.get(registrationId).getRegistration();
    }

    @Override
    public void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials) {
        LwM2mClient client = getClientByEndpoint(registration.getEndpoint());
        client.init(null, null, credentials, credentials.getDeviceProfile().getUuidId(), UUID.randomUUID());
        lwM2mClientsByRegistrationId.put(registration.getId(), client);
        profileUpdate(credentials.getDeviceProfile());
    }

    @Override
    public Collection<LwM2mClient> getLwM2mClients() {
        return lwM2mClientsByEndpoint.values();
    }

    @Override
    public Map<UUID, LwM2mClientProfile> getProfiles() {
        return profiles;
    }

    @Override
    public LwM2mClientProfile getProfile(UUID profileId) {
        return profiles.get(profileId);
    }

    @Override
    public LwM2mClientProfile getProfile(Registration registration) {
        return this.getProfiles().get(getClientByEndpoint(registration.getEndpoint()).getProfileId());
    }

    @Override
    public Map<UUID, LwM2mClientProfile> setProfiles(Map<UUID, LwM2mClientProfile> profiles) {
        return this.profiles = profiles;
    }

    @Override
    public LwM2mClientProfile profileUpdate(DeviceProfile deviceProfile) {
        LwM2mClientProfile lwM2MClientProfile = deviceProfile != null ?
                LwM2mTransportUtil.toLwM2MClientProfile(deviceProfile) : null;
        if (lwM2MClientProfile != null) {
            profiles.put(deviceProfile.getUuidId(), lwM2MClientProfile);
            return lwM2MClientProfile;
        } else {
            return null;
        }
    }

    @Override
    public Set<String> getSupportedIdVerInClient(LwM2mClient client) {
        Set<String> clientObjects = ConcurrentHashMap.newKeySet();
        Arrays.stream(client.getRegistration().getObjectLinks()).forEach(link -> {
            LwM2mPath pathIds = new LwM2mPath(link.getUrl());
            if (!pathIds.isRoot()) {
                clientObjects.add(convertPathFromObjectIdToIdVer(link.getUrl(), client.getRegistration()));
            }
        });
        return (clientObjects.size() > 0) ? clientObjects : null;
    }

    @Override
    public LwM2mClient getClientByDeviceId(UUID deviceId) {
        return lwM2mClientsByRegistrationId.values().stream().filter(e -> deviceId.equals(e.getDeviceId())).findFirst().orElse(null);
    }

}
