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
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RuleChainEdgeTest extends AbstractEdgeTest {

    private static final int CONFIGURATION_VERSION = 5;

    @Test
    public void testRuleChains() throws Exception {
        // create rule chain: 2 messages from create rule chain, 2 messages from load metadata
        edgeImitator.expectMessageAmount(4);
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        createRuleChainMetadata(savedRuleChain);
        Assert.assertTrue(edgeImitator.waitForMessages());
        List<RuleChainUpdateMsg> ruleChainUpdateMsgs = edgeImitator.findAllMessagesByType(RuleChainUpdateMsg.class);
        Assert.assertEquals(2, ruleChainUpdateMsgs.size());
        List<RuleChainMetadataUpdateMsg> ruleChainMetadataUpdateMsgs = edgeImitator.findAllMessagesByType(RuleChainMetadataUpdateMsg.class);
        Assert.assertEquals(2, ruleChainMetadataUpdateMsgs.size());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgs.get(0);
        RuleChain ruleChainMsg = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
        Assert.assertNotNull(ruleChainMsg);
        Assert.assertTrue(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()));
        Assert.assertEquals(savedRuleChain.getId(), ruleChainMsg.getId());
        Assert.assertEquals(savedRuleChain.getName(), ruleChainMsg.getName());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = ruleChainMetadataUpdateMsgs.get(0);
        RuleChainMetaData ruleChainMetaData = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
        Assert.assertNotNull(ruleChainMetaData);
        Assert.assertEquals(ruleChainMetaData.getRuleChainId(), savedRuleChain.getId());
        for (RuleNode ruleNode : ruleChainMetaData.getNodes()) {
            Assert.assertEquals(CONFIGURATION_VERSION, ruleNode.getConfigurationVersion());
        }

        // unassign rule chain from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), savedRuleChain.getUuidId().getMostSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), savedRuleChain.getUuidId().getLeastSignificantBits());

        // delete rule chain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ruleChain/" + savedRuleChain.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(5));
    }

    @Test
    public void testRuleChainToCloud() throws Exception {
        String ruleChainName = "Rule Chain Edge";
        UUID uuid = Uuids.timeBased();

        // create rule chain on edge
        RuleChain edgeRuleChain = new RuleChain();
        edgeRuleChain.setTenantId(tenantId);
        edgeRuleChain.setId(new RuleChainId(uuid));
        edgeRuleChain.setName(ruleChainName);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RuleChainUpdateMsg.Builder ruleChainUpdateMsgBuilder = RuleChainUpdateMsg.newBuilder();
        ruleChainUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        ruleChainUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        ruleChainUpdateMsgBuilder.setEntity(JacksonUtil.toString(edgeRuleChain));
        ruleChainUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsgBuilder);
        uplinkMsgBuilder.addRuleChainUpdateMsg(ruleChainUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        RuleChain ruleChain = doGet("/api/ruleChain/" + uuid, RuleChain.class);
        Assert.assertNotNull(ruleChain);
        Assert.assertEquals("Rule Chain Edge", ruleChain.getName());

        // update rule chain on edge
        edgeRuleChain.setName(ruleChainName + " Updated");
        uplinkMsgBuilder = UplinkMsg.newBuilder();
        ruleChainUpdateMsgBuilder = RuleChainUpdateMsg.newBuilder();
        ruleChainUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        ruleChainUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        ruleChainUpdateMsgBuilder.setEntity(JacksonUtil.toString(edgeRuleChain));
        ruleChainUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsgBuilder);
        uplinkMsgBuilder.addRuleChainUpdateMsg(ruleChainUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        ruleChain = doGet("/api/ruleChain/" + uuid, RuleChain.class);
        Assert.assertNotNull(ruleChain);
        Assert.assertEquals(ruleChainName + " Updated", ruleChain.getName());
    }

    private RuleChainMetaData createRuleChainMetadata(RuleChain ruleChain) {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(CONFIGURATION_VERSION);
        TbGetAttributesNodeConfiguration configuration = new TbGetAttributesNodeConfiguration();
        configuration.setFetchTo(TbMsgSource.METADATA);
        configuration.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(CONFIGURATION_VERSION);
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode3.setConfigurationVersion(CONFIGURATION_VERSION);
        ruleNode3.setConfiguration(JacksonUtil.valueToTree(configuration));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0, 1, "success");
        ruleChainMetaData.addConnectionInfo(0, 2, "fail");
        ruleChainMetaData.addConnectionInfo(1, 2, "success");

        return doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
    }

    @Test
    public void testUpdateRootRuleChain() throws Exception {
        edgeImitator.expectMessageAmount(2);
        updateRootRuleChainMetadata();
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        Optional<RuleChainMetadataUpdateMsg> ruleChainMetadataUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainMetadataUpdateMsg.class);
        Assert.assertTrue(ruleChainMetadataUpdateMsgOpt.isPresent());
    }

    @Test
    public void testSetRootRuleChain() throws Exception {
        // create rule chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge New Root Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        edgeImitator.expectMessageAmount(4);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        RuleChainMetaData metaData = createRuleChainMetadata(savedRuleChain);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // set new rule chain as root
        RuleChainId currentRootRuleChainId = edge.getRootRuleChainId();
        edgeImitator.expectMessageAmount(2);
        doPost("/api/edge/" + edge.getUuidId()
                + "/" + savedRuleChain.getUuidId() + "/root", Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        RuleChain ruleChainMsg = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
        Assert.assertNotNull(ruleChainMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertTrue(ruleChainMsg.isRoot());
        Assert.assertEquals(savedRuleChain.getId(), ruleChainMsg.getId());

        // update metadata for root rule chain
        edgeImitator.expectMessageAmount(1);
        metaData.getNodes().forEach(n -> n.setDebugSettings(DebugSettings.all()));
        doPost("/api/ruleChain/metadata", metaData, RuleChainMetaData.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        ruleChainMsg = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
        Assert.assertNotNull(ruleChainMsg);
        Assert.assertTrue(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()));
        Assert.assertEquals(savedRuleChain.getId(), ruleChainMsg.getId());
        Assert.assertEquals(savedRuleChain.getName(), ruleChainMsg.getName());
        Assert.assertTrue(ruleChainMsg.isRoot());

        // revert root rule chain
        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/" + currentRootRuleChainId.getId() + "/root", Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // unassign rule chain from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // delete rule chain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ruleChain/" + savedRuleChain.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(5));
    }

}
