// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.AbstractMessage;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.GroupPermissionProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.customer.CustomerServiceImpl.PUBLIC_CUSTOMER_SUFFIX;

@DaoSqlTest
public class EntityGroupEdgeTest extends AbstractEdgeTest {

    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Test
    public void testAllEdgeEntityGroup_autoAssignedToEdgeAfterUnAssign() throws Exception {
        sendAssetCreateMsgToCloud("Asset From Edge");

        EntityGroup edgeAllAssetGroup = findEdgeAllGroup(EntityType.ASSET);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/entityGroup/" + edgeAllAssetGroup.getUuidId().toString() + "/" + edgeAllAssetGroup.getType().name(), EntityGroup.class);

        validateThatEntityGroupNotAssignedToEdge(edgeAllAssetGroup.getId(), EntityType.ASSET);

        sendAssetCreateMsgToCloud("Asset From Edge 2");

        validateThatEntityGroupAssignedToEdge(edgeAllAssetGroup.getId(), EntityType.ASSET);
    }

    @Test
    public void testEdgeRename_edgeAllGroupsRenamed() throws Exception {
        UUID deviceUuid = Uuids.timeBased();
        sendDeviceCreateMsgToCloud("Device From Edge", deviceUuid);
        sendAssetCreateMsgToCloud("Asset From Edge");
        sendEntityViewCreateMsgToCloud("Entity View From Edge", deviceUuid);
        sendDashboardCreateMsgToCloud("Dashboard From Edge");

        String currentEdgeName = edge.getName();

        verifyEdgeAllGroupNamingConvention(EntityType.DEVICE, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ASSET, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ENTITY_VIEW, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.DASHBOARD, currentEdgeName);

        String newEdgeName = "New Edge Name for rename test";
        edge.setName(newEdgeName);
        edge = doPost("/api/edge", edge, Edge.class);

        verifyEdgeAllGroupNamingConvention(EntityType.DEVICE, newEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ASSET, newEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ENTITY_VIEW, newEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.DASHBOARD, newEdgeName);

        // rollback
        edge.setName(currentEdgeName);
        edge = doPost("/api/edge", edge, Edge.class);

        verifyEdgeAllGroupNamingConvention(EntityType.DEVICE, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ASSET, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ENTITY_VIEW, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.DASHBOARD, currentEdgeName);
    }

    @Test
    public void testSendGroupPermissionsRequestToCloud_publicAndNonPublicPermissions() throws Exception {
        Role groupRole = saveGroupRole();

        EntityGroup dashboardGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "GroupPermissionRequestTest", tenantId);
        UUID dashboardGroupId = dashboardGroup.getId().getId();

        // make dashboard group public -> add public permission -> this permission must be pushed to edge
        edgeImitator.expectMessageAmount(1);
        doPost("/api/entityGroup/" + dashboardGroupId + "/makePublic").andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        // add permission from tenant admin group (assigned to edge) to dashboard group -> this permission must be pushed to edge
        EntityGroupInfo tenantAdminGroup = findTenantAdminsGroup();
        saveGroupPermission(groupRole.getId(), tenantAdminGroup.getId(), dashboardGroup);

        // add permission from tenant users group (NOT assigned to edge) to dashboard group -> this permission must NOT be pushed to edge
        EntityGroup userGroupNotAssignedToEdge = createUserGroup();
        saveGroupPermission(groupRole.getId(), userGroupNotAssignedToEdge.getId(), dashboardGroup);

        EntityGroupRequestMsg.Builder entitiesGroupPermissionsRequestMsgBuilder = EntityGroupRequestMsg.newBuilder()
                .setEntityGroupIdMSB(dashboardGroupId.getMostSignificantBits())
                .setEntityGroupIdLSB(dashboardGroupId.getLeastSignificantBits())
                .setType(EntityType.DASHBOARD.name());
        testAutoGeneratedCodeByProtobuf(entitiesGroupPermissionsRequestMsgBuilder);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addEntityGroupPermissionsRequestMsg(entitiesGroupPermissionsRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(2);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        List<GroupPermissionProto> groupPermissionProtos = edgeImitator.findAllMessagesByType(GroupPermissionProto.class);
        Assert.assertEquals(2, groupPermissionProtos.size());

        for (GroupPermissionProto groupPermissionProto : groupPermissionProtos) {
            GroupPermission groupPermission = JacksonUtil.fromString(groupPermissionProto.getEntity(), GroupPermission.class, true);
            Assert.assertNotNull(groupPermission);
            if (groupPermission.isPublic()) {
                Customer publicCustomer = doGet("/api/tenant/customers?customerTitle=" + PUBLIC_CUSTOMER_SUFFIX, Customer.class);
                EntityGroupInfo publicUsersGroup = findGroupByOwnerIdTypeAndName(publicCustomer.getId(), EntityType.USER, EntityGroup.GROUP_PUBLIC_USERS_NAME);
                Assert.assertEquals(publicUsersGroup.getId(), groupPermission.getUserGroupId());
            } else {
                Assert.assertEquals(tenantAdminGroup.getId(), groupPermission.getUserGroupId());
            }
            Assert.assertEquals(dashboardGroup.getId(), groupPermission.getEntityGroupId());
        }
    }

