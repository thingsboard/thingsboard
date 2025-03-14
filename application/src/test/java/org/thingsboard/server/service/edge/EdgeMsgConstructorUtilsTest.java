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
package org.thingsboard.server.service.edge;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

import java.util.Collections;

public class EdgeMsgConstructorUtilsTest {
    private static final int CONFIGURATION_VERSION = 5;

    @Test
    public void testRuleChainMetadataUpdateMsgForAllEdgeVersions() {
        // GIVEN
        RuleChainMetaData metaData = createIncompatibleRuleNodesForOldEdge();

        // WHEN
        RuleNode ruleNode_V_4_0_0 = getRuleNodeFromMetadataUpdateMessage(metaData, EdgeVersion.V_4_0_0);
        RuleNode ruleNode_V_3_9_0 = getRuleNodeFromMetadataUpdateMessage(metaData, EdgeVersion.V_3_9_0);
        RuleNode ruleNode_V_3_8_0 = getRuleNodeFromMetadataUpdateMessage(metaData, EdgeVersion.V_3_8_0);
        RuleNode ruleNode_V_3_7_0 = getRuleNodeFromMetadataUpdateMessage(metaData, EdgeVersion.V_3_7_0);

        // THEN
        assertRuleNodeConfiguration(ruleNode_V_4_0_0, EdgeVersion.V_4_0_0);
        assertRuleNodeConfiguration(ruleNode_V_3_9_0, EdgeVersion.V_3_9_0);
        assertRuleNodeConfiguration(ruleNode_V_3_8_0, EdgeVersion.V_3_8_0);
        assertRuleNodeConfiguration(ruleNode_V_3_7_0, EdgeVersion.V_3_7_0);
    }

    private RuleChainMetaData createIncompatibleRuleNodesForOldEdge() {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("TbMsgTimeseriesNode");
        ruleNode1.setType(org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode.class.getName());
        ruleNode1.setConfigurationVersion(CONFIGURATION_VERSION);
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(new TbMsgTimeseriesNodeConfiguration().defaultConfiguration()));

        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(Collections.singletonList(ruleNode1));

        return ruleChainMetaData;
    }


    private RuleNode getRuleNodeFromMetadataUpdateMessage(RuleChainMetaData metaData, EdgeVersion edgeVersion) {
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                EdgeMsgConstructorUtils.constructRuleChainMetadataUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, metaData, edgeVersion);

        RuleChainMetaData ruleChainMetaData = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
        Assert.assertNotNull("RuleChainMetaData is null", ruleChainMetaData);

        RuleNode ruleNode = ruleChainMetaData.getNodes().stream().findFirst().orElse(null);
        Assert.assertNotNull("RuleNode is null for Edge version " + edgeVersion, ruleNode);
        Assert.assertNotNull("Configuration is null for Edge version " + edgeVersion, ruleNode.getConfiguration());

        return ruleNode;
    }

    private void assertRuleNodeConfiguration(RuleNode ruleNode, EdgeVersion edgeVersion) {
        if (EdgeVersionUtils.isEdgeVersionOlderThan(edgeVersion, EdgeVersion.V_3_9_0)) {
            Assert.assertEquals("Unexpected config size", 2, ruleNode.getConfiguration().size());
            Assert.assertFalse("Unexpected field 'processingSettings'", ruleNode.getConfiguration().has("processingSettings"));
        }else{
            Assert.assertEquals("Unexpected config size", 3, ruleNode.getConfiguration().size());
            Assert.assertTrue("Missing field 'processingSettings'", ruleNode.getConfiguration().has("processingSettings"));
        }
    }
}
