/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfigService;
import org.thingsboard.server.transport.lwm2m.server.session.LwM2MSessionManager;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MClientStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LwM2mClientContextImplTest {

    private static LwM2mClientContextImpl clientContext;

    @BeforeAll
    static void setUp() {
        var transportContextMock = mock(LwM2mTransportContext.class);
        var transportServerConfigMock = mock(LwM2MTransportServerConfig.class);
        var securityStoreMock = mock(TbMainSecurityStore.class);
        var clientStoreMock = mock(TbLwM2MClientStore.class);
        var sessionManagerMock = mock(LwM2MSessionManager.class);
        var deviceProfileCacheMock = mock(TransportDeviceProfileCache.class);
        var modelConfigServiceMock = mock(LwM2MModelConfigService.class);

        clientContext = new LwM2mClientContextImpl(transportContextMock, transportServerConfigMock, securityStoreMock,
                clientStoreMock, sessionManagerMock, deviceProfileCacheMock, modelConfigServiceMock);
    }

    @Test
    public void onUplinkWithClientWithoutPowerModeButProfileWithPowerModeDRX() {
        var profileId = UUID.randomUUID();
        var lwm2mSettingsMock = mock(OtherConfiguration.class);
        when(lwm2mSettingsMock.getPowerMode()).thenReturn(PowerMode.DRX);
        var transportConfigurationMock = mock(Lwm2mDeviceProfileTransportConfiguration.class);
        when(transportConfigurationMock.getType()).thenReturn(DeviceTransportType.LWM2M);
        when(transportConfigurationMock.getClientLwM2mSettings()).thenReturn(lwm2mSettingsMock);
        var profileDataMock = mock(DeviceProfileData.class);
        when(profileDataMock.getTransportConfiguration()).thenReturn(transportConfigurationMock);
        var deviceProfileMock = mock(DeviceProfile.class);
        when(deviceProfileMock.getProfileData()).thenReturn(profileDataMock);
        when(deviceProfileMock.getUuidId()).thenReturn(profileId);

        clientContext.profileUpdate(deviceProfileMock);

        var clientMock = mock(LwM2mClient.class);
        when(clientMock.getPowerMode()).thenReturn(null);
        when(clientMock.getProfileId()).thenReturn(profileId);

        clientContext.onUplink(clientMock);
        verify(clientMock, times(1)).updateLastUplinkTime();
    }

    @Test
    public void onUplinkWithClientWithoutPowerModeOrProfile() {
        LwM2mClient clientMock = mock(LwM2mClient.class);
        when(clientMock.getPowerMode()).thenReturn(null);
        when(clientMock.getDeviceId()).thenReturn(null);

        clientContext.onUplink(clientMock);
        verify(clientMock, times(1)).updateLastUplinkTime();
    }
}