/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.MailConfigTemplateId;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.mail.MailConfigTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.dao.settings.MailConfigTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api/mail/config/template")
@Slf4j
public class MailConfigTemplateController extends BaseController {
    private static final String MAIL_CONFIG_TEMPLATE_ID = "mailConfigTemplateId";
    private static final String MAIL_CONFIG_TEMPLATE_DEFINITION = "Mail configuration template is set of default smtp settings for mail server that specific provider supports";

    @ApiOperation(value = "Create or update mail configuration template (saveMailConfigTemplate)" + SYSTEM_AUTHORITY_PARAGRAPH,
            notes = MAIL_CONFIG_TEMPLATE_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public MailConfigTemplate saveMailConfigTemplate(@RequestBody MailConfigTemplate mailConfigTemplate) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.MAIL_CONFIGURATION_TEMPLATE, Operation.WRITE);
        return mailConfigTemplateService.saveMailConfigTemplate(mailConfigTemplate);
    }

    @ApiOperation(value = "Delete mail configuration template by id (deleteMailConfigTemplate)" + SYSTEM_AUTHORITY_PARAGRAPH,
            notes = MAIL_CONFIG_TEMPLATE_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/{mailConfigTemplateId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteMailConfigTemplate(@ApiParam(value = "String representation of client registration template id to delete", example = "139b1f81-2f5d-11ec-9dbe-9b627e1a88f4")
                                                 @PathVariable(MAIL_CONFIG_TEMPLATE_ID) String strMailConfTemplateId) throws ThingsboardException {
        checkParameter(MAIL_CONFIG_TEMPLATE_ID, strMailConfTemplateId);
        accessControlService.checkPermission(getCurrentUser(), Resource.MAIL_CONFIGURATION_TEMPLATE, Operation.DELETE);
        MailConfigTemplateId mailConfigTemplateId = new MailConfigTemplateId(toUUID(strMailConfTemplateId));
        mailConfigTemplateService.deleteMailConfigTemplateById(mailConfigTemplateId);
    }

    @ApiOperation(value = "Get the list of all OAuth2 client registration templates (getClientRegistrationTemplates)" + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            notes = MAIL_CONFIG_TEMPLATE_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<MailConfigTemplate> getClientRegistrationTemplates() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.MAIL_CONFIGURATION_TEMPLATE, Operation.READ);
        return mailConfigTemplateService.findAllMailConfigTemplates();
    }

}
