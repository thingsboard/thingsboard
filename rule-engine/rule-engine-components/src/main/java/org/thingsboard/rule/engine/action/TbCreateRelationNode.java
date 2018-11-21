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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create relation",
        configClazz = TbCreateRelationNodeConfiguration.class,
        nodeDescription = "Create the relation from the selected entity to originator of the message by type and direction",
        nodeDetails = "If relation is already exists - send Message via <b>Success</b> chain with error message, otherwise is also used <b>Success</b> chain without error message.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckRelationConfig")
public class TbCreateRelationNode implements TbNode {

    private TbCreateRelationNodeConfiguration config;
    private EntityId fromId;
    private EntityId toId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCreateRelationNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        DonAsynchron.withCallback(checkRelation(ctx, msg), result -> {
            if (result) {
                ctx.tellNext(msg, SUCCESS, new Throwable("Relation between message originator and Entity: " + config.getEntityId() + " is already exist"));
            } else {
                processCreateRelation(ctx, msg);
            }
        }, error -> ctx.tellFailure(msg, error), ctx.getDbCallbackExecutor());
    }

    @Override
    public void destroy() {
    }

    private ListenableFuture<Boolean> checkRelation(TbContext ctx, TbMsg msg) {
        if (EntitySearchDirection.TO.name().equals(config.getDirection())) {
            fromId = EntityIdFactory.getByTypeAndId(config.getEntityType(), config.getEntityId());
            toId = msg.getOriginator();
        } else {
            fromId = EntityIdFactory.getByTypeAndId(config.getEntityType(), config.getEntityId());
            toId = msg.getOriginator();
        }
        return ctx.getRelationService().checkRelation(ctx.getTenantId(), fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
    }

    private void processCreateRelation(TbContext ctx, TbMsg msg) {
        EntityRelation entityRelation = new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
        withCallback(ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), entityRelation),
                filterResult -> ctx.tellNext(msg, filterResult ? SUCCESS : FAILURE), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

}
