// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@RuleNode(
        type = ComponentType.ACTION,
        name = "remove from group",
        configClazz = TbRemoveFromGroupConfiguration.class,
        nodeDescription = "Removes Message Originator Entity from Entity Group",
        nodeDetails = "Finds target Entity Group by group name pattern and then removes Originator Entity from this group.",
        configDirective = "tbActionNodeRemoveFromGroupConfig",
        icon = "remove_circle",
        ruleChainTypes = RuleChainType.CORE,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/remove-from-group/"
)
public class TbRemoveFromGroupNode extends TbAbstractGroupActionNode<TbRemoveFromGroupConfiguration> {

    @Override
    protected boolean createGroupIfNotExists() {
        return false;
    }

    @Override
    protected TbRemoveFromGroupConfiguration loadGroupNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbRemoveFromGroupConfiguration.class);
    }

    @Override
    protected void doProcessEntityGroupAction(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        EntityId originatorId = msg.getOriginator();
        ctx.getPeContext().getEntityGroupService().removeEntityFromEntityGroup(ctx.getTenantId(), entityGroupId, originatorId);
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

}
