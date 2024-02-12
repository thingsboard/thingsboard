/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.stream.Collectors;


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
        var deleteRelationNodeConfiguration = TbNodeUtils.convert(configuration, TbDeleteRelationNodeConfiguration.class);
        if (!deleteRelationNodeConfiguration.isDeleteForSingleEntity()) {
            return deleteRelationNodeConfiguration;
        }
        checkIfConfigEntityTypeIsSupported(deleteRelationNodeConfiguration.getEntityType());
        return deleteRelationNodeConfiguration;
    }

    @Override
    protected boolean createEntityIfNotExists() {
        return false;
    }

    @Override
    protected ListenableFuture<RelationContainer> processEntityRelationAction(TbContext ctx, TbMsg msg) {
        if (config.isDeleteForSingleEntity()) {
            return Futures.transformAsync(getTargetEntityId(ctx, msg), targetEntityId -> {
                var deleteRelationFuture = deleteRelationToSpecificEntity(ctx, msg.getOriginator(), targetEntityId, processPattern(msg, config.getRelationType()));
                return Futures.transform(deleteRelationFuture, result -> new RelationContainer(msg, result), ctx.getDbCallbackExecutor());
            }, ctx.getDbCallbackExecutor());
        }
        return Futures.transform(deleteAllRelations(ctx, msg), result -> new RelationContainer(msg, result), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> deleteAllRelations(TbContext ctx, TbMsg msg) {
        var relationType = processPattern(msg, config.getRelationType());
        var tenantId = ctx.getTenantId();
        var originator = msg.getOriginator();
        var originatorRelationsFuture = EntitySearchDirection.FROM.name().equals(config.getDirection()) ?
                ctx.getRelationService().findByFromAndTypeAsync(tenantId, originator, relationType, RelationTypeGroup.COMMON) :
                ctx.getRelationService().findByToAndTypeAsync(tenantId, originator, relationType, RelationTypeGroup.COMMON);
        return Futures.transformAsync(originatorRelationsFuture, originatorRelations -> {
            if (originatorRelations.isEmpty()) {
                return Futures.immediateFuture(true);
            }
            var deleteRelationFutures = originatorRelations.stream()
                    .map(entityRelation -> ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), entityRelation))
                    .collect(Collectors.toCollection(ArrayList::new));
            return Futures.transformAsync(Futures.allAsList(deleteRelationFutures), deleteResults ->
                    Futures.immediateFuture(deleteResults.stream().allMatch(Boolean::booleanValue)), ctx.getDbCallbackExecutor());
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> deleteRelationToSpecificEntity(TbContext ctx, EntityId originator, EntityId targetEntityId, String relationType) {
        var sdId = processSingleSearchDirection(originator, targetEntityId);
        return Futures.transformAsync(ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON),
                result -> {
                    if (result) {
                        return ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON);
                    }
                    return Futures.immediateFuture(true);
                }, ctx.getDbCallbackExecutor());
    }

}
