/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.device;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessorTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.willReturn;

public abstract class AbstractDeviceProcessorTest extends BaseEdgeProcessorTest {

    protected DeviceId deviceId;
    protected DeviceProfileId deviceProfileId;
    protected DeviceProfile deviceProfile;

    @BeforeEach
    public void setUp() {
        edgeId = new EdgeId(UUID.randomUUID());
        tenantId = new TenantId(UUID.randomUUID());
        deviceId = new DeviceId(UUID.randomUUID());
        deviceProfileId = new DeviceProfileId(UUID.randomUUID());

        deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        deviceProfile.setName("DeviceProfile");
        deviceProfile.setDefault(true);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);

        Device device = new Device();
        device.setDeviceProfileId(deviceProfileId);
        device.setId(deviceId);
        device.setName("Device");
        device.setType(deviceProfile.getName());

        edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(EdgeEventActionType.ADDED);


        willReturn(device).given(deviceService).findDeviceById(tenantId, deviceId);
        willReturn(deviceProfile).given(deviceProfileService).findDeviceProfileById(tenantId, deviceProfileId);
        willReturn(new byte[]{0x00}).given(dataDecodingEncodingService).encode(deviceProfileData);

    }

    protected void updateDeviceProfileDefaultFields(long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                                                    long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        DashboardId dashboardId = getDashboardId(expectedDashboardIdMSB, expectedDashboardIdLSB);
        RuleChainId ruleChainId = getRuleChainId(expectedRuleChainIdMSB, expectedRuleChainIdLSB);

        deviceProfile.setDefaultDashboardId(dashboardId);
        deviceProfile.setDefaultEdgeRuleChainId(ruleChainId);

    }

    protected void verify(DownlinkMsg downlinkMsg, long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                          long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = downlinkMsg.getDeviceProfileUpdateMsgList().get(0);
        assertNotNull(deviceProfileUpdateMsg);
        Assertions.assertThat(deviceProfileUpdateMsg.getDefaultDashboardIdMSB()).isEqualTo(expectedDashboardIdMSB);
        Assertions.assertThat(deviceProfileUpdateMsg.getDefaultDashboardIdLSB()).isEqualTo(expectedDashboardIdLSB);
        Assertions.assertThat(deviceProfileUpdateMsg.getDefaultRuleChainIdMSB()).isEqualTo(expectedRuleChainIdMSB);
        Assertions.assertThat(deviceProfileUpdateMsg.getDefaultRuleChainIdLSB()).isEqualTo(expectedRuleChainIdLSB);
    }
}
