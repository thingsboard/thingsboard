// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
final class RuleChainToRuleNodeMsg extends TbToRuleNodeActorMsg {

    @Getter
    private final String fromRelationType;

    public RuleChainToRuleNodeMsg(TbContext ctx, TbMsg tbMsg, String fromRelationType) {
        super(ctx, tbMsg);
        this.fromRelationType = fromRelationType;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_TO_RULE_MSG;
    }
}
