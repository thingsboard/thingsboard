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
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "originator attributes",
        configClazz = TbGetAttributesNodeConfiguration.class,
        version = 1,
        nodeDescription = "Adds attributes and/or latest timeseries data for the message originator to the message or message metadata",
        nodeDetails = "Useful when you need to retrieve some attributes or the latest telemetry readings from the message originator " +
                "that are not included in the incoming message to use them for further message processing. " +
                "For example to filter messages based on the threshold value stored in the attributes.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbEnrichmentNodeOriginatorAttributesConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/enrichment/originator-attributes/"
)
public class TbGetAttributesNode extends TbAbstractGetAttributesNode<TbGetAttributesNodeConfiguration, EntityId> {

    @Override
    protected TbGetAttributesNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetAttributesNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<EntityId> findEntityIdAsync(TbContext ctx, TbMsg msg) {
        return Futures.immediateFuture(msg.getOriginator());
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "fetchToData",
                        TbMsgSource.DATA.name(),
                        TbMsgSource.METADATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }

}
