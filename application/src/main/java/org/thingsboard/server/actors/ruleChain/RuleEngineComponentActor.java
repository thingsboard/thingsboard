// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbRuleNodeUpdateException;
import org.thingsboard.server.actors.service.ComponentActor;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.RuleEngineComponentLifecycleEventTrigger;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorStopReason;

public abstract class RuleEngineComponentActor<T extends EntityId, P extends ComponentMsgProcessor<T>> extends ComponentActor<T, P> {

    public RuleEngineComponentActor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext, tenantId, id);
    }

    @Override
    protected void logLifecycleEvent(ComponentLifecycleEvent event, Exception e) {
        super.logLifecycleEvent(event, e);
        if (e instanceof TbRuleNodeUpdateException || (event == ComponentLifecycleEvent.STARTED && e != null)) {
            return;
        }
        processNotificationRule(event, e);
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {
        super.destroy(stopReason, cause);
        if (stopReason == TbActorStopReason.INIT_FAILED && cause != null) {
            processNotificationRule(ComponentLifecycleEvent.STARTED, cause);
        }
    }

    private void processNotificationRule(ComponentLifecycleEvent event, Throwable e) {
        if (processor == null) {
            return;
        }
        systemContext.getNotificationRuleProcessor().process(RuleEngineComponentLifecycleEventTrigger.builder()
                .tenantId(tenantId)
                .ruleChainId(getRuleChainId())
                .ruleChainName(getRuleChainName())
                .componentId(id)
                .componentName(processor.getComponentName())
                .eventType(event)
                .error(e)
                .build());
    }

    protected abstract RuleChainId getRuleChainId();

    protected abstract String getRuleChainName();

}
