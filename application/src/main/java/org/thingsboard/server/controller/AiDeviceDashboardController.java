// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.service.validator.DashboardDataValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.dashboard.AiDeviceDashboardService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/ai/devices")
class AiDeviceDashboardController extends BaseController {

    private final AiDeviceDashboardService aiDeviceDashboardService;
    private final DashboardDataValidator dashboardDataValidator;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{deviceId}/dashboard")
    JsonNode generateDashboard(
            @PathVariable UUID deviceId,
            @RequestBody JsonNode request,
            @RequestHeader(AUTHORIZATION_HEADER) String tbAccessToken
    ) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        checkDeviceId(new DeviceId(deviceId), Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY);
        accessControlService.checkPermission(user, Resource.DASHBOARD, Operation.CREATE);
        dashboardDataValidator.validateMaxDashboardsPerTenant(user.getTenantId());
        return aiDeviceDashboardService.generateDashboard(deviceId, request, tbAccessToken, user);
    }

}
