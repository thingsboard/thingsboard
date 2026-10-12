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
        name = "unassign entity group from edge",
        configClazz = TbUnassignEntityGroupFromEdgeNodeConfiguration.class,
        nodeDescription = "Unassigns configured Entity Groups from an Edge",
        nodeDetails = "Resolves the target edge either from the message originator (which must be an " +
                "<b>EDGE</b> entity) or by edge name pattern. Each configured entity group is resolved by " +
                "type and name, starting from the edge owner and going up the ownership hierarchy to the " +
                "tenant level. Group type and group name both support templatization. " +
                "Groups that are not currently assigned are skipped. This node never creates entity groups. " +
                "Groups named 'All' and '[Edge] ... All' can not be unassigned, since they are platform-managed. " +
                "Once the group is removed from the edge, the edge deletes the group and its entities locally.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbActionNodeUnassignEntityGroupFromEdgeConfig",
        icon = "router",
        ruleChainTypes = RuleChainType.CORE,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/unassign-entity-group-from-edge/"
)
public class TbUnassignEntityGroupFromEdgeNode extends TbAbstractEdgeEntityGroupActionNode<TbUnassignEntityGroupFromEdgeNodeConfiguration> {

    @Override
    protected Class<TbUnassignEntityGroupFromEdgeNodeConfiguration> getConfigClazz() {
        return TbUnassignEntityGroupFromEdgeNodeConfiguration.class;
    }

    @Override
    protected boolean desiredAssignedState() {
        return false;
    }

    @Override
    protected boolean createGroupIfNotExists() {
        return false;
    }

    @Override
    protected void doApply(TbContext ctx, EdgeId edgeId, ResolvedGroup group) {
        ctx.getPeContext().getEntityGroupService()
                .unassignEntityGroupFromEdge(ctx.getTenantId(), group.id(), edgeId, group.type());
    }

}
