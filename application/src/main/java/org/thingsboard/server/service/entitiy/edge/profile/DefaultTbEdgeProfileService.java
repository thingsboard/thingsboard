// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.edge.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.EdgeProfileApplyOp;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.profile.EdgeProfileJobService;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows.EdgeAssignmentRowRef;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;
import java.util.function.Function;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultTbEdgeProfileService extends AbstractTbEntityService implements TbEdgeProfileService {

    private final EdgeProfileService edgeProfileService;
    private final EdgeProfileJobService edgeProfileJobService;

    @Override
    public EdgeProfile save(EdgeProfile edgeProfile, User user) throws Exception {
        ActionType actionType = edgeProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = edgeProfile.getTenantId();
        try {
            if (edgeProfile.getId() != null) {
                EdgeProfile stored = edgeProfileService.findEdgeProfileById(tenantId, edgeProfile.getId());
                if (stored != null) {
                    edgeProfile.setDefault(stored.isDefault());
                    edgeProfile.setRootRuleChainId(stored.getRootRuleChainId());
                    edgeProfile.setProfileData(new EdgeProfileData(stored.getProfileData()));
                } else {
                    edgeProfile.setDefault(false);
                }
            }
            EdgeProfile saved = checkNotNull(edgeProfileService.saveEdgeProfile(edgeProfile));
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, actionType, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE_PROFILE), edgeProfile, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(EdgeProfile edgeProfile, User user) {
        TenantId tenantId = edgeProfile.getTenantId();
        try {
            if (edgeProfileJobService.hasActiveJobs(tenantId, edgeProfile.getId())) {
                throw new DataValidationException("Edge profile [" + edgeProfile.getName() + "] has apply jobs in progress. " +
                        "Wait for them to finish or cancel them in the Task manager!");
            }
            edgeProfileService.deleteEdgeProfile(tenantId, edgeProfile.getId());
            logEntityActionService.logEntityAction(tenantId, edgeProfile.getId(), edgeProfile, ActionType.DELETED, user, edgeProfile.getId().toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE_PROFILE), edgeProfile, ActionType.DELETED, user, e, edgeProfile.getId().toString());
            throw e;
        }
    }

    @Override
    public EdgeProfile setDefaultEdgeProfile(EdgeProfile edgeProfile, EdgeProfile previousDefaultEdgeProfile, User user) {
        TenantId tenantId = edgeProfile.getTenantId();
        EdgeProfileId edgeProfileId = edgeProfile.getId();
        try {
            if (edgeProfileService.setDefaultEdgeProfile(tenantId, edgeProfileId)) {
                if (previousDefaultEdgeProfile != null) {
                    previousDefaultEdgeProfile = edgeProfileService.findEdgeProfileById(tenantId, previousDefaultEdgeProfile.getId());
                    logEntityActionService.logEntityAction(tenantId, previousDefaultEdgeProfile.getId(), previousDefaultEdgeProfile, ActionType.UPDATED, user);
                }
                edgeProfile = edgeProfileService.findEdgeProfileById(tenantId, edgeProfileId);
                logEntityActionService.logEntityAction(tenantId, edgeProfileId, edgeProfile, ActionType.UPDATED, user);
            }
            return edgeProfile;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE_PROFILE), ActionType.UPDATED, user, e, edgeProfileId.toString());
            throw e;
        }
    }

    @Override
    public EdgeProfile addRows(EdgeProfile edgeProfile, EdgeProfileData rows, User user) {
        EdgeProfile added = new EdgeProfile();
        added.setProfileData(rows);
        List<EdgeAssignmentRowRef> rowRefs = EdgeProfileRows.rowsOf(added);
        if (rowRefs.isEmpty()) {
            return edgeProfile;
        }
        TenantId tenantId = edgeProfile.getTenantId();
        try {
            EdgeProfile saved = edgeProfileService.addRows(tenantId, edgeProfile.getId(), rows);
            edgeProfileJobService.submitRows(tenantId, saved, EdgeProfileApplyOp.ADD_ROWS, rowRefs, null, null);
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, ActionType.UPDATED, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE_PROFILE), edgeProfile, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public EdgeProfile addRuleChain(EdgeProfile edgeProfile, RuleChainId ruleChainId, User user) {
        return added(edgeProfile, user, p -> edgeProfileService.addRuleChain(p.getTenantId(), p.getId(), ruleChainId),
                EdgeAssignmentRowRef.ruleChain(ruleChainId));
    }

    @Override
    public EdgeProfile removeRuleChain(EdgeProfile edgeProfile, RuleChainId ruleChainId, User user) {
        return removed(edgeProfile, user, p -> edgeProfileService.removeRuleChain(p.getTenantId(), p.getId(), ruleChainId),
                EdgeAssignmentRowRef.ruleChain(ruleChainId));
    }

    @Override
    public EdgeProfile setRootRuleChain(EdgeProfile edgeProfile, RuleChainId ruleChainId, User user) {
        TenantId tenantId = edgeProfile.getTenantId();
        try {
            EdgeProfile saved = edgeProfileService.setRootRuleChain(tenantId, edgeProfile.getId(), ruleChainId);
            edgeProfileJobService.submitRows(tenantId, saved, EdgeProfileApplyOp.SET_ROOT, List.of(EdgeAssignmentRowRef.ruleChain(ruleChainId)),
                    ruleChainId, edgeProfile.getRootRuleChainId());
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, ActionType.UPDATED, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE_PROFILE), edgeProfile, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public EdgeProfile addEntityGroup(EdgeProfile edgeProfile, EntityGroupId entityGroupId, EntityType groupType, User user) {
        return added(edgeProfile, user, p -> edgeProfileService.addEntityGroup(p.getTenantId(), p.getId(), entityGroupId, groupType),
                EdgeAssignmentRowRef.entityGroup(entityGroupId, groupType));
    }

    @Override
    public EdgeProfile removeEntityGroup(EdgeProfile edgeProfile, EntityGroupId entityGroupId, User user) {
        EntityType groupType = edgeProfile.getProfileData().getEntityGroups().stream()
                .filter(g -> entityGroupId.equals(g.getEntityGroupId())).map(EdgeProfileEntityGroup::getGroupType).findFirst().orElse(null);
        if (groupType == null) {
            return edgeProfile;
        }
        return removed(edgeProfile, user, p -> edgeProfileService.removeEntityGroup(p.getTenantId(), p.getId(), entityGroupId),
                EdgeAssignmentRowRef.entityGroup(entityGroupId, groupType));
    }

    @Override
    public EdgeProfile addIntegration(EdgeProfile edgeProfile, IntegrationId integrationId, User user) {
        return added(edgeProfile, user, p -> edgeProfileService.addIntegration(p.getTenantId(), p.getId(), integrationId),
                EdgeAssignmentRowRef.integration(integrationId));
    }

    @Override
    public EdgeProfile removeIntegration(EdgeProfile edgeProfile, IntegrationId integrationId, User user) {
        return removed(edgeProfile, user, p -> edgeProfileService.removeIntegration(p.getTenantId(), p.getId(), integrationId),
                EdgeAssignmentRowRef.integration(integrationId));
    }

    @Override
    public EdgeProfile addSchedulerEvent(EdgeProfile edgeProfile, SchedulerEventId schedulerEventId, User user) {
        return added(edgeProfile, user, p -> edgeProfileService.addSchedulerEvent(p.getTenantId(), p.getId(), schedulerEventId),
                EdgeAssignmentRowRef.schedulerEvent(schedulerEventId));
    }

    @Override
    public EdgeProfile removeSchedulerEvent(EdgeProfile edgeProfile, SchedulerEventId schedulerEventId, User user) {
        return removed(edgeProfile, user, p -> edgeProfileService.removeSchedulerEvent(p.getTenantId(), p.getId(), schedulerEventId),
                EdgeAssignmentRowRef.schedulerEvent(schedulerEventId));
    }

    @Override
    public Job reapply(EdgeProfile edgeProfile, User user) {
        Job job = edgeProfileJobService.submitApply(edgeProfile.getTenantId(), edgeProfile);
        logEntityActionService.logEntityAction(edgeProfile.getTenantId(), edgeProfile.getId(), edgeProfile, ActionType.UPDATED, user);
        return job;
    }

    @Override
    public Job getLatestJob(EdgeProfile edgeProfile) {
        return edgeProfileJobService.findLatestJob(edgeProfile.getTenantId(), edgeProfile.getId());
    }

    private EdgeProfile added(EdgeProfile profile, User user, Function<EdgeProfile, EdgeProfile> change, EdgeAssignmentRowRef row) {
        TenantId tenantId = profile.getTenantId();
        try {
            EdgeProfile saved = change.apply(profile);
            edgeProfileJobService.submitRows(tenantId, saved, EdgeProfileApplyOp.ADD_ROWS, List.of(row), null, null);
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, ActionType.UPDATED, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE_PROFILE), profile, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    private EdgeProfile removed(EdgeProfile profile, User user, Function<EdgeProfile, EdgeProfile> change, EdgeAssignmentRowRef row) {
        TenantId tenantId = profile.getTenantId();
        try {
            EdgeProfile saved = change.apply(profile);
            edgeProfileJobService.submitRows(tenantId, saved, EdgeProfileApplyOp.REMOVE_ROW, List.of(row), null, null);
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, ActionType.UPDATED, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE_PROFILE), profile, ActionType.UPDATED, user, e);
            throw e;
        }
    }

}
