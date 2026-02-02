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
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class TrendzController extends BaseController {

    private final TrendzSettingsService trendzSettingsService;

    @ApiOperation(value = "Save Trendz settings (saveTrendzSettings)",
            notes = "Saves Trendz settings for this tenant.\n" + NEW_LINE +
                    "Here is an example of the Trendz settings:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"enabled\": true,\n" +
                    "  \"baseUrl\": \"https://some.domain.com:18888/also_necessary_prefix\"\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/trendz/settings")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public TrendzSettings saveTrendzSettings(@RequestBody TrendzSettings trendzSettings,
                                             @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        TenantId tenantId = user.getTenantId();
        trendzSettingsService.saveTrendzSettings(tenantId, trendzSettings);
        return trendzSettings;
    }

    @ApiOperation(value = "Get Trendz Settings (getTrendzSettings)",
            notes = "Retrieves Trendz settings for this tenant." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @GetMapping("/trendz/settings")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public TrendzSettings getTrendzSettings(@AuthenticationPrincipal SecurityUser user) {
        TenantId tenantId = user.getTenantId();
        return trendzSettingsService.findTrendzSettings(tenantId);
    }

}
