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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.NodeConnectionInfo;
import org.thingsboard.client.model.PageDataRuleChain;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainMetaData;
import org.thingsboard.client.model.RuleChainType;
import org.thingsboard.client.model.RuleNode;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class RuleChainApiClientTest extends AbstractApiClientTest {

    @Test
    public void testRuleChainAndNodeLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<RuleChain> createdChains = new ArrayList<>();

        // create 5 rule chains
        for (int i = 0; i < 5; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName(TEST_PREFIX + "RuleChain_" + timestamp + "_" + i);
            ruleChain.setType(RuleChainType.CORE);
            ruleChain.setDebugMode(false);

            RuleChain created = client.saveRuleChain(ruleChain);
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(ruleChain.getName(), created.getName());
            assertEquals(RuleChainType.CORE, created.getType());

            createdChains.add(created);
        }

        // list rule chains with text search
        PageDataRuleChain filteredChains = client.getRuleChains(100, 0, null,
                TEST_PREFIX + "RuleChain_" + timestamp, null, null);
        assertNotNull(filteredChains);
        assertEquals(5, filteredChains.getData().size());

        // get rule chain by id
        RuleChain searchChain = createdChains.get(2);
        RuleChain fetchedChain = client.getRuleChainById(searchChain.getId().getId().toString());
        assertEquals(searchChain.getName(), fetchedChain.getName());
        assertEquals(searchChain.getType(), fetchedChain.getType());

        // get metadata (initially has default node)
        RuleChainMetaData metadata = client.getRuleChainMetaData(searchChain.getId().getId().toString());
        assertNotNull(metadata);
        assertEquals(searchChain.getId().getId(), metadata.getRuleChainId().getId());

        // save metadata with rule nodes and connections
        RuleChainMetaData newMetadata = new RuleChainMetaData(metadata.getRuleChainId());
        newMetadata.setVersion(metadata.getVersion());
        newMetadata.setFirstNodeIndex(0);

        // node 0: message type switch
        RuleNode switchNode = new RuleNode();
        switchNode.setName("Message Type Switch");
        switchNode.setType("org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode");
        switchNode.setConfiguration(OBJECT_MAPPER.createObjectNode().put("version", 0));
        switchNode.setAdditionalInfo(OBJECT_MAPPER.createObjectNode().put("layoutX", 200).put("layoutY", 150));

        // node 1: log node for telemetry
        RuleNode logNode = new RuleNode();
        logNode.setName("Log Telemetry");
        logNode.setType("org.thingsboard.rule.engine.action.TbLogNode");
        logNode.setConfiguration(OBJECT_MAPPER.createObjectNode()
                .put("scriptLang", "TBEL")
                .put("jsScript", "return '\\nIncoming message:\\n' + JSON.stringify(msg) + '\\nIncoming metadata:\\n' + JSON.stringify(metadata);")
                .put("tbelScript", "return '\\nIncoming message:\\n' + JSON.stringify(msg) + '\\nIncoming metadata:\\n' + JSON.stringify(metadata);"));
        logNode.setAdditionalInfo(OBJECT_MAPPER.createObjectNode().put("layoutX", 500).put("layoutY", 100));

        // node 2: save timeseries
        RuleNode saveNode = new RuleNode();
        saveNode.setName("Save Timeseries");
        saveNode.setType("org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode");
        saveNode.setConfiguration(OBJECT_MAPPER.createObjectNode()
                .put("defaultTTL", 0)
                .put("skipLatestPersistence", false)
                .put("useServerTs", false));
        saveNode.setAdditionalInfo(OBJECT_MAPPER.createObjectNode().put("layoutX", 500).put("layoutY", 250));

        newMetadata.setNodes(List.of(switchNode, logNode, saveNode));

        // connection: switch -> log (on "Post telemetry")
        NodeConnectionInfo conn1 = new NodeConnectionInfo();
        conn1.setFromIndex(0);
        conn1.setToIndex(1);
        conn1.setType("Post telemetry");

        // connection: switch -> save timeseries (on "Post telemetry")
        NodeConnectionInfo conn2 = new NodeConnectionInfo();
        conn2.setFromIndex(0);
        conn2.setToIndex(2);
        conn2.setType("Post telemetry");

        newMetadata.setConnections(List.of(conn1, conn2));
        newMetadata.setRuleChainConnections(List.of());

        RuleChainMetaData savedMetadata = client.saveRuleChainMetaData(newMetadata, false);
        assertNotNull(savedMetadata);
        assertEquals(3, savedMetadata.getNodes().size());
        assertEquals(2, savedMetadata.getConnections().size());

        // verify saved nodes
        RuleChainMetaData fetchedMetadata = client.getRuleChainMetaData(searchChain.getId().getId().toString());
        assertEquals(3, fetchedMetadata.getNodes().size());
        assertTrue(fetchedMetadata.getNodes().stream()
                .anyMatch(node -> "Log Telemetry".equals(node.getName())));
        assertTrue(fetchedMetadata.getNodes().stream()
                .anyMatch(node -> "Save Timeseries".equals(node.getName())));

        // get output labels
        client.getRuleChainOutputLabels(searchChain.getId().getId().toString());

        // update rule chain
        RuleChain chainToUpdate = createdChains.get(3);
        chainToUpdate.setName(chainToUpdate.getName() + "_updated");
        chainToUpdate.setDebugMode(true);
        RuleChain updatedChain = client.saveRuleChain(chainToUpdate);
        assertEquals(chainToUpdate.getName(), updatedChain.getName());
        assertEquals(true, updatedChain.getDebugMode());

        // delete rule chain
        UUID chainToDeleteId = createdChains.get(0).getId().getId();
        client.deleteRuleChain(chainToDeleteId.toString());

        // verify deletion
        assertReturns404(() ->
                client.getRuleChainById(chainToDeleteId.toString())
        );

        PageDataRuleChain chainsAfterDelete = client.getRuleChains(100, 0, null,
                TEST_PREFIX + "RuleChain_" + timestamp, null, null);
        assertEquals(4, chainsAfterDelete.getData().size());
    }

}
