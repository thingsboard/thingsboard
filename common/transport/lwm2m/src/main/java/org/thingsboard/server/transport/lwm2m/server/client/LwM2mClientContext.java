/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LwM2mClientContext {

    LwM2mClient getClientByEndpoint(String endpoint);

    LwM2mClient getClientBySessionInfo(TransportProtos.SessionInfoProto sessionInfo);

    Optional<TransportProtos.SessionInfoProto> register(LwM2mClient lwM2MClient, Registration registration) throws LwM2MClientStateException;

    void updateRegistration(LwM2mClient client, Registration registration) throws LwM2MClientStateException;

    void unregister(LwM2mClient client, Registration registration) throws LwM2MClientStateException;

    Collection<LwM2mClient> getLwM2mClients();

    Lwm2mDeviceProfileTransportConfiguration getProfile(Registration registration);

    Lwm2mDeviceProfileTransportConfiguration profileUpdate(DeviceProfile deviceProfile);

    Set<String> getSupportedIdVerInClient(LwM2mClient registration);

    LwM2mClient getClientByDeviceId(UUID deviceId);

    String getObjectIdByKeyNameFromProfile(LwM2mClient lwM2mClient, String keyName);

    void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials);

    void update(LwM2mClient lwM2MClient);

    void sendMsgsAfterSleeping(LwM2mClient lwM2MClient);

    void onUplink(LwM2mClient client);

    Long getRequestTimeout(LwM2mClient client);

    boolean asleep(LwM2mClient client);

    boolean awake(LwM2mClient client);

    boolean isDownlinkAllowed(LwM2mClient client);

}
