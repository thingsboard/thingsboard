// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbRuleEngineActorMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Created by ashvayka on 19.03.18.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class RuleNodeToRuleChainTellNextMsg extends TbRuleEngineActorMsg implements Serializable {

    @Serial
    private static final long serialVersionUID = 4577026446412871820L;

    private final RuleChainId ruleChainId;
    private final RuleNodeId originator;
    private final Set<String> relationTypes;
    private final String failureMessage;

    public RuleNodeToRuleChainTellNextMsg(RuleChainId ruleChainId, RuleNodeId originator, Set<String> relationTypes, TbMsg tbMsg, String failureMessage) {
        super(tbMsg);
        this.ruleChainId = ruleChainId;
        this.originator = originator;
        this.relationTypes = relationTypes;
        this.failureMessage = failureMessage;
    }

    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message = reason == TbActorStopReason.STOPPED ? String.format("Rule chain [%s] stopped", ruleChainId.getId()) : String.format("Failed to initialize rule chain [%s]!", ruleChainId.getId());
        msg.getCallback().onFailure(new RuleEngineException(message));
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_TO_RULE_CHAIN_TELL_NEXT_MSG;
    }

}
