/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andrew Shvayka
 */
public class RuleChainActorMessageProcessor extends ComponentMsgProcessor<RuleChainId> {

    private final ActorRef parent;
    private final ActorRef self;
    private final Map<RuleNodeId, RuleNodeCtx> nodeActors;
    private final Map<RuleNodeId, List<RuleNodeRelation>> nodeRoutes;
    private final RuleChainService service;

    private RuleNodeId firstId;
    private RuleNodeCtx firstNode;
    private ComponentLifecycleState state;

    RuleChainActorMessageProcessor(TenantId tenantId, RuleChainId ruleChainId, ActorSystemContext systemContext
            , LoggingAdapter logger, ActorRef parent, ActorRef self) {
        super(systemContext, logger, tenantId, ruleChainId);
        this.parent = parent;
        this.self = self;
        this.nodeActors = new HashMap<>();
        this.nodeRoutes = new HashMap<>();
        this.service = systemContext.getRuleChainService();
    }

    @Override
    public void start(ActorContext context) throws Exception {
        if (state == ComponentLifecycleState.ACTIVE) {
            return;
        }
        RuleChain ruleChain = service.findRuleChainById(entityId);
        List<RuleNode> ruleNodeList = service.getRuleChainNodes(entityId);
        // Creating and starting the actors;
        for (RuleNode ruleNode : ruleNodeList) {
            String dispatcherName = tenantId.getId().equals(EntityId.NULL_UUID) ?
                    DefaultActorService.SYSTEM_RULE_DISPATCHER_NAME : DefaultActorService.TENANT_RULE_DISPATCHER_NAME;
            ActorRef ruleNodeActor = context.actorOf(
                    Props.create(new RuleNodeActor.ActorCreator(systemContext, tenantId, entityId, ruleNode.getId()))
                            .withDispatcher(dispatcherName), ruleNode.getId().toString());
            nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
        }
        // Populating the routes map;
        for (RuleNode ruleNode : ruleNodeList) {
            List<EntityRelation> relations = service.getRuleNodeRelations(ruleNode.getId());
            for (EntityRelation relation : relations) {
                if (relation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    RuleNodeCtx ruleNodeCtx = nodeActors.get(new RuleNodeId(relation.getTo().getId()));
                    if (ruleNodeCtx == null) {
                        throw new IllegalArgumentException("Rule Node [" + relation.getFrom() + "] has invalid relation to Rule node [" + relation.getTo() + "]");
                    }
                }
                nodeRoutes.computeIfAbsent(ruleNode.getId(), k -> new ArrayList<>())
                        .add(new RuleNodeRelation(ruleNode.getId(), relation.getTo(), relation.getType()));
            }
        }

        firstId = ruleChain.getFirstRuleNodeId();
        firstNode = nodeActors.get(ruleChain.getFirstRuleNodeId());
        state = ComponentLifecycleState.ACTIVE;
    }

    @Override
    public void stop(ActorContext context) throws Exception {
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).forEach(context::stop);
        nodeActors.clear();
        nodeRoutes.clear();
        state = ComponentLifecycleState.SUSPENDED;
    }

    @Override
    public void onClusterEventMsg(ClusterEventMsg msg) throws Exception {

    }

    void onServiceToRuleEngineMsg(ServiceToRuleEngineMsg envelope) {
        checkActive();
        TbMsg tbMsg = envelope.getTbMsg();
        //TODO: push to queue and act on ack in async way
        pushMstToNode(firstNode, tbMsg);
    }

    void onTellNext(RuleNodeToRuleChainTellNextMsg envelope) {
        checkActive();
        RuleNodeId originator = envelope.getOriginator();
        String targetRelationType = envelope.getRelationType();
        //TODO: log debug output
        List<RuleNodeRelation> relations = nodeRoutes.get(originator);
        if (relations == null) {
            return;
        }
        boolean copy = relations.size() > 1;
        for (RuleNodeRelation relation : relations) {
            TbMsg msg = envelope.getMsg();
            if (copy) {
                msg = msg.copy();
            }
            if (targetRelationType == null || targetRelationType.equalsIgnoreCase(relation.getType())) {
                switch (relation.getOut().getEntityType()) {
                    case RULE_NODE:
                        RuleNodeId targetRuleNodeId = new RuleNodeId(relation.getOut().getId());
                        RuleNodeCtx targetRuleNode = nodeActors.get(targetRuleNodeId);
                        pushMstToNode(targetRuleNode, msg);
                        break;
                    case RULE_CHAIN:
//                        TODO: implement
                        break;
                }
            }
        }
    }

    private void pushMstToNode(RuleNodeCtx nodeCtx, TbMsg msg) {
        firstNode.getSelfActor().tell(new RuleChainToRuleNodeMsg(new DefaultTbContext(systemContext, nodeCtx), msg), self);
    }

    private void checkActive() {
        if (state != ComponentLifecycleState.ACTIVE) {
            throw new IllegalStateException("Rule chain is not active!");
        }
    }

}
