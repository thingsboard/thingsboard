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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api")

public class PingController extends BaseController {

    @Autowired
    private DeviceService deviceService;

    @Operation(summary = "Ping Device by ID", description = "Pings a device by its ID to check connectivity.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/ping/{deviceId}", method = RequestMethod.GET)
    @ResponseBody

    public Map<String, Object> pingDeviceById(
            @Parameter(description = "The ID of the device to ping") @PathVariable("deviceId") UUID deviceId) throws Exception {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        Device device = deviceService.findDeviceById(tenantId, new DeviceId(deviceId));
        if (device == null) {
            throw new IllegalArgumentException("Device not found with ID: " + deviceId);
        }
        long lastActivityTime = device.getCreatedTime();
        boolean isReachable = (System.currentTimeMillis() - lastActivityTime) <(24 * 60 * 60 * 1000) ;

        Map<String, Object> response = new HashMap<>();
        response.put("deviceId", deviceId.toString());
        response.put("reachable", isReachable);
        response.put("lastSeen", lastActivityTime);
        return response;
    }
}