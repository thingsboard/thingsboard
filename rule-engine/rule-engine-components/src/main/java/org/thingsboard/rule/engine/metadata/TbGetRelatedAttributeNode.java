/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Arrays;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "related entity data",
        configClazz = TbGetRelatedDataNodeConfiguration.class,
        version = 1,
        nodeDescription = "Adds originators related entity attributes or latest telemetry or fields into message or message metadata",
        nodeDetails = "Related entity lookup based on the configured relation query. " +
                "If multiple related entities are found, only first entity is used for message enrichment, other entities are discarded. " +
                "Useful when you need to retrieve data from an entity that has a relation to the message originator and use them for further message processing.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbEnrichmentNodeRelatedAttributesConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/enrichment/related-entity-data/"
)
public class TbGetRelatedAttributeNode extends TbAbstractGetEntityDataNode<EntityId> {

    private static final String RELATED_ENTITY_NOT_FOUND_MESSAGE = "Failed to find related entity to message originator using relation query specified in the configuration!";

    @Override
    public TbGetRelatedDataNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetRelatedDataNodeConfiguration.class);
        checkIfMappingIsNotEmptyOrElseThrow(config.getDataMapping());
        checkDataToFetchSupportedOrElseThrow(config.getDataToFetch());
        return config;
    }

    @Override
    public ListenableFuture<EntityId> findEntityAsync(TbContext ctx, EntityId originator) {
        var relatedAttrConfig = (TbGetRelatedDataNodeConfiguration) config;
        return Futures.transform(
                EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, originator, relatedAttrConfig.getRelationsQuery()),
                checkIfEntityIsPresentOrThrow(RELATED_ENTITY_NOT_FOUND_MESSAGE),
                ctx.getDbCallbackExecutor());
    }

    @Override
    protected void checkDataToFetchSupportedOrElseThrow(DataToFetch dataToFetch) throws TbNodeException {
        if (dataToFetch == null) {
            throw new TbNodeException("DataToFetch property cannot be null! Supported values are: " + Arrays.toString(DataToFetch.values()));
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ? upgradeToUseFetchToAndDataToFetch(oldConfiguration) : new TbPair<>(false, oldConfiguration);
    }

}
