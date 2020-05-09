/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;

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
        icon = "add_circle",
        ruleChainTypes = {RuleChainType.SYSTEM, RuleChainType.EDGE}
)
public class TbCreateRelationNode extends TbAbstractRelationActionNode<TbCreateRelationNodeConfiguration> {

    @Override
    protected TbCreateRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateRelationNodeConfiguration.class);
    }

    @Override
    protected boolean createEntityIfNotExists() {
        return config.isCreateEntityIfNotExists();
    }

    @Override
    protected ListenableFuture<RelationContainer> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entity, String relationType) {
        ListenableFuture<Boolean> future = createIfAbsent(ctx, msg, entity, relationType);
        return Futures.transform(future, result -> {
            RelationContainer container = new RelationContainer();
            if (result && config.isChangeOriginatorToRelatedEntity()) {
                TbMsg tbMsg = ctx.transformMsg(msg, msg.getType(), entity.getEntityId(), msg.getMetaData(), msg.getData());
                container.setMsg(tbMsg);
            } else {
                container.setMsg(msg);
            }
            container.setResult(result);
            return container;
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> createIfAbsent(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType) {
        SearchDirectionIds sdId = processSingleSearchDirection(msg, entityContainer);
        ListenableFuture<Boolean> checkRelationFuture = Futures.transformAsync(ctx.getRelationService().checkRelation(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON), result -> {
            if (!result) {
                if (config.isRemoveCurrentRelations()) {
                    return processDeleteRelations(ctx, processFindRelations(ctx, msg, sdId, relationType));
                }
                return Futures.immediateFuture(false);
            }
            return Futures.immediateFuture(true);
        }, ctx.getDbCallbackExecutor());

        return Futures.transformAsync(checkRelationFuture, result -> {
            if (!result) {
                return processCreateRelation(ctx, entityContainer, sdId, relationType);
            }
            return Futures.immediateFuture(true);
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<List<EntityRelation>> processFindRelations(TbContext ctx, TbMsg msg, SearchDirectionIds sdId, String relationType) {
        if (sdId.isOriginatorDirectionFrom()) {
            return ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), relationType, RelationTypeGroup.COMMON);
        } else {
            return ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), relationType, RelationTypeGroup.COMMON);
        }
    }

    private ListenableFuture<Boolean> processDeleteRelations(TbContext ctx, ListenableFuture<List<EntityRelation>> listListenableFuture) {
        return Futures.transformAsync(listListenableFuture, entityRelations -> {
            if (!entityRelations.isEmpty()) {
                List<ListenableFuture<Boolean>> list = new ArrayList<>();
                for (EntityRelation relation : entityRelations) {
                    list.add(ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), relation));
                }
                return Futures.transform(Futures.allAsList(list), result -> false, ctx.getDbCallbackExecutor());
            }
            return Futures.immediateFuture(false);
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processCreateRelation(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        switch (entityContainer.getEntityType()) {
            case ASSET:
                return processAsset(ctx, entityContainer, sdId, relationType);
            case DEVICE:
                return processDevice(ctx, entityContainer, sdId, relationType);
            case CUSTOMER:
                return processCustomer(ctx, entityContainer, sdId, relationType);
            case DASHBOARD:
                return processDashboard(ctx, entityContainer, sdId, relationType);
            case ENTITY_VIEW:
                return processView(ctx, entityContainer, sdId, relationType);
            case EDGE:
                return processEdge(ctx, entityContainer, sdId, relationType);
            case TENANT:
                return processTenant(ctx, entityContainer, sdId, relationType);
        }
        return Futures.immediateFuture(true);
    }

    private ListenableFuture<Boolean> processView(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(entityContainer.getEntityId().getId())), entityView -> {
            if (entityView != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processEdge(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(entityContainer.getEntityId().getId())), edge -> {
            if (edge != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processDevice(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(entityContainer.getEntityId().getId())), device -> {
            if (device != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processAsset(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(entityContainer.getEntityId().getId())), asset -> {
            if (asset != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processCustomer(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(entityContainer.getEntityId().getId())), customer -> {
            if (customer != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processDashboard(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), new DashboardId(entityContainer.getEntityId().getId())), dashboard -> {
            if (dashboard != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processTenant(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), new TenantId(entityContainer.getEntityId().getId())), tenant -> {
            if (tenant != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processSave(TbContext ctx, SearchDirectionIds sdId, String relationType) {
        return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON));
    }

}
