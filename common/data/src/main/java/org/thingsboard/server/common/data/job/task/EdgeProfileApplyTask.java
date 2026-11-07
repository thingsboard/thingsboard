// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.job.EdgeProfileApplyOp;
import org.thingsboard.server.common.data.job.EdgeProfileJobRow;
import org.thingsboard.server.common.data.job.JobType;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class EdgeProfileApplyTask extends Task<EdgeProfileApplyTaskResult> {

    private EntityInfo entityInfo;
    private EdgeProfileApplyOp op;
    private EdgeProfileId edgeProfileId;
    private List<EdgeProfileJobRow> rows;
    private RuleChainId rootRuleChainId;
    private RuleChainId previousRootRuleChainId;

    @Override
    public EdgeProfileApplyTaskResult toFailed(Throwable error) {
        return EdgeProfileApplyTaskResult.failed(this, error);
    }

    @Override
    public EdgeProfileApplyTaskResult toDiscarded() {
        return EdgeProfileApplyTaskResult.discarded(this);
    }

    @Override
    public EntityId getEntityId() {
        return entityInfo.getId();
    }

    @Override
    public JobType getJobType() {
        return JobType.EDGE_PROFILE_APPLY;
    }

}
