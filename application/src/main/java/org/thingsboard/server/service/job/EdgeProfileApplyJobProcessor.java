// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.EdgeProfileApplyJobConfiguration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.EdgeProfileApplyTask;
import org.thingsboard.server.common.data.job.task.EdgeProfileApplyTaskResult;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows.EdgeAssignmentRowRef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class EdgeProfileApplyJobProcessor implements JobProcessor {

    private static final int PAGE_SIZE = 1024;

    private final EdgeService edgeService;
    private final EdgeProfileService edgeProfileService;

    @Override
    public int process(Job job, Consumer<Task<?>> taskConsumer) {
        EdgeProfileApplyJobConfiguration configuration = job.getConfiguration();
        int[] tasksCount = {0};
        forEachEdge(job, configuration, edge -> {
            taskConsumer.accept(createTask(job, configuration, edge));
            tasksCount[0]++;
        });
        return tasksCount[0];
    }

    @Override
    public void reprocess(Job job, List<TaskResult> taskFailures, Consumer<Task<?>> taskConsumer) {
        EdgeProfileApplyJobConfiguration configuration = job.getConfiguration();
        for (TaskResult taskFailure : taskFailures) {
            EdgeProfileApplyTaskResult.EdgeProfileApplyTaskFailure failure = ((EdgeProfileApplyTaskResult) taskFailure).getFailure();
            taskConsumer.accept(createTask(job, configuration, failure.getEntityInfo()));
        }
    }

    private void forEachEdge(Job job, EdgeProfileApplyJobConfiguration configuration, Consumer<EntityInfo> consumer) {
        TenantId tenantId = job.getTenantId();
        switch (configuration.getOp()) {
            case ADD_ROWS, SET_ROOT, APPLY -> {
                if (edgeProfileService.findEdgeProfileById(tenantId, configuration.getEdgeProfileId()) == null) {
                    return;
                }
                for (Edge edge : new PageDataIterable<>(link -> edgeService.findEdgesByTenantIdAndEdgeProfileId(tenantId, configuration.getEdgeProfileId(), link), PAGE_SIZE)) {
                    consumer.accept(new EntityInfo(edge.getId(), edge.getName()));
                }
            }
            case REMOVE_ROW -> {
                for (EdgeId edgeId : edgesCarryingRow(tenantId, EdgeProfileRows.fromJobRow(configuration.getRows().get(0)))) {
                    acceptIfPresent(tenantId, edgeId, consumer);
                }
            }
            case OWNER_CHANGE -> {
                for (CustomerId customerId : configuration.getCustomerIds()) {
                    for (Edge edge : new PageDataIterable<>(link -> edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, link), PAGE_SIZE)) {
                        consumer.accept(new EntityInfo(edge.getId(), edge.getName()));
                    }
                }
            }
            case EDGE_OWNER_CHANGE -> acceptIfPresent(tenantId, new EdgeId(job.getEntityId().getId()), consumer);
        }
    }

    private void acceptIfPresent(TenantId tenantId, EdgeId edgeId, Consumer<EntityInfo> consumer) {
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge != null) {
            consumer.accept(new EntityInfo(edge.getId(), edge.getName()));
        }
    }

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

    private EdgeProfileApplyTask createTask(Job job, EdgeProfileApplyJobConfiguration configuration, EntityInfo edge) {
        return EdgeProfileApplyTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .key(configuration.getTasksKey())
                .retries(0)
                .entityInfo(edge)
                .op(configuration.getOp())
                .edgeProfileId(configuration.getEdgeProfileId())
                .rows(configuration.getRows())
                .rootRuleChainId(configuration.getRootRuleChainId())
                .previousRootRuleChainId(configuration.getPreviousRootRuleChainId())
                .build();
    }

    @Override
    public JobType getType() {
        return JobType.EDGE_PROFILE_APPLY;
    }

}
