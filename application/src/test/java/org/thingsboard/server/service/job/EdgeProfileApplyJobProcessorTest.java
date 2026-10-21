// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.EdgeProfileApplyJobConfiguration;
import org.thingsboard.server.common.data.job.EdgeProfileApplyOp;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.EdgeProfileApplyTask;
import org.thingsboard.server.common.data.job.task.EdgeProfileApplyTaskResult;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows.EdgeAssignmentRowRef;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgeProfileApplyJobProcessorTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    EdgeService edgeService;
    @Mock
    EdgeProfileService edgeProfileService;
    @InjectMocks
    EdgeProfileApplyJobProcessor processor;

    private final EdgeProfileId profileId = new EdgeProfileId(UUID.randomUUID());

    private Job job(EdgeProfileApplyJobConfiguration configuration) {
        Job job = Job.builder().tenantId(TENANT_ID).type(JobType.EDGE_PROFILE_APPLY).key("k").entityId(profileId).configuration(configuration).build();
        job.setId(new JobId(UUID.randomUUID()));
        return job;
    }

    private Edge edge(String name) {
        Edge edge = new Edge(new EdgeId(UUID.randomUUID()));
        edge.setName(name);
        return edge;
    }

    private List<EdgeProfileApplyTask> process(Job job) throws Exception {
        List<Task<?>> tasks = new ArrayList<>();
        int count = processor.process(job, tasks::add);
        assertThat(count).isEqualTo(tasks.size());
        return tasks.stream().map(EdgeProfileApplyTask.class::cast).toList();
    }

    @Test
    void addRowsEmitsOneTaskPerProfileEdgeCarryingTheJobPayload() throws Exception {
        Edge e1 = edge("e1");
        Edge e2 = edge("e2");
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, profileId)).thenReturn(new EdgeProfile(profileId));
        when(edgeService.findEdgesByTenantIdAndEdgeProfileId(eq(TENANT_ID), eq(profileId), any()))
                .thenReturn(new PageData<>(List.of(e1, e2), 1, 2, false));
        RuleChainId root = new RuleChainId(UUID.randomUUID());
        Job job = job(EdgeProfileApplyJobConfiguration.builder().op(EdgeProfileApplyOp.SET_ROOT).edgeProfileId(profileId)
                .rows(List.of(EdgeProfileRows.toJobRow(EdgeAssignmentRowRef.ruleChain(root)))).rootRuleChainId(root).build());

        List<EdgeProfileApplyTask> tasks = process(job);

        assertThat(tasks).extracting(task -> task.getEntityInfo().getName()).containsExactly("e1", "e2");
        EdgeProfileApplyTask task = tasks.get(0);
        assertThat(task.getOp()).isEqualTo(EdgeProfileApplyOp.SET_ROOT);
        assertThat(task.getRootRuleChainId()).isEqualTo(root);
        assertThat(task.getKey()).isEqualTo(job.getConfiguration().getTasksKey());
        assertThat(task.getJobId()).isEqualTo(job.getId());
    }

    @Test
    void missingProfileEmitsNoTasks() throws Exception {
        Job job = job(EdgeProfileApplyJobConfiguration.builder().op(EdgeProfileApplyOp.APPLY).edgeProfileId(profileId).build());

        assertThat(process(job)).isEmpty();
        verify(edgeService, never()).findEdgesByTenantIdAndEdgeProfileId(any(), any(), any());
    }

    @Test
    void removeRowWalksTheEdgesCarryingTheRowWhateverTheirProfile() throws Exception {
        Edge carrying = edge("carrying");
        EdgeAssignmentRowRef row = EdgeAssignmentRowRef.ruleChain(new RuleChainId(UUID.randomUUID()));
        EdgeId deleted = new EdgeId(UUID.randomUUID());
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), eq(List.of(row.id())), eq(EntityType.RULE_CHAIN), any()))
                .thenReturn(new PageData<>(List.of(carrying.getId(), deleted), 1, 2, false));
        when(edgeService.findEdgeById(TENANT_ID, carrying.getId())).thenReturn(carrying);
        when(edgeService.findEdgeById(TENANT_ID, deleted)).thenReturn(null);
        Job job = job(EdgeProfileApplyJobConfiguration.builder().op(EdgeProfileApplyOp.REMOVE_ROW).edgeProfileId(profileId)
                .rows(List.of(EdgeProfileRows.toJobRow(row))).build());

        assertThat(process(job)).extracting(task -> task.getEntityInfo().getId()).containsExactly(carrying.getId());
        verify(edgeService, never()).findEdgesByTenantIdAndEdgeProfileId(any(), any(), any());
    }

    @Test
    void ownerChangeWalksTheEdgesOfEveryListedCustomer() throws Exception {
        CustomerId c1 = new CustomerId(UUID.randomUUID());
        CustomerId c2 = new CustomerId(UUID.randomUUID());
        when(edgeService.findEdgesByTenantIdAndCustomerId(eq(TENANT_ID), eq(c1), any())).thenReturn(new PageData<>(List.of(edge("a")), 1, 1, false));
        when(edgeService.findEdgesByTenantIdAndCustomerId(eq(TENANT_ID), eq(c2), any())).thenReturn(new PageData<>(List.of(edge("b")), 1, 1, false));
        Job job = job(EdgeProfileApplyJobConfiguration.builder().op(EdgeProfileApplyOp.OWNER_CHANGE).customerIds(List.of(c1, c2)).build());

        assertThat(process(job)).extracting(task -> task.getEntityInfo().getName()).containsExactly("a", "b");
    }

    @Test
    void edgeOwnerChangeEmitsOneTaskForTheMovedEdge() throws Exception {
        Edge moved = edge("moved");
        when(edgeService.findEdgeById(TENANT_ID, moved.getId())).thenReturn(moved);
        Job job = job(EdgeProfileApplyJobConfiguration.builder().op(EdgeProfileApplyOp.EDGE_OWNER_CHANGE).build());
        job.setEntityId(moved.getId());

        List<EdgeProfileApplyTask> tasks = process(job);

        assertThat(tasks).extracting(task -> task.getEntityInfo().getId()).containsExactly(moved.getId());
        assertThat(tasks.get(0).getOp()).isEqualTo(EdgeProfileApplyOp.EDGE_OWNER_CHANGE);
    }

    @Test
    void edgeOwnerChangeOfADeletedEdgeEmitsNoTasks() throws Exception {
        Job job = job(EdgeProfileApplyJobConfiguration.builder().op(EdgeProfileApplyOp.EDGE_OWNER_CHANGE).build());
        job.setEntityId(new EdgeId(UUID.randomUUID()));

        assertThat(process(job)).isEmpty();
    }

    @Test
    void reprocessRebuildsTasksForTheFailedEdgesOnly() throws Exception {
        Job job = job(EdgeProfileApplyJobConfiguration.builder().op(EdgeProfileApplyOp.APPLY).edgeProfileId(profileId).build());
        EntityInfo failedEdge = new EntityInfo(new EdgeId(UUID.randomUUID()), "failed");
        EdgeProfileApplyTask failedTask = EdgeProfileApplyTask.builder().entityInfo(failedEdge).build();
        List<Task<?>> tasks = new ArrayList<>();

        processor.reprocess(job, List.of(EdgeProfileApplyTaskResult.failed(failedTask, new RuntimeException("boom"))), tasks::add);

        assertThat(tasks).hasSize(1);
        EdgeProfileApplyTask task = (EdgeProfileApplyTask) tasks.get(0);
        assertThat(task.getEntityInfo()).isEqualTo(failedEdge);
        assertThat(task.getOp()).isEqualTo(EdgeProfileApplyOp.APPLY);
        assertThat(task.getEdgeProfileId()).isEqualTo(profileId);
    }

}
