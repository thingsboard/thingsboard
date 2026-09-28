// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EntityGroupControllerTest extends AbstractControllerTest {

    @Autowired
    private RoleService roleService;

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
    public void testSaveEntityGroup() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        Assert.assertNotNull(savedEntityGroup);
        Assert.assertNotNull(savedEntityGroup.getId());
        Assert.assertTrue(savedEntityGroup.getCreatedTime() > 0);
        Assert.assertEquals(entityGroup.getName(), savedEntityGroup.getName());
        savedEntityGroup.setName("New Entity Group");
        doPost("/api/entityGroup", savedEntityGroup, EntityGroup.class);
        EntityGroup foundEntityGroup = doGet("/api/entityGroup/" + savedEntityGroup.getId().getId().toString(), EntityGroup.class);
        Assert.assertEquals(savedEntityGroup.getName(), foundEntityGroup.getName());
    }

    @Test
    public void testSaveEntityGroupWithSameName() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        Assert.assertNotNull(savedEntityGroup);
        Assert.assertNotNull(savedEntityGroup.getId());
        EntityGroup entityGroup2 = new EntityGroup();
        entityGroup2.setName("Entity Group");
        entityGroup2.setType(EntityType.DEVICE);
        doPost("/api/entityGroup", entityGroup2).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Entity Group with such name, type and owner already exists!")));
    }

    @Test
    public void testFindEntityGroupById() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        EntityGroup foundEntityGroup = doGet("/api/entityGroup/" + savedEntityGroup.getId().getId().toString(), EntityGroup.class);
        Assert.assertNotNull(foundEntityGroup);
        Assert.assertEquals(savedEntityGroup, foundEntityGroup);
    }

    @Test
    public void testDeleteEntityGroup() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        doDelete("/api/entityGroup/" + savedEntityGroup.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/entityGroup/" + savedEntityGroup.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindEdgeEntityGroupsByTenantIdAndNameAndType() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        List<EntityGroupId> edgeEntityGroupIds = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            EntityGroup entityGroup = new EntityGroup();
            entityGroup.setType(EntityType.DEVICE);
            entityGroup.setName("Scheduler Event " + i);
            EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
            doPost("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/entityGroup/" + savedEntityGroup.getId().getId().toString() + "/DEVICE", EntityGroup.class);
            edgeEntityGroupIds.add(savedEntityGroup.getId());
        }

        List<EntityGroupId> loadedEdgeEntityGroupIds = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<EntityGroupInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/entityGroups/edge/" + savedEdge.getId().getId() + "/DEVICE?",
                    new TypeReference<>() {}, pageLink);
            loadedEdgeEntityGroupIds.addAll(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()));
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertTrue(edgeEntityGroupIds.size() == loadedEdgeEntityGroupIds.size() &&
                edgeEntityGroupIds.containsAll(loadedEdgeEntityGroupIds));

        for (EntityGroupId entityGroupId : loadedEdgeEntityGroupIds) {
            doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/entityGroup/" + entityGroupId.getId().toString() + "/DEVICE", EntityGroup.class);
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/entityGroups/edge/" + savedEdge.getId().getId() + "/DEVICE?",
                new TypeReference<>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getTotalElements());
    }

    @Test
    public void testShouldNotAddEntityToGroupWithOtherOwner() throws Exception {
        loginTenantAdmin();

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        Device device = new Device();
        device.setName(RandomStringUtils.randomAlphabetic(8));
        device.setType("default");
        device.setCustomerId(customerId);
        Device savedDevice = doPost("/api/device", device, Device.class);

        List<String> strEntityIds = new ArrayList<>();
        String deviceId = savedDevice.getId().getId().toString();
        strEntityIds.add(deviceId);
        doPost("/api/entityGroup/" + savedEntityGroup.getId() + "/addEntities", strEntityIds)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Unable to add entity with other owner than group. Entity id: " + deviceId)));
    }

    @Test
    public void testShouldNotDeleteTenantAdminGroupIfNoTenantAdminsLeft() throws Exception {
        loginTenantAdmin();

        EntityGroup tenantAdministratorsGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId,
                EntityType.USER, EntityGroup.GROUP_TENANT_ADMINS_NAME).get();

        doDelete("/api/entityGroup/" + tenantAdministratorsGroup.getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("At least one tenant administrator must remain!")));
    }

    @Test
    public void testShouldNotDeleteUserGroupOfAuthorizedUser() throws Exception {
        loginCustomerAdminUser();

        EntityGroup customerAdminGroups = entityGroupService.findEntityGroupByTypeAndName(tenantId, customerId,
                EntityType.USER, EntityGroup.GROUP_CUSTOMER_ADMINS_NAME).get();

        doDelete("/api/entityGroup/" + customerAdminGroups.getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Unable to remove the user group associated with the current user.")));
    }

    @Test
    public void testShouldNotDeleteLastTenantAdmin() throws Exception {
        loginTenantAdmin();

        EntityGroup tenantAdministratorsGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId,
                EntityType.USER, EntityGroup.GROUP_TENANT_ADMINS_NAME).get();

        List<String> strEntityIds = new ArrayList<>();
        strEntityIds.add(tenantAdminUser.getId().getId().toString());
        doPost("/api/entityGroup/" + tenantAdministratorsGroup.getId() + "/deleteEntities", strEntityIds)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("At least one tenant administrator must remain!")));
    }

    @Test
    public void testChangeEntityGroupPublicStatus() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Device group");
        entityGroup.setType(EntityType.DEVICE);
        entityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        doPost("/api/entityGroup/" + entityGroup.getUuidId() + "/makePublic").andExpect(status().isOk());

        GroupPermissionInfo groupPermission = doGetTyped("/api/entityGroup/" + entityGroup.getUuidId() + "/groupPermissions", new TypeReference<List<GroupPermissionInfo>>() {}).get(0);
        EntityGroup publicEntityGroup = doGet("/api/entityGroup/" + entityGroup.getUuidId(), EntityGroup.class);
        assertThat(publicEntityGroup.getAdditionalInfo().get("isPublic").asBoolean()).isTrue();
        assertThat(publicEntityGroup.getAdditionalInfo().get("publicCustomerId").asText()).isEqualTo(groupPermission.getUserGroupOwnerId().toString());

        assertThat(groupPermission.isPublic()).isTrue();
        assertThat(groupPermission.getEntityGroupId()).isEqualTo(publicEntityGroup.getId());
        assertThat(groupPermission.getEntityGroupType()).isEqualTo(publicEntityGroup.getType());

        doPost("/api/entityGroup/" + publicEntityGroup.getUuidId() + "/makePrivate").andExpect(status().isOk());

        EntityGroup privateEntityGroup = doGet("/api/entityGroup/" + entityGroup.getUuidId(), EntityGroup.class);
        assertThat(privateEntityGroup.getAdditionalInfo().get("isPublic").asBoolean()).isFalse();
        assertThat(privateEntityGroup.getAdditionalInfo().get("publicCustomerId").asText()).isEmpty();

        List<GroupPermissionInfo> groupPermissions = doGetTyped("/api/entityGroup/" + privateEntityGroup.getUuidId() + "/groupPermissions", new TypeReference<>() {});
        assertThat(groupPermissions).isEmpty();
    }

    @Test
    public void testReShareDashboardToChild() throws Exception {
        loginTenantAdmin();
        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setName("Dashboard Group");
        dashboardGroup.setOwnerId(tenantId);

        dashboardGroup = doPost("/api/entityGroup", dashboardGroup, EntityGroup.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Tenant Dashboard");
        dashboard = doPost("/api/dashboard?entityGroupId={entityGroupId}", dashboard, Dashboard.class, dashboardGroup.getId().getId().toString());

        //share group for customer
        var shareGroupRequest = new ShareGroupRequest(customerId, true, null, false, Collections.emptyList());
        doPost("/api/entityGroup/{entityGroupId}/share", shareGroupRequest, dashboardGroup.getId().toString());

        loginCustomerAdminUser();
        Dashboard foundDashboard = doGet("/api/dashboard/" + dashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertNotNull(foundDashboard);

        EntityGroup childAllUserGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, subCustomerId, EntityType.USER, "All").get();

        Role tenantRole = new Role();
        tenantRole.setTenantId(tenantId);
        tenantRole.setType(RoleType.GROUP);
        tenantRole.setName("Role to read dashboard");
        tenantRole.setPermissions(JacksonUtil.toJsonNode("[\"READ\"]"));
        tenantRole = roleService.saveRole(tenantId, tenantRole);

        //try to reshare to child customer with tenant role
        doPost("/api/entityGroup/" + dashboardGroup.getId().toString() + "/" + childAllUserGroup.getId().toString() + "/" + tenantRole.getId().toString() + "/share")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString("You don't have permission to perform 'READ' operation with ROLE '" + tenantRole.getName() + "'!")));

        Role customerRole = new Role();
        customerRole.setTenantId(tenantId);
        customerRole.setCustomerId(customerId);
        customerRole.setType(RoleType.GROUP);
        customerRole.setName("Role to read dashboard");
        customerRole.setPermissions(JacksonUtil.toJsonNode("[\"READ\"]"));
        customerRole = roleService.saveRole(tenantId, customerRole);

        // reshare with customer role
        doPost("/api/entityGroup/" + dashboardGroup.getId().toString() + "/" + childAllUserGroup.getId().toString() + "/" + customerRole.getId().toString() + "/share")
                .andExpect(status().isOk());

        loginSubCustomerAdminUser();
        Dashboard foundDashboard2 = doGet("/api/dashboard/" + dashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertNotNull(foundDashboard2);
    }

}
