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
package org.thingsboard.server.actors.ruleChain;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.plugin.RuleNodeUpdatedMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.common.MultipleTbQueueTbMsgCallbackWrapper;
import org.thingsboard.server.queue.common.TbQueueTbMsgCallbackWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class RuleChainActorMessageProcessor extends ComponentMsgProcessor<RuleChainId> {

    private static final String NA_RELATION_TYPE = "";
    private final TbActorRef parent;
    private final TbActorRef self;
    private final Map<RuleNodeId, RuleNodeCtx> nodeActors;
    private final Map<RuleNodeId, List<RuleNodeRelation>> nodeRoutes;
    private final RuleChainService service;
    private final TbClusterService clusterService;
    private String ruleChainName;

    private RuleNodeId firstId;
    private RuleNodeCtx firstNode;
    private boolean started;

    RuleChainActorMessageProcessor(TenantId tenantId, RuleChain ruleChain, ActorSystemContext systemContext, TbActorRef parent, TbActorRef self) {
        super(systemContext, tenantId, ruleChain.getId());
        this.ruleChainName = ruleChain.getName();
        this.parent = parent;
        this.self = self;
        this.nodeActors = new HashMap<>();
        this.nodeRoutes = new HashMap<>();
        this.service = systemContext.getRuleChainService();
        this.clusterService = systemContext.getClusterService();
    }

    @Override
    public String getComponentName() {
        return ruleChainName;
    }

    @Override
    public void start(TbActorCtx context) {
        if (!started) {
            RuleChain ruleChain = service.findRuleChainById(tenantId, entityId);
            if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
                List<RuleNode> ruleNodeList = service.getRuleChainNodes(tenantId, entityId);
                log.debug("[{}][{}] Starting rule chain with {} nodes", tenantId, entityId, ruleNodeList.size());
                // Creating and starting the actors;
                for (RuleNode ruleNode : ruleNodeList) {
                    log.trace("[{}][{}] Creating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    TbActorRef ruleNodeActor = createRuleNodeActor(context, ruleNode);
                    nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
                }
                initRoutes(ruleChain, ruleNodeList);
                started = true;
            }
        } else {
            onUpdate(context);
        }
    }

    @Override
    public void onUpdate(TbActorCtx context) {
        RuleChain ruleChain = service.findRuleChainById(tenantId, entityId);
        if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
            ruleChainName = ruleChain.getName();
            List<RuleNode> ruleNodeList = service.getRuleChainNodes(tenantId, entityId);
            log.debug("[{}][{}] Updating rule chain with {} nodes", tenantId, entityId, ruleNodeList.size());
            for (RuleNode ruleNode : ruleNodeList) {
                RuleNodeCtx existing = nodeActors.get(ruleNode.getId());
                if (existing == null) {
                    log.trace("[{}][{}] Creating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    TbActorRef ruleNodeActor = createRuleNodeActor(context, ruleNode);
                    nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
                } else {
                    log.trace("[{}][{}] Updating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    existing.setSelf(ruleNode);
                    existing.getSelfActor().tellWithHighPriority(new RuleNodeUpdatedMsg(tenantId, existing.getSelf().getId()));
                }
            }

            Set<RuleNodeId> existingNodes = ruleNodeList.stream().map(RuleNode::getId).collect(Collectors.toSet());
            List<RuleNodeId> removedRules = nodeActors.keySet().stream().filter(node -> !existingNodes.contains(node)).toList();
            removedRules.forEach(ruleNodeId -> {
                log.trace("[{}][{}] Removing rule node [{}]", tenantId, entityId, ruleNodeId);
                RuleNodeCtx removed = nodeActors.remove(ruleNodeId);
                removed.getSelfActor().tellWithHighPriority(new ComponentLifecycleMsg(tenantId, removed.getSelf().getId(), ComponentLifecycleEvent.DELETED));
            });

            initRoutes(ruleChain, ruleNodeList);
        }
    }

    @Override
    public void stop(TbActorCtx ctx) {
        log.trace("[{}][{}] Stopping rule chain with {} nodes", tenantId, entityId, nodeActors.size());
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).map(TbActorRef::getActorId).forEach(ctx::stop);
        nodeActors.clear();
        nodeRoutes.clear();
        started = false;
    }

    @Override
    public void onPartitionChangeMsg(PartitionChangeMsg msg) {
        log.debug("[{}][{}] onPartitionChangeMsg: [{}]", tenantId, entityId, msg);
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).forEach(actorRef -> actorRef.tellWithHighPriority(msg));
    }

    private TbActorRef createRuleNodeActor(TbActorCtx ctx, RuleNode ruleNode) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(ruleNode.getId()),
                () -> DefaultActorService.RULE_DISPATCHER_NAME,
                () -> new RuleNodeActor.ActorCreator(systemContext, tenantId, entityId, ruleChainName, ruleNode.getId()),
                () -> true);
    }

    private void initRoutes(RuleChain ruleChain, List<RuleNode> ruleNodeList) {
        nodeRoutes.clear();
        // Populating the routes map;
        for (RuleNode ruleNode : ruleNodeList) {
            List<EntityRelation> relations = service.getRuleNodeRelations(TenantId.SYS_TENANT_ID, ruleNode.getId());
            log.trace("[{}][{}][{}] Processing rule node relations [{}]", tenantId, entityId, ruleNode.getId(), relations.size());
            if (relations.isEmpty()) {
                nodeRoutes.put(ruleNode.getId(), Collections.emptyList());
            } else {
                for (EntityRelation relation : relations) {
                    log.trace("[{}][{}][{}] Processing rule node relation [{}]", tenantId, entityId, ruleNode.getId(), relation.getTo());
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
        }

        firstId = ruleChain.getFirstRuleNodeId();
        firstNode = nodeActors.get(firstId);
        state = ComponentLifecycleState.ACTIVE;
    }

    void onQueueToRuleEngineMsg(QueueToRuleEngineMsg envelope) {
        TbMsg msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        log.trace("[{}][{}] Processing message [{}]: {}", entityId, firstId, msg.getId(), msg);
        if (envelope.getRelationTypes() == null || envelope.getRelationTypes().isEmpty()) {
            onTellNext(msg, true);
        } else {
            onTellNext(msg, envelope.getMsg().getRuleNodeId(), envelope.getRelationTypes(), envelope.getFailureMessage());
        }
    }

    private void onTellNext(TbMsg msg, boolean useRuleNodeIdFromMsg) {
        try {
            checkComponentStateActive(msg);
            RuleNodeId targetId = useRuleNodeIdFromMsg ? msg.getRuleNodeId() : null;
            RuleNodeCtx targetCtx;
            if (targetId == null) {
                targetCtx = firstNode;
                msg = msg.copy()
                        .ruleChainId(entityId)
                        .resetRuleNodeId()
                        .build();
            } else {
                targetCtx = nodeActors.get(targetId);
            }
            if (targetCtx != null) {
                log.trace("[{}][{}] Pushing message to target rule node", entityId, targetId);
                pushMsgToNode(targetCtx, msg, NA_RELATION_TYPE);
            } else {
                log.trace("[{}][{}] Rule node does not exist. Probably old message", entityId, targetId);
                msg.getCallback().onSuccess();
            }
        } catch (RuleNodeException rne) {
            msg.getCallback().onFailure(rne);
        } catch (Exception e) {
            msg.getCallback().onFailure(new RuleEngineException(e.getMessage(), e));
        }
    }

    public void onRuleChainInputMsg(RuleChainInputMsg envelope) {
        var tbMsg = envelope.getMsg();
        if (!checkMsgValid(tbMsg)) {
            return;
        }
        if (entityId.equals(envelope.getRuleChainId())) {
            onTellNext(tbMsg, false);
        } else {
            parent.tell(envelope);
        }
    }

    public void onRuleChainOutputMsg(RuleChainOutputMsg envelope) {
        var tbMsg = envelope.getMsg();
        if (!checkMsgValid(tbMsg)) {
            return;
        }
        if (entityId.equals(envelope.getRuleChainId())) {
            var originatorNodeId = envelope.getTargetRuleNodeId();
            RuleNodeCtx ruleNodeCtx = nodeActors.get(originatorNodeId);
            if (ruleNodeCtx != null) {
                if (DebugModeUtil.isDebugAvailable(ruleNodeCtx.getSelf(), envelope.getRelationType())) {
                    systemContext.persistDebugOutput(tenantId, originatorNodeId, tbMsg, envelope.getRelationType());
                }
            }
            onTellNext(tbMsg, originatorNodeId, Collections.singleton(envelope.getRelationType()), RuleNodeException.UNKNOWN);
        } else {
            parent.tell(envelope);
        }
    }

    void onRuleChainToRuleChainMsg(RuleChainToRuleChainMsg envelope) {
        var tbMsg = envelope.getMsg();
        if (!checkMsgValid(tbMsg)) {
            return;
        }
        try {
            checkComponentStateActive(tbMsg);
            if (firstNode != null) {
                pushMsgToNode(firstNode, tbMsg, envelope.getFromRelationType());
            } else {
                tbMsg.getCallback().onSuccess();
            }
        } catch (RuleNodeException e) {
            log.debug("Rule Chain is not active. Current state [{}] for processor [{}][{}] tenant [{}]", state, entityId.getEntityType(), entityId, tenantId);
        }
    }

    void onTellNext(RuleNodeToRuleChainTellNextMsg envelope) {
        var msg = envelope.getMsg();
        if (checkMsgValid(msg)) {
            onTellNext(msg, envelope.getOriginator(), envelope.getRelationTypes(), envelope.getFailureMessage());
        }
    }

    private void onTellNext(TbMsg msg, RuleNodeId originatorNodeId, Set<String> relationTypes, String failureMessage) {
        try {
            checkComponentStateActive(msg);
            EntityId entityId = msg.getOriginator();
            TopicPartitionInfo tpi = systemContext.resolve(tenantId, entityId, msg);

            List<RuleNodeRelation> ruleNodeRelations = nodeRoutes.get(originatorNodeId);
            if (ruleNodeRelations == null) { // When unchecked, this will cause NullPointerException when rule node doesn't exist anymore
                log.warn("[{}][{}][{}] No outbound relations (null). Probably rule node does not exist. Probably old message.", tenantId, entityId, msg.getId());
                ruleNodeRelations = Collections.emptyList();
            }

            List<RuleNodeRelation> relationsByTypes = ruleNodeRelations.stream()
                    .filter(r -> contains(relationTypes, r.getType()))
                    .collect(Collectors.toList());
            int relationsCount = relationsByTypes.size();
            if (relationsCount == 0) {
                log.trace("[{}][{}][{}] No outbound relations to process", tenantId, entityId, msg.getId());
                if (relationTypes.contains(TbNodeConnectionType.FAILURE)) {
                    RuleNodeCtx ruleNodeCtx = nodeActors.get(originatorNodeId);
                    if (ruleNodeCtx != null) {
                        msg.getCallback().onFailure(new RuleNodeException(failureMessage, ruleChainName, ruleNodeCtx.getSelf()));
                    } else {
                        log.debug("[{}] Failure during message processing by Rule Node [{}]. Enable and see debug events for more info", entityId, originatorNodeId.getId());
                        msg.getCallback().onFailure(new RuleEngineException("Failure during message processing by Rule Node [" + originatorNodeId.getId().toString() + "]"));
                    }
                } else {
                    msg.getCallback().onSuccess();
                }
            } else if (relationsCount == 1) {
                for (RuleNodeRelation relation : relationsByTypes) {
                    log.trace("[{}][{}][{}] Pushing message to single target: [{}]", tenantId, entityId, msg.getId(), relation.getOut());
                    pushToTarget(tpi, msg, relation.getOut(), relation.getType());
                }
            } else {
                MultipleTbQueueTbMsgCallbackWrapper callbackWrapper = new MultipleTbQueueTbMsgCallbackWrapper(relationsCount, msg.getCallback());
                log.trace("[{}][{}][{}] Pushing message to multiple targets: [{}]", tenantId, entityId, msg.getId(), relationsByTypes);
                for (RuleNodeRelation relation : relationsByTypes) {
                    EntityId target = relation.getOut();
                    putToQueue(tpi, msg, callbackWrapper, target);
                }
            }
        } catch (RuleNodeException rne) {
            msg.getCallback().onFailure(rne);
        } catch (Exception e) {
            log.warn("[" + tenantId + "]" + "[" + entityId + "]" + "[" + msg.getId() + "]" + " onTellNext failure", e);
            msg.getCallback().onFailure(new RuleEngineException("onTellNext - " + e.getMessage(), e));
        }
    }

    private void putToQueue(TopicPartitionInfo tpi, TbMsg msg, TbQueueCallback callbackWrapper, EntityId target) {
        switch (target.getEntityType()) {
            case RULE_NODE:
                putToQueue(tpi, msg.copy()
                        .id(UUID.randomUUID())
                        .ruleChainId(entityId)
                        .ruleNodeId(new RuleNodeId(target.getId()))
                        .build(), callbackWrapper);
                break;
            case RULE_CHAIN:
                putToQueue(tpi, msg.copy()
                        .id(UUID.randomUUID())
                        .ruleChainId(new RuleChainId(target.getId()))
                        .resetRuleNodeId()
                        .build(), callbackWrapper);
                break;
        }
    }

    private void pushToTarget(TopicPartitionInfo tpi, TbMsg msg, EntityId target, String fromRelationType) {
        if (tpi.isMyPartition()) {
            switch (target.getEntityType()) {
                case RULE_NODE:
                    pushMsgToNode(nodeActors.get(new RuleNodeId(target.getId())), msg, fromRelationType);
                    break;
                case RULE_CHAIN:
                    parent.tell(new RuleChainToRuleChainMsg(new RuleChainId(target.getId()), entityId, msg, fromRelationType));
                    break;
            }
        } else {
            putToQueue(tpi, msg, new TbQueueTbMsgCallbackWrapper(msg.getCallback()), target);
        }
    }

    private void putToQueue(TopicPartitionInfo tpi, TbMsg newMsg, TbQueueCallback callbackWrapper) {
        ToRuleEngineMsg toQueueMsg = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsgProto(TbMsg.toProto(newMsg))
                .build();
        clusterService.pushMsgToRuleEngine(tpi, newMsg.getId(), toQueueMsg, callbackWrapper);
    }

    private boolean contains(Set<String> relationTypes, String type) {
        if (relationTypes == null) {
            return true;
        }
        for (String relationType : relationTypes) {
            if (relationType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private void pushMsgToNode(RuleNodeCtx nodeCtx, TbMsg msg, String fromRelationType) {
        if (nodeCtx != null) {
            var tbCtx = new DefaultTbContext(systemContext, ruleChainName, nodeCtx);
            nodeCtx.getSelfActor().tell(new RuleChainToRuleNodeMsg(tbCtx, msg, fromRelationType));
        } else {
            log.error("[{}][{}] RuleNodeCtx is empty", entityId, ruleChainName);
            msg.getCallback().onFailure(new RuleEngineException("Rule Node CTX is empty"));
        }
    }

    @Override
    protected RuleNodeException getInactiveException() {
        RuleNode firstRuleNode = firstNode != null ? firstNode.getSelf() : null;
        return new RuleNodeException("Rule Chain is not active!  Failed to initialize.", ruleChainName, firstRuleNode);
    }

}
