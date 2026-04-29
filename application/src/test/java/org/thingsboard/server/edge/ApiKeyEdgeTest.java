/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.ApiKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.gen.edge.v1.UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;

@DaoSqlTest
public class ApiKeyEdgeTest extends AbstractEdgeTest {

    @Autowired
    private ApiKeyService apiKeyService;

    private static final String DEFAULT_API_KEY_DESCRIPTION = "Edge Test ApiKey";
    private static final String UPDATED_API_KEY_DESCRIPTION = "Updated Edge Test ApiKey";

    @Test
    public void testApiKey_create_update_delete_fromCloud() throws Exception {
        // create ApiKey
        ApiKeyInfo apiKeyInfo = createSimpleApiKeyInfo(DEFAULT_API_KEY_DESCRIPTION);

        edgeImitator.expectMessageAmount(1);
        ApiKey savedApiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof ApiKeyUpdateMsg);
        ApiKeyUpdateMsg apiKeyUpdateMsg = (ApiKeyUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, apiKeyUpdateMsg.getMsgType());
        Assert.assertEquals(savedApiKey.getUuidId().getMostSignificantBits(), apiKeyUpdateMsg.getIdMSB());
        Assert.assertEquals(savedApiKey.getUuidId().getLeastSignificantBits(), apiKeyUpdateMsg.getIdLSB());
        ApiKey apiKeyFromMsg = JacksonUtil.fromString(apiKeyUpdateMsg.getEntity(), ApiKey.class, true);
        Assert.assertNotNull(apiKeyFromMsg);

        Assert.assertEquals(DEFAULT_API_KEY_DESCRIPTION, apiKeyFromMsg.getDescription());
        Assert.assertEquals(savedApiKey.getTenantId(), apiKeyFromMsg.getTenantId());

        // update ApiKey
        edgeImitator.expectMessageAmount(1);
        savedApiKey.setDescription(UPDATED_API_KEY_DESCRIPTION);
        savedApiKey = doPost("/api/apiKey", new ApiKeyInfo(savedApiKey), ApiKey.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof ApiKeyUpdateMsg);
        apiKeyUpdateMsg = (ApiKeyUpdateMsg) latestMessage;
        apiKeyFromMsg = JacksonUtil.fromString(apiKeyUpdateMsg.getEntity(), ApiKey.class, true);
        Assert.assertNotNull(apiKeyFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, apiKeyUpdateMsg.getMsgType());
        Assert.assertEquals(UPDATED_API_KEY_DESCRIPTION, apiKeyFromMsg.getDescription());

