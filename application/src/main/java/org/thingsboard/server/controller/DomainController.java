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
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.domain.TbDomainService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.common.data.audit.ActionType.UPDATED_OAUTH2_CLIENTS;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DomainController extends BaseController {

    private final TbDomainService tbDomainService;

    @ApiOperation(value = "Save or Update Domain (saveDomain)",
            notes = "Create or update the Domain. When creating domain, platform generates Domain Id as " + UUID_WIKI_LINK +
                    "The newly created Domain Id will be present in the response. " +
                    "Specify existing Domain Id to update the domain. " +
                    "Referencing non-existing Domain Id will cause 'Not Found' error." +
                    "\n\nDomain name is unique for entire platform setup.\n\n")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping(value = "/domain")
    public Domain saveDomain(
            @Parameter(description = "A JSON value representing the Domain.", required = true)
            @RequestBody Domain domain,
            @Parameter(description = "A list of oauth2 client registration ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam(name = "oauth2ClientRegistrationIds", required = false) String[] ids) throws Exception {
        List<String> oauth2ClientIds = ids != null ? Arrays.asList(ids) : Collections.emptyList();
        domain.setTenantId(getCurrentUser().getTenantId());
        checkEntity(domain.getId(), domain, Resource.DOMAIN);
        List<OAuth2RegistrationId> oAuth2ClientIds = new ArrayList<>();
        for (String id : oauth2ClientIds) {
            OAuth2RegistrationId oauth2ClientId = new OAuth2RegistrationId(toUUID(id));
            checkOauth2ClientId(oauth2ClientId, Operation.READ);
            oAuth2ClientIds.add(oauth2ClientId);
        }
        return tbDomainService.save(domain, oAuth2ClientIds, getCurrentUser());
    }

    @ApiOperation(value = "Update oauth2 clients (updateOauth2Clients)",
            notes = "Update oauth2 clients for the specified domain. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping(value = "/domain/{id}/oauth2Clients")
    public void updateOauth2Clients(@PathVariable UUID id,
                                         @RequestBody UUID[] oauth2ClientIds) throws ThingsboardException {
        DomainId domainId = new DomainId(id);
        Domain domain = null;
        try {
            domain = checkDomainId(domainId, Operation.WRITE);
            List<OAuth2RegistrationId> oAuth2ClientIds = new ArrayList<>();
            for (UUID outh2CLientId : oauth2ClientIds) {
                OAuth2RegistrationId oAuth2RegistrationId = new OAuth2RegistrationId(outh2CLientId);
                checkEntityId(oAuth2RegistrationId, Operation.READ);
                oAuth2ClientIds.add(oAuth2RegistrationId);
            }
            domainService.updateOauth2Clients(getTenantId(), domainId, oAuth2ClientIds);
            logEntityActionService.logEntityAction(domain.getTenantId(), domain.getId(), domain,
                    UPDATED_OAUTH2_CLIENTS, getCurrentUser(), oAuth2ClientIds.toString());
        } catch (Exception e) {
            if (domain != null) {
                    logEntityActionService.logEntityAction(getTenantId(), domainId, domain,
                            ActionType.UPDATED_OAUTH2_CLIENTS, getCurrentUser(), e);
            }
            throw e;
        }
    }

    @ApiOperation(value = "Get Domain infos (getDomainInfos)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping(value = "/domain/infos")
    public List<DomainInfo> getDomainInfos() throws ThingsboardException {
        return domainService.findDomainInfosByTenantId(getTenantId());
    }

    @ApiOperation(value = "Get Domain info by Id (getDomainInfoById)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping(value = "/domain/info/{id}")
    public DomainInfo getDomainInfoById(@PathVariable UUID id) throws ThingsboardException {
        DomainId domainId = new DomainId(id);
        return checkEntityId(domainId, domainService::findDomainInfoById, Operation.READ);
    }

    @ApiOperation(value = "Delete Domain by ID (deleteDomain)",
            notes = "Deletes Domain by ID. Referencing non-existing asset Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @DeleteMapping(value = "/domain/{id}")
    public void deleteDomain(@PathVariable UUID id) throws Exception {
        DomainId domainId = new DomainId(id);
        checkDomainId(domainId, Operation.DELETE);
        domainService.deleteDomainById(getTenantId(), domainId);
    }

}
