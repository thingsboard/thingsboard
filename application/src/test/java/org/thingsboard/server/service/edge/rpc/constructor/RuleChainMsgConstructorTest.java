/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RuleChainConnectionInfoProto;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleNodeProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class RuleChainMsgConstructorTest {

    private static final String RPC_CONNECTION_TYPE = "RPC";

    private RuleChainMsgConstructorV1 ruleChainMsgConstructorV1;

    private TenantId tenantId;

    @BeforeEach
    public void setup() {
        ruleChainMsgConstructorV1 = new RuleChainMsgConstructorV1();
        tenantId = new TenantId(UUID.randomUUID());
    }

    @Test
    public void testConstructRuleChainMetadataUpdatedMsg_V_3_4_0() {
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainMetaData ruleChainMetaData = createRuleChainMetaData(
                ruleChainId, 3, createRuleNodes(ruleChainId), createConnections());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                ruleChainMsgConstructorV1.constructRuleChainMetadataUpdatedMsg(
                        tenantId,
                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                        ruleChainMetaData,
                        EdgeVersion.V_3_4_0);

        assetV_3_3_3_and_V_3_4_0(ruleChainMetadataUpdateMsg);

        assertCheckpointRuleNodeConfiguration(
                ruleChainMetadataUpdateMsg.getNodesList(),
                "{\"queueName\":\"HighPriority\"}");
    }

    @Test
    public void testConstructRuleChainMetadataUpdatedMsg_V_3_3_3() {
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainMetaData ruleChainMetaData = createRuleChainMetaData(
                ruleChainId, 3, createRuleNodes(ruleChainId), createConnections());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                ruleChainMsgConstructorV1.constructRuleChainMetadataUpdatedMsg(
                        tenantId,
                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                        ruleChainMetaData,
                        EdgeVersion.V_3_3_3);

        assetV_3_3_3_and_V_3_4_0(ruleChainMetadataUpdateMsg);

        assertCheckpointRuleNodeConfiguration(
                ruleChainMetadataUpdateMsg.getNodesList(),
                "{\"queueName\":\"HighPriority\"}");
    }

    private void assetV_3_3_3_and_V_3_4_0(RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        Assertions.assertEquals(3, ruleChainMetadataUpdateMsg.getFirstNodeIndex(), "First rule node index incorrect!");
        Assertions.assertEquals(12, ruleChainMetadataUpdateMsg.getNodesCount(), "Nodes count incorrect!");
        Assertions.assertEquals(13, ruleChainMetadataUpdateMsg.getConnectionsCount(), "Connections count incorrect!");
        Assertions.assertEquals(0, ruleChainMetadataUpdateMsg.getRuleChainConnectionsCount(), "Rule chain connections count incorrect!");

        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 6, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(0));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 10, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(1));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 0, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(2));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 11, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(3));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 11, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(4));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 11, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(5));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 7, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(6));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 4, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(7));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 5, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(8));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 8, TbNodeConnectionType.OTHER), ruleChainMetadataUpdateMsg.getConnections(9));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 9, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(10));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(7, 11, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(11));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(10, 9, RPC_CONNECTION_TYPE), ruleChainMetadataUpdateMsg.getConnections(12));
    }

    @Test
    public void testConstructRuleChainMetadataUpdatedMsg_V_3_3_0() {
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainMetaData ruleChainMetaData = createRuleChainMetaData(ruleChainId, 3, createRuleNodes(ruleChainId), createConnections());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                ruleChainMsgConstructorV1.constructRuleChainMetadataUpdatedMsg(
                        tenantId,
                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                        ruleChainMetaData,
                        EdgeVersion.V_3_3_0);

        Assertions.assertEquals(2, ruleChainMetadataUpdateMsg.getFirstNodeIndex(),"First rule node index incorrect!");
        Assertions.assertEquals(10, ruleChainMetadataUpdateMsg.getNodesCount(), "Nodes count incorrect!");
        Assertions.assertEquals(10, ruleChainMetadataUpdateMsg.getConnectionsCount(),"Connections count incorrect!");
        Assertions.assertEquals(1, ruleChainMetadataUpdateMsg.getRuleChainConnectionsCount(), "Rule chain connections count incorrect!");

        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(2, 5, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(0));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 9, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(1));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 9, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(2));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 9, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(3));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 6, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(4));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 3, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(5));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 4, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(6));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 7, TbNodeConnectionType.OTHER), ruleChainMetadataUpdateMsg.getConnections(7));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 8, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(8));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 9, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(9));

        RuleChainConnectionInfoProto ruleChainConnection = ruleChainMetadataUpdateMsg.getRuleChainConnections(0);
        Assertions.assertEquals(2, ruleChainConnection.getFromIndex(), "From index incorrect!");
        Assertions.assertEquals(TbNodeConnectionType.SUCCESS, ruleChainConnection.getType(), "Type index incorrect!");
        Assertions.assertEquals("{\"description\":\"\",\"layoutX\":477,\"layoutY\":560,\"ruleChainNodeId\":\"rule-chain-node-UNDEFINED\"}",
                ruleChainConnection.getAdditionalInfo(),"Additional info incorrect!");
        Assertions.assertTrue(ruleChainConnection.getTargetRuleChainIdMSB() != 0,"Target rule chain id MSB incorrect!");
        Assertions.assertTrue(ruleChainConnection.getTargetRuleChainIdLSB() != 0,"Target rule chain id LSB incorrect!");

        assertCheckpointRuleNodeConfiguration(
                ruleChainMetadataUpdateMsg.getNodesList(),
                "{\"queueName\":\"HighPriority\"}");
    }

    @Test
    public void testConstructRuleChainMetadataUpdatedMsg_V_3_3_0_inDifferentOrder() {
        // same rule chain metadata, but different order of rule nodes
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainMetaData ruleChainMetaData1 = createRuleChainMetaData(ruleChainId, 8, createRuleNodesInDifferentOrder(ruleChainId), createConnectionsInDifferentOrder());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                ruleChainMsgConstructorV1.constructRuleChainMetadataUpdatedMsg(
                        tenantId,
                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, 
                        ruleChainMetaData1, 
                        EdgeVersion.V_3_3_0);

        Assertions.assertEquals(7, ruleChainMetadataUpdateMsg.getFirstNodeIndex(), "First rule node index incorrect!");
        Assertions.assertEquals(10, ruleChainMetadataUpdateMsg.getNodesCount(),"Nodes count incorrect!");
        Assertions.assertEquals(10, ruleChainMetadataUpdateMsg.getConnectionsCount(),"Connections count incorrect!");
        Assertions.assertEquals(1, ruleChainMetadataUpdateMsg.getRuleChainConnectionsCount(),"Rule chain connections count incorrect!");

        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(3, 0, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(0));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 0, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(1));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 3, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(2));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 6, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(3));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 5, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(4));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 2, TbNodeConnectionType.OTHER), ruleChainMetadataUpdateMsg.getConnections(5));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(4, 1, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()), ruleChainMetadataUpdateMsg.getConnections(6));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(5, 0, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(7));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(6, 0, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(8));
        compareNodeConnectionInfoAndProto(createNodeConnectionInfo(7, 4, TbNodeConnectionType.SUCCESS), ruleChainMetadataUpdateMsg.getConnections(9));

        RuleChainConnectionInfoProto ruleChainConnection = ruleChainMetadataUpdateMsg.getRuleChainConnections(0);
        Assertions.assertEquals(7, ruleChainConnection.getFromIndex(),"From index incorrect!");
        Assertions.assertEquals(TbNodeConnectionType.SUCCESS, ruleChainConnection.getType(), "Type index incorrect!");
        Assertions.assertEquals( "{\"description\":\"\",\"layoutX\":477,\"layoutY\":560,\"ruleChainNodeId\":\"rule-chain-node-UNDEFINED\"}",
                ruleChainConnection.getAdditionalInfo(), "Additional info incorrect!");
        Assertions.assertTrue(ruleChainConnection.getTargetRuleChainIdMSB() != 0, "Target rule chain id MSB incorrect!");
        Assertions.assertTrue(ruleChainConnection.getTargetRuleChainIdLSB() != 0, "Target rule chain id LSB incorrect!");

        assertCheckpointRuleNodeConfiguration(
                ruleChainMetadataUpdateMsg.getNodesList(),
                "{\"queueName\":\"HighPriority\"}");
    }

    private void assertCheckpointRuleNodeConfiguration(List<RuleNodeProto> nodesList,
                                                       String expectedConfiguration) {
        Optional<RuleNodeProto> checkpointRuleNodeOpt = nodesList.stream()
                .filter(rn -> "org.thingsboard.rule.engine.flow.TbCheckpointNode".equals(rn.getType()))
                .findFirst();
        Assertions.assertTrue(checkpointRuleNodeOpt.isPresent());
        RuleNodeProto checkpointRuleNode = checkpointRuleNodeOpt.get();
        Assertions.assertEquals(expectedConfiguration, checkpointRuleNode.getConfiguration());
    }

    private void compareNodeConnectionInfoAndProto(NodeConnectionInfo expected, org.thingsboard.server.gen.edge.v1.NodeConnectionInfoProto actual) {
        Assertions.assertEquals(expected.getFromIndex(), actual.getFromIndex());
        Assertions.assertEquals(expected.getToIndex(), actual.getToIndex());
        Assertions.assertEquals(expected.getType(), actual.getType());
    }

    private RuleChainMetaData createRuleChainMetaData(RuleChainId ruleChainId, Integer firstNodeIndex, List<RuleNode> nodes, List<NodeConnectionInfo> connections) {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChainId);
        ruleChainMetaData.setFirstNodeIndex(firstNodeIndex);
        ruleChainMetaData.setNodes(nodes);
        ruleChainMetaData.setConnections(connections);
        ruleChainMetaData.setRuleChainConnections(null);
        return ruleChainMetaData;
    }

    private List<NodeConnectionInfo> createConnections() {
        List<NodeConnectionInfo> result = new ArrayList<>();
        result.add(createNodeConnectionInfo(3, 6, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(3, 10, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(3, 0, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(4, 11, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(5, 11, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(6, 11, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 7, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 4, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 5, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 8, TbNodeConnectionType.OTHER));
        result.add(createNodeConnectionInfo(6, 9, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(7, 11, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(10, 9, RPC_CONNECTION_TYPE));
        return result;
    }

    private NodeConnectionInfo createNodeConnectionInfo(int fromIndex, int toIndex, String type) {
        NodeConnectionInfo result = new NodeConnectionInfo();
        result.setFromIndex(fromIndex);
        result.setToIndex(toIndex);
        result.setType(type);
        return result;
    }

    private List<RuleNode> createRuleNodes(RuleChainId ruleChainId) {
        List<RuleNode> result = new ArrayList<>();
        result.add(getOutputNode(ruleChainId));
        result.add(getAcknowledgeNode(ruleChainId));
        result.add(getCheckpointNode(ruleChainId));
        result.add(getDeviceProfileNode(ruleChainId));
        result.add(getSaveTimeSeriesNode(ruleChainId));
        result.add(getSaveClientAttributesNode(ruleChainId));
        result.add(getMessageTypeSwitchNode(ruleChainId));
        result.add(getLogRpcFromDeviceNode(ruleChainId));
        result.add(getLogOtherNode(ruleChainId));
        result.add(getRpcCallRequestNode(ruleChainId));
        result.add(getPushToAnalyticsNode(ruleChainId));
        result.add(getPushToCloudNode(ruleChainId));
        return result;
    }

    private RuleNode createRuleNode(RuleChainId ruleChainId, String type, String name, JsonNode configuration, JsonNode additionalInfo) {
        RuleNode e = new RuleNode();
        e.setRuleChainId(ruleChainId);
        e.setType(type);
        e.setName(name);
        e.setDebugMode(false);
        e.setConfiguration(configuration);
        e.setAdditionalInfo(additionalInfo);
        e.setId(new RuleNodeId(UUID.randomUUID()));
        return e;
    }

    private List<NodeConnectionInfo> createConnectionsInDifferentOrder() {
        List<NodeConnectionInfo> result = new ArrayList<>();
        result.add(createNodeConnectionInfo(0, 2, RPC_CONNECTION_TYPE));
        result.add(createNodeConnectionInfo(4, 1, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(5, 1, TbMsgType.ATTRIBUTES_UPDATED.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(5, 4, TbMsgType.TO_SERVER_RPC_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(5, 7, TbMsgType.POST_TELEMETRY_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(5, 6, TbMsgType.POST_ATTRIBUTES_REQUEST.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(5, 3, TbNodeConnectionType.OTHER));
        result.add(createNodeConnectionInfo(5, 2, TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE.getRuleNodeConnection()));
        result.add(createNodeConnectionInfo(6, 1, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(7, 1, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(8, 11, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(8, 5, TbNodeConnectionType.SUCCESS));
        result.add(createNodeConnectionInfo(8, 0, TbNodeConnectionType.SUCCESS));
        return result;
    }

    private List<RuleNode> createRuleNodesInDifferentOrder(RuleChainId ruleChainId) {
        List<RuleNode> result = new ArrayList<>();
        result.add(getPushToAnalyticsNode(ruleChainId));
        result.add(getPushToCloudNode(ruleChainId));
        result.add(getRpcCallRequestNode(ruleChainId));
        result.add(getLogOtherNode(ruleChainId));
        result.add(getLogRpcFromDeviceNode(ruleChainId));
        result.add(getMessageTypeSwitchNode(ruleChainId));
        result.add(getSaveClientAttributesNode(ruleChainId));
        result.add(getSaveTimeSeriesNode(ruleChainId));
        result.add(getDeviceProfileNode(ruleChainId));
        result.add(getCheckpointNode(ruleChainId));
        result.add(getAcknowledgeNode(ruleChainId));
        result.add(getOutputNode(ruleChainId));
        return result;
    }


    private RuleNode getOutputNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbRuleChainOutputNode",
                "Output node",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":178,\"layoutY\":592}"));
    }

    private RuleNode getCheckpointNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbCheckpointNode",
                "Checkpoint node",
                JacksonUtil.toJsonNode("{\"queueName\":\"HighPriority\"}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":178,\"layoutY\":647}"));
    }

    private RuleNode getSaveTimeSeriesNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode",
                "Save Timeseries",
                JacksonUtil.toJsonNode("{\"defaultTTL\":0}"),
                JacksonUtil.toJsonNode("{\"layoutX\":823,\"layoutY\":157}"));
    }

    private RuleNode getMessageTypeSwitchNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode",
                "Message Type Switch",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"layoutX\":347,\"layoutY\":149}"));
    }

    private RuleNode getLogOtherNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.action.TbLogNode",
                "Log Other",
                JacksonUtil.toJsonNode("{\"jsScript\":\"return '\\\\nIncoming message:\\\\n' + JSON.stringify(msg) + '\\\\nIncoming metadata:\\\\n' + JSON.stringify(metadata);\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":378}"));
    }

    private RuleNode getPushToCloudNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.edge.TbMsgPushToCloudNode",
                "Push to cloud",
                JacksonUtil.toJsonNode("{\"scope\":\"SERVER_SCOPE\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":1129,\"layoutY\":52}"));
    }

    private RuleNode getAcknowledgeNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbAckNode",
                "Acknowledge node",
                JacksonUtil.toJsonNode("{\"version\":0}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":177,\"layoutY\":703}"));
    }

    private RuleNode getDeviceProfileNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.profile.TbDeviceProfileNode",
                "Device Profile Node",
                JacksonUtil.toJsonNode("{\"persistAlarmRulesState\":false,\"fetchAlarmRulesStateOnStart\":false}"),
                JacksonUtil.toJsonNode("{\"description\":\"Process incoming messages from devices with the alarm rules defined in the device profile. Dispatch all incoming messages with \\\"Success\\\" relation type.\",\"layoutX\":187,\"layoutY\":468}"));
    }

    private RuleNode getSaveClientAttributesNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode",
                "Save Client Attributes",
                JacksonUtil.toJsonNode("{\"scope\":\"CLIENT_SCOPE\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":52}"));
    }

    private RuleNode getLogRpcFromDeviceNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.action.TbLogNode",
                "Log RPC from Device",
                JacksonUtil.toJsonNode("{\"jsScript\":\"return '\\\\nIncoming message:\\\\n' + JSON.stringify(msg) + '\\\\nIncoming metadata:\\\\n' + JSON.stringify(metadata);\"}"),
                JacksonUtil.toJsonNode("{\"layoutX\":825,\"layoutY\":266}"));
    }

    private RuleNode getRpcCallRequestNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode",
                "RPC Call Request",
                JacksonUtil.toJsonNode("{\"timeoutInSeconds\":60}"),
                JacksonUtil.toJsonNode("{\"layoutX\":824,\"layoutY\":466}"));
    }

    private RuleNode getPushToAnalyticsNode(RuleChainId ruleChainId) {
        return createRuleNode(ruleChainId,
                "org.thingsboard.rule.engine.flow.TbRuleChainInputNode",
                "Push to Analytics",
                JacksonUtil.toJsonNode("{\"ruleChainId\":\"af588000-6c7c-11ec-bafd-c9a47a5c8d99\"}"),
                JacksonUtil.toJsonNode("{\"description\":\"\",\"layoutX\":477,\"layoutY\":560}"));
    }
}
