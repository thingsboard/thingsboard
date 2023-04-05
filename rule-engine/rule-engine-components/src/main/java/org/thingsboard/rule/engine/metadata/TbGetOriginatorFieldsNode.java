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
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesFieldsAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * Created by ashvayka on 19.01.18.
 */
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "originator fields",
        configClazz = TbGetOriginatorFieldsConfiguration.class,
        nodeDescription = "Add Message Originator fields values into Message Metadata or Message Data",
        nodeDetails = "Will fetch fields values specified in mapping. If specified field is not part of originator fields it will be ignored. " +
                "This node supports only following originator types: TENANT, CUSTOMER, USER, ASSET, DEVICE, ALARM, RULE_CHAIN, ENTITY_VIEW.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeOriginatorFieldsConfig")
public class TbGetOriginatorFieldsNode extends TbAbstractNodeWithFetchTo<TbGetOriginatorFieldsConfiguration> {

    @Override
    protected TbGetOriginatorFieldsConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var getOriginatorFieldsConfiguration = TbNodeUtils.convert(configuration, TbGetOriginatorFieldsConfiguration.class);
        if (getOriginatorFieldsConfiguration.getFieldsMapping().isEmpty()) {
            throw new TbNodeException("At least one field mapping should be specified!");
        }
        return getOriginatorFieldsConfiguration;
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.checkTenantEntity(msg.getOriginator());
        var msgDataAsObjectNode = FetchTo.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        withCallback(collectMappedEntityFieldsAsync(ctx, msg.getOriginator()),
                targetKeysToSourceValuesMap -> {
                    TbMsgMetaData msgMetaData = msg.getMetaData().copy();
                    for (var entry : targetKeysToSourceValuesMap.entrySet()) {
                        var targetKeyName = entry.getKey();
                        var sourceFieldValue = entry.getValue();
                        if (FetchTo.DATA.equals(fetchTo)) {
                            msgDataAsObjectNode.put(targetKeyName, sourceFieldValue);
                        } else if (FetchTo.METADATA.equals(fetchTo)) {
                            msgMetaData.putValue(targetKeyName, sourceFieldValue);
                        }
                    }
                    TbMsg outMsg = transformMessage(msg, msgDataAsObjectNode, msgMetaData);
                    ctx.tellSuccess(outMsg);
                },
                t -> ctx.tellFailure(msg, t),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<Map<String, String>> collectMappedEntityFieldsAsync(TbContext ctx, EntityId entityId) {
        return Futures.transform(EntitiesFieldsAsyncLoader.findAsync(ctx, entityId),
                fieldsData -> {
                    var targetKeysToSourceValuesMap = new HashMap<String, String>();
                    for (var mappingEntry : config.getFieldsMapping().entrySet()) {
                        var sourceFieldName = mappingEntry.getKey();
                        var targetKeyName = mappingEntry.getValue();
                        var sourceFieldValue = fieldsData.getFieldValue(sourceFieldName, config.isIgnoreNullStrings());
                        if (sourceFieldValue != null) {
                            targetKeysToSourceValuesMap.put(targetKeyName, sourceFieldValue);
                        }
                    }
                    return targetKeysToSourceValuesMap;
                }, ctx.getDbCallbackExecutor()
        );
    }

}
