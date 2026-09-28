// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.BooleanUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RuleNode(
        type = ComponentType.ACTION,
        name = "add to group",
        configClazz = TbAddToGroupConfiguration.class,
        nodeDescription = "Adds Message Originator Entity to Entity Group",
        nodeDetails = "Finds target Entity Group by group name pattern and then adds Originator Entity to this group. " +
                "Will create new Entity Group if it doesn't exists and 'Create new group if not exists' is set to true.",
        configDirective = "tbActionNodeAddToGroupConfig",
        icon = "add_circle",
        ruleChainTypes = RuleChainType.CORE,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/add-to-group/"
)
public class TbAddToGroupNode extends TbAbstractGroupActionNode<TbAddToGroupConfiguration> {

    @Override
    protected boolean createGroupIfNotExists() {
        return config.isCreateGroupIfNotExists();
    }

    @Override
    protected TbAddToGroupConfiguration loadGroupNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAddToGroupConfiguration.class);
    }

    @Override
    protected void doProcessEntityGroupAction(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        if (BooleanUtils.toBooleanDefaultIfNull(config.isRemoveFromCurrentGroups(), false)) {
            DonAsynchron.withCallback(Futures.allAsList(getListenableFutures(ctx, msg)), containerList -> {
                processRemove(ctx, msg, entityGroupId, containerList);
                addEntityToGroup(ctx, msg, entityGroupId);
            }, throwable -> {
                throw new RuntimeException(throwable);
            });
        } else {
            addEntityToGroup(ctx, msg, entityGroupId);
        }
    }

    private void processRemove(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId, List<EntityGroupContainer> containerList) {
        if (!containerList.isEmpty()) {
            process(ctx, msg, entityGroupId, containerList, getAllGroupId(containerList));
        }

    }

    private EntityGroupId getAllGroupId(List<EntityGroupContainer> containerList) {
        for (EntityGroupContainer groupContainer : containerList) {
            if (groupContainer != null) {
                if (groupContainer.isGroupAll() && !groupContainer.getEntityGroupIds().isEmpty()) {
                    return groupContainer.getEntityGroupIds().get(0);
                }
            } else {
                throw new RuntimeException("Entity Group 'All' doesn't exist");
            }
        }
        throw new RuntimeException("Entity Group 'All' doesn't exist");
    }

    private void process(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId, List<EntityGroupContainer> containerList, EntityGroupId groupAllId) {
        boolean hadFirmware = false;
        boolean hadSoftware = false;
        for (EntityGroupContainer group : containerList) {
            if (!group.isGroupAll() && !group.getEntityGroupIds().isEmpty()) {
                for (EntityGroupId groupId : group.getEntityGroupIds()) {
                    if (!groupId.equals(entityGroupId) && !groupId.equals(groupAllId)) {
                        ctx.getPeContext().getEntityGroupService()
                                .removeEntityFromEntityGroup(ctx.getTenantId(), groupId, msg.getOriginator());
                        if (msg.getOriginator().getEntityType().equals(EntityType.DEVICE)) {
                            if (!hadFirmware) {
                                hadFirmware = ctx.getPeContext().getDeviceGroupOtaPackageService()
                                        .findDeviceGroupOtaPackageByGroupIdAndType(groupId, OtaPackageType.FIRMWARE) != null;
                            }
                            if (!hadSoftware) {
                                hadSoftware = ctx.getPeContext().getDeviceGroupOtaPackageService()
                                        .findDeviceGroupOtaPackageByGroupIdAndType(groupId, OtaPackageType.SOFTWARE) != null;
                            }
                        }
                    }
                }
            }
        }
        if ((hadFirmware || hadSoftware) && msg.getOriginator().getEntityType().equals(EntityType.DEVICE)) {
            ctx.getOtaPackageStateService().update(ctx.getTenantId(), List.of((DeviceId) msg.getOriginator()), hadFirmware, hadSoftware);
        }
    }

    private List<ListenableFuture<EntityGroupContainer>> getListenableFutures(TbContext ctx, TbMsg msg) {
        List<ListenableFuture<EntityGroupContainer>> futures = new ArrayList<>();
        futures.add(Futures.transform(ctx.getPeContext().getEntityGroupService()
                .findEntityGroupsForEntityAsync(ctx.getTenantId(), msg.getOriginator()), list -> new EntityGroupContainer(list, false), ctx.getDbCallbackExecutor()));
        futures.add(Futures.transform(getEntityGroupAllListenableFuture(ctx, msg), entityGroupAll -> entityGroupAll
                .map(entityGroup -> new EntityGroupContainer(Collections.singletonList(entityGroup.getId()), true))
                .orElse(null), ctx.getDbCallbackExecutor()));
        return futures;
    }

    private ListenableFuture<Optional<EntityGroup>> getEntityGroupAllListenableFuture(TbContext ctx, TbMsg msg) {
        ListenableFuture<? extends HasOwnerId> entityFuture;
        switch (msg.getOriginator().getEntityType()) { // TODO: use EntityServiceRegistry
            case DEVICE:
                entityFuture = Futures.immediateFuture(ctx.getDeviceService().findDeviceById(ctx.getTenantId(), (DeviceId) msg.getOriginator()));
                break;
            case ASSET:
                entityFuture = ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) msg.getOriginator());
                break;
            case CUSTOMER:
                entityFuture = ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), (CustomerId) msg.getOriginator());
                break;
            case ENTITY_VIEW:
                entityFuture = ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), (EntityViewId) msg.getOriginator());
                break;
            case EDGE:
                entityFuture = ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), (EdgeId) msg.getOriginator());
                break;
            case DASHBOARD:
                entityFuture = ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), (DashboardId) msg.getOriginator());
                break;
            default:
                throw new RuntimeException("Entity with EntityType: '" + msg.getOriginator().getEntityType() + "'  doesn't support grouping");
        }
        return Futures.transformAsync(entityFuture, entity -> {
            if (entity != null) {
                return ctx.getPeContext().getEntityGroupService().findEntityGroupByTypeAndNameAsync(ctx.getTenantId(),
                        entity.getOwnerId(), msg.getOriginator().getEntityType(), EntityGroup.GROUP_ALL_NAME);
            } else {
                return Futures.immediateFuture(Optional.empty());
            }
        }, ctx.getDbCallbackExecutor());
    }

    private void addEntityToGroup(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        EntityId originatorId = msg.getOriginator();
        ctx.getPeContext().getEntityGroupService().addEntityToEntityGroup(ctx.getTenantId(), entityGroupId, originatorId);
        if (originatorId.getEntityType().equals(EntityType.DEVICE)) {
            DeviceGroupOtaPackage fw =
                    ctx.getPeContext().getDeviceGroupOtaPackageService().findDeviceGroupOtaPackageByGroupIdAndType(entityGroupId, OtaPackageType.FIRMWARE);
            DeviceGroupOtaPackage sw =
                    ctx.getPeContext().getDeviceGroupOtaPackageService().findDeviceGroupOtaPackageByGroupIdAndType(entityGroupId, OtaPackageType.SOFTWARE);
            if (fw != null || sw != null) {
                ctx.getOtaPackageStateService().update(ctx.getTenantId(), List.of((DeviceId) originatorId), fw != null, sw != null);
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class EntityGroupContainer {

        private List<EntityGroupId> entityGroupIds;
        private boolean groupAll;

    }

}
