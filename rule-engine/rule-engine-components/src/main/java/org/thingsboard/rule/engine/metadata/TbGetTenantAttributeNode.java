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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.util.EntitiesTenantIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name="租户属性",
        configClazz = TbGetEntityAttrNodeConfiguration.class,
        nodeDescription = "将发起者租户属性或最新的遥测值添加到消息元数据",
        nodeDetails = "如果配置了属性丰富，服务器范围的属性将被添加到消息元数据。" +
                "如果配置了最新的遥测丰富，最新的遥测值将被添加到元数据。" +
                "如果要访问其他节点的这些属性，可以使用该模板" +
                "<code>metadata.temperature</code>。",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbEnrichmentNodeTenantAttributesConfig")
public class TbGetTenantAttributeNode extends TbEntityGetAttrNode<TenantId> {

    @Override
    protected ListenableFuture<TenantId> findEntityAsync(TbContext ctx, EntityId originator) {
        return EntitiesTenantIdAsyncLoader.findEntityIdAsync(ctx, originator);
    }

}
