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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "脚本",
        configClazz = TbTransformMsgNodeConfiguration.class,
        nodeDescription = "使用JavaScript更改消息的有效载荷，元数据以及消息类型",
        nodeDetails = "JavaScript函数接收3个输入参数<br/> " +
                "<code>metadata</code> - 是消息元数据。<br/>" +
                "<code>msg</code> - 是消息的有效负载。<br/>" +
                "<code>msgType</code> - 是消息类型。<br/>" +
                "必须返回如下结构：<br/>" +
                "<code>{ msg: <i style=\"color: #666;\">new payload</i>,<br/>&nbsp&nbsp&nbspmetadata: <i style=\"color: #666;\">new metadata</i>,<br/>&nbsp&nbsp&nbspmsgType: <i style=\"color: #666;\">new msgType</i> }</code><br/>" +
                "结果对象中的所有字段都是可选的，如果未指定，将从原始消息中获取。",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbTransformationNodeScriptConfig")
public class TbTransformMsgNode extends TbAbstractTransformNode {

    private TbTransformMsgNodeConfiguration config;
    private ScriptEngine jsEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbTransformMsgNodeConfiguration.class);
        this.jsEngine = ctx.createJsScriptEngine(config.getJsScript());
        setConfig(config);
    }

    @Override
    protected ListenableFuture<TbMsg> transform(TbContext ctx, TbMsg msg) {
        ctx.logJsEvalRequest();
        return jsEngine.executeUpdateAsync(msg);
    }

    @Override
    protected void transformSuccess(TbContext ctx, TbMsg msg, TbMsg m) {
        ctx.logJsEvalResponse();
        super.transformSuccess(ctx, msg, m);
    }

    @Override
    protected void transformFailure(TbContext ctx, TbMsg msg, Throwable t) {
        ctx.logJsEvalFailure();
        super.transformFailure(ctx, msg, t);
    }

    @Override
    public void destroy() {
        if (jsEngine != null) {
            jsEngine.destroy();
        }
    }
}
