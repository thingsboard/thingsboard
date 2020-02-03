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

import java.util.Set;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "转换器", customRelations = true,
        relationTypes = {},
        configClazz = TbJsSwitchNodeConfiguration.class,
        nodeDescription = "将传入的消息路由到一个或多个输出链",
        nodeDetails = "该节点执行配置的JS脚本。脚本应该返回下一个链名称的数组，其中消息应该被路由。" +
                "如果数组为空 - 消息不会被路由到下一个节点。" +
                "可以通过<code>msg</code>属性访问消息的有效负载，例如<code>msg.temperature < 10;</code><br/>" +
                "可以通过<code>metadata</code>属性访问消息的元数据，例如<code>metadata.customerName === 'John';</code><br/>" +
                "可以通过<code>msgType</code>属性访问消息类型。",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeSwitchConfig")
public class TbJsSwitchNode implements TbNode {

    private TbJsSwitchNodeConfiguration config;
    private ScriptEngine jsEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsSwitchNodeConfiguration.class);
        this.jsEngine = ctx.createJsScriptEngine(config.getJsScript());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ListeningExecutor jsExecutor = ctx.getJsExecutor();
        ctx.logJsEvalRequest();
        withCallback(jsExecutor.executeAsync(() -> jsEngine.executeSwitch(msg)),
                result -> {
                    ctx.logJsEvalResponse();
                    processSwitch(ctx, msg, result);
                },
                t -> {
                    ctx.logJsEvalFailure();
                    ctx.tellFailure(msg, t);
                }, ctx.getDbCallbackExecutor());
    }

    private void processSwitch(TbContext ctx, TbMsg msg, Set<String> nextRelations) {
        ctx.tellNext(msg, nextRelations);
    }

    @Override
    public void destroy() {
        if (jsEngine != null) {
            jsEngine.destroy();
        }
    }
}
