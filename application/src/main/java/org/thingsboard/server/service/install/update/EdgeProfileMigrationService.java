// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.cache.edge.EdgeCacheEvictEvent;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.edge.BaseEdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.relation.EntityRelationEvent;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.service.edge.profile.EdgeProfileApplier;
import org.thingsboard.server.service.install.InstallScripts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EdgeProfileMigrationService {

    private static final int PROFILE_PAGE_SIZE = 100;
    private static final int EDGE_CHUNK_SIZE = 256;
    private static final String DEFAULT_NAME = BaseEdgeProfileService.DEFAULT_EDGE_PROFILE_NAME;

    private final JdbcTemplate jdbcTemplate;
    private final EdgeProfileService edgeProfileService;
    private final RuleChainService ruleChainService;
    private final InstallScripts installScripts;
    private final ApplicationEventPublisher eventPublisher;

    // call through the proxy so @Transactional on processGroup applies
    @Autowired
    @Lazy
    private EdgeProfileMigrationService self;

    // edges still without a profile, keyed by type; the root is the one the type's profile gets
    record EdgeGroup(String type, UUID root) {

        boolean hasDefaultType() {
            return DEFAULT_NAME.equals(type);
        }

    }

    public void ensureEdgeProfiles(TenantId tenantId) {
        UUID tenantUuid = tenantId.getId();
        boolean hasProfiles = edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(1)).getTotalElements() > 0;
        // a crashed run can leave profiles with unassigned edges or stale auto-assign relations behind, so all three are checked
        if (hasProfiles && !hasUnassignedEdges(tenantUuid) && !hasStaleAutoAssignRelations(tenantUuid)) {
            return;
        }
        log.info("[{}] Starting edge profile migration ({})", tenantId, hasProfiles ? "resuming after a partial run" : "initial run");

        UUID templateRoot = findEdgeTemplateRoot(tenantUuid);
        if (templateRoot == null) {
            // a rootless default profile would leave the tenant unable to create edges, so provision the stock edge rule chains first
            log.info("[{}] Tenant has no root edge rule chain; provisioning the default edge rule chains", tenantId);
            installScripts.createDefaultEdgeRuleChains(tenantId);
            templateRoot = findEdgeTemplateRoot(tenantUuid);
            if (templateRoot == null) {
                log.warn("[{}] Still no root edge rule chain after provisioning; the default edge profile will have none" +
                        " and edges cannot be created until one is assigned to it", tenantId);
            }
        }
        // relation.to_id has no FK: drop auto-assign ids that no longer resolve to a rule chain
        List<UUID> autoAssign = resolveRuleChainIds(tenantId, jdbcTemplate.queryForList("SELECT to_id FROM relation WHERE from_id = ? AND from_type = 'TENANT' " +
                "AND relation_type_group = 'EDGE_AUTO_ASSIGN_RULE_CHAIN' AND to_type = 'RULE_CHAIN'", UUID.class, tenantUuid));
        List<EdgeGroup> groups = findGroups(tenantUuid, templateRoot);

        Map<String, EdgeProfile> byName = new HashMap<>();
        new PageDataIterable<>(link -> edgeProfileService.findEdgeProfilesByTenantId(tenantId, link), PROFILE_PAGE_SIZE)
                .forEach(profile -> byName.put(profile.getName(), profile));
        EdgeProfile defaultProfile = byName.values().stream().filter(EdgeProfile::isDefault).findFirst().orElse(null);

        int processed = 0;
        try {
            for (EdgeGroup group : groups) {
                EdgeProfile profile = self.resolveGroupProfile(tenantId, group, autoAssign, byName, defaultProfile);
                byName.put(profile.getName(), profile);
                // a chunk at a time, never a whole group: applyAfterCommit runs against a live node and must not hold row locks across a tenant
                while (self.processEdgeChunk(tenantId, group, profile) > 0) {
                    log.debug("[{}] Edge profile migration: chunk of group '{}' assigned to '{}'", tenantId, group.type(), profile.getName());
                }
                processed++;
            }
            jdbcTemplate.update("DELETE FROM relation WHERE from_id = ? AND from_type = 'TENANT' AND relation_type_group = 'EDGE_AUTO_ASSIGN_RULE_CHAIN'", tenantUuid);
        } catch (Exception e) {
            log.error("[{}] Edge profile migration failed after {}/{} edge groups; the run is resumable", tenantId, processed, groups.size(), e);
            throw e;
        }
        log.info("[{}] Edge profile migration completed: {} edge groups processed", tenantId, processed);
    }

    // a profile already named after the type (a resumed run) is reused whatever its root: the edges keep their own roots anyway
    @Transactional
    public EdgeProfile resolveGroupProfile(TenantId tenantId, EdgeGroup group, List<UUID> autoAssign,
                                           Map<String, EdgeProfile> byName, EdgeProfile defaultProfile) {
        EdgeProfile existing = group.hasDefaultType() ? defaultProfile : byName.get(group.type());
        return existing != null ? mergeAutoAssign(tenantId, existing, autoAssign)
                : createProfile(tenantId, group.type(), group.root(), autoAssign, group.hasDefaultType());
    }

    // one transaction per chunk of edges: an edge must never be left on a profile whose relation work did not finish
    @Transactional
    public int processEdgeChunk(TenantId tenantId, EdgeGroup group, EdgeProfile profile) {
        List<AssignedEdge> assigned = assignEdgesToProfile(tenantId.getId(), group, profile);
        if (assigned.isEmpty()) {
            return 0;
        }
        List<UUID> edgeIds = assigned.stream().map(AssignedEdge::id).toList();
        assignMissingRuleChains(tenantId, profile, edgeIds);
        stampRuleChainRelations(tenantId.getId(), profile, edgeIds);
        // the SQL update bypasses the by-name edge cache
        for (AssignedEdge edge : assigned) {
            eventPublisher.publishEvent(new EdgeCacheEvictEvent(tenantId, edge.name(), null));
        }
        return assigned.size();
    }

    private record AssignedEdge(UUID id, String name) {}

    // the default group is always present so the default profile exists even for a tenant without edges
    private List<EdgeGroup> findGroups(UUID tenantId, UUID templateRoot) {
        Map<String, Map<UUID, Long>> rootCounts = new TreeMap<>();
        jdbcTemplate.query("SELECT COALESCE(NULLIF(type, ''), ?), " +
                        "CASE WHEN EXISTS (SELECT 1 FROM rule_chain rc WHERE rc.id = e.root_rule_chain_id) THEN e.root_rule_chain_id END, count(*) " +
                        "FROM edge e WHERE tenant_id = ? AND edge_profile_id IS NULL GROUP BY 1, 2",
                rs -> {
                    Map<UUID, Long> counts = rootCounts.computeIfAbsent(rs.getString(1), t -> new HashMap<>());
                    UUID root = rs.getObject(2, UUID.class);
                    // a null or dangling root is no root of the edge's own: the edge takes the profile root and has no say in it
                    if (root != null) {
                        counts.merge(root, rs.getLong(3), Long::sum);
                    }
                }, DEFAULT_NAME, tenantId);
        rootCounts.remove(DEFAULT_NAME);
        List<EdgeGroup> groups = new ArrayList<>();
        // the default profile keeps the template root, so a new edge still starts on the root it got before the upgrade
        groups.add(new EdgeGroup(DEFAULT_NAME, templateRoot));
        rootCounts.forEach((type, counts) -> groups.add(new EdgeGroup(type, mostUsedRoot(counts, templateRoot))));
        return groups;
    }

    // a tie goes to the lowest id, so the pick does not depend on row order
    private static UUID mostUsedRoot(Map<UUID, Long> counts, UUID fallback) {
        return counts.entrySet().stream()
                .max(Map.Entry.<UUID, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey)
                .orElse(fallback);
    }

    // the auto-assign relations are deleted at the end of the run, so a profile reused by a resumed run must absorb them too
    private EdgeProfile mergeAutoAssign(TenantId tenantId, EdgeProfile profile, List<UUID> autoAssign) {
        List<RuleChainId> missing = new ArrayList<>();
        for (UUID id : autoAssign) {
            RuleChainId rcId = new RuleChainId(id);
            if (!profile.getProfileData().getRuleChainIds().contains(rcId) && !missing.contains(rcId)) {
                missing.add(rcId);
            }
        }
        if (missing.isEmpty()) {
            return profile;
        }
        EdgeProfile copy = new EdgeProfile(profile);
        copy.getProfileData().getRuleChainIds().addAll(missing);
        log.info("[{}] Folding {} auto-assign rule chain(s) into existing edge profile [{}]", tenantId, missing.size(), profile.getName());
        return edgeProfileService.saveEdgeProfile(copy, false);
    }

    private boolean hasUnassignedEdges(UUID tenantId) {
        Boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM edge WHERE tenant_id = ? AND edge_profile_id IS NULL)",
                Boolean.class, tenantId);
        return Boolean.TRUE.equals(exists);
    }

    private boolean hasStaleAutoAssignRelations(UUID tenantId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM relation WHERE from_id = ? AND from_type = 'TENANT' AND relation_type_group = 'EDGE_AUTO_ASSIGN_RULE_CHAIN')",
                Boolean.class, tenantId);
        return Boolean.TRUE.equals(exists);
    }

    // an edge keeps a root of its own, now an override of the profile root; a null or dangling one takes the profile root
    private List<AssignedEdge> assignEdgesToProfile(UUID tenantId, EdgeGroup group, EdgeProfile profile) {
        UUID profileRoot = profile.getRootRuleChainId() != null ? profile.getRootRuleChainId().getId() : null;
        return jdbcTemplate.query("UPDATE edge SET edge_profile_id = ?, type = ?, root_rule_chain_id = CASE WHEN EXISTS " +
                        "(SELECT 1 FROM rule_chain rc WHERE rc.id = edge.root_rule_chain_id) THEN edge.root_rule_chain_id ELSE CAST(? AS uuid) END " +
                        "WHERE id IN (SELECT e.id FROM edge e WHERE e.tenant_id = ? AND e.edge_profile_id IS NULL " +
                        "AND COALESCE(NULLIF(e.type, ''), ?) = ? LIMIT ?) RETURNING id, name",
                assignedEdgeMapper(), profile.getId().getId(), profile.getName(), profileRoot, tenantId, DEFAULT_NAME, group.type(), EDGE_CHUNK_SIZE);
    }

    private RowMapper<AssignedEdge> assignedEdgeMapper() {
        return (rs, rowNum) -> new AssignedEdge((UUID) rs.getObject("id"), rs.getString("name"));
    }

    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    private EdgeProfile createProfile(TenantId tenantId, String name, UUID root, List<UUID> autoAssign, boolean isDefault) {
        EdgeProfile profile = new EdgeProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setDefault(isDefault);
        List<RuleChainId> chains = new ArrayList<>();
        if (root != null) {
            chains.add(new RuleChainId(root));
            profile.setRootRuleChainId(new RuleChainId(root));
        }
        for (UUID id : autoAssign) {
            RuleChainId rcId = new RuleChainId(id);
            if (!chains.contains(rcId)) {
                chains.add(rcId);
            }
        }
        profile.getProfileData().setRuleChainIds(chains);
        return edgeProfileService.saveEdgeProfile(profile, false);
    }

    // legacy auto-assign reached only edges created after it was set; the profile now claims the chain for every edge
    private void assignMissingRuleChains(TenantId tenantId, EdgeProfile profile, List<UUID> chunk) {
        JsonNode stamp = EdgeProfileApplier.stamp(profile.getId());
        for (RuleChainId rcId : profile.getProfileData().getRuleChainIds()) {
            List<Object> args = new ArrayList<>(List.of(tenantId.getId(), profile.getId().getId(), rcId.getId()));
            args.addAll(chunk);
            List<UUID> edgeIds = jdbcTemplate.queryForList("SELECT e.id FROM edge e WHERE e.tenant_id = ? AND e.edge_profile_id = ? AND NOT EXISTS " +
                            "(SELECT 1 FROM relation r WHERE r.from_id = e.id AND r.from_type = 'EDGE' AND r.relation_type_group = 'EDGE' " +
                            "AND r.relation_type = 'Contains' AND r.to_type = 'RULE_CHAIN' AND r.to_id = ?) AND e.id IN (" + placeholders(chunk.size()) + ")",
                    UUID.class, args.toArray());
            for (UUID edgeId : edgeIds) {
                ruleChainService.assignRuleChainToEdge(tenantId, rcId, new EdgeId(edgeId), stamp);
            }
        }
    }

    private void stampRuleChainRelations(UUID tenantId, EdgeProfile profile, List<UUID> chunk) {
        String stamp = EdgeProfileApplier.stamp(profile.getId()).toString();
        for (RuleChainId rcId : profile.getProfileData().getRuleChainIds()) {
            List<Object> args = new ArrayList<>(List.of(stamp, rcId.getId(), tenantId, profile.getId().getId()));
            args.addAll(chunk);
            // RETURNING names the rows this statement actually stamped, so only those are evicted below
            List<UUID> stamped = jdbcTemplate.queryForList("UPDATE relation SET additional_info = ? WHERE from_type = 'EDGE' AND relation_type_group = 'EDGE' " +
                            "AND relation_type = 'Contains' AND to_type = 'RULE_CHAIN' AND to_id = ? AND additional_info IS NULL " +
                            "AND from_id IN (SELECT id FROM edge WHERE tenant_id = ? AND edge_profile_id = ? AND id IN (" + placeholders(chunk.size()) + ")) " +
                            "RETURNING from_id",
                    UUID.class, args.toArray());
            // the UPDATE never reaches BaseRelationService, so nothing would evict the relation cache on its own and
            // the applier would keep reading an unstamped copy for the whole cache TTL -- and skip the row as
            // not-managed-by-this-profile. Published inside processEdgeChunk's transaction, so the listener runs on commit.
            for (UUID edgeId : stamped) {
                eventPublisher.publishEvent(new EntityRelationEvent(new EdgeId(edgeId), rcId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
            }
        }
    }

    private List<UUID> resolveRuleChainIds(TenantId tenantId, List<UUID> ids) {
        List<UUID> resolved = new ArrayList<>();
        for (UUID id : ids) {
            if (ruleChainService.findRuleChainById(tenantId, new RuleChainId(id)) != null) {
                resolved.add(id);
            }
        }
        return resolved;
    }

    private UUID findEdgeTemplateRoot(UUID tenantId) {
        return jdbcTemplate.query("SELECT id FROM rule_chain WHERE tenant_id = ? AND type = 'EDGE' AND root = true ORDER BY created_time LIMIT 1",
                rs -> rs.next() ? rs.getObject(1, UUID.class) : null, tenantId);
    }

}
