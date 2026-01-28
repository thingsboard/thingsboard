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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbSaveToCustomCassandraTableNode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.aws.lambda.TbAwsLambdaNode;
import org.thingsboard.rule.engine.filter.TbCheckRelationNode;
import org.thingsboard.rule.engine.flow.TbAckNode;
import org.thingsboard.rule.engine.math.TbMathNode;
import org.thingsboard.rule.engine.metadata.CalculateDeltaNode;
import org.thingsboard.rule.engine.metadata.TbGetTelemetryNode;
import org.thingsboard.rule.engine.rest.TbSendRestApiCallReplyNode;
import org.thingsboard.rule.engine.telemetry.TbCalculatedFieldsNode;
import org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.thingsboard.server.service.edge.EdgeMsgConstructorUtils.EXCLUDED_NODES_BY_EDGE_VERSION;
import static org.thingsboard.server.service.edge.EdgeMsgConstructorUtils.IGNORED_PARAMS_BY_EDGE_VERSION;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EdgeMsgConstructorUtilsTest {

    private static final int CONFIGURATION_VERSION = 5;

    static Stream<EdgeVersion> provideEdgeVersions() {
        return Stream.of(
                EdgeVersion.V_4_0_0,
                EdgeVersion.V_3_9_0,
                EdgeVersion.V_3_8_0,
                EdgeVersion.V_3_7_0
        );
    }

    private static final RuleChainMetaData RULE_CHAIN_META_DATA = new RuleChainMetaData();
    private static final List<TbNode> TEST_NODES =
            List.of(
                    new TbSaveToCustomCassandraTableNode(),
                    new TbMsgAttributesNode(),
                    new TbMsgTimeseriesNode(),
                    new TbSendRestApiCallReplyNode(),
                    new TbAwsLambdaNode(),
                    new TbCalculatedFieldsNode(),

                    new TbMathNode(),
                    new CalculateDeltaNode(),
                    new TbAckNode(),
                    new TbCheckRelationNode(),
                    new TbGetTelemetryNode()
            );

    @BeforeAll
    static void setUp() {
        List<RuleNode> ruleNodes = TEST_NODES.stream()
                .map(node -> {
                    RuleNode ruleNode = new RuleNode();
                    ruleNode.setName(node.getClass().getName());
                    ruleNode.setType(node.getClass().getName());
                    ruleNode.setConfigurationVersion(CONFIGURATION_VERSION);
                    ruleNode.setConfiguration(JacksonUtil.valueToTree(createDefaultConfiguration(node)));
                    return ruleNode;
                })
                .toList();

        RULE_CHAIN_META_DATA.setFirstNodeIndex(0);
        RULE_CHAIN_META_DATA.setNodes(ruleNodes);
    }

    private static NodeConfiguration<?> createDefaultConfiguration(TbNode node) {
        try {
            org.thingsboard.rule.engine.api.RuleNode annotation = node.getClass().getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class);
            Constructor<?> constructor = annotation.configClazz().getConstructor();
            NodeConfiguration<?> configInstance = (NodeConfiguration<?>) constructor.newInstance();

            return configInstance.defaultConfiguration();
        } catch (Exception e) {
            throw new RuntimeException("Exception during creating RuleNodeConfiguration for node - " + node, e);
        }
    }

    @ParameterizedTest(name = "Test Sanitize Metadata For Edge: {0}")
    @MethodSource("provideEdgeVersions")
    @DisplayName("Test Sanitize Metadata For Legacy Edge Version")
    public void testSanitizeMetadataForLegacyEdgeVersion(EdgeVersion edgeVersion) {
        // WHEN
        List<RuleNode> ruleNodes = sanitizeMetadataForLegacyEdgeVersion(edgeVersion);

        // THEN
        ruleNodes.forEach(ruleNode -> {
            checkUpdateNodeConfigurationsForLegacyEdge(ruleNode, edgeVersion);
            checkRemoveExcludedNodesForLegacyEdge(ruleNode, edgeVersion);
        });
    }

    private List<RuleNode> sanitizeMetadataForLegacyEdgeVersion(EdgeVersion edgeVersion) {
        String metadataUpdateMsg = EdgeMsgConstructorUtils.constructRuleChainMetadataUpdatedMsg(
                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                RULE_CHAIN_META_DATA,
                edgeVersion
        ).getEntity();

        RuleChainMetaData updatedMetaData = JacksonUtil.fromString(metadataUpdateMsg, RuleChainMetaData.class, true);
        Assertions.assertNotNull(updatedMetaData, "RuleChainMetaData should not be null after update.");

        return updatedMetaData.getNodes();
    }

    private void checkUpdateNodeConfigurationsForLegacyEdge(RuleNode ruleNode, EdgeVersion edgeVersion) {
        if (IGNORED_PARAMS_BY_EDGE_VERSION.containsKey(edgeVersion) && IGNORED_PARAMS_BY_EDGE_VERSION.get(edgeVersion).containsKey(ruleNode.getType())) {
            String ignoredParam = IGNORED_PARAMS_BY_EDGE_VERSION.get(edgeVersion).get(ruleNode.getType());

            Assertions.assertFalse(ruleNode.getConfiguration().has(ignoredParam),
                    String.format("RuleNode '%s' for EdgeVersion '%s' should ignore '%s' config parameter.", ruleNode.getName(), edgeVersion, ignoredParam));
        }
    }

    private void checkRemoveExcludedNodesForLegacyEdge(RuleNode ruleNode, EdgeVersion edgeVersion) {
        boolean isNodeExcluded = Optional.ofNullable(EXCLUDED_NODES_BY_EDGE_VERSION.get(edgeVersion))
                .map(excludedNodes -> !excludedNodes.contains(ruleNode.getType()))
                .orElse(true);

        Assertions.assertTrue(isNodeExcluded,
                String.format("For EdgeVersion '%s', ruleNode '%s' should not be included.", edgeVersion, ruleNode.getType()));
    }

    @Test
    @DisplayName("mergeDownlinkDuplicates: latest per attribute key is retained and duplicates removed")
    public void testMergeDownlinkDuplicates() {
        UUID deviceId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

        var deviceAttrUpdate1 = createEdgeEvent(tenantId, 1, EdgeEventActionType.ATTRIBUTES_UPDATED,
                deviceId, EdgeEventType.DEVICE, createAttrBody(1_000L, "{\"a\":1,\"b\":1,\"d\":1}"));
        var deviceAttrUpdate2 = createEdgeEvent(tenantId, 2, EdgeEventActionType.ATTRIBUTES_UPDATED,
                deviceId, EdgeEventType.DEVICE, createAttrBody(2_000L, "{\"a\":2,\"b\":2,\"c\":2}"));
        var deviceAttrUpdate3 = createEdgeEvent(tenantId, 3, EdgeEventActionType.ATTRIBUTES_UPDATED,
                deviceId, EdgeEventType.DEVICE, createAttrBody(3_000L, "{\"a\":3,\"d\":3}"));

        var deviceUpdate = createEdgeEvent(tenantId, 4, EdgeEventActionType.UPDATED,
                deviceId, EdgeEventType.DEVICE, null);
        var deviceUpdateDup = createEdgeEvent(tenantId, 5, EdgeEventActionType.UPDATED,
                deviceId, EdgeEventType.DEVICE, null);

        var assetAttrUpdate1 = createEdgeEvent(tenantId, 6, EdgeEventActionType.ATTRIBUTES_UPDATED,
                assetId, EdgeEventType.ASSET, createAttrBody(6_000L, "{\"a\":6,\"d\":6}"));
        var assetAttrUpdate2 = createEdgeEvent(tenantId, 7, EdgeEventActionType.ATTRIBUTES_UPDATED,
                assetId, EdgeEventType.ASSET, createAttrBody(7_000L, "{\"a\":7,\"b\":7,\"c\":7}"));
        var assetAttrUpdate3 = createEdgeEvent(tenantId, 8, EdgeEventActionType.ATTRIBUTES_UPDATED,
                assetId, EdgeEventType.ASSET, createAttrBody(8_000L, "{\"a\":8,\"d\":8}"));

        List<EdgeEvent> input = List.of(deviceAttrUpdate1, deviceAttrUpdate2, deviceAttrUpdate3,
                deviceUpdate, deviceUpdateDup,
                assetAttrUpdate1, assetAttrUpdate2, assetAttrUpdate3);
        List<EdgeEvent> merged = EdgeMsgConstructorUtils.mergeAndFilterDownlinkDuplicates(input);

        Assertions.assertEquals(5, merged.size());

        EdgeEvent deviceMergedAttrBC = merged.get(0);
        Assertions.assertEquals(2, deviceMergedAttrBC.getSeqId());
        Assertions.assertEquals(deviceId, deviceMergedAttrBC.getEntityId());
        Assertions.assertEquals(2_000L, deviceMergedAttrBC.getBody().get("ts").asLong());
        Assertions.assertEquals(2, getIntValue(deviceMergedAttrBC.getBody(), "b"));
        Assertions.assertEquals(2, getIntValue(deviceMergedAttrBC.getBody(), "c"));
        Assertions.assertNull(getIntValue(deviceMergedAttrBC.getBody(), "a"));

        EdgeEvent deviceMergedAttrAD = merged.get(1);
        Assertions.assertEquals(3, deviceMergedAttrAD.getSeqId());
        Assertions.assertEquals(deviceId, deviceMergedAttrAD.getEntityId());
        Assertions.assertEquals(3_000L, deviceMergedAttrAD.getBody().get("ts").asLong());
        Assertions.assertEquals(3, getIntValue(deviceMergedAttrAD.getBody(), "a"));
        Assertions.assertEquals(3, getIntValue(deviceMergedAttrAD.getBody(), "d"));

        EdgeEvent mergedDeviceUpdate = merged.get(2);
        Assertions.assertEquals(4, mergedDeviceUpdate.getSeqId());
        Assertions.assertEquals(EdgeEventActionType.UPDATED, mergedDeviceUpdate.getAction());

        EdgeEvent assetMergedAttrBC = merged.get(3);
        Assertions.assertEquals(7, assetMergedAttrBC.getSeqId());
        Assertions.assertEquals(assetId, assetMergedAttrBC.getEntityId());
        Assertions.assertEquals(7_000L, assetMergedAttrBC.getBody().get("ts").asLong());
        Assertions.assertEquals(7, getIntValue(assetMergedAttrBC.getBody(), "b"));
        Assertions.assertEquals(7, getIntValue(assetMergedAttrBC.getBody(), "c"));
        Assertions.assertNull(getIntValue(assetMergedAttrBC.getBody(), "a"));

        EdgeEvent assetMergedAttrAD = merged.get(4);
        Assertions.assertEquals(8, assetMergedAttrAD.getSeqId());
        Assertions.assertEquals(assetId, assetMergedAttrAD.getEntityId());
        Assertions.assertEquals(8_000L, assetMergedAttrAD.getBody().get("ts").asLong());
        Assertions.assertEquals(8, getIntValue(assetMergedAttrAD.getBody(), "a"));
        Assertions.assertEquals(8, getIntValue(assetMergedAttrAD.getBody(), "d"));
    }

    @Test
    public void testMergeDownlinkDuplicates_attrBodyHasNoTs_returnOriginalList() {
        UUID deviceId = UUID.randomUUID();
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

        var deviceAttrUpdate1 = createEdgeEvent(tenantId, 1, EdgeEventActionType.ATTRIBUTES_UPDATED,
                deviceId, EdgeEventType.DEVICE, createAttrBodyWithoutTs("{\"a\":1,\"b\":1,\"d\":1}"));
        var deviceAttrUpdate2 = createEdgeEvent(tenantId, 2, EdgeEventActionType.ATTRIBUTES_UPDATED,
                deviceId, EdgeEventType.DEVICE, createAttrBodyWithoutTs("{\"a\":2,\"b\":2,\"c\":2}"));
        var deviceAttrUpdate3 = createEdgeEvent(tenantId, 3, EdgeEventActionType.ATTRIBUTES_UPDATED,
                deviceId, EdgeEventType.DEVICE, createAttrBodyWithoutTs("{\"a\":3,\"d\":3}"));

        List<EdgeEvent> input = List.of(deviceAttrUpdate1, deviceAttrUpdate2, deviceAttrUpdate3);
        List<EdgeEvent> merged = EdgeMsgConstructorUtils.mergeAndFilterDownlinkDuplicates(input);

        Assertions.assertEquals(3, merged.size());
        Assertions.assertEquals(deviceAttrUpdate1, merged.get(0));
        Assertions.assertEquals(deviceAttrUpdate2, merged.get(1));
        Assertions.assertEquals(deviceAttrUpdate3, merged.get(2));
    }

    private Integer getIntValue(JsonNode body, String key) {
        return body.get("kv").get(key) != null ? body.get("kv").get(key).asInt() : null;
    }

    private static JsonNode createAttrBodyWithoutTs(String kvJson) {
        return JacksonUtil.toJsonNode("{\"kv\":" + kvJson + "}");
    }

    private static JsonNode createAttrBody(long ts, String kvJson) {
        return JacksonUtil.toJsonNode("{\"ts\":" + ts + ",\"kv\":" + kvJson + "}");
    }

    private static EdgeEvent createEdgeEvent(TenantId tenantId,
                                             long seqId,
                                             EdgeEventActionType action,
                                             UUID entityId,
                                             EdgeEventType type,
                                             JsonNode body) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setSeqId(seqId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(action);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(type);
        edgeEvent.setBody(body);
        return edgeEvent;
    }
}
