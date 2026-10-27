// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.service.agent.template.TbAgentAppTemplateService;
import org.thingsboard.server.service.entitiy.agent.TbAgentAppProfileService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentAppProfileController extends BaseController {

    private static final String PROFILE_ID = "profileId";

    private final TbAgentAppProfileService tbProfileService;
    private final TbAgentAppTemplateService tbAgentAppTemplateService;
    private final SystemSecurityService systemSecurityService;

    @ApiOperation(value = "Get Agent Application Profile (getAgentAppProfileById)",
            notes = "Fetch the Agent Application Profile by Id." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profile/{profileId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppProfile getAgentAppProfileById(
            @Parameter(description = "Profile Id") @PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(PROFILE_ID, strProfileId);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        return checkAgentAppProfileId(profileId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent Application Profile Info (getAgentAppProfileInfoById)",
            notes = "Fetch the Agent Application Profile Info by Id. Readable by any tenant admin without "
                    + "AGENT_APP_PROFILE permission, so agent/application screens can resolve the linked profile. "
                    + "Returns the full profile, config and arguments included - an application profile carries "
                    + "deployment configuration rather than credentials, so it is not gated like other profiles."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profile/info/{profileId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppProfileInfo getAgentAppProfileInfoById(
            @Parameter(description = "Profile Id") @PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(PROFILE_ID, strProfileId);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        return checkAgentAppProfileInfoId(profileId);
    }

    @ApiOperation(value = "Create or Update Agent Application Profile (saveAgentAppProfile)",
            notes = "Creates or updates an agent application profile." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profile", method = RequestMethod.POST)
    @ResponseBody
    public AgentAppProfile saveAgentAppProfile(
            @RequestBody AgentAppProfile profile) throws Exception {
        profile.setTenantId(getTenantId());
        checkEntity(profile.getId(), profile, Resource.AGENT_APP_PROFILE);
        return tbProfileService.save(profile, getCurrentUser());
    }

    @ApiOperation(value = "Delete Agent Application Profile (deleteAgentAppProfile)",
            notes = "Deletes the agent application profile. Cannot delete if referenced by applications." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profile/{profileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAgentAppProfile(
            @Parameter(description = "Profile Id") @PathVariable(PROFILE_ID) String strProfileId) throws Exception {
        checkParameter(PROFILE_ID, strProfileId);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        AgentAppProfile profile = checkAgentAppProfileId(profileId, Operation.DELETE);
        tbProfileService.delete(profile, getCurrentUser());
    }

    @ApiOperation(value = "Merge template into application profile for preview (mergeAgentAppProfileForPreview)",
            notes = "Merges the specified template into an agent application profile for preview purposes. " +
                    "The compose type determines which compose configuration variant from the template is used. " +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/profiles/merge/{templateVersion}/preview")
    @ResponseBody
    public AgentAppProfile mergeAgentAppProfileForPreview(
            @Parameter(description = "The template version to merge (e.g. '4.3.1.2EDGEPE')")
            @PathVariable("templateVersion") String templateVersion,
            @Parameter(description = "The compose type to select from the template (e.g. 'monolith', 'microservices')")
            @RequestParam(required = false) String composeType,
            @Parameter(description = "The event action this merge previews (e.g. UPGRADE). Drives action-specific merge rules.")
            @RequestParam(required = false) AgentAppEventActionType actionType,
            @Parameter(description = "Whether to auto-fill host values (e.g. CLOUD_RPC_HOST) from the platform base URL. " +
                    "Should be true only for the install preview; keep false when previewing the saved compose of an existing profile to preserve user-configured host values.")
            @RequestParam(required = false, defaultValue = "false") boolean setHostValues,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Agent application profile to merge template with")
            @RequestBody AgentAppProfile appProfile,
            HttpServletRequest request) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        AgentAppTemplate template = resolveTemplate(appProfile.getAppType(), templateVersion);

        appProfile.setTenantId(tenantId);
        String baseUrl = resolveBaseUrl(request);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder()
                .template(template)
                .selectedComposeType(composeType)
                .setHostValues(setHostValues)
                .actionType(actionType)
                .baseUrl(baseUrl)
                .build();
        return tbProfileService.mergeForPreview(tenantId, appProfile, ctx);
    }

    @ApiOperation(value = "Create Agent Application Profile from template (materializeAgentAppProfile)",
            notes = "Creates an agent application profile from the default configuration of the template with the given " +
                    "app type and version. Used to materialize a predefined (virtual) profile shown in the UI into a real " +
                    "profile on first use. If a profile with the given app type and template version already exists, " +
                    "the existing profile is returned (the oldest one by created time) instead of creating a new one." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/profile/materialize/{appType}/{templateVersion}")
    @ResponseBody
    public AgentAppProfile materializeAgentAppProfile(
            @PathVariable @Parameter(description = "Application type, e.g. 'EDGE', 'GATEWAY', 'GENERIC'") AgentApplicationType appType,
            @PathVariable("templateVersion") @Parameter(description = "The template version to materialize (e.g. '4.3.1.2EDGEPE')") String templateVersion,
            @Parameter(description = "The compose type to select from the template (e.g. 'in_memory', 'kafka'). Defaults to the template's first compose type.")
            @RequestParam(required = false) String composeType,
            HttpServletRequest request) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_APP_PROFILE, Operation.CREATE);
        AgentAppTemplate template = resolveTemplate(appType, templateVersion);
        String baseUrl = resolveBaseUrl(request);
        return tbProfileService.createFromTemplate(getTenantId(), template, composeType, baseUrl, getCurrentUser());
    }

    private AgentAppTemplate resolveTemplate(AgentApplicationType appType, String templateVersion) throws ThingsboardException {
        return checkNotNull(tbAgentAppTemplateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                appType, AgentAppConfigType.DOCKER_COMPOSE, templateVersion));
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        return systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
    }

    @ApiOperation(value = "Get Agent Application Profiles by app type (getAgentAppProfilesByAppType)",
            notes = "Returns a list of agent application profiles filtered by application type."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profiles/{appType}", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppProfileInfo> getAgentAppProfilesByAppType(
            @PathVariable @Parameter(description = "Application type, e.g. 'EDGE', 'GATEWAY', 'GENERIC'") AgentApplicationType appType) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_APP_PROFILE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(agentAppProfileService.findProfileInfosByTenantIdAndAppType(tenantId, appType));
    }

    @ApiOperation(value = "Get Agent Application Profiles By Ids (getAgentAppProfilesByIds)",
            notes = "Requested agent application profiles must be owned by tenant which is performing the request. "
                    + NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/app/profiles", params = {"agentAppProfileIds"})
    @ResponseBody
    public List<AgentAppProfile> getAgentAppProfilesByIds(
            @Parameter(description = "A list of agent application profile ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("agentAppProfileIds") String[] strProfileIds) throws ThingsboardException {
        checkArrayParameter("agentAppProfileIds", strProfileIds);
        if (!accessControlService.hasPermission(getCurrentUser(), Resource.AGENT_APP_PROFILE, Operation.READ)) {
            return Collections.emptyList();
        }
        List<AgentAppProfileId> profileIds = new ArrayList<>();
        for (String strProfileId : strProfileIds) {
            profileIds.add(new AgentAppProfileId(toUUID(strProfileId)));
        }
        return checkNotNull(agentAppProfileService.findProfilesByTenantIdAndIds(getTenantId(), profileIds));
    }

    @ApiOperation(value = "Get Tenant Agent Application Profiles (getTenantAgentAppProfiles)",
            notes = "Returns a page of agent application profiles owned by tenant." + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agent/app/profiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentAppProfile> getTenantAgentAppProfiles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Optional search text") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_APP_PROFILE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentAppProfileService.findProfilesByTenantId(tenantId, pageLink));
    }
}
