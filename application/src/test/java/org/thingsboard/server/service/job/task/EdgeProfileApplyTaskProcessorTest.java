// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.job.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.EdgeProfileApplyOp;
import org.thingsboard.server.common.data.job.task.EdgeProfileApplyTask;
import org.thingsboard.server.service.edge.profile.EdgeProfileApplier;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows.EdgeAssignmentRowRef;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EdgeProfileApplyTaskProcessorTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    EdgeProfileApplier edgeProfileApplier;

    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());
    private final EdgeProfileId profileId = new EdgeProfileId(UUID.randomUUID());
    private final EdgeAssignmentRowRef row = EdgeAssignmentRowRef.ruleChain(new RuleChainId(UUID.randomUUID()));

    private EdgeProfileApplyTaskProcessor processor() {
        EdgeProfileApplyTaskProcessor processor = new EdgeProfileApplyTaskProcessor(edgeProfileApplier);
        ReflectionTestUtils.setField(processor, "timeoutMs", 1000L);
        return processor;
    }

    private EdgeProfileApplyTask task(EdgeProfileApplyOp op, RuleChainId root, RuleChainId previousRoot) {
        return EdgeProfileApplyTask.builder()
                .tenantId(TENANT_ID)
                .entityInfo(new EntityInfo(edgeId, "edge"))
                .op(op)
                .edgeProfileId(profileId)
                .rows(List.of(EdgeProfileRows.toJobRow(row)))
                .rootRuleChainId(root)
                .previousRootRuleChainId(previousRoot)
                .build();
    }

    @Test
    void addRowsAppliesTheRowsWithoutMovingTheRoot() throws Exception {
        assertThat(processor().process(task(EdgeProfileApplyOp.ADD_ROWS, null, null)).isSuccess()).isTrue();

        verify(edgeProfileApplier).addRows(TENANT_ID, edgeId, profileId, List.of(row), false, null, null);
    }

    @Test
    void setRootAppliesTheRowsAndMovesTheRoot() throws Exception {
        RuleChainId root = new RuleChainId(UUID.randomUUID());
        RuleChainId previousRoot = new RuleChainId(UUID.randomUUID());

        processor().process(task(EdgeProfileApplyOp.SET_ROOT, root, previousRoot));

        verify(edgeProfileApplier).addRows(TENANT_ID, edgeId, profileId, List.of(row), true, root, previousRoot);
    }

    @Test
    void removeRowRemovesEachRow() throws Exception {
        processor().process(task(EdgeProfileApplyOp.REMOVE_ROW, null, null));

        verify(edgeProfileApplier).removeRow(TENANT_ID, edgeId, profileId, row);
    }

    @Test
    void applyReappliesTheEdge() throws Exception {
        processor().process(task(EdgeProfileApplyOp.APPLY, null, null));

        verify(edgeProfileApplier).reapply(TENANT_ID, edgeId, profileId);
    }

    @Test
    void ownerChangeReappliesTheEdgeAgainstItsOwnProfile() throws Exception {
        EdgeProfileApplyTask task = EdgeProfileApplyTask.builder().tenantId(TENANT_ID)
                .entityInfo(new EntityInfo(edgeId, "edge")).op(EdgeProfileApplyOp.OWNER_CHANGE).build();

        processor().process(task);

        verify(edgeProfileApplier).applyAfterOwnerChange(TENANT_ID, edgeId);
    }

    @Test
    void edgeOwnerChangeReappliesTheMovedEdge() throws Exception {
        EdgeProfileApplyTask task = EdgeProfileApplyTask.builder().tenantId(TENANT_ID)
                .entityInfo(new EntityInfo(edgeId, "edge")).op(EdgeProfileApplyOp.EDGE_OWNER_CHANGE).build();

        processor().process(task);

        verify(edgeProfileApplier).applyAfterOwnerChange(TENANT_ID, edgeId);
    }

    @Test
    void failedTaskCarriesTheEdgeForReprocessing() {
        EdgeProfileApplyTask task = task(EdgeProfileApplyOp.APPLY, null, null);

        var failed = task.toFailed(new IllegalStateException("boom"));

        assertThat(failed.isSuccess()).isFalse();
        assertThat(failed.getFailure().getEntityInfo().getId()).isEqualTo(edgeId);
        assertThat(failed.getError()).isEqualTo(edgeId + ": boom");
    }

}
