// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.IntegrationConvertersInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.integration.TbIntegrationService;
import org.thingsboard.server.service.integration.IntegrationManagerService;
import org.thingsboard.server.service.integration.template.IntegrationPackageExportService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_CONFIGURATION_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class IntegrationController extends AutoCommitController {

    private final IntegrationManagerService integrationManagerService;
    private final TbIntegrationService tbIntegrationService;
    private final SecretConfigurationService secretConfigurationService;
    private final IntegrationPackageExportService packageExportService;
    private final ObjectMapper objectMapper;

    private static final String INTEGRATION_ID = "integrationId";

    @ApiOperation(value = "Get Integration (getIntegrationById)",
            notes = "Fetch the Integration object based on the provided Integration Id. " +
                    "The server checks that the integration is owned by the same tenant. "
                    + NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/integration/{integrationId}")
    public Integration getIntegrationById(@Parameter(required = true, description = INTEGRATION_ID_PARAM_DESCRIPTION)
                                          @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
        return checkIntegrationId(integrationId, Operation.READ);
    }

    @ApiOperation(value = "Get Integration by Routing Key (getIntegrationByRoutingKey)",
            notes = "Fetch the Integration object based on the provided routing key. " +
                    "The server checks that the integration is owned by the same tenant. "
                    + NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/integration/routingKey/{routingKey}")
    public Integration getIntegrationByRoutingKey(
            @Parameter(required = true, description = "A string value representing the integration routing key. For example, '542047e6-c1b2-112e-a87e-e49247c09d4b'")
            @PathVariable("routingKey") String routingKey) throws Exception {
        Integration integration = checkNotNull(integrationService.findIntegrationByRoutingKey(getTenantId(), routingKey));
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ, integration.getId(), integration);
        return integration;
    }

    @ApiOperation(value = "Create Or Update Integration (saveIntegration)",
            notes = "Create or update the Integration. When creating integration, platform generates Integration Id as " + UUID_WIKI_LINK +
                    "The newly created integration id will be present in the response. " +
                    "Specify existing Integration id to update the integration. " +
                    "Referencing non-existing integration Id will cause 'Not Found' error. " +
                    "Integration configuration is validated for each type of the integration before it can be created. " +
                    INTEGRATION_CONFIGURATION_DEFINITION +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Integration entity. " +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/integration")
    public Integration saveIntegration(@Parameter(required = true, description = "A JSON value representing the integration.")
                                       @RequestBody Integration integration) throws Exception {
        checkFeatureAllowed(PlatformFeature.INTEGRATIONS);
        SecurityUser currentUser = getCurrentUser();
        try {
            TenantId tenantId = getTenantId();
            integration.setTenantId(tenantId);
            boolean created = integration.getId() == null;

            checkEntity(integration.getId(), integration, Resource.INTEGRATION);

            if (!integration.isEdgeTemplate()) {
                try {
                    Integration copy = new Integration(integration);
                    secretConfigurationService.replaceSecretUsages(tenantId, copy.getConfiguration());
                    integrationManagerService.validateIntegrationConfiguration(copy).get(20, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    throwRealCause(e);
                }
            }

            Integration result = checkNotNull(integrationService.saveIntegration(integration));

            autoCommit(currentUser, result.getId());

            logEntityActionService.logEntityAction(getTenantId(), result.getId(), result,
                    created ? ActionType.ADDED : ActionType.UPDATED, currentUser);

            return result;
        } catch (TimeoutException e) {
            throw new ThingsboardRuntimeException("Timeout to validate the configuration!", ThingsboardErrorCode.GENERAL);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION), integration,
                    integration.getId() == null ? ActionType.ADDED : ActionType.UPDATED, currentUser, e);
            throw e;
        }
    }

    @ApiOperation(value = "Get Integrations (getIntegrations)",
            notes = "Returns a page of integrations owned by tenant. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/integrations", params = {"pageSize", "page"})
    public PageData<Integration> getIntegrations(
            @Parameter(description = "Fetch edge template integrations")
            @RequestParam(value = "isEdgeTemplate", required = false, defaultValue = "false") boolean isEdgeTemplate,
            @Parameter(required = true, description = PAGE_SIZE_DESCRIPTION, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(required = true, description = PAGE_NUMBER_DESCRIPTION, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "debugMode", "allowCreateDevicesOrAssets", "enabled", "remote", "routingKey", "secret"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (isEdgeTemplate) {
            return checkNotNull(integrationService.findTenantEdgeTemplateIntegrations(tenantId, pageLink));
        } else {
            return checkNotNull(integrationService.findTenantIntegrations(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Integration Infos (getIntegrationInfos)",
            notes = "Returns a page of integration infos owned by tenant. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/integrationInfos", params = {"pageSize", "page"})
    public PageData<IntegrationInfo> getIntegrationInfos(
            @Parameter(description = "Fetch edge template integrations")
            @RequestParam(value = "isEdgeTemplate", required = false, defaultValue = "false") boolean isEdgeTemplate,
            @Parameter(required = true, description = PAGE_SIZE_DESCRIPTION, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(required = true, description = PAGE_NUMBER_DESCRIPTION, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "debugMode", "allowCreateDevicesOrAssets", "enabled", "remote", "routingKey", "secret"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return tbIntegrationService.findTenantIntegrationInfos(tenantId, pageLink, isEdgeTemplate);
    }

    @ApiOperation(value = "Check integration connectivity (checkIntegrationConnection)",
            notes = "Checks if the connection to the integration is established. " +
                    "Throws an error if the connection is not established. Example: Failed to connect to MQTT broker at host:port.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/integration/check")
    public void checkIntegrationConnection(@Parameter(required = true, description = "A JSON value representing the integration.")
                                           @RequestBody Integration integration) throws Exception {
        // Deliberately unguarded: this only probes connectivity and persists nothing. It is the only way to
        // answer "why did my running integration stop connecting?", and saving one is refused anyway.
        try {
            checkNotNull(integration);
            TenantId tenantId = getCurrentUser().getTenantId();
            integration.setTenantId(tenantId);
            try {
                Integration copy = new Integration(integration);
                secretConfigurationService.replaceSecretUsages(tenantId, copy.getConfiguration());
                integrationManagerService.checkIntegrationConnection(copy).get(integrationManagerService.getIntegrationConnectionCheckApiRequestTimeoutSec(), TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throwRealCause(e);
            }
        } catch (TimeoutException e) {
            throw new ThingsboardRuntimeException("Timeout to process the request!", ThingsboardErrorCode.GENERAL);
        }
    }

    @ApiOperation(value = "Delete integration (deleteIntegration)",
            notes = "Deletes the integration and all the relations (from and to the integration). " +
                    "Referencing non-existing integration Id will cause an error. " +
                    NEW_LINE + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/integration/{integrationId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteIntegration(@Parameter(required = true, description = INTEGRATION_ID_PARAM_DESCRIPTION)
                                  @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        // Deliberately unguarded: removal is the one write direction that reduces use of a withheld feature.
        // Persisted integrations keep ingesting while it is withheld, so refusing this would leave no way out.
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            Integration integration = checkIntegrationId(integrationId, Operation.DELETE);

            integrationService.deleteIntegration(getTenantId(), integrationId);

            logEntityActionService.logEntityAction(getTenantId(), integrationId, integration, ActionType.DELETED,
                    getCurrentUser(), strIntegrationId);

        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION), ActionType.DELETED,
                    getCurrentUser(), e, strIntegrationId);

            throw e;
        }
    }

    @Hidden
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/integrations", params = {"integrationIds"})
    public List<Integration> getIntegrationsByIdsV1( @RequestParam("integrationIds") String[] strIntegrationIds) throws Exception {
        checkArrayParameter("integrationIds", strIntegrationIds);
        if (!accessControlService.hasPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ)) {
            return Collections.emptyList();
        }
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<IntegrationId> integrationIds = new ArrayList<>();
        for (String strIntegrationId : strIntegrationIds) {
            integrationIds.add(new IntegrationId(toUUID(strIntegrationId)));
        }
        List<Integration> integrations = checkNotNull(integrationService.findIntegrationsByIdsAsync(tenantId, integrationIds).get());
        return filterIntegrationsByReadPermission(integrations);
    }

    @ApiOperation(value = "Get Integrations By Ids (getIntegrationsByIds)",
            notes = "Requested integrations must be owned by tenant which is performing the request. " +
                    NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/integrations/list")
    public List<Integration> getIntegrationsByIds(
            @Parameter(description = "A list of integration ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("integrationIds") String[] strIntegrationIds) throws Exception {
        return getIntegrationsByIdsV1(strIntegrationIds);
    }

    private List<Integration> filterIntegrationsByReadPermission(List<Integration> integrations) {
        return integrations.stream().filter(integration -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ, integration.getId(), integration);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    @ApiOperation(value = "Assign integration to edge (assignIntegrationToEdge)",
            notes = "Creates assignment of an existing integration edge template to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment integration " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once integration will be delivered to edge service, it's going to start locally. " +
                    "\n\nOnly integration edge template can be assigned to edge." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/edge/{edgeId}/integration/{integrationId}")
    public Integration assignIntegrationToEdge(@PathVariable("edgeId") String strEdgeId,
                                               @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        checkFeatureAllowed(PlatformFeature.INTEGRATIONS);
        checkParameter("edgeId", strEdgeId);
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.WRITE);

            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            checkIntegrationId(integrationId, Operation.READ);

            Integration savedIntegration = checkNotNull(integrationService.assignIntegrationToEdge(getCurrentUser().getTenantId(), integrationId, edgeId));

            logEntityActionService.logEntityAction(getTenantId(), integrationId, savedIntegration,
                    ActionType.ASSIGNED_TO_EDGE, getCurrentUser(), strIntegrationId, strEdgeId, edge.getName());

            return savedIntegration;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION),
                    ActionType.ASSIGNED_TO_EDGE, getCurrentUser(), e, strIntegrationId, strEdgeId);

            throw e;
        }
    }

    @ApiOperation(value = "Unassign integration from edge (unassignIntegrationFromEdge)",
            notes = "Clears assignment of the integration to the edge. " +
                    EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove integration " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove integration locally." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/edge/{edgeId}/integration/{integrationId}")
    public Integration unassignIntegrationFromEdge(@PathVariable("edgeId") String strEdgeId,
                                                   @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        checkFeatureAllowed(PlatformFeature.INTEGRATIONS);
        checkParameter("edgeId", strEdgeId);
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.WRITE);
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            Integration integration = checkIntegrationId(integrationId, Operation.READ);

            Integration savedIntegration = checkNotNull(integrationService.unassignIntegrationFromEdge(getCurrentUser().getTenantId(), integrationId, edgeId, false));

            logEntityActionService.logEntityAction(getTenantId(), integrationId, integration,
                    ActionType.UNASSIGNED_FROM_EDGE, getCurrentUser(), strIntegrationId, strEdgeId, edge.getName());

            return savedIntegration;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION),
                    ActionType.UNASSIGNED_FROM_EDGE, getCurrentUser(), e, strIntegrationId, strEdgeId);

            throw e;
        }
    }

    @ApiOperation(value = "Get Edge Integrations (getEdgeIntegrations)",
            notes = "Returns a page of Integrations assigned to the specified edge. " + INTEGRATION_DESCRIPTION + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/{edgeId}/integrations", params = {"pageSize", "page"})
    public PageData<Integration> getEdgeIntegrations(
            @Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "debugMode", "allowCreateDevicesOrAssets", "enabled", "remote", "routingKey", "secret"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(integrationService.findIntegrationsByTenantIdAndEdgeId(tenantId, edgeId, pageLink));
    }

    @ApiOperation(value = "Get Edge Integrations (getEdgeIntegrationInfos)",
            notes = "Returns a page of Integrations assigned to the specified edge. " + INTEGRATION_DESCRIPTION + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/{edgeId}/integrationInfos", params = {"pageSize", "page"})
    public PageData<IntegrationInfo> getEdgeIntegrationInfos(
            @Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "debugMode", "allowCreateDevicesOrAssets", "enabled", "remote", "routingKey", "secret"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tbIntegrationService.findIntegrationInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink));
    }

    @ApiOperation(value = "Find edge missing attributes for assigned integrations (findEdgeMissingAttributes)",
            notes = "Returns list of edge attribute names that are missing in assigned integrations." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/integration/{edgeId}/missingAttributes", params = {"integrationIds"})
    public String findEdgeMissingAttributes(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                            @PathVariable(EDGE_ID) String strEdgeId,
                                            @Parameter(description = "A list of assigned integration ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
                                            @RequestParam("integrationIds") String[] strIntegrationIds) throws Exception {
        checkArrayParameter("integrationIds", strIntegrationIds);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        edgeId = checkNotNull(edgeId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<IntegrationId> integrationIds = new ArrayList<>();
        for (String strIntegrationId : strIntegrationIds) {
            integrationIds.add(new IntegrationId(toUUID(strIntegrationId)));
        }
        return edgeService.findEdgeMissingAttributes(tenantId, edgeId, integrationIds);
    }

    @ApiOperation(value = "Find missing attributes for all related edges (findAllRelatedEdgesMissingAttributes)",
            notes = "Returns list of attribute names of all related edges that are missing in the integration configuration." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/integration/{integrationId}/allMissingAttributes")
    public String findAllRelatedEdgesMissingAttributes(@Parameter(description = INTEGRATION_ID_PARAM_DESCRIPTION, required = true)
                                                       @PathVariable("integrationId") String strIntegrationId) throws Exception {
        checkParameter("integrationId", strIntegrationId);
        IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
        integrationId = checkNotNull(integrationId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        return edgeService.findAllRelatedEdgesMissingAttributes(tenantId, integrationId);
    }

    @ApiOperation(value = "Get Integrations Converters info (getIntegrationsConvertersInfo)",
            notes = "Returns a JSON object containing information about existing tenant converters and converters available in library. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/integrations/converters/info")
    public Map<IntegrationType, IntegrationConvertersInfo> getIntegrationsConvertersInfo() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ);
        return tbIntegrationService.getIntegrationsConvertersInfo(getTenantId());
    }

    @ApiOperation(value = "Export integration as IoT Hub package",
                  notes = "Returns a ZIP containing integration.json, uplink.json, optional downlink.json, "
                        + "and form.json. Sensitive fields are tokenized via @TemplateField annotations on "
                        + "the integration's runtime POJO.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/integration/{integrationId}/export-package")
    public ResponseEntity<byte[]> exportIntegrationPackage(
            @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
        Integration integration = checkIntegrationId(integrationId, Operation.READ);

        JsonNode integrationJson = objectMapper.valueToTree(integration);

        IntegrationPackageExportService.TokenizationResult tokenized =
            packageExportService.tokenize(integration.getType(), integrationJson);

        JsonNode uplinkJson = null;
        if (integration.getDefaultConverterId() != null) {
            var uplinkConverter = converterService.findConverterById(
                integration.getTenantId(), integration.getDefaultConverterId());
            if (uplinkConverter != null) {
                uplinkJson = packageExportService.cleanConverter(
                    objectMapper.valueToTree(uplinkConverter));
            }
        }

        JsonNode downlinkJson = null;
        if (integration.getDownlinkConverterId() != null) {
            var downlinkConverter = converterService.findConverterById(
                integration.getTenantId(), integration.getDownlinkConverterId());
            if (downlinkConverter != null) {
                downlinkJson = packageExportService.cleanConverter(
                    objectMapper.valueToTree(downlinkConverter));
            }
        }

        byte[] zipBytes = packageExportService.zip(
            tokenized.json(), uplinkJson, downlinkJson, tokenized.formEntries());

        String safe = (integration.getName() != null ? integration.getName() : "integration")
            .replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = (safe.isBlank() ? "integration" : safe) + "-package.zip";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentLength(zipBytes.length)
            .contentType(MediaType.valueOf("application/zip"))
            .body(zipBytes);
    }

}
