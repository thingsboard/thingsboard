// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.job.task;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.EdgeProfileApplyTask;
import org.thingsboard.server.common.data.job.task.EdgeProfileApplyTaskResult;
import org.thingsboard.server.queue.task.TaskProcessor;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.profile.EdgeProfileApplier;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows.EdgeAssignmentRowRef;

import java.util.List;

@TbCoreComponent
@Component
@RequiredArgsConstructor
public class EdgeProfileApplyTaskProcessor extends TaskProcessor<EdgeProfileApplyTask, EdgeProfileApplyTaskResult> {

    private final EdgeProfileApplier edgeProfileApplier;

    @Value("${edges.profile.apply_task_timeout_ms:120000}")
    private long timeoutMs;

    @Override
    public EdgeProfileApplyTaskResult process(EdgeProfileApplyTask task) throws Exception {
        TenantId tenantId = task.getTenantId();
        EdgeId edgeId = new EdgeId(task.getEntityId().getId());
        List<EdgeAssignmentRowRef> rows = task.getRows() == null ? List.of() : task.getRows().stream().map(EdgeProfileRows::fromJobRow).toList();
        switch (task.getOp()) {
            case ADD_ROWS -> edgeProfileApplier.addRows(tenantId, edgeId, task.getEdgeProfileId(), rows, false, null, null);
            case SET_ROOT -> edgeProfileApplier.addRows(tenantId, edgeId, task.getEdgeProfileId(), rows, true, task.getRootRuleChainId(), task.getPreviousRootRuleChainId());
            case REMOVE_ROW -> {
                for (EdgeAssignmentRowRef row : rows) {
                    edgeProfileApplier.removeRow(tenantId, edgeId, task.getEdgeProfileId(), row);
                }
            }
            case APPLY -> edgeProfileApplier.reapply(tenantId, edgeId, task.getEdgeProfileId());
            case OWNER_CHANGE, EDGE_OWNER_CHANGE -> edgeProfileApplier.applyAfterOwnerChange(tenantId, edgeId);
        }
        return EdgeProfileApplyTaskResult.success(task);
    }

    @Override
    public long getProcessingTimeout(EdgeProfileApplyTask task) {
        return timeoutMs;
    }

    @Override
    public JobType getJobType() {
        return JobType.EDGE_PROFILE_APPLY;
    }

}
