/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delete relation",
        configClazz = TbDeleteRelationNodeConfiguration.class,
        nodeDescription = "Finds target Entity by entity name pattern and then delete a relation to Originator Entity by type and direction.",
        nodeDetails = "If the relation successfully deleted -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
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
    protected ListenableFuture<Boolean> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entityContainer) {
        return deleteIfExist(ctx, msg, entityContainer);
    }

    private ListenableFuture<Boolean> deleteIfExist(TbContext ctx, TbMsg msg, EntityContainer entityContainer) {
        processSearchDirection(msg, entityContainer);
        return Futures.transformAsync(ctx.getRelationService().checkRelation(ctx.getTenantId(), fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON),
                result -> {
                    if (result) {
                        return processDeleteRelation(ctx);
                    }
                    return Futures.immediateFuture(true);
                });
    }

    private ListenableFuture<Boolean> processDeleteRelation(TbContext ctx) {
        return ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
    }

}
