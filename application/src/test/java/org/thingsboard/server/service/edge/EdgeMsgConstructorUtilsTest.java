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

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbSaveToCustomCassandraTableNode;
import org.thingsboard.rule.engine.action.TbSaveToCustomCassandraTableNodeConfiguration;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.aws.lambda.TbAwsLambdaNode;
import org.thingsboard.rule.engine.aws.lambda.TbAwsLambdaNodeConfiguration;
import org.thingsboard.rule.engine.rest.TbSendRestApiCallReplyNode;
import org.thingsboard.rule.engine.rest.TbSendRestApiCallReplyNodeConfiguration;
import org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode;
import org.thingsboard.rule.engine.telemetry.TbMsgAttributesNodeConfiguration;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.service.edge.EdgeMsgConstructorUtils.EXCLUDED_NODES_BY_EDGE_VERSION;
import static org.thingsboard.server.service.edge.EdgeMsgConstructorUtils.IGNORED_PARAMS_BY_EDGE_VERSION;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EdgeMsgConstructorUtilsTest {
    private static final int CONFIGURATION_VERSION = 5;

    private static final Map<NodeConfiguration, String> NODE_CONFIG_TO_NAME_MAP = Map.of(
            new TbMsgTimeseriesNodeConfiguration(), TbMsgTimeseriesNode.class.getName(),
            new TbMsgAttributesNodeConfiguration(), TbMsgAttributesNode.class.getName(),
            new TbSaveToCustomCassandraTableNodeConfiguration(), TbSaveToCustomCassandraTableNode.class.getName()
    );

    private static final Map<String, Integer> NODE_NAME_TO_CONFIG_PARAM_COUNT_MAP = Map.of(
            TbMsgTimeseriesNode.class.getName(), 3,
            TbMsgAttributesNode.class.getName(), 5,
            TbSaveToCustomCassandraTableNode.class.getName(), 3
    );


    private static final Map<NodeConfiguration, String> MISSING_NODE_CONFIGS_FOR_OLD_EDGES = Map.of(
            new TbSendRestApiCallReplyNodeConfiguration(), TbSendRestApiCallReplyNode.class.getName(),
            new TbAwsLambdaNodeConfiguration(), TbAwsLambdaNode.class.getName()
    );

    @ParameterizedTest(name = "Testing metadata update for EdgeVersion: {0}")
    @EnumSource(value = EdgeVersion.class, names = {"V_4_0_0", "V_3_9_0", "V_3_8_0", "V_3_7_0"})
    @DisplayName("Test RuleChain Metadata Update for Supported Edge Versions")
    public void testRuleChainMetadataUpdateForSupportedEdgeVersions(EdgeVersion edgeVersion) {
        // GIVEN
        RuleChainMetaData metaData = createMetadataWithNodes(NODE_CONFIG_TO_NAME_MAP);

        // WHEN
        List<RuleNode> ruleNodes = extractRuleNodesFromMetadata(metaData, edgeVersion);

        // THEN
        verifyRuleNodeConfigurations(ruleNodes, edgeVersion);
    }

    @ParameterizedTest(name = "Testing metadata with missing nodes for EdgeVersion: {0}")
    @EnumSource(value = EdgeVersion.class, names = {"V_4_0_0", "V_3_9_0", "V_3_8_0", "V_3_7_0"})
    @DisplayName("Test RuleChain Metadata with Missing Nodes for Old Edge Versions")
    public void testRuleChainMetadataWithMissingNodesForOldEdgeVersions(EdgeVersion edgeVersion) {
        // GIVEN
        RuleChainMetaData metaData = createMetadataWithNodes(MISSING_NODE_CONFIGS_FOR_OLD_EDGES);

        // WHEN
        List<RuleNode> ruleNodes = extractRuleNodesFromMetadata(metaData, edgeVersion);

        // THEN
        int expectedNodeCount = EXCLUDED_NODES_BY_EDGE_VERSION.containsKey(edgeVersion) ?
                MISSING_NODE_CONFIGS_FOR_OLD_EDGES.size() - EXCLUDED_NODES_BY_EDGE_VERSION.get(edgeVersion).size() :
                MISSING_NODE_CONFIGS_FOR_OLD_EDGES.size();
        Assertions.assertEquals(
                expectedNodeCount,
                ruleNodes.size(),
                String.format("EdgeVersion '%s' should have %d nodes, but found %d.", edgeVersion, expectedNodeCount, ruleNodes.size())
        );
    }

    private RuleChainMetaData createMetadataWithNodes(Map<NodeConfiguration, String> nodeConfigMap) {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        List<RuleNode> ruleNodes = new ArrayList<>();

        nodeConfigMap.forEach((config, nodeName) -> {
            RuleNode ruleNode = new RuleNode();
            ruleNode.setName(nodeName);
            ruleNode.setType(nodeName);
            ruleNode.setConfigurationVersion(CONFIGURATION_VERSION);
            ruleNode.setConfiguration(JacksonUtil.valueToTree(config.defaultConfiguration()));
            ruleNodes.add(ruleNode);
        });

        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);
        return ruleChainMetaData;
    }

    private List<RuleNode> extractRuleNodesFromMetadata(RuleChainMetaData metaData, EdgeVersion edgeVersion) {
        String metadataUpdateMsg = EdgeMsgConstructorUtils.constructRuleChainMetadataUpdatedMsg(
                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                metaData,
                edgeVersion
        ).getEntity();

        RuleChainMetaData updatedMetaData = JacksonUtil.fromString(metadataUpdateMsg, RuleChainMetaData.class, true);
        Assertions.assertNotNull(updatedMetaData, "RuleChainMetaData should not be null after update.");
        return updatedMetaData.getNodes();
    }

    private void verifyRuleNodeConfigurations(List<RuleNode> ruleNodes, EdgeVersion edgeVersion) {
        ruleNodes.forEach(ruleNode -> {
            String nodeType = ruleNode.getType();
            int expectedParamCount = NODE_NAME_TO_CONFIG_PARAM_COUNT_MAP.getOrDefault(nodeType, 0);

            boolean isRuleNodeModified = IGNORED_PARAMS_BY_EDGE_VERSION
                    .getOrDefault(edgeVersion, Map.of())
                    .containsKey(nodeType);

            int actualParamCount = isRuleNodeModified ? expectedParamCount - 1 : expectedParamCount;

            Assertions.assertEquals(
                    actualParamCount,
                    ruleNode.getConfiguration().size(),
                    String.format("RuleNode '%s' for EdgeVersion '%s' should have %d config parameters.",
                            ruleNode.getName(), edgeVersion, actualParamCount)
            );
        });
    }

}
