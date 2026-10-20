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
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInstructions;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.agent.AgentUpgradeRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.install.AgentInstallInstructionsService;
import org.thingsboard.server.service.agent.upgrade.AgentUpgradeVersionService;
import org.thingsboard.server.service.entitiy.agent.TbAgentApplicationService;
import org.thingsboard.server.service.entitiy.agent.TbAgentService;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentController extends BaseController {

    private static final String AGENT_ID = "agentId";
    private static final String AGENT_ID_PARAM_DESCRIPTION = "A string value representing the agent id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    private final TbAgentService tbAgentService;
    private final TbAgentApplicationService tbAgentApplicationService;
    private final AgentInstallInstructionsService agentInstallInstructionsService;
    private final AgentUpgradeVersionService agentUpgradeVersionService;

    @ApiOperation(value = "Get Agent (getAgentById)",
            notes = "Fetch the Agent object based on the provided Agent Id. " +
                    "The server checks that the agent is owned by the same tenant."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/{agentId}", method = RequestMethod.GET)
    @ResponseBody
    public Agent getAgentById(@Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                              @PathVariable(AGENT_ID) String strAgentId) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        return checkAgentId(agentId, Operation.READ);
    }

    @ApiOperation(value = "Upgrade Agent (upgradeAgent)",
            notes = "Creates an agent-scoped upgrade event that makes the agent replace its own container "
                    + "with the given image. At most one agent upgrade can be active per agent, and it blocks "
                    + "all application events for that agent until it completes."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/{agentId}/upgrade")
    @ResponseBody
    public AgentAppEvent upgradeAgent(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId,
            @RequestBody AgentUpgradeRequest request) throws Exception {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.WRITE);
        return tbAgentApplicationService.upgradeAgent(getCurrentUser().getTenantId(), agentId, request.getImageRef());
    }

    @ApiOperation(value = "Is agent upgrade available (isAgentUpgradeAvailable)",
            notes = "Returns 'true' when the agent reports an image older than the newest published one, "
                    + "'false' otherwise \u2014 including when the agent runs a floating tag such as 'latest', "
                    + "whose current content cannot be told from the tag."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/agent/{agentId}/upgrade/available")
    @ResponseBody
    public boolean isAgentUpgradeAvailable(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId) throws Exception {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        Agent agent = checkAgentId(agentId, Operation.READ);
        return agentUpgradeVersionService.isUpgradeAvailable(agent.getTenantId(), agent.getId());
    }

    @ApiOperation(value = "Get the newest published agent image (getLatestAgentImageRef)",
            notes = "Returns the newest agent image reference known from the version graph, or the floating "
                    + "tag while no version has been published. Lets a list of agents decide locally which "
                    + "rows are upgradable without asking per row."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/agent/upgrade/latest")
    @ResponseBody
    public String getLatestAgentImageRef() {
        return agentUpgradeVersionService.getLatestImageRef();
    }

    @ApiOperation(value = "Get the agent upgrade target (getAgentUpgradeTarget)",
            notes = "Returns the image reference the agent should be upgraded to, or an empty string when it "
                    + "already runs the newest published image or cannot be placed in the version graph."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/agent/{agentId}/upgrade/target")
    @ResponseBody
    public String getAgentUpgradeTarget(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId) throws Exception {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        Agent agent = checkAgentId(agentId, Operation.READ);
        return agentUpgradeVersionService.getUpgradeImageRef(agent.getTenantId(), agent.getId()).orElse("");
    }

    @ApiOperation(value = "Get Agent App Events by Agent Id (getAgentAppEventsByAgentId)",
            notes = "Returns a page of agent application events for all applications belonging to the specified agent. "
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/{agentId}/events", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentAppEvent> getAgentAppEventsByAgentId(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value reserved for future event filtering")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "updatedTime"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return agentAppEventService.findByAgentId(tenantId, agentId, pageLink);
    }

    @ApiOperation(value = "Get Agent App Event Infos by Agent Id (getAgentAppEventInfosByAgentId)",
            notes = "Returns a page of agent application events for all applications belonging to the specified agent, " +
                    "enriched with the application name for each event. Supports optional filtering by action type and status, " +
                    "and text search over the application name. "
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/{agentId}/eventInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentAppEventInfo> getAgentAppEventInfosByAgentId(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional text value to match against the application name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "updatedTime", "actionType", "startStatus", "processingStatus"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Optional filter by event action type")
            @RequestParam(required = false) AgentAppEventActionType actionType,
            @Parameter(description = "Optional filter by event status")
            @RequestParam(required = false) AgentProcessingStatus processingStatus) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return agentAppEventService.findInfosByAgentId(tenantId, agentId, actionType, processingStatus, pageLink);
    }

    @ApiOperation(value = "Get Agent Info (getAgentInfoById)",
            notes = "Fetch the Agent Info object based on the provided Agent Id. " +
                    "Agent Info extends the Agent object and adds customer title and 'is public' flag."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/info/{agentId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentInfo getAgentInfoById(@Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                                      @PathVariable(AGENT_ID) String strAgentId) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.READ);
        return withUpgradeTarget(checkNotNull(agentService.findAgentInfoById(getTenantId(), agentId)));
    }

    @ApiOperation(value = "Get Agent Infos By Ids (getAgentInfosByIds)",
            notes = "Requested agents must be owned by tenant or assigned to customer which user is performing the request. "
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/agentInfos", params = {"agentIds"})
    @ResponseBody
    public List<AgentInfo> getAgentInfosByIds(
            @Parameter(description = "A list of agent ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("agentIds") String[] strAgentIds) throws ThingsboardException {
        checkArrayParameter("agentIds", strAgentIds);
        TenantId tenantId = getCurrentUser().getTenantId();
        List<AgentId> agentIds = new ArrayList<>();
        for (String strAgentId : strAgentIds) {
            agentIds.add(new AgentId(toUUID(strAgentId)));
        }
        List<AgentInfo> agents = checkNotNull(agentService.findAgentInfosByTenantIdAndIds(tenantId, agentIds));
        return filterAgentsByReadPermission(agents).stream().map(this::withUpgradeTarget).toList();
    }

    private List<AgentInfo> filterAgentsByReadPermission(List<AgentInfo> agents) {
        return agents.stream().filter(agent -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.AGENT, Operation.READ, agent.getId(), agent);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

    @ApiOperation(value = "Get Agent Install Instructions (getAgentInstallInstructions)",
            notes = "Returns the docker install command for the specified agent with the server address and gRPC port resolved server-side." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/instructions/install/{agentId}/{method}", method = RequestMethod.GET)
    @ResponseBody
    public AgentInstructions getAgentInstallInstructions(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId,
            @Parameter(description = "Installation method ('docker')", schema = @Schema(allowableValues = {"docker"}))
            @PathVariable("method") String method,
            HttpServletRequest request) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        Agent agent = checkAgentId(agentId, Operation.READ);
        return checkNotNull(agentInstallInstructionsService.getInstallInstructions(agent, method, request));
    }

    @ApiOperation(value = "Create Or Update Agent (saveAgent)",
            notes = "Creates or Updates the Agent. When creating agent, platform generates Agent Id as " + UUID_WIKI_LINK +
                    "The newly created Agent id will be present in the response. " +
                    "Specify existing Agent id to update the agent. " +
                    "Referencing non-existing Agent Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Agent entity. "
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent")
    @ResponseBody
    public Agent saveAgent(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the agent.") @RequestBody Agent agent,
                           @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
                           @Parameter(description = "A list of entity group ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
                           @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds) throws Exception {
        return saveGroupEntity(agent, strEntityGroupId, strEntityGroupIds,
                (toSave, entityGroups) -> {
                    try {
                        return tbAgentService.save(toSave, entityGroups, getCurrentUser());
                    } catch (Exception e) {
                        throw handleException(e);
                    }
                });
    }

    @ApiOperation(value = "Delete agent (deleteAgent)",
            notes = "Deletes the agent and all the relations (from and to the agent). Referencing non-existing agent Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/{agentId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAgent(@Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                            @PathVariable(AGENT_ID) String strAgentId) throws Exception {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        Agent agent = checkAgentId(agentId, Operation.DELETE);
        tbAgentService.delete(agent, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Agents (getTenantAgents)",
            notes = "Returns a page of agents owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agents", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Agent> getTenantAgents(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing agent name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentService.findAgentsByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Agent Infos (getTenantAgentInfos)",
            notes = "Returns a page of agent info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agentInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentInfo> getTenantAgentInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing agent name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return withUpgradeTargets(checkNotNull(agentService.findAgentInfosByTenantId(tenantId, pageLink)));
    }

    @ApiOperation(value = "Get Customer Agents (getCustomerAgents)",
            notes = "Returns a page of agent objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/agents", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Agent> getCustomerAgents(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing agent name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentService.findAgentsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
    }

    @ApiOperation(value = "Get Customer Agent Infos (getCustomerAgentInfos)",
            notes = "Returns a page of agent info objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/agentInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentInfo> getCustomerAgentInfos(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing agent name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return withUpgradeTargets(checkNotNull(agentService.findAgentInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink)));
    }

    private AgentInfo withUpgradeTarget(AgentInfo agentInfo) {
        agentUpgradeVersionService.getUpgradeImageRefFor(agentInfo.getAgentVersion())
                .ifPresent(agentInfo::setUpgradeTargetImageRef);
        return agentInfo;
    }

    private PageData<AgentInfo> withUpgradeTargets(PageData<AgentInfo> agentInfos) {
        agentInfos.getData().forEach(this::withUpgradeTarget);
        return agentInfos;
    }
}
