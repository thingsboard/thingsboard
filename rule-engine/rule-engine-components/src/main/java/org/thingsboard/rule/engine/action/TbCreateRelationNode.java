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
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create relation",
        configClazz = TbCreateRelationNodeConfiguration.class,
        nodeDescription = "Finds target Entity by entity name pattern and (entity type pattern for Asset, Device) and then create a relation to Originator Entity by type and direction." +
                " If Selected entity type: Asset, Device or Customer will create new Entity if it doesn't exist and selected checkbox 'Create new entity if not exists'.<br>" +
                " In case that relation from the message originator to the selected entity not exist and  If selected checkbox 'Remove current relations'," +
                " before creating the new relation all existed relations to message originator by type and direction will be removed.<br>" +
                " If relation from the message originator to the selected entity created and If selected checkbox 'Change originator to related entity'," +
                " outbound message will be processed as a message from this entity.",
        nodeDetails = "If the relation already exists or successfully created -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateRelationConfig",
        icon = "add_circle"
)
public class TbCreateRelationNode extends TbAbstractRelationActionNode<TbCreateRelationNodeConfiguration> {

    @Override
    protected TbCreateRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        var createRelationNodeConfiguration = TbNodeUtils.convert(configuration, TbCreateRelationNodeConfiguration.class);
        checkIfConfigEntityTypeIsSupported(createRelationNodeConfiguration.getEntityType());
        return createRelationNodeConfiguration;
    }

    @Override
    protected boolean createEntityIfNotExists() {
        return config.isCreateEntityIfNotExists();
    }

    @Override
    protected ListenableFuture<TbPair<TbMsg, Boolean>> processEntityRelationAction(TbContext ctx, TbMsg msg) {
        return Futures.transformAsync(getTargetEntityId(ctx, msg), targetEntityId -> {
            var createRelationFuture = createRelationIfAbsent(ctx, msg, targetEntityId);
            return Futures.transform(createRelationFuture, result -> {
                if (result && config.isChangeOriginatorToRelatedEntity()) {
                    TbMsg tbMsg = ctx.transformMsgOriginator(msg, targetEntityId);
                    return new TbPair<>(tbMsg, result);
                }
                return new TbPair<>(msg, result);
            }, MoreExecutors.directExecutor());
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> createRelationIfAbsent(TbContext ctx, TbMsg msg, EntityId targetEntityId) {
        var originator = msg.getOriginator();
        var relationType = processPattern(msg, config.getRelationType());
        return Futures.transformAsync(deleteCurrentRelationsIfNeeded(ctx, originator, relationType), v ->
                        checkRelationAndCreateIfAbsent(ctx, originator, targetEntityId, relationType),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> deleteCurrentRelationsIfNeeded(TbContext ctx, EntityId originator, String relationType) {
        if (!config.isRemoveCurrentRelations()) {
            return Futures.immediateFuture(null);
        }
        var originatorRelationsFuture = EntitySearchDirection.FROM.equals(config.getDirection()) ?
                ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), originator, relationType, RelationTypeGroup.COMMON) :
                ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), originator, relationType, RelationTypeGroup.COMMON);
        return Futures.transformAsync(originatorRelationsFuture, originatorRelations -> {
            if (CollectionUtils.isEmpty(originatorRelations)) {
                return Futures.immediateFuture(null);
            }
            var deletedRelationsFutures = originatorRelations.stream()
                    .map(relation -> ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), relation))
                    .collect(Collectors.toCollection(ArrayList::new));
            return Futures.transform(Futures.allAsList(deletedRelationsFutures), result -> null, MoreExecutors.directExecutor());
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> checkRelationAndCreateIfAbsent(TbContext ctx, EntityId originator, EntityId targetEntityId, String relationType) {
        EntityId fromId;
        EntityId toId;
        if (EntitySearchDirection.FROM.equals(config.getDirection())) {
            fromId = originator;
            toId = targetEntityId;
        } else {
            toId = originator;
            fromId = targetEntityId;
        }
        var checkRelationFuture = ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), fromId, toId, relationType, RelationTypeGroup.COMMON);
        return Futures.transformAsync(checkRelationFuture, relationPresent ->
                relationPresent ?
                        Futures.immediateFuture(true) :
                        processCreateRelation(ctx, fromId, toId, targetEntityId, relationType), MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> processCreateRelation(TbContext ctx, EntityId fromId, EntityId toId, EntityId targetEntityId, String relationType) {
        switch (targetEntityId.getEntityType()) {
            case ASSET:
                return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(targetEntityId.getId())),
                        asset -> asset != null ? saveRelation(ctx, fromId, toId, relationType) : Futures.immediateFuture(true), MoreExecutors.directExecutor());
            case DEVICE:
                var device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(targetEntityId.getId()));
                return device != null ? saveRelation(ctx, fromId, toId, relationType) : Futures.immediateFuture(true);
            case CUSTOMER:
                return Futures.transformAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(targetEntityId.getId())),
                        customer -> customer != null ? saveRelation(ctx, fromId, toId, relationType) : Futures.immediateFuture(true), MoreExecutors.directExecutor());
            case DASHBOARD:
                return Futures.transformAsync(ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), new DashboardId(targetEntityId.getId())),
                        dashboard -> dashboard != null ? saveRelation(ctx, fromId, toId, relationType) : Futures.immediateFuture(true), MoreExecutors.directExecutor());
            case ENTITY_VIEW:
                return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(targetEntityId.getId())),
                        entityView -> entityView != null ? saveRelation(ctx, fromId, toId, relationType) : Futures.immediateFuture(true), MoreExecutors.directExecutor());
            case EDGE:
                return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(targetEntityId.getId())),
                        edge -> edge != null ? saveRelation(ctx, fromId, toId, relationType) : Futures.immediateFuture(true), MoreExecutors.directExecutor());
            case TENANT:
                return Futures.transformAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), TenantId.fromUUID(targetEntityId.getId())),
                        tenant -> tenant != null ? saveRelation(ctx, fromId, toId, relationType) : Futures.immediateFuture(true), MoreExecutors.directExecutor());
            case USER:
                return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(targetEntityId.getId())),
                        user -> user != null ? saveRelation(ctx, fromId, toId, relationType) : Futures.immediateFuture(true), MoreExecutors.directExecutor());
        }
        return Futures.immediateFuture(true);
    }

    private ListenableFuture<Boolean> saveRelation(TbContext ctx, EntityId fromId, EntityId toId, String relationType) {
        return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(fromId, toId, relationType, RelationTypeGroup.COMMON));
    }

}
