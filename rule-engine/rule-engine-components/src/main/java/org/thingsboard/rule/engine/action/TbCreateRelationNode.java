/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create relation",
        configClazz = TbCreateRelationNodeConfiguration.class,
        nodeDescription = "Create the relation from the selected entity to originator of the message by type and direction",
        nodeDetails = "If relation is already exists - send Message via <b>Success</b> chain with error message, otherwise is also used <b>Success</b> chain without error message.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "",
        icon = "add_circle"
)
public class TbCreateRelationNode extends TbAbstractRelationActionNode<TbCreateRelationNodeConfiguration> {

    private EntityId fromId;
    private EntityId toId;

    @Override
    protected boolean createEntityIfNotExists() {
        return config.isCreateEntityIfNotExists();
    }

    @Override
    protected TbCreateRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateRelationNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<Boolean> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entity) {
        return checkRelation(ctx, msg, entity);
    }

    private ListenableFuture<Boolean> checkRelation(TbContext ctx, TbMsg msg, EntityContainer entityContainer) {
        if (EntitySearchDirection.FROM.name().equals(config.getDirection())) {
            fromId = EntityIdFactory.getByTypeAndId(entityContainer.getEntityType().name(), entityContainer.getEntityId().toString());
            toId = msg.getOriginator();
        } else {
            toId = EntityIdFactory.getByTypeAndId(entityContainer.getEntityType().name(), entityContainer.getEntityId().toString());
            fromId = msg.getOriginator();
        }
        return Futures.transformAsync(ctx.getRelationService().checkRelation(ctx.getTenantId(), fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON),
                aBoolean -> {
                    if (!aBoolean){
                        return processCreateRelation(ctx);
                    }
                    return Futures.immediateFuture(true);
                });
    }

    private ListenableFuture<Boolean> processCreateRelation(TbContext ctx) {
        EntityRelation entityRelation = new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
        return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), entityRelation);
    }
}
