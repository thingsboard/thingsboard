/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "脚本", relationTypes = {"True", "False"},
        configClazz = TbJsFilterNodeConfiguration.class,
        nodeDescription = "使用JS脚本过滤传入的消息",
        nodeDetails = "使用配置的JS条件评估传入的消息。" +
                "如果是<b>True</b> - 通过<b>True</b>链发送消息，否则使用<b>False</b>链。" +
                "可以通过<code>msg</code>属性来访问消息的有效负载，例如<code>msg.temperature < 10;</code><br/>" +
                "可以通过<code>metadata</code>属性来访问消息的元数据，例如<code>metadata.customerName === 'John';</code><br/>" +
                "可以通过<code>msgType</code>属性来访问消息类型。",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeScriptConfig")

public class TbJsFilterNode implements TbNode {

    private TbJsFilterNodeConfiguration config;
    private ScriptEngine jsEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsFilterNodeConfiguration.class);
        this.jsEngine = ctx.createJsScriptEngine(config.getJsScript());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.logJsEvalRequest();
        withCallback(jsEngine.executeFilterAsync(msg),
                filterResult -> {
                    ctx.logJsEvalResponse();
                    ctx.tellNext(msg, filterResult ? "True" : "False");
                },
                t -> {
                    ctx.tellFailure(msg, t);
                    ctx.logJsEvalFailure();
                }, ctx.getDbCallbackExecutor());
    }

    @Override
    public void destroy() {
        if (jsEngine != null) {
            jsEngine.destroy();
        }
    }
}
