// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EntityGroupControllerTest extends AbstractControllerTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    private static final String GROUP_MANAGER_PERMISSIONS =
            "{\"DEVICE_GROUP\":[\"READ\",\"WRITE\"],\"USER_GROUP\":[\"READ\",\"WRITE\"],\"GROUP_PERMISSION\":[\"CREATE\"],\"ROLE\":[\"READ\"]}";

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
    public void testShouldNotChangeEntityGroupOwnerAsGroupOwner() throws Exception {
        loginTenantAdmin();
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Device Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        savedEntityGroup.setOwnerId(customerId);
        doPost("/api/entityGroup", savedEntityGroup)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Entity group owner can't be changed!")));

        assertThat(entityGroupService.findEntityGroupById(tenantId, savedEntityGroup.getId()).getOwnerId()).isEqualTo(tenantId);
    }

    @Test
    public void testShouldNotChangeEntityGroupType() throws Exception {
        loginTenantAdmin();
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Device Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        savedEntityGroup.setType(EntityType.USER);
        doPost("/api/entityGroup", savedEntityGroup)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Entity group type can't be changed!")));

        assertThat(entityGroupService.findEntityGroupById(tenantId, savedEntityGroup.getId()).getType()).isEqualTo(EntityType.DEVICE);
    }

    @Test
    public void testShouldNotMoveTenantEntityGroupToCustomerAsCustomerAdmin() throws Exception {
        EntityGroup tenantAdministratorsGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId,
                EntityType.USER, EntityGroup.GROUP_TENANT_ADMINS_NAME).get();

        loginCustomerAdminUser();
        EntityGroup movedEntityGroup = new EntityGroup(tenantAdministratorsGroup);
        movedEntityGroup.setOwnerId(customerId);
        doPost("/api/entityGroup", movedEntityGroup)
                .andExpect(status().isForbidden());

        assertThat(entityGroupService.findEntityGroupById(tenantId, tenantAdministratorsGroup.getId()).getOwnerId()).isEqualTo(tenantId);
    }

    @Test
    public void testShouldNotMoveEntityGroupSharedWithWriteAccess() throws Exception {
        loginTenantAdmin();
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Shared Device Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        var shareGroupRequest = new ShareGroupRequest(customerId, true, null, false, Collections.emptyList());
        doPost("/api/entityGroup/{entityGroupId}/share", shareGroupRequest, savedEntityGroup.getId().toString())
                .andExpect(status().isOk());

        loginCustomerAdminUser();
        EntityGroup sharedEntityGroup = doGet("/api/entityGroup/" + savedEntityGroup.getId().getId(), EntityGroup.class);
        sharedEntityGroup.setOwnerId(customerId);
        doPost("/api/entityGroup", sharedEntityGroup)
                .andExpect(status().isForbidden());

        assertThat(entityGroupService.findEntityGroupById(tenantId, savedEntityGroup.getId()).getOwnerId()).isEqualTo(tenantId);
    }

    @Test
    public void testShouldNotMoveOtherTenantEntityGroup() throws Exception {
        loginDifferentTenant();
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Other Tenant Device Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup otherTenantEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        loginCustomerAdminUser();
        otherTenantEntityGroup.setOwnerId(customerId);
        doPost("/api/entityGroup", otherTenantEntityGroup)
                .andExpect(status().isNotFound());

        assertThat(entityGroupService.findEntityGroupById(differentTenantId, otherTenantEntityGroup.getId()).getOwnerId()).isEqualTo(differentTenantId);
    }

    @Test
    public void testGetEntityGroupInfosByIdsReturnsOnlyOwnTenantGroups() throws Exception {
        loginTenantAdmin();
        EntityGroup ownGroup = new EntityGroup();
        ownGroup.setName("Own Device Group");
        ownGroup.setType(EntityType.DEVICE);
        ownGroup = doPost("/api/entityGroup", ownGroup, EntityGroup.class);

        loginDifferentTenant();
        EntityGroup otherTenantGroup = new EntityGroup();
        otherTenantGroup.setName("Other Tenant Device Group");
        otherTenantGroup.setType(EntityType.DEVICE);
        otherTenantGroup = doPost("/api/entityGroup", otherTenantGroup, EntityGroup.class);

        loginTenantAdmin();
        List<EntityInfo> entityInfos = doGetTyped("/api/entityGroupInfos?entityGroupIds=" + ownGroup.getId().getId() + "," + otherTenantGroup.getId().getId(),
                new TypeReference<>() {});
        assertThat(entityInfos).extracting(EntityInfo::getId).containsExactly(ownGroup.getId());
    }

    @Test
    public void testGetEntityGroupInfosByIdsReturnsOnlyReadableGroups() throws Exception {
        loginTenantAdmin();
        EntityGroup tenantGroup = new EntityGroup();
        tenantGroup.setName("Tenant Device Group");
        tenantGroup.setType(EntityType.DEVICE);
        tenantGroup = doPost("/api/entityGroup", tenantGroup, EntityGroup.class);

        loginCustomerAdminUser();
        EntityGroup customerGroup = new EntityGroup();
        customerGroup.setName("Customer Device Group");
        customerGroup.setType(EntityType.DEVICE);
        customerGroup.setOwnerId(customerId);
        customerGroup = doPost("/api/entityGroup", customerGroup, EntityGroup.class);

        List<EntityInfo> entityInfos = doGetTyped("/api/entityGroupInfos?entityGroupIds=" + customerGroup.getId().getId() + "," + tenantGroup.getId().getId(),
                new TypeReference<>() {});
        assertThat(entityInfos).extracting(EntityInfo::getId).containsExactly(customerGroup.getId());
    }

    @Test
    public void testShareGroupWithUserGroupOfOtherTenantReturnsNotFound() throws Exception {
        loginDifferentTenant();
        EntityGroup otherTenantUserGroup = new EntityGroup();
        otherTenantUserGroup.setName("Other Tenant User Group");
        otherTenantUserGroup.setType(EntityType.USER);
        otherTenantUserGroup = doPost("/api/entityGroup", otherTenantUserGroup, EntityGroup.class);

        loginTenantAdmin();
        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Shared Device Group");
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup = doPost("/api/entityGroup", deviceGroup, EntityGroup.class);

        ShareGroupRequest shareGroupRequest = new ShareGroupRequest(tenantId, false, otherTenantUserGroup.getId(), true, null);
        doPost("/api/entityGroup/" + deviceGroup.getId().getId() + "/share", shareGroupRequest)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(not(containsString(otherTenantUserGroup.getName()))));
    }

    @Test
    public void testShareGroupV2WithUserGroupOfOtherTenantReturnsNotFound() throws Exception {
        loginDifferentTenant();
        EntityGroup otherTenantUserGroup = new EntityGroup();
        otherTenantUserGroup.setName("Other Tenant User Group");
        otherTenantUserGroup.setType(EntityType.USER);
        otherTenantUserGroup = doPost("/api/entityGroup", otherTenantUserGroup, EntityGroup.class);

        loginTenantAdmin();
        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Shared Device Group");
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup = doPost("/api/entityGroup", deviceGroup, EntityGroup.class);

        ShareGroupRequest shareGroupRequest = new ShareGroupRequest(tenantId, false, otherTenantUserGroup.getId(), true, null);
        doPost("/api/v2/entityGroup/" + deviceGroup.getId().getId() + "/share", shareGroupRequest)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(not(containsString(otherTenantUserGroup.getName()))));
    }

    @Test
    public void testShareGroupWithAllUsersGroupOfOtherTenantReturnsNotFound() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();
        EntityGroup deviceGroup = createDeviceGroup("Shared Device Group", tenantId);

        ShareGroupRequest unknownOwnerRequest = new ShareGroupRequest(TenantId.fromUUID(UUID.randomUUID()), true, null, true, null);
        doPost("/api/entityGroup/{entityGroupId}/share", unknownOwnerRequest, deviceGroup.getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(is(msgErrorNotFound)));
        ShareGroupRequest otherTenantRequest = new ShareGroupRequest(differentTenantId, true, null, true, null);
        doPost("/api/entityGroup/{entityGroupId}/share", otherTenantRequest, deviceGroup.getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(is(msgErrorNotFound)));
        assertThat(findSharedUserGroupIds(deviceGroup)).isEmpty();
    }

    @Test
    public void testShareGroupWithAllUsersGroupOfOwnCustomerSharesWithThatGroup() throws Exception {
        loginTenantAdmin();
        EntityGroup deviceGroup = createDeviceGroup("Shared Device Group", tenantId);
        EntityGroup customerAllUsersGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, customerId,
                EntityType.USER, EntityGroup.GROUP_ALL_NAME).get();

        ShareGroupRequest shareGroupRequest = new ShareGroupRequest(customerId, true, null, true, null);
        doPost("/api/entityGroup/{entityGroupId}/share", shareGroupRequest, deviceGroup.getId().toString())
                .andExpect(status().isOk());
        assertThat(findSharedUserGroupIds(deviceGroup)).containsExactly(customerAllUsersGroup.getId());
    }

    @Test
    public void testShareGroupV2WithAllUsersGroupOfOtherTenantReturnsNotFound() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();
        EntityGroup deviceGroup = createDeviceGroup("Shared Device Group", tenantId);

        ShareGroupRequest unknownOwnerRequest = new ShareGroupRequest(TenantId.fromUUID(UUID.randomUUID()), true, null, true, null);
        doPost("/api/v2/entityGroup/{entityGroupId}/share", unknownOwnerRequest, deviceGroup.getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(is(msgErrorNotFound)));
        ShareGroupRequest otherTenantRequest = new ShareGroupRequest(differentTenantId, true, null, true, null);
        doPost("/api/v2/entityGroup/{entityGroupId}/share", otherTenantRequest, deviceGroup.getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(is(msgErrorNotFound)));
        assertThat(findSharedUserGroupIds(deviceGroup)).isEmpty();
    }

    @Test
    public void testShareGroupV2WithAllUsersGroupOfOwnCustomerSharesWithThatGroup() throws Exception {
        loginTenantAdmin();
        EntityGroup deviceGroup = createDeviceGroup("Shared Device Group", tenantId);
        EntityGroup customerAllUsersGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, customerId,
                EntityType.USER, EntityGroup.GROUP_ALL_NAME).get();

        ShareGroupRequest shareGroupRequest = new ShareGroupRequest(customerId, true, null, true, null);
        doPost("/api/v2/entityGroup/{entityGroupId}/share", shareGroupRequest, deviceGroup.getId().toString())
                .andExpect(status().isOk());
        assertThat(findSharedUserGroupIds(deviceGroup)).containsExactly(customerAllUsersGroup.getId());
    }

    @Test
    public void testShareGroupToChildOwnerUserGroupWithGroupsOfOtherTenantReturnsNotFound() throws Exception {
        loginDifferentTenant();
        EntityGroup otherTenantUserGroup = new EntityGroup();
        otherTenantUserGroup.setName("Other Tenant User Group");
        otherTenantUserGroup.setType(EntityType.USER);
        otherTenantUserGroup = doPost("/api/entityGroup", otherTenantUserGroup, EntityGroup.class);
        EntityGroup otherTenantDeviceGroup = new EntityGroup();
        otherTenantDeviceGroup.setName("Other Tenant Device Group");
        otherTenantDeviceGroup.setType(EntityType.DEVICE);
        otherTenantDeviceGroup = doPost("/api/entityGroup", otherTenantDeviceGroup, EntityGroup.class);

        loginTenantAdmin();
        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Shared Device Group");
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup = doPost("/api/entityGroup", deviceGroup, EntityGroup.class);
        EntityGroup userGroup = new EntityGroup();
        userGroup.setName("Own User Group");
        userGroup.setType(EntityType.USER);
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);
        Role role = new Role();
        role.setName("Read Group Role");
        role.setType(RoleType.GROUP);
        role.setPermissions(JacksonUtil.valueToTree(List.of(Operation.READ)));
        role = doPost("/api/role", role, Role.class);

        doPost("/api/entityGroup/" + deviceGroup.getId().getId() + "/" + otherTenantUserGroup.getId().getId() + "/" + role.getId().getId() + "/share")
                .andExpect(status().isNotFound());
        doPost("/api/entityGroup/" + otherTenantDeviceGroup.getId().getId() + "/" + userGroup.getId().getId() + "/" + role.getId().getId() + "/share")
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAttributesOfEntityGroupOfOtherTenantReturnsNotFound() throws Exception {
        loginDifferentTenant();
        EntityGroup otherTenantGroup = new EntityGroup();
        otherTenantGroup.setName("Other Tenant Asset Group");
        otherTenantGroup.setType(EntityType.ASSET);
        otherTenantGroup = doPost("/api/entityGroup", otherTenantGroup, EntityGroup.class);

        loginTenantAdmin();
        doGetAsync("/api/plugins/telemetry/ENTITY_GROUP/" + otherTenantGroup.getId().getId() + "/values/attributes")
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetEntityGroupOfOtherTenantReturnsNotFound() throws Exception {
        loginDifferentTenant();
        EntityGroup otherTenantGroup = new EntityGroup();
        otherTenantGroup.setName("Other Tenant Asset Group");
        otherTenantGroup.setType(EntityType.ASSET);
        otherTenantGroup = doPost("/api/entityGroup", otherTenantGroup, EntityGroup.class);

        loginTenantAdmin();
        doGet("/api/entityGroup/" + otherTenantGroup.getId().getId())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(not(containsString(otherTenantGroup.getName()))));
    }

    @Test
    public void testShouldNotUpdateNonExistingEntityGroup() throws Exception {
        loginTenantAdmin();
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setId(new EntityGroupId(UUID.randomUUID()));
        entityGroup.setName("Non-existing Device Group");
        entityGroup.setType(EntityType.DEVICE);
        entityGroup.setOwnerId(tenantId);
        doPost("/api/entityGroup", entityGroup)
                .andExpect(status().isNotFound());
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

    @Test
    public void testShareEntityGroupWithGroupRole() throws Exception {
        loginTenantAdmin();
        EntityGroup deviceGroup = createDeviceGroup("Device Group", tenantId);
        Role groupRole = createRole(tenantId, null, "Read role", RoleType.GROUP, "[\"READ\"]");

        shareWithRoles(deviceGroup, customerId, groupRole.getId())
                .andExpect(status().isOk());

        assertThat(findSharedRoleIds(deviceGroup)).containsExactly(groupRole.getId());
    }

    @Test
    public void testShareEntityGroupRejectsGenericRole() throws Exception {
        loginTenantAdmin();
        EntityGroup deviceGroup = createDeviceGroup("Device Group", tenantId);
        Role genericRole = createRole(tenantId, null, "Generic role", RoleType.GENERIC, "{\"DEVICE\":[\"READ\"]}");

        shareWithRoles(deviceGroup, customerId, genericRole.getId())
                .andExpect(status().isBadRequest());

        assertThat(findSharedRoleIds(deviceGroup)).isEmpty();
    }

    @Test
    public void testShareEntityGroupRequiresReadableRole() throws Exception {
        createDifferentTenant();
        Role otherTenantRole = createRole(differentTenantId, null, "Read role", RoleType.GROUP, "[\"READ\"]");

        loginTenantAdmin();
        EntityGroup deviceGroup = createDeviceGroup("Device Group", tenantId);

        shareWithRoles(deviceGroup, customerId, otherTenantRole.getId())
                .andExpect(status().isForbidden());

        assertThat(findSharedRoleIds(deviceGroup)).isEmpty();
    }

    @Test
    public void testShareEntityGroupSavesNothingWhenAnyRoleRejected() throws Exception {
        loginTenantAdmin();
        EntityGroup deviceGroup = createDeviceGroup("Device Group", tenantId);
        Role groupRole = createRole(tenantId, null, "Read role", RoleType.GROUP, "[\"READ\"]");
        Role genericRole = createRole(tenantId, null, "Generic role", RoleType.GENERIC, "{\"DEVICE\":[\"READ\"]}");

        shareWithRoles(deviceGroup, customerId, groupRole.getId(), genericRole.getId())
                .andExpect(status().isBadRequest());

        assertThat(findSharedRoleIds(deviceGroup)).isEmpty();
    }

    @Test
    public void testShareEntityGroupRequiresRoleOperationsHeldByUser() throws Exception {
        loginTenantAdmin();
        User manager = createGroupManager(createCustomerUserGroup(), GROUP_MANAGER_PERMISSIONS);
        EntityGroup deviceGroup = createDeviceGroup("Customer Device Group", customerId);
        Role readRole = createRole(tenantId, customerId, "Read role", RoleType.GROUP, "[\"READ\"]");
        Role rpcRole = createRole(tenantId, customerId, "Read and RPC role", RoleType.GROUP, "[\"READ\",\"RPC_CALL\"]");

        login(manager.getEmail(), "manager");
        shareWithRoles(deviceGroup, customerId, rpcRole.getId())
                .andExpect(status().isForbidden());
        assertThat(findSharedRoleIds(deviceGroup)).isEmpty();

        shareWithRoles(deviceGroup, customerId, readRole.getId())
                .andExpect(status().isOk());
        assertThat(findSharedRoleIds(deviceGroup)).containsExactly(readRole.getId());
    }

    @Test
    public void testShareEntityGroupAllowsRoleOperationsHeldOnEntities() throws Exception {
        loginTenantAdmin();
        User manager = createGroupManager(createCustomerUserGroup(),
                "{\"DEVICE_GROUP\":[\"READ\",\"WRITE\"],\"DEVICE\":[\"RPC_CALL\"],\"USER_GROUP\":[\"READ\",\"WRITE\"],\"GROUP_PERMISSION\":[\"CREATE\"],\"ROLE\":[\"READ\"]}");
        EntityGroup deviceGroup = createDeviceGroup("Customer Device Group", customerId);
        Role rpcRole = createRole(tenantId, customerId, "Read and RPC role", RoleType.GROUP, "[\"READ\",\"RPC_CALL\"]");

        login(manager.getEmail(), "manager");
        shareWithRoles(deviceGroup, customerId, rpcRole.getId())
                .andExpect(status().isOk());
        assertThat(findSharedRoleIds(deviceGroup)).containsExactly(rpcRole.getId());
    }

    @Test
    public void testShareEntityGroupAllowsRoleOperationsHeldOnSharedGroup() throws Exception {
        loginTenantAdmin();
        EntityGroup managers = createCustomerUserGroup();
        User manager = createGroupManager(managers, GROUP_MANAGER_PERMISSIONS);
        EntityGroup deviceGroup = createDeviceGroup("Customer Device Group", customerId);
        Role groupRpcRole = createRole(tenantId, customerId, "RPC role", RoleType.GROUP, "[\"RPC_CALL\"]");
        GroupPermission groupRpcPermission = new GroupPermission();
        groupRpcPermission.setRoleId(groupRpcRole.getId());
        groupRpcPermission.setUserGroupId(managers.getId());
        groupRpcPermission.setEntityGroupId(deviceGroup.getId());
        groupRpcPermission.setEntityGroupType(deviceGroup.getType());
        doPost("/api/groupPermission", groupRpcPermission, GroupPermission.class);
        Role rpcRole = createRole(tenantId, customerId, "Read and RPC role", RoleType.GROUP, "[\"READ\",\"RPC_CALL\"]");

        login(manager.getEmail(), "manager");
        shareWithRoles(deviceGroup, customerId, rpcRole.getId())
                .andExpect(status().isOk());
        assertThat(findSharedRoleIds(deviceGroup)).contains(rpcRole.getId());
    }

    private EntityGroup createCustomerUserGroup() throws Exception {
        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setName("Group managers");
        userGroup.setOwnerId(customerId);
        return doPost("/api/entityGroup", userGroup, EntityGroup.class);
    }

    private User createGroupManager(EntityGroup userGroup, String permissions) throws Exception {
        Role managerRole = createRole(tenantId, customerId, "Group manager", RoleType.GENERIC, permissions);
        GroupPermission managerPermission = new GroupPermission();
        managerPermission.setRoleId(managerRole.getId());
        managerPermission.setUserGroupId(userGroup.getId());
        doPost("/api/groupPermission", managerPermission, GroupPermission.class);

        User manager = new User();
        manager.setAuthority(Authority.CUSTOMER_USER);
        manager.setTenantId(tenantId);
        manager.setCustomerId(customerId);
        manager.setEmail("groupmanager@thingsboard.org");
        return createUser(manager, "manager", userGroup.getId());
    }

    private EntityGroup createDeviceGroup(String name, EntityId ownerId) throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setType(EntityType.DEVICE);
        entityGroup.setName(name);
        entityGroup.setOwnerId(ownerId);
        return doPost("/api/entityGroup", entityGroup, EntityGroup.class);
    }

    private Role createRole(TenantId roleTenantId, CustomerId roleCustomerId, String name, RoleType type, String permissions) {
        Role role = new Role();
        role.setTenantId(roleTenantId);
        role.setCustomerId(roleCustomerId);
        role.setType(type);
        role.setName(name);
        role.setPermissions(JacksonUtil.toJsonNode(permissions));
        return roleService.saveRole(roleTenantId, role);
    }

    private ResultActions shareWithRoles(EntityGroup entityGroup, CustomerId targetCustomerId, RoleId... roleIds) throws Exception {
        var shareGroupRequest = new ShareGroupRequest(targetCustomerId, true, null, false, List.of(roleIds));
        return doPost("/api/entityGroup/{entityGroupId}/share", shareGroupRequest, entityGroup.getId().toString());
    }

    private List<RoleId> findSharedRoleIds(EntityGroup entityGroup) {
        return groupPermissionService.findGroupPermissionByTenantIdAndEntityGroupId(tenantId, entityGroup.getId(), new PageLink(100))
                .getData().stream().map(GroupPermission::getRoleId).collect(Collectors.toList());
    }

    private List<EntityGroupId> findSharedUserGroupIds(EntityGroup entityGroup) {
        return groupPermissionService.findGroupPermissionByTenantIdAndEntityGroupId(tenantId, entityGroup.getId(), new PageLink(100))
                .getData().stream().map(GroupPermission::getUserGroupId).collect(Collectors.toList());
    }

}
