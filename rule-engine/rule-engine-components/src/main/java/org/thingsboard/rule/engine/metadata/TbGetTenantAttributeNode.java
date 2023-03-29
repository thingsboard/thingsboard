/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "tenant attributes",
        configClazz = TbGetEntityAttrNodeConfiguration.class,
        nodeDescription = "Add Originators Tenant Attributes or Latest Telemetry into Message Metadata/Data",
        nodeDetails = "If Attributes enrichment configured, server scope attributes are added into Message Metadata/Data. " +
                "If Latest Telemetry enrichment configured, latest telemetry added into Metadata/Data. " +
                "To access those attributes in other nodes this template can be used " +
                "<code>metadata.temperature</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeTenantAttributesConfig")
public class TbGetTenantAttributeNode extends TbAbstractGetEntityAttrNode<TenantId> {
    @Override
    public ListenableFuture<TenantId> findEntityAsync(TbContext ctx, EntityId originator) {
        ctx.checkTenantEntity(originator);
        return Futures.immediateFuture(ctx.getTenantId());
    }

    @Override
    public TbGetEntityAttrNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetEntityAttrNodeConfiguration.class);
    }
}
