// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.flow;

import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.FLOW,
        name = "output",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "transfers the message to the caller rule chain",
        nodeDetails = "Produces output of the rule chain processing. " +
                "The output is forwarded to the caller rule chain, as an output of the corresponding \"input\" rule node. " +
                "The output rule node name corresponds to the relation type of the output message, and it is used to forward messages to other rule nodes in the caller rule chain. ",
        configDirective = "tbFlowNodeRuleChainOutputConfig",
        outEnabled = false,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/flow/output/"
)
public class TbRuleChainOutputNode implements TbNode {

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) {}

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.output(msg, ctx.getSelf().getName());
    }

}
