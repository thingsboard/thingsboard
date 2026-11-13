// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.template.TbAgentAppTemplateService;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentAppTemplateController extends BaseController {

    private static final String APP_TYPE_PARAM_DESCRIPTION = "A string value representing the agent application type, e.g. 'EDGE', 'GATEWAY'";
    private static final String CONFIG_TYPE_PARAM_DESCRIPTION = "A string value representing the agent configuration type, e.g. 'DOCKER_COMPOSE'";

    private final TbAgentAppTemplateService tbAgentAppTemplateService;

    @ApiOperation(value = "Get Agent App Template (getLatestAgentAppTemplateByType)",
            notes = "Fetch the latest Agent App Template object for the given app type and config type."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/template/{appType}/{configType}/latest", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppTemplate getLatestAgentAppTemplateByAppTypeAndConfigType(
            @PathVariable @Parameter(description = APP_TYPE_PARAM_DESCRIPTION) AgentApplicationType appType,
            @PathVariable @Parameter(description = CONFIG_TYPE_PARAM_DESCRIPTION) AgentAppConfigType configType) throws ThingsboardException {
        return checkNotNull(tbAgentAppTemplateService.findLatestByAppTypeAndConfigType(appType, configType));
    }

    @ApiOperation(value = "Get Agent App Template by current version (getAgentAppTemplateByCurrentVersion)",
            notes = "Fetch the Agent App Template object based on the provided app type, config type, and current version. "
                    + "Matches the version exactly, including a floating tag used as a version such as the gateway "
                    + "'latest' tag."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/template/{appType}/{configType}/version/{currentVersion}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppTemplate getAgentAppTemplateByCurrentVersion(
            @PathVariable @Parameter(description = APP_TYPE_PARAM_DESCRIPTION) AgentApplicationType appType,
            @PathVariable @Parameter(description = CONFIG_TYPE_PARAM_DESCRIPTION) AgentAppConfigType configType,
            @PathVariable @Parameter(description = "A string value representing the current version of the template, e.g. '1.0.0'") String currentVersion) throws ThingsboardException {
        return checkNotNull(tbAgentAppTemplateService.findByAppTypeAndConfigTypeAndCurrentVersion(appType, configType, currentVersion));
    }

    @ApiOperation(value = "Get Agent App Templates by type (getAgentAppTemplatesByAppType)",
            notes = "Returns a list of agent app templates filtered by application type and config type."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/templates/{appType}/{configType}", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppTemplate> getAgentAppTemplatesByAppType(
            @PathVariable @Parameter(description = APP_TYPE_PARAM_DESCRIPTION) AgentApplicationType appType,
            @PathVariable @Parameter(description = CONFIG_TYPE_PARAM_DESCRIPTION) AgentAppConfigType configType) throws ThingsboardException {
        return checkNotNull(tbAgentAppTemplateService.findByAppTypeAndConfigType(appType, configType));
    }

    @ApiOperation(value = "Get all Agent App Templates (getAgentAppTemplates)",
            notes = "Returns a list of all agent app templates available for the current tenant."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/templates", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppTemplate> getAgentAppTemplates() throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(tbAgentAppTemplateService.findAll(tenantId));
    }

}
