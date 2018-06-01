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
package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

import javax.management.relation.RelationType;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check relation",
        configClazz = TbCheckRelationNodeConfiguration.class,
        relationTypes = {"True", "False"},
        nodeDescription = "Checks the relation from the selected entity to originator of the message by type and direction",
        nodeDetails = "If relation exists - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckRelationConfig")
public class TbCheckRelationNode implements TbNode {

    private TbCheckRelationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckRelationNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        EntityId from;
        EntityId to;
        if (EntitySearchDirection.FROM.name().equals(config.getDirection())) {
            from = EntityIdFactory.getByTypeAndId(config.getEntityType(), config.getEntityId());
            to = msg.getOriginator();
        } else {
            to = EntityIdFactory.getByTypeAndId(config.getEntityType(), config.getEntityId());
            from = msg.getOriginator();
        }
        withCallback(ctx.getRelationService().checkRelation(from, to, config.getRelationType(), RelationTypeGroup.COMMON),
                filterResult -> ctx.tellNext(msg, filterResult ? "True" : "False"), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    @Override
    public void destroy() {

    }
}
