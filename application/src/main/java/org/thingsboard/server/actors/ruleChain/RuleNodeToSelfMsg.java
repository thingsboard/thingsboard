// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
final class RuleNodeToSelfMsg extends TbToRuleNodeActorMsg {

    public RuleNodeToSelfMsg(TbContext ctx, TbMsg tbMsg) {
        super(ctx, tbMsg);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_TO_SELF_MSG;
    }

}
