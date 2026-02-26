/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PingControllerTest {
    @Mock
    private DeviceService deviceService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PingController pingController;

    private TenantId tenantId = new TenantId(UUID.randomUUID());
    private DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @Before
    public void setUp() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setTenantId(tenantId);
        securityUser.setEmail("test@thingsboard.org");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        SecurityContextHolder.setContext(securityContext);

}

@Test
public void testPingDeviceById_DeviceReachable() throws Exception {
    Device device = new Device();
    device.setId(deviceId);
    device.setTenantId(tenantId);
    device.setCreatedTime(System.currentTimeMillis());

    when(deviceService.findDeviceById(any(), any())).thenReturn(device);

    Map<String, Object> response = pingController.pingDeviceById(deviceId.getId());
    Assert.assertEquals(deviceId.getId().toString(), response.get("deviceId"));
    Assert.assertTrue((Boolean) response.get("reachable"));
}


@Test
public void testPingDeviceById_DeviceNotReachable() throws Exception {
    Device device = new Device();
    device.setId(deviceId);
    device.setTenantId(tenantId);
    device.setCreatedTime(System.currentTimeMillis() - (25 * 60 * 60 * 1000)); // 25 hours ago

    when(deviceService.findDeviceById(any(), any())).thenReturn(device);

    Map<String, Object> response = pingController.pingDeviceById(deviceId.getId());
    Assert.assertEquals(false, response.get("reachable"));
}
}