/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.NameConflictPolicy;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.UniquifyStrategy;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.MODEL_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NAME_CONFLICT_POLICY_DESC;
import static org.thingsboard.server.controller.ControllerConstants.UNIQUIFY_SEPARATOR_DESC;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UNIQUIFY_STRATEGY_DESC;
import static org.thingsboard.server.controller.EdgeController.EDGE_ID;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class EntityViewController extends BaseController {

    public final TbEntityViewService tbEntityViewService;

    public static final String ENTITY_VIEW_ID = "entityViewId";

    @ApiOperation(value = "Get entity view (getEntityViewById)",
            notes = "Fetch the EntityView object based on the provided entity view id. "
                    + ENTITY_VIEW_DESCRIPTION + MODEL_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityView/{entityViewId}")
    public EntityView getEntityViewById(
            @Parameter(description = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        return checkEntityViewId(new EntityViewId(toUUID(strEntityViewId)), Operation.READ);
    }

    @ApiOperation(value = "Get Entity View info (getEntityViewInfoById)",
            notes = "Fetch the Entity View info object based on the provided Entity View Id. "
                    + ENTITY_VIEW_INFO_DESCRIPTION + MODEL_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityView/info/{entityViewId}")
    public EntityViewInfo getEntityViewInfoById(
            @Parameter(description = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        return checkEntityViewInfoId(entityViewId, Operation.READ);
    }

    @ApiOperation(value = "Save or update entity view (saveEntityView)",
            notes = ENTITY_VIEW_DESCRIPTION + MODEL_DESCRIPTION +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Entity View entity." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/entityView")
    public EntityView saveEntityView(
            @Parameter(description = "A JSON object representing the entity view.")
            @RequestBody EntityView entityView,
            @Parameter(description = NAME_CONFLICT_POLICY_DESC)
            @RequestParam(name = "nameConflictPolicy", defaultValue = "FAIL") NameConflictPolicy nameConflictPolicy,
            @Parameter(description = UNIQUIFY_SEPARATOR_DESC)
            @RequestParam(name = "uniquifySeparator", defaultValue = "_") String uniquifySeparator,
            @Parameter(description = UNIQUIFY_STRATEGY_DESC)
            @RequestParam(name = "uniquifyStrategy", defaultValue = "RANDOM") UniquifyStrategy uniquifyStrategy) throws Exception {
        entityView.setTenantId(getCurrentUser().getTenantId());
        EntityView existingEntityView = null;
        if (entityView.getId() == null) {
            accessControlService
                    .checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.CREATE, null, entityView);
        } else {
            existingEntityView = checkEntityViewId(entityView.getId(), Operation.WRITE);
        }
        return tbEntityViewService.save(entityView, existingEntityView, new NameConflictStrategy(nameConflictPolicy, uniquifySeparator, uniquifyStrategy), getCurrentUser());
    }

    @ApiOperation(value = "Delete entity view (deleteEntityView)",
            notes = "Delete the EntityView object based on the provided entity view id. "
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/entityView/{entityViewId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEntityView(
            @Parameter(description = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        EntityView entityView = checkEntityViewId(entityViewId, Operation.DELETE);
        tbEntityViewService.delete(entityView, getCurrentUser());
    }

    @ApiOperation(value = "Get Entity View by name (getTenantEntityView)",
            notes = "Fetch the Entity View object based on the tenant id and entity view name. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/entityViews", params = {"entityViewName"})
    public EntityView getTenantEntityView(
            @Parameter(description = "Entity View name")
            @RequestParam String entityViewName) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName));
    }

    @ApiOperation(value = "Assign Entity View to customer (assignEntityViewToCustomer)",
            notes = "Creates assignment of the Entity View to customer. Customer will be able to query Entity View afterwards." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/customer/{customerId}/entityView/{entityViewId}")
    public EntityView assignEntityViewToCustomer(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);

        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);

        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        checkEntityViewId(entityViewId, Operation.ASSIGN_TO_CUSTOMER);

        return tbEntityViewService.assignEntityViewToCustomer(getTenantId(), entityViewId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign Entity View from customer (unassignEntityViewFromCustomer)",
            notes = "Clears assignment of the Entity View to customer. Customer will not be able to query Entity View afterwards." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/customer/entityView/{entityViewId}")
    public EntityView unassignEntityViewFromCustomer(
            @Parameter(description = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        EntityView entityView = checkEntityViewId(entityViewId, Operation.UNASSIGN_FROM_CUSTOMER);
        if (entityView.getCustomerId() == null || entityView.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new IncorrectParameterException("Entity View isn't assigned to any customer!");
        }

        Customer customer = checkCustomerId(entityView.getCustomerId(), Operation.READ);

        return tbEntityViewService.unassignEntityViewFromCustomer(getTenantId(), entityViewId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Get Customer Entity Views (getCustomerEntityViews)",
            notes = "Returns a page of Entity View objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/entityViews", params = {"pageSize", "page"})
    public PageData<EntityView> getCustomerEntityViews(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @Parameter(description = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (type != null && !type.trim().isEmpty()) {
            return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId, customerId, pageLink, type));
        } else {
            return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get Customer Entity View info (getCustomerEntityViewInfos)",
            notes = "Returns a page of Entity View info objects assigned to customer. " + ENTITY_VIEW_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/entityViewInfos", params = {"pageSize", "page"})
    public PageData<EntityViewInfo> getCustomerEntityViewInfos(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @Parameter(description = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (type != null && !type.trim().isEmpty()) {
            return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
        } else {
            return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get Tenant Entity Views (getTenantEntityViews)",
            notes = "Returns a page of entity views owned by tenant. " + ENTITY_VIEW_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/entityViews", params = {"pageSize", "page"})
    public PageData<EntityView> getTenantEntityViews(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @Parameter(description = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

        if (type != null && !type.trim().isEmpty()) {
            return checkNotNull(entityViewService.findEntityViewByTenantIdAndType(tenantId, pageLink, type));
        } else {
            return checkNotNull(entityViewService.findEntityViewByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Tenant Entity Views (getTenantEntityViews)",
            notes = "Returns a page of entity views info owned by tenant. " + ENTITY_VIEW_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/entityViewInfos", params = {"pageSize", "page"})
    public PageData<EntityViewInfo> getTenantEntityViewInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @Parameter(description = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (type != null && !type.trim().isEmpty()) {
            return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndType(tenantId, type, pageLink));
        } else {
            return checkNotNull(entityViewService.findEntityViewInfosByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Find related entity views (findByQuery)",
            notes = "Returns all entity views that are related to the specific entity. " +
                    "The entity id, relation type, entity view types, depth of the search, and other query parameters defined using complex 'EntityViewSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/entityViews")
    public List<EntityView> findByQuery(
            @Parameter(description = "The entity view search query JSON")
            @RequestBody EntityViewSearchQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEntityViewTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        List<EntityView> entityViews = checkNotNull(entityViewService.findEntityViewsByQuery(getTenantId(), query).get());
        entityViews = filterEntityViewsByReadPermission(entityViews);
        return entityViews;
    }

    @ApiOperation(value = "Get Entity View Types (getEntityViewTypes)",
            notes = "Returns a set of unique entity view types based on entity views that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityView/types")
    public List<EntitySubtype> getEntityViewTypes() throws ThingsboardException, ExecutionException, InterruptedException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        ListenableFuture<List<EntitySubtype>> entityViewTypes = entityViewService.findEntityViewTypesByTenantId(tenantId);
        return checkNotNull(entityViewTypes.get());
    }

    @ApiOperation(value = "Make entity view publicly available (assignEntityViewToPublicCustomer)",
            notes = "Entity View will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the entity view." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/customer/public/entityView/{entityViewId}")
    public EntityView assignEntityViewToPublicCustomer(
            @Parameter(description = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        checkEntityViewId(entityViewId, Operation.ASSIGN_TO_CUSTOMER);
        return tbEntityViewService.assignEntityViewToPublicCustomer(getTenantId(), entityViewId, getCurrentUser());
    }

    @ApiOperation(value = "Assign entity view to edge (assignEntityViewToEdge)",
            notes = "Creates assignment of an existing entity view to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment entity view " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once entity view will be delivered to edge service, it's going to be available for usage on remote edge instance.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/edge/{edgeId}/entityView/{entityViewId}")
    public EntityView assignEntityViewToEdge(@PathVariable(EDGE_ID) String strEdgeId,
                                             @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        checkEntityViewId(entityViewId, Operation.READ);

        return tbEntityViewService.assignEntityViewToEdge(getTenantId(), getCurrentUser().getCustomerId(),
                entityViewId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign entity view from edge (unassignEntityViewFromEdge)",
            notes = "Clears assignment of the entity view to the edge. " +
                    EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove entity view " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove entity view locally.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/edge/{edgeId}/entityView/{entityViewId}")
    public EntityView unassignEntityViewFromEdge(@PathVariable(EDGE_ID) String strEdgeId,
                                                 @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        EntityView entityView = checkEntityViewId(entityViewId, Operation.READ);

        return tbEntityViewService.unassignEntityViewFromEdge(getTenantId(), entityView.getCustomerId(), entityView,
                edge, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edge/{edgeId}/entityViews", params = {"pageSize", "page"})
    public PageData<EntityView> getEdgeEntityViews(
            @PathVariable(EDGE_ID) String strEdgeId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        PageData<EntityView> nonFilteredResult;
        if (type != null && !type.trim().isEmpty()) {
            nonFilteredResult = entityViewService.findEntityViewsByTenantIdAndEdgeIdAndType(tenantId, edgeId, type, pageLink);
        } else {
            nonFilteredResult = entityViewService.findEntityViewsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
        }
        List<EntityView> filteredEntityViews = filterEntityViewsByReadPermission(nonFilteredResult.getData());
        PageData<EntityView> filteredResult = new PageData<>(filteredEntityViews,
                nonFilteredResult.getTotalPages(),
                nonFilteredResult.getTotalElements(),
                nonFilteredResult.hasNext());
        return checkNotNull(filteredResult);
    }

    @ApiOperation(value = "Get Entity Views By Ids (getEntityViewsByIds)",
            notes = "Requested entity views must be owned by tenant or assigned to customer which user is performing the request. ")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityViews", params = {"entityViewIds"})
    public List<EntityView> getEntityViewsByIds(@Parameter(description = "A list of entity view ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("entityViewIds") Set<UUID> entityViewUUIDs) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        List<EntityViewId> entityViewIds = new ArrayList<>();
        for (UUID entityViewUUID : entityViewUUIDs) {
            entityViewIds.add(new EntityViewId(entityViewUUID));
        }
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndIds(tenantId, entityViewIds);
        return filterEntityViewsByReadPermission(entityViews);
    }

    private List<EntityView> filterEntityViewsByReadPermission(List<EntityView> entityViews) {
        return entityViews.stream().filter(entityView -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ, entityView.getId(), entityView);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

}
