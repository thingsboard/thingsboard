// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.util.IntegrationMqttClientSettingsComponent;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;

import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class LocalIntegrationContextTest {

    LocalIntegrationContext localIntegrationContext;
    IntegrationContextComponent ctx;
    PlatformIntegrationService platformIntegrationService;
    Integration configuration;
    @Mock
    IntegrationCallback<Void> callback;
    @Mock
    Runnable runnable;

    @BeforeEach
    void setUp() {
        configuration = new Integration(new IntegrationId(UUID.randomUUID()));
        platformIntegrationService = mock(PlatformIntegrationService.class);
        ctx = mock(IntegrationContextComponent.class);
        willReturn(platformIntegrationService).given(ctx).getPlatformIntegrationService();

        var mqttClientRetransmissionSettings = new IntegrationMqttClientSettingsComponent();
        mqttClientRetransmissionSettings.setRetransmissionMaxAttempts(3);
        mqttClientRetransmissionSettings.setRetransmissionInitialDelayMillis(5000L);
        mqttClientRetransmissionSettings.setRetransmissionJitterFactor(0.15);

        localIntegrationContext = spy(new LocalIntegrationContext(ctx, configuration, mqttClientRetransmissionSettings));
    }

    @Test
    void testProcessUplinkDataDeviceRun() {
        DeviceUplinkDataProto uplinkData = mock(DeviceUplinkDataProto.class);
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, uplinkData, callback);
        localIntegrationContext.processUplinkData(uplinkData, callback);
        Mockito.verify(runnable).run();
    }

    @Test
    void testProcessUplinkDataAssetRun() {
        AssetUplinkDataProto uplinkData = mock(AssetUplinkDataProto.class);
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, uplinkData, callback);
        localIntegrationContext.processUplinkData(uplinkData, callback);
        Mockito.verify(runnable).run();
    }

}