        // delete ApiKey
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/apiKey/" + savedApiKey.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof ApiKeyUpdateMsg);
        apiKeyUpdateMsg = (ApiKeyUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, apiKeyUpdateMsg.getMsgType());
        Assert.assertEquals(savedApiKey.getUuidId().getMostSignificantBits(), apiKeyUpdateMsg.getIdMSB());
        Assert.assertEquals(savedApiKey.getUuidId().getLeastSignificantBits(), apiKeyUpdateMsg.getIdLSB());
    }

    @Test
    public void testApiKey_create_update_delete_toCloud() throws Exception {
        // create
        ApiKey apiKey = createSimpleApiKey(DEFAULT_API_KEY_DESCRIPTION);
        UUID uuid = Uuids.timeBased();
        UplinkMsg uplinkMsg = getUplinkMsg(uuid, apiKey, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        checkApiKeyOnCloud(uplinkMsg, uuid, apiKey.getDescription());

        // update
        apiKey.setDescription(UPDATED_API_KEY_DESCRIPTION);
        UplinkMsg updatedUplinkMsg = getUplinkMsg(uuid, apiKey, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);

        checkApiKeyOnCloud(updatedUplinkMsg, uuid, apiKey.getDescription());

        // delete
        UplinkMsg deleteUplinkMsg = getDeleteUplinkMsg(uuid);
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(deleteUplinkMsg);
        Assert.assertTrue(edgeImitator.waitForResponses());

        ApiKeyId apiKeyId = new ApiKeyId(uuid);
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                Assert.assertNull(apiKeyService.findApiKeyById(tenantId, apiKeyId))
        );
    }

    @Test
    public void testApiKey_pushedDuringUserSync() throws Exception {
        // create tenant admin user - expect 3 messages: 1 UserUpdateMsg + 2 UserCredentialsUpdateMsg
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("apiKeyTestUser@thingsboard.org");
        user.setFirstName("ApiKey");
        user.setLastName("TestUser");

        edgeImitator.expectMessageAmount(3);
        User savedUser = createUser(user, "tenant");
        Assert.assertTrue(edgeImitator.waitForMessages());
        Assert.assertEquals(1, edgeImitator.findAllMessagesByType(UserUpdateMsg.class).size());
        Assert.assertEquals(2, edgeImitator.findAllMessagesByType(UserCredentialsUpdateMsg.class).size());

        // create API key for this user - expect 1 ApiKeyUpdateMsg
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(tenantId);
        apiKeyInfo.setUserId(savedUser.getId());
        apiKeyInfo.setDescription("Test API Key for user sync");
        apiKeyInfo.setEnabled(true);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Assert.assertEquals(1, edgeImitator.findAllMessagesByType(ApiKeyUpdateMsg.class).size());

        // update user - expect 3 messages: UserUpdateMsg + UserCredentialsUpdateMsg + ApiKeyUpdateMsg
        savedUser.setLastName("UpdatedLastName");
        edgeImitator.expectMessageAmount(3);
        doPost("/api/user", savedUser, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        Assert.assertEquals(1, edgeImitator.findAllMessagesByType(UserUpdateMsg.class).size());
        Assert.assertEquals(1, edgeImitator.findAllMessagesByType(UserCredentialsUpdateMsg.class).size());
        Assert.assertEquals(1, edgeImitator.findAllMessagesByType(ApiKeyUpdateMsg.class).size());

        Optional<ApiKeyUpdateMsg> apiKeyUpdateMsgOpt = edgeImitator.findMessageByType(ApiKeyUpdateMsg.class);
        Assert.assertTrue(apiKeyUpdateMsgOpt.isPresent());
        ApiKeyUpdateMsg apiKeyUpdateMsg = apiKeyUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, apiKeyUpdateMsg.getMsgType());
    }

    private ApiKeyInfo createSimpleApiKeyInfo(String description) {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(tenantId);
        apiKeyInfo.setUserId(tenantAdminUserId);
        apiKeyInfo.setDescription(description);
        apiKeyInfo.setEnabled(true);
        return apiKeyInfo;
    }

    private ApiKey createSimpleApiKey(String description) {
        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(tenantId);
        apiKey.setUserId(tenantAdminUserId);
        apiKey.setDescription(description);
        apiKey.setEnabled(true);
        apiKey.setValue("test-api-key-value-" + UUID.randomUUID());
        return apiKey;
    }

    private UplinkMsg getDeleteUplinkMsg(UUID uuid) throws InvalidProtocolBufferException {
        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        ApiKeyUpdateMsg.Builder apiKeyDeleteMsgBuilder = ApiKeyUpdateMsg.newBuilder();
        apiKeyDeleteMsgBuilder.setMsgType(ENTITY_DELETED_RPC_MESSAGE);
        apiKeyDeleteMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        apiKeyDeleteMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(apiKeyDeleteMsgBuilder);

        upLinkMsgBuilder.addApiKeyUpdateMsg(apiKeyDeleteMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        return upLinkMsgBuilder.build();
    }

    private UplinkMsg getUplinkMsg(UUID uuid, ApiKey apiKey, UpdateMsgType updateMsgType) throws InvalidProtocolBufferException {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        ApiKeyUpdateMsg.Builder apiKeyUpdateMsgBuilder = ApiKeyUpdateMsg.newBuilder();
        apiKeyUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        apiKeyUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        apiKeyUpdateMsgBuilder.setEntity(JacksonUtil.toString(apiKey));
        apiKeyUpdateMsgBuilder.setMsgType(updateMsgType);
        testAutoGeneratedCodeByProtobuf(apiKeyUpdateMsgBuilder);
        uplinkMsgBuilder.addApiKeyUpdateMsg(apiKeyUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        return uplinkMsgBuilder.build();
    }

    private void checkApiKeyOnCloud(UplinkMsg uplinkMsg, UUID uuid, String description) throws Exception {
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        ApiKey apiKey = apiKeyService.findApiKeyById(tenantId, new ApiKeyId(uuid));
        Assert.assertNotNull(apiKey);
        Assert.assertEquals(description, apiKey.getDescription());
    }

}
