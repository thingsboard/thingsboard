/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api/oauth2/config/template")
@Slf4j
public class OAuth2ConfigTemplateController extends BaseController {
    private static final String CLIENT_REGISTRATION_TEMPLATE_ID = "clientRegistrationTemplateId";

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public OAuth2ClientRegistrationTemplate saveClientRegistrationTemplate(@RequestBody OAuth2ClientRegistrationTemplate clientRegistrationTemplate) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_TEMPLATE, Operation.WRITE);
            return oAuth2ConfigTemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/{clientRegistrationTemplateId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteClientRegistrationTemplate(@PathVariable(CLIENT_REGISTRATION_TEMPLATE_ID) String strClientRegistrationTemplateId) throws ThingsboardException {
        checkParameter(CLIENT_REGISTRATION_TEMPLATE_ID, strClientRegistrationTemplateId);
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_TEMPLATE, Operation.DELETE);
            OAuth2ClientRegistrationTemplateId clientRegistrationTemplateId = new OAuth2ClientRegistrationTemplateId(toUUID(strClientRegistrationTemplateId));
            oAuth2ConfigTemplateService.deleteClientRegistrationTemplateById(clientRegistrationTemplateId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<OAuth2ClientRegistrationTemplate> getClientRegistrationTemplates() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_TEMPLATE, Operation.READ);
            return oAuth2ConfigTemplateService.findAllClientRegistrationTemplates();
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
