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

import org.thingsboard.server.common.data.device.data.lwm2m.OtherConfiguration;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClient;

import java.util.Optional;

public interface LwM2MOtaUpdateService {

    void init(LwM2MClient client);

    void forceFirmwareUpdate(LwM2MClient client);

    void onTargetFirmwareUpdate(LwM2MClient client, String newFwTitle, String newFwVersion, Optional<String> newFwUrl, Optional<String> newFwTag);

    void onTargetSoftwareUpdate(LwM2MClient client, String newSwTitle, String newSwVersion, Optional<String> newSwUrl, Optional<String> newSwTag);

    void onCurrentFirmwareNameUpdate(LwM2MClient client, String name);

    void onFirmwareStrategyUpdate(LwM2MClient client, OtherConfiguration configuration);

    void onCurrentSoftwareStrategyUpdate(LwM2MClient client, OtherConfiguration configuration);

    void onCurrentFirmwareVersion3Update(LwM2MClient client, String version);

    void onCurrentFirmwareVersionUpdate(LwM2MClient client, String version);

    void onCurrentFirmwareStateUpdate(LwM2MClient client, Long state);

    void onCurrentFirmwareResultUpdate(LwM2MClient client, Long result);

    void onCurrentFirmwareDeliveryMethodUpdate(LwM2MClient lwM2MClient, Long value);

    void onCurrentSoftwareNameUpdate(LwM2MClient lwM2MClient, String name);

    void onCurrentSoftwareVersion3Update(LwM2MClient lwM2MClient, String version);

    void onCurrentSoftwareVersionUpdate(LwM2MClient client, String version);

    void onCurrentSoftwareStateUpdate(LwM2MClient lwM2MClient, Long value);

    void onCurrentSoftwareResultUpdate(LwM2MClient client, Long result);
}
