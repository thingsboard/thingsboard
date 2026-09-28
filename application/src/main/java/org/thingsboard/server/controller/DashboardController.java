// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HomeDashboard;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_RESOURCES;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_RESOURCES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.USER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.ControllerConstants.WL_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.WL_WRITE_CHECK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class DashboardController extends BaseController {

    private final TbDashboardService tbDashboardService;
    private final TbResourceService tbResourceService;

    public static final String DASHBOARD_ID = "dashboardId";
    private static final String HOME_DASHBOARD_ID = "homeDashboardId";
    private static final String HOME_DASHBOARD_HIDE_TOOLBAR = "homeDashboardHideToolbar";
    public static final String DASHBOARD_INFO_DEFINITION = "The Dashboard Info object contains lightweight information about the dashboard (e.g. title, image, assigned customers) but does not contain the heavyweight configuration JSON.";
    public static final String DASHBOARD_DEFINITION = "The Dashboard object is a heavyweight object that contains information about the dashboard (e.g. title, image, assigned customers) and also configuration JSON (e.g. layouts, widgets, entity aliases).";
    public static final String HIDDEN_FOR_MOBILE = "Exclude dashboards that are hidden for mobile";

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Value("${ui.dashboard.max_datapoints_limit}")
    private long maxDatapointsLimit;

    @ApiOperation(value = "Get server time (getServerTime)",
            notes = "Get the server time (milliseconds since January 1, 1970 UTC). " +
                    "Used to adjust view of the dashboards according to the difference between browser and server time.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboard/serverTime")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(type = "integer", format = "int64", example = "1636023857137")))
    public long getServerTime() {
        return System.currentTimeMillis();
    }

    @ApiOperation(value = "Get max data points limit (getMaxDatapointsLimit)",
            notes = "Get the maximum number of data points that dashboard may request from the server per in a single subscription command. " +
                    "This value impacts the time window behavior. It impacts 'Max values' parameter in case user selects 'None' as 'Data aggregation function'. " +
                    "It also impacts the 'Grouping interval' in case of any other 'Data aggregation function' is selected. " +
                    "The actual value of the limit is configurable in the system configuration file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboard/maxDatapointsLimit")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(type = "integer", format = "int64", example = "5000")))
    public long getMaxDatapointsLimit() {
        return maxDatapointsLimit;
    }

    @ApiOperation(value = "Get Dashboard Info (getDashboardInfoById)",
            notes = "Get the information about the dashboard based on 'dashboardId' parameter. " + DASHBOARD_INFO_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboard/info/{dashboardId}")
    public DashboardInfo getDashboardInfoById(
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        return checkDashboardInfoId(dashboardId, Operation.READ);
    }

    @ApiOperation(value = "Get Dashboard (getDashboardById)",
            notes = "Get the dashboard based on 'dashboardId' parameter. " + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
    )
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Dashboard.class)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboard/{dashboardId}")
    public void getDashboardById(@Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
                                 @PathVariable(DASHBOARD_ID) String strDashboardId,
                                 @Parameter(description = INCLUDE_RESOURCES_DESCRIPTION)
                                 @RequestParam(value = INCLUDE_RESOURCES, required = false) boolean includeResources,
                                 @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader,
                                 HttpServletResponse response) throws Exception {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.READ);
        if (includeResources) {
            dashboard.setResources(tbResourceService.exportResources(dashboard, getCurrentUser()));
        }
        response.setContentType(APPLICATION_JSON_VALUE);
        compressResponseWithGzipIFAccepted(acceptEncodingHeader, response, JacksonUtil.writeValueAsBytes(dashboard));
    }

    @ApiOperation(value = "Create Or Update Dashboard (saveDashboard)",
            notes = "Create or update the Dashboard. When creating dashboard, platform generates Dashboard Id as " + UUID_WIKI_LINK +
                    "The newly created Dashboard id will be present in the response. " +
                    "Specify existing Dashboard id to update the dashboard. " +
                    "Referencing non-existing dashboard Id will cause 'Not Found' error. " +
                    "Only users with 'TENANT_ADMIN') authority may create the dashboards." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Dashboard entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Dashboard.class)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/dashboard")
    public void saveDashboard(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the dashboard.")
                              @RequestBody Dashboard dashboard,
                              @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
                              @Parameter(description = "A list of entity group ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
                              @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds,
                              @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader,
                              HttpServletResponse response) throws Exception {
        SecurityUser user = getCurrentUser();
        var savedDashboard = saveGroupEntity(dashboard, strEntityGroupId, strEntityGroupIds, (dashboard1, entityGroups) -> {
            try {
                return tbDashboardService.save(dashboard1, entityGroups, user);
            } catch (Exception e) {
                throw handleException(e);
            }
        });
        response.setContentType(APPLICATION_JSON_VALUE);
        compressResponseWithGzipIFAccepted(acceptEncodingHeader, response, JacksonUtil.writeValueAsBytes(savedDashboard));
    }

    @ApiOperation(value = "Delete the Dashboard (deleteDashboard)",
            notes = "Delete the Dashboard. Only users with 'TENANT_ADMIN') authority may delete the dashboards." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/dashboard/{dashboardId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDashboard(
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.DELETE);
        tbDashboardService.delete(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Dashboards by System Administrator (getTenantDashboardsByTenantId)",
            notes = "Returns a page of dashboard info objects owned by tenant. " + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/tenant/{tenantId}/dashboards")
    public PageData<DashboardInfo> getTenantDashboardsByTenantId(
            @Parameter(description = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(TENANT_ID) String strTenantId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        checkTenantId(tenantId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
        return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Dashboards (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by the tenant of a current user. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/dashboards")
    public PageData<DashboardInfo> getTenantDashboards(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = HIDDEN_FOR_MOBILE)
            @RequestParam(required = false) Boolean mobile,
            @Parameter(description = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (mobile != null && mobile) {
            return checkNotNull(dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink));
        } else {
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Dashboards (getUserDashboards)",
            notes = "Returns a page of Dashboard Info objects available for specified or current user. " +
                    PAGE_DATA_PARAMETERS + DASHBOARD_INFO_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/user/dashboards", params = {"pageSize", "page"})
    public PageData<DashboardInfo> getUserDashboards(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = HIDDEN_FOR_MOBILE)
            @RequestParam(required = false) Boolean mobile,
            @Parameter(description = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Filter by allowed operations for the current user")
            @RequestParam(required = false) String operation,
            @Parameter(description = USER_ID_PARAM_DESCRIPTION)
            @RequestParam(name = "userId", required = false) String strUserId) throws ThingsboardException {
        SecurityUser securityUser;
        if (!StringUtils.isEmpty(strUserId)) {
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.READ);
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user, false));
        } else {
            securityUser = getCurrentUser();
        }
        Operation operationType = Operation.READ;
        if (!StringUtils.isEmpty(operation)) {
            try {
                operationType = Operation.valueOf(operation);
            } catch (IllegalArgumentException e) {
                throw new ThingsboardException("Unsupported operation type '" + operation + "'!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        MergedUserPermissions mergedUserPermissions = securityUser.getUserPermissions();
        return entityService.findUserEntities(securityUser.getTenantId(), securityUser.getCustomerId(), mergedUserPermissions, EntityType.DASHBOARD,
                operationType, null, pageLink, mobile != null ? mobile : false, false);
    }

    @ApiOperation(value = "Get All Dashboards for current user (getAllDashboards)",
            notes = "Returns a page of dashboard info objects owned by the tenant or the customer of a current user. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboards/all", params = {"pageSize", "page"})
    public PageData<DashboardInfo> getAllDashboards(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(dashboardService.findTenantDashboardsByTenantId(tenantId, pageLink));
            }
        } else {
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
            } else {
                return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        }
    }

    @ApiOperation(value = "Get Customer Dashboards (getCustomerDashboards)",
            notes = "Returns a page of dashboard info objects owned by the specified customer. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/dashboards")
    public PageData<DashboardInfo> getCustomerDashboards(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (includeCustomers != null && includeCustomers) {
            return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
        } else {
            return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get dashboards by Dashboard Ids (getDashboardsByIds)",
            notes = "Returns a list of DashboardInfo objects based on the provided ids. Filters the list based on the user permissions. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboards", params = {"dashboardIds"})
    public List<DashboardInfo> getDashboardsByIds(
            @Parameter(description = "A list of dashboard ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("dashboardIds") Set<UUID> dashboardUUIDs) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<DashboardId> dashboardIds = new ArrayList<>();
        for (UUID dashboardUUID : dashboardUUIDs) {
            dashboardIds.add(new DashboardId(dashboardUUID));
        }
        List<DashboardInfo> dashboards = dashboardService.findDashboardInfoByIds(tenantId, dashboardIds);
        return filterDashboardsByReadPermission(dashboards);
    }

    @ApiOperation(value = "Get dashboards by Entity Group Id (getDashboardsByEntityGroupId)",
            notes = "Returns a page of Dashboard objects that belongs to specified Entity Group Id. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityGroup/{entityGroupId}/dashboards", params = {"pageSize", "page"})
    public PageData<DashboardInfo> getDashboardsByEntityGroupId(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.DASHBOARD, entityGroup.getType());
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(dashboardService.findDashboardsByEntityGroupId(entityGroupId, pageLink));
    }

    @ApiOperation(value = "Import Dashboards (importGroupDashboards)",
            notes = "Import the dashboards to specified group."
                    + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_WRITE_CHECK,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/entityGroup/{entityGroupId}/dashboards/import")
    public void importGroupDashboards(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = "JSON array with the dashboard objects", required = true)
            @RequestBody List<Dashboard> dashboardList,
            @Parameter(description = "Overwrite dashboards with the same name")
            @RequestParam(required = false, defaultValue = "false", name = "overwrite") boolean overwrite) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        checkEntityGroupId(entityGroupId, Operation.WRITE);
        dashboardService.importDashboards(tenantId, entityGroupId, dashboardList, overwrite);
    }

    @ApiOperation(value = "Export Dashboards (exportGroupDashboards)",
            notes = "Export the dashboards that belong to specified group id."
                    + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Dashboard.class))))
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/entityGroup/{entityGroupId}/dashboards/export", params = {"limit"})
    public void exportGroupDashboards(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = "Limit of the entities to export", required = true)
            @RequestParam int limit,
            @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader,
            HttpServletResponse response) throws Exception {
        TenantId tenantId = getCurrentUser().getTenantId();
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        checkEntityGroupId(entityGroupId, Operation.READ);
        TimePageLink pageLink = new TimePageLink(limit);
        response.setContentType(APPLICATION_JSON_VALUE);
        var dashboards = dashboardService.exportDashboards(tenantId, entityGroupId, pageLink);
        compressResponseWithGzipIFAccepted(acceptEncodingHeader, response, JacksonUtil.writeValueAsBytes(dashboards));
    }

    private List<DashboardInfo> filterDashboardsByReadPermission(List<DashboardInfo> dashboards) {
        return dashboards.stream().filter(dashboard -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ, dashboard.getId(), dashboard);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

    @ApiOperation(value = "Get Home Dashboard (getHomeDashboard)",
            notes = "Returns the home dashboard object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. "
                    + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = HomeDashboard.class)))
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboard/home")
    public void getHomeDashboard(@RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader,
                                 HttpServletResponse response) throws Exception {
        SecurityUser securityUser = getCurrentUser();
        response.setContentType(APPLICATION_JSON_VALUE);
        if (securityUser.isSystemAdmin()) {
            return;
        }
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        JsonNode additionalInfo;
        HomeDashboard homeDashboard = null;

        boolean ownerWhiteLabelingAllowed = whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), user.getCustomerId());

        if (ownerWhiteLabelingAllowed) {
            additionalInfo = user.getAdditionalInfo();
            homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
        }
        if (homeDashboard == null) {
            if (securityUser.isCustomerUser() && ownerWhiteLabelingAllowed) {
                Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                additionalInfo = customer.getAdditionalInfo();
                homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
            }
            //TODO: merge with parent customers if any.
            if (homeDashboard == null && ((securityUser.isTenantAdmin() && ownerWhiteLabelingAllowed) ||
                    (securityUser.isCustomerUser() && whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), null)))) {
                Tenant tenant = tenantService.findTenantById(securityUser.getTenantId());
                additionalInfo = tenant.getAdditionalInfo();
                homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
            }
        }
        if (homeDashboard != null) {
            compressResponseWithGzipIFAccepted(acceptEncodingHeader, response, JacksonUtil.writeValueAsBytes(homeDashboard));
        }
    }

    @ApiOperation(value = "Get Home Dashboard Info (getHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboard/home/info")
    public HomeDashboardInfo getHomeDashboardInfo() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        if (securityUser.isSystemAdmin()) {
            return null;
        }
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        JsonNode additionalInfo = user.getAdditionalInfo();
        return getHomeDashboardInfo(securityUser, additionalInfo);
    }

    @ApiOperation(value = "Get Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the corresponding tenant. " +
                    TENANT_AUTHORITY_PARAGRAPH + WL_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/dashboard/home/info")
    public HomeDashboardInfo getTenantHomeDashboardInfo() throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.READ);
        Tenant tenant = tenantService.findTenantById(getTenantId());
        JsonNode additionalInfo = tenant.getAdditionalInfo();
        DashboardId dashboardId = null;
        boolean hideDashboardToolbar = true;
        if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
            String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
            dashboardId = new DashboardId(toUUID(strDashboardId));
            if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
            }
        }
        return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
    }

    @ApiOperation(value = "Get Customer Home Dashboard Info (getCustomerHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the corresponding customer. " +
                    CUSTOMER_AUTHORITY_PARAGRAPH + WL_READ_CHECK)
    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @GetMapping(value = "/customer/dashboard/home/info")
    public HomeDashboardInfo getCustomerHomeDashboardInfo() throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.READ);
        Customer customer = customerService.findCustomerById(getTenantId(), getCurrentUser().getCustomerId());
        JsonNode additionalInfo = customer.getAdditionalInfo();
        DashboardId dashboardId = null;
        boolean hideDashboardToolbar = true;
        if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
            String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
            dashboardId = new DashboardId(toUUID(strDashboardId));
            if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
            }
        }
        return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
    }

    @ApiOperation(value = "Update Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Update the home dashboard assignment for the current tenant. " +
                    TENANT_AUTHORITY_PARAGRAPH + WL_WRITE_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/tenant/dashboard/home/info")
    @ResponseStatus(value = HttpStatus.OK)
    public void setTenantHomeDashboardInfo(
            @Parameter(description = "A JSON object that represents home dashboard id and other parameters", required = true)
            @RequestBody HomeDashboardInfo homeDashboardInfo) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.WRITE);
        if (homeDashboardInfo.getDashboardId() != null) {
            checkDashboardId(homeDashboardInfo.getDashboardId(), Operation.READ);
        }
        Tenant tenant = tenantService.findTenantById(getTenantId());
        JsonNode additionalInfo = tenant.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        if (homeDashboardInfo.getDashboardId() != null) {
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, homeDashboardInfo.getDashboardId().getId().toString());
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, homeDashboardInfo.isHideDashboardToolbar());
        } else {
            ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_ID);
            ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_HIDE_TOOLBAR);
        }
        tenant.setAdditionalInfo(additionalInfo);
        tenantService.saveTenant(tenant);
    }

    @ApiOperation(value = "Update Customer Home Dashboard Info (setCustomerHomeDashboardInfo)",
            notes = "Update the home dashboard assignment for the current customer. " +
                    CUSTOMER_AUTHORITY_PARAGRAPH + WL_WRITE_CHECK)
    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @PostMapping(value = "/customer/dashboard/home/info")
    @ResponseStatus(value = HttpStatus.OK)
    public void setCustomerHomeDashboardInfo(
            @Parameter(description = "A JSON object that represents home dashboard id and other parameters", required = true)
            @RequestBody HomeDashboardInfo homeDashboardInfo) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.WRITE);
        if (homeDashboardInfo.getDashboardId() != null) {
            checkDashboardId(homeDashboardInfo.getDashboardId(), Operation.READ);
        }
        Customer customer = customerService.findCustomerById(getTenantId(), getCurrentUser().getCustomerId());
        JsonNode additionalInfo = customer.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        if (homeDashboardInfo.getDashboardId() != null) {
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, homeDashboardInfo.getDashboardId().getId().toString());
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, homeDashboardInfo.isHideDashboardToolbar());
        } else {
            ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_ID);
            ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_HIDE_TOOLBAR);
        }
        customer.setAdditionalInfo(additionalInfo);
        customerService.saveCustomer(customer);
    }

    private HomeDashboard extractHomeDashboardFromAdditionalInfo(JsonNode additionalInfo) {
        try {
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
                Dashboard dashboard = checkDashboardId(dashboardId, Operation.READ);
                boolean hideDashboardToolbar = true;
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
                return new HomeDashboard(dashboard, hideDashboardToolbar);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
