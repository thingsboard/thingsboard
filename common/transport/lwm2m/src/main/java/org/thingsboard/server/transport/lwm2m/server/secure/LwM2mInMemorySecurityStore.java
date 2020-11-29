/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.lwm2m.server.secure;

import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MGetSecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.ReadResultSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportService;
import org.thingsboard.server.transport.lwm2m.server.client.AttrTelemetryObserveValue;
import org.thingsboard.server.transport.lwm2m.server.client.ModelClient;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.*;

@Slf4j
@Component("LwM2mInMemorySecurityStore")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' )|| ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2mInMemorySecurityStore extends InMemorySecurityStore {
    // lock for the two maps
    protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();
    private final boolean infosAreCompromised = false;
    protected Map<String /** registrationId */, ModelClient> sessions = new ConcurrentHashMap<>();
    protected Map<UUID /** registrationId */, AttrTelemetryObserveValue> profiles = new ConcurrentHashMap<>();
    protected Set<String> removeSessions = ConcurrentHashMap.newKeySet();
    private SecurityStoreListener listener;

    @Autowired
    LwM2MGetSecurityInfo lwM2MGetSecurityInfo;

    @Override
    public SecurityInfo getByEndpoint(String endPoint) {
        readLock.lock();
        try {
            String registrationId = this.getByRegistrationId(endPoint, null);
            return (registrationId != null && sessions.size() > 0 && sessions.get(registrationId) != null) ? sessions.get(registrationId).getInfo() : this.add(endPoint);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        readLock.lock();
        try {
            String integrationId = this.getByRegistrationId(null, identity);
            return (integrationId != null) ? sessions.get(integrationId).getInfo() : add(identity);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        readLock.lock();
        try {
            return Collections.unmodifiableCollection(this.sessions.entrySet().stream().map(model -> model.getValue().getInfo()).collect(Collectors.toList()));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Using  for the next connection of clients
     * if the "unreg" does not have time to remove the registration from the session
     * @param registrationId
     */
    public void addRemoveSessions(String registrationId) {
        removeSessions.add(registrationId);
    }

    /**
     * Using  if the "unreg" is access
     */
    public void delRemoveSessions() {
        try {
            removeSessions.stream().forEach(regId -> {
                delRemoveSessionAndListener(regId);
                removeSessions.remove(regId);
            });
        } finally {
        }
    }

    /**
     * Removed registration Client from sessions and listener
     * @param registrationId if Client
     */
    private void delRemoveSessionAndListener(String registrationId) {
        writeLock.lock();
        try {
            ModelClient modelClient = (sessions.get(registrationId) != null) ? sessions.get(registrationId) : null;
            if (modelClient != null) {
                if (listener != null) {
                    listener.securityInfoRemoved(infosAreCompromised, modelClient.getInfo());
                }
                sessions.remove(registrationId);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void setListener(SecurityStoreListener listener) {
        this.listener = listener;
    }

    public ModelClient getByModelClient(String endPoint, String identity) {
        Map.Entry<String, ModelClient> modelClients = (endPoint != null) ?
                this.sessions.entrySet().stream().filter(model -> endPoint.equals(model.getValue().getEndPoint())).findAny().orElse(null) :
                this.sessions.entrySet().stream().filter(model -> identity.equals(model.getValue().getIdentity())).findAny().orElse(null);
        return (modelClients != null) ? modelClients.getValue() : null;
    }

    public ModelClient getByRegistrationIdModelClient(String registrationId) {
        return this.sessions.get(registrationId);
    }

    private String getByRegistrationId(String endPoint, String identity) {
        List<String> registrationIds = (endPoint != null) ?
                this.sessions.entrySet().stream().filter(model -> endPoint.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()) :
                this.sessions.entrySet().stream().filter(model -> identity.equals(model.getValue().getIdentity())).map(model -> model.getKey()).collect(Collectors.toList());
        return (registrationIds != null && registrationIds.size() > 0) ? registrationIds.get(0) : null;
    }

    public String getByRegistrationId(String credentialsId) {
        List<String> registrationIds = (this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()).size() > 0) ?
                this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()) :
                this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getIdentity())).map(model -> model.getKey()).collect(Collectors.toList());
        return (registrationIds != null && registrationIds.size() > 0) ? registrationIds.get(0) : null;
    }

    public Registration getByRegistration(String registrationId) {
        return this.sessions.get(registrationId).getRegistration();
    }

    private SecurityInfo add(String identity) {
        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(identity, TypeServer.CLIENT);
        UUID profileUuid = (addUpdateProfileParameters(store.getDeviceProfile())) ? store.getDeviceProfile().getUuidId() : null;
        if (store.getSecurityInfo() != null) {
            if (store.getSecurityMode() < DEFAULT_MODE.code) {
                String endpoint = store.getSecurityInfo().getEndpoint();
                sessions.put(endpoint, new ModelClient(endpoint, store.getSecurityInfo().getIdentity(), store.getSecurityInfo(), store.getMsg(), null, null, profileUuid));

            }
        } else {
            if (store.getSecurityMode() == NO_SEC.code)
                sessions.put(identity, new ModelClient(identity, null, null, store.getMsg(), null, null, profileUuid));
            else log.error("Registration failed: FORBIDDEN, endpointId: [{}]", identity);
        }
        return store.getSecurityInfo();
    }

    @SneakyThrows
    public ModelClient getModel(LeshanServer lwServer, Registration registration, LwM2MTransportService transportService) {
        writeLock.lock();
        try {
            if (this.sessions.get(registration.getEndpoint()) == null) {
                this.add(registration.getEndpoint());
            }
            ModelClient  modelClient = (ModelClient) this.sessions.get(registration.getEndpoint()).clone();
            modelClient.setRegistrationParam(lwServer, registration);
            modelClient.setAttributes(registration.getAdditionalRegistrationAttributes());
            this.sessions.put(registration.getId(), modelClient);
            this.sessions.remove(registration.getEndpoint());
            return modelClient;
        } finally {
            writeLock.unlock();
        }
    }

    public Map<String, ModelClient> getSessions() {
        return this.sessions;
    }

    public Map<UUID, AttrTelemetryObserveValue> getProfiles() {
        return this.profiles;
    }

    /**
     *
     * @param deviceProfile
     */
    public boolean addUpdateProfileParameters(DeviceProfile deviceProfile) {
        JsonObject profilesConfigData = LwM2MTransportHandler.getObserveAttrTelemetryFromThingsboard(deviceProfile);
        if (profilesConfigData != null) {
            boolean updateProfile = (profiles.get(deviceProfile.getUuidId()) != null);
            profiles.put(deviceProfile.getUuidId(), LwM2MTransportHandler.getNewProfileParameters(profilesConfigData));
            // TODO   if (updateProfile)
        }
        return (profilesConfigData != null);
    }

}
