// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.usage.UsageInfoService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class UsageInfoController extends BaseController {

    @Autowired
    private UsageInfoService usageInfoService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/usage", method = RequestMethod.GET)
    @ResponseBody
    public UsageInfo getTenantUsageInfo() throws ThingsboardException {
        return checkNotNull(usageInfoService.getUsageInfo(getCurrentUser().getTenantId()));
    }
}
