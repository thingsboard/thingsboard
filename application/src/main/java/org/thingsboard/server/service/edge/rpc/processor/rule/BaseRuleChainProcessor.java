/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.rule;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleNodeProto;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
public class BaseRuleChainProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateRuleChain(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateMsg ruleChainUpdateMsg) {
        boolean created = false;
        RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);
        if (ruleChain == null) {
            created = true;
            ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            ruleChain.setCreatedTime(Uuids.unixTimestamp(ruleChainId.getId()));
        }
        ruleChain.setName(ruleChainUpdateMsg.getName());
        ruleChain.setType(RuleChainType.EDGE);
        ruleChain.setDebugMode(ruleChainUpdateMsg.getDebugMode());
        ruleChain.setConfiguration(JacksonUtil.toJsonNode(ruleChainUpdateMsg.getConfiguration()));

        UUID firstRuleNodeUUID = safeGetUUID(ruleChainUpdateMsg.getFirstRuleNodeIdMSB(), ruleChainUpdateMsg.getFirstRuleNodeIdLSB());
        ruleChain.setFirstRuleNodeId(firstRuleNodeUUID != null ? new RuleNodeId(firstRuleNodeUUID) : null);

        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        if (created) {
            ruleChain.setId(ruleChainId);
        }
        ruleChainService.saveRuleChain(ruleChain, false);
        return created;
    }

    protected boolean saveOrUpdateRuleChainMetadata(TenantId tenantId, RuleChainId ruleChainId, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) throws IOException {
        RuleChainMetaData ruleChainMetadata = new RuleChainMetaData();
        ruleChainMetadata.setRuleChainId(ruleChainId);
        ruleChainMetadata.setNodes(parseNodeProtos(ruleChainId, ruleChainMetadataUpdateMsg.getNodesList()));
        ruleChainMetadata.setConnections(parseConnectionProtos(ruleChainMetadataUpdateMsg.getConnectionsList()));
        ruleChainMetadata.setRuleChainConnections(parseRuleChainConnectionProtos(ruleChainMetadataUpdateMsg.getRuleChainConnectionsList()));
        if (ruleChainMetadataUpdateMsg.getFirstNodeIndex() != -1) {
            ruleChainMetadata.setFirstNodeIndex(ruleChainMetadataUpdateMsg.getFirstNodeIndex());
        }
        if (ruleChainMetadata.getNodes().size() > 0) {
            ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetadata, Function.identity());
            return true;
        }
        return false;
    }

    private List<RuleNode> parseNodeProtos(RuleChainId ruleChainId, List<RuleNodeProto> nodesList) throws IOException {
        List<RuleNode> result = new ArrayList<>();
        for (RuleNodeProto proto : nodesList) {
            RuleNode ruleNode = new RuleNode();
            RuleNodeId ruleNodeId = new RuleNodeId(new UUID(proto.getIdMSB(), proto.getIdLSB()));
            ruleNode.setId(ruleNodeId);
            ruleNode.setCreatedTime(Uuids.unixTimestamp(ruleNodeId.getId()));
            ruleNode.setRuleChainId(ruleChainId);
            ruleNode.setType(proto.getType());
            ruleNode.setName(proto.getName());
            ruleNode.setDebugMode(proto.getDebugMode());
            ruleNode.setConfiguration(JacksonUtil.OBJECT_MAPPER.readTree(proto.getConfiguration()));
            ruleNode.setAdditionalInfo(JacksonUtil.OBJECT_MAPPER.readTree(proto.getAdditionalInfo()));
            result.add(ruleNode);
        }
        return result;
    }

    private List<NodeConnectionInfo> parseConnectionProtos(List<org.thingsboard.server.gen.edge.v1.NodeConnectionInfoProto> connectionsList) {
        List<NodeConnectionInfo> result = new ArrayList<>();
        for (org.thingsboard.server.gen.edge.v1.NodeConnectionInfoProto proto : connectionsList) {
            NodeConnectionInfo info = new NodeConnectionInfo();
            info.setFromIndex(proto.getFromIndex());
            info.setToIndex(proto.getToIndex());
            info.setType(proto.getType());
            result.add(info);
        }
        return result;
    }

    private List<RuleChainConnectionInfo> parseRuleChainConnectionProtos(List<org.thingsboard.server.gen.edge.v1.RuleChainConnectionInfoProto> ruleChainConnectionsList) throws IOException {
        List<RuleChainConnectionInfo> result = new ArrayList<>();
        for (org.thingsboard.server.gen.edge.v1.RuleChainConnectionInfoProto proto : ruleChainConnectionsList) {
            RuleChainConnectionInfo info = new RuleChainConnectionInfo();
            info.setFromIndex(proto.getFromIndex());
            info.setTargetRuleChainId(new RuleChainId(new UUID(proto.getTargetRuleChainIdMSB(), proto.getTargetRuleChainIdLSB())));
            info.setType(proto.getType());
            info.setAdditionalInfo(JacksonUtil.OBJECT_MAPPER.readTree(proto.getAdditionalInfo()));
            result.add(info);
        }
        return result;
    }
}
