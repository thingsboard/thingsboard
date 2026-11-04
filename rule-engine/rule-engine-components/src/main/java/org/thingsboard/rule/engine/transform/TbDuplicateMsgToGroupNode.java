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
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to group",
        version = 1,
        configClazz = TbDuplicateMsgToGroupNodeConfiguration.class,
        nodeDescription = "Duplicates message to all entities belonging to specific entity group",
        nodeDetails = "Entities are fetched from entity group that is detected according to the configuration. " +
                "Entity group can be specified directly or can be message originator entity itself. " +
                "For each entity from group new message is created with entity as originator " +
                "and message parameters copied from original message.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbTransformationNodeDuplicateToGroupConfig",
        icon = "call_split",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/transformation/duplicate-to-group/"
)
public class TbDuplicateMsgToGroupNode extends TbAbstractDuplicateMsgNode<TbDuplicateMsgToGroupNodeConfiguration> {

    static final String GROUP_OWNER_ID_KEY = "groupOwnerId";

    @Override
    protected TbDuplicateMsgToGroupNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbDuplicateMsgToGroupNodeConfiguration.class);
        if (!config.isEntityGroupIsMessageOriginator()) {
            if (config.getEntityGroupId() == null || config.getEntityGroupId().isNullUid()) {
                throw new IllegalArgumentException("EntityGroupId should be specified!");
            }
            ctx.checkTenantEntity(config.getEntityGroupId());
        }
        return config;
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        return duplicate(ctx, msg);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg) {
        return ctx.getPeContext().getEntityGroupService().findAllEntityIdsAsync(ctx.getTenantId(), detectTargetEntityGroupId(msg.getOriginator()), new PageLink(Integer.MAX_VALUE));
    }

    private EntityGroupId detectTargetEntityGroupId(EntityId original) {
        if (config.isEntityGroupIsMessageOriginator()) {
            if (EntityType.ENTITY_GROUP.equals(original.getEntityType())) {
                return new EntityGroupId(original.getId());
            } else {
                throw new RuntimeException("Message originator is not an entity group!");
            }
        } else {
            return config.getEntityGroupId();
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (oldConfiguration.has(GROUP_OWNER_ID_KEY)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove(GROUP_OWNER_ID_KEY);
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
