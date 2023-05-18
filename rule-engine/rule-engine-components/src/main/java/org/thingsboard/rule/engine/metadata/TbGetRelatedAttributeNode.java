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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

    private static final String RELATED_ENTITY_NOT_FOUND_MESSAGE = "Failed to find related entity to message originator using relation query specified in the configuration!";

    @Override
    public TbGetRelatedAttrNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetRelatedAttrNodeConfiguration.class);
        checkIfMappingIsNotEmptyOrElseThrow(config.getAttrMapping());
        checkDataToFetchSupportedOrElseThrow(config.getDataToFetch());
        return config;
    }

    @Override
    public ListenableFuture<EntityId> findEntityAsync(TbContext ctx, EntityId originator) {
        var relatedAttrConfig = (TbGetRelatedAttrNodeConfiguration) config;
        return Futures.transformAsync(
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
