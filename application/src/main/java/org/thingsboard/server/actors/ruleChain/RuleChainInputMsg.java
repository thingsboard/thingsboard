// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class RuleChainInputMsg extends TbToRuleChainActorMsg {

    public RuleChainInputMsg(RuleChainId target, TbMsg tbMsg) {
        super(tbMsg, target);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_INPUT_MSG;
    }
}
