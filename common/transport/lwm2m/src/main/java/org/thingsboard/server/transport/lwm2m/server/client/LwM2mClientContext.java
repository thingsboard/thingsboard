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
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Map;
import java.util.UUID;

public interface LwM2mClientContext {

    void delRemoveSessionAndListener(String registrationId);

    LwM2mClient getLwM2MClient(String endPoint, String identity);

    LwM2mClient getLwM2MClient(TransportProtos.SessionInfoProto sessionInfo);

    LwM2mClient getLwM2mClient(UUID sessionId);

    LwM2mClient getLwM2mClientWithReg(Registration registration, String registrationId);

    LwM2mClient updateInSessionsLwM2MClient(Registration registration);

    Registration getRegistration(String registrationId);

    Map<String, LwM2mClient> getLwM2mClients();

    Map<UUID, LwM2mClientProfile> getProfiles();

    LwM2mClientProfile getProfile(UUID profileUuId);

    LwM2mClientProfile getProfile(Registration registration);

    Map<UUID, LwM2mClientProfile> setProfiles(Map<UUID, LwM2mClientProfile> profiles);

    boolean addUpdateProfileParameters(DeviceProfile deviceProfile);
}