    @Test
    public void testEntityGroupEntitiesRequestForUnassignedGroupIsIgnored() throws Exception {
        EntityGroup deviceGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "UnassignedGroupRequestTest", tenantId);

        edgeImitator.expectMessageAmount(2);
        saveDevice("Unassigned Group Request Device", THERMOSTAT_DEVICE_PROFILE_NAME, deviceGroup.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        unAssignEntityGroupFromEdge(deviceGroup);

        UUID deviceGroupUuid = deviceGroup.getUuidId();
        UplinkMsg uplinkMsg = UplinkMsg.newBuilder()
                .addEntityGroupEntitiesRequestMsg(EntityGroupRequestMsg.newBuilder()
                        .setEntityGroupIdMSB(deviceGroupUuid.getMostSignificantBits())
                        .setEntityGroupIdLSB(deviceGroupUuid.getLeastSignificantBits())
                        .setType(EntityType.DEVICE.name())
                        .build())
                .build();

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertFalse(edgeImitator.waitForMessages(5));
        Assert.assertTrue(edgeImitator.findMessageByType(DeviceUpdateMsg.class).isEmpty());
    }

    @Test
    public void testFailedEntityGroupDownlinkIsRetriedWhenEnabled() throws Exception {
        EntityGroup deviceGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "Retried Unassign Group", tenantId);

