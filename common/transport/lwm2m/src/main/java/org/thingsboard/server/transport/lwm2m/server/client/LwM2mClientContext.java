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

import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface LwM2mClientContext {

    LwM2mClient getClientByRegistrationId(String registrationId);

    LwM2mClient getClientByEndpoint(String endpoint);

    LwM2mClient getClientBySessionInfo(TransportProtos.SessionInfoProto sessionInfo);

    void register(LwM2mClient lwM2MClient, Registration registration) throws LwM2MClientStateException;

    void updateRegistration(LwM2mClient client, Registration registration) throws LwM2MClientStateException;

    void unregister(LwM2mClient client, Registration registration) throws LwM2MClientStateException;

    Collection<LwM2mClient> getLwM2mClients();

    Map<UUID, LwM2mClientProfile> getProfiles();

    LwM2mClientProfile getProfile(UUID profileUuId);

    LwM2mClientProfile getProfile(Registration registration);

    Map<UUID, LwM2mClientProfile> setProfiles(Map<UUID, LwM2mClientProfile> profiles);

    LwM2mClientProfile profileUpdate(DeviceProfile deviceProfile);

    Set<String> getSupportedIdVerInClient(LwM2mClient registration);

    LwM2mClient getClientByDeviceId(UUID deviceId);

    void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials);


}
