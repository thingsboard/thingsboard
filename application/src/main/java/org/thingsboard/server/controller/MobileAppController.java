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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.common.data.mobile.MobileAppInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.mobile.TbMobileAppService;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MobileAppController extends BaseController {

    private final TbMobileAppService tbMobileAppService;

    @ApiOperation(value = "Save Or update Mobile app (saveMobileApp)",
            notes = "Create or update the Mobile app. When creating mobile app, platform generates Mobile App Id as " + UUID_WIKI_LINK +
                    "The newly created Mobile App Id will be present in the response. " +
                    "Specify existing Mobile App Id to update the domain. " +
                    "Referencing non-existing Mobile App Id will cause 'Not Found' error." +
                    "\n\nMobile app package name is unique for entire platform setup.\n\n")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping(value = "/mobileApp")
    public MobileApp saveMobileApp(
            @Parameter(description = "A JSON value representing the Domain.", required = true)
            @RequestBody MobileApp mobileApp,
            @Parameter(description = "A list of entity group ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam(name = "oauth2ClientIds", required = false) UUID[] oauth2ClientIds) throws Exception {
        mobileApp.setTenantId(getCurrentUser().getTenantId());

        List<OAuth2ClientId> oAuth2Clients = new ArrayList<>();
        for (UUID id : oauth2ClientIds) {
            OAuth2ClientId oauth2ClientId = new OAuth2ClientId(id);
            checkOauth2ClientId(oauth2ClientId, Operation.READ);
            oAuth2Clients.add(oauth2ClientId);
        }
        return tbMobileAppService.save(mobileApp, oAuth2Clients, getCurrentUser());
    }

    @ApiOperation(value = "Update oauth2 clients (updateOauth2Clients)",
            notes = "Update oauth2 clients to the specified mobile app. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping(value = "/mobileApp/{id}/oauth2Clients")
    public void updateOauth2Clients(@PathVariable UUID id,
                                         @RequestBody UUID[] oauth2ClientIds) throws ThingsboardException {
        MobileAppId mobileAppId = new MobileAppId(id);
        MobileApp mobileApp = null;
        try {
            mobileApp = checkMobileAppId(mobileAppId, Operation.WRITE);
            List<OAuth2ClientId> oAuth2ClientIds = new ArrayList<>();
            for (UUID outh2CLientId : oauth2ClientIds) {
                OAuth2ClientId oAuth2ClientId = new OAuth2ClientId(outh2CLientId);
                checkEntityId(oAuth2ClientId, Operation.READ);
                oAuth2ClientIds.add(oAuth2ClientId);
            }
            mobileAppService.updateOauth2Clients(getTenantId(), mobileAppId, oAuth2ClientIds);
        } catch (Exception e) {
            if (mobileApp != null) {
                logEntityActionService.logEntityAction(getTenantId(), mobileAppId, mobileApp,
                        ActionType.UPDATED, getCurrentUser(), e);
            }
            throw e;
        }
    }

    @ApiOperation(value = "Get mobile app infos (getMobileAppInfos)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping(value = "/mobileApp/infos")
    public List<MobileAppInfo> getMobileAppInfos() throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return mobileAppService.findMobileAppInfosByTenantId(tenantId);
    }

    @ApiOperation(value = "Get mobile info by id (getMobileAppInfoById)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping(value = "/mobileApp/info/{id}")
    public MobileAppInfo getMobileAppInfoById(@PathVariable UUID id) throws ThingsboardException {
        MobileAppId mobileAppId = new MobileAppId(id);
        return checkEntityId(mobileAppId, mobileAppService::findMobileAppInfoById, Operation.READ);
    }

    @ApiOperation(value = "Delete Mobile App by ID (deleteMobileApp)",
            notes = "Deletes Mobile App by ID. Referencing non-existing asset Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @DeleteMapping(value = "/mobileApp/{id}")
    public void deleteMobileApp(@PathVariable UUID id) throws Exception {
        MobileAppId mobileAppId = new MobileAppId(id);
        checkMobileAppId(mobileAppId, Operation.DELETE);
        mobileAppService.deleteMobileAppById(getTenantId(), mobileAppId);
    }

}
