package org.thingsboard.server.actors.ruleChain;

import lombok.Data;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.03.18.
 */
@Data
final class RuleChainToRuleNodeMsg implements TbActorMsg {

    private final TbContext ctx;
    private final TbMsg msg;

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_TO_RULE_MSG;
    }
}
