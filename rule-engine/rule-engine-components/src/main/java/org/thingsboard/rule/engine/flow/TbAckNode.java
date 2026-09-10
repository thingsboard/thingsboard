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
        name = "acknowledge",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Acknowledges the incoming message",
        nodeDetails = "After acknowledgement, the message is pushed to related rule nodes. Useful if you don't care what happens to this message next.",
        configDirective = "tbNodeEmptyConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/flow/acknowledge/"
)
public class TbAckNode implements TbNode {

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) {}

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.ack(msg);
        ctx.tellSuccess(msg);
    }

}
