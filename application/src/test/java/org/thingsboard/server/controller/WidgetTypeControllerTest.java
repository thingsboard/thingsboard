// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class WidgetTypeControllerTest extends AbstractControllerTest {

    private IdComparator<WidgetType> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveWidgetType() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        Assert.assertNotNull(savedWidgetType);
        Assert.assertNotNull(savedWidgetType.getId());
        Assert.assertNotNull(savedWidgetType.getFqn());
        Assert.assertTrue(savedWidgetType.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedWidgetType.getTenantId());
        Assert.assertEquals(widgetType.getName(), savedWidgetType.getName());
        Assert.assertEquals(widgetType.getDescriptor(), savedWidgetType.getDescriptor());

        savedWidgetType.setName("New Widget Type");

        doPost("/api/widgetType", savedWidgetType, WidgetType.class);

        WidgetTypeDetails foundWidgetType = doGet("/api/widgetType/" + savedWidgetType.getId().getId().toString(), WidgetTypeDetails.class);
        Assert.assertEquals(foundWidgetType.getName(), savedWidgetType.getName());
    }

    @Test
    public void testUpdateWidgetTypeFromDifferentTenant() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        loginDifferentTenant();
        doPost("/api/widgetType", savedWidgetType, WidgetTypeDetails.class, status().isForbidden());
        deleteDifferentTenant();
    }

    @Test
    public void testFindWidgetTypeById() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);
        WidgetTypeDetails foundWidgetType = doGet("/api/widgetType/" + savedWidgetType.getId().getId().toString(), WidgetTypeDetails.class);
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);
    }

    @Test
    public void testDeleteWidgetType() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        doDelete("/api/widgetType/" + savedWidgetType.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/widgetType/" + savedWidgetType.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveWidgetTypeWithEmptyName() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        doPost("/api/widgetType", widgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets type name should be specified")));
    }

    @Test
    public void testSaveWidgetTypeWithEmptyDescriptor() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{}", JsonNode.class));
        doPost("/api/widgetType", widgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets type descriptor can't be empty")));
    }

    @Test
    public void testUpdateWidgetTypeFqn() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);
        savedWidgetType.setFqn("some_fqn");
        doPost("/api/widgetType", savedWidgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Update of widget type fqn is prohibited")));

    }

    @Test
    public void testGetBundleWidgetTypes() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        List<WidgetType> widgetTypes = new ArrayList<>();
        for (int i = 0; i < 89; i++) {
            WidgetTypeDetails widgetType = new WidgetTypeDetails();
            widgetType.setName("Widget Type " + i);
            widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
            widgetTypes.add(new WidgetType(doPost("/api/widgetType", widgetType, WidgetTypeDetails.class)));
        }

        List<String> widgetTypeIds = widgetTypes.stream().map(type -> type.getId().getId().toString()).collect(Collectors.toList());
        doPost("/api/widgetsBundle/" + widgetsBundle.getId().getId().toString() + "/widgetTypes", widgetTypeIds);

        List<WidgetType> loadedWidgetTypes = doGetTyped("/api/widgetTypes?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>() {}, widgetsBundle.getId().getId().toString());

        Collections.sort(widgetTypes, idComparator);
        Collections.sort(loadedWidgetTypes, idComparator);

        Assert.assertEquals(widgetTypes, loadedWidgetTypes);

        // Create customer with widget type permissions
        Customer customer = new Customer();
        customer.setTitle("Test Customer");
        customer.setTenantId(savedTenant.getId());
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setName("UserGroup" + RandomStringUtils.randomAlphabetic(5));
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        Role role = createWidgetTypeGenericReadRole(RandomStringUtils.randomAlphabetic(10));
        role = doPost("/api/role", role, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(role.getId());
        groupPermission.setUserGroupId(userGroup.getId());
        doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("testCustomer" + RandomStringUtils.randomAlphabetic(5) + "@thingsboard.io");
        customerUser = createUser(customerUser, "password", userGroup.getId());

        login(customerUser.getEmail(), "password");

        List<WidgetType> loadedWidgetTypesCustomer = doGetTyped("/api/widgetTypes?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>() {}, widgetsBundle.getId().getId().toString());
        Collections.sort(loadedWidgetTypesCustomer, idComparator);
        Assert.assertEquals(widgetTypes, loadedWidgetTypesCustomer);

        List<WidgetTypeDetails> customerLoadedWidgetTypesDetails = doGetTyped("/api/widgetTypesDetails?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>() {}, widgetsBundle.getId().getId().toString());
        List<WidgetType> widgetTypesFromDetailsListCustomer = customerLoadedWidgetTypesDetails.stream().map(WidgetType::new).collect(Collectors.toList());
        Collections.sort(widgetTypesFromDetailsListCustomer, idComparator);
        Assert.assertEquals(widgetTypesFromDetailsListCustomer, loadedWidgetTypes);

        loginSysAdmin();

        List<WidgetType> sysAdminLoadedWidgetTypes = doGetTyped("/api/widgetTypes?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>() {}, widgetsBundle.getId().getId().toString());
        Collections.sort(sysAdminLoadedWidgetTypes, idComparator);
        Assert.assertEquals(widgetTypes, sysAdminLoadedWidgetTypes);

        List<WidgetTypeDetails> sysAdminLoadedWidgetTypesDetails = doGetTyped("/api/widgetTypesDetails?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>() {}, widgetsBundle.getId().getId().toString());
        List<WidgetType> widgetTypesFromDetailsListSysAdmin = sysAdminLoadedWidgetTypesDetails.stream().map(WidgetType::new).collect(Collectors.toList());
        Collections.sort(widgetTypesFromDetailsListSysAdmin, idComparator);
        Assert.assertEquals(widgetTypesFromDetailsListSysAdmin, loadedWidgetTypes);
    }

    @Test
    public void testGetWidgetType() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);
        WidgetType foundWidgetType = doGet("/api/widgetType?fqn={fqn}",
                WidgetType.class, "tenant." + savedWidgetType.getFqn());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(new WidgetType(savedWidgetType), foundWidgetType);
    }

    @Test
    public void testWidgetTypeAccessIfTenantUserHasGroupDashboardPermission() throws Exception {
        User tenantUser = new User();
        tenantUser.setAuthority(Authority.TENANT_ADMIN);
        tenantUser.setTenantId(tenantId);
        tenantUser.setEmail("testEmail" + RandomStringUtils.randomAlphabetic(5) + "@thingsboard.io");

        checkUserWithGroupDashboardPermissionHasWidgetTypeReadPermission(tenantUser);
    }

    @Test
    public void testWidgetTypeAccessIfCustomerUserHasGroupDashboardPermission() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Customer");
        customer.setTenantId(savedTenant.getId());
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("testEmail" + RandomStringUtils.randomAlphabetic(5) + "@thingsboard.io");

        checkUserWithGroupDashboardPermissionHasWidgetTypeReadPermission(customerUser);
    }

    @Test
    public void testWidgetTypeAccessIfTenantUserHasGenericDashboardPermission() throws Exception {
        User tenantUser = new User();
        tenantUser.setAuthority(Authority.TENANT_ADMIN);
        tenantUser.setTenantId(savedTenant.getId());
        tenantUser.setEmail("testEmail" + RandomStringUtils.randomAlphabetic(5) + "@thingsboard.io");

        checkUserWithGenericDashboardPermissionHasWidgetTypeReadPermission(tenantUser);
    }

    @Test
    public void testWidgetTypeAccessIfCustomerUserHasGenericDashboardPermission() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Customer");
        customer.setTenantId(savedTenant.getId());
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("testEmail" + RandomStringUtils.randomAlphabetic(5) + "@thingsboard.io");

        checkUserWithGenericDashboardPermissionHasWidgetTypeReadPermission(customerUser);
    }

    private void checkUserWithGroupDashboardPermissionHasWidgetTypeReadPermission(User user) throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setName("UserGroup" + RandomStringUtils.randomAlphabetic(5));
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setName("Dashboard group");
        dashboardGroup = doPost("/api/entityGroup", dashboardGroup, EntityGroup.class);

        Role savedGroupRole = createReadGroupRole(RandomStringUtils.randomAlphabetic(10));
        savedGroupRole = doPost("/api/role", savedGroupRole, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(savedGroupRole.getId());
        groupPermission.setUserGroupId(userGroup.getId());
        groupPermission.setEntityGroupId(dashboardGroup.getId());
        groupPermission.setEntityGroupType(dashboardGroup.getType());

        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        doPost("/api/groupPermission", groupPermission)
                .andExpect(status().isBadRequest());

        user = createUser(user, "password", userGroup.getId());
        login(user.getEmail(), "password");

        WidgetType foundWidgetType = doGet("/api/widgetType?fqn={fqn}",
                WidgetType.class, "tenant." + savedWidgetType.getFqn());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(new WidgetType(savedWidgetType), foundWidgetType);
    }

    private void checkUserWithGenericDashboardPermissionHasWidgetTypeReadPermission(User user) throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setName("UserGroup" + RandomStringUtils.randomAlphabetic(5));
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        Role role = createGenericReadRole(RandomStringUtils.randomAlphabetic(10));
        role = doPost("/api/role", role, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(role.getId());
        groupPermission.setUserGroupId(userGroup.getId());

        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        doPost("/api/groupPermission", groupPermission)
                .andExpect(status().isBadRequest());

        user = createUser(user, "password", userGroup.getId());
        login(user.getEmail(), "password");

        WidgetType foundWidgetType = doGet("/api/widgetType?fqn={fqn}",
                WidgetType.class, "tenant." + savedWidgetType.getFqn());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(new WidgetType(savedWidgetType), foundWidgetType);
    }

    private Role createReadGroupRole(String roleName) {
        Role role = new Role();
        role.setTenantId(savedTenant.getId());
        role.setName(roleName);
        role.setType(RoleType.GROUP);
        ArrayNode readPermissions = JacksonUtil.newArrayNode();
        readPermissions.add("READ");
        role.setPermissions(readPermissions);
        return role;
    }

    private Role createGenericReadRole(String roleName) {
        Role role = new Role();
        role.setTenantId(savedTenant.getId());
        role.setName(roleName);
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"READ\"]}"));
        return role;
    }

    private Role createWidgetTypeGenericReadRole(String roleName) {
        Role role = new Role();
        role.setTenantId(savedTenant.getId());
        role.setName(roleName);
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"WIDGET_TYPE\":[\"READ\"]}"));
        return role;
    }

}
