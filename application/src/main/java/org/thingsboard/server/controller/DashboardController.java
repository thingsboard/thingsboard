/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.HomeDashboard;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_RESOURCES;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_RESOURCES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

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

    @Value("${ui.dashboard.max_datapoints_limit}")
    private long maxDatapointsLimit;

    @ApiOperation(value = "Get server time (getServerTime)",
            notes = "Get the server time (milliseconds since January 1, 1970 UTC). " +
                    "Used to adjust view of the dashboards according to the difference between browser and server time.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboard/serverTime")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "1636023857137")))
    public long getServerTime() throws ThingsboardException {
        return System.currentTimeMillis();
    }

    @ApiOperation(value = "Get max data points limit (getMaxDatapointsLimit)",
            notes = "Get the maximum number of data points that dashboard may request from the server per in a single subscription command. " +
                    "This value impacts the time window behavior. It impacts 'Max values' parameter in case user selects 'None' as 'Data aggregation function'. " +
                    "It also impacts the 'Grouping interval' in case of any other 'Data aggregation function' is selected. " +
                    "The actual value of the limit is configurable in the system configuration file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboard/maxDatapointsLimit")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "5000")))
    public long getMaxDatapointsLimit() throws ThingsboardException {
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
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Dashboard entity. " +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/dashboard")
    public void saveDashboard(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the dashboard.")
                                   @RequestBody Dashboard dashboard,
                                   @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader,
                                   HttpServletResponse response) throws Exception {
        dashboard.setTenantId(getTenantId());
        checkEntity(dashboard.getId(), dashboard, Resource.DASHBOARD);
        var savedDashboard = tbDashboardService.save(dashboard, getCurrentUser());
        response.setContentType(APPLICATION_JSON_VALUE);
        compressResponseWithGzipIFAccepted(acceptEncodingHeader, response, JacksonUtil.writeValueAsBytes(savedDashboard));
    }

    @ApiOperation(value = "Delete the Dashboard (deleteDashboard)",
            notes = "Delete the Dashboard." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/dashboard/{dashboardId}")
    public void deleteDashboard(@Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
                                @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.DELETE);
        tbDashboardService.delete(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Assign the Dashboard (assignDashboardToCustomer)",
            notes = "Assign the Dashboard to specified Customer or do nothing if the Dashboard is already assigned to that Customer. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/customer/{customerId}/dashboard/{dashboardId}")
    public Dashboard assignDashboardToCustomer(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);

        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDashboardService.assignDashboardToCustomer(dashboard, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign the Dashboard (unassignDashboardFromCustomer)",
            notes = "Unassign the Dashboard from specified Customer or do nothing if the Dashboard is already assigned to that Customer. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/customer/{customerId}/dashboard/{dashboardId}")
    public Dashboard unassignDashboardFromCustomer(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DASHBOARD_ID, strDashboardId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        return tbDashboardService.unassignDashboardFromCustomer(dashboard, customer, getCurrentUser());
    }

    @ApiOperation(value = "Update the Dashboard Customers (updateDashboardCustomers)",
            notes = "Updates the list of Customers that this Dashboard is assigned to. Removes previous assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object. " + TENANT_AUTHORITY_PARAGRAPH)

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/dashboard/{dashboardId}/customers")
    public Dashboard updateDashboardCustomers(
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @Parameter(description = "JSON array with the list of customer ids, or empty to remove all customers")
            @RequestBody(required = false) String[] strCustomerIds) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.updateDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Adds the Dashboard Customers (addDashboardCustomers)",
            notes = "Adds the list of Customers to the existing list of assignments for the Dashboard. Keeps previous assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/dashboard/{dashboardId}/customers/add")
    public Dashboard addDashboardCustomers(
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @Parameter(description = "JSON array with the list of customer ids")
            @RequestBody String[] strCustomerIds) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.addDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Remove the Dashboard Customers (removeDashboardCustomers)",
            notes = "Removes the list of Customers from the existing list of assignments for the Dashboard. Keeps other assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/dashboard/{dashboardId}/customers/remove")
    public Dashboard removeDashboardCustomers(
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @Parameter(description = "JSON array with the list of customer ids")
            @RequestBody String[] strCustomerIds) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.removeDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Assign the Dashboard to Public Customer (assignDashboardToPublicCustomer)",
            notes = "Assigns the dashboard to a special, auto-generated 'Public' Customer. Once assigned, unauthenticated users may browse the dashboard. " +
                    "This method is useful if you like to embed the dashboard on public web pages to be available for users that are not logged in. " +
                    "Be aware that making the dashboard public does not mean that it automatically makes all devices and assets you use in the dashboard to be public." +
                    "Use [assign Asset to Public Customer](#!/asset-controller/assignAssetToPublicCustomerUsingPOST) and " +
                    "[assign Device to Public Customer](#!/device-controller/assignDeviceToPublicCustomerUsingPOST) for this purpose. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard assignDashboardToPublicCustomer(
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDashboardService.assignDashboardToPublicCustomer(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Unassign the Dashboard from Public Customer (unassignDashboardFromPublicCustomer)",
            notes = "Unassigns the dashboard from a special, auto-generated 'Public' Customer. Once unassigned, unauthenticated users may no longer browse the dashboard. " +
                    "Returns the Dashboard object." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Dashboard unassignDashboardFromPublicCustomer(
            @Parameter(description = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        return tbDashboardService.unassignDashboardFromPublicCustomer(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Dashboards by System Administrator (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by tenant. " + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getTenantDashboards(
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
        return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Dashboards (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by the tenant of a current user. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
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
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (mobile != null && mobile) {
            return checkNotNull(dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink));
        } else {
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Customer Dashboards (getCustomerDashboards)",
            notes = "Returns a page of dashboard info objects owned by the specified customer. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getCustomerDashboards(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
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
        checkParameter(CUSTOMER_ID, strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (mobile != null && mobile) {
            return checkNotNull(dashboardService.findMobileDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        } else {
            return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get Home Dashboard (getHomeDashboard)",
            notes = "Returns the home dashboard object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. "
                    + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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
        JsonNode additionalInfo = user.getAdditionalInfo();
        HomeDashboard homeDashboard;
        homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
        if (homeDashboard == null) {
            if (securityUser.isCustomerUser()) {
                Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                additionalInfo = customer.getAdditionalInfo();
                homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
            }
            if (homeDashboard == null) {
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
    @RequestMapping(value = "/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
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
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
    public HomeDashboardInfo getTenantHomeDashboardInfo() throws ThingsboardException {
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

    @ApiOperation(value = "Update Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Update the home dashboard assignment for the current tenant. " +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void setTenantHomeDashboardInfo(
            @Parameter(description = "A JSON object that represents home dashboard id and other parameters", required = true)
            @RequestBody HomeDashboardInfo homeDashboardInfo) throws ThingsboardException {

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

    @ApiOperation(value = "Assign dashboard to edge (assignDashboardToEdge)",
            notes = "Creates assignment of an existing dashboard to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment dashboard " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once dashboard will be delivered to edge service, it's going to be available for usage on remote edge instance." +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard assignDashboardToEdge(@PathVariable("edgeId") String strEdgeId,
                                           @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        checkDashboardId(dashboardId, Operation.READ);
        return tbDashboardService.asignDashboardToEdge(getTenantId(), dashboardId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign dashboard from edge (unassignDashboardFromEdge)",
            notes = "Clears assignment of the dashboard to the edge. " +
                    EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove dashboard " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove dashboard locally." +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Dashboard unassignDashboardFromEdge(@PathVariable("edgeId") String strEdgeId,
                                               @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.READ);

        return tbDashboardService.unassignDashboardFromEdge(dashboard, edge, getCurrentUser());
    }

    @ApiOperation(value = "Get Edge Dashboards (getEdgeDashboards)",
            notes = "Returns a page of dashboard info objects assigned to the specified edge. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getEdgeDashboards(
            @Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
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
        checkParameter("edgeId", strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<DashboardInfo> nonFilteredResult = dashboardService.findDashboardsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
        List<DashboardInfo> filteredDashboards = filterDashboardsByReadPermission(nonFilteredResult.getData());
        PageData<DashboardInfo> filteredResult = new PageData<>(filteredDashboards,
                nonFilteredResult.getTotalPages(),
                nonFilteredResult.getTotalElements(),
                nonFilteredResult.hasNext());
        return checkNotNull(filteredResult);
    }

    @ApiOperation(value = "Get dashboards by Dashboard Ids (getDashboardsByIds)",
            notes = "Returns a list of DashboardInfo objects based on the provided ids. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/dashboards", params = {"dashboardIds"})
    public List<DashboardInfo> getDashboardsByIds(@Parameter(description = "A list of dashboard ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("dashboardIds") Set<UUID> dashboardUUIDs) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        List<DashboardId> dashboardIds = new ArrayList<>();
        for (UUID dashboardUUID : dashboardUUIDs) {
            dashboardIds.add(new DashboardId(dashboardUUID));
        }
        List<DashboardInfo> dashboards = dashboardService.findDashboardInfoByIds(tenantId, dashboardIds);
        return filterDashboardsByReadPermission(dashboards);
    }

    private Set<CustomerId> customerIdFromStr(String[] strCustomerIds) {
        Set<CustomerId> customerIds = new HashSet<>();
        if (strCustomerIds != null) {
            for (String strCustomerId : strCustomerIds) {
                customerIds.add(new CustomerId(UUID.fromString(strCustomerId)));
            }
        }
        return customerIds;
    }

    private List<DashboardInfo> filterDashboardsByReadPermission(List<DashboardInfo> dashboards) {
        return dashboards.stream().filter(dashboard -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ, dashboard.getId(), dashboard);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

}
