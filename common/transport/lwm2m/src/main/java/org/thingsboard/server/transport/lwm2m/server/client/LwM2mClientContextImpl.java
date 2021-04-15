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

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.thingsboard.server.transport.lwm2m.secure.ReadResultSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.convertToIdVerFromObjectId;

@Service
@TbLwM2mTransportComponent
public class LwM2mClientContextImpl implements LwM2mClientContext {

    private static final boolean INFOS_ARE_COMPROMISED = false;

    private final Map<String /** registrationId */, LwM2mClient> lwM2mClients = new ConcurrentHashMap<>();
    private Map<UUID /** profileUUid */, LwM2mClientProfile> profiles = new ConcurrentHashMap<>();

    private final LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator;

    private final EditableSecurityStore securityStore;

    public LwM2mClientContextImpl(LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator, EditableSecurityStore securityStore) {
        this.lwM2MCredentialsSecurityInfoValidator = lwM2MCredentialsSecurityInfoValidator;
        this.securityStore = securityStore;
    }

    public void delRemoveSessionAndListener(String registrationId) {
        LwM2mClient lwM2MClient = this.lwM2mClients.get(registrationId);
        if (lwM2MClient != null) {
            this.securityStore.remove(lwM2MClient.getEndpoint(), INFOS_ARE_COMPROMISED);
            this.lwM2mClients.remove(registrationId);
        }
    }

    @Override
    public LwM2mClient getLwM2MClient(String endPoint, String identity) {
        Map.Entry<String, LwM2mClient> modelClients = endPoint != null ?
                this.lwM2mClients.entrySet().stream().filter(model -> endPoint.equals(model.getValue().getEndpoint())).findAny().orElse(null) :
                this.lwM2mClients.entrySet().stream().filter(model -> identity.equals(model.getValue().getIdentity())).findAny().orElse(null);
        return modelClients != null ? modelClients.getValue() : null;
    }

    @Override
    public LwM2mClient getLwM2MClient(TransportProtos.SessionInfoProto sessionInfo) {
        return getLwM2mClient(new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
    }

    @Override
    public LwM2mClient getLwM2mClient(UUID sessionId) {
        return lwM2mClients.values().stream().filter(c -> c.getSessionId().equals(sessionId)).findAny().get();
    }

    @Override
    public LwM2mClient getLwM2mClientWithReg(Registration registration, String registrationId) {
        LwM2mClient client = registrationId != null ?
                this.lwM2mClients.get(registrationId) :
                this.lwM2mClients.containsKey(registration.getId()) ?
                        this.lwM2mClients.get(registration.getId()) :
                        this.lwM2mClients.get(registration.getEndpoint());
        return client != null ? client : updateInSessionsLwM2MClient(registration);
    }

    @Override
    public LwM2mClient updateInSessionsLwM2MClient(Registration registration) {
        if (this.lwM2mClients.get(registration.getEndpoint()) == null) {
            this.addLwM2mClientToSession(registration.getEndpoint());
        }
        LwM2mClient lwM2MClient = lwM2mClients.get(registration.getEndpoint());
        lwM2MClient.setRegistration(registration);
        this.lwM2mClients.remove(registration.getEndpoint());
        this.lwM2mClients.put(registration.getId(), lwM2MClient);
        return lwM2MClient;
    }

    public Registration getRegistration(String registrationId) {
        return this.lwM2mClients.get(registrationId).getRegistration();
    }

    /**
     * Add new LwM2MClient to session
     *
     * @param identity-
     * @return SecurityInfo. If error - SecurityInfoError
     * and log:
     * - FORBIDDEN - if there is no authorization
     * - profileUuid - if the device does not have a profile
     * - device - if the thingsboard does not have a device with a name equal to the identity
     */
    @Override
    public LwM2mClient addLwM2mClientToSession(String identity) {
        ReadResultSecurityStore store = lwM2MCredentialsSecurityInfoValidator.createAndValidateCredentialsSecurityInfo(identity, TypeServer.CLIENT);
        if (store.getSecurityMode() < LwM2MSecurityMode.DEFAULT_MODE.code) {
            UUID profileUuid = (store.getDeviceProfile() != null && addUpdateProfileParameters(store.getDeviceProfile())) ? store.getDeviceProfile().getUuidId() : null;
            LwM2mClient client;
            if (store.getSecurityInfo() != null && profileUuid != null) {
                String endpoint = store.getSecurityInfo().getEndpoint();
                client = new LwM2mClient(endpoint, store.getSecurityInfo().getIdentity(), store.getSecurityInfo(), store.getMsg(), profileUuid, UUID.randomUUID());
                lwM2mClients.put(endpoint, client);
            } else if (store.getSecurityMode() == NO_SEC.code && profileUuid != null) {
                client = new LwM2mClient(identity, null, null, store.getMsg(), profileUuid, UUID.randomUUID());
                lwM2mClients.put(identity, client);
            } else {
                throw new RuntimeException(String.format("Registration failed: FORBIDDEN/profileUuid/device %s , endpointId: %s  [PSK]", profileUuid, identity));
            }
            return client;
        } else {
            throw new RuntimeException(String.format("Registration failed: FORBIDDEN, endpointId: %s", identity));
        }
    }

    @Override
    public Map<String, LwM2mClient> getLwM2mClients() {
        return lwM2mClients;
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
        return this.getProfiles().get(getLwM2mClientWithReg(registration, null).getProfileId());
    }

    @Override
    public Map<UUID, LwM2mClientProfile> setProfiles(Map<UUID, LwM2mClientProfile> profiles) {
        return this.profiles = profiles;
    }

    @Override
    public boolean addUpdateProfileParameters(DeviceProfile deviceProfile) {
        LwM2mClientProfile lwM2MClientProfile = LwM2mTransportHandler.getLwM2MClientProfileFromThingsboard(deviceProfile);
        if (lwM2MClientProfile != null) {
            profiles.put(deviceProfile.getUuidId(), lwM2MClientProfile);
            return true;
        }
        return false;
    }

    /**
     * if isVer - ok or default ver=DEFAULT_LWM2M_VERSION
     * @param registration -
     * @return - all objectIdVer in client
     */
    @Override
    public Set<String> getSupportedIdVerInClient(Registration registration) {
        Set<String> clientObjects = ConcurrentHashMap.newKeySet();
        Arrays.stream(registration.getObjectLinks()).forEach(url -> {
            LwM2mPath pathIds = new LwM2mPath(url.getUrl());
            if (!pathIds.isRoot()) {
                clientObjects.add(convertToIdVerFromObjectId(url.getUrl(),  registration));
            }
        });
        return (clientObjects.size() > 0) ? clientObjects : null;
    }
}
