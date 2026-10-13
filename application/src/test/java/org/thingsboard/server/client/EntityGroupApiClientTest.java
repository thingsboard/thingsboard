// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.AddEntitiesToEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.AssignEntityGroupToEdgeArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteEdgeArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteRoleArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllEdgeEntityGroupsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllEntityGroupsByOwnerAndTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllEntityGroupsByTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllSharedEntityGroupsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEdgeEntityGroupsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntitiesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupAllByOwnerAndTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupByOwnerAndNameAndTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupEntityInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupEntityInfosByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupEntityInfosByOwnerAndTypeAndPageLinkArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupEntityInfosByTypeAndPageLinkArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupEntityInfosHierarchyByOwnerAndTypeAndPageLinkArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupPermissionsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupsByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupsByOwnerAndTypeAndPageLinkArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupsByTypeAndPageLinkArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupsForEntityArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupsHierarchyByOwnerAndTypeAndPageLinkArgs;
import org.thingsboard.client.api.ThingsboardApi.GetGroupEntityArgs;
import org.thingsboard.client.api.ThingsboardApi.GetOwnerInfoArgs;
import org.thingsboard.client.api.ThingsboardApi.GetOwnerInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetOwnersArgs;
import org.thingsboard.client.api.ThingsboardApi.MakeEntityGroupPrivateArgs;
import org.thingsboard.client.api.ThingsboardApi.MakeEntityGroupPublicArgs;
import org.thingsboard.client.api.ThingsboardApi.RemoveEntitiesFromEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEdgeArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveRoleArgs;
import org.thingsboard.client.api.ThingsboardApi.ShareEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.ShareEntityGroupToChildOwnerUserGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.UnassignEntityGroupFromEdgeArgs;
import org.thingsboard.client.model.Edge;
import org.thingsboard.client.model.EntityGroup;
import org.thingsboard.client.model.EntityGroupId;
import org.thingsboard.client.model.EntityGroupInfo;
import org.thingsboard.client.model.EntityInfo;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.GroupPermissionInfo;
import org.thingsboard.client.model.PageDataContactBasedObject;
import org.thingsboard.client.model.PageDataEntityGroupInfo;
import org.thingsboard.client.model.PageDataEntityInfo;
import org.thingsboard.client.model.PageDataShortEntityView;
import org.thingsboard.client.model.Role;
import org.thingsboard.client.model.RoleType;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.client.model.ShareGroupRequest;
import org.thingsboard.client.model.ShortEntityView;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class EntityGroupApiClientTest extends AbstractApiClientTest {

    private static final String DEVICE_TYPE = "DEVICE";
    private static final String CUSTOMER_TYPE = "CUSTOMER";

    @Test
    public void testEntityGroupLifecycle() throws Exception {
        long ts = System.currentTimeMillis();
        String name = TEST_PREFIX + ts;

        EntityGroup entityGroup = buildGroup(name, DEVICE_TYPE);
        EntityGroupInfo created = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(entityGroup)
                .build());
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(name, created.getName());
        String groupId = created.getId().getId().toString();

        EntityGroupInfo fetched = client.getEntityGroupById(GetEntityGroupByIdArgs.builder()
                .entityGroupId(groupId)
                .build());
        assertEquals(groupId, fetched.getId().getId().toString());
        assertEquals(name, fetched.getName());

        EntityInfo entityInfo = client.getEntityGroupEntityInfoById(GetEntityGroupEntityInfoByIdArgs.builder()
                .entityGroupId(groupId)
                .build());
        assertNotNull(entityInfo);
        assertEquals(groupId, entityInfo.getId().getId().toString());

        EntityGroupInfo byName = client.getEntityGroupByOwnerAndNameAndType(GetEntityGroupByOwnerAndNameAndTypeArgs.builder()
                .ownerType("TENANT")
                .ownerId(tenantId())
                .groupType(DEVICE_TYPE)
                .groupName(name)
                .build());
        assertEquals(groupId, byName.getId().getId().toString());

        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .build());
        assertReturns404(() -> client.getEntityGroupById(GetEntityGroupByIdArgs.builder()
                .entityGroupId(groupId)
                .build()));
    }

    @Test
    public void testGetEntityGroupsByType() throws Exception {
        long ts = System.currentTimeMillis();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ids.add(client.saveEntityGroup(SaveEntityGroupArgs.builder()
                    .entityGroup(buildGroup(TEST_PREFIX + ts + "_" + i, DEVICE_TYPE))
                    .build())
                    .getId().getId().toString());
        }

        List<EntityGroupInfo> all = client.getAllEntityGroupsByType(GetAllEntityGroupsByTypeArgs.builder()
                .groupType(DEVICE_TYPE)
                .build());
        assertNotNull(all);
        assertTrue(all.size() >= 3);

        PageDataEntityGroupInfo page = client.getEntityGroupsByTypeAndPageLink(GetEntityGroupsByTypeAndPageLinkArgs.builder()
                .groupType(DEVICE_TYPE)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);

        PageDataEntityInfo infoPage = client.getEntityGroupEntityInfosByTypeAndPageLink(GetEntityGroupEntityInfosByTypeAndPageLinkArgs.builder()
                .groupType(DEVICE_TYPE)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(infoPage);
        assertTrue(infoPage.getTotalElements() >= 3);

        for (String id : ids) client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(id)
                .build());
    }

    @Test
    public void testGetEntityGroupsByOwner() throws Exception {
        long ts = System.currentTimeMillis();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ids.add(client.saveEntityGroup(SaveEntityGroupArgs.builder()
                    .entityGroup(buildGroup(TEST_PREFIX + ts + "_" + i, DEVICE_TYPE))
                    .build())
                    .getId().getId().toString());
        }

        List<EntityGroupInfo> all = client.getAllEntityGroupsByOwnerAndType(GetAllEntityGroupsByOwnerAndTypeArgs.builder()
                .ownerType("TENANT")
                .ownerId(tenantId())
                .groupType(DEVICE_TYPE)
                .build());
        assertNotNull(all);
        assertTrue(all.size() >= 3);

        PageDataEntityGroupInfo page = client.getEntityGroupsByOwnerAndTypeAndPageLink(GetEntityGroupsByOwnerAndTypeAndPageLinkArgs.builder()
                .ownerType("TENANT")
                .ownerId(tenantId())
                .groupType(DEVICE_TYPE)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);

        PageDataEntityInfo infoPage = client.getEntityGroupEntityInfosByOwnerAndTypeAndPageLink(GetEntityGroupEntityInfosByOwnerAndTypeAndPageLinkArgs.builder()
                .ownerType("TENANT")
                .ownerId(tenantId())
                .groupType(DEVICE_TYPE)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(infoPage);
        assertTrue(infoPage.getTotalElements() >= 3);

        EntityGroupInfo allGroup = client.getEntityGroupAllByOwnerAndType(GetEntityGroupAllByOwnerAndTypeArgs.builder()
                .ownerType("TENANT")
                .ownerId(tenantId())
                .groupType(DEVICE_TYPE)
                .build());
        assertNotNull(allGroup);
        assertTrue("Expected the 'All' group to have groupAll=true", allGroup.getGroupAll());

        for (String id : ids) client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(id)
                .build());
    }

    @Test
    public void testGetEntityGroupsHierarchy() throws Exception {
        long ts = System.currentTimeMillis();
        EntityGroupInfo group = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(buildGroup(TEST_PREFIX + ts, DEVICE_TYPE))
                .build());
        String groupId = group.getId().getId().toString();

        PageDataEntityGroupInfo hierarchy = client.getEntityGroupsHierarchyByOwnerAndTypeAndPageLink(GetEntityGroupsHierarchyByOwnerAndTypeAndPageLinkArgs.builder()
                .ownerType("TENANT")
                .ownerId(tenantId())
                .groupType(DEVICE_TYPE)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(hierarchy);
        assertNotNull(hierarchy.getData());
        assertTrue(hierarchy.getData().stream().anyMatch(g -> g.getId().getId().toString().equals(groupId)));

        PageDataEntityInfo infoHierarchy = client.getEntityGroupEntityInfosHierarchyByOwnerAndTypeAndPageLink(GetEntityGroupEntityInfosHierarchyByOwnerAndTypeAndPageLinkArgs.builder()
                .ownerType("TENANT")
                .ownerId(tenantId())
                .groupType(DEVICE_TYPE)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(infoHierarchy);
        assertNotNull(infoHierarchy.getData());
        assertTrue(infoHierarchy.getData().stream()
                .anyMatch(i -> i.getId().getId().toString().equals(groupId)));

        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .build());
    }

    @Test
    public void testEntityGroupMembership() throws Exception {
        long ts = System.currentTimeMillis();

        EntityGroupInfo group = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(buildGroup(TEST_PREFIX + ts, CUSTOMER_TYPE))
                .build());
        String groupId = group.getId().getId().toString();
        String memberCustomerId = customerId();

        client.addEntitiesToEntityGroup(AddEntitiesToEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .requestBody(List.of(memberCustomerId))
                .build());

        PageDataShortEntityView members = client.getEntities(GetEntitiesArgs.builder()
                .entityGroupId(groupId)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(members);
        assertEquals(1, members.getData().size());
        assertEquals(memberCustomerId, members.getData().get(0).getId().getId().toString());

        ShortEntityView member = client.getGroupEntity(GetGroupEntityArgs.builder()
                .entityGroupId(groupId)
                .entityId(memberCustomerId)
                .build());
        assertNotNull(member);
        assertEquals(memberCustomerId, member.getId().getId().toString());

        List<EntityGroupId> customerGroups = client.getEntityGroupsForEntity(GetEntityGroupsForEntityArgs.builder()
                .entityType("CUSTOMER")
                .entityId(memberCustomerId)
                .build());
        assertNotNull(customerGroups);
        assertTrue(customerGroups.stream().anyMatch(id -> id.getId().toString().equals(groupId)));

        client.removeEntitiesFromEntityGroup(RemoveEntitiesFromEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .requestBody(List.of(memberCustomerId))
                .build());
        PageDataShortEntityView afterRemove = client.getEntities(GetEntitiesArgs.builder()
                .entityGroupId(groupId)
                .pageSize("100")
                .page("0")
                .build());
        assertEquals(0, afterRemove.getData().size());

        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .build());
    }

    @Test
    public void testGetEntityGroupsByIds() throws Exception {
        long ts = System.currentTimeMillis();
        EntityGroupInfo g1 = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(buildGroup(TEST_PREFIX + ts + "_a", DEVICE_TYPE))
                .build());
        EntityGroupInfo g2 = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(buildGroup(TEST_PREFIX + ts + "_b", DEVICE_TYPE))
                .build());
        String id1 = g1.getId().getId().toString();
        String id2 = g2.getId().getId().toString();

        List<EntityGroupInfo> byIds = client.getEntityGroupsByIds(GetEntityGroupsByIdsArgs.builder()
                .entityGroupIds(List.of(id1, id2))
                .build());
        assertNotNull(byIds);
        assertEquals(2, byIds.size());
        assertTrue(byIds.stream().anyMatch(g -> g.getId().getId().toString().equals(id1)));
        assertTrue(byIds.stream().anyMatch(g -> g.getId().getId().toString().equals(id2)));

        List<EntityInfo> infosByIds = client.getEntityGroupEntityInfosByIds(GetEntityGroupEntityInfosByIdsArgs.builder()
                .entityGroupIds(List.of(id1, id2))
                .build());
        assertNotNull(infosByIds);
        assertEquals(2, infosByIds.size());

        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(id1)
                .build());
        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(id2)
                .build());
    }

    @Test
    public void testPublicAndSharedGroups() throws Exception {
        long ts = System.currentTimeMillis();
        EntityGroupInfo group = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(buildGroup(TEST_PREFIX + ts, DEVICE_TYPE))
                .build());
        String groupId = group.getId().getId().toString();

        client.makeEntityGroupPublic(MakeEntityGroupPublicArgs.builder()
                .entityGroupId(groupId)
                .build());

        GroupPermissionInfo entityGroupPermissions = client.getEntityGroupPermissions(GetEntityGroupPermissionsArgs.builder()
                .entityGroupId(groupId)
                .build()).get(0);
        assertTrue(entityGroupPermissions.getPublic());

        client.makeEntityGroupPrivate(MakeEntityGroupPrivateArgs.builder()
                .entityGroupId(groupId)
                .build());
        List<EntityGroupInfo> afterPrivate = client.getAllSharedEntityGroups(GetAllSharedEntityGroupsArgs.builder()
                .groupType(DEVICE_TYPE)
                .build());
        assertTrue(afterPrivate.stream().noneMatch(g -> g.getId().getId().toString().equals(groupId)));

        List<GroupPermissionInfo> entityGroupPermissions1 = client.getEntityGroupPermissions(GetEntityGroupPermissionsArgs.builder()
                .entityGroupId(groupId)
                .build());
        assertTrue(entityGroupPermissions1.isEmpty());

        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .build());
    }

    @Test
    public void testShareEntityGroup() throws Exception {
        long ts = System.currentTimeMillis();

        Role role = new Role();
        role.setName(TEST_PREFIX + ts + "_role");
        role.setType(RoleType.GROUP);
        role.setPermissions(JacksonUtil.toJsonNode("[\"READ\"]"));
        Role savedRole = client.saveRole(SaveRoleArgs.builder()
                .role(role)
                .build());
        String roleId = savedRole.getId().getId().toString();

        EntityGroupInfo broadGroup = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(buildGroup(TEST_PREFIX + ts + "_broad", DEVICE_TYPE))
                .build());
        String broadGroupId = broadGroup.getId().getId().toString();
        ShareGroupRequest shareRequest = new ShareGroupRequest();
        shareRequest.setAllUserGroup(true);
        shareRequest.setOwnerId(savedClientCustomer.getId());
        client.shareEntityGroup(ShareEntityGroupArgs.builder()
                .entityGroupId(broadGroupId)
                .shareGroupRequest(shareRequest)
                .build());

        EntityGroupInfo specificGroup = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(buildGroup(TEST_PREFIX + ts + "_specific", DEVICE_TYPE))
                .build());
        String specificGroupId = specificGroup.getId().getId().toString();
        EntityInfo customerAdminGroup = client.getEntityGroupEntityInfosByOwnerAndTypeAndPageLink(GetEntityGroupEntityInfosByOwnerAndTypeAndPageLinkArgs.builder()
                .ownerType("CUSTOMER")
                .ownerId(customerId())
                .groupType("USER")
                .pageSize("1")
                .page("0")
                .textSearch("Customer Administrators")
                .build()).getData().get(0);
        String customerAdminGroupId = customerAdminGroup.getId().getId().toString();
        client.shareEntityGroupToChildOwnerUserGroup(ShareEntityGroupToChildOwnerUserGroupArgs.builder()
                .entityGroupId(specificGroupId)
                .userGroupId(customerAdminGroupId)
                .roleId(roleId)
                .build());

        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(broadGroupId)
                .build());
        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(specificGroupId)
                .build());
        client.deleteRole(DeleteRoleArgs.builder()
                .roleId(roleId)
                .build());
    }

    @Test
    public void testOwnerMethods() throws Exception {
        PageDataContactBasedObject owners = client.getOwners(GetOwnersArgs.builder()
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(owners);
        assertNotNull(owners.getData());
        assertFalse(owners.getData().isEmpty());

        PageDataEntityInfo ownerInfos = client.getOwnerInfos(GetOwnerInfosArgs.builder()
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(ownerInfos);
        assertNotNull(ownerInfos.getData());
        assertFalse(ownerInfos.getData().isEmpty());

        EntityInfo tenantInfo = client.getOwnerInfo(GetOwnerInfoArgs.builder()
                .ownerType("TENANT")
                .ownerId(tenantId())
                .build());
        assertNotNull(tenantInfo);
        assertEquals(tenantId(), tenantInfo.getId().getId().toString());

        EntityInfo customerInfo = client.getOwnerInfo(GetOwnerInfoArgs.builder()
                .ownerType("CUSTOMER")
                .ownerId(customerId())
                .build());
        assertNotNull(customerInfo);
        assertEquals(customerId(), customerInfo.getId().getId().toString());
    }

    @Test
    public void testEdgeMethods() throws Exception {
        long ts = System.currentTimeMillis();

        Edge edge = new Edge();
        edge.setName(TEST_PREFIX + ts + "_edge");
        edge.setType("default");
        edge.setSecret("edgeSecret");
        edge.setEdgeLicenseKey("edgeLicenseKey");
        edge.setCloudEndpoint("http://localhost:8080");
        edge.setRoutingKey("routing");
        Edge savedEdge = client.saveEdge(SaveEdgeArgs.builder()
                .edge(edge)
                .build());
        assertNotNull(savedEdge);
        String edgeId = savedEdge.getId().getId().toString();

        EntityGroupInfo group = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(buildGroup(TEST_PREFIX + ts, DEVICE_TYPE))
                .build());
        String groupId = group.getId().getId().toString();

        EntityGroup assigned = client.assignEntityGroupToEdge(AssignEntityGroupToEdgeArgs.builder()
                .edgeId(edgeId)
                .groupType(DEVICE_TYPE)
                .entityGroupId(groupId)
                .build());
        assertNotNull(assigned);
        assertEquals(groupId, assigned.getId().getId().toString());

        List<EntityGroupInfo> allEdgeGroups = client.getAllEdgeEntityGroups(GetAllEdgeEntityGroupsArgs.builder()
                .edgeId(edgeId)
                .groupType(DEVICE_TYPE)
                .build());
        assertNotNull(allEdgeGroups);
        assertTrue(allEdgeGroups.stream().anyMatch(g -> g.getId().getId().toString().equals(groupId)));

        PageDataEntityGroupInfo edgeGroupsPage = client.getEdgeEntityGroups(GetEdgeEntityGroupsArgs.builder()
                .edgeId(edgeId)
                .groupType(DEVICE_TYPE)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(edgeGroupsPage);
        assertTrue(edgeGroupsPage.getData().stream().anyMatch(g -> g.getId().getId().toString().equals(groupId)));

        EntityGroup unassigned = client.unassignEntityGroupFromEdge(UnassignEntityGroupFromEdgeArgs.builder()
                .edgeId(edgeId)
                .groupType(DEVICE_TYPE)
                .entityGroupId(groupId)
                .build());
        assertNotNull(unassigned);
        List<EntityGroupInfo> afterUnassign = client.getAllEdgeEntityGroups(GetAllEdgeEntityGroupsArgs.builder()
                .edgeId(edgeId)
                .groupType(DEVICE_TYPE)
                .build());
        assertTrue(afterUnassign.stream().noneMatch(g -> g.getId().getId().toString().equals(groupId)));

        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .build());
        client.deleteEdge(DeleteEdgeArgs.builder()
                .edgeId(edgeId)
                .build());
    }

    private EntityGroup buildGroup(String name, String type) {
        EntityGroup group = new EntityGroup();
        group.setName(name);
        group.setType(EntityGroup.TypeEnum.valueOf(type));
        return group;
    }

    private String tenantId() {
        return savedClientTenant.getId().getId().toString();
    }

    private String customerId() {
        return savedClientCustomer.getId().getId().toString();
    }

}
