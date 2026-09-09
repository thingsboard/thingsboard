// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.ota;

import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Optional;

public interface LwM2MOtaUpdateService {

    void init(LwM2mClient client);

    void forceFirmwareUpdate(LwM2mClient client);

    void onTargetFirmwareUpdate(LwM2mClient client, String newFwTitle, String newFwVersion, Optional<String> newFwUrl, Optional<String> newFwTag);

    void onTargetSoftwareUpdate(LwM2mClient client, String newSwTitle, String newSwVersion, Optional<String> newSwUrl, Optional<String> newSwTag);

    void onCurrentFirmwareNameUpdate(LwM2mClient client, String name);

    void onFirmwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration);

    void onCurrentSoftwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration);

    void onCurrentFirmwareVersion3Update(LwM2mClient client, String version);

    void onCurrentFirmwareVersionUpdate(LwM2mClient client, String version);

    void onCurrentFirmwareStateUpdate(LwM2mClient client, Long state);

    void onCurrentFirmwareResultUpdate(LwM2mClient client, Long result);

    void onCurrentFirmwareDeliveryMethodUpdate(LwM2mClient lwM2MClient, Long value);

    void onCurrentSoftwareNameUpdate(LwM2mClient lwM2MClient, String name);

    void onCurrentSoftwareVersion3Update(LwM2mClient lwM2MClient, String version);

    void onCurrentSoftwareVersionUpdate(LwM2mClient client, String version);

    void onCurrentSoftwareStateUpdate(LwM2mClient lwM2MClient, Long value);

    void onCurrentSoftwareResultUpdate(LwM2mClient client, Long result);

    boolean isOtaDownloading(LwM2mClient client);
}
