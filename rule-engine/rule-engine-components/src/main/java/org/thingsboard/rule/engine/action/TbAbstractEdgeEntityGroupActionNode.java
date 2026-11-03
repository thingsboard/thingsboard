// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

public abstract class TbAbstractEdgeEntityGroupActionNode<C extends TbAbstractEdgeEntityGroupActionNodeConfiguration> implements TbNode {

    private static final String SUPPORTED_TYPES_SUFFIX = " Only " + EdgeUtils.EDGE_SUPPORTED_GROUP_TYPES + " group types are supported for edge group actions.";

    protected C config;

    private EntityGroupResolver groupResolver;
    private List<ConfiguredGroup> configuredGroups;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, getConfigClazz());
        validateConfig();
        this.configuredGroups = prepareGroups();
        this.groupResolver = new EntityGroupResolver(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(process(ctx, msg),
                v -> ctx.tellSuccess(msg),
                t -> ctx.tellFailure(msg, t),
                ctx.getDbCallbackExecutor());
    }

    protected abstract Class<C> getConfigClazz();

    /** The assignment state this node exists to produce: true for assign, false for unassign. */
    protected abstract boolean desiredAssignedState();

    protected abstract boolean createGroupIfNotExists();

    protected abstract void doApply(TbContext ctx, EdgeId edgeId, ResolvedGroup group);

    private ListenableFuture<Void> process(TbContext ctx, TbMsg msg) {
        return Futures.transformAsync(resolveEdge(ctx, msg),
                edge -> apply(ctx, edge, resolveGroups(msg, edge)),
                ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Edge> resolveEdge(TbContext ctx, TbMsg msg) {
        return switch (config.getEdgeSource()) {
            case MESSAGE_ORIGINATOR -> resolveEdgeFromOriginator(ctx, msg);
            case EDGE_NAME_PATTERN -> resolveEdgeByNamePattern(ctx, msg);
        };
    }

    private ListenableFuture<Edge> resolveEdgeFromOriginator(TbContext ctx, TbMsg msg) {
        EntityId originator = msg.getOriginator();
        if (!EntityType.EDGE.equals(originator.getEntityType())) {
            return Futures.immediateFailedFuture(new RuntimeException(
                    "Message originator must be an EDGE entity, but was " + originator.getEntityType() + "!"));
        }
        EdgeId edgeId = new EdgeId(originator.getId());
        String notFound = "Edge with id '" + edgeId.getId() + "' was not found!";
        return Futures.transform(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), edgeId),
                edge -> {
                    Edge found = requireFound(edge, notFound);
                    // findEdgeByIdAsync does not filter by tenant; same wording, so this cannot be used to probe other tenants
                    if (!found.getTenantId().equals(ctx.getTenantId())) {
                        throw new RuntimeException(notFound);
                    }
                    return found;
                }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Edge> resolveEdgeByNamePattern(TbContext ctx, TbMsg msg) {
        String edgeName = TbNodeUtils.processPattern(config.getEdgeNamePattern(), msg);
        if (StringUtils.isBlank(edgeName)) {
            return Futures.immediateFailedFuture(new RuntimeException(
                    "Resolved edge name is empty for pattern '" + config.getEdgeNamePattern() + "'!"));
        }
        if (isTemplatized(edgeName)) {
            return Futures.immediateFailedFuture(new RuntimeException(
                    "Edge name pattern '" + config.getEdgeNamePattern() + "' was not resolved!"));
        }
        return Futures.transform(ctx.getEdgeService().findEdgeByTenantIdAndNameAsync(ctx.getTenantId(), edgeName),
                edge -> requireFound(edge, "Edge with name '" + edgeName + "' was not found!"),
                ctx.getDbCallbackExecutor());
    }

    private static Edge requireFound(Edge edge, String notFoundMessage) {
        if (edge == null) {
            throw new RuntimeException(notFoundMessage);
        }
        return edge;
    }

    // Sequential rather than fanned out: each row can walk the whole owner chain, and the first bad row should fail deterministically.
    private List<ResolvedGroup> resolveGroups(TbMsg msg, Edge edge) {
        List<ResolvedGroup> resolved = new ArrayList<>(configuredGroups.size());
        Set<EntityGroupId> seen = new HashSet<>();
        for (ConfiguredGroup group : configuredGroups) {
            EntityType groupType = resolveGroupType(group, msg);
            String groupName = resolveGroupName(group, msg);
            EntityGroupId groupId = resolveGroup(edge.getOwnerId(), groupType, groupName);
            if (seen.add(groupId)) {
                resolved.add(new ResolvedGroup(groupId, groupType));
            }
        }
        return resolved;
    }

    private EntityType resolveGroupType(ConfiguredGroup group, TbMsg msg) {
        if (group.literalType() != null) {
            return group.literalType();
        }
        String resolved = TbNodeUtils.processPattern(group.typePattern(), msg);
        if (StringUtils.isBlank(resolved)) {
            throw new RuntimeException("Resolved group type is empty for pattern '" + group.typePattern() + "'!");
        }
        EntityType groupType = parseGroupType(resolved);
        if (groupType == null) {
            throw new RuntimeException(unsupportedGroupTypeError(resolved, group.typePattern()));
        }
        return groupType;
    }

    private String resolveGroupName(ConfiguredGroup group, TbMsg msg) {
        if (group.literalName() != null) {
            return group.literalName();
        }
        String groupName = TbNodeUtils.processPattern(group.namePattern(), msg);
        if (StringUtils.isBlank(groupName)) {
            throw new RuntimeException("Resolved group name is empty for pattern '" + group.namePattern() + "'!");
        }
        if (isTemplatized(groupName)) {
            throw new RuntimeException("Group name pattern '" + group.namePattern() + "' was not resolved!");
        }
        if (isReservedGroupName(groupName)) {
            throw new RuntimeException(reservedGroupNameError(groupName));
        }
        return groupName;
    }

    private EntityGroupId resolveGroup(EntityId startingOwnerId, EntityType groupType, String groupName) {
        return createGroupIfNotExists()
                ? groupResolver.resolveOrCreate(startingOwnerId, groupType, groupName)
                : groupResolver.resolve(startingOwnerId, groupType, groupName);
    }

    // Groups already in the desired state are skipped: the redundant call would publish an edge event that
    // makes the edge re-sync every entity in the group for nothing.
    private ListenableFuture<Void> apply(TbContext ctx, Edge edge, List<ResolvedGroup> groups) {
        List<ListenableFuture<Decision>> decisions = new ArrayList<>(groups.size());
        for (ResolvedGroup group : groups) {
            decisions.add(Futures.transform(
                    ctx.getPeContext().getEntityGroupService().checkEntityGroupAssignedToEdgeAsync(
                            ctx.getTenantId(), edge.getId(), group.id(), group.type()),
                    // Unknown counts as not assigned, which is the safe side for both: assign retries, unassign skips
                    assigned -> new Decision(group, Boolean.TRUE.equals(assigned) != desiredAssignedState()),
                    ctx.getDbCallbackExecutor()));
        }
        return Futures.transform(Futures.allAsList(decisions), decided -> {
            for (Decision decision : decided) {
                if (decision.apply()) {
                    doApply(ctx, edge.getId(), decision.group());
                }
            }
            return null;
        }, ctx.getDbCallbackExecutor());
    }

    private void validateConfig() throws TbNodeException {
        if (config.getEdgeSource() == null) {
            throw new TbNodeException("Edge source should be specified!", true);
        }
        if (EdgeSource.EDGE_NAME_PATTERN == config.getEdgeSource() && StringUtils.isBlank(config.getEdgeNamePattern())) {
            throw new TbNodeException("Edge name pattern should be specified!", true);
        }
        if (config.getEntityGroups() == null || config.getEntityGroups().isEmpty()) {
            throw new TbNodeException("At least one entity group should be specified!", true);
        }
    }

    // processPattern re-serialises the metadata and re-parses the whole message body on every call, so a value
    // with no placeholder in it must never reach it at runtime.
    private List<ConfiguredGroup> prepareGroups() throws TbNodeException {
        List<ConfiguredGroup> prepared = new ArrayList<>(config.getEntityGroups().size());
        for (EntityGroupPattern pattern : config.getEntityGroups()) {
            if (StringUtils.isBlank(pattern.getGroupType())) {
                throw new TbNodeException("Group type should be specified!", true);
            }
            if (StringUtils.isBlank(pattern.getGroupName())) {
                throw new TbNodeException("Group name should be specified!", true);
            }
            EntityType literalType = null;
            if (!isTemplatized(pattern.getGroupType())) {
                literalType = parseGroupType(pattern.getGroupType());
                if (literalType == null) {
                    throw new TbNodeException(unsupportedGroupTypeError(pattern.getGroupType()), true);
                }
            }
            String literalName = null;
            if (!isTemplatized(pattern.getGroupName())) {
                if (isReservedGroupName(pattern.getGroupName())) {
                    throw new TbNodeException(reservedGroupNameError(pattern.getGroupName()), true);
                }
                literalName = pattern.getGroupName();
            }
            prepared.add(new ConfiguredGroup(pattern.getGroupType(), literalType, pattern.getGroupName(), literalName));
        }
        return prepared;
    }

    static boolean isTemplatized(String value) {
        return value != null && (value.contains("${") || value.contains("$["));
    }

    // Platform-managed groups: assigning either produces no edge event, so it would be a silent no-op.
    static boolean isReservedGroupName(String groupName) {
        return EntityGroup.GROUP_ALL_NAME.equals(groupName) || EdgeUtils.isEdgeGroupAll(groupName);
    }

    static String reservedGroupNameError(String groupName) {
        return "Entity group '" + groupName + "' can not be used for edge group actions!";
    }

    static String unsupportedGroupTypeError(String value) {
        return "Unsupported entity group type '" + value + "'!" + SUPPORTED_TYPES_SUFFIX;
    }

    static String unsupportedGroupTypeError(String resolved, String groupTypePattern) {
        return "Unsupported entity group type '" + resolved + "' resolved from pattern '"
                + groupTypePattern + "'!" + SUPPORTED_TYPES_SUFFIX;
    }

    /** Returns null when the value is not one of the edge-supported group types. */
    static EntityType parseGroupType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        EntityType groupType;
        try {
            groupType = EntityType.valueOf(value.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
        return EdgeUtils.EDGE_SUPPORTED_GROUP_TYPES.contains(groupType) ? groupType : null;
    }

    // literalType and literalName are null exactly when that pattern needs the message to resolve.
    private record ConfiguredGroup(String typePattern, EntityType literalType, String namePattern, String literalName) {}

    protected record ResolvedGroup(EntityGroupId id, EntityType type) {}

    private record Decision(ResolvedGroup group, boolean apply) {}

}
