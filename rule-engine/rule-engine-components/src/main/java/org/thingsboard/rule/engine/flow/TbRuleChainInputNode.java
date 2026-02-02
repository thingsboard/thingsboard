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
package org.thingsboard.rule.engine.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Optional;
import java.util.UUID;

@RuleNode(
        type = ComponentType.FLOW,
        name = "rule chain",
        configClazz = TbRuleChainInputNodeConfiguration.class,
        version = 1,
        nodeDescription = "Transfers the message to another rule chain",
        nodeDetails = "The incoming message is forwarded to the input node of target rule chain. " +
                "If 'Forward message to the originator's default rule chain' is enabled, " +
                "then target rule chain might be resolved dynamically based on incoming message originator. " +
                "In this case rule chain specified in the configuration will be used as fallback rule chain.<br><br>" +
                "Output connections: <i>Any connection(s) produced by output node(s) in the target rule chain.</i>",
        configDirective = "tbFlowNodeRuleChainInputConfig",
        relationTypes = {},
        ruleChainNode = true,
        customRelations = true,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/flow/rule-chain/"
)
public class TbRuleChainInputNode implements TbNode {

    private RuleChainId ruleChainId;
    private boolean forwardMsgToDefaultRuleChain;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbRuleChainInputNodeConfiguration config = TbNodeUtils.convert(configuration, TbRuleChainInputNodeConfiguration.class);
        if (config.getRuleChainId() == null) {
            throw new TbNodeException("Rule chain must be set!", true);
        }
        UUID ruleChainUUID;
        try {
            ruleChainUUID = UUID.fromString(config.getRuleChainId());
        } catch (Exception e) {
            throw new TbNodeException("Failed to parse rule chain id: " + config.getRuleChainId(), true);
        }
        ruleChainId = new RuleChainId(ruleChainUUID);
        ctx.checkTenantEntity(ruleChainId);
        forwardMsgToDefaultRuleChain = config.isForwardMsgToDefaultRuleChain();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        RuleChainId targetRuleChainId = forwardMsgToDefaultRuleChain ?
                getOriginatorDefaultRuleChainId(ctx, msg).orElse(ruleChainId) : ruleChainId;
        ctx.input(msg, targetRuleChainId);
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0 -> {
                if (!oldConfiguration.has("forwardMsgToDefaultRuleChain")) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put("forwardMsgToDefaultRuleChain", false);
                }
            }
            default -> {
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    private Optional<RuleChainId> getOriginatorDefaultRuleChainId(TbContext ctx, TbMsg msg) {
        return Optional.ofNullable(
                switch (msg.getOriginator().getEntityType()) {
                    case DEVICE ->
                            ctx.getDeviceProfileCache().get(ctx.getTenantId(), (DeviceId) msg.getOriginator()).getDefaultRuleChainId();
                    case ASSET ->
                            ctx.getAssetProfileCache().get(ctx.getTenantId(), (AssetId) msg.getOriginator()).getDefaultRuleChainId();
                    default -> null;
                });
    }

}
