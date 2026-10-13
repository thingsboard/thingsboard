// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.GroupPermissionProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;
import java.util.Map;

@DaoSqlTest
public class GroupPermissionsEdgeTest extends AbstractEdgeTest {

    @Test
    public void testSaveGroupPermissionWithGenericRole() throws Exception {
        EntityGroup userEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "testSaveGroupPermissionWithGenericRole", tenantId);
        Role otaPackageReadGeneric = saveGenericRole(Resource.OTA_PACKAGE, List.of(Operation.READ));
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(otaPackageReadGeneric.getId());
        groupPermission.setUserGroupId(userEntityGroup.getId());
        groupPermission.setEntityGroupId(null);
        groupPermission.setEntityGroupType(null);

        edgeImitator.expectMessageAmount(1);
        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof GroupPermissionProto);
        GroupPermissionProto groupPermissionProto = (GroupPermissionProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, groupPermissionProto.getMsgType());
        GroupPermission result = JacksonUtil.fromString(groupPermissionProto.getEntity(), GroupPermission.class, true);
        Assert.assertNotNull(result);
        Assert.assertEquals(otaPackageReadGeneric.getId(), result.getRoleId());
        Assert.assertEquals(groupPermission.getUserGroupId(), result.getUserGroupId());

        Role aiModelAllGeneric = saveGenericRole(Resource.AI_MODEL, List.of(Operation.ALL));
        groupPermission.setRoleId(aiModelAllGeneric.getId());

        edgeImitator.expectMessageAmount(1);
        doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof GroupPermissionProto);
        groupPermissionProto = (GroupPermissionProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, groupPermissionProto.getMsgType());
        result = JacksonUtil.fromString(groupPermissionProto.getEntity(), GroupPermission.class, true);
        Assert.assertNotNull(result);
        Assert.assertEquals(aiModelAllGeneric.getId(), result.getRoleId());

    }

    private Role saveGenericRole(Resource resource, List<Operation> operations) throws Exception {
        Map<Resource, List<Operation>> permissions = Map.of(resource, operations);
        Role genericRole = new Role();
        genericRole.setTenantId(tenantId);
        genericRole.setName(resource.name() + "_generic_role");
        genericRole.setType(RoleType.GENERIC);
        genericRole.setPermissions(JacksonUtil.valueToTree(permissions));
        edgeImitator.expectMessageAmount(1);
        genericRole = doPost("/api/role", genericRole, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return genericRole;
    }

}
