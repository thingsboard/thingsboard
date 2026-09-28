// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RoleEdgeTest extends AbstractEdgeTest {

    @Test
    public void testTenantRole() throws Exception {
        // create role
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        role.setName("Generic Edge Role");
        edgeImitator.expectMessageAmount(1);
        Role savedRole = doPost("/api/role", role, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        RoleProto roleProto = (RoleProto) latestMessage;
        Role roleMsg = JacksonUtil.fromString(roleProto.getEntity(), Role.class, true);
        Assert.assertNotNull(roleMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals(RoleType.GENERIC, roleMsg.getType());
        Assert.assertEquals(savedRole.getId(), roleMsg.getId());
        Assert.assertEquals(savedRole.getPermissions(), roleMsg.getPermissions());

        // update role
        edgeImitator.expectMessageAmount(1);
        savedRole.setName("Generic Edge Role Updated");
        savedRole = doPost("/api/role", savedRole, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        roleProto = (RoleProto) latestMessage;
        roleMsg = JacksonUtil.fromString(roleProto.getEntity(), Role.class, true);
        Assert.assertNotNull(roleMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals("Generic Edge Role Updated", roleMsg.getName());

        // delete role
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/role/" + savedRole.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        roleProto = (RoleProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals(savedRole.getUuidId().getMostSignificantBits(), roleProto.getIdMSB());
        Assert.assertEquals(savedRole.getUuidId().getLeastSignificantBits(), roleProto.getIdLSB());
    }

    @Test
    public void testCustomerRole() throws Exception {
        // create customer
        edgeImitator.expectMessageAmount(1);
        Customer savedCustomer = saveCustomer("Edge Customer", null);
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        role.setName("Customer Generic Edge Role");
        role.setOwnerId(savedCustomer.getId());
        role.setCustomerId(savedCustomer.getId());
        Role savedRole = doPost("/api/role", role, Role.class);
        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to customer
        edgeImitator.expectMessageAmount(5);
        doPost("/api/owner/CUSTOMER/" + savedCustomer.getId().getId() + "/EDGE/" + edge.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<CustomerUpdateMsg> customerUpdateMsgs = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateMsgs.isPresent());
        CustomerUpdateMsg customerAUpdateMsg = customerUpdateMsgs.get();
        Customer customer = JacksonUtil.fromString(customerAUpdateMsg.getEntity(), Customer.class, true);
        Assert.assertNotNull(customer);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerAUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), customerAUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), customerAUpdateMsg.getIdLSB());
        Assert.assertEquals(savedCustomer.getTitle(), customer.getTitle());

        Optional<RoleProto> roleProtoOpt = edgeImitator.findMessageByType(RoleProto.class);
        Assert.assertTrue(roleProtoOpt.isPresent());
        RoleProto roleProto = roleProtoOpt.get();
        Role roleMsg = JacksonUtil.fromString(roleProto.getEntity(), Role.class, true);
        Assert.assertNotNull(roleMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals(RoleType.GENERIC, roleMsg.getType());
        Assert.assertEquals(savedRole.getId(), roleMsg.getId());
        Assert.assertEquals(savedCustomer.getId(), roleMsg.getCustomerId());
        Assert.assertEquals(savedRole.getPermissions(), roleMsg.getPermissions());

        List<EntityGroupUpdateMsg> entityGroupUpdateMsgs = edgeImitator.findAllMessagesByType(EntityGroupUpdateMsg.class);
        Assert.assertEquals(2, entityGroupUpdateMsgs.size());

        Optional<EdgeConfiguration> edgeConfigurationOpt = edgeImitator.findMessageByType(EdgeConfiguration.class);
        Assert.assertTrue(edgeConfigurationOpt.isPresent());

        // update role
        edgeImitator.expectMessageAmount(1);
        savedRole.setName("Customer Generic Edge Role Updated");
        savedRole = doPost("/api/role", savedRole, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        roleProto = (RoleProto) latestMessage;
        roleMsg = JacksonUtil.fromString(roleProto.getEntity(), Role.class, true);
        Assert.assertNotNull(roleMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals("Customer Generic Edge Role Updated", roleMsg.getName());

        // delete role
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/role/" + savedRole.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        roleProto = (RoleProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals(savedRole.getUuidId().getMostSignificantBits(), roleProto.getIdMSB());
        Assert.assertEquals(savedRole.getUuidId().getLeastSignificantBits(), roleProto.getIdLSB());

        // change owner to tenant
        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 2);

        // delete customers
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

}
