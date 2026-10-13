// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.rule.engine.flow.TbRuleChainInputNode;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeBulkImportService;
import org.thingsboard.server.service.edge.instructions.EdgeInstallInstructionsService;
import org.thingsboard.server.service.edge.instructions.EdgeUpgradeInstructionsService;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class EdgeController extends BaseController {

    private final EdgeBulkImportService edgeBulkImportService;
    private final TbEdgeService tbEdgeService;
    private final Optional<EdgeRpcService> edgeRpcServiceOpt;
    private final Optional<EdgeInstallInstructionsService> edgeInstallServiceOpt;
    private final Optional<EdgeUpgradeInstructionsService> edgeUpgradeServiceOpt;

    public static final String EDGE_ID = "edgeId";
    public static final String EDGE_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the edge is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the edge is assigned to the same customer.";

    @ApiOperation(value = "Is edges support enabled (isEdgesSupportEnabled)",
            notes = "Returns 'true' if edges support enabled on server, 'false' - otherwise.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edges/enabled")
    public boolean isEdgesSupportEnabled() {
        return edgesEnabled;
    }

    @ApiOperation(value = "Get Edge (getEdgeById)",
            notes = "Get the Edge object based on the provided Edge Id. " + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edge/{edgeId}")
    public Edge getEdgeById(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
                            @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);
        SecurityUser user = getCurrentUser();
        if (!hasPermissionEdgeCreateOrWrite(user)) {
            cleanUpLicenseKey(edge);
        }
        return edge;
    }

    @ApiOperation(value = "Get Edge Info (getEdgeInfoById)",
            notes = "Get the Edge info object based on the provided Edge Id. " + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edge/info/{edgeId}")
    public EdgeInfo getEdgeInfoById(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                    @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        EdgeInfo edge = checkEdgeInfoId(edgeId, Operation.READ);
        SecurityUser user = getCurrentUser();
        if (!hasPermissionEdgeCreateOrWrite(user)) {
            cleanUpLicenseKey(edge);
        }
        return edge;
    }

    @ApiOperation(value = "Create Or Update Edge (saveEdge)",
            notes = "Create or update the Edge. When creating edge, platform generates Edge Id as " + UUID_WIKI_LINK +
                    "The newly created edge id will be present in the response. " +
                    "Specify existing Edge id to update the edge. " +
                    "Referencing non-existing Edge Id will cause 'Not Found' error." +
                    "\n\nEdge name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the edge names and non-unique 'label' field for user-friendly visualization purposes." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Edge entity. ")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/edge")
    public Edge saveEdge(
            @Parameter(description = "A JSON value representing the edge.", required = true)
            @RequestBody Edge edge,
            @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
            @Parameter(description = "A list of entity group ids, separated by comma ','", array = @ArraySchema(schema = @Schema()))
            @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds) throws Exception {
        TenantId tenantId = getCurrentUser().getTenantId();
        edge.setTenantId(tenantId);
        boolean created = edge.getId() == null;

        RuleChain edgeTemplateRootRuleChain = null;
        if (created) {
            edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(tenantId);
            if (edgeTemplateRootRuleChain == null) {
                throw new DataValidationException("Root edge rule chain is not available!");
            }
        }

        List<EntityGroupId> entityGroupIds = new ArrayList<>();
        List<EntityGroup> entityGroups = new ArrayList<>();
        String[] groupIds = null;
        if (!StringUtils.isEmpty(strEntityGroupId)) {
            groupIds = new String[]{strEntityGroupId};
        } else if (strEntityGroupIds != null && strEntityGroupIds.length > 0) {
            groupIds = strEntityGroupIds;
        }
        if (groupIds != null) {
            for (String id : groupIds) {
                EntityGroupId entityGroupId = new EntityGroupId(toUUID(id));
                EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
                entityGroupIds.add(entityGroupId);
                entityGroups.add(entityGroup);
            }
        }

        Operation operation = created ? Operation.CREATE : Operation.WRITE;

        if (!entityGroupIds.isEmpty()) {
            for (EntityGroupId entityGroupId : entityGroupIds) {
                accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, operation,
                        edge.getId(), edge, entityGroupId);
            }
        } else {
            accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, operation,
                    edge.getId(), edge, null);
        }

        return tbEdgeService.save(edge, edgeTemplateRootRuleChain, entityGroups, getCurrentUser());
    }

    @ApiOperation(value = "Delete edge (deleteEdge)",
            notes = "Deletes the edge. Referencing non-existing edge Id will cause an error."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/edge/{edgeId}")
    public void deleteEdge(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
                           @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.DELETE);
        tbEdgeService.delete(edge, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Edges (getEdges)",
            notes = "Returns a page of edges owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edges", params = {"pageSize", "page"})
    public PageData<Edge> getEdges(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                   @RequestParam int pageSize,
                                   @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                   @RequestParam int page,
                                   @Parameter(description = EDGE_TEXT_SEARCH_DESCRIPTION)
                                   @RequestParam(required = false) String textSearch,
                                   @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "label", "customerTitle"}))
                                   @RequestParam(required = false) String sortProperty,
                                   @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getCurrentUser().getTenantId();
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.READ);
        return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Edges (getTenantEdges)",
            notes = "Returns a page of edges owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/edges", params = {"pageSize", "page"})
    public PageData<Edge> getTenantEdges(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.READ);
        if (type != null && !type.trim().isEmpty()) {
            return checkNotNull(edgeService.findEdgesByTenantIdAndType(tenantId, type, pageLink));
        } else {
            return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
        }
    }

    @Hidden
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/edges", params = {"edgeName"})
    public Edge getTenantEdge(@Parameter(description = "Unique name of the edge", required = true)
                              @RequestParam String edgeName) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(edgeService.findEdgeByTenantIdAndName(tenantId, edgeName));
    }

    @ApiOperation(value = "Get Tenant Edge by name (getTenantEdgeByName)",
            notes = "Requested edge must be owned by tenant or customer that the user belongs to. " +
                    "Edge name is an unique property of edge. So it can be used to identify the edge." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/edge")
    public Edge getTenantEdgeByName(@Parameter(description = "Unique name of the edge", required = true)
                                    @RequestParam String edgeName) throws ThingsboardException {
        return getTenantEdge(edgeName);
    }

    @ApiOperation(value = "Set root rule chain for provided edge (setEdgeRootRuleChain)",
            notes = "Change root rule chain of the edge to the new provided rule chain. \n" +
                    "This operation will send a notification to update root rule chain on remote edge service."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/edge/{edgeId}/{ruleChainId}/root")
    public Edge setEdgeRootRuleChain(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable(EDGE_ID) String strEdgeId,
                                     @Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable("ruleChainId") String strRuleChainId) throws Exception {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter("ruleChainId", strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        checkRuleChain(ruleChainId, Operation.READ);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.WRITE);
        return tbEdgeService.setEdgeRootRuleChain(edge, ruleChainId, getCurrentUser());
    }

    @ApiOperation(value = "Get Customer Edges (getCustomerEdges)",
            notes = "Returns a page of edges objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/edges")
    public PageData<Edge> getCustomerEdges(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<Edge> result;
        if (type != null && !type.trim().isEmpty()) {
            result = edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink);
        } else {
            result = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        }
        if (!hasPermissionEdgeCreateOrWrite(user)) {
            for (Edge edge : result.getData()) {
                cleanUpLicenseKey(edge);
            }
        }
        return checkNotNull(result);
    }

    @ApiOperation(value = "Get Edges (getUserEdges)",
            notes = "Returns a page of edges available for current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/user/edges", params = {"pageSize", "page"})
    public PageData<Edge> getUserEdges(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SecurityUser currentUser = getCurrentUser();
        MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
        return entityService.findUserEntities(currentUser.getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, EntityType.EDGE,
                Operation.READ, type, pageLink);
    }

    @ApiOperation(value = "Get All Edge Infos for current user (getAllEdgeInfos)",
            notes = "Returns a page of edge info objects owned by the tenant or the customer of a current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edgeInfos/all", params = {"pageSize", "page"})
    public PageData<EdgeInfo> getAllEdgeInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            if (includeCustomers != null && includeCustomers) {
                if (type != null && !type.isEmpty()) {
                    return checkNotNull(edgeService.findEdgeInfosByTenantIdAndType(tenantId, type, pageLink));
                } else {
                    return checkNotNull(edgeService.findEdgeInfosByTenantId(tenantId, pageLink));
                }
            } else {
                if (type != null && !type.isEmpty()) {
                    return checkNotNull(edgeService.findTenantEdgeInfosByTenantIdAndType(tenantId, type, pageLink));
                } else {
                    return checkNotNull(edgeService.findTenantEdgeInfosByTenantId(tenantId, pageLink));
                }
            }
        } else {
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (includeCustomers != null && includeCustomers) {
                if (type != null && !type.isEmpty()) {
                    return checkNotNull(edgeService.findEdgeInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(tenantId, customerId, type, pageLink));
                } else {
                    return checkNotNull(edgeService.findEdgeInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
                }
            } else {
                if (type != null && !type.isEmpty()) {
                    return checkNotNull(edgeService.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
                } else {
                    return checkNotNull(edgeService.findEdgeInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
                }
            }
        }
    }

    @ApiOperation(value = "Get Customer Edge Infos (getCustomerEdgeInfos)",
            notes = "Returns a page of edge info objects owned by the specified customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/edgeInfos")
    public PageData<EdgeInfo> getCustomerEdgeInfos(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (includeCustomers != null && includeCustomers) {
            if (type != null && !type.isEmpty()) {
                return checkNotNull(edgeService.findEdgeInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(edgeService.findEdgeInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
            }
        } else {
            if (type != null && !type.isEmpty()) {
                return checkNotNull(edgeService.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(edgeService.findEdgeInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        }
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edges", params = {"edgeIds"})
    public List<Edge> getEdgesByIds(
            @RequestParam("edgeIds") String[] strEdgeIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("edgeIds", strEdgeIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = user.getCustomerId();
        List<EdgeId> edgeIds = new ArrayList<>();
        for (String strEdgeId : strEdgeIds) {
            edgeIds.add(new EdgeId(toUUID(strEdgeId)));
        }
        ListenableFuture<List<Edge>> edgesFuture;
        if (customerId == null || customerId.isNullUid()) {
            edgesFuture = edgeService.findEdgesByTenantIdAndIdsAsync(tenantId, edgeIds);
        } else {
            edgesFuture = edgeService.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, edgeIds);
        }
        List<Edge> edges = edgesFuture.get();
        if (!hasPermissionEdgeCreateOrWrite(user)) {
            for (Edge edge : edges) {
                cleanUpLicenseKey(edge);
            }
        }
        return checkNotNull(edges);
    }

    @ApiOperation(value = "Get Edges By Ids (getEdgeList)",
            notes = "Requested edges must be owned by tenant or assigned to customer which user is performing the request." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edges/list")
    public List<Edge> getEdgeList(
            @Parameter(description = "A list of edges ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("edgeIds") String[] strEdgeIds) throws ThingsboardException, ExecutionException, InterruptedException {
        return getEdgesByIds(strEdgeIds);
    }

    @ApiOperation(value = "Find related edges (findEdgesByQuery)",
            notes = "Returns all edges that are related to the specific entity. " +
                    "The entity id, relation type, edge types, depth of the search, and other query parameters defined using complex 'EdgeSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/edges")
    public List<Edge> findEdgesByQuery(@RequestBody EdgeSearchQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEdgeTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<Edge> edges = checkNotNull(edgeService.findEdgesByQuery(tenantId, query).get());
        edges = edges.stream().filter(edge -> {
            try {
                accessControlService.checkPermission(user, Resource.EDGE, Operation.READ, edge.getId(), edge);
                return true;
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
        if (!hasPermissionEdgeCreateOrWrite(user)) {
            for (Edge edge : edges) {
                cleanUpLicenseKey(edge);
            }
        }
        return edges;
    }

    @ApiOperation(value = "Get edges by Entity Group Id (getEdgesByEntityGroupId)",
            notes = "Returns a page of Edge objects that belongs to specified Entity Group Id. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityGroup/{entityGroupId}/edges", params = {"pageSize", "page"})
    public PageData<Edge> getEdgesByEntityGroupId(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int page,
            @Parameter(description = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.EDGE, entityGroup.getType());
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(edgeService.findEdgesByEntityGroupId(entityGroupId, pageLink));
    }

    @ApiOperation(value = "Get Edge Types (getEdgeTypes)",
            notes = "Returns a set of unique edge types based on edges that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edge/types")
    public List<EntitySubtype> getEdgeTypes() throws ThingsboardException, ExecutionException, InterruptedException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        accessControlService.checkPermission(user, Resource.EDGE, Operation.READ);
        ListenableFuture<List<EntitySubtype>> edgeTypes = edgeService.findEdgeTypesByTenantId(tenantId);
        return checkNotNull(edgeTypes.get());
    }

    @ApiOperation(value = "Sync edge (syncEdge)",
            notes = "Starts synchronization process between edge and cloud. \n" +
                    "All entities that are assigned to particular edge are going to be send to remote edge service." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/edge/sync/{edgeId}")
    public DeferredResult<ResponseEntity> syncEdge(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                                   @PathVariable("edgeId") String strEdgeId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        final DeferredResult<ResponseEntity> response = new DeferredResult<>();
        if (isEdgesEnabled() && edgeRpcServiceOpt.isPresent()) {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            edgeId = checkNotNull(edgeId);
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            edgeRpcServiceOpt.get().processSyncRequest(tenantId, edgeId, fromEdgeSyncResponse -> reply(response, fromEdgeSyncResponse));
        } else {
            throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
        }
        return response;
    }

    private void reply(DeferredResult<ResponseEntity> response, FromEdgeSyncResponse fromEdgeSyncResponse) {
        if (fromEdgeSyncResponse.isSuccess()) {
            response.setResult(new ResponseEntity<>(HttpStatus.OK));
        } else {
            response.setErrorResult(new ThingsboardException(fromEdgeSyncResponse.getError(), ThingsboardErrorCode.GENERAL));
        }
    }

    @ApiOperation(value = "Find missing rule chains (findMissingToRelatedRuleChains)",
            notes = "Returns list of rule chains ids that are not assigned to particular edge, but these rule chains are present in the already assigned rule chains to edge." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/missingToRelatedRuleChains/{edgeId}")
    public String findMissingToRelatedRuleChains(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                                 @PathVariable("edgeId") String strEdgeId) throws ThingsboardException {
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        edgeId = checkNotNull(edgeId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        return edgeService.findMissingToRelatedRuleChains(tenantId, edgeId, TbRuleChainInputNode.class.getName());
    }

    @ApiOperation(value = "Import the bulk of edges (processEdgesBulkImport)",
            notes = "There's an ability to import the bulk of edges using the only .csv file." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edge/bulk_import")
    public BulkImportResult<Edge> processEdgesBulkImport(@RequestBody BulkImportRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(user.getTenantId());
        if (edgeTemplateRootRuleChain == null) {
            throw new DataValidationException("Root edge rule chain is not available!");
        }
        return edgeBulkImportService.processBulkImport(request, user,
                (edge, savingFunction) -> {
                    try {
                        EntityGroupId entityGroupId;
                        EntityGroup entityGroup = null;
                        if (!StringUtils.isEmpty(request.getEntityGroupId())) {
                            entityGroupId = new EntityGroupId(toUUID(request.getEntityGroupId()));
                            entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
                        }
                        savingFunction.apply(edge, entityGroup);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @ApiOperation(value = "Check edge license (checkInstance)",
            notes = "Checks license request from edge service by forwarding request to license portal.")
    @PostMapping(value = "/license/checkInstance")
    public ResponseEntity<JsonNode> checkInstance(@RequestBody JsonNode request) throws ThingsboardException {
        log.debug("Checking instance [{}]", request);
        try {
            return edgeLicenseService.checkInstance(request);
        } catch (Exception e) {
            log.error("Error occurred: [{}]", e.getMessage(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.SUBSCRIPTION_VIOLATION);
        }
    }

    @ApiOperation(value = "Activate edge instance (activateInstance)",
            notes = "Activates edge license on license portal.")
    @PostMapping(value = "/license/activateInstance", params = {"licenseSecret", "releaseDate"})
    public ResponseEntity<JsonNode> activateInstance(@RequestParam String licenseSecret,
                                                     @RequestParam String releaseDate) throws ThingsboardException {
        log.debug("Activating instance [{}], [{}]", licenseSecret, releaseDate);
        try {
            return edgeLicenseService.activateInstance(licenseSecret, releaseDate);
        } catch (Exception e) {
            log.error("Error occurred: [{}]", e.getMessage(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.SUBSCRIPTION_VIOLATION);
        }
    }

    private void cleanUpLicenseKey(Edge edge) {
        edge.setEdgeLicenseKey(null);
    }

    private boolean hasPermissionEdgeCreateOrWrite(SecurityUser user) throws ThingsboardException {
        return accessControlService.hasPermission(user, Resource.EDGE, Operation.CREATE) ||
                accessControlService.hasPermission(user, Resource.EDGE, Operation.WRITE);
    }

    @ApiOperation(value = "Get Edge Install Instructions (getEdgeInstallInstructions)",
            notes = "Get an install instructions for provided edge id." + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/instructions/install/{edgeId}/{method}")
    public EdgeInstructions getEdgeInstallInstructions(
            @Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("edgeId") String strEdgeId,
            @Parameter(description = "Installation method ('docker', 'ubuntu' or 'centos')", schema = @Schema(allowableValues = {"docker", "ubuntu", "centos"}))
            @PathVariable("method") String installationMethod,
            HttpServletRequest request) throws ThingsboardException {
        if (isEdgesEnabled() && edgeInstallServiceOpt.isPresent()) {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            edgeId = checkNotNull(edgeId);
            Edge edge = checkEdgeId(edgeId, Operation.READ);
            return checkNotNull(edgeInstallServiceOpt.get().getInstallInstructions(edge, installationMethod, request));
        } else {
            throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
        }
    }

    @ApiOperation(value = "Get Edge Upgrade Instructions (getEdgeUpgradeInstructions)",
            notes = "Get an upgrade instructions for provided edge version." + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/instructions/upgrade/{edgeVersion}/{method}")
    public EdgeInstructions getEdgeUpgradeInstructions(
            @Parameter(description = "Edge version", required = true)
            @PathVariable("edgeVersion") String edgeVersion,
            @Parameter(description = "Upgrade method ('docker', 'ubuntu' or 'centos')", schema = @Schema(allowableValues = {"docker", "ubuntu", "centos"}))
            @PathVariable("method") String method) throws Exception {
        if (isEdgesEnabled() && edgeUpgradeServiceOpt.isPresent()) {
            return checkNotNull(edgeUpgradeServiceOpt.get().getUpgradeInstructions(edgeVersion, method));
        } else {
            throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
        }
    }

    @ApiOperation(value = "Is edge upgrade enabled (isEdgeUpgradeAvailable)",
            notes = "Returns 'true' if upgrade available for connected edge, 'false' - otherwise.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/{edgeId}/upgrade/available")
    public boolean isEdgeUpgradeAvailable(
            @Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("edgeId") String strEdgeId) throws Exception {
        if (isEdgesEnabled() && edgeUpgradeServiceOpt.isPresent()) {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            edgeId = checkNotNull(edgeId);
            Edge edge = checkEdgeId(edgeId, Operation.READ);
            return edgeUpgradeServiceOpt.get().isUpgradeAvailable(edge.getTenantId(), edge.getId());
        } else {
            throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
        }
    }

}
