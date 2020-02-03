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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name="关联属性",
        configClazz = TbGetRelatedAttrNodeConfiguration.class,
        nodeDescription = "将发起者相关实体属性或最新遥测添加到消息元数据",
        nodeDetails = "使用配置的关系方向和关系类型找到相关实体。" +
                "如果找到了多个相关实体，则只有第一个实体用于属性丰富，其他实体将被丢弃。" +
                "如果配置了属性丰富，服务器范围的属性将被添加到消息元数据。" +
                "如果配置了最新的遥测丰富，最新的遥测将被添加到元数据中。" +
                "要访问其他节点中的这些属性，可以使用此模板" +
                "<code>metadata.temperature</code>。",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbEnrichmentNodeRelatedAttributesConfig")

public class TbGetRelatedAttributeNode extends TbEntityGetAttrNode<EntityId> {

    private TbGetRelatedAttrNodeConfiguration config;

    @Override
    public void init(TbContext context, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetRelatedAttrNodeConfiguration.class);
        setConfig(config);
    }

    @Override
    protected ListenableFuture<EntityId> findEntityAsync(TbContext ctx, EntityId originator) {
        return EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, originator, config.getRelationsQuery());
    }
}
