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
    private final Map<String, LwM2mClient> lwM2mClientsByRegistrationId = new ConcurrentHashMap<>();
    private Map<UUID, LwM2mClientProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<LwM2mClient> register(Registration registration){
        LwM2mClient lwM2MClient = new LwM2mClient(context.getNodeId(), registration);
        lwM2MClient.lock();
        try {
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
        } finally {
            lwM2MClient.unlock();
        }
        return Optional.ofNullable(lwM2MClient);
    }

    @Override
    public Optional<LwM2mClient>  updateRegistration(Registration registration) {
        Optional<LwM2mClient> lwM2mClient = Optional.ofNullable(this.lwM2mClientsByRegistrationId.get(registration.getId()));
        if (lwM2mClient.isPresent() && !lwM2mClient.get().getRegistration().getIdentity().equals(registration.getIdentity())) {
            lwM2mClient.get().setRegistration(registration);
        }
        return lwM2mClient;
    }

    @Override
    public Optional<LwM2mClient>  unregister(Registration registration) {
        LwM2mClient lwM2MClient = this.lwM2mClientsByRegistrationId.get(registration.getId());
        lwM2MClient.lock();
        try {
            lwM2mClientsByRegistrationId.remove(registration.getId());
                this.securityStore.remove(lwM2MClient.getEndpoint());
                UUID profileId = lwM2MClient.getProfileId();
                if (profileId != null) {
                    Optional<LwM2mClient> otherClients = lwM2mClientsByRegistrationId.values().stream().filter(e -> e.getProfileId().equals(profileId)).findFirst();
                    if (otherClients.isEmpty()) {
                        profiles.remove(profileId);
                    }
                }
        } finally {
            lwM2MClient.unlock();
        }
        return Optional.ofNullable(lwM2MClient);
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
        if (lwM2mClient == null && this.lwM2mClientsByRegistrationId.size() > 0) {
            lwM2mClient = this.lwM2mClientsByRegistrationId.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null) {
            log.warn("Device TimeOut? lwM2mClient is null.");
            if (lwM2mClientsByRegistrationId.size() > 0) {
                log.warn("000.10_1) SessionInfo input [{}], " +
                                "lwM2mClientsByRegistrationId_EndPoint: [{}], lwM2mClientsByRegistrationId_Registration: [{}], lwM2mClientsByRegistrationId_Session: [{}]",
                        new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()),
                        ((LwM2mClient) lwM2mClientsByRegistrationId.values().toArray()[0]).getEndpoint(),
                        ((LwM2mClient) lwM2mClientsByRegistrationId.values().toArray()[0]).getRegistration().getId(),
                        new UUID(((LwM2mClient) lwM2mClientsByRegistrationId.values().toArray()[0]).getSession().getSessionIdMSB(), ((LwM2mClient) lwM2mClientsByRegistrationId.values().toArray()[0]).getSession().getSessionIdLSB()));

            }
            else {
                log.warn("000.10_2) SessionInfo input [{}], lwM2mClientsByRegistrationId.size = 0",
                        new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
            }
            log.error("", new RuntimeException());
        }
        return lwM2mClient;
    }

    public Registration getRegistration(String registrationId) {
        return this.lwM2mClientsByRegistrationId.get(registrationId).getRegistration();
    }

    @Override
    public void registerClientIsX509(Registration registration, ValidateDeviceCredentialsResponse credentials) {
        Optional<LwM2mClient> lwM2MClient = this.updateRegistration(registration);
        if (lwM2MClient.isPresent()) {
            lwM2MClient.get().init(null, null, credentials, credentials.getDeviceProfile().getUuidId(), UUID.randomUUID());
            lwM2mClientsByRegistrationId.put(registration.getId(), lwM2MClient.get());
            profileUpdate(credentials.getDeviceProfile());
        }
    }

    @Override
    public Collection<LwM2mClient> getLwM2mClients() {
        return lwM2mClientsByRegistrationId.values();
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
        Optional<LwM2mClient> lwM2MClient = this.updateRegistration(registration);
        return lwM2MClient.isPresent() ? this.getProfiles().get(lwM2MClient.get().getProfileId()) : null;
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
