// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CustomerEdgeTest extends AbstractEdgeTest {

    @Test
    public void testCreateUpdateDeleteCustomer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        Customer savedSubSubCustomerA = saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());
        // create customer B
        Customer savedCustomerB = saveCustomer("Edge Customer B", null);

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        // update customer and validate changes on edge
        updateCustomerAndValidateChangesOnEdge(savedCustomerA, "Edge Customer A Updated");

        // update sub customer and validate changes on edge
        updateCustomerAndValidateChangesOnEdge(savedSubCustomerA, "Edge Sub Customer A Updated");

        // update sub sub customer and validate NO changes on edge
        updateCustomerAndValidate_NO_ChangesOnEdge(savedSubSubCustomerA, "Edge Sub Sub Customer A Updated");

        // update customer B and validate NO changes on edge
        updateCustomerAndValidate_NO_ChangesOnEdge(savedCustomerB, "Edge Customer B Updated");

        // change edge owner from sub customer A to tenant
        changeEdgeOwnerFromSubCustomerToTenant(savedCustomerA, savedSubCustomerA, 4);

        // delete customers
        doDelete("/api/customer/" + savedCustomerA.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/customer/" + savedCustomerB.getUuidId())
                .andExpect(status().isOk());
    }

    private void updateCustomerAndValidateChangesOnEdge(Customer customer, String updatedTitle) throws InterruptedException {
        edgeImitator.expectMessageAmount(1);
        customer.setTitle(updatedTitle);
        doPost("/api/customer", customer, Customer.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomerUpdateMsg);
        CustomerUpdateMsg customerUpdateMsg = (CustomerUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(customer.getUuidId().getMostSignificantBits(), customerUpdateMsg.getIdMSB());
        Assert.assertEquals(customer.getUuidId().getLeastSignificantBits(), customerUpdateMsg.getIdLSB());
        Customer result = JacksonUtil.fromString(customerUpdateMsg.getEntity(), Customer.class, true);
        Assert.assertNotNull(result);
        Assert.assertEquals(updatedTitle, result.getTitle());
    }

    private void updateCustomerAndValidate_NO_ChangesOnEdge(Customer customer, String updatedTitle) throws InterruptedException {
        edgeImitator.expectMessageAmount(1);
        customer.setTitle(updatedTitle);
        doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(1));
    }

    @Test
    public void testChangeOwnerOfCustomer_validateChangesToEdgeEntityGroups() throws Exception {
        edgeImitator.expectMessageAmount(1);
        Customer savedCustomer = saveCustomer("Edge Owner Customer Groups", null);
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change an edge owner from tenant to customer (no roles)
        changeEdgeOwnerFromTenantToCustomer(savedCustomer, 0);

        // the customer's two entity groups are assigned to the edge
        List<EntityGroupUpdateMsg> assignedGroups = edgeImitator.findAllMessagesByType(EntityGroupUpdateMsg.class);
        Assert.assertEquals(2, assignedGroups.size());
        for (EntityGroupUpdateMsg groupUpdateMsg : assignedGroups) {
            Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, groupUpdateMsg.getMsgType());
            EntityGroup entityGroup = JacksonUtil.fromString(groupUpdateMsg.getEntity(), EntityGroup.class, true);
            Assert.assertNotNull(entityGroup);
            Assert.assertEquals(savedCustomer.getId(), entityGroup.getOwnerId());
        }

        // change an edge owner back to tenant: the two entity groups are unassigned from the edge
        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 2);

        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testChangeOwnerOfCustomerFromTenantToCustomer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        Customer savedCustomer = saveCustomer("Edge Owner Customer Ordering", null);
        createCustomerRole(savedCustomer, "Customer Ordering Edge Role 1");
        createCustomerRole(savedCustomer, "Customer Ordering Edge Role 2");
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change an edge owner from tenant to customer (2 roles)
        changeEdgeOwnerFromTenantToCustomer(savedCustomer, 2);

        // Asserts the customer is delivered before every one of its roles; see EdgeEntityProcessor.syncCustomer for the rationale.
        List<AbstractMessage> downlinkMsgs = edgeImitator.getDownlinkMsgs();
        List<CustomerUpdateMsg> customerMsgs = edgeImitator.findAllMessagesByType(CustomerUpdateMsg.class);
        List<RoleProto> roleMsgs = edgeImitator.findAllMessagesByType(RoleProto.class);
        Assert.assertFalse("Expected a CustomerUpdateMsg among the downlinks", customerMsgs.isEmpty());
        Assert.assertEquals("Expected both roles among the downlinks", 2, roleMsgs.size());
        int customerIndex = downlinkMsgs.indexOf(customerMsgs.get(0));
        for (RoleProto roleMsg : roleMsgs) {
            Assert.assertTrue("Customer must be sent to the edge before each of its roles",
                    customerIndex < downlinkMsgs.indexOf(roleMsg));
        }

        // change an edge owner back to tenant and clean up
        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 2);

        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    private void createCustomerRole(Customer customer, String name) throws Exception {
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        role.setName(name);
        role.setOwnerId(customer.getId());
        role.setCustomerId(customer.getId());
        doPost("/api/role", role, Role.class);
    }

}
