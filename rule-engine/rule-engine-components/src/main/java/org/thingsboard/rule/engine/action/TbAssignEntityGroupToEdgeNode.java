// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;

@RuleNode(
        type = ComponentType.ACTION,
        name = "assign entity group to edge",
        configClazz = TbAssignEntityGroupToEdgeNodeConfiguration.class,
        nodeDescription = "Assigns configured Entity Groups to an Edge",
        nodeDetails = "Resolves the target edge either from the message originator (which must be an " +
                "<b>EDGE</b> entity) or by edge name pattern. Each configured entity group is resolved by " +
                "type and name, starting from the edge owner and going up the ownership hierarchy to the " +
                "tenant level. Group type and group name both support templatization. " +
                "New groups are created under the edge owner when 'Create new group if not exists' is enabled. " +
                "Groups that are already assigned are skipped. " +
                "Groups named 'All' and '[Edge] ... All' can not be assigned.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbActionNodeAssignEntityGroupToEdgeConfig",
        icon = "router",
        ruleChainTypes = RuleChainType.CORE,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/assign-entity-group-to-edge/"
)
public class TbAssignEntityGroupToEdgeNode extends TbAbstractEdgeEntityGroupActionNode<TbAssignEntityGroupToEdgeNodeConfiguration> {

    @Override
    protected Class<TbAssignEntityGroupToEdgeNodeConfiguration> getConfigClazz() {
        return TbAssignEntityGroupToEdgeNodeConfiguration.class;
    }

    @Override
    protected boolean desiredAssignedState() {
        return true;
    }

    @Override
    protected boolean createGroupIfNotExists() {
        return config.isCreateGroupIfNotExists();
    }

    @Override
    protected void doApply(TbContext ctx, EdgeId edgeId, ResolvedGroup group) {
        ctx.getPeContext().getEntityGroupService()
                .assignEntityGroupToEdge(ctx.getTenantId(), group.id(), edgeId, group.type());
    }

}
