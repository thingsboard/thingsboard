/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.gateway_device;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class GatewayFlagCacheTest {

    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("a00ec470-c6b4-11ef-8c88-63b5533fb5bc"));
    final DeviceId deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

    @Mock
    DeviceService deviceService;

    GatewayFlagCache cache;

    @BeforeEach
    void setup() {
        cache = new GatewayFlagCache(deviceService);
        ReflectionTestUtils.setField(cache, "maxSize", 100);
        ReflectionTestUtils.setField(cache, "timeToLiveInMinutes", 5);
        cache.init();
    }

    Device device(boolean gateway) {
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        if (gateway) {
            device.setAdditionalInfo(JacksonUtil.newObjectNode().put(DataConstants.GATEWAY_PARAMETER, true));
        }
        return device;
    }

    @Test
    void unknownFlagIsTreatedAsPotentialGateway() {
        assertThat(cache.isPotentialGateway(deviceId)).isTrue();
    }

    @Test
    void resolvedFlagIsCachedAndUsedByPotentialGatewayCheck() {
        given(deviceService.findDeviceById(tenantId, deviceId)).willReturn(device(false));

        assertThat(cache.isGateway(tenantId, deviceId)).isFalse();
        assertThat(cache.isPotentialGateway(deviceId)).isFalse();

        // subsequent checks are served from the cache without a device lookup
        assertThat(cache.isGateway(tenantId, deviceId)).isFalse();
        then(deviceService).should(times(1)).findDeviceById(tenantId, deviceId);
    }

    @Test
    void flagIsResolvedAgainAfterInvalidation() {
        given(deviceService.findDeviceById(tenantId, deviceId)).willReturn(device(false));
        assertThat(cache.isGateway(tenantId, deviceId)).isFalse();

        cache.onDeviceLifecycleEvent(ComponentLifecycleMsg.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .event(ComponentLifecycleEvent.UPDATED)
                .build());

        given(deviceService.findDeviceById(tenantId, deviceId)).willReturn(device(true));
        assertThat(cache.isGateway(tenantId, deviceId)).isTrue();
        then(deviceService).should(times(2)).findDeviceById(tenantId, deviceId);
    }

    @Test
    void missingDeviceIsResolvedAsNotAGateway() {
        given(deviceService.findDeviceById(tenantId, deviceId)).willReturn(null);

        assertThat(cache.isGateway(tenantId, deviceId)).isFalse();
        assertThat(cache.isPotentialGateway(deviceId)).isFalse();
    }

    @Test
    void deviceLifecycleEventInvalidatesTheFlag() {
        given(deviceService.findDeviceById(tenantId, deviceId)).willReturn(device(false));
        cache.isGateway(tenantId, deviceId);
        assertThat(cache.isPotentialGateway(deviceId)).isFalse();

        cache.onDeviceLifecycleEvent(ComponentLifecycleMsg.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .event(ComponentLifecycleEvent.UPDATED)
                .build());

        assertThat(cache.isPotentialGateway(deviceId)).isTrue();
    }

    @Test
    void nonDeviceLifecycleEventIsIgnored() {
        given(deviceService.findDeviceById(tenantId, deviceId)).willReturn(device(false));
        cache.isGateway(tenantId, deviceId);

        cache.onDeviceLifecycleEvent(ComponentLifecycleMsg.builder()
                .tenantId(tenantId)
                .entityId(new AssetId(deviceId.getId()))
                .event(ComponentLifecycleEvent.UPDATED)
                .build());

        assertThat(cache.isPotentialGateway(deviceId)).isFalse();
    }

}
