// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeAssignmentRow;
import org.thingsboard.server.common.data.edge.EdgeAssignmentSource;
import org.thingsboard.server.common.data.edge.EdgeAssignments;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EdgeAssignmentsService {

    private static final int PAGE_SIZE = 1024;

    private final EdgeProfileService edgeProfileService;
    private final EdgeService edgeService;
    private final RuleChainService ruleChainService;
    private final EntityGroupService entityGroupService;
    private final IntegrationService integrationService;
    private final SchedulerEventService schedulerEventService;
    private final CustomerService customerService;
    private final OwnersCacheService ownersCacheService;
    private final RelationService relationService;

    public EdgeAssignments forProfile(TenantId tenantId, EdgeProfile profile) {
        EdgeAssignments result = header(profile);
        result.setEdgesCount(edgeService.countByTenantIdAndEdgeProfileId(tenantId, profile.getId()));
        Map<EntityId, String> owners = new HashMap<>();
        for (RuleChainId id : profile.getProfileData().getRuleChainIds()) {
            RuleChain rc = ruleChainService.findRuleChainById(tenantId, id);
            if (rc != null) {
                result.getRuleChains().add(ruleChainRow(rc, id.equals(profile.getRootRuleChainId()), null));
            }
        }
        for (EdgeProfileEntityGroup g : profile.getProfileData().getEntityGroups()) {
            EntityGroup group = entityGroupService.findEntityGroupById(tenantId, g.getEntityGroupId());
            if (group != null) {
                result.getEntityGroups().add(groupRow(tenantId, group, null, owners));
            }
        }
        for (IntegrationId id : profile.getProfileData().getIntegrationIds()) {
            Integration integration = integrationService.findIntegrationById(tenantId, id);
            if (integration != null) {
                result.getIntegrations().add(simpleRow(id, integration.getName(), tenantId, "Tenant", null));
            }
        }
        for (SchedulerEventId id : profile.getProfileData().getSchedulerEventIds()) {
            SchedulerEventInfo event = schedulerEventService.findSchedulerEventInfoById(tenantId, id);
            if (event != null) {
                EntityId owner = EdgeProfileApplier.ownerOf(tenantId, event.getCustomerId());
                result.getSchedulerEvents().add(simpleRow(id, event.getName(), owner, ownerName(tenantId, owner, owners), null));
            }
        }
        return result;
    }

    public EdgeAssignments forEdge(TenantId tenantId, Edge edge, SecurityUser user) {
        // edge_profile_id is nullable and findEdgeProfileById rejects a null id
        EdgeProfile profile = edge.getEdgeProfileId() != null ? edgeProfileService.findEdgeProfileById(tenantId, edge.getEdgeProfileId()) : null;
        EdgeAssignments result = profile != null ? header(profile) : new EdgeAssignments();
        result.setRootRuleChainId(edge.getRootRuleChainId());
        // the stamp on the relation, not the profile's content, says who put the row on this edge: an entity can
        // sit in the profile while this edge's own row predates it, and that row must stay removable here
        Map<EntityId, EntityRelation> edgeRelations = relationsByTo(tenantId, edge);
        Map<EntityId, String> owners = new HashMap<>();
        MergedUserPermissions permissions = user.getUserPermissions();
        boolean tenantAdmin = Authority.TENANT_ADMIN.equals(user.getAuthority());
        if (tenantAdmin && permissions.hasGenericPermission(Resource.RULE_CHAIN, Operation.READ)) {
            for (RuleChain rc : new PageDataIterable<>(link -> ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edge.getId(), link), PAGE_SIZE)) {
                result.getRuleChains().add(ruleChainRow(rc, rc.getId().equals(edge.getRootRuleChainId()), source(edgeRelations, profile, rc.getId())));
            }
        }
        for (EntityType groupType : EdgeProfileData.SYNCABLE_GROUP_TYPES) {
            MergedGroupTypePermissionInfo groupPermissions = permissions.getReadGroupPermissions().get(groupType);
            if (groupPermissions == null || !groupPermissions.isHasGenericRead()) {
                continue;
            }
            for (EntityGroupInfo group : new PageDataIterable<>(link -> entityGroupService.findEdgeEntityGroupInfosByOwnerIdType(tenantId, edge.getId(), user.getOwnerId(), groupType, link), PAGE_SIZE)) {
                if (group.isEdgeGroupAll()) {
                    continue;
                }
                result.getEntityGroups().add(groupRow(tenantId, group, source(edgeRelations, profile, group.getId()), owners));
            }
        }
        if (tenantAdmin && permissions.hasGenericPermission(Resource.INTEGRATION, Operation.READ)) {
            for (Integration integration : new PageDataIterable<>(link -> integrationService.findIntegrationsByTenantIdAndEdgeId(tenantId, edge.getId(), link), PAGE_SIZE)) {
                result.getIntegrations().add(simpleRow(integration.getId(), integration.getName(), tenantId, "Tenant", source(edgeRelations, profile, integration.getId())));
            }
        }
        if (permissions.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.READ)) {
            Set<EntityId> visibleOwners = tenantAdmin ? null : visibleOwners(tenantId, user);
            for (SchedulerEventInfo event : new PageDataIterable<>(link -> schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edge.getId(), link), PAGE_SIZE)) {
                EntityId owner = EdgeProfileApplier.ownerOf(tenantId, event.getCustomerId());
                if (visibleOwners != null && !visibleOwners.contains(owner)) {
                    continue;
                }
                result.getSchedulerEvents().add(simpleRow(event.getId(), event.getName(), owner, ownerName(tenantId, owner, owners), source(edgeRelations, profile, event.getId())));
            }
        }
        return result;
    }

    private Set<EntityId> visibleOwners(TenantId tenantId, SecurityUser user) {
        Set<EntityId> owners = new HashSet<>(ownersCacheService.getChildOwners(tenantId, user.getOwnerId()));
        owners.add(user.getOwnerId());
        return owners;
    }

    private static EdgeAssignments header(EdgeProfile profile) {
        EdgeAssignments a = new EdgeAssignments();
        a.setEdgeProfileId(profile.getId());
        a.setEdgeProfileName(profile.getName());
        a.setRootRuleChainId(profile.getRootRuleChainId());
        a.setProfileRootRuleChainId(profile.getRootRuleChainId());
        return a;
    }

    // every assignment this screen lists is a relation from the edge in the EDGE group, so one lookup covers
    // rule chains, entity groups, integrations and scheduler events alike
    private Map<EntityId, EntityRelation> relationsByTo(TenantId tenantId, Edge edge) {
        List<EntityRelation> relations = relationService.findByFrom(tenantId, edge.getId(), RelationTypeGroup.EDGE);
        return relations.stream().collect(Collectors.toMap(EntityRelation::getTo, Function.identity(), (a, b) -> a));
    }

    private static EdgeAssignmentSource source(Map<EntityId, EntityRelation> edgeRelations, EdgeProfile profile, EntityId id) {
        if (profile == null) {
            return EdgeAssignmentSource.EDGE;
        }
        EntityRelation relation = edgeRelations.get(id);
        return relation != null && EdgeProfileApplier.isStampedBy(relation, profile.getId())
                ? EdgeAssignmentSource.PROFILE : EdgeAssignmentSource.EDGE;
    }

    private static EdgeAssignmentRow ruleChainRow(RuleChain rc, boolean root, EdgeAssignmentSource source) {
        EdgeAssignmentRow row = new EdgeAssignmentRow();
        row.setId(rc.getId());
        row.setName(rc.getName());
        row.setRoot(root);
        row.setSource(source);
        return row;
    }

    private EdgeAssignmentRow groupRow(TenantId tenantId, EntityGroup group, EdgeAssignmentSource source, Map<EntityId, String> owners) {
        EdgeAssignmentRow row = new EdgeAssignmentRow();
        row.setId(group.getId());
        row.setName(group.getName());
        row.setGroupType(group.getType());
        row.setOwnerId(group.getOwnerId());
        row.setOwnerName(ownerName(tenantId, group.getOwnerId(), owners));
        row.setSource(source);
        return row;
    }

    private static EdgeAssignmentRow simpleRow(EntityId id, String name, EntityId ownerId, String ownerName, EdgeAssignmentSource source) {
        EdgeAssignmentRow row = new EdgeAssignmentRow();
        row.setId(id);
        row.setName(name);
        row.setOwnerId(ownerId);
        row.setOwnerName(ownerName);
        row.setSource(source);
        return row;
    }

    private String ownerName(TenantId tenantId, EntityId ownerId, Map<EntityId, String> cache) {
        if (ownerId == null || EntityType.TENANT.equals(ownerId.getEntityType())) {
            return "Tenant";
        }
        return cache.computeIfAbsent(ownerId, id -> {
            Customer customer = customerService.findCustomerById(tenantId, new CustomerId(id.getId()));
            return customer != null ? customer.getTitle() : id.toString();
        });
    }

}
