/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.Base64Utils;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeState;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * @author Andrew Shvayka
 */
public class RuleNodeActorMessageProcessor extends ComponentMsgProcessor<RuleNodeId> {

    private final ActorRef parent;
    private final ActorRef self;
    private final RuleChainService service;
    private RuleNode ruleNode;
    private TbNode tbNode;

    RuleNodeActorMessageProcessor(TenantId tenantId, RuleChainId ruleChainId, RuleNodeId ruleNodeId, ActorSystemContext systemContext
            , LoggingAdapter logger, ActorRef parent, ActorRef self) {
        super(systemContext, logger, tenantId, ruleNodeId);
        this.parent = parent;
        this.self = self;
        this.service = systemContext.getRuleChainService();
        this.ruleNode = systemContext.getRuleChainService().findRuleNodeById(entityId);
    }

    @Override
    public void start(ActorContext context) throws Exception {
        tbNode = initComponent(ruleNode);
    }

    @Override
    public void stop(ActorContext context) throws Exception {
        tbNode.destroy();
    }

    @Override
    public void onClusterEventMsg(ClusterEventMsg msg) throws Exception {

    }

    void onRuleChainToRuleNodeMsg(RuleChainToRuleNodeMsg msg) throws Exception {
        if (ruleNode.isDebugMode()) {
            systemContext.persistDebugInput(tenantId, entityId, msg.getMsg());
        }
        tbNode.onMsg(msg.getCtx(), msg.getMsg());
    }

    private TbNode initComponent(RuleNode ruleNode) throws Exception {
        Class<?> componentClazz = Class.forName(ruleNode.getType());
        TbNode tbNode = (TbNode) (componentClazz.newInstance());
        tbNode.init(new TbNodeConfiguration(ruleNode.getConfiguration()), new TbNodeState());
        return tbNode;
    }

}
