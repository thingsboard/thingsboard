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
package org.thingsboard.rule.engine.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@RuleNode(
        type = ComponentType.FILTER,
        name = "check relation presence",
        configClazz = TbCheckRelationNodeConfiguration.class,
        version = 1,
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        nodeDescription = "Checks the presence of the relation between the originator of the message and other entities.",
        nodeDetails = "If 'check relation to specific entity' is selected, you should specify a related entity. " +
                "Otherwise, the rule node checks the presence of a relation to any entity. " +
                "In both cases, relation lookup is based on configured direction and type.<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>",
        configDirective = "tbFilterNodeCheckRelationConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/filter/check-relation-presence/"
)
public class TbCheckRelationNode implements TbNode {

    private TbCheckRelationNodeConfiguration config;
    private EntityId singleEntityId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckRelationNodeConfiguration.class);
        if (config.isCheckForSingleEntity()) {
            if (StringUtils.isEmpty(config.getEntityType()) || StringUtils.isEmpty(config.getEntityId())) {
                throw new TbNodeException("Entity should be specified!");
            }
            this.singleEntityId = EntityIdFactory.getByTypeAndId(config.getEntityType(), config.getEntityId());
            ctx.checkTenantEntity(singleEntityId);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ListenableFuture<Boolean> checkRelationFuture = config.isCheckForSingleEntity() ?
                processSingle(ctx, msg) : processList(ctx, msg);
        withCallback(checkRelationFuture,
                filterResult -> ctx.tellNext(msg, filterResult ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processSingle(TbContext ctx, TbMsg msg) {
        EntityId from;
        EntityId to;
        if (EntitySearchDirection.FROM.name().equals(config.getDirection())) {
            to = singleEntityId;
            from = msg.getOriginator();
        } else {
            from = singleEntityId;
            to = msg.getOriginator();
        }
        return ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), from, to, config.getRelationType(), RelationTypeGroup.COMMON);
    }

    private ListenableFuture<Boolean> processList(TbContext ctx, TbMsg msg) {
        ListenableFuture<List<EntityRelation>> relationListFuture = EntitySearchDirection.FROM.name().equals(config.getDirection()) ?
                ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON) :
                ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON);
        return Futures.transformAsync(relationListFuture, this::isEmptyList, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> isEmptyList(List<EntityRelation> entityRelations) {
        return entityRelations.isEmpty() ? Futures.immediateFuture(false) : Futures.immediateFuture(true);
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        if (fromVersion == 0) {
            var newConfigObjectNode = (ObjectNode) oldConfiguration;
            var directionPropertyName = "direction";
            if (!newConfigObjectNode.has(directionPropertyName)) {
                throw new TbNodeException("property to update: '" + directionPropertyName + "' doesn't exists in configuration!");
            }
            String direction = newConfigObjectNode.get(directionPropertyName).asText();
            if (EntitySearchDirection.TO.name().equals(direction)) {
                newConfigObjectNode.put(directionPropertyName, EntitySearchDirection.FROM.name());
                return new TbPair<>(true, newConfigObjectNode);
            }
            if (EntitySearchDirection.FROM.name().equals(direction)) {
                newConfigObjectNode.put(directionPropertyName, EntitySearchDirection.TO.name());
                return new TbPair<>(true, newConfigObjectNode);
            }
            throw new TbNodeException("property to update: '" + directionPropertyName + "' has invalid value!");
        }
        return new TbPair<>(false, oldConfiguration);
    }

}
