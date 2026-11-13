// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.pat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
@DaoSqlTest
public class ApiKeyAuthenticationProviderTest extends AbstractControllerTest {

    private final TypeReference<PageData<Device>> PAGE_DATA_DEVICE_TYPE_REF = new TypeReference<>() {
    };
    private final TypeReference<PageData<DomainInfo>> PAGE_DATA_DOMAIN_TYPE_REF = new TypeReference<>() {
    };

    ApiKey savedApiKey;

    @Autowired
    private ApiKeyService apiKeyService;

    @MockitoSpyBean
    private UserPermissionsService userPermissionsService;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo(tenantId, tenantAdminUserId, false, null);
        savedApiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(savedApiKey.getValue());
    }

    @After
    public void cleanUp() throws Exception {
        resetApiKey();
        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void testSaveEdgeWithApiKey() throws Exception {
        Edge edge = constructEdge("My edge", "default");

        Mockito.reset(tbClusterService, auditLogService);

        Edge savedEdge = doPostWithApiKey("/api/edge", edge, Edge.class, null);

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        Assert.assertEquals(tenantId, savedEdge.getTenantId());
        Assert.assertNotNull(savedEdge.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedEdge.getCustomerId().getId());
        Assert.assertEquals(edge.getName(), savedEdge.getName());

        testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ADDED, 2);

        savedEdge.setName("My new edge");
        doPostWithApiKey("/api/edge", savedEdge, Edge.class, null);

        Edge foundEdge = doGetWithApiKey("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(foundEdge, foundEdge.getId(), foundEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.UPDATED, 1);

        doDeleteWithApiKey("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testUnauthorizedWhenKeyDisabled() throws Exception {
        ApiKeyInfo disabledApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/enabled/false", Boolean.FALSE, ApiKeyInfo.class);
        Assert.assertFalse(disabledApiKeyInfo.isEnabled());
        doGetWithApiKey("/api/admin/featuresInfo").andExpect(status().isUnauthorized());
    }

    @Test
    public void testUnauthorizedWhenKeyExpired() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo(tenantId, tenantAdminUserId, false, null);
        apiKeyInfo.setExpirationTime(System.currentTimeMillis() - 1000);
        ApiKey savedApiKeyWithBad = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(savedApiKeyWithBad.getValue());
        doPost("/api/apiKey", savedApiKey, ApiKeyInfo.class);
        doGetWithApiKey("/api/admin/featuresInfo").andExpect(status().isUnauthorized());
    }

    @Test
    public void testUnauthorizedWhenUserCredentialsDisabled() throws Exception {
        User newUser = new User();
        newUser.setAuthority(Authority.TENANT_ADMIN);
        newUser.setTenantId(tenantId);
        newUser.setEmail("testUser" + RandomStringUtils.secure().nextAlphanumeric(10) + "@thingsboard.org");
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        User savedUser = createUser(newUser, "testPassword1");

        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("Test API key for user credentials test");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(savedUser.getId());
        ApiKey testApiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(testApiKey.getValue());

        doGetWithApiKey("/api/admin/repositorySettings/exists").andExpect(status().isOk());

        doPost("/api/user/" + savedUser.getId().getId() + "/userCredentialsEnabled?userCredentialsEnabled=false").andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> doGetWithApiKey("/api/admin/repositorySettings/exists").andExpect(status().isUnauthorized()));

        resetApiKey();
        doDelete("/api/apiKey/" + testApiKey.getId()).andExpect(status().isOk());
        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testUnauthorizedWhenUserDeleted() throws Exception {
        User newUser = new User();
        newUser.setAuthority(Authority.TENANT_ADMIN);
        newUser.setTenantId(tenantId);
        newUser.setEmail("testUser" + RandomStringUtils.secure().nextAlphanumeric(10) + "@thingsboard.org");
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        User savedUser = createUser(newUser, "testPassword1");

        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("Test API key for user deletion test");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(savedUser.getId());
        ApiKey testApiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(testApiKey.getValue());

        doGetWithApiKey("/api/admin/repositorySettings/exists").andExpect(status().isOk());

        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> doGetWithApiKey("/api/admin/repositorySettings/exists").andExpect(status().isUnauthorized()));
    }

    @Test
    public void testInternalApiKeyWithTenantAdminPermissions_canOnlyRead() throws Exception {
        loginSysAdmin();

        Map<Resource, Set<Operation>> tenantAdminPermissions = new HashMap<>();
        tenantAdminPermissions.put(Resource.DEVICE, Set.of(Operation.READ));

        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.TENANT_ADMIN, tenantAdminPermissions);

        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);

        var apiKeyInfo = constructApiKeyInfo(TenantId.SYS_TENANT_ID, currentUserId, true, authorityPermissionsInfo);
        ApiKey internalApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
        Assert.assertNotNull(internalApiKey);
        Assert.assertTrue(internalApiKey.isInternal());

        setApiKey(internalApiKey.getValue());

        PageLink pageLink = new PageLink(15, 0);
        doGetTypedWithPageLinkAndInternalApiKey("/api/tenant/devices?", PAGE_DATA_DEVICE_TYPE_REF, pageLink, tenantAdminUserId);

        Device device = constructDevice("Read permissions for device");

        doPostWithApiKey("/api/device", device, tenantAdminUserId)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testInternalApiKeyWithTenantAdminPermissions_allPermissionsForDevice() throws Exception {
        loginSysAdmin();

        Map<Resource, Set<Operation>> tenantAdminPermissions = new HashMap<>();
        tenantAdminPermissions.put(Resource.DEVICE, Set.of(Operation.ALL));

        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.TENANT_ADMIN, tenantAdminPermissions);

        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);

        var apiKeyInfo = constructApiKeyInfo(TenantId.SYS_TENANT_ID, currentUserId, true, authorityPermissionsInfo);
        ApiKey internalApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
        Assert.assertNotNull(internalApiKey);
        Assert.assertTrue(internalApiKey.isInternal());

        setApiKey(internalApiKey.getValue());

        PageLink pageLink = new PageLink(15, 0);
        doGetTypedWithPageLinkAndInternalApiKey("/api/tenant/devices?", PAGE_DATA_DEVICE_TYPE_REF, pageLink, tenantAdminUserId);

        Device device = constructDevice("All permissions for device");
        Device savedDevice = doPostWithApiKey("/api/device", device, Device.class, tenantAdminUserId);

        doDeleteWithInternalApiKey("/api/device/" + savedDevice.getId().getId().toString(), tenantAdminUserId)
                .andExpect(status().isOk());
    }

    @Test
    public void testInternalApiKeyWithTenantAdminPermissions_noPermissionsForDevice() throws Exception {
        loginSysAdmin();

        Map<Resource, Set<Operation>> tenantAdminPermissions = new HashMap<>();
        tenantAdminPermissions.put(Resource.ASSET, Set.of(Operation.ALL));

        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.TENANT_ADMIN, tenantAdminPermissions);

        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);

        var apiKeyInfo = constructApiKeyInfo(TenantId.SYS_TENANT_ID, currentUserId, true, authorityPermissionsInfo);
        ApiKey internalApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
        Assert.assertNotNull(internalApiKey);
        Assert.assertTrue(internalApiKey.isInternal());

        setApiKey(internalApiKey.getValue());

        doGetWithInternalApiKey("/api/tenant/devices?deviceName=" + "Test", tenantAdminUserId)
                .andExpect(status().isForbidden());

        Device device = constructDevice("No permissions for device");
        doPostWithApiKey("/api/device", device, tenantAdminUserId)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testInternalApiKeyWithNoPermission_useApiKeyUserPermissionsAsDefault() throws Exception {
        loginSysAdmin();

        var apiKeyInfo = constructApiKeyInfo(TenantId.SYS_TENANT_ID, currentUserId, true, null);
        ApiKey internalApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
        Assert.assertNotNull(internalApiKey);
        Assert.assertTrue(internalApiKey.isInternal());

        setApiKey(internalApiKey.getValue());

        doGetTypedWithPageLinkAndInternalApiKey("/api/domain/infos?", PAGE_DATA_DOMAIN_TYPE_REF, new PageLink(10, 0), null);
    }

    @Test
    public void testSubCustomerUserGroupPermissionsPreservedWithInternalApiKey() throws Exception {
        // Verify the fix for: sub-customer user with group access to a parent customer's device group
        // (granted via group role assignment) should see that group both via JWT and via internal API key.
        // Before the fix, API key auth used empty groupPermissions, so the shared group was invisible.

        loginTenantAdmin();

        // Create a device group owned by the parent customer
        EntityGroup parentDeviceGroup = new EntityGroup();
        parentDeviceGroup.setName("Parent Customer Device Group");
        parentDeviceGroup.setType(EntityType.DEVICE);
        parentDeviceGroup.setOwnerId(customerId);
        parentDeviceGroup = doPost("/api/entityGroup", parentDeviceGroup, EntityGroup.class);

        // Create a GROUP role with READ operation
        Role groupRole = new Role();
        groupRole.setTenantId(tenantId);
        groupRole.setName("Read Device Group Role");
        groupRole.setType(RoleType.GROUP);
        ArrayNode readOps = JacksonUtil.newArrayNode();
        readOps.add(Operation.READ.name());
        groupRole.setPermissions(readOps);
        groupRole = doPost("/api/role", groupRole, Role.class);

        // Share the parent customer's device group with the sub-customer's "All" users group
        EntityGroupInfo subCustomerAllUsersGroup = findGroupByOwnerIdTypeAndName(
                subCustomerId, EntityType.USER, EntityGroup.GROUP_ALL_NAME
        );
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setUserGroupId(subCustomerAllUsersGroup.getId());
        groupPermission.setRoleId(groupRole.getId());
        groupPermission.setEntityGroupId(parentDeviceGroup.getId());
        groupPermission.setEntityGroupType(EntityType.DEVICE);
        GroupPermission savedGroupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        // Create an internal API key with restricted customer user permissions (only DEVICE READ — no DEVICE_GROUP generic read)
        loginSysAdmin();
        Map<Resource, Set<Operation>> customerUserPermissions = new HashMap<>();
        customerUserPermissions.put(Resource.DEVICE, Set.of(Operation.READ));
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.CUSTOMER_USER, customerUserPermissions);
        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);
        ApiKey internalApiKey = apiKeyService.saveApiKey(
                TenantId.SYS_TENANT_ID,
                constructApiKeyInfo(TenantId.SYS_TENANT_ID, currentUserId, true, authorityPermissionsInfo)
        );
        setApiKey(internalApiKey.getValue());

        final EntityGroup finalParentDeviceGroup = parentDeviceGroup;
        final Role finalGroupRole = groupRole;

        try {
            TypeReference<List<EntityGroupInfo>> groupListType = new TypeReference<>() {
            };

            // Via JWT: sub-customer user must see the shared parent device group
            loginSubCustomerAdminUser();
            List<EntityGroupInfo> groupsViaJwt = doGetTyped("/api/entityGroups/DEVICE", groupListType);
            Assert.assertTrue(
                    "Sub-customer user must see parent's device group via JWT",
                    groupsViaJwt.stream().anyMatch(g -> g.getId().equals(finalParentDeviceGroup.getId()))
            );

            // Via internal API key: sub-customer user must also see the same shared device group
            List<EntityGroupInfo> groupsViaApiKey = readResponse(
                    doGetWithInternalApiKey(
                            "/api/entityGroups/DEVICE", subCustomerAdminUserId)
                            .andExpect(status().isOk()
                            ),
                    groupListType
            );
            Assert.assertTrue(
                    "Sub-customer user must see parent's device group via internal API key",
                    groupsViaApiKey.stream().anyMatch(g -> g.getId().equals(finalParentDeviceGroup.getId()))
            );
        } finally {
            quietly("loginTenantAdmin", this::loginTenantAdmin);
            quietly("delete groupPermission", () -> doDelete("/api/groupPermission/" + savedGroupPermission.getUuidId()).andExpect(status().isOk()));
            quietly("delete role", () -> doDelete("/api/role/" + finalGroupRole.getUuidId()).andExpect(status().isOk()));
            quietly("delete entityGroup", () -> doDelete("/api/entityGroup/" + finalParentDeviceGroup.getUuidId()).andExpect(status().isOk()));
            quietly("delete internalApiKey", () -> apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, internalApiKey, true));
            resetApiKey();
        }
    }

    @Test
    public void testApiKeyCeilsGroupPermissions_dashboardGroupInvisibleWhenKeyOnlyGrantsDeviceRead() throws Exception {
        // Ceiling semantics: user has a GROUP role (READ) on a DASHBOARD group shared by the parent customer,
        // so via JWT the sub-customer can see that dashboard group.
        // But the internal API key only grants DEVICE:READ → DASHBOARD is not in the ceiling at all.
        // ceilGroupPermissions must drop the dashboard group entirely, so it must not appear via the API key.

        loginTenantAdmin();

        // Create a dashboard group owned by the parent customer
        EntityGroup parentDashboardGroup = new EntityGroup();
        parentDashboardGroup.setName("Parent Customer Dashboard Group");
        parentDashboardGroup.setType(EntityType.DASHBOARD);
        parentDashboardGroup.setOwnerId(customerId);
        parentDashboardGroup = doPost("/api/entityGroup", parentDashboardGroup, EntityGroup.class);

        // GROUP role with READ on the dashboard group (needed for listing; ceiling test is about resource type, not operation)
        Role dashboardRole = new Role();
        dashboardRole.setTenantId(tenantId);
        dashboardRole.setName("Read Dashboard Group Role");
        dashboardRole.setType(RoleType.GROUP);
        ArrayNode readOps = JacksonUtil.newArrayNode();
        readOps.add(Operation.READ.name());
        dashboardRole.setPermissions(readOps);
        dashboardRole = doPost("/api/role", dashboardRole, Role.class);

        // Share the parent customer's dashboard group with the sub-customer's "All" users group
        EntityGroupInfo subCustomerAllUsersGroup = findGroupByOwnerIdTypeAndName(
                subCustomerId, EntityType.USER, EntityGroup.GROUP_ALL_NAME
        );
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setUserGroupId(subCustomerAllUsersGroup.getId());
        groupPermission.setRoleId(dashboardRole.getId());
        groupPermission.setEntityGroupId(parentDashboardGroup.getId());
        groupPermission.setEntityGroupType(EntityType.DASHBOARD);
        GroupPermission savedGroupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        // Internal API key: DEVICE:READ only — DASHBOARD is intentionally absent from the ceiling
        loginSysAdmin();
        Map<Resource, Set<Operation>> customerUserPermissions = new HashMap<>();
        customerUserPermissions.put(Resource.DEVICE, Set.of(Operation.READ));
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.CUSTOMER_USER, customerUserPermissions);
        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);
        ApiKey internalApiKey = apiKeyService.saveApiKey(
                TenantId.SYS_TENANT_ID,
                constructApiKeyInfo(TenantId.SYS_TENANT_ID, currentUserId, true, authorityPermissionsInfo)
        );
        setApiKey(internalApiKey.getValue());

        final EntityGroup finalParentDashboardGroup = parentDashboardGroup;
        final Role finalDashboardRole = dashboardRole;

        try {
            TypeReference<List<EntityGroupInfo>> groupListType = new TypeReference<>() {
            };

            // Via JWT: sub-customer user must see the shared dashboard group (positive control)
            loginSubCustomerAdminUser();
            List<EntityGroupInfo> dashboardGroupsViaJwt = doGetTyped("/api/entityGroups/DASHBOARD", groupListType);
            Assert.assertTrue(
                    "Sub-customer user must see parent's dashboard group via JWT",
                    dashboardGroupsViaJwt.stream().anyMatch(g -> g.getId().equals(finalParentDashboardGroup.getId()))
            );

            // Via internal API key: dashboard group must NOT appear — DASHBOARD is outside the API key ceiling
            List<EntityGroupInfo> dashboardGroupsViaApiKey = readResponse(
                    doGetWithInternalApiKey("/api/entityGroups/DASHBOARD", subCustomerAdminUserId)
                            .andExpect(status().isOk()),
                    groupListType
            );
            Assert.assertFalse(
                    "API key ceiling must block DASHBOARD group when key only grants DEVICE:READ",
                    dashboardGroupsViaApiKey.stream().anyMatch(g -> g.getId().equals(finalParentDashboardGroup.getId()))
            );
        } finally {
            quietly("loginTenantAdmin", this::loginTenantAdmin);
            quietly("delete groupPermission", () -> doDelete("/api/groupPermission/" + savedGroupPermission.getUuidId()).andExpect(status().isOk()));
            quietly("delete role", () -> doDelete("/api/role/" + finalDashboardRole.getUuidId()).andExpect(status().isOk()));
            quietly("delete entityGroup", () -> doDelete("/api/entityGroup/" + finalParentDashboardGroup.getUuidId()).andExpect(status().isOk()));
            quietly("delete internalApiKey", () -> apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, internalApiKey, true));
            resetApiKey();
        }
    }

    // The group permission tests rely on the sub-customer admin being a member of the "All" users group.
    // This test makes that assumption explicit so that refactors of user-to-group wiring fail loudly here.
    @Test
    public void testSubCustomerAdminIsImplicitlyInAllUsersGroup() throws Exception {
        loginTenantAdmin();
        EntityGroupInfo subCustomerAllUsersGroup = findGroupByOwnerIdTypeAndName(
                subCustomerId, EntityType.USER, EntityGroup.GROUP_ALL_NAME
        );
        Assert.assertNotNull("Sub-customer's All users group must exist", subCustomerAllUsersGroup);
        Assert.assertTrue(
                "Sub-customer admin must be a member of the All users group",
                entityGroupService.isEntityInGroup(tenantId, subCustomerAdminUserId, subCustomerAllUsersGroup.getId())
        );
    }

    // Verifies that the API key ceiling blocks access to individual dashboard entities.
    // Dashboard IN shared group: accessible via JWT but blocked (403) via API key — ceiling removes the group permission.
    // Dashboard NOT in any shared group: also blocked (403) via API key — no permission path exists at all.
    @Test
    public void testApiKeyCeiling_dashboardEntityAccessBlockedViaApiKey() throws Exception {
        loginTenantAdmin();

        // Create a dashboard group owned by the parent customer and share it with the sub-customer
        EntityGroup parentDashboardGroup = new EntityGroup();
        parentDashboardGroup.setName("Parent Customer Dashboard Group For Entity Test");
        parentDashboardGroup.setType(EntityType.DASHBOARD);
        parentDashboardGroup.setOwnerId(customerId);
        parentDashboardGroup = doPost("/api/entityGroup", parentDashboardGroup, EntityGroup.class);

        Role dashboardRole = new Role();
        dashboardRole.setTenantId(tenantId);
        dashboardRole.setName("Read Dashboard Group Role For Entity Test");
        dashboardRole.setType(RoleType.GROUP);
        ArrayNode readOps = JacksonUtil.newArrayNode();
        readOps.add(Operation.READ.name());
        dashboardRole.setPermissions(readOps);
        dashboardRole = doPost("/api/role", dashboardRole, Role.class);

        EntityGroupInfo subCustomerAllUsersGroup = findGroupByOwnerIdTypeAndName(
                subCustomerId, EntityType.USER, EntityGroup.GROUP_ALL_NAME
        );
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setUserGroupId(subCustomerAllUsersGroup.getId());
        groupPermission.setRoleId(dashboardRole.getId());
        groupPermission.setEntityGroupId(parentDashboardGroup.getId());
        groupPermission.setEntityGroupType(EntityType.DASHBOARD);
        GroupPermission savedGroupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        // Dashboard IN the shared group
        Dashboard dashboardInGroup = new Dashboard();
        dashboardInGroup.setTitle("Dashboard In Shared Group");
        dashboardInGroup = doPost(
                "/api/dashboard?entityGroupId=" + parentDashboardGroup.getUuidId(),
                dashboardInGroup, Dashboard.class
        );

        // Dashboard NOT in the shared group (stays in parent customer's default "All" group)
        Dashboard dashboardOutOfGroup = new Dashboard();
        dashboardOutOfGroup.setTitle("Dashboard Outside Shared Group");
        dashboardOutOfGroup = doPost("/api/dashboard", dashboardOutOfGroup, Dashboard.class);

        // Internal API key: DEVICE:READ only — no DASHBOARD in ceiling
        loginSysAdmin();
        Map<Resource, Set<Operation>> customerUserPermissions = new HashMap<>();
        customerUserPermissions.put(Resource.DEVICE, Set.of(Operation.READ));
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.CUSTOMER_USER, customerUserPermissions);
        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);
        ApiKey internalApiKey = apiKeyService.saveApiKey(
                TenantId.SYS_TENANT_ID,
                constructApiKeyInfo(TenantId.SYS_TENANT_ID, currentUserId, true, authorityPermissionsInfo)
        );
        setApiKey(internalApiKey.getValue());

        final Dashboard finalDashboardInGroup = dashboardInGroup;
        final Dashboard finalDashboardOutOfGroup = dashboardOutOfGroup;
        final Role finalDashboardRole = dashboardRole;
        final EntityGroup finalParentDashboardGroup = parentDashboardGroup;

        try {
            // Via JWT: sub-customer can read the dashboard that is in the shared group (positive control)
            loginSubCustomerAdminUser();
            doGet("/api/dashboard/" + finalDashboardInGroup.getId().getId(), Dashboard.class);

            // Via API key: dashboard IN shared group → 403 (ceiling drops dashboard group permission entirely)
            doGetWithInternalApiKey("/api/dashboard/" + finalDashboardInGroup.getId().getId(), subCustomerAdminUserId)
                    .andExpect(status().isForbidden());

            // Via API key: dashboard NOT in any shared group → 403 (no path to access at all)
            doGetWithInternalApiKey("/api/dashboard/" + finalDashboardOutOfGroup.getId().getId(), subCustomerAdminUserId)
                    .andExpect(status().isForbidden());
        } finally {
            quietly("loginTenantAdmin", this::loginTenantAdmin);
            quietly("delete dashboardInGroup", () -> doDelete("/api/dashboard/" + finalDashboardInGroup.getId().getId()).andExpect(status().isOk()));
            quietly("delete dashboardOutOfGroup", () -> doDelete("/api/dashboard/" + finalDashboardOutOfGroup.getId().getId()).andExpect(status().isOk()));
            quietly("delete groupPermission", () -> doDelete("/api/groupPermission/" + savedGroupPermission.getUuidId()).andExpect(status().isOk()));
            quietly("delete role", () -> doDelete("/api/role/" + finalDashboardRole.getUuidId()).andExpect(status().isOk()));
            quietly("delete entityGroup", () -> doDelete("/api/entityGroup/" + finalParentDashboardGroup.getUuidId()).andExpect(status().isOk()));
            quietly("delete internalApiKey", () -> apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, internalApiKey, true));
            resetApiKey();
        }
    }

    // Covers the new BadCredentialsException branch: if getMergedPermissions throws, authentication must fail with 401.
    @Test
    public void testInternalApiKeyAuth_failsWithUnauthorized_whenMergedPermissionsThrows() throws Exception {
        loginSysAdmin();

        Map<Resource, Set<Operation>> tenantAdminPermissions = new HashMap<>();
        tenantAdminPermissions.put(Resource.DEVICE, Set.of(Operation.READ));
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.TENANT_ADMIN, tenantAdminPermissions);
        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);
        ApiKey internalApiKey = apiKeyService.saveApiKey(
                TenantId.SYS_TENANT_ID,
                constructApiKeyInfo(TenantId.SYS_TENANT_ID, currentUserId, true, authorityPermissionsInfo)
        );
        setApiKey(internalApiKey.getValue());

        Mockito.doThrow(new RuntimeException("Simulated DB failure"))
                .when(userPermissionsService).getMergedPermissions(ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());

        try {
            doGetWithInternalApiKey("/api/entityGroups/DEVICE", tenantAdminUserId)
                    .andExpect(status().isUnauthorized());
        } finally {
            Mockito.reset(userPermissionsService);
            quietly("delete internalApiKey", () -> apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, internalApiKey, true));
            resetApiKey();
        }
    }

    private void quietly(String label, ThrowingRunnable r) {
        try {
            r.run();
        } catch (Exception e) {
            log.warn("cleanup: {} failed", label, e);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private ApiKeyInfo constructApiKeyInfo(TenantId tenantId, UserId userId, boolean internal, AuthorityPermissionsInfo permissions) {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setUserId(userId);
        apiKeyInfo.setPermissions(permissions);
        apiKeyInfo.setDescription("API key");
        apiKeyInfo.setTenantId(tenantId);
        apiKeyInfo.setInternal(internal);
        apiKeyInfo.setEnabled(true);
        return apiKeyInfo;
    }

    private Device constructDevice(String name) {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device.setTenantId(tenantId);
        return device;
    }

}
