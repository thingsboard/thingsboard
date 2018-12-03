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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create relation",
        configClazz = TbCreateRelationNodeConfiguration.class,
        nodeDescription = "Finds target Entity by entity name pattern and then create a relation to Originator Entity by type and direction." +
                " If Selected entity type: Asset, Device or Customer will create new Entity if it doesn't exist and 'Create new entity if not exists' is set to true.",
        nodeDetails = "If the relation already exists or successfully created -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateRelationConfig",
        icon = "add_circle"
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
    protected ListenableFuture<Boolean> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entity) {
        return createIfAbsent(ctx, msg, entity);
    }

    private ListenableFuture<Boolean> createIfAbsent(TbContext ctx, TbMsg msg, EntityContainer entityContainer) {
        processSearchDirection(msg, entityContainer);
        return Futures.transformAsync(ctx.getRelationService().checkRelation(ctx.getTenantId(), fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON),
                result -> {
                    if (!result) {
                        return processCreateRelation(ctx, entityContainer);
                    }
                    return Futures.immediateFuture(true);
                });
    }

    private ListenableFuture<Boolean> processCreateRelation(TbContext ctx, EntityContainer entityContainer) {
        EntityRelation entityRelation = null;
        switch (entityContainer.getEntityType()) {
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetById(ctx.getTenantId(), new AssetId(entityContainer.getEntityId().getId()));
                if (asset == null) {
                    return Futures.immediateFuture(true);
                } else {
                    entityRelation = new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
                }
                break;
            case DEVICE:
                Device device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(entityContainer.getEntityId().getId()));
                if (device == null) {
                    return Futures.immediateFuture(true);
                } else {
                    entityRelation = new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
                }
                break;
            case CUSTOMER:
                Customer customer = ctx.getCustomerService().findCustomerById(ctx.getTenantId(), new CustomerId(entityContainer.getEntityId().getId()));
                if (customer == null) {
                    return Futures.immediateFuture(true);
                } else {
                    entityRelation = new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
                }
                break;
            case DASHBOARD:
                Dashboard dashboard = ctx.getDashboardService().findDashboardById(ctx.getTenantId(), new DashboardId(entityContainer.getEntityId().getId()));
                if (dashboard == null) {
                    return Futures.immediateFuture(true);
                } else {
                    entityRelation = new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
                }
                break;
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(ctx.getTenantId(), new EntityViewId(entityContainer.getEntityId().getId()));
                if (entityView == null) {
                    return Futures.immediateFuture(true);
                } else {
                    entityRelation = new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
                }
                break;
            case TENANT:
                TenantId tenantId = ctx.getTenantId();
                if (!tenantId.equals(entityContainer.getEntityId())) {
                    return Futures.immediateFuture(true);
                } else {
                    entityRelation = new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON);
                }
                break;
        }
        if (entityRelation != null) {
            return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), entityRelation);
        } else {
            return Futures.immediateFuture(true);
        }
    }
}
