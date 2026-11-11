// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.integration.api.util.IntegrationMqttClientSettingsComponent;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.integration.service.api.IntegrationApiService;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbIntegrationExecutorComponent;
import org.thingsboard.server.service.integration.EventStorageService;
import org.thingsboard.server.service.integration.IntegrationContextProvider;

@TbIntegrationExecutorComponent
@Service
@RequiredArgsConstructor
public class TbIntegrationExecutorContextProvider implements IntegrationContextProvider {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final IntegrationApiService apiService;
    private final IntegrationStatisticsService statisticsService;
    private final TbIntegrationExecutorContextComponent contextComponent;
    private final LogSettingsComponent logSettingsComponent;
    private final IntegrationMqttClientSettingsComponent integrationMqttClientSettingsComponent;
    private final EventStorageService eventStorageService;

    @Override
    public IntegrationContext buildIntegrationContext(Integration configuration) {
        return new TbIntegrationExecutorIntegrationContext(
                serviceInfoProvider.getServiceId(), apiService, statisticsService,
                contextComponent, logSettingsComponent, integrationMqttClientSettingsComponent, configuration, eventStorageService
        );
    }

}
