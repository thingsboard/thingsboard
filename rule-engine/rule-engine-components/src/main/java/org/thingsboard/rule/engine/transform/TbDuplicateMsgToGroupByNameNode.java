// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to group by name",
        version = 1,
        configClazz = TbDuplicateMsgToGroupByNameNodeConfiguration.class,
        nodeDescription = "Duplicates message to all entities belonging to resolved Entity group",
        nodeDetails = "Entities are fetched from entity group that is detected according to the configuration. " +
                "When <b>\"search entity group on Tenant level only\"</b> is enabled, the search is restricted to the Tenant level only. " +
                "If <b>\"consider originator as a group owner\"</b> is enabled and the originator is a Tenant or Customer, the search starts from the originator's level and goes up the hierarchy to the tenant level if the group isn't found. " +
                "Otherwise, the search starts at the same level as the message originator's owner. " +
                "Entity group is dynamically resolved based on it's name and type. " +
                "For each entity from group new message is created with entity as originator " +
                "and message parameters copied from original message.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbTransformationNodeDuplicateToGroupByNameConfig",
        icon = "call_split",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/transformation/duplicate-to-group-by-name/"
)
public class TbDuplicateMsgToGroupByNameNode extends TbAbstractDuplicateMsgNode<TbDuplicateMsgToGroupByNameNodeConfiguration> {

    private static final String CONSIDER_MESSAGE_ORIGINATOR_AS_A_GROUP_OWNER = "considerMessageOriginatorAsAGroupOwner";

    @Override
    protected TbDuplicateMsgToGroupByNameNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbDuplicateMsgToGroupByNameNodeConfiguration.class);
        if (Resource.groupResourceFromGroupType(config.getGroupType()) == null) {
            throw new IllegalArgumentException("Entity Type :" + config.getGroupType() + " is not a group entity. " +
                    "Only " + EntityType.GROUP_ENTITY_TYPES + " types are allowed!");
        }
        if (StringUtils.isEmpty(config.getGroupName())) {
            throw new IllegalArgumentException("Group name should be specified!");
        }
        return config;
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        return duplicate(ctx, msg);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg) {
        String groupName = TbNodeUtils.processPattern(config.getGroupName(), msg);
        var entityGroupId = detectTargetEntityGroupId(ctx, msg.getOriginator(), groupName);
        return ctx.getPeContext().getEntityGroupService().findAllEntityIdsAsync(ctx.getTenantId(), entityGroupId, new PageLink(Integer.MAX_VALUE));
    }

    private EntityGroupId detectTargetEntityGroupId(TbContext ctx, EntityId originator, String groupName) {
        if (config.isSearchEntityGroupForTenantOnly()) {
            return tryFindGroupByOwnerId(ctx, ctx.getTenantId(), groupName);
        }
        if (config.isConsiderMessageOriginatorAsAGroupOwner() &&
                (originator.getEntityType() == EntityType.TENANT ||
                        originator.getEntityType() == EntityType.CUSTOMER)) {
            return tryFindGroupByOwnerId(ctx, originator, groupName);
        }
        return tryFindGroupByOwnerId(ctx, ctx.getPeContext().getOwner(ctx.getTenantId(), originator), groupName);
    }

    private EntityGroupId tryFindGroupByOwnerId(TbContext ctx, EntityId ownerId, String groupName) {
        EntityGroupId entityGroupId = ctx.getPeContext().getEntityGroupService()
                .findEntityGroupByTypeAndName(ctx.getTenantId(), ownerId, config.getGroupType(), groupName)
                .map(IdBased::getId).orElse(null);
        if (entityGroupId != null) {
            return entityGroupId;
        } else {
            if (!EntityType.TENANT.equals(ownerId.getEntityType())) {
                return tryFindGroupByOwnerId(ctx, ctx.getPeContext().getOwner(ctx.getTenantId(), ownerId), groupName);
            } else {
                throw new RuntimeException("Can't find group with type: " + config.getGroupType() + " name: " + groupName + "!");
            }
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        if (fromVersion == 0) {
            if (!oldConfiguration.has(CONSIDER_MESSAGE_ORIGINATOR_AS_A_GROUP_OWNER)) {
                hasChanges = true;
                ((ObjectNode) oldConfiguration).put(CONSIDER_MESSAGE_ORIGINATOR_AS_A_GROUP_OWNER, false);
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
