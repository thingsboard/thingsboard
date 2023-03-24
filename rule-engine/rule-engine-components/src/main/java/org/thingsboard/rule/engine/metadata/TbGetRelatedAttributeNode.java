/**
 * Copyright © 2016-2023 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "related attributes",
        configClazz = TbGetRelatedAttrNodeConfiguration.class,
        nodeDescription = "Add Originators Related Entity Attributes or Latest Telemetry into Message Metadata/Data",
        nodeDetails = "Related Entity found using configured relation direction and Relation Type. " +
                "If multiple Related Entities are found, only first Entity is used for attributes enrichment, other entities are discarded. " +
                "If Attributes enrichment configured, server scope attributes are added into Message Metadata/Data. " +
                "If Latest Telemetry enrichment configured, latest telemetry added into Metadata/Data. " +
                "To access those attributes in other nodes this template can be used " +
                "<code>metadata.temperature</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeRelatedAttributesConfig")
public class TbGetRelatedAttributeNode extends TbAbstractGetEntityAttrNode<EntityId> {
    @Override
    public TbGetRelatedAttrNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetRelatedAttrNodeConfiguration.class);
    }

    @Override
    public ListenableFuture<EntityId> findEntityAsync(TbContext ctx, EntityId originator) {
        ctx.checkTenantEntity(originator);
        var relatedAttrConfig = (TbGetRelatedAttrNodeConfiguration) config;
        return EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, originator, relatedAttrConfig.getRelationsQuery());
    }
}
