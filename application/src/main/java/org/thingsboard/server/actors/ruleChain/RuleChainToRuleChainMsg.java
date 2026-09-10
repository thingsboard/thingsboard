// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class RuleChainToRuleChainMsg extends TbToRuleChainActorMsg  {

    @Getter
    private final RuleChainId source;
    @Getter
    private final String fromRelationType;

    public RuleChainToRuleChainMsg(RuleChainId target, RuleChainId source, TbMsg tbMsg, String fromRelationType) {
        super(tbMsg, target);
        this.source = source;
        this.fromRelationType = fromRelationType;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_TO_RULE_CHAIN_MSG;
    }
}
