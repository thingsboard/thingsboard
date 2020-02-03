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
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "发起者类型",
        configClazz = TbOriginatorTypeFilterNodeConfiguration.class,
        relationTypes = {"True", "False"},
        nodeDescription = "根据消息的发起者类型过滤传入的消息",
        nodeDetails = "如果是期望的传入消息发起者类型 - 通过<b>True</b>链发送消息，否则使用<b>False</b>链。",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbFilterNodeOriginatorTypeConfig")
public class TbOriginatorTypeFilterNode implements TbNode {

    TbOriginatorTypeFilterNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbOriginatorTypeFilterNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        EntityType originatorType = msg.getOriginator().getEntityType();
        ctx.tellNext(msg, config.getOriginatorTypes().contains(originatorType) ? "True" : "False");
    }

    @Override
    public void destroy() {

    }
}
