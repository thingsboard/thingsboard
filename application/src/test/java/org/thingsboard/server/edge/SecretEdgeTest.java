// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class SecretEdgeTest extends AbstractEdgeTest {

    private static final String DEFAULT_SECRET_NAME = "Edge Test Secret";
    private static final String CUSTOMER_SECRET_NAME = "Edge Customer Secret";
    private static final String UPDATED_SECRET_DESCRIPTION = "Updated Edge Test Secret";
    public static final String TEST_VALUE = "test-value";

    @Autowired
    private SecretService secretService;

    @Test
    public void testSecret_create_update_delete() throws Exception {
        // create
        Secret secret = createSecret();

        edgeImitator.expectMessageAmount(1);
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof SecretUpdateMsg);
        SecretUpdateMsg msg = (SecretUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, msg.getMsgType());

        Secret secretFromMsg = JacksonUtil.fromString(msg.getEntity(), Secret.class, true);
        Assert.assertNotNull(secretFromMsg);
        Assert.assertEquals(DEFAULT_SECRET_NAME, secretFromMsg.getName());

        // update
        edgeImitator.expectMessageAmount(1);
        savedSecret.setDescription(UPDATED_SECRET_DESCRIPTION);
        secret = new Secret(savedSecret, secret.getEncryptedValue());
        secret.setValue(TEST_VALUE);
        savedSecret = doPost("/api/secret", secret, SecretInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        msg = (SecretUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, msg.getMsgType());

        secretFromMsg = JacksonUtil.fromString(msg.getEntity(), Secret.class, true);
        Assert.assertNotNull(secretFromMsg);
        Assert.assertEquals(UPDATED_SECRET_DESCRIPTION, secretFromMsg.getDescription());

        // update without setting the value
        edgeImitator.expectMessageAmount(1);
        savedSecret.setDescription(UPDATED_SECRET_DESCRIPTION);
        secret = new Secret(savedSecret, secret.getEncryptedValue());
        savedSecret = doPost("/api/secret", secret, SecretInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        msg = (SecretUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, msg.getMsgType());

        secretFromMsg = JacksonUtil.fromString(msg.getEntity(), Secret.class, true);
        Assert.assertNotNull(secretFromMsg);
        Assert.assertEquals(UPDATED_SECRET_DESCRIPTION, secretFromMsg.getDescription());

        // delete
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/secret/" + savedSecret.getUuidId()).andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        msg = (SecretUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, msg.getMsgType());
    }

    @Test
    public void testSecret_customerLevel() throws Exception {
        Customer savedCustomer = createCustomerAndChangeEdgeOwner();

        // SecretController derives the customer id from the current user, so the customer-scoped
        // secret is created through the service to place it in the customer's scope.
        Secret secret = createCustomerScopedSecret(savedCustomer.getId());

        // create
        edgeImitator.expectMessageAmount(1);
        Secret savedSecret = secretService.saveSecret(tenantId, secret);
        Assert.assertTrue(edgeImitator.waitForMessages());

        SecretUpdateMsg msg = (SecretUpdateMsg) edgeImitator.getLatestMessage();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, msg.getMsgType());
        Secret secretFromMsg = JacksonUtil.fromString(msg.getEntity(), Secret.class, true);
        Assert.assertNotNull(secretFromMsg);
        Assert.assertEquals(CUSTOMER_SECRET_NAME, secretFromMsg.getName());
        Assert.assertEquals(savedCustomer.getId(), secretFromMsg.getCustomerId());

        // update
        edgeImitator.expectMessageAmount(1);
        savedSecret.setDescription(UPDATED_SECRET_DESCRIPTION);
        savedSecret = secretService.saveSecret(tenantId, savedSecret);
        Assert.assertTrue(edgeImitator.waitForMessages());

        msg = (SecretUpdateMsg) edgeImitator.getLatestMessage();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, msg.getMsgType());
        secretFromMsg = JacksonUtil.fromString(msg.getEntity(), Secret.class, true);
        Assert.assertNotNull(secretFromMsg);
        Assert.assertEquals(UPDATED_SECRET_DESCRIPTION, secretFromMsg.getDescription());

        // delete
        edgeImitator.expectMessageAmount(1);
        secretService.deleteSecret(tenantId, savedSecret);
        Assert.assertTrue(edgeImitator.waitForMessages());

        msg = (SecretUpdateMsg) edgeImitator.getLatestMessage();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, msg.getMsgType());

        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 2);
        doDelete("/api/customer/" + savedCustomer.getUuidId()).andExpect(status().isOk());
    }

    @Test
    public void testSecret_customerSecretReachesOnlyEdgesOfThatCustomer() throws Exception {
        Customer otherCustomer = saveCustomer("Other Edge Customer", null);

        // a tenant-owned edge does not receive a customer secret
        edgeImitator.expectMessageAmount(1);
        Secret otherCustomerSecret = secretService.saveSecret(tenantId, createCustomerScopedSecret(otherCustomer.getId()));
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        Customer edgeCustomer = createCustomerAndChangeEdgeOwner();

        // nor does an edge owned by a different customer
        edgeImitator.expectMessageAmount(1);
        otherCustomerSecret.setDescription(UPDATED_SECRET_DESCRIPTION);
        otherCustomerSecret = secretService.saveSecret(tenantId, otherCustomerSecret);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // tenant secrets and the owner customer's secrets still reach it
        edgeImitator.expectMessageAmount(2);
        Secret tenantSecret = secretService.saveSecret(tenantId, createSecret());
        Secret edgeCustomerSecret = secretService.saveSecret(tenantId, createCustomerScopedSecret(edgeCustomer.getId()));
        Assert.assertTrue(edgeImitator.waitForMessages());
        Set<SecretId> receivedSecretIds = edgeImitator.findAllMessagesByType(SecretUpdateMsg.class).stream()
                .map(msg -> JacksonUtil.fromString(msg.getEntity(), Secret.class, true).getId())
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(tenantSecret.getId(), edgeCustomerSecret.getId()), receivedSecretIds);

        edgeImitator.expectMessageAmount(3);
        secretService.deleteSecret(tenantId, tenantSecret);
        secretService.deleteSecret(tenantId, edgeCustomerSecret);
        secretService.deleteSecret(tenantId, otherCustomerSecret);
        Assert.assertTrue(edgeImitator.waitForMessages());

        changeEdgeOwnerFromCustomerToTenant(edgeCustomer, 2);
        doDelete("/api/customer/" + edgeCustomer.getUuidId()).andExpect(status().isOk());
        doDelete("/api/customer/" + otherCustomer.getUuidId()).andExpect(status().isOk());
    }

    private Secret createSecret() {
        Secret secret = new Secret();
        secret.setTenantId(tenantId);
        secret.setName(SecretEdgeTest.DEFAULT_SECRET_NAME);
        secret.setType(SecretType.TEXT);
        secret.setValue(TEST_VALUE);
        secret.setDescription("test-description");
        return secret;
    }

    private Secret createCustomerScopedSecret(CustomerId customerId) {
        Secret secret = new Secret();
        secret.setTenantId(tenantId);
        secret.setCustomerId(customerId);
        secret.setName(SecretEdgeTest.CUSTOMER_SECRET_NAME);
        secret.setType(SecretType.TEXT);
        secret.setValue(TEST_VALUE);
        secret.setDescription("test-description");
        return secret;
    }

    private Customer createCustomerAndChangeEdgeOwner() throws Exception {
        Customer savedCustomer = saveCustomer("Edge Customer", null);
        saveCustomer("Edge Sub Customer", savedCustomer.getId());

        TimeUnit.MILLISECONDS.sleep(500);

        changeEdgeOwnerToCustomer(savedCustomer);
        return savedCustomer;
    }

}
