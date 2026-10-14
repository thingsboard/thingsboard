// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.group.BaseEntityGroupService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * Pushes profile rows onto edges; hand-made, unstamped assignments are never touched. Not core-only:
 * an owner change from the rule engine re-applies profiles as well, see {@link #onOwnerChanged}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeProfileApplier {

    public static final String STAMP_KEY = "edgeProfileId";

    private static final int PAGE_SIZE = 1024;

    private final EdgeProfileService edgeProfileService;
    private final EdgeService edgeService;
    private final RelationService relationService;
    private final RuleChainService ruleChainService;
    private final EntityGroupService entityGroupService;
    private final IntegrationService integrationService;
    private final SchedulerEventService schedulerEventService;
    private final OwnersCacheService ownersCacheService;
    private final EdgeEventService edgeEventService;
    // core-only bean; the root is only ever pushed on the core, while this applier also lives on rule-engine nodes
    private final ObjectProvider<RuleChainEdgeProcessor> ruleChainEdgeProcessor;

    // one thread covers profile edits; owner changes can walk a whole customer subtree, hence tunable
    @Value("${edges.profile.applier.pool_size:1}")
    private int poolSize = 1;

    // beyond this, fan-outs are dropped rather than run on the REST caller's thread
    @Value("${edges.profile.applier.queue_capacity:10000}")
    private int queueCapacity = 10_000;

    private ExecutorService executor;

    // a profile switch and an Apply both rewrite an edge's rows; one at a time per edge, or the later one works from a stale profile
    private final Striped<Lock> edgeLocks = Striped.lazyWeakLock(64);

    @PostConstruct
    void init() {
        executor = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueCapacity),
                ThingsBoardThreadFactory.forName("edge-profile-applier"),
                (_, _) -> log.warn("Edge profile applier queue is full ({} tasks); dropping a fan-out." +
                        " Re-apply the affected edge profile to finish it, and consider raising edges.profile.applier.pool_size", queueCapacity));
    }

    @PreDestroy
    void shutdown() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                List<Runnable> dropped = executor.shutdownNow();
                log.warn("Edge profile applier stopped with {} queued fan-out task(s) dropped; re-apply the affected edge profiles to finish them", dropped.size());
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public record EdgeAssignmentRowRef(EntityId id, EntityType groupType) {

        public static EdgeAssignmentRowRef ruleChain(RuleChainId id) {
            return new EdgeAssignmentRowRef(id, null);
        }

        public static EdgeAssignmentRowRef entityGroup(EntityGroupId id, EntityType groupType) {
            return new EdgeAssignmentRowRef(id, groupType);
        }

        public static EdgeAssignmentRowRef integration(IntegrationId id) {
            return new EdgeAssignmentRowRef(id, null);
        }

        public static EdgeAssignmentRowRef schedulerEvent(SchedulerEventId id) {
            return new EdgeAssignmentRowRef(id, null);
        }

        public EntityType entityType() {
            return id.getEntityType();
        }

        public String relationType() {
            if (EntityType.ENTITY_GROUP.equals(id.getEntityType())) {
                return BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
            }
            return EntityRelation.CONTAINS_TYPE;
        }

    }

    /**
     * Owner of the entity a row points at; {@code present} is false when that entity no longer exists.
     * {@code customerBound} marks entities that cannot exist on an edge without their customer (scheduler events, users).
     */
    private record RowScope(boolean present, EntityId ownerId, boolean customerBound) {
        static final RowScope MISSING = new RowScope(false, null, false);

    }

    /** The per-row part of a fan-out, resolved once for all edges. */
    private record RowState(EdgeAssignmentRowRef row, RowScope scope, Set<EdgeId> edgesCarryingRow) {
    }

    public static ObjectNode stamp(EdgeProfileId edgeProfileId) {
        ObjectNode node = JacksonUtil.newObjectNode();
        node.put(STAMP_KEY, edgeProfileId.getId().toString());
        return node;
    }

    public static boolean isStampedBy(EntityRelation relation, EdgeProfileId edgeProfileId) {
        JsonNode info = relation.getAdditionalInfo();
        return info != null && info.hasNonNull(STAMP_KEY) && edgeProfileId.getId().toString().equals(info.get(STAMP_KEY).asText());
    }

    public static boolean isStamped(EntityRelation relation) {
        JsonNode info = relation.getAdditionalInfo();
        return info != null && info.hasNonNull(STAMP_KEY);
    }

    // an edge moving between profiles that share a row keeps the old stamp; the new profile has to claim the row to manage it
    private static boolean stampedByAnotherProfile(EntityRelation relation, EdgeProfileId edgeProfileId) {
        return isStamped(relation) && !isStampedBy(relation, edgeProfileId);
    }

    public Edge applyToEdge(TenantId tenantId, Edge edge, boolean created) throws Exception {
        return applyToEdge(tenantId, edge, created, false, null);
    }

    public Edge applyToEdge(TenantId tenantId, Edge edge, boolean created, boolean forceRoot, EdgeProfileId previousProfileId) throws Exception {
        Lock lock = edgeLocks.get(edge.getId());
        lock.lock();
        try {
            return doApplyToEdge(tenantId, edge, created, forceRoot, previousProfileId);
        } finally {
            lock.unlock();
        }
    }

    private Edge doApplyToEdge(TenantId tenantId, Edge edge, boolean created, boolean forceRoot, EdgeProfileId previousProfileId) throws Exception {
        if (edge.getEdgeProfileId() == null) {
            // a partially migrated tenant can still have an edge with no profile; id validation would throw on a null id
            log.warn("[{}][{}] Edge has no profile, nothing to apply", tenantId, edge.getId());
            return edge;
        }
        EdgeProfile profile = edgeProfileService.findEdgeProfileById(tenantId, edge.getEdgeProfileId());
        if (profile == null) {
            log.warn("[{}][{}] Edge references missing profile {}", tenantId, edge.getId(), edge.getEdgeProfileId());
            return edge;
        }
        List<EdgeAssignmentRowRef> rows = rowsOf(profile);
        for (EdgeAssignmentRowRef row : rows) {
            RowScope scope = scopeOf(tenantId, row, edge);
            EntityRelation relation = relationService.getRelation(tenantId, edge.getId(), row.id(), row.relationType(), RelationTypeGroup.EDGE);
            if (isEligible(tenantId, row, scope, edge)) {
                if (relation == null || stampedByAnotherProfile(relation, profile.getId())) {
                    assign(tenantId, profile, row, edge.getId());
                }
            } else if (relation != null && isStampedBy(relation, profile.getId())) {
                unassign(tenantId, row, edge.getId());
            }
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
            try {
                unassign(tenantId, row, edge.getId());
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to drop {} left over from previous edge profile {}", tenantId, edge.getId(), row.id(), previousProfileId, e);
            }
        }
    }

    public void applyRowToProfileEdgesAsync(TenantId tenantId, EdgeProfile profile, EdgeAssignmentRowRef row) {
        executor.submit(() -> guarded(() -> applyRowToProfileEdges(tenantId, profile, row), "applyRow", tenantId, profile));
    }

    public void unassignRowFromProfileEdgesAsync(TenantId tenantId, EdgeProfile profile, EdgeAssignmentRowRef row) {
        executor.submit(() -> guarded(() -> unassignRowFromProfileEdges(tenantId, profile, row), "unassignRow", tenantId, profile));
    }

    public void setRootAsync(TenantId tenantId, EdgeProfile profile, RuleChainId previousRoot) {
        executor.submit(() -> guarded(() -> setRoot(tenantId, profile, previousRoot), "setRoot", tenantId, profile));
    }

    // one task for the whole subtree, so the queue grows with owner changes rather than with the edges they cover
    // owner changes are the only writes that change which rows an edge may see without going through a profile
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onOwnerChanged(ActionEntityEvent<?> event) {
        if (event.getActionType() != ActionType.CHANGE_OWNER || event.getEntityId() == null) {
            return;
        }
        TenantId tenantId = event.getTenantId();
        switch (event.getEntityId().getEntityType()) {
            case EDGE -> applyToMovedEdge(tenantId, new EdgeId(event.getEntityId().getId()));
            case CUSTOMER -> applyToEdgesOfOwnersAsync(tenantId,
                    ownersWhoseViewChanged(tenantId, event.getEntityId(), JacksonUtil.fromString(event.getBody(), EntityId.class)));
            default -> {
            }
        }
    }

    private void applyToMovedEdge(TenantId tenantId, EdgeId edgeId) {
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            return;
        }
        try {
            applyToEdge(tenantId, edge, false);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to re-apply edge profile after an owner change", tenantId, edgeId, e);
        }
    }

    // edges under the moved customer keep their view; the owners it left lose its rows and the ones it joined gain them
    private Set<CustomerId> ownersWhoseViewChanged(TenantId tenantId, EntityId movedId, EntityId formerOwnerId) {
        Set<EntityId> former = ownerChain(tenantId, formerOwnerId);
        Set<EntityId> current = ownerChain(tenantId, ownersCacheService.getOwner(tenantId, movedId));
        Set<CustomerId> changed = new HashSet<>();
        for (EntityId ownerId : Sets.symmetricDifference(former, current)) {
            if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                changed.add(new CustomerId(ownerId.getId()));
            }
        }
        return changed;
    }

    private Set<EntityId> ownerChain(TenantId tenantId, EntityId ownerId) {
        return ownerId == null ? Set.of() : ownersCacheService.fetchOwnersHierarchy(tenantId, ownerId);
    }

    private void applyToEdgesOfOwnersAsync(TenantId tenantId, Collection<CustomerId> ownerIds) {
        if (ownerIds.isEmpty()) {
            return;
        }
        executor.submit(() -> {
            int touched = 0;
            for (CustomerId ownerId : ownerIds) {
                for (Edge edge : new PageDataIterable<>(
                        link -> edgeService.findEdgesByTenantIdAndCustomerId(tenantId, ownerId, link), PAGE_SIZE)) {
                    try {
                        applyToEdge(tenantId, edge, false);
                        touched++;
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to re-apply edge profile after an ownership change", tenantId, edge.getId(), e);
                    }
                }
            }
            log.debug("[{}] Re-applied edge profiles for {} edge(s) under {} moved owner(s)", tenantId, touched, ownerIds.size());
        });
    }

    public void reapplyAsync(TenantId tenantId, EdgeProfile profile) {
        executor.submit(() -> guarded(() -> {
            reapply(tenantId, profile);
            return 0;
        }, "reapply", tenantId, profile));
    }

    public int applyRowToProfileEdges(TenantId tenantId, EdgeProfile profile, EdgeAssignmentRowRef row) {
        RowState state = stateOf(tenantId, row);
        int touched = 0;
        for (Edge edge : profileEdges(tenantId, profile)) {
            touched += applyRowToEdge(tenantId, profile, state, edge);
        }
        return touched;
    }

    private int applyRowToEdge(TenantId tenantId, EdgeProfile profile, RowState state, Edge edge) {
        EdgeAssignmentRowRef row = state.row();
        boolean has = state.edgesCarryingRow().contains(edge.getId());
        if (isEligible(tenantId, row, state.scope(), edge)) {
            // checked whatever the snapshot said: a relation made by hand after edgesCarryingRow was taken would
            // otherwise be assigned over and stamped, handing the profile a row the user owns -- which the profile
            // then deletes as its own. One cached read is nothing next to the write it guards.
            EntityRelation relation = relationService.getRelation(tenantId, edge.getId(), row.id(), row.relationType(), RelationTypeGroup.EDGE);
            if (relation != null && !stampedByAnotherProfile(relation, profile.getId())) {
                return 0;
            }
            try {
                assign(tenantId, profile, row, edge.getId());
                return 1;
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to assign {} to edge {}", tenantId, profile.getId(), row.id(), edge.getId(), e);
            }
        } else if (has) {
            EntityRelation relation = relationService.getRelation(tenantId, edge.getId(), row.id(), row.relationType(), RelationTypeGroup.EDGE);
            if (relation == null || !isStampedBy(relation, profile.getId())) {
                return 0;
            }
            try {
                unassign(tenantId, row, edge.getId());
                return 1;
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to unassign no longer eligible {} from edge {}", tenantId, profile.getId(), row.id(), edge.getId(), e);
            }
        }
        return 0;
    }

    public int unassignRowFromProfileEdges(TenantId tenantId, EdgeProfile profile, EdgeAssignmentRowRef row) {
        // walks the edges carrying the row, not the profile's edges: one switched away meanwhile still holds this profile's stamp
        int removed = 0;
        int failed = 0;
        for (EdgeId edgeId : edgesCarryingRow(tenantId, row)) {
            EntityRelation relation = relationService.getRelation(tenantId, edgeId, row.id(), row.relationType(), RelationTypeGroup.EDGE);
            if (relation == null || !isStampedBy(relation, profile.getId()) || isRowOfAnotherCurrentProfile(tenantId, edgeId, profile, row)) {
                continue;
            }
            try {
                unassign(tenantId, row, edgeId);
                removed++;
            } catch (Exception e) {
                failed++;
                log.warn("[{}][{}] Failed to unassign {} from edge {}", tenantId, profile.getId(), row.id(), edgeId, e);
            }
        }
        if (failed > 0) {
            log.warn("[{}][{}] {} edge(s) failed to unassign {}; re-apply the profile to finish the job", tenantId, profile.getId(), failed, row.id());
        }
        return removed;
    }

    // the edge's new profile carries the row too and claims it on the switch
    private boolean isRowOfAnotherCurrentProfile(TenantId tenantId, EdgeId edgeId, EdgeProfile profile, EdgeAssignmentRowRef row) {
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null || edge.getEdgeProfileId() == null || profile.getId().equals(edge.getEdgeProfileId())) {
            return false;
        }
        EdgeProfile current = edgeProfileService.findEdgeProfileById(tenantId, edge.getEdgeProfileId());
        return current != null && rowsOf(current).contains(row);
    }

    // an edge follows the profile root only while it still runs the root the profile had; any other root is the edge's own
    public int setRoot(TenantId tenantId, EdgeProfile profile, RuleChainId previousRoot) {
        RuleChainId root = profile.getRootRuleChainId();
        if (root == null || root.equals(previousRoot)) {
            return 0;
        }
        int updated = 0;
        for (Edge edge : profileEdges(tenantId, profile)) {
            if (!Objects.equals(edge.getRootRuleChainId(), previousRoot)) {
                continue;
            }
            try {
                setEdgeRoot(tenantId, edge, root);
                updated++;
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to set root rule chain {} for edge {}", tenantId, profile.getId(), root, edge.getId(), e);
            }
        }
        return updated;
    }

    public void reapply(TenantId tenantId, EdgeProfile profile) {
        List<EdgeAssignmentRowRef> rows = rowsOf(profile);
        // rows resolved once up front; resolving per edge would cost rows x edges lookups
        List<RowState> states = new ArrayList<>(rows.size());
        for (EdgeAssignmentRowRef row : rows) {
            states.add(stateOf(tenantId, row));
        }
        Set<UUID> currentRowIds = rows.stream().map(row -> row.id().getId()).collect(Collectors.toSet());
        for (Edge listed : profileEdges(tenantId, profile)) {
            Lock lock = edgeLocks.get(listed.getId());
            lock.lock();
            try {
                // the edge may have switched profile since its page was read; that switch applies the new profile itself
                Edge edge = edgeService.findEdgeById(tenantId, listed.getId());
                if (edge == null || !profile.getId().equals(edge.getEdgeProfileId())) {
                    continue;
                }
                for (RowState state : states) {
                    if (applyRowToEdge(tenantId, profile, state, edge) == 0) {
                        resendRowToEdge(tenantId, profile, state, edge);
                    }
                }
                dropStampedRowsNoLongerOnProfile(tenantId, profile, edge, currentRowIds);
            } finally {
                lock.unlock();
            }
        }
        // only an edge without a root takes the profile root here; an overridden root is left alone
        setRoot(tenantId, profile, null);
    }

    // Apply is the repair: a row the cloud already assigns may be missing on the edge, so it is sent again
    private void resendRowToEdge(TenantId tenantId, EdgeProfile profile, RowState state, Edge edge) {
        EdgeAssignmentRowRef row = state.row();
        if (!isEligible(tenantId, row, state.scope(), edge)) {
            return;
        }
        EntityRelation relation = relationService.getRelation(tenantId, edge.getId(), row.id(), row.relationType(), RelationTypeGroup.EDGE);
        if (relation == null || !isStampedBy(relation, profile.getId())) {
            return;
        }
        EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(row.entityType());
        try {
            edgeEventService.saveAsync(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), type, EdgeEventActionType.ASSIGNED_TO_EDGE, row.id(), stamp(profile.getId()))).get();
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to re-send {} to edge {}", tenantId, profile.getId(), row.id(), edge.getId(), e);
        }
    }

    // an interrupted fan-out or a switch racing a row edit leaves stamps no profile of this edge owns; only this sweep reaches them
    private void dropStampedRowsNoLongerOnProfile(TenantId tenantId, EdgeProfile profile, Edge edge, Set<UUID> currentRowIds) {
        for (EntityRelation relation : relationService.findByFrom(tenantId, edge.getId(), RelationTypeGroup.EDGE)) {
            if (currentRowIds.contains(relation.getTo().getId()) || !isStamped(relation)) {
                continue;
            }
            EdgeAssignmentRowRef row = rowRefOf(relation);
            if (row == null) {
                continue;
            }
            try {
                unassign(tenantId, row, edge.getId());
                log.info("[{}][{}] Dropped {} left stamped by this profile but no longer on it", tenantId, edge.getId(), relation.getTo());
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to drop stranded row {} from edge", tenantId, profile.getId(), relation.getTo(), e);
            }
        }
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

    public static List<EdgeAssignmentRowRef> rowsOf(EdgeProfile profile) {
        List<EdgeAssignmentRowRef> rows = new ArrayList<>();
        for (RuleChainId id : profile.getProfileData().getRuleChainIds()) {
            rows.add(EdgeAssignmentRowRef.ruleChain(id));
        }
        for (EdgeProfileEntityGroup g : profile.getProfileData().getEntityGroups()) {
            rows.add(EdgeAssignmentRowRef.entityGroup(g.getEntityGroupId(), g.getGroupType()));
        }
        for (IntegrationId id : profile.getProfileData().getIntegrationIds()) {
            rows.add(EdgeAssignmentRowRef.integration(id));
        }
        for (SchedulerEventId id : profile.getProfileData().getSchedulerEventIds()) {
            rows.add(EdgeAssignmentRowRef.schedulerEvent(id));
        }
        return rows;
    }

    private Iterable<Edge> profileEdges(TenantId tenantId, EdgeProfile profile) {
        return new PageDataIterable<>(link -> edgeService.findEdgesByTenantIdAndEdgeProfileId(tenantId, profile.getId(), link), PAGE_SIZE);
    }

    private RowState stateOf(TenantId tenantId, EdgeAssignmentRowRef row) {
        return new RowState(row, scopeOf(tenantId, row, null), edgesCarryingRow(tenantId, row));
    }

    /** The edges that already have this row assigned, whatever put it there. */
    private Set<EdgeId> edgesCarryingRow(TenantId tenantId, EdgeAssignmentRowRef row) {
        Set<EdgeId> edgeIds = new HashSet<>();
        if (EntityType.ENTITY_GROUP.equals(row.entityType())) {
            new PageDataIterable<>(link -> edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(tenantId, List.of(new EntityGroupId(row.id().getId())), row.groupType(), link), PAGE_SIZE)
                    .forEach(edgeIds::add);
        } else {
            new PageDataIterable<>(link -> edgeService.findEdgeIdsByTenantIdAndEntityIds(tenantId, List.of(row.id()), row.entityType(), link), PAGE_SIZE)
                    .forEach(edgeIds::add);
        }
        return edgeIds;
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
        if (isEligible(tenantId, scope.ownerId(), edge.getOwnerId(), scope.customerBound())) {
            return true;
        }
        if (scope.customerBound()) {
            log.debug("[{}][{}] Skipping {} owned by {}: the edge does not hold that customer", tenantId, edge.getId(), row.id(), scope.ownerId());
        }
        return false;
    }

    boolean isEligible(TenantId tenantId, EdgeAssignmentRowRef row, Edge edge) {
        return isEligible(tenantId, row, scopeOf(tenantId, row, edge), edge);
    }

    public boolean isEligible(TenantId tenantId, EntityId entityOwnerId, EntityId edgeOwnerId, boolean customerBound) {
        if (entityOwnerId == null || EntityType.TENANT.equals(entityOwnerId.getEntityType())) {
            return true;
        }
        // an edge holds only its owner's customer and the ones above it, so a customer-bound entity of any other customer is rejected there
        if (customerBound) {
            return entityOwnerId.equals(edgeOwnerId);
        }
        if (edgeOwnerId == null || EntityType.TENANT.equals(edgeOwnerId.getEntityType())) {
            return true;
        }
        if (entityOwnerId.equals(edgeOwnerId)) {
            return true;
        }
        return ownersCacheService.isChildOwner(tenantId, new CustomerId(edgeOwnerId.getId()), new CustomerId(entityOwnerId.getId()));
    }

    public static EntityId ownerOf(TenantId tenantId, CustomerId customerId) {
        if (customerId == null || customerId.isNullUid()) {
            return tenantId;
        }
        return customerId;
    }

    private void guarded(IntSupplier task, String op, TenantId tenantId, EdgeProfile profile) {
        try {
            int n = task.getAsInt();
            log.debug("[{}][{}] edge profile {}: {} edges touched", tenantId, profile.getId(), op, n);
        } catch (Exception e) {
            log.error("[{}][{}] edge profile {} failed", tenantId, profile.getId(), op, e);
        }
    }

}
