/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeBulkImportService;
import org.thingsboard.server.service.importing.BulkImportRequest;
import org.thingsboard.server.service.importing.BulkImportResult;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
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

    public static final String EDGE_ID = "edgeId";
    public static final String EDGE_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the edge is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the edge is assigned to the same customer.";

    @ApiOperation(value = "Is edges support enabled (isEdgesSupportEnabled)",
            notes = "Returns 'true' if edges support enabled on server, 'false' - otherwise.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges/enabled", method = RequestMethod.GET)
    @ResponseBody
    public boolean isEdgesSupportEnabled() {
        return edgesEnabled;
    }

    @ApiOperation(value = "Get Edge (getEdgeById)",
            notes = "Get the Edge object based on the provided Edge Id. " + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    public Edge getEdgeById(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                            @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.READ);
            if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())) {
                cleanUpLicenseKey(edge);
            }
            return edge;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Edge Info (getEdgeInfoById)",
            notes = "Get the Edge Info object based on the provided Edge Id. " + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/info/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    public EdgeInfo getEdgeInfoById(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                    @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            EdgeInfo edgeInfo = checkEdgeInfoId(edgeId, Operation.READ);
            if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())) {
                cleanUpLicenseKey(edgeInfo);
            }
            return edgeInfo;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Edge (saveEdge)",
            notes = "Create or update the Edge. When creating edge, platform generates Edge Id as " + UUID_WIKI_LINK +
                    "The newly created edge id will be present in the response. " +
                    "Specify existing Edge id to update the edge. " +
                    "Referencing non-existing Edge Id will cause 'Not Found' error." +
                    "\n\nEdge name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the edge names and non-unique 'label' field for user-friendly visualization purposes."
            + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge", method = RequestMethod.POST)
    @ResponseBody
    public Edge saveEdge(@ApiParam(value = "A JSON value representing the edge.", required = true)
                         @RequestBody Edge edge) throws ThingsboardException {
        try {
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

            Operation operation = created ? Operation.CREATE : Operation.WRITE;

            accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, operation,
                    edge.getId(), edge);

            Edge savedEdge = checkNotNull(edgeService.saveEdge(edge, true));
            onEdgeCreatedOrUpdated(tenantId, savedEdge, edgeTemplateRootRuleChain, !created, getCurrentUser());

            return savedEdge;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.EDGE), edge,
                    null, edge.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    private void onEdgeCreatedOrUpdated(TenantId tenantId, Edge edge, RuleChain edgeTemplateRootRuleChain, boolean updated, SecurityUser user) throws IOException, ThingsboardException {
        if (!updated) {
            ruleChainService.assignRuleChainToEdge(tenantId, edgeTemplateRootRuleChain.getId(), edge.getId());
            edgeNotificationService.setEdgeRootRuleChain(tenantId, edge, edgeTemplateRootRuleChain.getId());
            edgeService.assignDefaultRuleChainsToEdge(tenantId, edge.getId());
        }

        tbClusterService.broadcastEntityStateChangeEvent(edge.getTenantId(), edge.getId(),
                updated ? ComponentLifecycleEvent.UPDATED : ComponentLifecycleEvent.CREATED);

        logEntityAction(user, edge.getId(), edge, null, updated ? ActionType.UPDATED : ActionType.ADDED, null);
    }

    @ApiOperation(value = "Delete edge (deleteEdge)",
            notes = "Deletes the edge. Referencing non-existing edge Id will cause an error."+ TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEdge(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                           @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.DELETE);
            edgeService.deleteEdge(getTenantId(), edgeId);

            tbClusterService.broadcastEntityStateChangeEvent(getTenantId(), edgeId,
                    ComponentLifecycleEvent.DELETED);

            logEntityAction(edgeId, edge,
                    null,
                    ActionType.DELETED, null, strEdgeId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.EDGE),
                    null,
                    null,
                    ActionType.DELETED, e, strEdgeId);

            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Edges (getEdges)",
            notes = "Returns a page of edges owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Edge> getEdges(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                   @RequestParam int pageSize,
                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                   @RequestParam int page,
                                   @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
                                   @RequestParam(required = false) String textSearch,
                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                   @RequestParam(required = false) String sortProperty,
                                   @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Assign edge to customer (assignEdgeToCustomer)",
            notes = "Creates assignment of the edge to customer. Customer will be able to query edge afterwards." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/edge/{edgeId}", method = RequestMethod.POST)
    @ResponseBody
    public Edge assignEdgeToCustomer(@ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable("customerId") String strCustomerId,
                                     @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(EDGE_ID, strEdgeId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);

            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.ASSIGN_TO_CUSTOMER);

            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(getCurrentUser().getTenantId(), edgeId, customerId));

            tbClusterService.broadcastEntityStateChangeEvent(getTenantId(), edgeId,
                    ComponentLifecycleEvent.UPDATED);

            logEntityAction(edgeId, savedEdge,
                    savedEdge.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strEdgeId, strCustomerId, customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedEdge.getTenantId(), savedEdge.getId(),
                    customerId, EdgeEventActionType.ASSIGNED_TO_CUSTOMER);

            return savedEdge;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.EDGE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strEdgeId, strCustomerId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Unassign edge from customer (unassignEdgeFromCustomer)",
            notes = "Clears assignment of the edge to customer. Customer will not be able to query edge afterwards." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/edge/{edgeId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Edge unassignEdgeFromCustomer(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                         @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (edge.getCustomerId() == null || edge.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Edge isn't assigned to any customer!");
            }
            Customer customer = checkCustomerId(edge.getCustomerId(), Operation.READ);

            Edge savedEdge = checkNotNull(edgeService.unassignEdgeFromCustomer(getCurrentUser().getTenantId(), edgeId));

            tbClusterService.broadcastEntityStateChangeEvent(getTenantId(), edgeId,
                    ComponentLifecycleEvent.UPDATED);

            logEntityAction(edgeId, edge,
                    edge.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strEdgeId, customer.getId().toString(), customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedEdge.getTenantId(), savedEdge.getId(),
                    customer.getId(), EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER);

            return savedEdge;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.EDGE), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strEdgeId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Make edge publicly available (assignEdgeToPublicCustomer)",
            notes = "Edge will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the edge." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/edge/{edgeId}", method = RequestMethod.POST)
    @ResponseBody
    public Edge assignEdgeToPublicCustomer(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                           @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.ASSIGN_TO_CUSTOMER);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(edge.getTenantId());
            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(getCurrentUser().getTenantId(), edgeId, publicCustomer.getId()));

            tbClusterService.broadcastEntityStateChangeEvent(getTenantId(), edgeId,
                    ComponentLifecycleEvent.UPDATED);

            logEntityAction(edgeId, savedEdge,
                    savedEdge.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strEdgeId, publicCustomer.getId().toString(), publicCustomer.getName());

            return savedEdge;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.EDGE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strEdgeId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Edges (getTenantEdges)",
            notes = "Returns a page of edges owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Edge> getTenantEdges(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(edgeService.findEdgesByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Edge Infos (getTenantEdgeInfos)",
            notes = "Returns a page of edges info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + EDGE_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edgeInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EdgeInfo> getTenantEdgeInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(edgeService.findEdgeInfosByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(edgeService.findEdgeInfosByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Edge (getTenantEdge)",
            notes = "Requested edge must be owned by tenant or customer that the user belongs to. " +
                    "Edge name is an unique property of edge. So it can be used to identify the edge." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edges", params = {"edgeName"}, method = RequestMethod.GET)
    @ResponseBody
    public Edge getTenantEdge(@ApiParam(value = "Unique name of the edge", required = true)
                              @RequestParam String edgeName) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(edgeService.findEdgeByTenantIdAndName(tenantId, edgeName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Set root rule chain for provided edge (setEdgeRootRuleChain)",
            notes = "Change root rule chain of the edge to the new provided rule chain. \n" +
                    "This operation will send a notification to update root rule chain on remote edge service." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/{ruleChainId}/root", method = RequestMethod.POST)
    @ResponseBody
    public Edge setEdgeRootRuleChain(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable(EDGE_ID) String strEdgeId,
                                     @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable("ruleChainId") String strRuleChainId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter("ruleChainId", strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            checkRuleChain(ruleChainId, Operation.WRITE);

            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.WRITE);
            accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.WRITE,
                    edge.getId(), edge);

            Edge updatedEdge = edgeNotificationService.setEdgeRootRuleChain(getTenantId(), edge, ruleChainId);

            tbClusterService.broadcastEntityStateChangeEvent(updatedEdge.getTenantId(), updatedEdge.getId(), ComponentLifecycleEvent.UPDATED);

            logEntityAction(updatedEdge.getId(), updatedEdge, null, ActionType.UPDATED, null);

            return updatedEdge;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.EDGE),
                    null,
                    null,
                    ActionType.UPDATED, e, strEdgeId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Edges (getCustomerEdges)",
            notes = "Returns a page of edges objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Edge> getCustomerEdges(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            PageData<Edge> result;
            if (type != null && type.trim().length() > 0) {
                result = edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink);
            } else {
                result = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            }
            if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
                for (Edge edge : result.getData()) {
                    cleanUpLicenseKey(edge);
                }
            }
            return checkNotNull(result);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Edge Infos (getCustomerEdgeInfos)",
            notes = "Returns a page of edges info objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + EDGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/edgeInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EdgeInfo> getCustomerEdgeInfos(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            PageData<EdgeInfo> result;
            if (type != null && type.trim().length() > 0) {
                result = edgeService.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink);
            } else {
                result = edgeService.findEdgeInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            }
            if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
                for (Edge edge : result.getData()) {
                    cleanUpLicenseKey(edge);
                }
            }
            return checkNotNull(result);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Edges By Ids (getEdgesByIds)",
            notes = "Requested edges must be owned by tenant or assigned to customer which user is performing the request." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges", params = {"edgeIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Edge> getEdgesByIds(
            @ApiParam(value = "A list of edges ids, separated by comma ','", required = true)
            @RequestParam("edgeIds") String[] strEdgeIds) throws ThingsboardException {
        checkArrayParameter("edgeIds", strEdgeIds);
        try {
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
            if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
                for (Edge edge : edges) {
                    cleanUpLicenseKey(edge);
                }
            }
            return checkNotNull(edges);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find related edges (findByQuery)",
            notes = "Returns all edges that are related to the specific entity. " +
                    "The entity id, relation type, edge types, depth of the search, and other query parameters defined using complex 'EdgeSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges", method = RequestMethod.POST)
    @ResponseBody
    public List<Edge> findByQuery(@RequestBody EdgeSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEdgeTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
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
            if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
                for (Edge edge : edges) {
                    cleanUpLicenseKey(edge);
                }
            }
            return edges;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Edge Types (getEdgeTypes)",
            notes = "Returns a set of unique edge types based on edges that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getEdgeTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> edgeTypes = edgeService.findEdgeTypesByTenantId(tenantId);
            return checkNotNull(edgeTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Sync edge (syncEdge)",
            notes = "Starts synchronization process between edge and cloud. \n" +
                    "All entities that are assigned to particular edge are going to be send to remote edge service." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/sync/{edgeId}", method = RequestMethod.POST)
    public void syncEdge(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                         @PathVariable("edgeId") String strEdgeId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        try {
            if (isEdgesEnabled()) {
                EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
                edgeId = checkNotNull(edgeId);
                SecurityUser user = getCurrentUser();
                TenantId tenantId = user.getTenantId();
                edgeGrpcService.startSyncProcess(tenantId, edgeId);
            } else {
                throw new ThingsboardException("Edges support disabled", ThingsboardErrorCode.GENERAL);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find missing rule chains (findMissingToRelatedRuleChains)",
            notes = "Returns list of rule chains ids that are not assigned to particular edge, but these rule chains are present in the already assigned rule chains to edge." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/missingToRelatedRuleChains/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    public String findMissingToRelatedRuleChains(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                                 @PathVariable("edgeId") String strEdgeId) throws ThingsboardException {
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            edgeId = checkNotNull(edgeId);
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            return edgeService.findMissingToRelatedRuleChains(tenantId, edgeId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Import the bulk of edges (processEdgesBulkImport)",
            notes = "There's an ability to import the bulk of edges using the only .csv file." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping("/edge/bulk_import")
    public BulkImportResult<Edge> processEdgesBulkImport(@RequestBody BulkImportRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(user.getTenantId());
        if (edgeTemplateRootRuleChain == null) {
            throw new DataValidationException("Root edge rule chain is not available!");
        }

        return edgeBulkImportService.processBulkImport(request, user, importedAssetInfo -> {
            try {
                onEdgeCreatedOrUpdated(user.getTenantId(), importedAssetInfo.getEntity(), edgeTemplateRootRuleChain, importedAssetInfo.isUpdated(), user);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void cleanUpLicenseKey(Edge edge) {
        edge.setEdgeLicenseKey(null);
    }

    @ApiOperation(value = "Check edge license (checkInstance)",
            notes = "Checks license request from edge service by forwarding request to license portal.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/license/checkInstance", method = RequestMethod.POST)
    @ResponseBody
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
            notes = "Activates edge license on license portal.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/license/activateInstance", params = {"licenseSecret", "releaseDate"}, method = RequestMethod.POST)
    @ResponseBody
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
}
