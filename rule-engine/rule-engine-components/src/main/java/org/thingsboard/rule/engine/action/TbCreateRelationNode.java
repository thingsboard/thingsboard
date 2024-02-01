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
import org.springframework.util.CollectionUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Device;
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
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
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
    protected ListenableFuture<RelationContainer> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityId targetEntityId, String relationType) {
        ListenableFuture<Boolean> future = createRelationIfAbsent(ctx, msg, targetEntityId, relationType);
        return Futures.transform(future, result -> {
            if (result && config.isChangeOriginatorToRelatedEntity()) {
                TbMsg tbMsg = ctx.transformMsgOriginator(msg, targetEntityId);
                return new RelationContainer(tbMsg, result);
            }
            return new RelationContainer(msg, result);
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> createRelationIfAbsent(TbContext ctx, TbMsg msg, EntityId targetEntityId, String relationType) {
        SearchDirectionIds sdId = processSingleSearchDirection(msg, targetEntityId);
        return Futures.transformAsync(deleteCurrentRelationsIfNeeded(ctx, msg, sdId, relationType), v ->
                        checkRelationAndCreateIfAbsent(ctx, targetEntityId, relationType, sdId),
                ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Void> deleteCurrentRelationsIfNeeded(TbContext ctx, TbMsg msg, SearchDirectionIds sdId, String relationType) {
        return config.isRemoveCurrentRelations() ?
                deleteOriginatorRelations(ctx, findOriginatorRelations(ctx, msg, sdId, relationType)) :
                Futures.immediateFuture(null);
    }

    private ListenableFuture<List<EntityRelation>> findOriginatorRelations(TbContext ctx, TbMsg msg, SearchDirectionIds sdId, String relationType) {
        return sdId.isOriginatorDirectionFrom() ?
                ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), relationType, RelationTypeGroup.COMMON) :
                ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), relationType, RelationTypeGroup.COMMON);
    }

    private ListenableFuture<Void> deleteOriginatorRelations(TbContext ctx, ListenableFuture<List<EntityRelation>> originatorRelationsFuture) {
        return Futures.transformAsync(originatorRelationsFuture, originatorRelations -> {
            List<ListenableFuture<Boolean>> list = new ArrayList<>();
            if (!CollectionUtils.isEmpty(originatorRelations)) {
                for (EntityRelation relation : originatorRelations) {
                    list.add(ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), relation));
                }
            }
            return Futures.transform(Futures.allAsList(list), result -> null, ctx.getDbCallbackExecutor());
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> checkRelationAndCreateIfAbsent(TbContext ctx, EntityId targetEntityId, String relationType, SearchDirectionIds sdId) {
        return Futures.transformAsync(checkRelation(ctx, sdId, relationType), relationPresent ->
                relationPresent ?
                        Futures.immediateFuture(true) :
                        processCreateRelation(ctx, targetEntityId, sdId, relationType), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> checkRelation(TbContext ctx, SearchDirectionIds sdId, String relationType) {
        return ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON);
    }

    private ListenableFuture<Boolean> processCreateRelation(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        switch (targetEntityId.getEntityType()) {
            case ASSET:
                return processAsset(ctx, targetEntityId, sdId, relationType);
            case DEVICE:
                return processDevice(ctx, targetEntityId, sdId, relationType);
            case CUSTOMER:
                return processCustomer(ctx, targetEntityId, sdId, relationType);
            case DASHBOARD:
                return processDashboard(ctx, targetEntityId, sdId, relationType);
            case ENTITY_VIEW:
                return processView(ctx, targetEntityId, sdId, relationType);
            case EDGE:
                return processEdge(ctx, targetEntityId, sdId, relationType);
            case TENANT:
                return processTenant(ctx, targetEntityId, sdId, relationType);
            case USER:
                return processUser(ctx, targetEntityId, sdId, relationType);
        }
        return Futures.immediateFuture(true);
    }

    private ListenableFuture<Boolean> processView(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(targetEntityId.getId())),
                entityView -> entityView != null ? processSave(ctx, sdId, relationType) : Futures.immediateFuture(true), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processEdge(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(targetEntityId.getId())),
                edge -> edge != null ? processSave(ctx, sdId, relationType) : Futures.immediateFuture(true), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processDevice(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        Device device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(targetEntityId.getId()));
        return device != null ? processSave(ctx, sdId, relationType) : Futures.immediateFuture(true);
    }

    private ListenableFuture<Boolean> processAsset(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(targetEntityId.getId())),
                asset -> asset != null ? processSave(ctx, sdId, relationType) : Futures.immediateFuture(true), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processCustomer(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(targetEntityId.getId())),
                customer -> customer != null ? processSave(ctx, sdId, relationType) : Futures.immediateFuture(true), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processDashboard(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), new DashboardId(targetEntityId.getId())),
                dashboard -> dashboard != null ? processSave(ctx, sdId, relationType) : Futures.immediateFuture(true), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processTenant(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), TenantId.fromUUID(targetEntityId.getId())),
                tenant -> tenant != null ? processSave(ctx, sdId, relationType) : Futures.immediateFuture(true), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processUser(TbContext ctx, EntityId targetEntityId, SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(targetEntityId.getId())),
                user -> user != null ? processSave(ctx, sdId, relationType) : Futures.immediateFuture(true), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processSave(TbContext ctx, SearchDirectionIds sdId, String relationType) {
        return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON));
    }

}
