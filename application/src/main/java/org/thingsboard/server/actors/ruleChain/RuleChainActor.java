// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;

public class RuleChainActor extends RuleEngineComponentActor<RuleChainId, RuleChainActorMessageProcessor> {

    private final RuleChain ruleChain;

    private RuleChainActor(ActorSystemContext systemContext, TenantId tenantId, RuleChain ruleChain) {
        super(systemContext, tenantId, ruleChain.getId());
        this.ruleChain = ruleChain;
    }

    @Override
    protected RuleChainActorMessageProcessor createProcessor(TbActorCtx ctx) {
        return new RuleChainActorMessageProcessor(tenantId, ruleChain, systemContext,
                ctx.getParentRef(), ctx);
    }

    @Override
    protected boolean doProcess(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                processor.onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case RULE_TO_RULE_CHAIN_TELL_NEXT_MSG:
                processor.onTellNext((RuleNodeToRuleChainTellNextMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                processor.onRuleChainToRuleChainMsg((RuleChainToRuleChainMsg) msg);
                break;
            case RULE_CHAIN_INPUT_MSG:
                processor.onRuleChainInputMsg((RuleChainInputMsg) msg);
                break;
            case RULE_CHAIN_OUTPUT_MSG:
                processor.onRuleChainOutputMsg((RuleChainOutputMsg) msg);
                break;
            case PARTITION_CHANGE_MSG:
                processor.onPartitionChangeMsg((PartitionChangeMsg) msg);
                break;
            case STATS_PERSIST_TICK_MSG:
                onStatsPersistTick(id);
                break;
            default:
                return false;
        }
        return true;
    }

    public static class ActorCreator extends ContextBasedCreator {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;
        private final RuleChain ruleChain;

        public ActorCreator(ActorSystemContext context, TenantId tenantId, RuleChain ruleChain) {
            super(context);
            this.tenantId = tenantId;
            this.ruleChain = ruleChain;
        }

        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(ruleChain.getId());
        }

        @Override
        public TbActor createActor() {
            return new RuleChainActor(context, tenantId, ruleChain);
        }
    }

    @Override
    protected RuleChainId getRuleChainId() {
        return ruleChain.getId();
    }

    @Override
    protected String getRuleChainName() {
        return ruleChain.getName();
    }

    @Override
    protected long getErrorPersistFrequency() {
        return systemContext.getRuleChainErrorPersistFrequency();
    }

}
