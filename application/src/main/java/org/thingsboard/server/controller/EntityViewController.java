// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.NameConflictPolicy;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.UniquifyStrategy;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.MODEL_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NAME_CONFLICT_POLICY_DESC;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UNIQUIFY_SEPARATOR_DESC;
import static org.thingsboard.server.controller.ControllerConstants.UNIQUIFY_STRATEGY_DESC;

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

    @ApiOperation(value = "Get entity view info (getEntityViewInfoById)",
            notes = "Fetch the Entity View info object based on the provided entity view id. "
                    + ENTITY_VIEW_INFO_DESCRIPTION + MODEL_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityView/info/{entityViewId}")
    public EntityViewInfo getEntityViewInfoById(
            @Parameter(description = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        return checkEntityViewInfoId(new EntityViewId(toUUID(strEntityViewId)), Operation.READ);
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
            @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
            @Parameter(description = "A list of entity group ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds,
            @Parameter(description = NAME_CONFLICT_POLICY_DESC)
            @RequestParam(name = "nameConflictPolicy", defaultValue = "FAIL") NameConflictPolicy nameConflictPolicy,
            @Parameter(description = UNIQUIFY_SEPARATOR_DESC)
            @RequestParam(name = "uniquifySeparator", defaultValue = "_") String uniquifySeparator,
            @Parameter(description = UNIQUIFY_STRATEGY_DESC)
            @RequestParam(name = "uniquifyStrategy", defaultValue = "RANDOM") UniquifyStrategy uniquifyStrategy) throws Exception {
        SecurityUser user = getCurrentUser();
        return saveGroupEntity(entityView, strEntityGroupId, strEntityGroupIds, (entityView1, entityGroups) -> {
            try {
                return tbEntityViewService.save(entityView1, entityGroups, new NameConflictStrategy(nameConflictPolicy, uniquifySeparator, uniquifyStrategy), user);
            } catch (Exception e) {
                throw handleException(e);
            }
        });
    }

    @ApiOperation(value = "Delete entity view (deleteEntityView)",
            notes = "Delete the EntityView object based on the provided entity view id. "
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
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

    @Hidden
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/entityViews", params = {"entityViewName"})
    public EntityView getTenantEntityView(
            @RequestParam String entityViewName) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName));
    }

    @ApiOperation(value = "Get Entity View by name (getTenantEntityViewByName)",
            notes = "Fetch the Entity View object based on the tenant id and entity view name. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/entityView")
    public EntityView getTenantEntityViewByName(
            @Parameter(description = "Entity View name")
            @RequestParam String entityViewName) throws ThingsboardException {
        return getTenantEntityView(entityViewName);
    }

    @ApiOperation(value = "Get Customer Entity Views (getCustomerEntityViews)",
            notes = "Returns a page of Entity View objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/entityViews")
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
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (type != null && !type.trim().isEmpty()) {
            return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId, customerId, pageLink, type));
        } else {
            return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
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
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

        if (type != null && !type.trim().isEmpty()) {
            return checkNotNull(entityViewService.findEntityViewByTenantIdAndType(tenantId, pageLink, type));
        } else {
            return checkNotNull(entityViewService.findEntityViewByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Entity Views (getUserEntityViews)",
            notes = "Returns a page of entity views that are available for the current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/user/entityViews", params = {"pageSize", "page"})
    public PageData<EntityView> getUserEntityViews(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @Parameter(description = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SecurityUser currentUser = getCurrentUser();
        MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
        return entityService.findUserEntities(currentUser.getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, EntityType.ENTITY_VIEW,
                Operation.READ, type, pageLink);
    }

    @ApiOperation(value = "Get All Entity View Infos for current user (getAllEntityViewInfos)",
            notes = "Returns a page of entity view info objects owned by the tenant or the customer of a current user. "
                    + ENTITY_VIEW_INFO_DESCRIPTION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityViewInfos/all", params = {"pageSize", "page"})
    public PageData<EntityViewInfo> getAllEntityViewInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @Parameter(description = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            if (includeCustomers != null && includeCustomers) {
                if (type != null && !type.isEmpty()) {
                    return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndType(tenantId, type, pageLink));
                } else {
                    return checkNotNull(entityViewService.findEntityViewInfosByTenantId(tenantId, pageLink));
                }
            } else {
                if (type != null && !type.isEmpty()) {
                    return checkNotNull(entityViewService.findTenantEntityViewInfosByTenantIdAndType(tenantId, type, pageLink));
                } else {
                    return checkNotNull(entityViewService.findTenantEntityViewInfosByTenantId(tenantId, pageLink));
                }
            }
        } else {
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (includeCustomers != null && includeCustomers) {
                if (type != null && !type.isEmpty()) {
                    return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(tenantId, customerId, type, pageLink));
                } else {
                    return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
                }
            } else {
                if (type != null && !type.isEmpty()) {
                    return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
                } else {
                    return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
                }
            }
        }
    }

    @ApiOperation(value = "Get Customer Entity View Infos (getCustomerEntityViewInfos)",
            notes = "Returns a page of entity view info objects owned by the specified customer. "
                    + ENTITY_VIEW_INFO_DESCRIPTION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/entityViewInfos", params = {"pageSize", "page"})
    public PageData<EntityViewInfo> getCustomerEntityViewInfos(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @Parameter(description = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (includeCustomers != null && includeCustomers) {
            if (type != null && !type.isEmpty()) {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
            }
        } else {
            if (type != null && !type.isEmpty()) {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        }
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityViews", params = {"entityViewIds"})
    public List<EntityView> getEntityViewsByIdsV1(
            @Parameter(description = "A list of entity view ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("entityViewIds") Set<UUID> entityViewUUIDs) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<EntityViewId> entityViewIds = new ArrayList<>();
        for (UUID entityViewUUID : entityViewUUIDs) {
            entityViewIds.add(new EntityViewId(entityViewUUID));
        }
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndIds(tenantId, entityViewIds);
        return filterEntityViewsByReadPermission(entityViews);
    }

    @ApiOperation(value = "Find related entity views (findEntityViewsByQuery)",
            notes = "Returns all entity views that are related to the specific entity. " +
                    "The entity id, relation type, entity view types, depth of the search, and other query parameters defined using complex 'EntityViewSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/entityViews")
    public List<EntityView> findEntityViewsByQuery(
            @Parameter(description = "The entity view search query JSON")
            @RequestBody EntityViewSearchQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEntityViewTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        List<EntityView> entityViews = checkNotNull(entityViewService.findEntityViewsByQuery(getTenantId(), query).get());
        return filterEntityViewsByReadPermission(entityViews);
    }

    @ApiOperation(value = "Get entity views by Entity Group Id (getEntityViewsByEntityGroupId)",
            notes = "Returns a page of Entity View objects that belongs to specified Entity View Id. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityGroup/{entityGroupId}/entityViews", params = {"pageSize", "page"})
    public PageData<EntityView> getEntityViewsByEntityGroupId(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.ENTITY_VIEW, entityGroup.getType());
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(entityViewService.findEntityViewsByEntityGroupId(entityGroupId, pageLink));
    }

    @ApiOperation(value = "Get Entity Views By Ids (getEntityViewsByIds)",
            notes = "Requested entity views must be owned by tenant or assigned to customer which user is performing the request. " + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityViews/list")
    public List<EntityView> getEntityViewsByIds(@Parameter(description = "A list of entity view ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
                                                @RequestParam("entityViewIds") Set<UUID> entityViewUUIDs) throws ThingsboardException {
        return getEntityViewsByIdsV1(entityViewUUIDs);
    }

    private List<EntityView> filterEntityViewsByReadPermission(List<EntityView> entityViews) {
        return entityViews.stream().filter(entityView -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ, entityView.getId(), entityView);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

    @ApiOperation(value = "Get Entity View Types (getEntityViewTypes)",
            notes = "Returns a set of unique entity view types based on entity views that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityView/types")
    public List<EntitySubtype> getEntityViewTypes() throws ThingsboardException, ExecutionException, InterruptedException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.ENTITY_VIEW, Operation.READ);
        TenantId tenantId = user.getTenantId();
        ListenableFuture<List<EntitySubtype>> entityViewTypes = entityViewService.findEntityViewTypesByTenantId(tenantId);
        return checkNotNull(entityViewTypes.get());
    }

}
