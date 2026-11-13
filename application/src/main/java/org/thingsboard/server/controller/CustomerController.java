// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.NameConflictPolicy;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.UniquifyStrategy;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.customer.TbCustomerService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.NAME_CONFLICT_POLICY_DESC;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UNIQUIFY_SEPARATOR_DESC;
import static org.thingsboard.server.controller.ControllerConstants.UNIQUIFY_STRATEGY_DESC;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerController extends BaseController {

    private final TbCustomerService tbCustomerService;

    public static final String IS_PUBLIC = "isPublic";
    public static final String CUSTOMER_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the customer is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the user belongs to the customer.";

    @ApiOperation(value = "Get Customer (getCustomerById)",
            notes = "Get the Customer object based on the provided Customer Id. "
                    + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}", method = RequestMethod.GET)
    @ResponseBody
    public Customer getCustomerById(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        checkDashboardInfo(customer.getAdditionalInfo(), HOME_DASHBOARD);
        return customer;
    }

    @ApiOperation(value = "Get Customer info (getCustomerInfoById)",
            notes = "Get the Customer info object based on the provided Customer Id. "
                    + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/info/{customerId}", method = RequestMethod.GET)
    @ResponseBody
    public CustomerInfo getCustomerInfoById(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        CustomerInfo customer = checkCustomerInfoId(customerId, Operation.READ);
        checkDashboardInfo(customer.getAdditionalInfo(), HOME_DASHBOARD);
        return customer;
    }


    @ApiOperation(value = "Get short Customer info (getShortCustomerInfoById)",
            notes = "Get the short customer object that contains only the title and 'isPublic' flag. "
                    + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/shortInfo", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getShortCustomerInfoById(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        ObjectNode infoObject = JacksonUtil.newObjectNode();
        infoObject.put("title", customer.getTitle());
        infoObject.put(IS_PUBLIC, customer.isPublic());
        return infoObject;
    }

    @ApiOperation(value = "Get Customer Title (getCustomerTitleById)",
            notes = "Get the title of the customer. "
                    + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/title", method = RequestMethod.GET, produces = "application/text")
    @ResponseBody
    public String getCustomerTitleById(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        return customer.getTitle();
    }

    @ApiOperation(value = "Create or update Customer (saveCustomer)",
            notes = "Creates or Updates the Customer. When creating customer, platform generates Customer Id as " + UUID_WIKI_LINK +
                    "The newly created Customer Id will be present in the response. " +
                    "Specify existing Customer Id to update the Customer. " +
                    "Referencing non-existing Customer Id will cause 'Not Found' error." + "Remove 'id', 'tenantId' from the request body example (below) to create new Customer entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer", method = RequestMethod.POST)
    @ResponseBody
    public Customer saveCustomer(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the customer.") @RequestBody Customer customer,
                                 @Parameter(description = ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
                                 @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
                                 @Parameter(description = ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION, array = @ArraySchema(schema = @Schema(type = "string")))
                                 @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds,
                                 @Parameter(description = NAME_CONFLICT_POLICY_DESC)
                                 @RequestParam(name = "nameConflictPolicy", defaultValue = "FAIL") NameConflictPolicy nameConflictPolicy,
                                 @Parameter(description = UNIQUIFY_SEPARATOR_DESC)
                                 @RequestParam(name = "uniquifySeparator", defaultValue = "_") String uniquifySeparator,
                                 @Parameter(description = UNIQUIFY_STRATEGY_DESC)
                                 @RequestParam(name = "uniquifyStrategy", defaultValue = "RANDOM") UniquifyStrategy uniquifyStrategy) throws ThingsboardException {
        if (!accessControlService.hasPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE)) {
            String prevHomeDashboardId = null;
            boolean prevHideDashboardToolbar = true;
            if (customer.getId() != null) {
                Customer prevCustomer = customerService.findCustomerById(getTenantId(), customer.getId());
                JsonNode additionalInfo = prevCustomer.getAdditionalInfo();
                if (additionalInfo != null && additionalInfo.hasNonNull(HOME_DASHBOARD_ID)) {
                    prevHomeDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                    if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                        prevHideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                    }
                }
            }
            JsonNode additionalInfo = customer.getAdditionalInfo();
            if (additionalInfo == null) {
                additionalInfo = JacksonUtil.newObjectNode();
                customer.setAdditionalInfo(additionalInfo);
            }
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, prevHomeDashboardId);
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, prevHideDashboardToolbar);
        }
        SecurityUser user = getCurrentUser();
        return saveGroupEntity(customer, strEntityGroupId, strEntityGroupIds, (customer1, entityGroups) -> {
            try {
                return tbCustomerService.save(customer, entityGroups, new NameConflictStrategy(nameConflictPolicy, uniquifySeparator, uniquifyStrategy), user);
            } catch (Exception e) {
                throw handleException(e);
            }
        });
    }

    @ApiOperation(value = "Delete Customer (deleteCustomer)",
            notes = "Deletes the Customer and all customer Users. " +
                    "All assigned Dashboards, Assets, Devices, etc. will be unassigned but not deleted. " +
                    "Referencing non-existing Customer Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCustomer(@Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
                               @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.DELETE);
        tbCustomerService.delete(customer, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Customers (getCustomers)",
            notes = "Returns a page of customers owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/customers")
    public PageData<Customer> getCustomers(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "email", "country", "city"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(customerService.findCustomersByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Customer by Customer title (getTenantCustomer)",
            notes = "Get the Customer using Customer Title. " + TENANT_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/customers")
    public Customer getTenantCustomer(
            @Parameter(description = "A string value representing the Customer title.")
            @RequestParam String customerTitle) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(customerService.findCustomerByTenantIdAndTitle(tenantId, customerTitle), "Customer with title [" + customerTitle + "] is not found");
    }

    @ApiOperation(value = "Get Customers (getUserCustomers)",
            notes = "Returns a page of customers available for the user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/customers", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Customer> getUserCustomers(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "email", "country", "city"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SecurityUser currentUser = getCurrentUser();
        MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
        return entityService.findUserEntities(currentUser.getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, EntityType.CUSTOMER,
                Operation.READ, null, pageLink);
    }

    @ApiOperation(value = "Get All Customer Infos for current user (getAllCustomerInfos)",
            notes = "Returns a page of customer info objects owned by the tenant or the customer of a current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customerInfos/all", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<CustomerInfo> getAllCustomerInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "email", "country", "city"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(customerService.findCustomerInfosByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(customerService.findTenantCustomerInfosByTenantId(tenantId, pageLink));
            }
        } else {
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(customerService.findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
            } else {
                return checkNotNull(customerService.findCustomerInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        }
    }

    @ApiOperation(value = "Get Customer sub-customers Infos (getCustomerCustomerInfos)",
            notes = "Returns a page of customer info objects owned by the specified customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/customerInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<CustomerInfo> getCustomerCustomerInfos(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "email", "country", "city"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (includeCustomers != null && includeCustomers) {
            return checkNotNull(customerService.findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
        } else {
            return checkNotNull(customerService.findCustomerInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customers", params = {"customerIds"})
    public List<Customer> getCustomersByIdsV1(@RequestParam("customerIds") Set<UUID> customerUUIDs) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<CustomerId> customerIds = new ArrayList<>();
        for (UUID customerUUID : customerUUIDs) {
            customerIds.add(new CustomerId(customerUUID));
        }
        List<Customer> customers = customerService.findCustomersByTenantIdAndIds(tenantId, customerIds);
        return filterCustomersByReadPermission(customers);
    }

    @ApiOperation(value = "Get customers by Customer Ids (getCustomersByIds)",
            notes = "Returns a list of Customer objects based on the provided ids." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customers/list")
    public List<Customer> getCustomersByIds(
            @Parameter(description = "A list of customer ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("customerIds") Set<UUID> customerUUIDs) throws ThingsboardException {
        return getCustomersByIdsV1(customerUUIDs);
    }

    @ApiOperation(value = "Get customers by Entity Group Id (getCustomersByEntityGroupId)",
            notes = "Returns a page of Customer objects that belongs to specified Entity Group Id. " +
                    PAGE_DATA_PARAMETERS + "\n\n" + RBAC_GROUP_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/customers", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Customer> getCustomersByEntityGroupId(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "email", "country", "city"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.CUSTOMER, entityGroup.getType());
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(customerService.findCustomersByEntityGroupId(entityGroupId, pageLink));
    }

    private List<Customer> filterCustomersByReadPermission(List<Customer> customers) {
        return customers.stream().filter(customer -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ, customer.getId(), customer);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

}
