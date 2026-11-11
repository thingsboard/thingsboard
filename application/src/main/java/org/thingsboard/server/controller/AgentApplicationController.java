// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.template.TbAgentAppTemplateService;
import org.thingsboard.server.service.entitiy.agent.TbAgentApplicationService;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentApplicationController extends BaseController {

    private static final String AGENT_APP_ID = "agentApplicationId";
    private static final String AGENT_APP_ID_PARAM_DESCRIPTION = "A string value representing the agent application id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String AGENT_APP_EVENT_ID = "agentAppEventId";
    private static final String AGENT_APP_EVENT_ID_PARAM_DESCRIPTION = "A string value representing the agent app event id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String AGENT_ID = "agentId";
    private static final String AGENT_ID_PARAM_DESCRIPTION = "A string value representing the agent id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    private final TbAgentApplicationService tbAgentApplicationService;
    private final TbAgentAppTemplateService tbAgentAppTemplateService;
    private final SystemSecurityService systemSecurityService;

    @ApiOperation(value = "Get Agent Application (getAgentApplicationById)",
            notes = "Fetch the Agent Application object based on the provided Agent Application Id."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/app/{agentApplicationId}")
    @ResponseBody
    public AgentApplicationInfo getAgentApplicationById(@Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
                                                        @PathVariable(AGENT_APP_ID) String strAgentAppId) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.READ);
        return agentAppService.findInfoById(getTenantId(), agentApplicationId);
    }

    @ApiOperation(value = "Get Agent Applications by Agent Id (getAgentApplicationsByAgentId)",
            notes = "Returns a page of agent applications that belong to the specified agent. "
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/{agentId}/apps", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<AgentApplicationInfo> getAgentApplicationsByAgentId(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing application name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentAppService.findInfosByAgentId(tenantId, agentId, pageLink));
    }

    @ApiOperation(value = "Get Agent Application by Related Entity (getAgentApplicationByRelatedEntity)",
            notes = "Returns the agent application linked to the specified entity. "
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/apps/{entityType}/{entityId}")
    @ResponseBody
    public AgentApplication getAgentApplicationByRelatedEntity(
            @Parameter(description = "Entity type", required = true)
            @PathVariable String entityType,
            @Parameter(description = "Entity id", required = true)
            @PathVariable String entityId) throws ThingsboardException {
        EntityId relatedEntityId = EntityIdFactory.getByTypeAndId(entityType, entityId);
        checkEntityId(relatedEntityId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(agentAppService.findByRelatedEntity(tenantId, relatedEntityId));
    }

    @ApiOperation(value = "Update Agent Application (saveAgentApplication)",
            notes = "Updates the Agent Application."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PutMapping("/agent/app")
    @ResponseBody
    public AgentApplication updateAgentApplication(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the agent application.")
            @RequestBody AgentApplication agentApplication) throws Exception {
        agentApplication.setTenantId(getTenantId());
        checkEntityId(agentApplication.getId(), Operation.WRITE);
        checkAgentId(agentApplication.getAgentId(), Operation.WRITE);
        if (agentApplication.getApplicationProfileId() != null) {
            checkAgentAppProfileId(agentApplication.getApplicationProfileId(), Operation.READ);
        }
        return tbAgentApplicationService.update(agentApplication, getCurrentUser());
    }

    @ApiOperation(value = "Install Agent Application (installAgentApp)",
            notes = "Creates a new agent application and an INSTALL event in a single operation. " +
                    "Returns both the created application and the INSTALL event so the caller can open "
                    + "a progress view without a second round-trip. The request body must include the application object."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/event")
    @ResponseBody
    public AgentAppInstallResponse installAgentApp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the install event request with the application.")
            @RequestBody AgentAppEventRequest request) throws Exception {
        AgentApplication application = request.getApplication();
        if (application == null) {
            throw new ThingsboardException("Install request must include an application", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        checkAgentId(application.getAgentId(), Operation.WRITE);
        if (application.getApplicationProfileId() != null) {
            checkAgentAppProfileInfoId(application.getApplicationProfileId());
        }
        if (request.getRelatedEntityId() != null) {
            checkRelatedEntityForCredentialMerge(request.getRelatedEntityId());
        }
        TenantId tenantId = getCurrentUser().getTenantId();
        return tbAgentApplicationService.install(tenantId, request, getCurrentUser());
    }

    @ApiOperation(value = "Execute Agent Application Event (createAgentAppEvent)",
            notes = "Creates an event for the specified agent application. The action type determines the operation " +
                    "(UPDATE, DELETE, RESTART, UPGRADE, ROLLBACK). Step inputs can be provided for actions that require them. "
                    + "Returns the created event so the caller can open a progress view."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/{agentApplicationId}/event")
    @ResponseBody
    public AgentAppEvent createAgentAppEvent(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the event request.")
            @RequestBody AgentAppEventRequest request) throws Exception {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentAppEventActionType actionType = request.getActionType();
        if (actionType == null) {
            throw new ThingsboardException("Action type must not be null", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (actionType == AgentAppEventActionType.INSTALL) {
            throw new ThingsboardException("Use the install endpoint for INSTALL events", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.WRITE);
        TenantId tenantId = getCurrentUser().getTenantId();
        return tbAgentApplicationService.execActionEvent(tenantId, agentApplicationId, request);
    }

    @ApiOperation(value = "Cancel Agent Application Event (cancelAgentAppEvent)",
            notes = "Force-cancels an in-flight or pending agent application event, marking it as ERROR. "
                    + "Cannot cancel events that are already in a terminal state (FINISHED or ERROR)."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/{agentApplicationId}/event/{agentAppEventId}/cancel")
    @ResponseStatus(value = HttpStatus.OK)
    public void cancelAgentAppEvent(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @Parameter(description = AGENT_APP_EVENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_EVENT_ID) String strAgentAppEventId) throws Exception {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        checkParameter(AGENT_APP_EVENT_ID, strAgentAppEventId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        AgentAppEventId agentAppEventId = new AgentAppEventId(toUUID(strAgentAppEventId));
        AgentAppEvent event = checkAgentAppEventId(agentAppEventId, Operation.WRITE);
        if (!agentApplicationId.equals(event.getApplicationId())) {
            throw new ThingsboardException("Event does not belong to the specified agent application",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        TenantId tenantId = getCurrentUser().getTenantId();
        tbAgentApplicationService.cancelEvent(tenantId, agentAppEventId);
    }

    @ApiOperation(value = "Get Agent Application Event (getAgentAppEventById)",
            notes = "Fetches a single agent application event by id. Works for orphan events "
                    + "whose application has been deleted (application_id is nullable)."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/agent/app/event/{agentAppEventId}")
    @ResponseBody
    public AgentAppEvent getAgentAppEventById(
            @Parameter(description = AGENT_APP_EVENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_EVENT_ID) String strAgentAppEventId) throws ThingsboardException {
        checkParameter(AGENT_APP_EVENT_ID, strAgentAppEventId);
        AgentAppEventId agentAppEventId = new AgentAppEventId(toUUID(strAgentAppEventId));
        return checkAgentAppEventId(agentAppEventId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent Application Units (getAgentAppUnits)",
            notes = "Returns a page of units (containers, volumes, networks) for the specified agent application. "
                    + "Live per-unit data such as `image` and `state` lives in server-scope attributes on each unit "
                    + "and should be fetched separately via the standard telemetry/attributes API."
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/app/{agentApplicationId}/units", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<AgentAppUnit> getAgentAppUnits(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Case-insensitive substring match against the unit identifier")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = "Optional unit type filter (CONTAINER, VOLUME, NETWORK)")
            @RequestParam(required = false) AgentAppUnitType type,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "identifier", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        AgentAppUnitFilter filter = AgentAppUnitFilter.builder()
                .tenantId(tenantId)
                .applicationId(agentApplicationId)
                .type(type)
                .build();
        return agentAppUnitService.findByFilter(filter, pageLink);
    }

    @ApiOperation(value = "Get Agent Application Events (getAgentAppEvents)",
            notes = "Returns a page of events for the specified agent application. "
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/app/{agentApplicationId}/events", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<AgentAppEvent> getAgentAppEvents(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional case-insensitive substring matched against the event action type and processing status")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = "Optional action type filter")
            @RequestParam(required = false) AgentAppEventActionType actionType,
            @Parameter(description = "Optional processing status filter")
            @RequestParam(required = false) AgentProcessingStatus processingStatus,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "updatedTime"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        AgentAppEventFilter filter = AgentAppEventFilter.builder()
                .tenantId(tenantId)
                .applicationId(agentApplicationId)
                .actionType(actionType)
                .processingStatus(processingStatus)
                .build();
        return agentAppEventService.findByFilter(filter, pageLink);
    }

    @ApiOperation(value = "Merge template into application for preview (mergeAgentApplicationForPreview)",
            notes = "Merges the specified template into an agent application for preview purposes. " +
                    "If no application is provided in the body, a new one is created from the template. " +
                    "The compose type determines which compose configuration variant from the template is used. " +
                    "Optionally provide relatedEntityType and relatedEntityId to auto-fill entity credentials (Edge routing key/secret or Gateway access token) into the compose."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/merge/{templateVersion}/preview")
    @ResponseBody
    public AgentApplication mergeAgentApplicationForPreview(
            @Parameter(description = "The template version to merge (e.g. '4.3.1.2EDGEPE')")
            @PathVariable("templateVersion") String templateVersion,
            @Parameter(description = "Application type (EDGE, GATEWAY, GENERIC). Required when no application body is supplied.")
            @RequestParam(required = false) AgentApplicationType appType,
            @Parameter(description = "The compose type to select from the template (e.g. 'monolith', 'microservices')")
            @RequestParam(required = false) String composeType,
            @Parameter(description = "The event action this merge previews (e.g. UPGRADE). Drives action-specific merge rules.")
            @RequestParam(required = false) AgentAppEventActionType actionType,
            @Parameter(description = "Whether to auto-fill host values (e.g. CLOUD_RPC_HOST) from the platform base URL. " +
                    "Should be true only for the install preview; keep false when previewing the saved compose of an existing application to preserve user-configured host values.")
            @RequestParam(required = false, defaultValue = "false") boolean setHostValues,
            @Parameter(description = "Related entity type (EDGE or DEVICE)")
            @RequestParam(required = false) String relatedEntityType,
            @Parameter(description = "Related entity id")
            @RequestParam(required = false) String relatedEntityId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional agent application to merge with. If null, a new application is created from the template.")
            @RequestBody(required = false) AgentApplication application,
            HttpServletRequest request) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        AgentApplicationType resolvedAppType = appType != null ? appType
                : (application != null ? application.getAppType() : null);
        AgentAppTemplate template = null;
        if (templateVersion != null && !templateVersion.isEmpty() && resolvedAppType != null) {
            template = checkNotNull(tbAgentAppTemplateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                    resolvedAppType, AgentAppConfigType.DOCKER_COMPOSE, templateVersion));
        }

        if (application == null) {
            if (template == null) {
                throw new ThingsboardException("Either an application body or an appType (to resolve the template) must be provided",
                        ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            application = AgentApplication.fromTemplate(template);
        }
        application.setTenantId(tenantId);
        EntityId resolvedRelatedEntityId = null;
        if (relatedEntityType != null && relatedEntityId != null) {
            resolvedRelatedEntityId = EntityIdFactory.getByTypeAndId(relatedEntityType, relatedEntityId);
            checkRelatedEntityForCredentialMerge(resolvedRelatedEntityId);
        }
        String baseUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder()
                .template(template)
                .selectedComposeType(composeType)
                .relatedEntityId(resolvedRelatedEntityId)
                .setHostValues(setHostValues)
                .actionType(actionType)
                .baseUrl(baseUrl)
                .build();
        return tbAgentApplicationService.mergeForPreview(tenantId, application, ctx);
    }

    @ApiOperation(value = "Get Managed Related Entity Ids (getManagedRelatedEntityIds)",
            notes = "Returns the ids of entities of the given type that are already linked to an agent application."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/agent/app/managedRelatedEntities/{entityType}")
    @ResponseBody
    public List<EntityId> getManagedRelatedEntityIds(
            @Parameter(description = "Related entity type (EDGE or DEVICE)", required = true)
            @PathVariable String entityType) throws ThingsboardException {
        EntityType resolvedType = resolveRelatedEntityType(entityType);
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return agentAppService.findManagedRelatedEntityIds(tenantId, resolvedType);
    }

    @ApiOperation(value = "Get Related Entity Candidates (getRelatedEntityCandidates)",
            notes = "Returns a page of Edges or Gateway Devices (devices with 'gateway' set in additional info) that can be linked to an agent application. "
                    + "Entities already linked to another application are excluded unless passed as 'currentEntityId'. Results are sorted by name. "
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/app/relatedEntityCandidates", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<EntityInfo> getRelatedEntityCandidates(
            @Parameter(description = "Related entity type (EDGE or DEVICE)", required = true, schema = @Schema(allowableValues = {"EDGE", "DEVICE"}))
            @RequestParam String entityType,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing entity name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = "Optional id of the entity currently linked to the edited application; it is kept in the result even though it is managed")
            @RequestParam(required = false) String currentEntityId) throws ThingsboardException {
        EntityType resolvedType = resolveRelatedEntityType(entityType);
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.resourceFromEntityType(resolvedType), Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        EntityId resolvedCurrentEntityId = StringUtils.isNotEmpty(currentEntityId)
                ? EntityIdFactory.getByTypeAndUuid(resolvedType, toUUID(currentEntityId)) : null;
        PageLink pageLink = createPageLink(pageSize, page, textSearch, "name", "ASC");
        return checkNotNull(agentAppService.findRelatedEntityCandidates(tenantId, resolvedType, resolvedCurrentEntityId, pageLink));
    }

    private EntityType resolveRelatedEntityType(String entityType) throws ThingsboardException {
        EntityType resolvedType;
        try {
            resolvedType = EntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException("Unsupported entity type: " + entityType, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (resolvedType != EntityType.EDGE && resolvedType != EntityType.DEVICE) {
            throw new ThingsboardException("Unsupported related entity type: " + entityType, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return resolvedType;
    }

    @ApiOperation(value = "Assign Related Entity to Agent Application (assignRelatedEntityToAgentApp)",
            notes = "Links an Edge or Gateway Device to the specified agent application and re-merges entity credentials into the compose."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/{agentApplicationId}/relatedEntity/{entityType}/{entityId}")
    @ResponseBody
    public AgentApplication assignRelatedEntityToAgentApp(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @Parameter(description = "Related entity type (EDGE or DEVICE)", required = true)
            @PathVariable String entityType,
            @Parameter(description = "Related entity id", required = true)
            @PathVariable String entityId) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.WRITE);
        EntityId relatedEntityId = EntityIdFactory.getByTypeAndId(entityType, entityId);
        checkRelatedEntityForCredentialMerge(relatedEntityId);
        TenantId tenantId = getCurrentUser().getTenantId();
        return tbAgentApplicationService.assignRelatedEntity(tenantId, agentApplicationId, relatedEntityId, getCurrentUser());
    }

    /**
     * Authorizes a related entity whose credentials the caller will receive merged into the returned compose.
     * A DEVICE additionally requires {@link Operation#READ_CREDENTIALS}, the same permission
     * {@code DeviceController.getDeviceCredentialsByDeviceId} requires for that data, so these endpoints can
     * not be used to read a gateway access token that plain READ would not grant.
     */
    private void checkRelatedEntityForCredentialMerge(EntityId relatedEntityId) throws ThingsboardException {
        checkEntityId(relatedEntityId, Operation.READ);
        if (EntityType.DEVICE.equals(relatedEntityId.getEntityType())) {
            checkEntityId(relatedEntityId, Operation.READ_CREDENTIALS);
        }
    }

    @ApiOperation(value = "Unassign Related Entity from Agent Application (unassignRelatedEntityFromAgentApp)",
            notes = "Removes the link between an agent application and its related Edge or Gateway Device."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/agent/app/{agentApplicationId}/relatedEntity")
    @ResponseBody
    public AgentApplication unassignRelatedEntityFromAgentApp(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.WRITE);
        TenantId tenantId = getCurrentUser().getTenantId();
        return tbAgentApplicationService.unassignRelatedEntity(tenantId, agentApplicationId, getCurrentUser());
    }

    @ApiOperation(value = "Detach Application from Profile (detachFromProfile)",
            notes = "Detaches the application from its profile. The application keeps the effective config it "
                    + "already carries (profile config with its own credentials merged in) and becomes standalone and editable."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/{agentApplicationId}/detach")
    @ResponseBody
    public AgentApplication detachFromProfile(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId) throws Exception {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId appId = new AgentApplicationId(toUUID(strAgentAppId));
        AgentApplication app = checkAgentAppId(appId, Operation.WRITE);

        if (app.getApplicationProfileId() == null) {
            throw new ThingsboardException("Application is not attached to any profile", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        app.setApplicationProfileId(null);
        return tbAgentApplicationService.update(app, getCurrentUser());
    }

    @ApiOperation(value = "Attach Application to Profile (attachToProfile)",
            notes = "Re-attaches the application to a profile. The application adopts the profile's template version " +
                    "without running any upgrade or downgrade steps. " +
                    "After attachment the profile becomes the source of the config; the app's own credentials are carried over."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/{agentApplicationId}/attach/{profileId}")
    @ResponseBody
    public AgentApplication attachToProfile(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @PathVariable("profileId") String strProfileId) throws Exception {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        checkParameter("profileId", strProfileId);

        AgentApplicationId appId = new AgentApplicationId(toUUID(strAgentAppId));
        AgentApplication app = checkAgentAppId(appId, Operation.WRITE);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        AgentAppProfile profile = checkAgentAppProfileId(profileId, Operation.READ);

        if (app.getTemplateVersion() == null || profile.getTemplateVersion() == null) {
            throw new ThingsboardException("Cannot attach: application or profile has no templateId",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        app.setApplicationProfileId(profileId);
        return tbAgentApplicationService.update(app, getCurrentUser());
    }
}
