/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbRuleEngineActorMsg;
import org.thingsboard.server.common.msg.queue.RuleNodeException;

@EqualsAndHashCode(callSuper = true)
public abstract class TbToRuleNodeActorMsg extends TbRuleEngineActorMsg {

    @Getter
    private final TbContext ctx;

    public TbToRuleNodeActorMsg(TbContext ctx, TbMsg tbMsg) {
        super(tbMsg);
        this.ctx = ctx;
    }

    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message = reason == TbActorStopReason.STOPPED ? "Rule node stopped" : "Failed to initialize rule node!";
        msg.getCallback().onFailure(new RuleNodeException(message, ctx.getRuleChainName(), ctx.getSelf()));
    }
}
