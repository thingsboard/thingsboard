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
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.util.EntitiesCustomerIdAsyncLoader;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name="管控机构属性",
        configClazz = TbGetEntityAttrNodeConfiguration.class,
        nodeDescription = "将发起者管控机构属性或最新的遥测添加到消息元数据",
        nodeDetails = "如果配置了属性丰富，则会将服务器范围属性添加到消息元数据。" +
                "如果配置了最新遥测丰富，最新的遥测数据将会被添加到元数据。" +
                "要在其他节点中访问这些属性，可以使用此模板" +
                "<code>metadata.temperature</code>。",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbEnrichmentNodeCustomerAttributesConfig")
public class TbGetCustomerAttributeNode extends TbEntityGetAttrNode<CustomerId> {

    @Override
    protected ListenableFuture<CustomerId> findEntityAsync(TbContext ctx, EntityId originator) {
        return EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctx, originator);
    }

}
