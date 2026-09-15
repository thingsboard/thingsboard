// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "tenant attributes",
        configClazz = TbGetEntityDataNodeConfiguration.class,
        version = 1,
        nodeDescription = "Adds message originator tenant attributes or latest telemetry into message or message metadata",
        nodeDetails = "Useful when you need to retrieve some common configuration or threshold set " +
                "that is stored as tenant attributes or telemetry data and use it for further message processing.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbEnrichmentNodeTenantAttributesConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/enrichment/tenant-attributes/"
)
public class TbGetTenantAttributeNode extends TbAbstractGetEntityDataNode<TenantId> {

    @Override
    public TbGetEntityDataNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetEntityDataNodeConfiguration.class);
        checkIfMappingIsNotEmptyOrElseThrow(config.getDataMapping());
        checkDataToFetchSupportedOrElseThrow(config.getDataToFetch());
        return config;
    }

    @Override
    public ListenableFuture<TenantId> findEntityAsync(TbContext ctx, EntityId originator) {
        return Futures.immediateFuture(ctx.getTenantId());
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ? upgradeToUseFetchToAndDataToFetch(oldConfiguration) : new TbPair<>(false, oldConfiguration);
    }

}
