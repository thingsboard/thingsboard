// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.filter;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

@RuleNode(
        type = ComponentType.FILTER,
        name = "switch", customRelations = true,
        relationTypes = {},
        configClazz = TbJsSwitchNodeConfiguration.class,
        nodeDescription = "Routes incoming message to one OR multiple output connections.",
        nodeDetails = "Node executes configured TBEL(recommended) or JavaScript function that returns array of strings (connection names). " +
                "If Array is empty - message not routed to next Node. " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>msg.temperature < 10;</code><br/>" +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>metadata.customerName === 'John';</code><br/>" +
                "Message type can be accessed via <code>msgType</code> property.<br><br>" +
                "Output connections: <i>Custom connection(s) defined by switch node</i> or <code>Failure</code>",
        configDirective = "tbFilterNodeSwitchConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/filter/switch/"
)
public class TbJsSwitchNode implements TbNode {

    private ScriptEngine scriptEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbJsSwitchNodeConfiguration.class);
        scriptEngine = ctx.createScriptEngine(config.getScriptLang(), ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        Futures.addCallback(scriptEngine.executeSwitchAsync(msg), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Set<String> result) {
                processSwitch(ctx, msg, result);
            }

            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        }, MoreExecutors.directExecutor()); //usually runs in a callbackExecutor
    }

    private void processSwitch(TbContext ctx, TbMsg msg, Set<String> nextRelations) {
        ctx.tellNext(msg, nextRelations);
    }

    @Override
    public void destroy() {
        if (scriptEngine != null) {
            scriptEngine.destroy();
        }
    }

}
