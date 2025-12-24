/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.gen.edge.v1.UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;

@DaoSqlTest
public class AiModelEdgeTest extends AbstractEdgeTest {

    private static final String DEFAULT_AI_MODEL_NAME = "Edge Test AiModel";
    private static final String UPDATED_AI_MODEL_NAME = "Updated Edge Test AiModel";

    @Test
    public void testAiModel_create_update_delete_fromCloud() throws Exception {
        // create AiModel
        AiModel aiModel = createSimpleAiModel(DEFAULT_AI_MODEL_NAME);

        edgeImitator.expectMessageAmount(1);
        AiModel savedAiModel = doPost("/api/ai/model", aiModel, AiModel.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AiModelUpdateMsg);
        AiModelUpdateMsg aiModelUpdateMsg = (AiModelUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, aiModelUpdateMsg.getMsgType());
        Assert.assertEquals(savedAiModel.getUuidId().getMostSignificantBits(), aiModelUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAiModel.getUuidId().getLeastSignificantBits(), aiModelUpdateMsg.getIdLSB());
        AiModel aiModelFromMsg = JacksonUtil.fromString(aiModelUpdateMsg.getEntity(), AiModel.class, true);
        Assert.assertNotNull(aiModelFromMsg);

        Assert.assertEquals(DEFAULT_AI_MODEL_NAME, aiModelFromMsg.getName());
        Assert.assertEquals(savedAiModel.getTenantId(), aiModelFromMsg.getTenantId());

        // update AiModel
        edgeImitator.expectMessageAmount(1);
        savedAiModel.setName(UPDATED_AI_MODEL_NAME);
        savedAiModel = doPost("/api/ai/model", savedAiModel, AiModel.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AiModelUpdateMsg);
        aiModelUpdateMsg = (AiModelUpdateMsg) latestMessage;
        aiModelFromMsg = JacksonUtil.fromString(aiModelUpdateMsg.getEntity(), AiModel.class, true);
        Assert.assertNotNull(aiModelFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, aiModelUpdateMsg.getMsgType());
        Assert.assertEquals(UPDATED_AI_MODEL_NAME, aiModelFromMsg.getName());

        // delete AiModel
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ai/model/" + savedAiModel.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AiModelUpdateMsg);
        aiModelUpdateMsg = (AiModelUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, aiModelUpdateMsg.getMsgType());
        Assert.assertEquals(savedAiModel.getUuidId().getMostSignificantBits(), aiModelUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAiModel.getUuidId().getLeastSignificantBits(), aiModelUpdateMsg.getIdLSB());
    }

    @Test
    public void testAiModel_create_update_delete_toCloud() throws Exception {
        // create
        AiModel aiModel = createSimpleAiModel(DEFAULT_AI_MODEL_NAME);
        UUID uuid = Uuids.timeBased();
        UplinkMsg uplinkMsg = getUplinkMsg(uuid, aiModel, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        checkAiModelOnCloud(uplinkMsg, uuid, aiModel.getName());

        // update
        aiModel.setName(UPDATED_AI_MODEL_NAME);
        UplinkMsg updatedUplinkMsg = getUplinkMsg(uuid, aiModel, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);

        checkAiModelOnCloud(updatedUplinkMsg, uuid, aiModel.getName());

        // delete
        UplinkMsg deleteUplinkMsg = getDeleteUplinkMsg(uuid);
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(deleteUplinkMsg);
        Assert.assertTrue(edgeImitator.waitForResponses());

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                doGet("/api/ai/model/" + uuid, AiModel.class, status().isNotFound())
        );
    }

    @Test
    public void testAiModelToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        AiModel aiModel = createSimpleAiModel(DEFAULT_AI_MODEL_NAME);

        edgeImitator.expectMessageAmount(1);
        AiModel savedAiModel = doPost("/api/ai/model", aiModel, AiModel.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UUID uuid = Uuids.timeBased();

        UplinkMsg uplinkMsg = getUplinkMsg(uuid, aiModel, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<AiModelUpdateMsg> aiModelUpdateMsgOpt = edgeImitator.findMessageByType(AiModelUpdateMsg.class);
        Assert.assertTrue(aiModelUpdateMsgOpt.isPresent());
        AiModelUpdateMsg latestAiModelUpdateMsg = aiModelUpdateMsgOpt.get();
        AiModel aiModelFromMsg = JacksonUtil.fromString(latestAiModelUpdateMsg.getEntity(), AiModel.class, true);
        Assert.assertNotNull(aiModelFromMsg);
        Assert.assertNotEquals(DEFAULT_AI_MODEL_NAME, aiModelFromMsg.getName());

        Assert.assertNotEquals(savedAiModel.getUuidId(), uuid);

        AiModel aiModelFromCloud = doGet("/api/ai/model/" + uuid, AiModel.class);
        Assert.assertNotNull(aiModelFromCloud);
        Assert.assertNotEquals(DEFAULT_AI_MODEL_NAME, aiModelFromCloud.getName());
    }

    private AiModel createSimpleAiModel(String name) {
        AiModel aiModel = new AiModel();
        aiModel.setTenantId(tenantId);
        aiModel.setName(name);
        aiModel.setConfiguration(OpenAiChatModelConfig.builder()
                .providerConfig(new OpenAiProviderConfig(null, "test-api-key"))
                .modelId("gpt-4o")
                .temperature(0.5)
                .topP(0.3)
                .frequencyPenalty(0.1)
                .presencePenalty(0.2)
                .maxOutputTokens(1000)
                .timeoutSeconds(60)
                .maxRetries(2)
                .build());
        return aiModel;
    }

    private UplinkMsg getDeleteUplinkMsg(UUID uuid) throws InvalidProtocolBufferException {
        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        AiModelUpdateMsg.Builder aiModelDeleteMsgBuilder = AiModelUpdateMsg.newBuilder();
        aiModelDeleteMsgBuilder.setMsgType(ENTITY_DELETED_RPC_MESSAGE);
        aiModelDeleteMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        aiModelDeleteMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(aiModelDeleteMsgBuilder);

        upLinkMsgBuilder.addAiModelUpdateMsg(aiModelDeleteMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        return upLinkMsgBuilder.build();
    }

    private UplinkMsg getUplinkMsg(UUID uuid, AiModel aiModel, UpdateMsgType updateMsgType) throws InvalidProtocolBufferException {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AiModelUpdateMsg.Builder aiModelUpdateMsgBuilder = AiModelUpdateMsg.newBuilder();
        aiModelUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        aiModelUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        aiModelUpdateMsgBuilder.setEntity(JacksonUtil.toString(aiModel));
        aiModelUpdateMsgBuilder.setMsgType(updateMsgType);
        testAutoGeneratedCodeByProtobuf(aiModelUpdateMsgBuilder);
        uplinkMsgBuilder.addAiModelUpdateMsg(aiModelUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        return uplinkMsgBuilder.build();
    }

    private void checkAiModelOnCloud(UplinkMsg uplinkMsg, UUID uuid, String resourceTitle) throws Exception {
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        AiModel aiModel = doGet("/api/ai/model/" + uuid, AiModel.class);
        Assert.assertNotNull(aiModel);
        Assert.assertEquals(resourceTitle, aiModel.getName());
    }

}
