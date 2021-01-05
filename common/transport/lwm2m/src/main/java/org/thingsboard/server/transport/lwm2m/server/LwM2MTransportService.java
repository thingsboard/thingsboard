/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Collection;
import java.util.Optional;

public interface LwM2MTransportService {

    void onRegistered(LeshanServer lwServer, Registration registration, Collection<Observation> previousObsersations);

    void updatedReg(LeshanServer lwServer, Registration registration);

    void unReg(Registration registration, Collection<Observation> observations);

    void onSleepingDev(Registration registration);

    void setCancelObservations(LeshanServer lwServer, Registration registration);

    void setCancelObservationRecourse(LeshanServer lwServer, Registration registration, String path);

    void onObservationResponse(Registration registration, String path, ReadResponse response);

    void onAttributeUpdate(TransportProtos.AttributeUpdateNotificationMsg msg, TransportProtos.SessionInfoProto sessionInfo);

    void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile);

    void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt);

    void doTrigger(LeshanServer lwServer, Registration registration, String path);

    void doDisconnect(TransportProtos.SessionInfoProto sessionInfo);


}
