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
package org.thingsboard.server.transport.lwm2m.server.ota;

import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Optional;

public interface LwM2MOtaUpdateService {

    void init(LwM2mClient client);

    void forceFirmwareUpdate(LwM2mClient client);

    void onTargetFirmwareUpdate(LwM2mClient client, String newFirmwareTitle, String newFirmwareVersion, Optional<String> newFirmwareUrl);

    void onTargetSoftwareUpdate(LwM2mClient client, String newSoftwareTitle, String newSoftwareVersion);

    void onCurrentFirmwareNameUpdate(LwM2mClient client, String name);

    void onCurrentFirmwareVersion3Update(LwM2mClient client, String version);

    void onCurrentFirmwareVersion5Update(LwM2mClient client, String version);

    void onCurrentFirmwareStateUpdate(LwM2mClient client, Long state);

    void onCurrentFirmwareResultUpdate(LwM2mClient client, Long result);

    void onCurrentFirmwareDeliveryMethodUpdate(LwM2mClient lwM2MClient, Long value);
}
