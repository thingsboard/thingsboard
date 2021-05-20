/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntityContainer;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delete relation",
        configClazz = TbDeleteRelationNodeConfiguration.class,
        nodeDescription = "Finds target Entity by entity name pattern and then delete a relation to Originator Entity by type and direction" +
                " if 'Delete single entity' is set to true, otherwise rule node will delete all relations to the originator of the message by type and direction.",
        nodeDetails = "If the relation(s) successfully deleted -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeleteRelationConfig",
        icon = "remove_circle"
)
public class TbDeleteRelationNode extends TbAbstractRelationActionNode<TbDeleteRelationNodeConfiguration> {

    @Override
    protected TbDeleteRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbDeleteRelationNodeConfiguration.class);
    }

    @Override
    protected boolean createEntityIfNotExists() {
        return false;
    }

    @Override
    protected ListenableFuture<RelationContainer> processEntityRelationAction(TbContext ctx, TbMsg msg, String relationType) {
        return getRelationContainerListenableFuture(ctx, msg, relationType);
    }

    @Override
    protected ListenableFuture<RelationContainer> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType) {
        return Futures.transform(processSingle(ctx, msg, entityContainer, relationType), result -> new RelationContainer(msg, result), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<RelationContainer> getRelationContainerListenableFuture(TbContext ctx, TbMsg msg, String relationType) {
        if (config.isDeleteForSingleEntity()) {
            return Futures.transformAsync(getEntity(ctx, msg), entityContainer -> doProcessEntityRelationAction(ctx, msg, entityContainer, relationType), ctx.getDbCallbackExecutor());
        } else {
            return Futures.transform(processList(ctx, msg), result -> new RelationContainer(msg, result), ctx.getDbCallbackExecutor());
        }
    }

    private ListenableFuture<Boolean> processList(TbContext ctx, TbMsg msg) {
        return Futures.transformAsync(processListSearchDirection(ctx, msg), entityRelations -> {
            if (entityRelations.isEmpty()) {
                return Futures.immediateFuture(true);
            } else {
                List<ListenableFuture<Boolean>> listenableFutureList = new ArrayList<>();
                for (EntityRelation entityRelation : entityRelations) {
                    listenableFutureList.add(ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), entityRelation));
                }
                return Futures.transformAsync(Futures.allAsList(listenableFutureList), booleans -> {
                    for (Boolean bool : booleans) {
                        if (!bool) {
                            return Futures.immediateFuture(false);
                        }
                    }
                    return Futures.immediateFuture(true);
                }, ctx.getDbCallbackExecutor());
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processSingle(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType) {
        SearchDirectionIds sdId = processSingleSearchDirection(msg, entityContainer);
        return Futures.transformAsync(ctx.getRelationService().checkRelation(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON),
                result -> {
                    if (result) {
                        return processSingleDeleteRelation(ctx, sdId, relationType);
                    }
                    return Futures.immediateFuture(true);
                }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processSingleDeleteRelation(TbContext ctx, SearchDirectionIds sdId, String relationType) {
        return ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON);
    }

}
