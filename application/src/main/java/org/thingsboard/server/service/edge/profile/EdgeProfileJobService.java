// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.JobManager;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.EdgeProfileApplyJobConfiguration;
import org.thingsboard.server.common.data.job.EdgeProfileApplyOp;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows.EdgeAssignmentRowRef;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeProfileJobService {

    private static final String APPLY_KEY_PREFIX = "edgeProfileApply:";
    private static final List<JobStatus> ACTIVE_STATUSES = List.of(JobStatus.QUEUED, JobStatus.PENDING, JobStatus.RUNNING);

    private final JobManager jobManager;
    private final JobService jobService;

    public Job submitRows(TenantId tenantId, EdgeProfile profile, EdgeProfileApplyOp op, List<EdgeAssignmentRowRef> rows,
                          RuleChainId root, RuleChainId previousRoot) {
        EdgeProfileApplyJobConfiguration configuration = configuration(profile, op);
        configuration.setRows(rows.stream().map(EdgeProfileRows::toJobRow).toList());
        configuration.setRootRuleChainId(root);
        configuration.setPreviousRootRuleChainId(previousRoot);
        return submit(tenantId, UUID.randomUUID().toString(), profile.getId(), configuration);
    }

    public Job submitApply(TenantId tenantId, EdgeProfile profile) {
        String key = APPLY_KEY_PREFIX + profile.getId();
        Job active = findActiveJobByKey(tenantId, key);
        if (active != null) {
            return active;
        }
        try {
            return submit(tenantId, key, profile.getId(), configuration(profile, EdgeProfileApplyOp.APPLY));
        } catch (IllegalArgumentException e) {
            active = findActiveJobByKey(tenantId, key);
            if (active != null) {
                return active;
            }
            throw e;
        }
    }

    public Job submitOwnerChange(TenantId tenantId, CustomerId movedCustomerId, Collection<CustomerId> customerIds) {
        EdgeProfileApplyJobConfiguration configuration = EdgeProfileApplyJobConfiguration.builder()
                .op(EdgeProfileApplyOp.OWNER_CHANGE)
                .customerIds(List.copyOf(customerIds))
                .build();
        return submit(tenantId, UUID.randomUUID().toString(), movedCustomerId, configuration);
    }

    public Job submitEdgeOwnerChange(TenantId tenantId, EdgeId movedEdgeId) {
        EdgeProfileApplyJobConfiguration configuration = EdgeProfileApplyJobConfiguration.builder()
                .op(EdgeProfileApplyOp.EDGE_OWNER_CHANGE)
                .build();
        return submit(tenantId, UUID.randomUUID().toString(), movedEdgeId, configuration);
    }

    public Job findLatestJob(TenantId tenantId, EdgeProfileId edgeProfileId) {
        PageData<Job> jobs = jobService.findJobsByFilter(tenantId, filter(edgeProfileId, null), latestFirst());
        return jobs.getData().isEmpty() ? null : jobs.getData().get(0);
    }

    public boolean hasActiveJobs(TenantId tenantId, EdgeProfileId edgeProfileId) {
        return !jobService.findJobsByFilter(tenantId, filter(edgeProfileId, ACTIVE_STATUSES), latestFirst()).getData().isEmpty();
    }

    private Job findActiveJobByKey(TenantId tenantId, String key) {
        Job latest = jobService.findLatestJobByKey(tenantId, key);
        return latest != null && ACTIVE_STATUSES.contains(latest.getStatus()) ? latest : null;
    }

    private static EdgeProfileApplyJobConfiguration configuration(EdgeProfile profile, EdgeProfileApplyOp op) {
        return EdgeProfileApplyJobConfiguration.builder()
                .op(op)
                .edgeProfileId(profile.getId())
                .edgeProfileName(profile.getName())
                .build();
    }

    private static JobFilter filter(EdgeProfileId edgeProfileId, List<JobStatus> statuses) {
        return JobFilter.builder()
                .types(List.of(JobType.EDGE_PROFILE_APPLY))
                .statuses(statuses)
                .entities(List.of(edgeProfileId.getId()))
                .build();
    }

    private static PageLink latestFirst() {
        return new PageLink(1, 0, null, new SortOrder("createdTime", SortOrder.Direction.DESC));
    }

    private Job submit(TenantId tenantId, String key, EntityId entityId,EdgeProfileApplyJobConfiguration configuration) {
        Job job = Job.builder()
                .tenantId(tenantId)
                .type(JobType.EDGE_PROFILE_APPLY)
                .key(key)
                .entityId(entityId)
                .configuration(configuration)
                .build();
        try {
            Job saved = jobManager.submitJob(job).get();
            log.debug("[{}][{}] Submitted edge profile job {} ({})", tenantId, entityId, saved.getId(), configuration.getOp());
            return saved;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
