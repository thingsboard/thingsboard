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
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbRuleNodeUpdateException;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.gen.transport.TransportProtos;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class RuleNodeActorMessageProcessor extends ComponentMsgProcessor<RuleNodeId> {

    private static final String UNKNOWN_NAME = "Unknown";
    private final String ruleChainName;
    private final TbApiUsageReportClient apiUsageClient;
    private final DefaultTbContext defaultCtx;
    private RuleNode ruleNode;
    private TbNode tbNode;
    private RuleNodeInfo info;

    RuleNodeActorMessageProcessor(TenantId tenantId, String ruleChainName,
                                  RuleNodeId ruleNodeId, ActorSystemContext systemContext, TbActorCtx selfActor) {
        super(systemContext, tenantId, ruleNodeId);
        this.apiUsageClient = systemContext.getApiUsageClient();
        this.ruleChainName = ruleChainName;
        this.ruleNode = systemContext.getRuleChainService().findRuleNodeById(tenantId, entityId);
        this.defaultCtx = new DefaultTbContext(systemContext, ruleChainName, new RuleNodeCtx(tenantId, selfActor, ruleNode));
        this.info = new RuleNodeInfo(ruleNodeId, ruleChainName, getName(ruleNode));
    }

    @Override
    public void start(TbActorCtx context) throws Exception {
        if (isMyNodePartition()) {
            log.debug("[{}][{}] Starting", tenantId, entityId);
            tbNode = initComponent(ruleNode);
            if (tbNode != null) {
                state = ComponentLifecycleState.ACTIVE;
            }
        }
    }

    @Override
    public void onUpdate(TbActorCtx context) throws Exception {
        RuleNode newRuleNode = systemContext.getRuleChainService().findRuleNodeById(tenantId, entityId);
        if (isMyNodePartition(newRuleNode)) {
            this.info = new RuleNodeInfo(entityId, ruleChainName, getName(newRuleNode));
            boolean restartRequired = state != ComponentLifecycleState.ACTIVE ||
                    !(ruleNode.getType().equals(newRuleNode.getType()) &&
                            ruleNode.getConfiguration().equals(newRuleNode.getConfiguration()));
            this.ruleNode = newRuleNode;
            this.defaultCtx.updateSelf(newRuleNode);
            if (restartRequired) {
                if (tbNode != null) {
                    tbNode.destroy();
                }
                try {
                    start(context);
                } catch (Exception e) {
                    throw new TbRuleNodeUpdateException("Failed to update rule node", e);
                }
            }
        } else if (tbNode != null) {
            stop(null);
            tbNode = null;
        }
    }

    @Override
    public void stop(TbActorCtx context) {
        log.debug("[{}][{}] Stopping", tenantId, entityId);
        if (tbNode != null) {
            tbNode.destroy();
            state = ComponentLifecycleState.SUSPENDED;
        }
    }

    @Override
    public void onPartitionChangeMsg(PartitionChangeMsg msg) throws Exception {
        log.debug("[{}][{}] onPartitionChangeMsg: [{}]", tenantId, entityId, msg);
        if (tbNode != null) {
            if (!isMyNodePartition()) {
                stop(null);
                tbNode = null;
            } else {
                tbNode.onPartitionChangeMsg(defaultCtx, msg);
            }
        } else if (isMyNodePartition()) {
            start(null);
        }
    }

    public void onRuleToSelfMsg(RuleNodeToSelfMsg msg) throws Exception {
        checkComponentStateActive(msg.getMsg());
        TbMsg tbMsg = msg.getMsg();
        int ruleNodeCount = tbMsg.getAndIncrementRuleNodeCounter();
        var tenantProfileConfiguration = getTenantProfileConfiguration();
        int maxRuleNodeExecutionsPerMessage = tenantProfileConfiguration.getMaxRuleNodeExecsPerMessage();
        if (maxRuleNodeExecutionsPerMessage == 0 || ruleNodeCount < maxRuleNodeExecutionsPerMessage) {
            apiUsageClient.report(tenantId, tbMsg.getCustomerId(), ApiUsageRecordKey.RE_EXEC_COUNT);
            persistDebugInputIfAllowed(msg.getMsg(), "Self");
            try {
                tbNode.onMsg(defaultCtx, msg.getMsg());
            } catch (Exception e) {
                defaultCtx.tellFailure(msg.getMsg(), e);
            }
        } else {
            tbMsg.getCallback().onFailure(new RuleNodeException("Message is processed by more then " + maxRuleNodeExecutionsPerMessage + " rule nodes!", ruleChainName, ruleNode));
        }
    }

    void onRuleChainToRuleNodeMsg(RuleChainToRuleNodeMsg msg) throws Exception {
        if (!isMyNodePartition()) {
            putToNodePartition(msg.getMsg());
        } else {
            msg.getMsg().getCallback().onProcessingStart(info);
            checkComponentStateActive(msg.getMsg());
            TbMsg tbMsg = msg.getMsg();
            int ruleNodeCount = tbMsg.getAndIncrementRuleNodeCounter();
            var tenantProfileConfiguration = getTenantProfileConfiguration();
            int maxRuleNodeExecutionsPerMessage = tenantProfileConfiguration.getMaxRuleNodeExecsPerMessage();
            if (maxRuleNodeExecutionsPerMessage == 0 || ruleNodeCount < maxRuleNodeExecutionsPerMessage) {
                apiUsageClient.report(tenantId, tbMsg.getCustomerId(), ApiUsageRecordKey.RE_EXEC_COUNT);
                persistDebugInputIfAllowed(msg.getMsg(), msg.getFromRelationType());
                try {
                    tbNode.onMsg(msg.getCtx(), msg.getMsg());
                } catch (Exception e) {
                    msg.getCtx().tellFailure(msg.getMsg(), e);
                }
            } else {
                tbMsg.getCallback().onFailure(new RuleNodeException("Message is processed by more then " + maxRuleNodeExecutionsPerMessage + " rule nodes!", ruleChainName, ruleNode));
            }
        }
    }

    @Override
    public String getComponentName() {
        return getName(ruleNode);
    }

    private String getName(RuleNode ruleNode) {
        return ruleNode != null ? ruleNode.getName() : UNKNOWN_NAME;
    }

    private TbNode initComponent(RuleNode ruleNode) throws Exception {
        TbNode tbNode = null;
        if (ruleNode != null) {
            Class<?> componentClazz = Class.forName(ruleNode.getType());
            tbNode = (TbNode) (componentClazz.getDeclaredConstructor().newInstance());
            tbNode.init(defaultCtx, new TbNodeConfiguration(ruleNode.getConfiguration()));
        }
        return tbNode;
    }

    @Override
    protected RuleNodeException getInactiveException() {
        return new RuleNodeException("Rule Node is not active! Failed to initialize.", ruleChainName, ruleNode);
    }

    private boolean isMyNodePartition() {
        return isMyNodePartition(this.ruleNode);
    }

    private boolean isMyNodePartition(RuleNode ruleNode) {
        boolean result = ruleNode == null || !ruleNode.isSingletonMode()
                || systemContext.getDiscoveryService().isMonolith()
                || defaultCtx.isLocalEntity(ruleNode.getId());
        if (!result) {
            log.trace("[{}][{}] Is not my node partition", tenantId, entityId);
        }
        return result;
    }

    //Message will return after processing. See RuleChainActorMessageProcessor.pushToTarget.
    private void putToNodePartition(TbMsg source) {
        TbMsg tbMsg = TbMsg.newMsg(source, source.getQueueName(), source.getRuleChainId(), entityId);
        TopicPartitionInfo tpi = systemContext.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, ruleNode.getId());
        TransportProtos.ToRuleEngineMsg toQueueMsg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsgProto(TbMsg.toProto(tbMsg))
                .build();
        systemContext.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(), toQueueMsg, null);
        defaultCtx.ack(source);
    }

    private void persistDebugInputIfAllowed(TbMsg msg, String fromNodeConnectionType) {
        if (DebugModeUtil.isDebugAllAvailable(ruleNode)) {
            systemContext.persistDebugInput(tenantId, entityId, msg, fromNodeConnectionType);
        }
    }

}
