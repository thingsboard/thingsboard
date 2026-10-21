// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.BaseEntityGroupService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows.EdgeAssignmentRowRef;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.edge.profile.EdgeProfileRows.isStamped;
import static org.thingsboard.server.service.edge.profile.EdgeProfileRows.isStampedBy;
import static org.thingsboard.server.service.edge.profile.EdgeProfileRows.ownerOf;
import static org.thingsboard.server.service.edge.profile.EdgeProfileRows.rowsOf;
import static org.thingsboard.server.service.edge.profile.EdgeProfileRows.stamp;
import static org.thingsboard.server.service.edge.profile.EdgeProfileRows.unstamped;

/**
 * Applies edge profiles to edges; hand-made, unstamped assignments are never touched.
 * Runs each per-edge step in its own transaction under a cluster-wide advisory lock on the edge.
 * Callers must not already hold a transaction: the edge lock is always taken before any relation lock.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EdgeProfileApplier {

    private final EdgeProfileService edgeProfileService;
    private final EdgeService edgeService;
    private final RelationService relationService;
    private final RuleChainService ruleChainService;
    private final EntityGroupService entityGroupService;
    private final IntegrationService integrationService;
    private final SchedulerEventService schedulerEventService;
    private final EdgeProfileEligibility edgeProfileEligibility;
    private final EdgeEventService edgeEventService;
    // core-only bean; the root is only ever pushed on the core, while this applier also lives on rule-engine nodes
    private final ObjectProvider<RuleChainEdgeProcessor> ruleChainEdgeProcessor;

    @Value("${edges.profile.apply_lock_timeout_ms:60000}")
    private long lockTimeoutMs = 60_000;

    /**
     * Owner of the entity a row points at; {@code present} is false when that entity no longer exists.
     * {@code customerBound} marks entities that cannot exist on an edge without their customer (scheduler events, users).
     */
    private record RowScope(boolean present, EntityId ownerId, boolean customerBound) {
        static final RowScope MISSING = new RowScope(false, null, false);

    }

    @Transactional(rollbackFor = Exception.class)
    public Edge applyToEdge(TenantId tenantId, EdgeId edgeId, boolean created, boolean forceRoot, EdgeProfileId previousProfileId) throws Exception {
        Edge edge = lock(tenantId, edgeId);
        if (edge == null) {
            return null;
        }
        EdgeProfile profile = currentProfile(tenantId, edge);
        if (profile == null) {
            return edge;
        }
        return applyToEdge(tenantId, edge, profile, created, forceRoot, previousProfileId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean applyAfterOwnerChange(TenantId tenantId, EdgeId edgeId) throws Exception {
        return applyToEdge(tenantId, edgeId, false, false, null) != null;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addRows(TenantId tenantId, EdgeId edgeId, EdgeProfileId edgeProfileId, List<EdgeAssignmentRowRef> rows,
                           boolean moveRoot, RuleChainId root, RuleChainId previousRoot) throws Exception {
        Edge edge = lock(tenantId, edgeId);
        if (edge == null || !edgeProfileId.equals(edge.getEdgeProfileId())) {
            return false;
        }
        EdgeProfile profile = edgeProfileService.findEdgeProfileById(tenantId, edgeProfileId);
        if (profile == null) {
            return false;
        }
        List<EdgeAssignmentRowRef> profileRows = rowsOf(profile);
        boolean changed = false;
        for (EdgeAssignmentRowRef row : rows) {
            if (profileRows.contains(row)) {
                changed |= applyRowToEdge(tenantId, profile, row, edge);
            }
        }
        if (moveRoot) {
            changed |= moveRoot(tenantId, edge, root, previousRoot);
        }
        return changed;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean removeRow(TenantId tenantId, EdgeId edgeId, EdgeProfileId edgeProfileId, EdgeAssignmentRowRef row) {
        Edge edge = lock(tenantId, edgeId);
        if (edge == null) {
            return false;
        }
        EdgeProfile profile = edgeProfileService.findEdgeProfileById(tenantId, edgeProfileId);
        if (profile != null && rowsOf(profile).contains(row)) {
            return false;
        }
        EdgeProfile currentProfile = edge.getEdgeProfileId() != null ? edgeProfileService.findEdgeProfileById(tenantId, edge.getEdgeProfileId()) : null;
        return unassignRowFromEdge(tenantId, edgeProfileId, row, edge, currentProfile);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean reapply(TenantId tenantId, EdgeId edgeId, EdgeProfileId edgeProfileId) throws Exception {
        Edge edge = lock(tenantId, edgeId);
        if (edge == null || !edgeProfileId.equals(edge.getEdgeProfileId())) {
            return false;
        }
        EdgeProfile profile = edgeProfileService.findEdgeProfileById(tenantId, edgeProfileId);
        if (profile == null) {
            return false;
        }
        reapplyEdge(tenantId, profile, edge);
        return true;
    }

    private Edge lock(TenantId tenantId, EdgeId edgeId) {
        edgeProfileService.lockEdgeForApply(tenantId, edgeId, lockTimeoutMs);
        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException("Interrupted while waiting for the apply lock of edge " + edgeId);
        }
        return edgeService.findEdgeById(tenantId, edgeId);
    }

    private EdgeProfile currentProfile(TenantId tenantId, Edge edge) {
        if (edge.getEdgeProfileId() == null) {
            log.warn("[{}][{}] Edge has no profile, nothing to apply", tenantId, edge.getId());
            return null;
        }
        EdgeProfile profile = edgeProfileService.findEdgeProfileById(tenantId, edge.getEdgeProfileId());
        if (profile == null) {
            log.warn("[{}][{}] Edge references missing profile {}", tenantId, edge.getId(), edge.getEdgeProfileId());
        }
        return profile;
    }

    // an edge moving between profiles that share a row keeps the old stamp; the new profile has to claim the row to manage it
    private static boolean stampedByAnotherProfile(EntityRelation relation, EdgeProfileId edgeProfileId) {
        return isStamped(relation) && !isStampedBy(relation, edgeProfileId);
    }

    private Edge applyToEdge(TenantId tenantId, Edge edge, EdgeProfile profile, boolean created, boolean forceRoot, EdgeProfileId previousProfileId) throws Exception {
        List<EdgeAssignmentRowRef> rows = rowsOf(profile);
        for (EdgeAssignmentRowRef row : rows) {
            applyRowToEdge(tenantId, profile, row, edge);
        }
        RuleChainId root = profile.getRootRuleChainId();
        // an existing edge keeps its own root; it takes the profile root on create and on a profile change only
        // the new root has to reach the edge before the old profile's rows are dropped: an edge refuses to delete its current root
        Edge result = root != null && (created || forceRoot) ? setEdgeRoot(tenantId, edge, root) : edge;
        if (previousProfileId != null && !previousProfileId.equals(profile.getId())) {
            dropRowsOfPreviousProfile(tenantId, result, previousProfileId, rows);
        }
        return result;
    }

    private void dropRowsOfPreviousProfile(TenantId tenantId, Edge edge, EdgeProfileId previousProfileId, List<EdgeAssignmentRowRef> keep) {
        EdgeProfile previous = edgeProfileService.findEdgeProfileById(tenantId, previousProfileId);
        if (previous == null) {
            log.debug("[{}][{}] Previous edge profile {} no longer exists, nothing to drop", tenantId, edge.getId(), previousProfileId);
            return;
        }
        Set<EntityId> kept = keep.stream().map(EdgeAssignmentRowRef::id).collect(Collectors.toSet());
        for (EdgeAssignmentRowRef row : rowsOf(previous)) {
            if (kept.contains(row.id())) {
                continue;
            }
            EntityRelation relation = relationService.getRelation(tenantId, edge.getId(), row.id(), row.relationType(), RelationTypeGroup.EDGE);
            if (relation == null || !isStampedBy(relation, previousProfileId)) {
                continue;
            }
            if (isEdgeRoot(edge, row)) {
                keepAsEdgeOwn(tenantId, edge, relation);
            } else {
                unassign(tenantId, row, edge.getId());
            }
        }
    }

    private boolean applyRowToEdge(TenantId tenantId, EdgeProfile profile, EdgeAssignmentRowRef row, Edge edge) {
        EntityRelation relation = relationService.getRelation(tenantId, edge.getId(), row.id(), row.relationType(), RelationTypeGroup.EDGE);
        return applyRowToEdge(tenantId, profile, row, edge, relation, isEligible(tenantId, row, scopeOf(tenantId, row, edge), edge));
    }

    private boolean applyRowToEdge(TenantId tenantId, EdgeProfile profile, EdgeAssignmentRowRef row, Edge edge, EntityRelation relation, boolean eligible) {
        if (eligible) {
            if (relation != null && !stampedByAnotherProfile(relation, profile.getId())) {
                return false;
            }
            assign(tenantId, profile, row, edge.getId());
            return true;
        }
        if (relation == null || !isStampedBy(relation, profile.getId()) || isEdgeRoot(edge, row)) {
            return false;
        }
        unassign(tenantId, row, edge.getId());
        return true;
    }

    private boolean unassignRowFromEdge(TenantId tenantId, EdgeProfileId edgeProfileId, EdgeAssignmentRowRef row, Edge edge, EdgeProfile currentProfile) {
        EntityRelation relation = relationService.getRelation(tenantId, edge.getId(), row.id(), row.relationType(), RelationTypeGroup.EDGE);
        if (relation == null || !isStampedBy(relation, edgeProfileId)) {
            return false;
        }
        if (currentProfile != null && !edgeProfileId.equals(currentProfile.getId()) && rowsOf(currentProfile).contains(row)) {
            return false;
        }
        if (isEdgeRoot(edge, row)) {
            keepAsEdgeOwn(tenantId, edge, relation);
            return true;
        }
        unassign(tenantId, row, edge.getId());
        return true;
    }

    private void keepAsEdgeOwn(TenantId tenantId, Edge edge, EntityRelation relation) {
        relation.setAdditionalInfo(unstamped(relation));
        relationService.saveRelation(tenantId, relation);
        log.info("[{}][{}] Root rule chain {} left its profile and stays assigned as the edge's own", tenantId, edge.getId(), relation.getTo());
    }

    // an edge follows the profile root only while it still runs the root the profile had; any other root is the edge's own
    private boolean moveRoot(TenantId tenantId, Edge edge, RuleChainId root, RuleChainId previousRoot) throws Exception {
        if (root == null || root.equals(previousRoot) || !Objects.equals(edge.getRootRuleChainId(), previousRoot)) {
            return false;
        }
        setEdgeRoot(tenantId, edge, root);
        return true;
    }

    private void reapplyEdge(TenantId tenantId, EdgeProfile profile, Edge edge) throws Exception {
        List<EntityRelation> edgeRelations = relationService.findByFrom(tenantId, edge.getId(), RelationTypeGroup.EDGE);
        Map<UUID, EntityRelation> relationsByTarget = edgeRelations.stream()
                .collect(Collectors.toMap(relation -> relation.getTo().getId(), Function.identity(), (a, b) -> a));
        List<EdgeAssignmentRowRef> rows = rowsOf(profile);
        List<ListenableFuture<Void>> resends = new ArrayList<>();
        for (EdgeAssignmentRowRef row : rows) {
            EntityRelation relation = relationsByTarget.get(row.id().getId());
            if (relation != null && !row.relationType().equals(relation.getType())) {
                relation = null;
            }
            boolean eligible = isEligible(tenantId, row, scopeOf(tenantId, row, edge), edge);
            if (!applyRowToEdge(tenantId, profile, row, edge, relation, eligible)) {
                resends.add(resendRowToEdge(tenantId, profile, row, edge, relation, eligible));
            }
        }
        Futures.allAsList(resends).get();
        Set<UUID> currentRowIds = rows.stream().map(row -> row.id().getId()).collect(Collectors.toSet());
        dropStampedRowsNoLongerOnProfile(tenantId, edge, currentRowIds, edgeRelations);
        // only an edge without a root takes the profile root here; an overridden root is left alone
        moveRoot(tenantId, edge, profile.getRootRuleChainId(), null);
    }

    // Apply is the repair: a row the cloud already assigns may be missing on the edge, so it is sent again
    private ListenableFuture<Void> resendRowToEdge(TenantId tenantId, EdgeProfile profile, EdgeAssignmentRowRef row, Edge edge, EntityRelation relation, boolean eligible) {
        if (!eligible || relation == null || !isStampedBy(relation, profile.getId())) {
            return Futures.immediateVoidFuture();
        }
        EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(row.entityType());
        return edgeEventService.saveAsync(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), type, EdgeEventActionType.ASSIGNED_TO_EDGE, row.id(), stamp(profile.getId())));
    }

    // an interrupted fan-out or a switch racing a row edit leaves stamps no profile of this edge owns; only this sweep reaches them
    private void dropStampedRowsNoLongerOnProfile(TenantId tenantId, Edge edge, Set<UUID> currentRowIds, List<EntityRelation> edgeRelations) {
        for (EntityRelation relation : edgeRelations) {
            if (currentRowIds.contains(relation.getTo().getId()) || !isStamped(relation)) {
                continue;
            }
            EdgeAssignmentRowRef row = rowRefOf(relation);
            if (row == null) {
                continue;
            }
            if (isEdgeRoot(edge, row)) {
                keepAsEdgeOwn(tenantId, edge, relation);
                continue;
            }
            unassign(tenantId, row, edge.getId());
            log.info("[{}][{}] Dropped {} left stamped by a profile it is no longer on", tenantId, edge.getId(), relation.getTo());
        }
    }

    private static boolean isEdgeRoot(Edge edge, EdgeAssignmentRowRef row) {
        return EntityType.RULE_CHAIN.equals(row.entityType()) && row.id().equals(edge.getRootRuleChainId());
    }

    private static EdgeAssignmentRowRef rowRefOf(EntityRelation relation) {
        EntityId to = relation.getTo();
        if (relation.getType().startsWith(BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX)) {
            for (EntityType groupType : EdgeProfileData.SYNCABLE_GROUP_TYPES) {
                if (relation.getType().equals(BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name())) {
                    return EdgeAssignmentRowRef.entityGroup(new EntityGroupId(to.getId()), groupType);
                }
            }
            return null;
        }
        return switch (to.getEntityType()) {
            case RULE_CHAIN -> EdgeAssignmentRowRef.ruleChain(new RuleChainId(to.getId()));
            case INTEGRATION -> EdgeAssignmentRowRef.integration(new IntegrationId(to.getId()));
            case SCHEDULER_EVENT -> EdgeAssignmentRowRef.schedulerEvent(new SchedulerEventId(to.getId()));
            default -> null;
        };
    }

    private void assign(TenantId tenantId, EdgeProfile profile, EdgeAssignmentRowRef row, EdgeId edgeId) {
        ObjectNode stamp = stamp(profile.getId());
        switch (row.entityType()) {
            case RULE_CHAIN -> ruleChainService.assignRuleChainToEdge(tenantId, new RuleChainId(row.id().getId()), edgeId, stamp);
            case ENTITY_GROUP ->
                    entityGroupService.assignEntityGroupToEdge(tenantId, new EntityGroupId(row.id().getId()), edgeId, row.groupType(), stamp);
            case INTEGRATION -> integrationService.assignIntegrationToEdge(tenantId, new IntegrationId(row.id().getId()), edgeId, stamp);
            case SCHEDULER_EVENT -> schedulerEventService.assignSchedulerEventToEdge(tenantId, new SchedulerEventId(row.id().getId()), edgeId, stamp);
            default -> throw new IllegalArgumentException("Unsupported edge profile row type " + row.entityType());
        }
    }

    private void unassign(TenantId tenantId, EdgeAssignmentRowRef row, EdgeId edgeId) {
        switch (row.entityType()) {
            case RULE_CHAIN -> ruleChainService.unassignRuleChainFromEdge(tenantId, new RuleChainId(row.id().getId()), edgeId, false);
            case ENTITY_GROUP ->
                    entityGroupService.unassignEntityGroupFromEdge(tenantId, new EntityGroupId(row.id().getId()), edgeId, row.groupType());
            case INTEGRATION -> integrationService.unassignIntegrationFromEdge(tenantId, new IntegrationId(row.id().getId()), edgeId, false);
            case SCHEDULER_EVENT -> schedulerEventService.unassignSchedulerEventFromEdge(tenantId, new SchedulerEventId(row.id().getId()), edgeId);
            default -> throw new IllegalArgumentException("Unsupported edge profile row type " + row.entityType());
        }
    }

    private Edge setEdgeRoot(TenantId tenantId, Edge edge, RuleChainId root) throws Exception {
        Edge result = edge;
        if (!root.equals(edge.getRootRuleChainId())) {
            edge.setRootRuleChainId(root);
            result = edgeService.saveEdge(edge);
        }
        // persisted before returning, so every row dropped after this is queued behind the new root
        ruleChainEdgeProcessor.getObject().pushRootRuleChain(tenantId, result.getId(), root).get();
        return result;
    }

    // one lookup per row; the edge is only used to make the "missing entity" log line traceable
    private RowScope scopeOf(TenantId tenantId, EdgeAssignmentRowRef row, Edge edge) {
        switch (row.entityType()) {
            case ENTITY_GROUP -> {
                EntityGroup group = entityGroupService.findEntityGroupById(tenantId, new EntityGroupId(row.id().getId()));
                if (group == null) {
                    log.debug("[{}][{}] Edge profile references missing entity group {}, skipping", tenantId, edge != null ? edge.getId() : null, row.id());
                    return RowScope.MISSING;
                }
                return new RowScope(true, group.getOwnerId(), EntityType.USER.equals(group.getType()));
            }
            case SCHEDULER_EVENT -> {
                SchedulerEventInfo event = schedulerEventService.findSchedulerEventInfoById(tenantId, new SchedulerEventId(row.id().getId()));
                if (event == null) {
                    log.debug("[{}][{}] Edge profile references missing scheduler event {}, skipping", tenantId, edge != null ? edge.getId() : null, row.id());
                    return RowScope.MISSING;
                }
                return new RowScope(true, ownerOf(tenantId, event.getCustomerId()), true);
            }
            case RULE_CHAIN -> {
                RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, new RuleChainId(row.id().getId()));
                if (ruleChain == null) {
                    log.debug("[{}][{}] Edge profile references missing rule chain {}, skipping", tenantId, edge != null ? edge.getId() : null, row.id());
                    return RowScope.MISSING;
                }
                return new RowScope(true, tenantId, false);
            }
            case INTEGRATION -> {
                Integration integration = integrationService.findIntegrationById(tenantId, new IntegrationId(row.id().getId()));
                if (integration == null) {
                    log.debug("[{}][{}] Edge profile references missing integration {}, skipping", tenantId, edge != null ? edge.getId() : null, row.id());
                    return RowScope.MISSING;
                }
                return new RowScope(true, tenantId, false);
            }
            default -> throw new IllegalArgumentException("Unsupported edge profile row type " + row.entityType());
        }
    }

    private boolean isEligible(TenantId tenantId, EdgeAssignmentRowRef row, RowScope scope, Edge edge) {
        if (!scope.present()) {
            return false;
        }
        if (edgeProfileEligibility.isEligible(tenantId, scope.ownerId(), edge.getOwnerId(), scope.customerBound())) {
            return true;
        }
        if (scope.customerBound()) {
            log.debug("[{}][{}] Skipping {} owned by {}: the edge does not hold that customer", tenantId, edge.getId(), row.id(), scope.ownerId());
        }
        return false;
    }

}
