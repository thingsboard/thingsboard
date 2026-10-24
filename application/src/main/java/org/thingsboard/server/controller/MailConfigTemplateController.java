// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.mail.TbMailConfigTemplateService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.IOException;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/mail/config/template")
@Slf4j
public class MailConfigTemplateController extends BaseController {
    private static final String MAIL_CONFIG_TEMPLATE_DEFINITION = "Mail configuration template is set of default smtp settings for mail server that specific provider supports";
    private final TbMailConfigTemplateService mailConfigTemplateService;

    @ApiOperation(value = "Get the list of all OAuth2 client registration templates (getMailConfigTemplates)" + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            notes = MAIL_CONFIG_TEMPLATE_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public JsonNode getMailConfigTemplates() throws ThingsboardException, IOException {
        SecurityUser user = getCurrentUser();
        if (user.isSystemAdmin()) {
            accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        } else {
            accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.READ);
        }
        return mailConfigTemplateService.findAllMailConfigTemplates();
    }

}
