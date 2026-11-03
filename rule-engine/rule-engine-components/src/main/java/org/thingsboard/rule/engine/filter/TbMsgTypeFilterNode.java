// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.filter;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.FILTER,
        name = "message type filter",
        configClazz = TbMsgTypeFilterNodeConfiguration.class,
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        nodeDescription = "Filter incoming messages by Message Type",
        nodeDetails = "If incoming message type is expected - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>",
        configDirective = "tbFilterNodeMessageTypeConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/filter/message-type-filter/"
)
public class TbMsgTypeFilterNode implements TbNode {

    private TbMsgTypeFilterNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbMsgTypeFilterNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.tellNext(msg, config.getMessageTypes().contains(msg.getType()) ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
    }

}
