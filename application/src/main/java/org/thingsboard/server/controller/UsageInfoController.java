// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.usage.UsageInfoService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import static org.thingsboard.server.common.data.exception.ThingsboardErrorCode.PERMISSION_DENIED;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class UsageInfoController extends BaseController {

    @Autowired
    private UsageInfoService usageInfoService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/usage")
    public UsageInfo getTenantUsageInfo() throws ThingsboardException {
        if (!getMergedUserPermissions(getCurrentUser(), false).hasGenericPermission(Resource.ALL, Operation.READ)) {
            throw new ThingsboardException("You don't have permission to read UsageInfo!", PERMISSION_DENIED);
        }
        return checkNotNull(usageInfoService.getUsageInfo(getCurrentUser().getTenantId()));
    }

}