        edgeEventStorageSettings.setRetryFailedEntityDownlinks(true);
        edgeImitator.setEntityGroupDownlinkFailures(1);
        try {
            unAssignEntityGroupFromEdge(deviceGroup);
        } finally {
            edgeImitator.setEntityGroupDownlinkFailures(0);
            edgeEventStorageSettings.setRetryFailedEntityDownlinks(false);
        }
    }

    @Test
    public void testCustomerOwnedGroupReachesTenantEdgeWithItsOwnerCustomers() throws Exception {
        Customer parent = saveCustomer("Group Owner Parent", null);
        Customer child = saveCustomer("Group Owner Child", parent.getId());
        EntityGroup group = new EntityGroup();
        group.setType(EntityType.DEVICE);
        group.setName("Group Owner Child Devices");
        group.setOwnerId(child.getId());
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);

        edgeImitator.expectMessageAmount(3);
        doPost("/api/edge/" + edge.getUuidId() + "/entityGroup/" + savedGroup.getUuidId() + "/" + EntityType.DEVICE.name(), EntityGroup.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        List<AbstractMessage> downlinks = edgeImitator.getDownlinkMsgs();
        List<UUID> customerIds = downlinks.stream().filter(CustomerUpdateMsg.class::isInstance).map(CustomerUpdateMsg.class::cast)
                .map(msg -> new UUID(msg.getIdMSB(), msg.getIdLSB())).toList();
        Assert.assertEquals(List.of(parent.getUuidId(), child.getUuidId()), customerIds);
        int lastCustomerIndex = -1;
        int groupIndex = -1;
        for (int i = 0; i < downlinks.size(); i++) {
            if (downlinks.get(i) instanceof CustomerUpdateMsg) {
                lastCustomerIndex = i;
            } else if (downlinks.get(i) instanceof EntityGroupUpdateMsg msg && new UUID(msg.getIdMSB(), msg.getIdLSB()).equals(savedGroup.getUuidId())) {
                groupIndex = i;
            }
        }
        Assert.assertTrue("the group has to follow its owner customers", groupIndex > lastCustomerIndex);

        EntityGroup tenantGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "Tenant Devices For Owner Check", tenantId);
        Assert.assertTrue(edgeImitator.findAllMessagesByType(CustomerUpdateMsg.class).isEmpty());

        unAssignEntityGroupFromEdge(tenantGroup);
        unAssignEntityGroupFromEdge(savedGroup);
    }

    @Test
    public void testFailedEntityGroupDownlinkIsDroppedByDefault() throws Exception {
        EntityGroup deviceGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "Dropped Unassign Group", tenantId);

        edgeImitator.setEntityGroupDownlinkFailures(1);
        try {
            edgeImitator.expectMessageAmount(1);
            doDelete("/api/edge/" + edge.getUuidId()
                    + "/entityGroup/" + deviceGroup.getUuidId().toString() + "/" + deviceGroup.getType().name(), EntityGroup.class);
            Assert.assertFalse(edgeImitator.waitForMessages(5));
        } finally {
            edgeImitator.setEntityGroupDownlinkFailures(0);
        }
    }

    private EntityGroup createUserGroup() {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setType(EntityType.USER);
        entityGroup.setName("Not Assigned To Edge");
        entityGroup.setOwnerId(tenantId);
        return doPost("/api/entityGroup", entityGroup, EntityGroup.class);
    }

    private Role saveGroupRole() throws Exception {
        Role groupRole = new Role();
        groupRole.setTenantId(tenantId);
        groupRole.setName("Read Group Role");
        groupRole.setType(RoleType.GROUP);
        ArrayNode readPermissions = JacksonUtil.newArrayNode();
        readPermissions.add("READ");
        groupRole.setPermissions(readPermissions);
        edgeImitator.expectMessageAmount(1);
        Role savedGroupRole = doPost("/api/role", groupRole, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return savedGroupRole;
    }

    private void saveGroupPermission(RoleId roleId, EntityGroupId userGroupId, EntityGroup entityGroup) {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(roleId);
        groupPermission.setUserGroupId(userGroupId);
        groupPermission.setEntityGroupId(entityGroup.getId());
        groupPermission.setEntityGroupType(entityGroup.getType());
        doPost("/api/groupPermission", groupPermission, GroupPermission.class);
    }

    private void verifyEdgeAllGroupNamingConvention(EntityType entityType, String edgeName) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    EntityGroup edgeAllGroup = findEdgeAllGroup(entityType);
                    return edgeAllGroup.getName().contains(edgeName);
                });
    }

    private void sendDeviceCreateMsgToCloud(String deviceName, UUID uuid) throws Exception {
        Device device = buildDeviceForUplinkMsg(deviceName, uuid);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setEntity(JacksonUtil.toString(device));
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private void sendAssetCreateMsgToCloud(String assetName) throws Exception {
        Asset asset = buildAssetForUplinkMsg(assetName);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AssetUpdateMsg.Builder assetUpdateMsgBuilder = AssetUpdateMsg.newBuilder();
        assetUpdateMsgBuilder.setIdMSB(asset.getUuidId().getMostSignificantBits());
        assetUpdateMsgBuilder.setIdLSB(asset.getUuidId().getLeastSignificantBits());
        assetUpdateMsgBuilder.setEntity(JacksonUtil.toString(asset));
        assetUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(assetUpdateMsgBuilder);
        uplinkMsgBuilder.addAssetUpdateMsg(assetUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private void sendEntityViewCreateMsgToCloud(String entityViewName, UUID entityUuid) throws Exception {
        EntityView entityView = buildEntityViewForUplinkMsg(entityUuid, entityViewName);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        EntityViewUpdateMsg.Builder entityViewUpdateMsgBuilder = EntityViewUpdateMsg.newBuilder();
        entityViewUpdateMsgBuilder.setIdMSB(entityView.getUuidId().getMostSignificantBits());
        entityViewUpdateMsgBuilder.setIdLSB(entityView.getUuidId().getLeastSignificantBits());
        entityViewUpdateMsgBuilder.setEntity(JacksonUtil.toString(entityView));
        entityViewUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addEntityViewUpdateMsg(entityViewUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private void sendDashboardCreateMsgToCloud(String dashboardName) throws Exception {
        Dashboard dashboard = buildDashboardForUplinkMsg(dashboardName);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DashboardUpdateMsg.Builder dashboardUpdateMsgBuilder = DashboardUpdateMsg.newBuilder();
        dashboardUpdateMsgBuilder.setIdMSB(dashboard.getUuidId().getMostSignificantBits());
        dashboardUpdateMsgBuilder.setIdLSB(dashboard.getUuidId().getLeastSignificantBits());
        dashboardUpdateMsgBuilder.setEntity(JacksonUtil.toString(dashboard));
        dashboardUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDashboardUpdateMsg(dashboardUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private EntityGroup findEdgeAllGroup(EntityType groupType) throws Exception {
        List<EntityGroupInfo> groupsList = getEntityGroupsByOwnerAndType(tenantId, groupType);
        EntityGroup edgeAllAssetGroup = null;
        for (EntityGroupInfo tmp : groupsList) {
            if (tmp.isEdgeGroupAll()) {
                edgeAllAssetGroup = tmp;
                break;
            }
        }
        Assert.assertNotNull(edgeAllAssetGroup);
        return edgeAllAssetGroup;
    }

    private Device buildDeviceForUplinkMsg(String name, UUID uuid) {
        Device device = new Device();
        device.setId(new DeviceId(uuid));
        device.setTenantId(tenantId);
        device.setType("default");
        device.setName(name);
        return device;
    }

    private Asset buildAssetForUplinkMsg(String name) {
        Asset asset = new Asset();
        asset.setId(new AssetId(Uuids.timeBased()));
        asset.setTenantId(tenantId);
        asset.setName(name);
        asset.setType("default");
        return asset;
    }

    private EntityView buildEntityViewForUplinkMsg(UUID uuid, String name) {
        EntityView entityView = new EntityView();
        entityView.setId(new EntityViewId(Uuids.timeBased()));
        entityView.setTenantId(tenantId);
        entityView.setName(name);
        entityView.setType("default");
        entityView.setEntityId(new DeviceId(uuid));
        return entityView;
    }

    private Dashboard buildDashboardForUplinkMsg(String name) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(new DashboardId(Uuids.timeBased()));
        dashboard.setTenantId(tenantId);
        dashboard.setTitle(name);
        dashboard.setConfiguration(TextNode.valueOf(""));
        return dashboard;
    }

}
