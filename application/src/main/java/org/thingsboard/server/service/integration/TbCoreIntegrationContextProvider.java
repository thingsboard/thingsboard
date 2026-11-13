// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.util.IntegrationMqttClientSettingsComponent;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.queue.util.TbCoreComponent;

@TbCoreComponent
@RequiredArgsConstructor
@Service
public class TbCoreIntegrationContextProvider implements IntegrationContextProvider {

    private final IntegrationContextComponent contextComponent;
    private final IntegrationMqttClientSettingsComponent integrationMqttClientSettingsComponent;

    @Override
    public IntegrationContext buildIntegrationContext(Integration configuration) {
        return new LocalIntegrationContext(contextComponent, configuration, integrationMqttClientSettingsComponent);
    }

}
