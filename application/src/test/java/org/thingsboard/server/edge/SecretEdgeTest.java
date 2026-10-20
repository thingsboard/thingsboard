// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class SecretEdgeTest extends AbstractEdgeTest {

    private static final String DEFAULT_SECRET_NAME = "Edge Test Secret";
    private static final String UPDATED_SECRET_DESCRIPTION = "Updated Edge Test Secret";
    public static final String TEST_VALUE = "test-value";

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
        Assert.assertEquals(UPDATED_SECRET_DESCRIPTION, secretFromMsg.getDescription());

        // delete
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/secret/" + savedSecret.getUuidId()).andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        msg = (SecretUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, msg.getMsgType());
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

}
